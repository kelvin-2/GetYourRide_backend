# GetYourRide — Backend & Live Trip Tracking Documentation

**Repo:** `kelvin-2/GetYourRide_backend`
**Stack:** Spring Boot, Spring Security (JWT, stateless), Hibernate/JPA (TiDB/MySQL), OpenRouteService (routing), Geoapify (geocoding)
**Server port:** `8080`

---

## 1. Authentication

Security is JWT-based and stateless (`SessionCreationPolicy.STATELESS`). Only `/api/auth/**` and `/error` are public — every other endpoint requires a valid JWT (`Authorization: Bearer <token>`), enforced by `JwtAuthFilter` ahead of Spring Security's own filter chain.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/driver/register` | Register a driver (student driver or shuttle driver) |
| POST | `/api/auth/driver/login` | Driver login → returns `AuthResponse` (JWT) |
| POST | `/api/auth/student/register` | Register a student |
| POST | `/api/auth/student/login` | Student login → returns `AuthResponse` (JWT) |

---

## 2. Existing API Reference

### 2.1 Trips — `/api/trips`

| Method | Endpoint | Body / Params | Returns |
|---|---|---|---|
| POST | `/api/trips` | `CreateTripRequest` | `TripResponse` — creates a trip |
| POST | `/api/trips/{tripId}/book` | `BookCarpoolRequest` | `TripResponse` — books a carpool seat with pickup/drop-off stops |
| GET | `/api/trips/{id}` | — | `TripResponse` |
| GET | `/api/trips` | — | `List<TripResponse>` — all trips |
| GET | `/api/trips/status/{status}` | — | `List<TripResponse>` filtered by status |
| PATCH | `/api/trips/{id}/status` | `?status=` (query param) | `TripResponse` — generic status update |
| PATCH | `/api/trips/{id}/cancel` | — | `TripResponse` — sets status to `CANCELLED` |
| PATCH | `/api/trips/{id}/complete` | — | `TripResponse` — sets status to `COMPLETED`, sets arrival time |
| PATCH | `/api/trips/{id}/schedule` | — | `TripResponse` — sets status to `SCHEDULED` |
| GET | `/api/trips/search` | `departure`, `destination` **or** `depLat/depLng/destLat/destLng/radius` | `List<TripResponse>` |

**`CreateTripRequest` body:**
```json
{
  "tripType": "SHUTTLE | STUDENT_DRIVER",
  "departureStop": "string",
  "destinationStop": "string",
  "departureLat": 0.0, "departureLng": 0.0,
  "destinationLat": 0.0, "destinationLng": 0.0,
  "departureTime": "2026-07-29T08:00:00",
  "availableSeats": 4,
  "price": 25.00,
  "stops": [ { "stopName": "string", "latitude": 0.0, "longitude": 0.0, "stopOrder": 1 } ]
}
```

**`TripResponse` body** includes trip, driver, vehicle, and stop details in one payload (`tripId`, `driverId`, `driverName`, `registrationNumber`, `vehicleModel`, `vehicleColour`, `vehicleCapacity`, `tripType`, `departureStop/Lat/Lng`, `destinationStop/Lat/Lng`, `departureTime`, `arrivalTime`, `availableSeats`, `price`, `status`, `stops: List<TripStopResponse>`).

### 2.2 Trip Stops — `/api/trips/{tripId}/stops`

| Method | Endpoint | Body | Returns |
|---|---|---|---|
| POST | `/api/trips/{tripId}/stops` | `TripStopRequest` | `TripStopResponse` — driver adds a generic stop |
| POST | `/api/trips/{tripId}/stops/student` | `TripStopRequest` | `TripStopResponse` — student-specific stop |
| GET | `/api/trips/{tripId}/stops` | — | `List<TripStopResponse>` |
| DELETE | `/api/trips/{tripId}/stops/{stopId}` | — | `204 No Content` |

**`TripStopRequest`:** `{ "stopName": "string", "latitude": 0.0, "longitude": 0.0, "stopOrder": 1 }`
**`TripStopResponse`:** `{ "id", "stopName", "latitude", "longitude", "stopOrder", "studentId", "studentName" }`

### 2.3 Route — `/api/rides/{rideId}/route`

| Method | Endpoint | Returns |
|---|---|---|
| GET | `/api/rides/{rideId}/route` | `RouteResponse` — `{ coordinates: [[lat,lng], ...], distanceMeters, durationSeconds }` from OpenRouteService |

> ✅ **Resolved in Phase 2.** The hardcoded placeholders (`-33.9581, 25.6014` → `-33.9615, 25.6089`) are
> gone. `RouteController` now delegates to `TripRouteService`, which reads the trip's real
> `departure_lat/lng` and `destination_lat/lng`. Behaviour changes worth knowing: a non-existent trip
> now returns `404` and a trip with missing or `0,0` coordinates returns `400`, where both previously
> returned the placeholder route as if nothing were wrong.

### 2.3a Trip legs — `/api/trips/{tripId}`

| Method | Endpoint | Returns |
|---|---|---|
| POST | `/api/trips/{tripId}/precompute-route` | `TripLegRouteResponse[]` — stores one ORS route per consecutive stop pair into `trip_leg_route`, replacing any existing legs |
| GET | `/api/trips/{tripId}/legs` | `TripLegRouteResponse[]` — previously precomputed legs in travel order |

`TripLegRouteResponse` summarises each leg (`legIndex`, `fromStopOrder`, `toStopOrder`, stop names,
`distanceMeters`, `durationSeconds`, `pointCount`, `startPoint`, `endPoint`) rather than returning the
full polyline, which would be hundreds of points per leg.

### 2.4 Geocoding — `/api/geocode`

| Method | Endpoint | Params/Body | Returns |
|---|---|---|---|
| POST | `/api/geocode` | `{ "address": "string" }` | `GeocodeResponse` — one-shot geocode, used right before saving a confirmed address |
| GET | `/api/geocode/suggestions` | `?query=` | `List<AddressSuggestion>` — autocomplete as the student types |
| GET | `/api/geocode/reverse` | `?lat=&lon=` | `AddressSuggestion` — turns a GPS fix into a readable address (same shape as suggestions, so it saves as a stop identically) |

### 2.5 Shuttle Stops — `/api/shuttle-stops`

| Method | Endpoint | Returns |
|---|---|---|
| GET | `/api/shuttle-stops` | `List<ShuttleStopResponse>` — all fixed pickup points |
| GET | `/api/shuttle-stops/time-slots` | `List<ShuttleTimeSlotResponse>` — departure/arrival slots |

### 2.6 Vehicles — `/api/vehicles`

| Method | Endpoint | Body | Returns |
|---|---|---|---|
| POST | `/api/vehicles` | `VehicleRequest` | `VehicleResponse` |
| GET | `/api/vehicles/my` | — | `List<VehicleResponse>` — current driver's vehicles |
| GET | `/api/vehicles` | — | `List<VehicleResponse>` — all vehicles |

---

## 3. Database Schema Relevant to Tracking

### Existing (already in `shuttle_db`)

- **`trip`** — `trip_id`, `driver_id`, `trip_type`, `departure_stop`, `departure_lat/lng`, `destination_stop`, `destination_lat/lng`, `departure_time`, `arrival_time`, `available_seats`, `price`, `status`.
- **`trip_stop`** — `id`, `trip_id`, `latitude`, `longitude`, `stop_name`, `stop_order`, `student_id`, `status` — ordered, per-trip waypoints. This is the foundation for leg-based simulation. `status` is `ENUM('PENDING','ARRIVED')`, added in Phase 4 by `doc/02_trip_stop_status.sql`; it was absent from the original schema, which blocked the simulator's stop-arrival step.

### Migration applied (cleanup + tracking columns)

Run via `01_cleanup_and_simulation_schema.sql`:

- Deduplicated 7 identical stop rows on trip 24.
- Corrected 5 stops that had `0,0` placeholder coordinates or geographically nonsensical locations, replaced with verified real Gqeberha coordinates.
- Standardized `trip.status` casing and locked it to an `ENUM('SCHEDULED','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED')`.
- Added to `trip`: `current_lat`, `current_lng`, `current_leg_index`, `current_point_index`, `dwell_until`.
- Added `trip_leg_route` — one row per leg (`from_stop_order` → `to_stop_order`) storing the ORS polyline (`route_geometry` JSON), `distance_meters`, `duration_seconds`.
- Added `trip_location_history` — breadcrumb trail, one row per simulated tick, for replay/debugging and drawing the traveled path on the map.

---

## 4. Live Tracking Simulation — Design

The shuttle has multiple ordered stops (`trip_stop`), so tracking is **leg-based**: the vehicle "moves" stop₁→stop₂→stop₃→...→stopₙ, one leg's road-following polyline at a time, rather than a single straight line for the whole trip.

### 4.1 Precompute routes when a trip is created/scheduled

For each pair of consecutive stops (ordered by `stop_order`), call ORS once and store the result:

```
for i in 0..stops.size-2:
    leg = ORS.getRoute(stops[i].coords, stops[i+1].coords)
    INSERT INTO trip_leg_route (trip_id, from_stop_order, to_stop_order, route_geometry, distance_meters, duration_seconds)
```

This must reuse the **same ORS client** currently wired into `RouteService` (used by `RouteController`) rather than duplicating the integration.

### 4.2 Fix `RouteController` first

Before building the simulator, `RouteController.getRoute()` needs to pull real coordinates from `TripRepository` instead of the hardcoded placeholders — this is a prerequisite, since the simulation reuses the same routing pathway. Uncomment the `TripRepository` wiring already sketched in the file's `TODO` comments.

### 4.3 Scheduled simulation engine

A new `@Scheduled` job (e.g. every 4 seconds) advances every trip with `status = 'IN_PROGRESS'`:

```java
@Scheduled(fixedRate = 4000)
public void tickAllActiveTrips() {
    List<Trip> activeTrips = tripRepository.findByStatus(TripStatus.IN_PROGRESS);
    for (Trip trip : activeTrips) advanceTrip(trip);
}

private void advanceTrip(Trip trip) {
    if (trip.getDwellUntil() != null && now().isBefore(trip.getDwellUntil())) return; // paused at a stop

    TripLegRoute leg = legRouteRepo.findByTripAndLeg(trip.getId(), trip.getCurrentLegIndex());
    List<double[]> points = parseGeometry(leg.getRouteGeometry());
    int nextIndex = trip.getCurrentPointIndex() + STEP_SIZE;

    if (nextIndex >= points.size() - 1) {
        handleStopArrival(trip); // marks stop ARRIVED, sets dwell_until, advances leg index
        return;
    }

    double[] point = points.get(nextIndex);
    trip.setCurrentLat(point[0]);
    trip.setCurrentLng(point[1]);
    trip.setCurrentPointIndex(nextIndex);
    tripRepository.save(trip);

    locationHistoryRepo.save(new TripLocationHistory(trip.getId(), point[0], point[1]));
    messagingTemplate.convertAndSend("/topic/trip/" + trip.getId(),
        new LocationUpdateDTO(trip.getId(), point[0], point[1], trip.getCurrentLegIndex()));
}
```

- **Speed control:** `STEP_SIZE` (points advanced per tick) controls how fast the shuttle appears to move. Can be computed dynamically per-leg from `distance_meters` / `duration_seconds` for realistic relative speed between legs, instead of one flat value.
- **Dwell time at stops:** on arrival, set `trip.dwell_until = now() + N seconds` (simulated boarding time) before moving to the next leg — keeps everything inside one scheduled method with no extra task infrastructure.
- **Trip completion:** when the vehicle reaches the final stop's leg end, set `trip.status = COMPLETED`.

### 4.4 Real-time transport

✅ **Implemented in Phase 3.** `spring-boot-starter-websocket` is now a dependency, `WebSocketConfig`
registers the STOMP endpoint at `/ws` with a simple in-memory broker on `/topic`, and
`TrackingBroadcastService` is the single publishing seam the Phase 4 simulator should call — it should
not hold a `SimpMessagingTemplate` itself.

Handshake authentication applies: clients send `Authorization: Bearer <token>` on the `/ws` handshake,
same as any REST call. ⚠️ Subscriptions are **not** authorised per-trip yet — any authenticated user
can subscribe to any trip's topic. Tracked against Phase 5 in `doc/Task`.

The two message shapes published on `/topic/trip/{tripId}`, implemented as `LocationUpdateDTO` and
`StopEventDTO` in `dto/message` and locked by `TrackingMessageContractTest`:

```json
// per-tick position update
{ "type": "LOCATION_UPDATE", "tripId": 42, "lat": -33.96, "lng": 25.61, "legIndex": 1 }

// stop lifecycle event
{ "type": "STOP_EVENT", "tripId": 42, "stopId": 7, "status": "ARRIVED" }
```

### 4.5 Android (`TrackingScreen`, OSMDroid)

- Subscribe to `/topic/trip/{tripId}` once tracking starts.
- On `LOCATION_UPDATE`: animate the marker between old/new lat-lng over ~1s (don't snap) so movement looks smooth between 4s ticks.
- On `STOP_EVENT`: update the stop list UI (arrived badge, etc.).
- On screen load: fetch `GET /api/trips/{tripId}/stops` and the trip's current position once via REST to draw initial state before socket updates arrive.

### 4.6 End-to-end flow for a simulated trip

1. `POST /api/trips` with `stops[]` → trip + stops created.
2. Backend precomputes and stores `trip_leg_route` rows for every consecutive stop pair (§4.1).
3. `PATCH /api/trips/{id}/status?status=IN_PROGRESS` (or a dedicated `/start` endpoint) → sets `current_leg_index=0`, `current_point_index=0`, `current_lat/lng` = first stop's coordinates.
4. Scheduler (§4.3) picks it up on the next tick and starts walking the polyline, broadcasting over STOMP (§4.4).
5. Android `TrackingScreen` renders it live (§4.5).

---

## 5. Outstanding Work Before This Is Fully Live

| # | Item | Status |
|---|---|---|
| 1 | DB cleanup (dedupe/fix coordinates, standardize status) | ✅ Done — `01_cleanup_and_simulation_schema.sql` |
| 2 | `trip` tracking columns + `trip_leg_route` / `trip_location_history` tables | ✅ Done — same migration |
| 3 | Wire `RouteController` to real trip coordinates (remove placeholders) | ✅ Done — Phase 2, via `TripRouteService` |
| 4 | Leg-route precomputation on trip creation | ✅ Done — Phase 2, but as `POST /api/trips/{id}/precompute-route` rather than inside `createTrip`, so an ORS outage cannot block ride posting |
| 5 | `@Scheduled` simulation engine | ✅ Done — Phase 4. Off unless `getyourride.tracking.simulation.enabled=true` |
| 6 | WebSocket/STOMP configuration + broadcasting | ✅ Done — Phase 3. ⚠️ Subscriptions not yet authorised per-trip (Phase 5) |
| 7 | Android `TrackingScreen` subscription + marker animation | ❌ Not started |

## 6. Security Note

`application.properties` currently has the TiDB database password, ORS API key, and Geoapify API key committed in plaintext to the repo. Worth moving these to environment variables or a secrets manager (e.g. `${DB_PASSWORD}` pulled from env) before this goes anywhere near a public repo or production — not part of the tracking work, but flagging it since it's sitting right next to the config you'll be touching anyway.
