# GetYourRide — Geocoding Migration to Google Places API

**Date:** August 2026
**File changed:** `GeocodingService.java`
**Reason:** Capstone demo — show Google Maps Platform integration using a free Maps Demo Key, while keeping the existing provider (Geoapify) as an automatic safety net in case the demo key fails live.

---

## 1. Summary

`GeocodingService` now tries **Google** first for every autocomplete, geocode, and reverse-geocode request. If Google fails for any reason (bad key, rate limit, no results, network error), it **silently falls back to Geoapify**, which was the original working implementation. No other files needed to change — controller, `AddressSuggestion` DTO, and the entire Android frontend are untouched, because the method signatures and response shape (`displayName`, `latitude`, `longitude`) stayed identical.

---

## 2. APIs used

### Google Places API (New) — Autocomplete
- **Endpoint:** `POST https://places.googleapis.com/v1/places:autocomplete`
- **Purpose:** Returns address/place predictions as the student types.
- **Auth:** `X-Goog-Api-Key` header (Maps Demo Key or a billing-enabled key)
- **Field mask used:** `suggestions.placePrediction.placeId,suggestions.placePrediction.text`
- **Important limitation:** This endpoint does **not** return coordinates — only a `placeId` and display text. Coordinates must be resolved separately via Place Details.
- **Request body example:**
  ```json
  {
    "input": "dolphin court",
    "locationBias": {
      "circle": {
        "center": { "latitude": -33.9608, "longitude": 25.6022 },
        "radius": 50000.0
      }
    },
    "includedRegionCodes": ["za"]
  }
  ```

### Google Places API (New) — Place Details
- **Endpoint:** `GET https://places.googleapis.com/v1/places/{placeId}`
- **Purpose:** Resolves a `placeId` (from Autocomplete) into actual coordinates + formatted address.
- **Auth:** `X-Goog-Api-Key` header
- **Field mask used:** `location,formattedAddress`
- **Why it's needed:** Called automatically, server-side, for every autocomplete suggestion returned, so the frontend still receives ready-to-use lat/lon exactly like before — no two-step flow was pushed onto the Android app.

### Google Geocoding API (classic)
- **Endpoint:** `GET https://maps.googleapis.com/maps/api/geocode/json`
- **Purpose:** Used for `geocode(address)` (address → coordinates) and `reverseGeocode(lat, lon)` (coordinates → address). Chosen over the new Places API for these two because it returns lat/lng directly in one call, with no session token or field mask needed.
- **Auth:** `key` query parameter
- **Params used:** `address`, `region=za`, `components=country:ZA` (geocode) / `latlng` (reverse)

### Geoapify (fallback provider — unchanged from before)
- `https://api.geoapify.com/v1/geocode/autocomplete`
- `https://api.geoapify.com/v1/geocode/search`
- `https://api.geoapify.com/v1/geocode/reverse`
- Kept exactly as originally implemented, used only when Google fails.

---

## 3. Behavior / fallback logic

| Method | Primary | Fallback trigger | Fallback |
|---|---|---|---|
| `suggest(query)` | Google Autocomplete + Place Details (resolved server-side) | Empty results, 401/403/429, network/parse error | Geoapify autocomplete |
| `geocode(address)` | Google Geocoding API | Not found / error, same address-variant ladder retried (Gqeberha↔Port Elizabeth, Bird Street, Summerstrand, simplified address) | Geoapify search, same ladder |
| `reverseGeocode(lat, lon)` | Google Geocoding API | No results / error | Geoapify reverse, final fallback = `"Current Location"` label |

The same **campus alias short-circuit** (South Campus, North Campus, Second Avenue Campus, Bird Street, Missionvale) and **address-variant fallback ladder** from the original Geoapify implementation were preserved and now apply to whichever provider is being tried.

---

## 4. Configuration required

`application.properties`:
```properties
google.maps.api.key=YOUR_DEMO_KEY
geoapify.api.key=YOUR_EXISTING_GEOAPIFY_KEY
```

Both keys are required — Google is primary, Geoapify is the fallback and needs to stay configured.

---

## 5. Logging added for debugging

Console logs added at each network call to make failures easy to diagnose live during a demo:

- **Outgoing request:** `Google Places Autocomplete REQUEST body={...}` — logs exactly what's sent to Google before the call.
- **Success:** `Google Places Autocomplete resolved=N query=...` — confirms how many suggestions Google returned and were successfully resolved to coordinates.
- **Failure with reason:** on HTTP errors, the actual Google error body is now logged (not just a status code), e.g.:
  ```
  Google Places FORBIDDEN (403) — query=dolphin — body={"error":{"code":403,"message":"This API key is not authorized to use this service or API.","status":"PERMISSION_DENIED"}}
  ```
  Specific handling for `401` (bad/missing key), `403` (API not enabled / key restricted — most likely issue with a Demo Key used server-side), and `429` (rate limited).
- **Fallback triggered:** `Google suggestions empty/failed — falling back to Geoapify for: ...` — shows exactly when and why the app dropped to the backup provider.
- Geoapify's original logging (`LocationIQ`/`Geoapify status=...`, rate-limit and auth failures) was preserved as-is in the fallback path.

---

## 6. Known limitation

Google's **Maps Demo Key** is primarily intended for browser-based JS/Places UI Kit usage. It is not officially confirmed to be supported for server-side REST calls (Autocomplete New, Place Details, Geocoding API) the way a normal billing-enabled key is — it happened to work in testing (see resolved=5 logs above), but this should be treated as unofficial/best-effort for demo purposes. If it stops working, the code falls back to Geoapify automatically with no visible failure to the user.
