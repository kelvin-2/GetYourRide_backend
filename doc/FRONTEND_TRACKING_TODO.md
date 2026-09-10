# Frontend work needed for trip tracking

Backend scope is complete and verified end-to-end against `shuttle_db`. This document lists what
the Android app (`kelvin-2/GetYourRide`) needs to show a simulated trip moving.

## The model we settled on

This is a capstone simulation, not a production tracking system, so the design is deliberately
simple:

- **The driver starts the trip.** The driver changes the trip's status to `IN_PROGRESS` from their
  own screen. That one action precomputes the route and starts the simulation — the vehicle begins
  moving on the server.
- **The student watches by polling.** When the student taps "Track", the app repeatedly calls
  `GET /api/trips/{id}` on a timer and redraws the marker from `currentLat`/`currentLng`. The
  student does **not** start anything.
- **No WebSocket / STOMP on the app.** There is no live socket. The earlier plan to consume STOMP
  is dropped for the capstone. The backend still publishes STOMP messages, but the app ignores
  them; polling is the whole client story. `StompRideLocationSocket` and `MockRideLocationSocket`
  can be left unused or deleted.

Everything below serves that model. Every item was found by reading the app against the running
backend, not guessed at.

---

## Summary

| # | Item | Whose screen | Severity |
|---|------|--------------|----------|
| 1 | Driver "Start trip" action calling `POST /api/trips/{id}/start` | Driver | **Blocking** |
| 2 | Read `currentLat`/`currentLng`/`currentLegIndex` from `TripResponse` | Student | **Blocking** |
| 3 | Poll `GET /api/trips/{id}` on a timer while tracking; stop on COMPLETED | Student | **Blocking** |
| 4 | Trip selection should prefer the running trip, and compares dates as strings | Student | High |
| 5 | Read stop `status` so passed stops render as visited | Student | High |
| 6 | Status vocabulary disagrees with the backend | Both | Medium |
| 7 | Drop the STOMP socket path (no live tracking) | Student | Low |
| 8 | `etaMinutes` is always null but the UI has an ETA card | Student | Low |

---

## 1. Driver "Start trip" action — blocking

The driver's home screen (`DriverHomeViewModel` / `StudentDriverHomeScreen`) lists active rides and
can cancel one, but has no way to *start* one. Starting is what makes the vehicle move, so without
this nothing on the student side ever animates.

**Use the single start endpoint** — it precomputes the route and sets `IN_PROGRESS` in one call, so
the driver cannot accidentally start a trip that has no route:

```kotlin
// TripApi.kt
@POST("api/trips/{id}/start")
suspend fun startTrip(
    @Path("id") tripId: Long,
    @Query("recomputeRoute") recomputeRoute: Boolean = false
): Response<TripResponse>
```

```kotlin
// TripRepository.kt
suspend fun startTrip(tripId: Long): Result<TripResponse> = try {
    val response = api.startTrip(tripId)
    if (response.isSuccessful && response.body() != null) Result.success(response.body()!!)
    else Result.failure(Exception("Failed to start trip: ${response.code()}"))
} catch (e: Exception) { Result.failure(e) }
```

```kotlin
// DriverHomeViewModel.kt — mirror the existing cancelRide pattern
fun startRide(tripId: Long) {
    viewModelScope.launch {
        tripRepository.startTrip(tripId).onSuccess { loadMyTrips() }
            .onFailure { /* surface a message; the trip stays SCHEDULED on failure */ }
    }
}
```

Add a "Start trip" button to each `SCHEDULED` ride card in the driver's list, calling
`startRide(trip.tripId)`. After it succeeds the trip shows as `IN_PROGRESS`.

Do **not** use the older `PATCH /api/trips/{id}/status?status=IN_PROGRESS` for this. It also starts
tracking, but it does *not* precompute the route, so a trip that was never routed goes `IN_PROGRESS`
and then sits still. `/start` is the safe path.

> Why the driver and not the student: the student tapping "Track" only ever *watches*. Starting is
> the driver's action, which matches how a real trip begins and keeps the student screen read-only.

---

## 2. Read the live position from `TripResponse` — blocking

`TripResponse` now carries the vehicle's position. Add the fields to `TripDtos.kt`:

```kotlin
data class TripResponse(
    ...
    val currentLat: Double?,        // vehicle position; null until the trip is started
    val currentLng: Double?,
    val currentLegIndex: Int?,      // which leg the vehicle is on
    ...
)
```

Then in `TrackingViewModel.toTrackingData()`, seed the marker from them:

```kotlin
driverLocation = if (currentLat != null && currentLng != null) GeoPoint(currentLat, currentLng)
                 else null,
```

Verified live: `GET /api/trips/559` mid-journey returned
`currentLat=-33.975451, currentLng=25.640794, currentLegIndex=3`.

---

## 3. Poll the trip while tracking — blocking

This replaces the socket. `TrackingViewModel` already has a polling method
(`refreshTripDetails`) but only runs it when the socket drops. With no socket, poll unconditionally
while the tracking screen is open.

```kotlin
// TrackingViewModel — start this once the trip to track is resolved
private fun startPolling(tripId: Long) {
    pollingJob?.cancel()
    pollingJob = viewModelScope.launch {
        while (isActive) {
            tripRepository.getTripById(tripId).onSuccess { trip ->
                updateActive { trip.toTrackingData() }        // redraws marker + stop states
                if (trip.status.equals("COMPLETED", true)) cancel()  // stop polling when it ends
            }
            delay(4_000)   // matches the server tick; 3-5s is fine
        }
    }
}
```

- 4 seconds matches the server's tick interval, so the marker moves about once per poll.
- Stop polling on `COMPLETED` (and on `CANCELLED`) so it does not run forever.
- `onCleared()` must cancel `pollingJob` — it already cancels the socket, add the job there too.

Because the driver started the trip and the marker animates between polled points (the screen
already interpolates over ~1s), the student sees smooth movement without any socket.

---

## 4. Trip selection ignores which trip is actually running

`TripRepository.getActiveTrackableTrip()` picks the CONFIRMED booking with the soonest
`departureTime`:

```kotlin
.filter { it.status.uppercase() in TRACKABLE_TRIP_STATUSES }
.minByOrNull { it.departureTime }
```

Two problems: it ignores whether a trip is actually `IN_PROGRESS`, and it sorts `departureTime` as a
`String`. So a trip the driver just started can lose to an older `SCHEDULED` one, and the student
taps Track only to watch the wrong trip.

This is why `doc/05_carpool_tracking_seed.sql` had to give its trips earlier departure times than
the student's other bookings — it is working around this bug. Prefer a running trip:

```kotlin
bookings.map { it.trip }
    .filter { it.status.uppercase() in TRACKABLE_TRIP_STATUSES }
    .minWithOrNull(
        compareBy<TripResponse> { if (it.status.equals("IN_PROGRESS", true)) 0 else 1 }
            .thenBy { it.departureTime }   // ISO-8601 sorts correctly as a string
    )
```

---

## 5. Read stop `status` so passed stops render as visited

The backend returns `status` (`PENDING`/`ARRIVED`) on each stop. `TripStopResponse` in
`TripDtos.kt` does not declare it, so Gson drops it and every stop renders as still-to-come — even
ones the vehicle already passed. With polling this matters more, since each poll carries fresh stop
states.

```kotlin
data class TripStopResponse(
    val id: Long,
    val stopName: String,
    val latitude: Double,
    val longitude: Double,
    val stopOrder: Int,
    val status: String,          // "PENDING" | "ARRIVED"
    val studentId: Long?,
    val studentName: String?
)
```

```kotlin
// in toTrackingData()
currentStopIndex = stops.count { it.status.equals("ARRIVED", true) },
```

Note: the destination is not a stop row, so arrival there shows up as the trip becoming
`COMPLETED`, not as a stop flipping to `ARRIVED` (see item 6).

---

## 6. Status vocabulary disagrees with the backend

The backend `trip.status` is now a DB ENUM, so the set is closed:

```
SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
```

`TrackingViewModel.toTrackingData()` maps `"ARRIVED"` (never emitted) and has no case for
`CONFIRMED` or `COMPLETED`, so a finished trip currently reads as `ON_THE_WAY`. Combined with item 3
stopping the poll on `COMPLETED`, you want the screen to actually show the arrived state:

```kotlin
status = when (status.uppercase()) {
    "SCHEDULED", "CONFIRMED" -> RideStatus.ON_THE_WAY
    "IN_PROGRESS"            -> RideStatus.IN_TRANSIT
    "COMPLETED"              -> RideStatus.ARRIVED
    "CANCELLED"              -> RideStatus.CANCELLED
    else                     -> RideStatus.ON_THE_WAY
}
```

Also drop `"ARRIVED"` from `TripRepository.TRACKABLE_TRIP_STATUSES` — it can never match a backend
status.

---

## 7. Drop the STOMP socket path

`StompRideLocationSocket`, `MockRideLocationSocket`, the `RideLocationSocket` interface, and the
`socket` parameter on `TrackingViewModel`/`TrackingViewModelFactory` are all unused under the
polling model. Simplest is to delete them and remove the `socket` constructor param, so
`TrackingViewModel` depends only on `TripRepository`. Leaving them in place is harmless but
misleading — someone will wonder which path is live. The krossbow STOMP dependency in
`app/build.gradle.kts` can come out too.

---

## 8. `etaMinutes` is always null

`TrackingScreen` shows an ETA card that always reads `--`, because `toTrackingData()` hardcodes
`etaMinutes = null`. Optional for the capstone. If you want a real value, `GET /api/trips/{id}/legs`
returns `durationSeconds` per leg and `currentLegIndex` says which leg you are on, so the remaining
legs' durations sum to an ETA — but note the simulation compresses time roughly 10x, so the number
would describe simulated time, not real driving time. Decide which you want before wiring it, or
leave the card showing `--`.

---

## How to test the whole flow

Three carpool trips are seeded (`doc/05_carpool_tracking_seed.sql`), all booked (CONFIRMED) by
`test@mandela.ac.za`:

| Trip | Route | Stops |
|------|-------|-------|
| 558 | Newton Park → South Campus | 3 |
| 559 | South Campus → North Campus | 2 |
| 560 | Newton Park → Missionvale Campus | 0 |

Requires `getyourride.tracking.simulation.enabled=true` (now the default).

**Driver side:** open a trip and hit "Start trip" (item 1) → it calls `POST /api/trips/558/start` →
the trip goes `IN_PROGRESS` and the vehicle starts moving on the server.

**Student side:** tap "Track" → the app polls `GET /api/trips/558` every ~4s and animates the marker
along `currentLat`/`currentLng` until the trip reaches `COMPLETED`.

You can prove the backend half without the app at all:

```
POST  /api/trips/558/start            # driver's action
GET   /api/trips/558                  # poll: currentLat/currentLng change, currentLegIndex steps 0->3
GET   /api/trips/558/stops            # stop status flips PENDING -> ARRIVED
```

Measured on trip 559: 206 seconds from `IN_PROGRESS` to `COMPLETED` across 4 legs, ~20s dwell at
each stop. Re-run a finished trip with `PATCH /api/trips/{id}/schedule` then start it again.

### Adding a stop before the trip starts

You mentioned students add a stop before the trip is confirmed/started. That path is safe: adding a
stop to a trip that has not been started yet does no routing at all (there are no legs to rebuild),
so it never calls ORS and never fails on that account — the stop is just saved. When the driver
later starts the trip, `POST /api/trips/{id}/start` builds the route across every stop, including
the one added at booking time. Student pickup stops are appended after existing stops; if you need a
pickup inserted at a specific position rather than at the end, say so — the backend appends today.

---

## Backend notes for awareness

- **Shuttle trips are not trackable yet.** All 19 `shuttle_stop` rows have NULL coordinates and the
  `route` table has no intermediate waypoints, so a shuttle trip has nothing to route through. This
  work is carpool-only by decision. Backfilling shuttle stop coordinates is separate work.
- **STOMP subscriptions are not authorised per-trip.** Irrelevant while the app polls and ignores
  the socket, but if a live socket is ever added, any authenticated user can currently subscribe to
  any trip's topic. Tracked as Phase 5 in `doc/Task`.
- **Secrets are still committed** in `application.properties` (DB password, ORS/Geoapify keys).
  Worth moving to environment variables before this goes public.
