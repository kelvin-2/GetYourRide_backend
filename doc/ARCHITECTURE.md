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

## 🛠 Business Logic (Service Layer)

- **TripService:** Manages the lifecycle of a trip. When a trip is retrieved, it now includes detailed vehicle information (Model, Colour, Capacity) in the `TripResponse` to help students identify their ride.
- **AuthServices:** Handle registration and login logic for both Students and Drivers, including JWT generation with custom claims (e.g., `isFunded` for students).

---

## 📝 Naming Conventions
- **Controllers:** `XxxController`
- **Services:** `XxxService` (Interface) / `XxxServiceImpl` (Implementation)
- **Repositories:** `XxxRepository`
- **DTOs:** `XxxRequest` / `XxxResponse`
