# GetYourRide — Backend Architecture & File Structure

Spring Boot + MySQL backend for GetYourRide. This document is the source of truth for where things live. Update it whenever a new package or major file is added.

**Stack:** Java 17, Spring Boot 3.5.16, Maven, MySQL, JWT auth, plain-text passwords (no hashing — deliberate decision for this project).

---

## Root package

```
com.example1.getyourride
```

Everything below sits inside this package (i.e. inside `src/main/java/com/example1/getyourride/`).

---

## Full folder structure

```
com.example1.getyourride
│
├── GetYourRideApplication.java        ← main entry point (already exists)
│
├── config/
│   ├── SecurityConfig.java            ← Spring Security filter chain, auth rules
│   ├── CorsConfig.java                ← allows the Android app to call the API
│   └── WebSocketConfig.java           ← (future) config for live tracking sockets
│
├── controller/
│   ├── AuthController.java            ← /api/auth/register, /login, /logout
│   ├── UserController.java            ← /api/users/**
│   ├── TripController.java            ← /api/trips/**  (on-demand student-driver trips)
│   ├── ShuttleRouteController.java    ← /api/routes/**  (fixed shuttle routes)
│   └── TrackingController.java        ← (future) live location endpoints
│
├── dto/
│   ├── request/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── CreateTripRequest.java
│   │   └── LocationUpdateRequest.java ← (future) driver sends GPS ping
│   │
│   └── response/
│       ├── AuthResponse.java          ← returns JWT + basic user info
│       ├── UserResponse.java
│       ├── TripResponse.java
│       └── LocationResponse.java      ← (future) current location of a trip/driver
│
├── entity/
│   ├── User.java                      ← base user (email, plain-text password, role)
│   ├── Driver.java                    ← extends/relates to User, driver-specific fields
│   ├── ShuttleRoute.java              ← fixed route definition
│   ├── ShuttleStop.java               ← (future) stops along a route, if your ERD has them
│   ├── Trip.java                      ← on-demand trip between student & driver
│   ├── Role.java                      ← enum: STUDENT, STUDENT_DRIVER, SHUTTLE_DRIVER
│   └── LocationPing.java              ← (future) one GPS point in a trip's tracking history
│
├── repository/
│   ├── UserRepository.java
│   ├── DriverRepository.java
│   ├── TripRepository.java
│   ├── ShuttleRouteRepository.java
│   └── LocationPingRepository.java    ← (future)
│
├── service/
│   ├── AuthService.java               ← interface
│   ├── UserService.java               ← interface
│   ├── TripService.java               ← interface
│   ├── TrackingService.java           ← (future) interface
│   │
│   └── impl/
│       ├── AuthServiceImpl.java
│       ├── UserServiceImpl.java
│       ├── TripServiceImpl.java
│       └── TrackingServiceImpl.java   ← (future)
│
├── security/
│   ├── JwtUtil.java                   ← generate/validate/parse JWT tokens
│   ├── JwtAuthFilter.java             ← intercepts requests, checks token
│   └── CustomUserDetailsService.java  ← loads User for Spring Security
│
├── exception/
│   ├── GlobalExceptionHandler.java    ← @ControllerAdvice, turns exceptions into clean JSON errors
│   ├── ResourceNotFoundException.java
│   └── BadRequestException.java
│
└── util/
    └── (shared helpers, e.g. response wrapper classes)
```

---

## Layer responsibilities (quick reference)

| Layer | Job | Talks to |
|---|---|---|
| `controller` | Receives HTTP requests, validates input shape, returns responses. No business logic. | `service` |
| `service` | All business logic and rules live here. | `repository` |
| `repository` | Talks to the database via Spring Data JPA. No logic, just queries. | MySQL |
| `entity` | Maps directly to database tables. | — |
| `dto` | What actually goes over the wire to/from the Android app. Never expose `entity` directly. | `controller` ↔ `service` |
| `security` | Everything JWT and authentication-mechanics related. | `config`, `service` |
| `config` | Wires beans together (security rules, CORS, future WebSocket setup). | — |
| `exception` | Catches errors anywhere in the app and formats them consistently. | global |

---

## Naming conventions

- Entities: singular noun, e.g. `Trip`, not `Trips`.
- Repositories: `<Entity>Repository`, e.g. `TripRepository`.
- Services: interface `<Entity>Service` + impl `<Entity>ServiceImpl` in `service/impl/`.
- DTOs: `<Action><Entity>Request` / `<Entity>Response`, e.g. `CreateTripRequest`, `TripResponse`.
- Controllers: `<Entity>Controller`, base path `/api/<entity-plural>`.

---

## Live ride tracking — where it will plug in (future work)

Not building this yet, but the structure already has space reserved so it slots in cleanly later:

- **`entity/LocationPing.java`** — one row per GPS update (tripId, lat, lng, timestamp).
- **`repository/LocationPingRepository.java`** — fetch latest ping per trip, or history.
- **`service/TrackingService.java`** + **`impl/TrackingServiceImpl.java`** — business logic: save pings, compute "is driver near pickup", etc.
- **`controller/TrackingController.java`** — REST endpoints if using polling (e.g. `GET /api/trips/{id}/location`).
- **`config/WebSocketConfig.java`** — if you go the real-time push route instead of polling (recommended for true "live" tracking — Android subscribes to a topic per trip and gets pushed updates instead of polling every few seconds).

Two implementation paths to decide between when we get there:
1. **REST polling** — simpler, Android app calls `GET /location` every few seconds. Good enough for an MVP.
2. **WebSocket (STOMP over SockJS)** — true push updates, more complex but matches what students expect from "live tracking" (like Uber). Spring Boot supports this natively via `spring-boot-starter-websocket`.

Decision can wait until trips and auth are solid.

---

## Current build status

- [x] Maven project created, dependencies resolved (Spring Boot 3.5.16, Java 17)
- [x] Database connection config (`application.properties`) — pending verification
- [ ] Package structure created in IntelliJ
- [ ] `User` entity
- [ ] `UserRepository`
- [ ] `JwtUtil` + `JwtAuthFilter`
- [ ] `AuthController` — register/login/logout
- [ ] `SecurityConfig`
- [ ] Trip entity + endpoints
- [ ] Shuttle route entity + endpoints
- [ ] Live tracking (post-MVP)
