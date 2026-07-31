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
│   ├── request/        # Incoming request payloads
│   └── response/       # Outgoing response payloads
├── entity/             # JPA Entities (Database Models)
├── exception/          # Global Exception Handling
├── repository/         # Spring Data JPA Repositories (Data Access)
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
- `status` (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED)

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

## 🛠 Business Logic (Service Layer)

- **TripService:** Manages the lifecycle of a trip. When a trip is retrieved, it now includes detailed vehicle information (Model, Colour, Capacity) in the `TripResponse` to help students identify their ride.
- **AuthServices:** Handle registration and login logic for both Students and Drivers, including JWT generation with custom claims (e.g., `isFunded` for students).

---

## 📝 Naming Conventions
- **Controllers:** `XxxController`
- **Services:** `XxxService` (Interface) / `XxxServiceImpl` (Implementation)
- **Repositories:** `XxxRepository`
- **DTOs:** `XxxRequest` / `XxxResponse`
