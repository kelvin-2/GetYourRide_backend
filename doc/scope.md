# GetYourRide Backend — Project Scope (for Kiro)


## Tech Stack (confirmed from `pom.xml`)

- Java 17, Spring Boot 3.5.16 (starter-parent)
- Spring Data JPA + Hibernate, MySQL (`mysql-connector-j`)
- Spring Security (stateless, JWT via JJWT 0.12.6)
- Lombok
- Cloudinary SDK (`cloudinary-http44` 1.36.0) — driver document uploads
- Maven / Maven Wrapper

## Package Structure

```
com.example1.getyourride
├── config/       CorsConfig, SecurityConfig, CloudinaryConfig
├── controller/    (see Implemented Endpoints below)
├── dto/request/, dto/response/
├── entity/       Student, Driver, DriverApplication, Vehicle, Trip, TripStop, ShuttleStop, ShuttleTimeSlot, Role, ShuttleRoute
├── exception/    GlobalExceptionHandler, BadRequestException, ResourceNotFoundException
├── repository/   (Spring Data JPA, one per entity)
├── security/     JwtAuthFilter, JwtUtil, CustomUserDetailsService, SecurityConstants
└── service/ (+ service/impl)
```

## Implemented Endpoints (confirmed via controller scan)

| Controller | Base path | Endpoints |
|---|---|---|
| `StudentAuthController` | `/api/auth/student` | `POST /register`, `POST /login` |
| `DriverAuthController` | `/api/auth/driver` | `POST /register`, `POST /login` |
| `DriverApplicationController` | `/api/driver-applications` | `POST /`, `POST /{applicationId}/documents`, `POST /{applicationId}/finalize` |
| `DriverProfileController` | `/api/driver-profile` | `GET /`, `POST /upload-document`, `DELETE /` |
| `VehicleController` | `/api/vehicles` | `POST /`, `GET /my`, `GET /` |
| `ShuttleStopController` | `/api/shuttle-stops` | `GET /`, `GET /time-slots` |
| `TripController` | `/api/trips` | `POST /`, `POST /{tripId}/book`, `GET /{id}`, `GET /`, `GET /status/{status}`, `GET /search`, `GET /my-trips`, `PATCH /{id}/status`, `PATCH /{id}/cancel`, `PATCH /{id}/complete`, `PATCH /{id}/schedule`, `POST /offer` |
| `TripStopController` | `/api/trips/{tripId}/stops` | `POST /`, `POST /student`, `GET /`, `DELETE /{stopId}` |
| `RouteController` | `/api/rides` | `GET /{rideId}/route` — **⚠️ hardcoded placeholder coordinates**, not yet wired to real trip data (see Known Gaps) |
| `GeocodingController` | `/api/geocode` | `POST /`, `GET /suggestions`, `GET /reverse` |
| `UserController` | — | present but currently empty/unused |

## Core Entities (confirmed from `entity/`)

- **Student** — studentId, studentNumber, name, email, phone, password, `isFunded`, createdAt
- **Driver** — driverId, name, email, phone, studentNumber, role, `isVerified`, joinDate, password, totalTrips
- **DriverApplication** — applicationId, driverId, contactNumber, vehicle make/model/registration/capacity/colour, license/registration file paths, `applicationStatus` (defaults `"Pending Review"`)
- **Vehicle** — vehicleId, driverId (FK), registrationNumber, model, vehicleYear, colour, capacity
- **Trip** — tripId, driver (FK), vehicle (FK), tripType, departure/destination stop + lat/lng, departureTime, arrivalTime, availableSeats, price, status, list of `TripStop`
- **TripStop** — id, trip (FK), latitude, longitude, stopName, stopOrder, student (FK) — this is the carpool passenger-stop join entity
- **ShuttleStop** / **ShuttleTimeSlot** — stopName, area, location, lat/lng; period (MORNING/AFTERNOON), departs/arrives

**Note:** there is no separate `Booking` or `Passenger` entity — a student is attached to a ride via `TripStop.student`. If a cleaner booking/passenger model is wanted, that's new work, not a rename of something existing.

## External Services (confirmed from `GeocodingService.java` and `application.properties`)

- **Geoapify** — primary and only geocoding provider in code today (autocomplete, search, reverse). Bounded to a Nelson Mandela Bay bounding box.
- **OpenRouteService (ORS)** — used for routing (`RouteService`), but only reachable through the still-placeholder `RouteController`.
- **Nominatim/OSM is not present in this codebase** — if that was used elsewhere (e.g. an earlier prototype), it isn't in this repo.
- **Cloudinary** — driver application document uploads (license, registration).

## Security

- Stateless JWT auth (`JwtAuthFilter` + `JwtUtil`), BCrypt password encoder configured — **but `Student.password` is documented in `doc/ARCHITECTURE.md` as intentionally stored in plain text "per project decision."** Confirm whether that's still the intended behavior before Kiro touches auth code; it reads as a real vulnerability, not a stylistic choice.
- `/api/auth/**`, `/api/driver-applications/**`, `/error` are public; `/api/admin/**` requires `ADMIN`/`STAFF` role; everything else requires authentication.
- No `/api/admin/**` controller currently exists — the security rule is future-proofed but unused.

## Known Gaps / In-Progress Work (from `doc/Task` and `doc/GetYourRide_Tracking_Documentation.md`)

There is already a **detailed phased build plan** for live trip tracking + route simulation, tracked in `doc/Task`:

| Phase | Branch | Status |
|---|---|---|
| 0 — DB cleanup & tracking schema (`trip_leg_route`, `trip_location_history`, new `trip` columns) | `step-0-db-cleanup` | ✅ Done |
| 1 — Fix the 0,0 coordinate validation bug | `step-1-validation-fixes` | Not confirmed done in code |
| 2 — Wire `RouteController` to real trip data + precompute `trip_leg_route` via ORS | `step-2-route-wiring` | **Not done** — confirmed hardcoded placeholders still in `RouteController` |
| 3 — Leg precompute | `step-3-leg-precompute` | Not started |
| 4 — WebSocket/STOMP infra | `step-4-websocket` | Not started |
| 5 — Simulation engine (scheduler) | `step-5-scheduler` | Not started |
| 6 — Android tracking screen | `step-6-android-tracking` | Not started (Android repo, out of scope here) |
| 7 — Polish | `step-7-polish` | Not started |

**Before starting new work, read `doc/Task` and `doc/GetYourRide_Tracking_Documentation.md` in full** — they contain acceptance criteria per phase and should be treated as the authoritative plan for tracking/simulation, not re-derived from scratch.

## Confirmed Security Issue — Not Just a Style Note

`src/main/resources/application.properties` is committed to the repo **with live credentials in plaintext**: MySQL root password, ORS API key, Geoapify API key, and full Cloudinary credentials (cloud name, API key, API secret). This is flagged in `doc/Task` itself ("still committed in plaintext"), so it's a known, unresolved issue — worth rotating these keys and moving to environment variables / a `.env` + `application-local.properties` (gitignored) before any more work builds on top of them, since anyone with repo access currently has them.

## Not Implemented (mentioned in earlier feature lists, absent from code)

- **Notifications** — no code, no dependency (no FCM/email library) found anywhere in `src/main`.
- **Passenger management** as a distinct concept — folded into `TripStop.student`.
- **Admin dashboard / reporting** — security rule exists, no controller/service backing it.
- **Live tracking / route simulation** — planned in detail (see above), not yet built.

## Out of Scope

- Android frontend (separate repository, Kotlin/Jetpack Compose)
- Payments (no payment code or dependency present)

---