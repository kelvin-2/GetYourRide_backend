# GetYourRide - Feature & API Map

This document provides a detailed overview of the project's features, the external APIs it integrates with, and the location of key components in the codebase.

## 1. Project Features & Component Locations

### Authentication & User Management
*   **Student Auth**: Registration and login for students.
    *   `StudentAuthController.java`, `StudentAuthService.java`
*   **Driver Auth**: Registration and login for drivers.
    *   `DriverAuthController.java`, `DriverAuthService.java`
*   **JWT Security**: Token-based authentication and authorization.
    *   `SecurityConfig.java`, `JwtUtil.java` (in `security/` package)
*   **User Profiles**: Managing user-specific data.
    *   `UserController.java`, `UserServiceImpl.java`, `DriverProfileController.java`

### Driver Application & Onboarding
*   **Application Submission**: Potential drivers can apply by submitting documents.
    *   `DriverApplicationController.java`, `DriverApplicationService.java`
*   **Admin Review**: Administrators can approve or reject driver applications.
    *   `AdminDriverApplicationController.java`, `AdminDriverApplicationService.java`
*   **Document Upload**: Uses Cloudinary for storing driver documents (licenses, etc.).
    *   `CloudinaryConfig.java`

### Shuttle & Vehicle Management
*   **Vehicle Tracking**: Management of vehicles used for trips.
    *   `VehicleController.java`, `VehicleService.java`
*   **Shuttle Stops**: Definition and management of pickup/drop-off points.
    *   `ShuttleStopController.java`, `ShuttleStopService.java`
*   **Booking**: Students booking rides on available shuttles.
    *   `ShuttleBookingController.java`, `ShuttleBookingService.java`

### Trip Management & Real-time Tracking
*   **Trip Lifecycle**: Creation, start, and completion of trips.
    *   `TripController.java`, `TripService.java`
*   **Real-time Tracking**: Live updates of vehicle positions via WebSockets.
    *   `WebSocketConfig.java`, `TrackingBroadcastService.java`
*   **Trip Simulation**: A simulator that moves vehicles along routes when no real GPS feed is available.
    *   `TripSimulationService.java`, `SchedulingConfig.java`
*   **ETA Calculation**: Predictive arrival times for upcoming stops.
    *   `TripEtaController.java`, `TripEtaService.java`

### Routing & Geocoding
*   **Route Calculation**: Finding the best path between stops.
    *   `RouteController.java`, `RouteService.java`
*   **Geocoding/Reverse Geocoding**: Converting addresses to coordinates and vice versa.
    *   `GeocodingController.java`, `GeocodingService.java`
*   **Trip Leg Routes**: Storing precomputed polylines for trip segments.
    *   `TripLegRouteController.java`, `TripRouteService.java`

### Notifications & Reviews
*   **Notifications**: Sending alerts to users (e.g., trip started, arrival).
    *   `NotificationController.java`, `NotificationService.java`
*   **Trip Reviews**: Allowing students to rate and review their trips.
    *   `TripReviewController.java`, `TripReviewService.java`

---

## 2. External APIs Used

The project integrates several third-party services, configured primarily in `application.properties`.

| API | Purpose | Key in `application.properties` |
| :--- | :--- | :--- |
| **OpenRouteService (ORS)** | Route calculation, polylines, and durations for legs. | `ors.api.key` |
| **Geoapify** | Geocoding, address suggestions (autocomplete), and reverse geocoding. | `geoapify.api.key` |
| **LocationIQ** | Fallback geocoding service. | `locationiq.api.key` |
| **Google Maps** | Mapping services, place search, and geocoding. | `google.maps.api.key` |
| **Cloudinary** | Image and document storage for driver applications. | `cloudinary.cloud-name`, `cloudinary.api-key`, `cloudinary.api-secret` |

---

## 3. Project Directory Structure

```text
src/main/java/com/example1/getyourride/
├── config/           # Spring configuration (Security, WebSocket, Cloudinary, etc.)
├── controller/       # REST API Endpoints
├── dto/              # Data Transfer Objects (Request/Response models)
│   ├── message/      # WebSocket/STOMP broadcast messages
│   ├── request/      # Incoming request payloads
│   └── response/     # Outgoing response payloads
├── entity/           # JPA Entities (Database Tables)
├── exception/        # Custom Exception classes and Global Exception Handler
├── repository/       # Data Access Layer (Spring Data JPA)
├── scheduler/        # Background tasks (e.g., Trip Simulation ticks)
├── security/         # JWT and Security utility classes
├── service/          # Business Logic (Interfaces)
│   └── impl/         # Business Logic (Implementations)
└── validation/       # Custom validators for requests
```

### Key Data Files
*   `doc/`: Contains SQL migration scripts (`01_...sql` to `07_...sql`) and detailed technical guides.
*   `src/main/resources/application.properties`: The central configuration hub.
