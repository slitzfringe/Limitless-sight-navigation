# Quickstart & Validation Guide: AR Road Navigation POC

**Feature**: `001-ar-road-navigation-poc` | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

This guide covers how to build, run, replay, and validate the POC, and how evidence is labelled (Constitution VI).

## Current build: stationary bench milestone

The current app is a standalone Android debug build (Android 10+, SDK 35,
Gradle 8.11.1, JDK 17+). It does not need maps credentials, location, or network
access. The remaining sections describe the intended full POC, not features of
this build.

1. Build with `gradle :app:assembleDebug :app:lintDebug` (`ANDROID_HOME` must
   point to the SDK). Connect an unlocked phone, approve USB debugging, and
   confirm `adb devices` reports `device`.
2. From the workspace root, run `adb install -r app\build\outputs\apk\debug\app-debug.apk`.
   If HyperOS rejects the install via USB, review its developer options and
   any device prompt. Open **Limitless Sight** on the phone.
3. Keep the phone still and upright in landscape. Tap **Enable camera**,
   select each rear camera Android exposes, and run the four-second lens check
   for each, facing the same detailed scene in unchanged light. Record any
   failed stream. The app compensates for landscape orientation automatically;
   if an OEM or physical lens reports incompatible metadata, tap **Rotate preview
   90°** until the horizon is level. The detail score is scene dependent and is
   not geometric calibration. The bench preview uses the largest practical 4:3
   stream and fits the complete frame; black bars are expected when preserving
   the full field of view in the landscape preview area.
4. Open **Simulated route**, cycle through the manoeuvres, enable **Simulate
   low confidence**, and check that all road chevrons disappear while the top
   pointer remains. Restore confidence, then return to the camera. Never use
   the build to navigate or while controlling a vehicle.
5. Tap **Start live road detection** while stationary. The current bench slice
   uses the bundled quantized FFNet-78S-LowRes Cityscapes segmentation model on
   live camera frames. It draws simulated straight chevrons only when road-region
   confidence is sustained; these chevrons are not connected to a navigation
   provider. The model and its Qualcomm license are under
   `app/src/main/assets/models/ffnet_78s_lowres/`; evaluate dataset/model terms
   before any commercial release.
6. Run `powershell -NoProfile -File .\tools\Collect-Diagnostics.ps1` to copy
   the app's two rolling private logs into the ignored `diagnostics` folder.
   The app logs lifecycle, camera metadata/errors, lens check results and
   simulation changes; no frames, audio, or location are recorded. The logs
   survive app restarts but not data clearing or uninstalling. A bench check
   does not verify the road-overlay safety or accuracy gates.

---

## 1. Prerequisites

| Item | Version / note |
|------|---------------|
| Android Studio | Latest stable; Android SDK Platform for target SDK; NDK not required (unless a native preprocessing step is added) |
| JDK | 17 |
| Devices | 2 reference devices (1 mid-range, 1 flagship), Android 10+, with ≥ 2 rear lenses on at least one device. Developer options + USB debugging enabled |
| Mounts | 1 handlebar phone mount (2W), 1 dashboard/windshield mount (car). Both landscape, vibration-damped preferred |
| Google Cloud | Project with Navigation SDK + Places API (New) enabled. Billing account linked (needed to unlock the free monthly caps). **Before first use**: set per-API daily quotas below the free caps, plus a budget alert at ₹1. API key restricted to package + SHA-1 |
| OSM fallback (free) | Docker on a dev machine: GraphHopper (or Valhalla) + Photon, loaded with the Southern-Zone/Karnataka OSM extract from Geofabrik. Reachable from test phones over LAN/VPN |
| Python | 3.11 for `ml/` (training/eval) |

## 2. Configure secrets (never commit)

`local.properties` (git-ignored):

```
MAPS_API_KEY=<restricted key>
OSM_ROUTING_URL=http://<lan-host>:8989     # GraphHopper (or Valhalla :8002)
OSM_GEOCODER_URL=http://<lan-host>:2322    # Photon
NAV_PROVIDER=google                        # google | osm
```

## 3. Build & run

```powershell
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew test                      # JVM unit tests (geometry, guidance, postprocess, nav:api contract vs fake)
./gradlew :app:connectedDebugAndroidTest   # instrumented UI + camera enumeration
./gradlew :benchmark:connectedCheck        # startup + frame timing
```

First-run walk-through (stationary, vehicle parked safely):
1. Accept the safety notice and provider terms → grant camera + location.
2. Choose vehicle (two-wheeler/car) and mount → mount the phone in **landscape**.
3. Camera picker → tap each lens to preview → "Test all cameras" → accept the recommendation.
4. Search a destination (e.g., "Cubbon Park") → choose a route → Start.
5. Verify the HUD (pointer, distance, speed hidden while stationary) and chevrons on the visible road.

## 4. Developer mode tools

| Tool | How | Purpose |
|------|-----|---------|
| Diagnostics overlay | Settings → Developer → Diagnostics | FPS, inference ms, confidence, mode, thermal level, mask preview |
| Trip recording | Developer → Record trip (indicator shown) | Builds replay/eval data (FR-101) |
| Replay mode | Developer → Replay → choose TripLog | Deterministic pipeline runs (FR-100) |
| Fault injection | Replay → Inject: perception loss / GPS loss / off-route / thermal L3 / camera stall | SC-006 |

## 5. Evaluation datasets (versioned, frozen per milestone)

| Set | Content | Labels | Used for |
|-----|---------|--------|----------|
| `blr-seg-eval-v1` | ≥ 2,000 frames from ≥ 30 trips; coverage matrix (research R-15) | Segmentation classes + ego-carriageway polygon | SC-001, model selection |
| `blr-replay-v1` | ≥ 40 TripLogs (5–15 min) incl. listed junctions | Manoeuvre timestamps, correct-branch flags, lane counts | SC-002/003/006/009/010 |
| `blr-signal-v1` | ≥ 500 signal approach sequences (day/night) | Per-movement state per frame, free-left flags | SC-008 |

Faces and number plates are blurred before frames leave the controlled storage.

## 6. Bengaluru evaluation routes (initial set; extend as needed)

| ID | Route / location | Exercises |
|----|------------------|-----------|
| BLR-01 | Hebbal flyover loops (Bellary Rd ↔ ORR) | Flyover vs service road vs ramp split (SC-003) |
| BLR-02 | Silk Board junction & elevated section toward Electronic City | Multi-level split, heavy mixed traffic, signals |
| BLR-03 | Electronic City elevated expressway vs Hosur Road below | Parallel stacked roads |
| BLR-04 | Outer Ring Road (Marathahalli–Bellandur) incl. service roads | Multi-lane lane-change pre-guidance (US6), U-turn openings |
| BLR-05 | KR Puram (Tin Factory/ hanging bridge area) | Complex junction, Metro work zones |
| BLR-06 | Mekhri Circle underpass | Underpass vs surface road, GPS degradation |
| BLR-07 | Indiranagar/Koramangala residential grid | Narrow undivided roads (FULL_NARROW), footpath edges, animals |
| BLR-08 | MG Road / Trinity Circle area | Signals with arrows/countdowns, roundabout-like circle |
| BLR-09 | Any route at night + any route in rain | Low-light/rain degradation (E2/E3) |

## 7. Supervised field-test protocol (mandatory)

1. **Roles**: rider/driver (never touches the phone while moving), **passenger/safety operator** (operates the app, notes events), optional follow vehicle with camera.
2. **Pre-check**: mount secure, calibration PASS, recording ON, battery > 60%, legal compliance (licence, helmet, documents), weather acceptable.
3. **Conduct**: obey all traffic rules regardless of app guidance. Abort the test if the app shows wrong-branch or footpath guidance repeatedly. Never test during peak-risk conditions without a plan.
4. **Logging**: the operator tags events (wrong junction, leakage, fallback, reroute) by voice note or button press in dev mode.
5. **Post-trip**: export TripLog, label manoeuvres, compute metrics, file defects linked to FR/SC IDs.

## 8. Requirement → evidence map

| Criterion | Kind | Method | Environment | Evidence location |
|-----------|------|--------|-------------|------------------|
| SC-001 leakage | Safety | Offline eval + replay overlay masks vs labelled ego polygon | Bench/replay | `evidence/sc-001/<date>/report.md` |
| SC-002a/b junction | Safety / Quality | Field runs ≥ 100 manoeuvres + replay | Supervised field | `evidence/sc-002/` |
| SC-003a/b parallel roads | Safety / Quality | BLR-01/02/03/06 approaches | Supervised field + replay | `evidence/sc-003/` |
| SC-004 glance | Target only | Study protocol + pilot (≥ 5 participants), video coding of glances | Simulator/closed road | `evidence/sc-004/` |
| SC-005a/b calibration | Quality | Moderated usability sessions, expert lens choice | Bench/parking | `evidence/sc-005/` |
| SC-006 fail-safe | Safety | Fault injection in replay | Replay | `evidence/sc-006/` |
| SC-007 performance & cost | Quality (+ hard NFR-011/014) | Macrobenchmark, Perfetto, batterystats, 30-min thermal soak (45-min for target), network inspection, Cloud billing report = ₹0 | Bench + field | `evidence/sc-007/` |
| SC-008 signal gate | Safety (only if US7 enabled) | Offline eval on `blr-signal-v1` | Bench | `evidence/sc-008/` |
| SC-009a/b lane cues | Safety / Quality | Replay on BLR-04 | Replay | `evidence/sc-009/` |
| SC-010 jitter | Quality | Replay straight segments | Replay | `evidence/sc-010/` |

Every evidence report states its label (**bench**, **replay**, **supervised field**, or **not verified**), the gate result (pass/fail), and the measured value against the target.

**Gate order**: safety gates (SC-001, SC-006, SC-009a, plus SC-002a/SC-003a in replay) pass in replay → closed-road run → supervised public-road runs → field safety and quality gates → POC accepted. Any safety-gate failure in the field stops public-road runs until a fix passes replay again.

**Free-cap check** (each test week): review Cloud Console usage per SKU against the free caps, and confirm the in-app usage counters match.

## 9. Disable / recovery

- Stretch features: toggle off in Settings → Developer → Feature flags.
- "AR off": Settings → Guidance → AR overlay OFF (HUD + fallback arrow only).
- Bad calibration: Settings → Camera → Recalibrate. Deleting the app removes all local data.
