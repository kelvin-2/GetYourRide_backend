### Trip Coordinate Saving Implementation

This document describes the changes made to the "Get Your Ride" application to support saving the latitude and longitude of trip addresses in the `trip` table.

#### 1. Database Schema Changes (Entity Update)
The `Trip` entity (`src/main/java/com/example1/getyourride/entity/Trip.java`) has been updated to include four new columns:
- `departure_lat` (Double): Latitude of the departure stop.
- `departure_lng` (Double): Longitude of the departure stop.
- `destination_lat` (Double): Latitude of the destination stop.
- `destination_lng` (Double): Longitude of the destination stop.

#### 2. DTO Updates
- **CreateTripRequest**: Now accepts optional `departureLat`, `departureLng`, `destinationLat`, and `destinationLng` fields.
- **TripResponse**: Now returns the coordinates for both departure and destination stops.

#### 3. Service Layer Logic
The `TripServiceImpl` has been enhanced to handle these coordinates during trip creation:
- **Automatic Geocoding**: If coordinates are not provided in the request, the service automatically uses `GeocodingService` (powered by Nominatim/OpenStreetMap) to fetch the latitude and longitude based on the provided address strings (`departureStop` and `destinationStop`).
- **Coordinate Persistence**: The fetched or provided coordinates are saved along with the trip details in the database.

#### 4. Usage in Route Calculation
These saved coordinates can now be used in `RouteController` to fetch accurate routes from OpenRouteService instead of using placeholders.

#### Implementation Details
- **File**: `TripServiceImpl.java`
- **Method**: `createTrip(CreateTripRequest request)`
- **Logic**:
  ```java
  if (request.getDepartureLat() == null) {
      GeocodeResponse res = geocodingService.geocode(request.getDepartureStop());
      if (res.isFound()) {
          trip.setDepartureLat(res.getLatitude());
          trip.setDepartureLng(res.getLongitude());
      }
  }
  ```
