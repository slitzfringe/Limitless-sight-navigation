# Contract: NavigationProvider

**Module**: `nav:api` (no vendor SDK dependencies) | **Implementations**: `nav:google` (primary, within free caps), `nav:osm` (free fallback: MapLibre Navigation + GraphHopper/Valhalla), `nav:fake` (tests/replay) | **Decorator**: `nav:usage` (free-cap guard)
**Covers**: FR-020…FR-025, FR-030, FR-052, FR-062, NFR-014, US2, US4, US6

## Interface (Kotlin, illustrative)

```kotlin
interface NavigationProvider {
    val id: ProviderId                         // GOOGLE, OSM, FAKE
    val capabilities: ProviderCapabilities     // see below

    /** Must be accepted before startGuidance; shows the vendor terms UI if the vendor requires it. */
    suspend fun ensureTermsAccepted(activity: Activity): TermsResult

    suspend fun autocomplete(query: String, bias: LocationBias, session: SearchSession): Result<List<Place>>
    suspend fun resolvePlace(placeId: String, session: SearchSession): Result<Place>

    suspend fun computeRoutes(origin: LatLng, destination: Place, mode: TravelMode): Result<List<Route>>
    suspend fun startGuidance(route: Route): Result<Unit>
    suspend fun stopGuidance()

    val progress: StateFlow<RouteProgress?>          // ≥ 1 Hz while guiding
    val events: SharedFlow<NavEvent>                 // OffRoute, Rerouted(route), Arrived, GpsLost, GpsRestored, Error
}

data class ProviderCapabilities(
    val twoWheelerRouting: Boolean,
    val laneGuidance: Boolean,
    val speedLimits: Boolean,
    val junctionViews: Boolean,      // vendor junction imagery (none in POC adapters)
    val liveTraffic: Boolean,        // Google: true; OSM: false
    val billable: Boolean,           // true → calls pass through the usage guard
    val headlessGuidance: Boolean,   // can guide without showing its own map UI
    val mapViewForMinimap: Boolean,
)

data class LocationBias(val center: LatLng, val radiusM: Int = 40_000, val regionCode: String = "IN")
```

## Behavioural rules

| # | Rule |
|---|------|
| N1 | `autocomplete` MUST bias to `LocationBias` (default centre: Bengaluru 12.9716 N, 77.5946 E, 40 km) and restrict to region `IN`. Uses a session token per search session to control billing. |
| N2 | `computeRoutes` MUST return the recommended route first. Traffic-aware. `TWO_WHEELER` falls back to `DRIVE` with `Route.travelMode = DRIVE` when unsupported. The UI shows the note. |
| N3 | `progress` MUST emit within 2 s of `startGuidance` and then ≥ 1 Hz. Each emission carries a monotonic timestamp. |
| N4 | When the vendor detects off-route, the adapter MUST emit `NavEvent.OffRoute`, then `Rerouted(route)` once a new route is active (rerouting is vendor-driven). |
| N5 | Adapters MUST map vendor manoeuvre enums onto `Maneuver` (data-model). Unmapped types → `UNKNOWN` (guidance shows pointer-only). |
| N6 | Lane data, when present, MUST be mapped to `LaneInfo` with `indexFromLeft` (lane 1 = left-most, left-hand traffic). |
| N7 | Adapters MUST NOT persist route geometry beyond the trip. Only `placeId` and titles may be stored (recents). |
| N8 | All network errors map to typed errors: `Offline`, `QuotaExceeded`, `FreeCapReached(sku)`, `NotAuthorized`, `NoRoute`, `VendorError(code)`. |
| N9 | Mock/test location sources are rejected unless `FeatureFlags.replayMode` is on (Edge E18). |
| N10 | Vendor attribution (e.g., "Google Maps" logo/text, "© OpenStreetMap contributors") MUST be shown where the terms or licence require it. |
| N11 | **Free-cap guard**: every billable call (`computeRoutes`/`startGuidance` destination, `autocomplete` request, `resolvePlace`) on a `billable` provider MUST go through `UsageGuard`. It increments a persisted per-SKU monthly counter and throws `FreeCapReached(sku)` at the configured limit (default 80% of the free cap). Limits are config, not code, and record the date the pricing page was checked. |
| N12 | Autocomplete MUST debounce (≥ 300 ms) and need ≥ 3 characters, so free Places usage isn't wasted. |
| N13 | On `FreeCapReached`, the app offers the `osm` provider (if configured) or shows "Monthly free navigation limit reached". It MUST never silently fall through to a billable call. |

## Google adapter notes (to verify against the pinned SDK version)
- Navigation SDK `Navigator` for guidance; turn-by-turn data feed (`NavInfo` → current/remaining `StepInfo`, lanes, distances) for our custom HUD; travel mode two-wheeler.
- Places SDK / Places API (New) Autocomplete with location bias and `includedRegionCodes = ["in"]`.
- The Navigation SDK and Maps SDK cannot both be in the app. Use the Navigation SDK's map APIs if a provider map is needed.
- Terms dialog via the SDK's terms API before first guidance.

## OSM fallback adapter notes (free)
- MapLibre Navigation Android (MIT) for progress, off-route detection and rerouting. Routes come from a self-hosted GraphHopper or Valhalla with the Karnataka/Southern-Zone OSM extract. Base URL is set in config.
- Place search: a self-hosted Photon/Nominatim instance on the same OSM extract. Public Nominatim usage policy forbids autocomplete-style use, so don't use the public server.
- No live traffic. Lane data only where OSM `turn:lanes` tags exist. `capabilities` reflect this.
- Two-wheeler: use a motorcycle/scooter profile if the engine supports it; otherwise `DRIVE` with the UI note (N2).

## Test obligations
- Contract test suite in `nav:api` run against `fake` (always), `osm` (against a local routing server), and `google` (instrumented, network, manual trigger only, counted against the free cap).
- `UsageGuard` unit tests: counter persistence, month rollover, limit reached → `FreeCapReached`, no billable call after the limit.
- Fake provider supports scripted scenarios: straight, left turn, fork/flyover, U-turn, off-route, GPS loss, arrival.
