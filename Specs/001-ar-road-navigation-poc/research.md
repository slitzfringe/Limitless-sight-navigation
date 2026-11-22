# Phase 0 Research: AR Road Navigation POC

**Feature**: `001-ar-road-navigation-poc` | **Date**: 2026-11-22 | **Spec**: [spec.md](./spec.md)

Each entry records **Decision**, **Rationale**, **Alternatives considered**, and **Open risks / verification needed**. Vendor capabilities and terms were checked on 2026-11-22 and must be rechecked against the pinned SDK version before integration.

---

## R-01 — Navigation & routing provider

**Question from product owner**: "Can we use the Google Maps already present on the phone, connected to the user's Google account?"

**Finding**: No. The consumer Google Maps app does not expose its live route, manoeuvres or progress to third-party apps. Intents can only *launch* Google Maps navigation in Google's own UI. Reading another app's screen or notifications to extract turn data would be unreliable, would break the provider's terms, and would be a privacy and security anti-pattern.

**Constraint (product owner, 2026-11-22)**: "go with the best free version". The POC must cost ₹0 in provider fees.

**Decision**: Use a **navigation SDK embedded in our app**, behind an internal `NavigationProvider` contract ([contracts/navigation-provider.md](./contracts/navigation-provider.md)), and stay free:

1. **Primary — Google Navigation SDK for Android + Places API (New), used only inside the free monthly caps.**
   - Free cap: **1,000 billable destinations per month** for the Navigation SDK (Google Maps Platform blog, "Making Navigation SDKs more accessible with new pricing", May 2025: "The usage cap will remain the same, with 1,000 billable events or destinations per month available at no cost"). Rerouting and in-route updates are not billed separately. Places autocomplete and place details also have free monthly usage per SKU.
   - POC usage estimate: about 10 testers × 3 trips/day × 22 days ≈ 660 destinations/month, under the cap. Search sessions are a small fraction of the Places caps.
   - **Staying at ₹0** (NFR-014):
     (a) Cloud Console per-API **daily quotas** sized so monthly totals stay below the free caps (for example, Navigation ≈ 30 destinations/day);
     (b) a **budget alert at ₹1**;
     (c) the in-app **usage guard** (FR-025), which stops at 80% of each cap;
     (d) replay and bench tests use `nav:fake` and never call Google.
   - Why it is the best free option: the best Bengaluru road, traffic and place data; **two-wheeler** routing; rerouting; lane data in the turn-by-turn feed (`StepInfo` lanes, recommended lanes); and a data feed built for custom HUDs.
   - Needs: a Google Cloud project with **billing enabled** (a card on file is needed to unlock the free caps; with the guards above it is never charged), a restricted API key, and an in-app Google terms dialog. No consumer Google account is needed.
2. **Free fallback — MapLibre Navigation Android (MIT) + OpenStreetMap.**
   - Routing from **GraphHopper or Valhalla** (both open-source), self-hosted on a developer machine or small free-tier VM with the Southern Zone / Karnataka OSM extract. GraphHopper's own example app shows MapLibre Navigation working with it.
   - Unlimited and free; no vendor terms; works when the Google caps are reached or billing can't be enabled.
   - Limits: no live traffic; OSM lane and turn tags in Bengaluru are patchy; two-wheeler profile depends on the routing engine's profiles. The AR design already copes with missing lane data (US6-4).
   - Base map not needed: the minimap is our own vector rendering (R-12).

**Rationale**: only a navigation SDK legitimately provides continuous route progress, off-route detection, rerouting, and step/lane data. Using a plain routing API (Google Routes API / Directions) and building turn-by-turn ourselves is restricted for real-time consumer navigation under Google's terms. Google's free cap comfortably covers a supervised POC and gives the best data. MapLibre + OSM guarantees we are never blocked by cost.

**Alternatives considered**:

| Option | Why not (for POC) |
|--------|-------------------|
| Consumer Google Maps app + intents | No data access; only launches Google's UI |
| Google Routes API + own turn-by-turn engine | Terms risk for real-time navigation; must build map-matching/rerouting |
| Mappls (MapmyIndia) Navigation SDK | Strong India data and junction views. But free-tier terms for navigation are not clearly published, and it needs a commercial onboarding. Kept as a post-POC option for flyover junction views |
| Mapbox Navigation SDK | Has a free tier, but weaker Bengaluru coverage/traffic than Google; proprietary licence; Mapbox Vision AR SDK discontinued |
| HERE SDK (Navigate edition) | Viable lane data; commercial licensing overhead |
| Ola Maps | No clearly documented public navigation SDK for third-party apps at the time of writing |

**Open risks / verification**:
- Free caps and SKUs change. Re-check the Google Maps Platform pricing page at integration time and set quotas to match. Record the checked date in the Decision log.
- Confirm that lane data appears for Bengaluru junctions in the SDK's data feed (coverage may be sparse). The design must not depend on it (US6 has fallbacks).
- Confirm that the SDK can run navigation **without** displaying its full map UI, or with a minimised map view, and the related terms on attribution.
- Caching: route coordinates are kept only for the active trip (Google's policy allows temporary lat/lng caching for Navigation SDK content within limits). Recent destinations store **place IDs** + user-visible labels only.
- OSM fallback: measure route quality on the BLR evaluation routes before relying on it for field runs.

---

## R-02 — Platform, language and UI toolkit

**Decision**: Native **Android**, **Kotlin** (JDK 17 toolchain), **Jetpack Compose** for non-AR screens (onboarding, camera picker, search, summaries), and a custom **OpenGL ES 3.1** surface for the AR view. Min SDK **29** (Android 10), target the latest stable SDK. ARM64-only native code.

**Rationale**: Bengaluru's market is predominantly Android. Camera2 exposes physical lenses and intrinsics, which iOS restricts differently. Native gives the lowest latency and smallest footprint, matching the "lightweight, highly efficient, fast" requirement. API 29 adds the thermal status API and covers a large share of in-use devices.

**Alternatives**: Flutter / React Native (extra camera-to-GPU copy and plugin limits for multi-camera Camera2 control); Unity (heavy APK, overkill); cross-platform C++ core (deferred to glass phase).

---

## R-03 — Camera access, lens enumeration, calibration inputs

**Decision**: Use **Camera2** directly (not CameraX) for the AR pipeline.

- Enumerate `CameraManager.cameraIdList` + `CameraCharacteristics.getPhysicalCameraIds()` (API 28+) for logical multi-cameras. Label lenses by `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` and `SENSOR_INFO_PHYSICAL_SIZE` → horizontal FOV. Shortest focal length = Ultra-wide.
- If physical streams can't be opened directly (OEM restriction), use the logical camera with `CONTROL_ZOOM_RATIO` (API 30+) < 1.0 to reach the ultra-wide. Record that lens switching is OEM-controlled (Edge E14) and warn the user.
- Intrinsics/distortion from `LENS_INTRINSIC_CALIBRATION` and `LENS_DISTORTION` (API 28+) when published. Otherwise estimate from focal length + sensor size, with distortion defaulting to zero plus an online check.
- Capture settings during navigation: fixed focus at infinity (`LENS_FOCUS_DISTANCE = 0`) where supported; AE target FPS range `[30,30]`; exposure-time cap (default ≤ 1/120 s by day) to limit motion blur; video stabilization **off** (EIS warps geometry and breaks the homography); OIS allowed but noted; noise reduction `FAST`; edge `FAST`.
- Two streams: (1) preview `SurfaceTexture` → GL (display), (2) `ImageReader` YUV at the processing resolution (default 640×360) → perception.

**Calibration test design** (per camera, ≈ 8–12 s each):

| Check | Method | Pass guide (default) |
|-------|--------|----------------------|
| Orientation & level | Gravity vector from `TYPE_GRAVITY`; roll and pitch of the device | Landscape, roll ±5°, pitch so the horizon sits within the top 25–45% of the frame |
| Vibration | Accelerometer RMS over 3 s (stationary, engine on) | Below threshold. Otherwise suggest a firmer mount |
| Sharpness | Variance of Laplacian on the lower half (road area), normalised by resolution | ≥ device-relative threshold |
| Noise | Std-dev of flat patches / ISO reported | ISO ≤ limit at the capped exposure |
| Frame rate | Measured sustained fps for 3 s at the processing size | ≥ 28 fps |
| Inference latency | Run the perception model on 30 frames of this lens | p95 ≤ budget (plan) |
| Road visibility | Drivable-area ratio in the expected lower region + horizon detection | Road present, horizon detected |
| Distortion | Published coefficients or straight-line test on lane/kerb edges | Undistortable within the shader budget |

**Score** = weighted sum: sharpness 20, fps 15, latency 15, road visibility 20, FOV suitability 15 (prefer 70–100° HFOV; ultra-wide penalised by distortion), noise 10, vibration 5. The lens with the best score is recommended. Ties favour the main camera (best ISP tuning).

**Rationale**: the user explicitly asked for lens choice plus calibration, because budget ultra-wide lenses are often soft. Objective scoring turns this into a guided choice.

**Open risks**: OEM camera quirks (Xiaomi, Samsung, Vivo, Oppo, Realme, OnePlus are common in Bengaluru) → maintain a device quirk table. Some devices refuse concurrent physical streams or report incorrect intrinsics.

---

## R-04 — On-device perception model(s)

**Decision**: one **multi-class lightweight semantic segmentation** network at 15–30 Hz. A **separate small traffic-light detector** runs at a lower rate only in the stretch feature.

Segmentation classes (POC): `road`, `footpath/sidewalk`, `kerb/median/divider`, `lane_marking`, `vehicle` (incl. 2W/auto/bus), `person`, `animal`, `barrier/cone`, `other`.

- Architecture candidates to benchmark: **TwinLiteNet-style** (drivable + lane, < 0.5 M params), **PP-LiteSeg-T**, **DeepLabV3+ MobileNetV3-Small**, **YOLOP** (multi-task, heavier). Pick the best accuracy at ≤ 15 ms GPU inference on the mid-range reference device at 512×288 or 640×360.
- Runtime: **LiteRT (TensorFlow Lite)** with the GPU delegate (FP16). NNAPI/vendor NPU delegates are only used where benchmarked and stable. INT8 quantisation if accuracy holds. Target model size ≤ 8 MB.
- Training data: **IDD (India Driving Dataset)**, which covers Indian roads including Bengaluru/Hyderabad and has classes for road, drivable fallback, sidewalk, curb, etc. Fine-tune on our own labelled **Bengaluru captures** (night, rain, flyovers, Metro work zones). BDD100K / Cityscapes / Mapillary only for pre-training where licences allow.
- Temporal stability: exponential moving average on logits, plus optical-flow-free mask warping using IMU rotation between inferences.

**Traffic-light detector (US7)**: detector on an upper-frame crop at ~5–10 Hz + crop classifier: {red, amber, green} × {disc, left, straight, right, U-turn} + flashing (temporal) + countdown digits (stretch). Candidates: EfficientDet-Lite0, an SSD-MobileNet variant, or a YOLOX-Nano class model (Apache-2.0).

**Licence gate**: avoid AGPL detector code and weights (e.g., Ultralytics YOLO) unless a commercial licence is bought. IDD, BDD100K and Mapillary have research/non-commercial or registration terms. Record them in a licence register and confirm commercial-use rights before release (Constitution "Licensing"). Our own captured dataset is the long-term solution.

**Alternatives**: separate lane, drivable and object models (more compute); cloud inference (violates Constitution II.1 and adds latency); classical CV lane detection (fails on faded Bengaluru markings).

---

## R-05 — How guidance is anchored to the real road (key architectural decision)

**Problem**: phone GNSS in Bengaluru is often 5–15 m off (urban canyons, flyovers, Metro viaducts). Heading is noisy at low speed. If we project the route polyline (lat/lng) straight into the camera, arrows land on the wrong junction or on the footpath.

**Decision — "perception-anchored, route-directed" hybrid**:

1. The **navigation provider** decides *what* to do and *when*: current step, manoeuvre type (left, right, slight, fork, ramp, U-turn, roundabout exit n), distance to manoeuvre, recommended lanes, off-route.
2. **Perception** decides *where* on the road: the ego corridor (own carriageway/lane), road edges, junction mouths/branches visible in the bird's-eye view (BEV).
3. The **guidance planner** fuses them in the vehicle frame:
   - **Cruise**: chevrons follow the ego-corridor centreline from perception.
   - **Approach** (distance-to-manoeuvre ≤ display distance): look for a matching opening in the BEV drivable mask on the manoeuvre side, near the predicted distance ± position uncertainty. On a match, build a smooth curve into it. Without a match, keep chevrons straight and show the manoeuvre via the top pointer and an edge arrow cue (Straight-only AR mode).
   - **Commit**: once the vehicle enters the curve, guidance tracks the curve and then the new ego corridor.
4. Route geometry (provider polyline) acts as a **prior**: the exit bearing for the curve and plausibility checks (route vs perceived junction agreement feeds the confidence score).

**Alternatives considered**:

| Option | Why not |
|--------|---------|
| Pure GPS/IMU world-anchored rendering | GNSS error → wrong junction, footpath leakage |
| ARCore Geospatial API / Streetscape Geometry (VPS) | Needs ARCore to own the camera (blocks the user's lens choice and ultra-wide), designed for handheld walking-speed tracking, VPS coverage in moving vehicles uncertain. **Revisit for the glass phase** |
| HD lane-level maps | Not available for Bengaluru at reasonable cost |

---

## R-06 — Camera-to-road projection (geometry)

**Decision**: flat-ground pinhole model with a dynamic pitch/roll.

- Camera intrinsics K (from R-03). Distortion corrected in the display shader (and on mask sampling).
- Extrinsics: height *h* (vehicle profile + calibration), pitch θ, roll φ from calibration. They are refined per frame by the **gravity vector** (handles mount flex and bike lean) and slowly by **vanishing-point estimation** from lane/road edges (online, low-gain).
- Ground-plane homography **H = K · [r1 r2 t]**, mapping road-plane (x lateral, z forward, metres) ↔ image pixels. BEV warp of the mask is computed at low resolution (e.g., 128×256 cells at 0.25 m) for planning.
- Valid range: 4 m – 60 m forward (default). Beyond 60 m, flat-ground errors on slopes and flyover ramps grow, so chevrons fade out.
- **Two-wheeler lean**: when roll exceeds ±15° (cornering), reduce overlay to Straight-only/fallback, since bike lean breaks the flat-mount assumption.

---

## R-07 — Ego carriageway determination (left-hand traffic)

**Decision**: rule-based classifier in BEV on top of segmentation, executed per inference with temporal hysteresis:

1. Extract the drivable connected component containing the vehicle's own position (bottom-centre of the BEV).
2. If a **median/divider** class or a non-drivable gap separates drivable regions → the ego carriageway is the component containing the vehicle. Typically this is left of the median.
3. Else if a **continuous centre marking** (solid/double/dashed centre line) is detected → ego = region left of it.
4. Else compute road width *W* at 10–20 m:
   - *W* < 5.5 m (narrow lane/residential) → ego = full width.
   - *W* ≥ 5.5 m → ego = left 55% of width, measured from the left road edge (undivided two-way assumption).
5. One-way roads: the provider's road attributes may indicate one-way where available. Otherwise apply rule 4 (conservative).
6. Lane split: if lane markings are detected in the ego carriageway → lanes indexed from the left (lane 1 = left-most). Ego lane = the lane containing the vehicle.

**Never-draw zones**: footpath, kerb/median, barrier, non-ego carriageway, area beyond 60 m, near-field keep-clear (first 4 m).

---

## R-08 — Guidance path generation (smooth curves)

**Decision**:

- **Centreline**: sample ego-corridor midpoints per BEV row (0.5 m steps). Fit a **cubic smoothing spline**, curvature-limited (min radius 4 m two-wheeler, 6 m car).
- **Turns/forks/ramps**: G1/G2-continuous blend (cubic Bézier or clothoid approximation) from the current tangent into the exit tangent, given by the exit bearing (route geometry) and the perceived opening centre. Validate that ≥ 95% of sampled curve points lie in the drivable mask (excluding occluders). Otherwise reject → Straight-only AR.
- **Lane change (US6)**: S-curve (quintic lateral offset over a longitudinal distance ≥ 3 s × speed).
- **U-turn**: approach in the right-most lane of the own carriageway → semicircular arc through the median opening (radius ≈ half the carriageway width, min 4–6 m) → merge into the left-side carriageway in the opposite direction. Draw only on mask-validated pixels. Opening not perceived → pointer-only guidance.
- **Roundabout** (clockwise in India): arc around the island with the exit count on the HUD.
- Path smoothing over time: Kalman-style filter on spline control points in the vehicle frame, compensated by ego-motion (speed + yaw rate).

---

## R-09 — Rendering & visual design

**Decision**: one GLES 3.1 render pass per display frame:

1. Camera OES texture → full-screen quad (undistortion optional).
2. Chevrons as **instanced quads on the road plane**, transformed by H (vertex shader). Shapes come from a signed-distance-field (SDF) texture for crisp edges and a soft glow at any size.
3. The fragment shader samples the **ego-corridor mask** (clip) and **occluder mask** (vehicles/people/animals). Chevrons are discarded where outside or occluded, with a 2-px feather.
4. HUD (pointer, distance, speed, ETA, minimap) in the same GL context, as a 2D layer, to avoid Compose recompositions during driving.

**Visual spec** (from NFS/Forza references; default):

| Property | Value |
|----------|-------|
| Neutral colour | Cyan/teal `#2EE6D6` with 1 px darker outline; high-contrast option white `#FFFFFF` + black outline |
| Opacity | 45% near → 20% far; glow 15% |
| Chevron size | Width ≈ 60% of corridor width (max 2.5 m), depth 0.8 m, spacing 3–8 m by speed |
| Flow animation | Pattern scrolls toward the vehicle at speed-proportional rate; static when stopped |
| Turn emphasis | Chevrons rotate to the local path tangent; on curves they compress (denser) to read as a bend |
| Signal colours | Green `#3BE36B`, Amber `#FFB020`, Red `#FF3B3B` + stop-bar shape |
| Fade | Fade in 200 ms, fade out 300 ms on mode changes |
| Keep-clear | First 4 m, and bottom 12% of the screen |
| Coverage cap | ≤ 20% of frame pixels. Opacity is auto-reduced if exceeded |

**Latency strategy**: render the newest camera frame at display rate. Apply the latest perception result, warped by the IMU rotation delta (gyro) between the inference frame and the displayed frame ("late warp"). Inference runs asynchronously on a dedicated thread with a single-slot queue that drops stale frames.

---

## R-10 — Positioning, heading, speed, flyover disambiguation

**Decision**:
- Position, road-snapped location and route progress come from the **navigation provider**, which uses fused location internally. Raw `FusedLocationProviderClient` is used only for HUD speed if the provider doesn't expose it.
- Heading: course over ground when speed > 3 m/s. Otherwise the rotation-vector sensor plus the calibrated yaw offset. Complementary filter with gyro yaw rate.
- Speed: GNSS Doppler speed (`Location.speed`) smoothed (EMA, τ ≈ 0.7 s). Hidden if `speedAccuracyMetersPerSecond` > 1.5 m/s.
- **Flyover vs service road**: barometer (`TYPE_PRESSURE`) relative altitude trend (+4–8 m climb over 100–200 m indicates a ramp), plus perception (ramp edge/barrier patterns), plus the provider's off-route detection. Used only to **confirm or alert** ("You may be on the service road"). Never to force a reroute on our own.

---

## R-11 — Traffic-signal association & colouring logic (US7)

**Decision**:
1. Detect signal heads in the upper image region. Track them across frames (IoU tracker).
2. **Associate** a head with the ego approach when all of these hold: the head is within the lateral corridor of the ego path ± 6 m at its estimated distance (from bounding-box height with known head size, roughly 0.3 m lamp diameter), it faces the camera (visible lit lamps), and the route's next manoeuvre point is within 80 m.
3. **Movement mapping**: route manoeuvre → indication preference: left → left arrow, else disc; straight → straight arrow, else disc; right/U-turn → right arrow, else disc.
4. **Temporal voting**: show a colour only if the same state wins ≥ 4 of the last 5 classifications **and** mean confidence ≥ 0.9 (default). Flashing amber → neutral. Conflicting heads → neutral.
5. **Green requires** a positively identified green indication for the mapped movement. Absence of red is never treated as green.
6. **Free-left**: POC uses (a) a curated list of known free-left junctions on the evaluation routes and (b) perception of a left slip lane. On a free-left, left-turn guidance stays neutral.

---

## R-12 — Minimap (US8)

**Decision**: default to an **in-house vector minimap** drawn in GL from the active route geometry (route line, manoeuvre markers, heading-up, speed-based zoom). It has no basemap, so it costs no extra network or GPU and has no tile-licensing issue. Optionally (flagged) show the provider's map view in a small circular viewport if the SDK allows it with acceptable performance and attribution.

**Alternative**: full provider map in a corner, which adds GPU/memory load; evaluate after SC-007 is met.

---

## R-13 — Performance, thermal and battery strategy

**Decision**: budgets in plan.md. `PowerManager.addThermalStatusListener` (API 29) and `getThermalHeadroom` (API 30+) drive a **thermal ladder**:

| Level | Trigger | Action |
|-------|---------|--------|
| L0 | NONE/LIGHT | Full: 30 fps display, 15–30 Hz perception |
| L1 | MODERATE or headroom > 0.8 | Perception → 10 Hz, signal detector off |
| L2 | SEVERE | Perception → 6 Hz at lower res, minimap off, display 24 fps |
| L3 | CRITICAL | AR off → Fallback arrow mode, camera preview kept at low fps |
| L4 | EMERGENCY/SHUTDOWN | Camera off, HUD-only navigation |

Use `Display` refresh-rate hints to stay at 30/60 Hz. Ask the OS for sustained performance mode where available. Show a battery-saver prompt at < 15%.

---

## R-14 — Testing strategy

- **Unit tests** (JVM): geometry (homography, BEV), ego-carriageway rules, path generator, lane-change planner, signal state machine, mode controller, thermal ladder, scoring.
- **Replay tests**: recorded drives (video + sensor + location + provider event log) run through the full pipeline in developer replay mode on device. Assertions on metrics (SC-001, SC-006, SC-009, SC-010).
- **Model evaluation** (offline, Python): mIoU per class, ego-carriageway IoU, leakage metric, signal confusion matrix, false-green count.
- **Benchmarks**: Jetpack Macrobenchmark (startup), custom frame-timing traces with Perfetto, battery via `dumpsys batterystats`, thermal logs.
- **Instrumented UI tests**: Compose UI tests for onboarding, camera picker, search; mocked `NavigationProvider`.
- **Supervised field tests**: protocol in quickstart.md.

---

## R-15 — Data collection & labelling

- Capture with the app's developer recording mode (FR-101) on mounted phones. A passenger or safety operator operates the phone.
- Label with CVAT (self-hosted) using a class schema aligned to R-04. Blur faces and number plates before any data leaves the team's controlled storage.
- Dataset versioning: DVC or a simple manifest with hashes. Evaluation set frozen per release.
- Coverage matrix: {day, night, dusk, rain} × {arterial divided, undivided, narrow residential, flyover/ramp, underpass, Metro work zone, signalised junction, free-left, U-turn opening, roundabout}.

---

## R-16 — Legal and safety context (India)

- Indian rules (Central Motor Vehicles Rules amendment, 2020) are commonly understood to permit handheld device use **for route navigation only, if it does not distract the driver**. **Legal counsel must confirm** before any public release. The app enforces mount-only, no-interaction-while-moving behaviour (Constitution I.5).
- DPDP Act 2023: minimal personal data; location used for the navigation purpose; clear notice and consent; deletion of local history on request.
- Store policy: Google Play "driver distraction" and location-permission policies. Foreground service type `location` + `camera` declarations.
