# GetYourRide Backend File Structure Guide

This document explains the backend project structure in a simple way so new contributors can understand where to look when working on features.

## 1. Project overview

The backend is a Spring Boot application for the GetYourRide system. It handles:

- user authentication and authorization
- driver and student account flows
- shuttle routes and stops
- trip creation and management
- driver application submissions
- geocoding and route-related operations

The main Java package is:

- com.example1.getyourride

---

## 2. Main folder structure

```text
src/main/java/com/example1/getyourride/
├── config/
├── controller/
├── dto/
│   ├── request/
│   └── response/
├── entity/
├── exception/
├── repository/
├── security/
├── service/
│   └── impl/
└── GetYourRideApplication.java
```

### What each folder is for

#### config/
Contains Spring configuration classes.

Examples:
- SecurityConfig: defines security rules and authentication behavior
- CorsConfig: allows frontend/mobile apps to access the backend
- CloudinaryConfig: configures Cloudinary file uploads

#### controller/
Contains REST API controllers. These receive HTTP requests and return responses.

Examples:
- DriverAuthController
- StudentAuthController
- TripController
- RouteController
- VehicleController
- DriverApplicationController

#### dto/
Contains data transfer objects used for requests and responses.

- request/: incoming payloads from clients
- response/: outgoing payloads sent back to clients

Examples:
- DriverApplicationRequest
- AuthResponse
- TripResponse

#### entity/
Contains JPA entities that map to database tables.

Examples:
- Driver
- Student
- Trip
- ShuttleRoute
- ShuttleStop
- Vehicle
- DriverApplication

#### repository/
Contains Spring Data repositories for database access.

Examples:
- DriverRepository
- StudentRepository
- TripRepository
- VehicleRepository
- DriverApplicationRepository

#### security/
Contains authentication and JWT-related code.

Examples:
- JwtUtil
- Security filters and auth helpers

#### service/
Contains business logic. This is where most application rules live.

Examples:
- DriverApplicationService
- DriverAuthService
- StudentAuthService
- TripService
- RouteService

The impl/ folder contains concrete implementations of service logic.

#### exception/
Contains exception classes and global exception handling.

This is where application errors are centralized and returned in a consistent way.

---

## 3. How the backend is organized by layer

A Spring Boot backend is usually split into layers. This project follows that pattern:

| Layer | Responsibility |
|---|---|
| controller | Handles HTTP requests and responses |
| service | Contains business logic |
| repository | Talks to the database |
| entity | Represents database tables |
| dto | Defines request/response data shapes |
| config | Configures Spring and app behavior |
| security | Handles authentication and JWT |

---

## 4. Typical request flow

A normal request usually follows this path:

1. A client sends a request to a controller
2. The controller calls a service
3. The service uses repositories to read or write data
4. The result is returned as a DTO response

Example:

- Client calls a driver application endpoint
- Controller receives the request
- Service validates the data and creates related database records
- Repository saves the records
- Response is returned to the client

---

## 5. Where to look for common features

### Authentication
- controller: StudentAuthController, DriverAuthController
- service: StudentAuthService, DriverAuthService
- security: JwtUtil and security configuration

### Driver application flow
- controller: DriverApplicationController
- service: DriverApplicationService
- repository: DriverApplicationRepository, DriverRepository, VehicleRepository
- entity: DriverApplication, Driver, Vehicle

### Trips and routes
- controller: TripController, RouteController
- service: TripService, RouteService
- repository: TripRepository, ShuttleRouteRepository
- entity: Trip, ShuttleRoute, ShuttleStop

### File uploads
- service: Cloudinary-related logic in service classes
- config: CloudinaryConfig

---

## 6. Important files to know

- GetYourRideApplication.java: main Spring Boot application entry point
- application.properties: main app configuration, database, secrets, API keys
- pom.xml: Maven dependencies and build configuration

---

## 7. Good practice for new contributors

When adding a new feature, follow this pattern:

1. Create or update the entity if a new database table is needed
2. Add a repository for database access
3. Implement business logic in the service layer
4. Expose the feature in a controller
5. Use DTOs for request/response payloads
6. Keep controllers thin and services focused on logic

---

## 8. Summary

If you are new to this project, think of it like this:

- controllers receive requests
- services handle rules and logic
- repositories talk to the database
- entities represent data
- DTOs define what is sent to and from the client

This structure keeps the backend easier to maintain and extend.
