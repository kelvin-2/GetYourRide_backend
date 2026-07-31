# GetYourRide — Backend Architecture

This document provides a comprehensive overview of the backend architecture for the GetYourRide project.

## 🏗 Technology Stack
- **Framework:** Spring Boot 3.x
- **Language:** Java 17
- **Database:** MySQL
- **Authentication:** JWT (JSON Web Tokens)
- **Build Tool:** Maven

---

## 📂 Project Structure

The project follows a standard layered architecture:

```
com.example1.getyourride
│
├── config/             # Spring Configuration (Security, CORS, etc.)
├── controller/         # REST Controllers (API Endpoints)
├── dto/                # Data Transfer Objects
│   ├── message/        # Server-pushed WebSocket/STOMP messages
│   ├── request/        # Incoming request payloads
│   └── response/       # Outgoing response payloads
├── entity/             # JPA Entities (Database Models)
├── exception/          # Global Exception Handling
├── repository/         # Spring Data JPA Repositories (Data Access)
├── scheduler/          # @Scheduled triggers (simulation tick)
├── security/           # JWT and Security logic
├── validation/         # Custom Bean Validation constraints
└── service/            # Business Logic Layer
    └── impl/           # Service Implementations
```

---

## 🗄 Database Schema (Entities)

### 1. Student
Represents a student user who can book rides.
- `studentId` (PK)
- `studentNumber` (Unique)
- `firstName`, `lastName`
- `email` (Unique)
- `phone`
- `password` (Stored in plain text per project decision)
- `isFunded` (Boolean)
- `createdAt`

### 2. Driver
Represents a driver (either a student driver or a shuttle driver).
- `driverId` (PK)
- `firstName`, `lastName`
- `email` (Unique)
- `phone`
- `password`
- `role` (STUDENT_DRIVER, SHUTTLE_DRIVER)
- `isVerified`
- `totalTrips`
- `joinDate`

### 3. Vehicle
A vehicle tied to a specific driver.
- `vehicleId` (PK)
- `driverId` (FK to Driver)
- `registrationNumber` (Unique)
- `model`
- `vehicleYear`
- `colour`
- `capacity`

### 4. Trip
Represents a ride offered by a driver.
- `tripId` (PK)
- `driverId` (FK to Driver)
- `registrationNumber` (FK to Vehicle)
- `tripType` (e.g., STUDENT_DRIVER, SHUTTLE)
- `departureStop`, `destinationStop`
- `departureTime`, `arrivalTime`
- `availableSeats`
- `price`
- `status` (SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED)

### 5. TripLegRoute
One precomputed road-following route between two consecutive stops on a trip. Maps
`trip_leg_route`, added by the Phase 0 tracking migration.
- `id` (PK)
- `trip` (FK to Trip)
- `fromStopOrder`, `toStopOrder` — the `trip_stop.stop_order` values this leg connects
- `routeGeometry` — MySQL `json`, an array of `[latitude, longitude]` pairs in travel order
- `distanceMeters`, `durationSeconds` — as reported by OpenRouteService

Tracking is leg-based: the vehicle follows one leg's polyline at a time rather than a single
straight line for the whole trip. Geometry is fetched from ORS once and stored, because ORS has a
request quota and adds latency that a per-tick simulation loop cannot absorb.

### 6. TripLocationHistory
One recorded vehicle position. Maps `trip_location_history`, added by the Phase 0 migration. The
simulation engine appends a row per tick.
- `id` (PK)
- `trip` (FK to Trip)
- `latitude`, `longitude`
- `recordedAt`

This is the durable trail. Broadcast messages are fire-and-forget, so without this a client that
connects late or drops a frame could not reconstruct where the vehicle has been. Its existence is
what lets the broadcaster treat a dropped message as acceptable.

### Trip live-tracking columns

`Trip` also maps five columns added by the Phase 0 migration and wired up in Phase 4. Together they
are the simulation cursor — the resume point that lets a tick pick up where the last one stopped:
- `currentLat`, `currentLng` — last published position
- `currentLegIndex` — which `trip_leg_route` row the vehicle is on
- `currentPointIndex` — how far along that leg's polyline
- `dwellUntil` — while set and in the future, the vehicle is paused at a stop

### TripStop status

`trip_stop.status` is `ENUM('PENDING','ARRIVED')`, added by `doc/02_trip_stop_status.sql` and mapped
to `TripStopStatus`. It was missing from the original schema, which blocked the Phase 4 requirement
to mark a stop arrived.

Note this is separate from `StopEventStatus` in `dto/message`. That one is the WebSocket wire
contract; this one is persistence. They overlap on `ARRIVED` today but serve different masters —
`PENDING` is never broadcast, and a future column value should not be forced onto the Android client
just because the schema gained it.

---

## 🚀 API Endpoints

### Authentication
#### Student Auth (`/api/auth/student`)
- `POST /register`: Register a new student.
- `POST /login`: Login as a student and receive a JWT.

#### Driver Auth (`/api/auth/driver`)
- `POST /register`: Register a new driver.
- `POST /login`: Login as a driver and receive a JWT.

### Trips (`/api/trips`)
- `POST /`: Create a new trip (Authenticated Drivers).
- `GET /`: List all trips.
- `GET /{id}`: Get trip details by ID.
- `GET /status/{status}`: Filter trips by status.
- `PATCH /{id}/status?status={status}`: Update trip status (Authenticated Drivers).

### Trip Leg Routes (`/api/trips/{tripId}`)
- `POST /precompute-route`: Calculate and store one road-following route per consecutive pair of
  the trip's stops. Idempotent — replaces any existing legs.
- `GET /legs`: List the trip's precomputed legs in travel order (geometry summarised).

### Routes (`/api/rides`)
- `GET /{rideId}/route`: Road-following route between the trip's departure and destination
  coordinates. `rideId` is a trip id.

### Live Tracking (WebSocket)
- `/ws`: STOMP handshake endpoint. Subscribe to `/topic/trip/{tripId}` for live updates.

### Tracking Test Publisher (`/api/trips/{tripId}/tracking`) — dev only
Registered only when `getyourride.tracking.test-publisher.enabled=true`; returns 404 otherwise.
- `POST /test-location?lat=&lng=&legIndex=`: Publish one `LOCATION_UPDATE`.
- `POST /test-stop-event?stopId=&status=`: Publish one `STOP_EVENT`.

---

## 🔐 Security & Authentication

- **JWT Authentication:** The application uses JWT to secure endpoints.
- **Roles:** Different access levels for `STUDENT`, `STUDENT_DRIVER`, and `SHUTTLE_DRIVER`.
- **Stateless:** The backend does not maintain sessions; every request must include a valid `Authorization: Bearer <token>` header.
- **Password Handling:** Note that passwords are currently stored in plain text as per the project requirements.

---

## ✅ Request Validation

Validation happens at the request boundary, not in services. Controllers annotate their
`@RequestBody` arguments with `@Valid`; violations become a `MethodArgumentNotValidException`
that `GlobalExceptionHandler` renders as a `400` with a `{"field": "message"}` body.

### Coordinate validation

`trip_stop.latitude` and `trip_stop.longitude` are `NOT NULL DOUBLE`, so the database accepts
`0.0, 0.0` — a point in the Atlantic off West Africa. That is the signature of a client which
lost the selected address suggestion's coordinates, and stored `0,0` stops corrupt every
downstream distance and route calculation.

- **`validation/ValidCoordinates`** — class-level constraint applied to `TripStopRequest`.
  Class-level rather than field-level because validity is a property of the lat/lng *pair*.
- **`validation/CoordinatesValidator`** — rejects exactly `0,0` (within a 1e-6 tolerance, so
  `-0.0` and float noise are caught too) and any value outside ±90 / ±180. A lone zero
  component is allowed: the equator is a real place. There is deliberately **no** Gqeberha
  bounding box, because `GeocodingService` supports out-of-town addresses.
- **`validation/HasCoordinates`** — small interface implemented by coordinate-bearing DTOs, so
  the validator does not import from `dto` and the dependency stays one-directional.

The constraint lives on the shared `TripStopRequest`, so all four stop-writing paths are
covered by one rule: `POST /api/trips`, `POST /api/trips/{id}/book`,
`POST /api/trips/{id}/stops` and `POST /api/trips/{id}/stops/student`.

### Cascading into nested DTOs

`@Valid` does not descend into nested objects or collections on its own. Both
`CreateTripRequest.stops` and the two `BookCarpoolRequest` stop fields carry `@Valid` for this
reason — without it the constraints on `TripStopRequest` are inert and bad data saves silently.
`CreateTripRequest.stops` additionally declares an element-level
`List<@NotNull TripStopRequest>`, because `@Valid` cascades into list elements but skips `null`
ones, which previously turned a `"stops": [null]` payload into a `NullPointerException`.

### Diagnostic logging

The stop-writing services log received coordinates at `DEBUG` before persistence, to separate
"the client never sent them" from "the backend discarded them". Off by default — these are
effectively student home addresses. Enable with
`logging.level.com.example1.getyourride.service.impl=DEBUG` (commented line in
`application.properties`).

### Manual verification

Confirming coordinates round-trip correctly needs a live database, so it cannot be covered by
the automated tests. Against a running instance:

```bash
# Expect 400 with a message on stops[0].latitude / stops[0].longitude
curl -i -X POST http://localhost:8080/api/trips \
  -H "Authorization: Bearer <driver-token>" -H "Content-Type: application/json" \
  -d '{"tripType":"Carpool","departureStop":"Walmer","destinationStop":"South Campus",
       "departureTime":"2026-08-01T07:30:00","availableSeats":3,"price":25.00,
       "stops":[{"stopName":"Somewhere","latitude":0.0,"longitude":0.0,"stopOrder":1}]}'

# Expect 400 naming stops[0]
# ... same request with "stops":[null]

# Expect 201/200, then confirm the row matches what was sent
# ... same request with "latitude":-33.9758,"longitude":25.5858
```

```sql
-- Must return zero rows at all times
SELECT * FROM trip_stop WHERE latitude = 0 AND longitude = 0;
```

---

## 🗺 Routing (OpenRouteService)

Two layers, deliberately separated:

- **`RouteService`** — the only OpenRouteService integration point. Knows nothing about trips.
  Owns the API key, the coordinate-order flip, and error translation. Anything needing a
  road-following route goes through it rather than building its own HTTP call.
- **`TripRouteService` / `TripRouteServiceImpl`** — trip-aware. Resolves a trip id into real
  coordinates and owns `trip_leg_route` precomputation. Controllers depend on this so they never
  touch `TripRepository` directly.

### Coordinate order

ORS takes `lng,lat`. The rest of the application — `RouteResponse`, `trip_leg_route.route_geometry`,
the Android client — uses `lat,lng`. `RouteService` flips in both directions and is the only place
that should. A flip here puts every position in the wrong hemisphere, silently, so it is covered by
tests.

### Leg precomputation

`POST /api/trips/{tripId}/precompute-route` loops the trip's stops ordered by `stop_order`, calls
ORS once per consecutive pair, and stores each result as a `trip_leg_route` row. Notes on the
design:

- **Not wired into trip creation.** One ORS call per leg means an outage or exhausted quota would
  stop drivers from posting rides. As a separate, idempotent call, a failure leaves the trip intact
  and the operation can simply be retried.
- **Idempotent.** Existing legs for the trip are deleted and flushed before the new set is written,
  so re-running after stops change does not leave stale legs behind.
- **Atomic.** The ORS calls happen inside the transaction. That holds a connection open across
  external HTTP, which is normally worth avoiding, but a partially written leg set would leave the
  simulator with a route that stops halfway. Stop counts are small and this is an explicit setup
  action, not a hot path.
- **Requires at least two stops.** Fewer than two means no consecutive pair, so it returns `400`
  rather than silently storing nothing.
- **Rejects `0,0` and missing stop coordinates** before spending ORS quota. Phase 1 blocks these at
  the request boundary, but rows edited directly in the database could still hold them, and ORS
  would return a route across the Atlantic rather than an error.

> **Known design gap:** legs are built purely from `trip_stop` rows, per §4.1 of the tracking
> documentation. `trip_stop` does not always span the trip's own `departure_stop`/`destination_stop`
> — trip 24's stops run *Summerstrand → North Campus* while its destination is *South Campus*, so
> its legs stop short of the destination. This follows the documented design rather than inventing a
> splice rule; revisit if legs are meant to always terminate at the trip destination.

---

## 📡 Live Tracking Transport (STOMP over WebSocket)

Position updates are pushed, not polled. `spring-boot-starter-websocket` provides the transport;
`WebSocketConfig` registers it.

| Concern | Value |
|---|---|
| Handshake endpoint | `/ws` (plain WebSocket, no SockJS) |
| Broker | Simple in-memory broker on `/topic` |
| Per-trip destination | `/topic/trip/{tripId}` |
| Message shapes | `LocationUpdateDTO`, `StopEventDTO` — both on the same destination |

### Message contract

Both shapes carry a `type` discriminator, because subscribers receive them on one destination.
These field names are a contract with the Android client — renaming them breaks it silently, with no
compile error on either side.

```json
{ "type": "LOCATION_UPDATE", "tripId": 42, "lat": -33.96, "lng": 25.61, "legIndex": 1 }
{ "type": "STOP_EVENT", "tripId": 42, "stopId": 7, "status": "ARRIVED" }
```

`lat`/`lng` are abbreviated here while the rest of the codebase uses `latitude`/`longitude`. That
inconsistency is deliberate: the contract predates the code and the client already expects it.
`TrackingMessageContractTest` asserts the exact serialised JSON so drift fails the build.

### Design notes

- **Simple broker, not a relay.** Subscriptions live in application memory — no external broker to
  operate, and tracking messages are transient (a missed tick is superseded two seconds later, with
  `trip_location_history` as the durable record). It does not work across multiple instances, since
  each would only reach its own connected clients. Scaling out means `enableStompBrokerRelay`, which
  is a change in `WebSocketConfig` rather than to any publishing code.
- **No SockJS.** It would relocate the real endpoint to `/ws/websocket`, breaking both the Android
  client and a direct `wscat` test against `/ws`. Add a second SockJS endpoint if a browser client
  ever needs a fallback.
- **No application destination prefix.** Tracking is push-only; there are no `@MessageMapping`
  handlers, so declaring a prefix would advertise an inbound path nothing serves.
- **`TrackingBroadcastService`** is the only thing that talks to `SimpMessagingTemplate`. Publish
  failures are logged and swallowed: from Phase 4 the caller is a `@Scheduled` tick shared by all
  active trips, so a propagating exception could stall the simulation for every trip.

### Security

The `/ws` handshake is a normal HTTP GET, so it passes through the Spring Security filter chain and
is covered by the existing `anyRequest().authenticated()` rule. It is **not** in the public matcher
list — clients must send `Authorization: Bearer <token>` on the handshake, which `JwtAuthFilter`
reads exactly as for any REST call.

> ⚠️ **Open gap:** authentication gates the *connection*, not the *subscription*. Any authenticated
> user can subscribe to `/topic/trip/{tripId}` for a trip they have no relationship with and watch
> its live position. Closing this needs a per-destination authorisation check on inbound SUBSCRIBE
> frames plus a policy decision on who may watch a trip. Tracked against Phase 5 in `doc/Task`.

`spring-security-messaging` is not on the classpath, so there is no STOMP CSRF interception in play.

### Manual verification

The test publisher is disabled by default. Enable it locally, then:

```bash
# 1. Subscribe (token required — the handshake is authenticated)
wscat -H "Authorization: Bearer <token>" -c ws://localhost:8080/ws
# then send STOMP frames: CONNECT, then SUBSCRIBE to /topic/trip/42

# 2. Publish from another terminal
curl -X POST -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/trips/42/tracking/test-location?lat=-33.96&lng=25.61&legIndex=1"
curl -X POST -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/trips/42/tracking/test-stop-event?stopId=7&status=ARRIVED"
```

---

## 🚐 Trip Simulation Engine

There is no real GPS feed from drivers, so vehicle movement is simulated. A scheduled tick walks each
in-progress trip along the leg polylines precomputed into `trip_leg_route`.

### The cursor

A trip's position is a cursor into its own route data: `current_leg_index` selects the leg,
`current_point_index` selects the point within it. A tick advances the cursor, writes the new
coordinates to `trip`, appends a `trip_location_history` row, and broadcasts a `LOCATION_UPDATE`.

Keeping the cursor in the database rather than in memory does two things: the simulation survives a
restart instead of teleporting the vehicle, and trips are inherently independent because each one's
progress lives in its own row.

### Tick lifecycle

1. **Dwelling?** If `dwell_until` is in the future, do nothing. If it has passed, clear it and move.
2. **Advance.** Step forward by a per-leg step size. Write position, record history, broadcast.
3. **End of leg?** Snap to the leg's final point rather than overshooting. Mark the leg's destination
   stop `ARRIVED`, broadcast a `STOP_EVENT`, then either start the next leg after a dwell pause, or
   complete the trip if that was the final leg.

Completion is gated on the leg index being the last one, so a trip cannot report `COMPLETED` early.

### Step size

Derived per leg from the leg's own ORS `duration_seconds` rather than one flat value, so a long
highway leg and a short side-street leg take time proportional to reality. A fixed step would crawl
through a dense urban polyline and rocket along a sparse one.

```
wallClockSeconds = durationSeconds / speedMultiplier
ticksForLeg      = max(wallClockSeconds / tickIntervalSeconds, 1)
stepSize         = max(1, ceil(pointCount / ticksForLeg))
```

`speedMultiplier` compresses real time — at 10x a five-minute leg is watched in thirty seconds.
Clamping to at least 1 matters: without it a leg with more points than ticks would compute a sub-1
step and the vehicle would never move.

### Why the scheduler is a separate bean

`scheduler.TripSimulationScheduler` holds the `@Scheduled` method; `TripSimulationServiceImpl` holds
the logic. The split is deliberate. Each trip must advance in its own transaction so one failing trip
cannot roll back or block the others, and Spring applies `@Transactional` through a proxy — a method
calling another method on `this` bypasses it. Crossing a bean boundary makes the proxy apply, and lets
the loop catch per trip.

### Configuration

All under `getyourride.tracking.simulation.*`:

| Property | Default | Purpose |
|---|---|---|
| `enabled` | **`false`** | Master switch. The scheduler bean is not registered unless this is `true`. |
| `tick-interval-ms` | `4000` | Tick cadence. Read at annotation level, so changes need a restart. |
| `speed-multiplier` | `10.0` | Real-time compression. `1.0` is real time. |
| `dwell-seconds` | `20` | Simulated boarding pause at each stop. |
| `fallback-step-size` | `5` | Points per tick when a leg has no usable ORS duration. |

> **`enabled` is off by default on purpose.** Every tick writes to the database — it mutates
> `trip.current_*` and appends `trip_location_history` rows for any `IN_PROGRESS` trip. Anything that
> starts an application context inherits that, including the `@SpringBootTest` suite, which runs
> against the configured database. An opt-in flag means a test run cannot quietly advance live trips.

### Starting a trip

`PATCH /api/trips/{id}/status?status=IN_PROGRESS` calls `TripSimulationService.startTracking`, which
resets the cursor to zero, seeds `current_lat/lng` at the first stop, and clears any `ARRIVED` stops
from a previous run. Without the reset, restarting a trip would resume from stale indices and the
vehicle would appear mid-route; without the stop reset, the stop list would render as already-visited.

A trip with no precomputed legs starts but cannot move, and logs a warning naming the
`precompute-route` endpoint — it is a legitimate state, not an error.

### Manual verification

```properties
getyourride.tracking.simulation.enabled=true
getyourride.tracking.simulation.speed-multiplier=60   # optional: watch it quickly
```

```bash
curl -X POST -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/trips/{id}/precompute-route     # legs must exist first
curl -X PATCH -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/trips/{id}/status?status=IN_PROGRESS"
```

```sql
-- Rows accumulate every tick
SELECT COUNT(*), MAX(recorded_at) FROM trip_location_history WHERE trip_id = ?;
-- Cursor advances, dwell_until is set at each stop
SELECT current_lat, current_lng, current_leg_index, current_point_index, dwell_until, status
FROM trip WHERE trip_id = ?;
-- Stops flip to ARRIVED in order
SELECT stop_order, stop_name, status FROM trip_stop WHERE trip_id = ? ORDER BY stop_order;
```

---

## 🛠 Business Logic (Service Layer)

- **TripService:** Manages the lifecycle of a trip. When a trip is retrieved, it now includes detailed vehicle information (Model, Colour, Capacity) in the `TripResponse` to help students identify their ride.
- **TripRouteService:** Resolves trips to coordinates for `GET /api/rides/{rideId}/route`, and precomputes per-leg ORS geometry into `trip_leg_route`. See the Routing section above.
- **TripSimulationService:** Advances in-progress trips along their leg polylines, records positions, and drives stop-arrival and completion. Triggered by `scheduler.TripSimulationScheduler`. See the Trip Simulation Engine section above.
- **TrackingBroadcastService:** The single seam onto the STOMP transport. See the Live Tracking Transport section above.
- **AuthServices:** Handle registration and login logic for both Students and Drivers, including JWT generation with custom claims (e.g., `isFunded` for students).

---

## 📝 Naming Conventions
- **Controllers:** `XxxController`
- **Services:** `XxxService` (Interface) / `XxxServiceImpl` (Implementation)
- **Repositories:** `XxxRepository`
- **DTOs:** `XxxRequest` / `XxxResponse`
