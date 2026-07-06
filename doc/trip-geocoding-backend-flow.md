# Trip Creation & Geocoding — Backend Flow (Spring Boot)

Scope: driver creates a trip (e.g. Bird Street Campus → North Campus) and lat/lon for both pickup and destination get persisted to `shuttle_db`.

---

## 1\. Overview

Two entry points into the backend, corresponding to two moments in the UI:

| UI moment | Endpoint | Backend method |
| :---- | :---- | :---- |
| Driver typing an address | `GET /api/geocode/suggest?q=...` | `GeocodingService.suggest()` |
| Driver submits "Create Trip" form | `POST /api/trips` | `TripService.createTrip()` |

The suggest endpoint is called many times per trip (as the user types). The create-trip endpoint is called once. Coordinates should ideally travel with the suggestion the driver picked, so `createTrip()` normally does **not** need to call Nominatim again — it only does so as a fallback.

---

## 2\. Endpoint 1 — Autocomplete suggestions

**Controller**

@RestController

@RequestMapping("/api/geocode")

public class GeocodingController {

    private final GeocodingService geocodingService;

    public GeocodingController(GeocodingService geocodingService) {

        this.geocodingService \= geocodingService;

    }

    @GetMapping("/suggest")

    public List\<AddressSuggestion\> suggest(@RequestParam("q") String query) {

        return geocodingService.suggest(query);

    }

}

**Flow**

Client (debounced keystroke)

   │  GET /api/geocode/suggest?q=Bird+Street

   ▼

GeocodingController.suggest()

   │

   ▼

GeocodingService.suggest(partialQuery)

   │  \- guards: query.length() \< 3 → return \[\]

   │  \- builds Nominatim URL with viewbox \+ bounded=1 (NMU area bias)

   ▼

fetchResults(url)

   │  \- sets required User-Agent header

   │  \- RestTemplate.exchange(...) → Nominatim

   ▼

List\<AddressSuggestion\>  (displayName, lat, lon)

   │

   ▼

Returned to client as JSON

Each `AddressSuggestion` already carries `lat`/`lon`. The client should hold onto the **whole object** when the driver taps one, not just the display string — this is what avoids a second geocoding round-trip later.

---

## 3\. Endpoint 2 — Create trip

**Request DTO**

public class CreateTripRequest {

    private String pickupAddress;

    private Double pickupLat;      // nullable — present if picked from suggestions

    private Double pickupLon;

    private String destinationAddress;

    private Double destinationLat;

    private Double destinationLon;

    // getters/setters

}

**Controller**

@RestController

@RequestMapping("/api/trips")

public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {

        this.tripService \= tripService;

    }

    @PostMapping

    public ResponseEntity\<Trip\> createTrip(@RequestBody CreateTripRequest request) {

        Trip trip \= tripService.createTrip(request);

        return ResponseEntity.ok(trip);

    }

}

**Service — resolves coordinates, then persists**

@Service

public class TripService {

    private final GeocodingService geocodingService;

    private final TripRepository tripRepository;

    public TripService(GeocodingService geocodingService, TripRepository tripRepository) {

        this.geocodingService \= geocodingService;

        this.tripRepository \= tripRepository;

    }

    public Trip createTrip(CreateTripRequest request) {

        double\[\] pickup \= resolveCoords(

                request.getPickupLat(), request.getPickupLon(), request.getPickupAddress());

        double\[\] destination \= resolveCoords(

                request.getDestinationLat(), request.getDestinationLon(), request.getDestinationAddress());

        Trip trip \= new Trip();

        trip.setPickupAddress(request.getPickupAddress());

        trip.setPickupLat(pickup\[0\]);

        trip.setPickupLon(pickup\[1\]);

        trip.setDestinationAddress(request.getDestinationAddress());

        trip.setDestinationLat(destination\[0\]);

        trip.setDestinationLon(destination\[1\]);

        return tripRepository.save(trip);

    }

    private double\[\] resolveCoords(Double lat, Double lon, String address) {

        if (lat \!= null && lon \!= null) {

            return new double\[\]{lat, lon};

        }

        // Fallback: driver typed manually, never picked a suggestion

        GeocodeResponse geo \= geocodingService.geocode(address);

        if (\!geo.isFound()) {

            throw new AddressNotFoundException(address);

        }

        return new double\[\]{geo.getLat(), geo.getLon()};

    }

}

**Flow**

Client

   │  POST /api/trips  { pickupAddress, pickupLat, pickupLon,

   │                      destinationAddress, destinationLat, destinationLon }

   ▼

TripController.createTrip()

   │

   ▼

TripService.createTrip(request)

   │

   ├── resolveCoords(pickup)

   │      ├── coords present → use as-is (no network call)

   │      └── coords null   → geocodingService.geocode(address) → Nominatim fallback

   │

   ├── resolveCoords(destination)   (same logic)

   │

   ▼

Trip entity populated (address \+ lat \+ lon for both points)

   │

   ▼

TripRepository.save(trip)   → INSERT INTO trips (...)

   │

   ▼

Trip returned to client (201/200 with saved entity, including generated id)

---

## 4\. Entity / table

@Entity

@Table(name \= "trips")

public class Trip {

    @Id

    @GeneratedValue(strategy \= GenerationType.IDENTITY)

    private Long id;

    private String pickupAddress;

    private double pickupLat;

    private double pickupLon;

    private String destinationAddress;

    private double destinationLat;

    private double destinationLon;

    // driverId, status, createdAt, etc.

}

ALTER TABLE trips

    ADD COLUMN pickup\_lat DOUBLE NOT NULL,

    ADD COLUMN pickup\_lon DOUBLE NOT NULL,

    ADD COLUMN destination\_lat DOUBLE NOT NULL,

    ADD COLUMN destination\_lon DOUBLE NOT NULL;

Use `DOUBLE` (or `DECIMAL(9,6)` for fixed precision) — `FLOAT` loses accuracy at map-zoom scale and can shift a pin by several meters.

---

## 5\. Error handling to add

| Failure point | Suggested handling |
| :---- | :---- |
| Nominatim unreachable / times out | Catch `RestClientException` in `fetchResults`, return empty list for `suggest`, throw a clear `GeocodingUnavailableException` for `geocode` |
| Address not found on fallback geocode | `AddressNotFoundException` → mapped to `400 Bad Request` via `@ControllerAdvice` |
| Nominatim rate-limit (429) | Backoff/retry once, or surface a "try again" error to the client |

---

## 6\. Optional optimization — known campus locations

Bird Street Campus, North Campus, etc. are fixed points that never move. Consider a small lookup table so these never hit Nominatim at all:

CREATE TABLE campus\_locations (

    id INT AUTO\_INCREMENT PRIMARY KEY,

    name VARCHAR(100) NOT NULL UNIQUE,

    lat DOUBLE NOT NULL,

    lon DOUBLE NOT NULL

);

`GeocodingService` (or a new `CampusLocationService`) checks this table first; only falls through to live Nominatim geocoding for addresses not in the list (e.g. student residences, off-campus pickups). This cuts external API calls and guarantees pin accuracy for the most common trip endpoints.  
