# Funded Student Shuttle Booking

This document details how the system handles shuttle bookings for funded students, ensuring that only those with proper funding can access these rides.

## Overview

Shuttle trips are a premium service reserved for students who are marked as **funded** in the system. The backend enforces this through multiple layers:

1.  **Filtering in Search**: Non-funded students do not see shuttle trips in their search results.
2.  **Booking Validation**: The booking endpoint explicitly checks the `isFunded` status of the student before allowing a seat to be reserved.
3.  **Real-time Summary**: Upon successful booking, the student receives a summary of their confirmed shuttle trips and all available shuttle options.

## Technical Implementation

### 1. Linking Funded Students to Trips

When a booking is initiated at `POST /api/shuttle/book/{tripId}`:

- The system identifies the student using their authenticated email.
- It fetches the student's record and verifies the `isFunded` property.
- If the student is funded, a `Booking` entity is created, linking the `Student` and the `Trip`.
- The `availableSeats` on the `Trip` are decremented.
- The link is persisted in the `trip_booking` table.

### 2. Return Data to Frontend

Upon successful booking, the API returns a `ShuttleBookingSummaryResponse` containing:

- `bookingConfirmation`: Details of the newly created booking.
- `myConfirmedShuttles`: A list of all shuttle trips that this user has successfully booked.
- `allShuttleTrips`: A list of all available shuttle trips in the system, allowing the frontend to refresh the view immediately.

### 3. Viewing User Trips

Students can view all their booked trips (both Carpool and Shuttle) via:
`GET /api/trips/my`

This endpoint has been enhanced to support both Drivers (returning trips they are driving) and Students (returning trips they have booked).

## How to Test

You can use the provided `auth-requests.http` file to test these features. Ensure you are logged in as a student with `isFunded: true` to test the successful booking flow.
