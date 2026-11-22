# Feature Specification: AR Road Navigation POC ("phone as glass")

**Feature ID**: `001-ar-road-navigation-poc`
**Created**: 2026-11-22
**Status**: Draft
**Risk tier**: HIGH RISK. The user is controlling a vehicle, so wrong guidance can contribute to a crash.
**Input**: Product description from the product owner: "Limitless Sight". A phone app that uses the phone camera to recognize the road and draws navigation chevrons on the user's own carriageway, game-style (NFS/Forza), for riders and drivers in Bengaluru. POC scope: camera selection with calibration, destination entry, best route, camera-synced on-road chevrons, top direction pointer, speed, lane pre-guidance, and optionally signal-aware colours and a minimap.

---

## Scope

### In scope (POC)

- Android phone mounted **horizontally (landscape)** on a two-wheeler handlebar or a car dashboard/windshield.
- Selecting a camera lens from all rear cameras the device exposes, plus a calibration test that recommends the best lens.
- Destination search with the current location as the source. Computing the best route for the chosen vehicle type.
- Live camera view (the "glass" in this POC) with:
  - on-road guidance chevrons drawn only on the user's own carriageway;
  - smoothly curved turn, fork, ramp and U-turn guidance;
  - lane pre-positioning prompts before turns;
  - a **top direction pointer** (rotating arrow), next-manoeuvre distance, and **current speed**;
  - (optional, not required for POC acceptance) signal-aware chevron colouring;
  - (stretch) a game-style **minimap**.
- Rerouting when off-route, and safe degradation when confidence is low.
- Bengaluru as the launch and evaluation geography.

### Out of scope (POC)

- Wearable glass hardware, optical see-through rendering.
- Hazard warnings (pedestrians, animals, potholes, speed breakers). Planned for feature 002.
- iOS; front cameras; portrait mode.
- Full offline maps/routing. The route is cached for the current trip only.
- Voice turn-by-turn narration. Only optional short audio chimes are included.
- Multi-stop trips, trip history sync, accounts, social features.
- Any control of the vehicle.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Choose a camera and calibrate it (Priority: P1)

As a rider mounting my phone horizontally, I want the app to list my phone's rear cameras, test them, and help me pick the one that sees the road best. The overlay should then sit exactly on the road.

**Why this priority**: every later feature depends on a working camera and a correct camera-to-road model. Wide-angle lenses on budget phones often have poor image quality, distortion and low frame rates, so choosing blindly would break chevron placement.

**Independent Test**: on a supported phone mounted on a stationary vehicle facing a road, complete onboarding. The app lists every rear camera with a friendly name, runs calibration on the user's choice (and optionally on all), shows a quality report, and saves a calibration profile. This can be verified with no route.

**Acceptance Scenarios**:

1. **Given** a phone with 3 rear lenses (ultra-wide, main, telephoto), **When** the user opens the camera step, **Then** all 3 are listed as "Camera 1/2/3". Each shows a friendly label (Ultra-wide/Main/Tele), field of view, and a live thumbnail preview.
2. **Given** the user selects a camera, **When** they tap "Calibrate", **Then** the app checks and scores (0–100): orientation (landscape, roll within ±5°), mount stability (vibration), image sharpness, low-light noise, achievable frame rate, road visibility (share of frame showing road in the expected region), horizon/pitch, and on-device processing speed for that lens. It shows pass/warn/fail per check with a fix-it hint.
3. **Given** the selected lens scores below the recommended threshold and another lens scores higher, **When** the report is shown, **Then** the app recommends the higher-scoring lens and explains why, e.g. "Ultra-wide image is too soft in this light; Main camera recommended". The user can accept the recommendation or keep their choice after a warning.
4. **Given** the user chooses "Test all cameras", **When** calibration completes, **Then** the app ranks all lenses and preselects the best one.
5. **Given** the phone is in portrait or tilted beyond tolerance, **When** calibration runs, **Then** the app shows a level guide (bubble indicator). Calibration does not pass until the phone is horizontal and the road occupies the lower part of the frame.
6. **Given** calibration succeeds, **When** the user next opens the app with the same device and mount type, **Then** the saved profile is reused. A quick 3-second re-check confirms the mount angle without a full calibration.
7. **Given** the user picks the vehicle type (two-wheeler / car) and mount position, **When** calibration completes, **Then** the camera height and mount defaults for that vehicle are applied and can be fine-tuned.

---

### User Story 2 — Set a destination and get the best route (Priority: P1)

As a user, I want to type or pick a destination in Bengaluru and get the best route from where I am now for my vehicle type, so that I can start riding.

**Why this priority**: there is nothing to augment without a route.

**Independent Test**: while stationary with GPS fix, search "Indiranagar 100 Feet Road". Select a result and see route summary (distance, ETA, via-roads). Start navigation. This works without camera perception.

**Acceptance Scenarios**:

1. **Given** location permission is granted and a GPS fix is available, **When** the user opens destination search, **Then** the source is set to "Current location" automatically. Suggestions are biased to Bengaluru.
2. **Given** the user types at least 3 characters, **When** suggestions appear, **Then** each shows a name, locality and distance. Selecting one shows the route summary.
3. **Given** the vehicle type is two-wheeler, **When** a route is computed, **Then** the provider's two-wheeler routing is used where available. Otherwise driving routing is used, with a visible note.
4. **Given** multiple route alternatives exist, **When** the summary is shown, **Then** the recommended (fastest, traffic-aware) route is preselected. Up to 2 alternatives can be chosen while stationary.
5. **Given** the vehicle is moving (speed > 5 km/h), **When** the user tries to edit the destination, **Then** text input is blocked with "Stop safely to change destination". Voice entry is not provided in the POC.
6. **Given** there is no network, **When** the user searches, **Then** a clear offline message appears and recent destinations (stored on device) are offered. Routing requires network in the POC.

---

### User Story 3 — On-road chevrons on my own carriageway (Priority: P1) 🎯 MVP core

As a rider, I want translucent chevrons drawn on the road surface of the carriageway I'm riding on, flowing toward where I need to go. Turns should appear as smooth curves. Then I can follow the route without looking at a map.

**Why this priority**: this is the product's core value.

**Independent Test**: on a recorded Bengaluru drive replayed through the app (replay mode), and on a supervised field run, chevrons stay on the user's carriageway. They curve into the correct turn at the correct junction, and never appear on footpaths, medians or oncoming lanes.

**Acceptance Scenarios**:

1. **Given** navigation is active on a straight road, **When** the camera sees the road, **Then** a row of translucent chevrons is drawn on the road surface. It follows the road's perspective and fades with distance. It is pinned to the road and does not drift with the phone.
2. **Given** an **undivided two-way road** (single carriageway), **When** chevrons are drawn, **Then** they occupy the **left (own) half** of the drivable road, or the user's lane if lane markings are detected. They do not extend into the right (oncoming) half.
3. **Given** a **divided road** (median/divider visible), **When** chevrons are drawn, **Then** they are drawn only on the carriageway left of the median, never on the median or the oncoming carriageway.
4. **Given** a narrow single-lane road (no lane markings, width below the threshold), **When** chevrons are drawn, **Then** they may span the whole drivable width. They are still clipped at the road edge, kerb and footpath.
5. **Given** an upcoming left/right turn, **When** the vehicle is within the turn display distance, **Then** the chevron path bends in a smooth curve (no sharp angles) into the exit road. Chevrons ahead of the curve point in the turn direction, and the curve's position matches the actual junction mouth seen by the camera.
6. **Given** a **fork, ramp, flyover or underpass split** (e.g., flyover straight vs service road left), **When** approaching the split, **Then** chevrons visibly sit on the correct branch, and the other branch shows no chevrons. A short "Take flyover" / "Take service road (left)" label appears near the top pointer.
7. **Given** a **U-turn** at a median opening, **When** approaching, **Then** the chevrons guide toward the right-most lane of the own carriageway (or the median opening lane). They then draw a smooth U-shaped arc through the opening, on the road surface only.
8. **Given** a pedestrian, vehicle or animal is standing on the road where chevrons would be drawn, **When** rendering, **Then** chevrons are hidden behind that object (the object appears in front of them).
9. **Given** the user is in mixed traffic, **When** chevrons are drawn, **Then** overlay opacity stays within the safe range. The bottom keep-clear band and the user's immediate path area (nearest few metres) are not obscured.
10. **Given** the route is followed, **When** the vehicle passes a manoeuvre point, **Then** guidance moves to the next segment within 1 second (default), with no flicker.

---

### User Story 4 — Safe degradation, rerouting and trust signals (Priority: P1)

As a user, I want the app to tell me honestly when it cannot see the road or is unsure of my position. It should fall back to simple guidance and reroute me if I miss a turn, so I'm never misled.

**Why this priority**: required by Constitution Principle I.3 ("fail safe"). A POC cannot be road-tested without it.

**Independent Test**: in replay and bench tests, inject low perception confidence, GPS loss, a wrong turn and thermal throttling. Verify the fallback mode, banners and reroute behaviour.

**Acceptance Scenarios**:

1. **Given** road segmentation confidence drops below the threshold (night glare, heavy rain, lens obstructed, bus directly ahead), **When** this persists > 0.5 s (default), **Then** on-road chevrons fade out. A **screen-fixed fallback arrow** with the next-manoeuvre distance is shown, plus a small "Road view limited" indicator.
2. **Given** confidence recovers for > 1 s, **When** the condition clears, **Then** on-road chevrons fade back in. No abrupt jumps.
3. **Given** the user deviates from the route (missed turn, wrong flyover/service road), **When** the navigation provider reports off-route, **Then** the chevrons stop and the pointer shows "Rerouting…". The new route is used within 5 s (default, network permitting).
4. **Given** GPS accuracy degrades (> 25 m, e.g. under a flyover or in an underpass), **When** this happens, **Then** the app uses the provider's dead-reckoning/position estimate and shows a "Weak GPS" indicator. It suppresses manoeuvre-specific on-road curves until position is reliable, and keeps straight-ahead chevrons only if perception confidence is high.
5. **Given** the device reaches a severe thermal state, **When** throttling is needed, **Then** the app lowers processing rate and resolution in steps, showing "Reduced quality (device hot)". If still critical, it switches to fallback-arrow-only mode instead of crashing.
6. **Given** the camera is disconnected or fails, **When** this happens, **Then** navigation continues in fallback mode with a clear message.
7. **Given** the user taps the large "Pause" control, **When** paused, **Then** all overlays hide except a small "Paused" badge. Navigation progress continues in the background.

---

### User Story 5 — Top direction pointer, distance and speed HUD (Priority: P2)

As a user, I want a pointer at the top of the view that rotates toward my route direction and next manoeuvre. I also want a clear speed readout, so I always know where I'm heading even when the road view is ambiguous.

**Why this priority**: high value and low complexity. It also serves as the fallback guidance in US4. P2 only because US3 delivers the core value. The fallback-arrow behaviour itself is part of US4.

**Independent Test**: during navigation, rotate the vehicle/heading and verify that the pointer angle equals the bearing to the next guidance target relative to the vehicle heading, within the tolerance. Compare the speed readout with a reference GPS logger.

**Acceptance Scenarios**:

1. **Given** navigation is active, **When** the next manoeuvre is a right turn 300 m ahead, **Then** the top pointer points along the route direction and shows "300 m" with a right-turn glyph. It rotates smoothly as the bearing changes.
2. **Given** the user is heading directly away from the route (e.g., after a wrong turn), **When** the pointer updates, **Then** it points backward/sideways accordingly. It does not stay frozen.
3. **Given** the vehicle is moving, **When** speed is shown, **Then** it reads in km/h, updates at least 1 Hz, and is smoothed. It is hidden when the GPS speed accuracy is poor.
4. **Given** the posted speed limit is known from the provider, **When** current speed exceeds it, **Then** the speed readout turns amber. No sound. If the speed limit is unavailable, nothing is shown.
5. **Given** the HUD is visible, **When** measured, **Then** the HUD (pointer, distance, speed, ETA) occupies only the top band and corners, per the layout in the plan.

---

### User Story 6 — Lane pre-positioning before turns (Priority: P2)

As a driver in the centre lane who needs to turn left soon, I want the chevrons to gradually guide me into the left lane at a safe distance before the junction. I can then turn safely instead of cutting across lanes.

**Why this priority**: a major safety benefit, but it depends on US3 and on lane detection quality, which varies on Bengaluru roads where markings are faded or absent.

**Independent Test**: on replay drives of multi-lane roads (e.g., Outer Ring Road, Hosur Road) with labelled lanes, verify the timing and direction of lane-shift prompts against the planned manoeuvre.

**Acceptance Scenarios**:

1. **Given** a left turn ahead, the user in a middle lane, and lane markings detected, **When** the vehicle reaches the lane-change start distance (speed-dependent, default: max(150 m, 8 s × speed)), **Then** the chevron path drifts gently (S-curve) from the current lane toward the target lane over ≥ 3 s of travel. A small lane-change cue appears.
2. **Given** the provider supplies lane guidance (recommended lanes), **When** available, **Then** the target lane follows the provider's recommendation. Otherwise it follows the turn side: left-most lane for left turns, right-most lane of own carriageway for right turns and U-turns.
3. **Given** the user is already in the target lane, **When** approaching the turn, **Then** no lane-change cue is shown.
4. **Given** lane markings are not detected, **When** a turn approaches, **Then** the chevrons bias toward the turn side of the own carriageway without implying a specific lane.
5. **Given** the remaining distance is too short for a safe lane change (default: < 3 s at current speed or < 40 m), **When** the user is not in a turn-capable lane, **Then** the app does not prompt a last-second cut. It shows "Continue straight — rerouting" if the provider reroutes, or keeps the guidance on the current lane.
6. **Given** a **free-left** junction (left turn permitted irrespective of signal, common in Bengaluru), **When** the route turns left, **Then** guidance moves to the free-left slip lane if one is detected.

---

### User Story 7 — Signal-aware guidance colour (Priority: P3, optional — not required for POC acceptance)

As a user approaching a traffic signal, I want the on-road chevrons to take the colour of the signal that applies to **my** movement (left/straight/right). Then I know the state without searching for the signal head.

**Why this priority**: valuable, but very hard to get right. Bengaluru signals often have separate arrow lights, countdown timers, and police overrides. A false "green" is a safety risk. Delivered only if the accuracy gates in SC-008 are met. Otherwise the feature ships disabled.

**Independent Test**: on a labelled evaluation set of Bengaluru signal approaches and in supervised field runs, measure per-movement signal state accuracy. Measure especially the **false-green rate**.

**Acceptance Scenarios**:

1. **Given** a signal head with a full red disc is detected for the ego approach, **When** within the signal display distance (default ≤ 80 m), **Then** the chevrons turn **red** and show a translucent "stop bar" shape near the stop line. Colour and shape both carry the meaning, for colour-blind users.
2. **Given** separate arrow signals (e.g., green left arrow, red straight), **When** the route goes straight, **Then** chevrons show **red**. If the route goes left, they show **green**. The colour follows the arrow matching the route manoeuvre.
3. **Given** amber/yellow, **When** detected, **Then** chevrons turn amber.
4. **Given** the signal state is uncertain, the signal is not associated with the ego approach, a manual/police-controlled junction, or flashing amber, **When** rendering, **Then** chevrons keep the **neutral** route colour. **Uncertain must never render as green.**
5. **Given** a countdown timer is detected, **When** available, **Then** the remaining seconds may be shown next to the top pointer (stretch-of-stretch).
6. **Given** a free-left turn, **When** the route turns left at a junction marked as free-left, **Then** the signal colour is not applied to left-turn guidance.

---

### User Story 8 — Game-style minimap (Priority: P3, stretch)

As a user, I want a small circular minimap in a corner showing the route line and my position, heading-up, as in racing games. Then I get context, such as which side road comes next, without opening a full map.

**Why this priority**: nice-to-have context. It must not compete with the on-road guidance for attention.

**Independent Test**: during navigation, the minimap shows the route line, the current position marker and upcoming manoeuvres. It rotates heading-up and stays within its size and opacity limits.

**Acceptance Scenarios**:

1. **Given** navigation is active, **When** the minimap is enabled (default: on for car, off for two-wheeler), **Then** a circular, translucent, heading-up map (≈ 300–500 m radius, zooming with speed) appears bottom-left, with the route highlighted.
2. **Given** the user taps the minimap while stationary, **When** tapped, **Then** a full-screen route overview opens. It is not available while moving.
3. **Given** the map provider is unavailable, **When** rendering, **Then** the minimap shows a simple vector route line from the cached route geometry, with no basemap.

---

### Edge Cases

| # | Situation | Expected behaviour |
|---|-----------|-------------------|
| E1 | Bus/truck directly in front blocks most of the road | Chevrons drawn only on visible road. If visible road < threshold → fallback arrow (US4) |
| E2 | Heavy rain, water drops on lens, wet reflective road | Lower confidence → fallback. Reflections must not be classified as road beyond confidence limits |
| E3 | Night, glare from oncoming headlights, sodium lamps | Night profile (exposure/ISO limits); fallback if confidence low |
| E4 | Faded/absent lane markings (very common) | Use drivable-area geometry; skip lane-specific prompts (US6-4) |
| E5 | Road under construction, barricades, Metro work zones | Barricades are non-drivable. If the route geometry conflicts with perception → do not draw a curve, show fallback arrow |
| E6 | Flyover vs service road vs underpass (stacked/parallel roads) | Use provider manoeuvre (ramp/fork/keep), lane/branch detection and the barometric altitude trend to show the correct branch. If ambiguous → pointer label ("Take flyover") with no on-road curve |
| E7 | Wrong-way traffic or one-way roads | Never draw on the oncoming side. Provider handles one-way legality |
| E8 | Roundabout (e.g., Trinity Circle style) | Smooth clockwise-arc guidance (left-hand traffic) with the exit count on the pointer |
| E9 | Speed breakers, potholes | Not detected in the POC. Chevrons must not imply road quality |
| E10 | Phone vibration on two-wheeler | Stabilization and temporal smoothing. If vibration exceeds the limit → advise re-mounting at calibration and reduce overlay detail when riding |
| E11 | Device overheats in Bengaluru sun | Thermal ladder (US4-5) |
| E12 | Incoming phone call / app backgrounded | Navigation continues in the provider. When the user returns, the overlay resumes after a quick sanity check. No overlay drawn while backgrounded |
| E13 | Low battery (< 15%) | Prompt battery-saver: lower frame rate, minimap off |
| E14 | Lens changes due to OEM auto-switching (logical multi-camera zoom) | Lock the selected physical lens. If the OEM prevents this, warn at calibration |
| E15 | User mounts the phone differently after calibration | Quick re-check detects pitch/roll change > tolerance and asks for recalibration |
| E16 | Police manually controlling junction | Signal colouring neutral (US7-4) |
| E17 | Destination unreachable by two-wheeler (e.g., expressway restrictions) | Provider error surfaced; suggest car routing or nearest point |
| E18 | GPS spoofing / mock locations | Ignore mock locations in navigation unless developer replay mode is on |

---

## Requirements *(mandatory)*

### Functional Requirements

**Onboarding, permissions & mounting**

- **FR-001**: The app MUST request camera, precise location, and (optionally) notification permissions with plain-language reasons. It MUST work without notification permission.
- **FR-002**: The app MUST show a one-time safety notice ("mount the phone, don't operate while moving, guidance is advisory") and the selected navigation provider's terms. Both must be accepted before first navigation.
- **FR-003**: The app MUST let the user choose a vehicle type (two-wheeler, car) and mount position (handlebar, dashboard, windshield). It MUST store a camera-height default per choice (two-wheeler 1.1 m, car dashboard 1.2 m) that the user can adjust.
- **FR-004**: Navigation screens MUST be landscape-only. The screen MUST stay on during navigation.

**Camera selection & calibration (US1)**

- **FR-010**: The app MUST list every rear-facing camera the OS exposes, **including individual physical lenses behind logical multi-camera devices** where the OS allows. Each entry needs an index ("Camera 1…N"), an inferred label (Ultra-wide/Main/Tele), horizontal field of view, and a live preview.
- **FR-011**: The app MUST run a calibration test per selected camera that measures and scores: (a) device roll/pitch (landscape, level); (b) vibration level; (c) image sharpness; (d) noise in current light; (e) sustained frame rate at the processing resolution; (f) road visibility and horizon position; (g) on-device processing latency with that lens's stream; (h) lens distortion severity, from the provider's intrinsic data or estimated.
- **FR-012**: The app MUST compute an overall suitability score per camera, recommend the best camera, and explain the recommendation. The user MAY override with a warning.
- **FR-013**: The app MUST offer "Test all cameras" to rank every lens automatically.
- **FR-014**: The app MUST derive and persist a **calibration profile**: camera id, intrinsics, distortion, mount height, pitch, roll, yaw offset relative to vehicle heading, and processing resolution. It is used for road-plane projection.
- **FR-015**: The app MUST run a quick re-check (≤ 3 s, default) at each navigation start and require recalibration if mount pitch/roll differ beyond tolerance (default ±3°).
- **FR-016**: The app MUST keep the selected physical lens fixed during navigation, with no auto lens switching, digital zoom or OEM scene modes.

**Destination & routing (US2)**

- **FR-020**: The app MUST set the source to the current device location and provide destination search with autocomplete biased to Bengaluru (and India).
- **FR-021**: The app MUST compute a traffic-aware best route for the chosen vehicle type through the navigation provider, and show distance, ETA and main roads.
- **FR-022**: The app MUST support choosing among provider alternatives while stationary.
- **FR-023**: The app MUST block text entry and route changes while speed > 5 km/h (default).
- **FR-024**: The app MUST store up to 20 recent destinations on-device (place id + label). The user can clear them.
- **FR-025**: The app MUST keep a local monthly usage counter per billable provider call type (navigation destinations, autocomplete, place details). It MUST refuse new billable calls once the configured safety limit is reached (default: 80% of the provider's free monthly cap) and show "Monthly free navigation limit reached". The count resets on the provider's billing-month boundary. In developer mode, the user can switch to the free open-source provider instead (research R-01).

**Route–camera synchronization**

- **FR-030**: The app MUST continuously track route progress (snapped position, distance along route, current step, next manoeuvre, distance to manoeuvre, lane guidance when available) through the navigation provider at ≥ 1 Hz.
- **FR-031**: The app MUST estimate vehicle heading (from course over ground when moving, from device orientation when slow) and keep the camera's heading aligned with the route, using the calibrated yaw offset.
- **FR-032**: The app MUST timestamp camera frames, perception results, location, and orientation on a common monotonic clock, and render overlays matched to the displayed frame's timestamp.

**On-road guidance rendering (US3)**

- **FR-040**: The app MUST detect, per frame, the drivable road surface, road boundaries (kerb/footpath edge), lane markings when present, medians/dividers, and objects on the road (vehicles, people, animals) for occlusion.
- **FR-041**: The app MUST identify the **ego carriageway** under left-hand traffic rules:
  - divided road → drivable region left of the median;
  - undivided road with centre marking → left of the centre line;
  - undivided without marking and width ≥ threshold → left portion of the road (default ≤ 55% of drivable width, measured from the left edge);
  - narrow road (< threshold, default 5.5 m) → full drivable width.
- **FR-042**: The app MUST render translucent, perspective-correct chevrons **on the road plane**, within the ego carriageway (or ego/target lane), clipped by the drivable mask and occluded by detected objects.
- **FR-043**: Guidance path geometry MUST be smooth (continuous tangent; no sharp corners). Turns, forks, ramps, U-turns and roundabouts use smooth curves that fit the observed junction geometry when it is visible.
- **FR-044**: Chevron flow animation speed MUST scale with vehicle speed. Spacing grows with speed (default 3–8 m). Visibility fades from near to far (default 5–60 m).
- **FR-045**: The default visual style MUST be translucent (default opacity 35–55%), use a neutral route colour (cyan/teal), and stay within the overlay coverage limit (default ≤ 20% of frame pixels). It MUST keep a near-field keep-clear zone (default first 4 m) free.
- **FR-046**: At forks/ramps/flyover splits, the app MUST draw chevrons only on the branch matching the route manoeuvre and show a short text label for the branch.
- **FR-047**: Temporal smoothing MUST prevent chevron jitter: under steady driving, the projected path may not shift more than the jitter tolerance between consecutive frames (default tolerance 10 px at 1080p; acceptance per SC-010).

**Safety & degradation (US4)**

- **FR-050**: The app MUST compute a per-frame **guidance confidence** from perception confidence, ego-carriageway clarity, position accuracy, and the agreement between route geometry and perceived road. It MUST switch between modes *Full AR*, *Straight-only AR*, and *Fallback arrow* with hysteresis.
- **FR-051**: The app MUST show visible status indicators for: road view limited, weak GPS, rerouting, reduced quality (thermal), paused.
- **FR-052**: The app MUST request reroute (or accept provider reroute) when off-route and present the new route within 5 s (default) when network is available.
- **FR-053**: The app MUST follow a documented thermal ladder that reduces processing rate and resolution before stopping AR.
- **FR-054**: The app MUST provide a large on-screen **Pause** and **End** control usable with gloves (default ≥ 15 mm touch target).
- **FR-055**: The app MUST NOT show any guidance that asserts safety ("safe to go", "lane clear").

**HUD (US5)**

- **FR-060**: The app MUST show a top-centre **direction pointer** that rotates toward the route direction (look-ahead point on the route, default 50–150 m depending on speed). Next to it: next-manoeuvre glyph and distance.
- **FR-061**: The app MUST show current speed (km/h) with smoothing. It MUST hide speed when accuracy is poor.
- **FR-062**: The app SHOULD show speed-limit context when the provider supplies it (amber speed when exceeding).
- **FR-063**: The app MUST show ETA and remaining distance in a compact corner element.

**Lane pre-positioning (US6)**

- **FR-070**: The app MUST plan a lane-shift start distance before turns based on speed (default max(150 m, 8 s × speed)). It MUST express the shift as a smooth S-curve in the chevron path when lanes are detected.
- **FR-071**: The app MUST use provider lane recommendations when available. Otherwise it falls back to turn-side rules for left-hand traffic.
- **FR-072**: The app MUST NOT prompt a lane change when remaining time/distance is below the safe minimum (default 3 s or 40 m).

**Signal colouring (US7, stretch, feature-flagged)**

- **FR-080**: When enabled, the app MUST detect traffic-signal heads, classify state per indication (disc / left / straight / right arrows; red / amber / green; flashing; countdown digits), and associate the head with the ego approach.
- **FR-081**: The app MUST colour chevrons by the state of the indication matching the route manoeuvre, only above the confidence threshold. In every other case it keeps the neutral colour.
- **FR-082**: Red state MUST also change chevron shape (stop bar), so meaning isn't conveyed by colour alone.
- **FR-083**: The feature MUST stay disabled by default until SC-008 is met.

**Minimap (US8, stretch)**

- **FR-090**: The app SHOULD provide a heading-up circular minimap with the route line, current position and next manoeuvre markers, and speed-based zoom. It can be toggled; size and opacity are bounded.

**Developer & test tooling**

- **FR-100**: The app MUST support a developer **replay mode**: play a recorded video + sensor/location log through the full pipeline with identical timing, for deterministic tests.
- **FR-101**: The app MUST support opt-in local **trip recording** (video + logs) in developer mode, with an on-screen recording indicator.
- **FR-102**: The app MUST expose an on-screen diagnostics overlay in developer mode: FPS, latency, confidence, mode, thermal state, drivable mask preview.

### Non-Functional Requirements

Each NFR has a **POC gate**, which must pass for the POC to be accepted, and a **target**, which we aim for, measure and report but which doesn't block the POC. See *Acceptance policy* under Success Criteria. Gates are measured on the mid-range reference device unless stated otherwise.

| ID | Category | POC gate (pass/fail) | Target (measured & reported) |
|----|----------|----------------------|------------------------------|
| NFR-001 | Latency | Motion-to-photon ≤ 150 ms p95 | ≤ 100 ms p95 |
| NFR-002 | Frame rate | Display ≥ 30 fps at thermal L0–L1 (≥ 24 fps at L2); perception ≥ 10 inferences/s; overlay interpolated between inferences with motion sensors | Perception ≥ 15/s mid-range, ≥ 25/s flagship |
| NFR-003 | Startup | Cold start to camera preview ≤ 4 s; navigation start after route selection ≤ 3 s | ≤ 2.5 s / ≤ 2 s |
| NFR-004 | App size | Download size ≤ 80 MB including models and the navigation SDK | ≤ 50 MB |
| NFR-005 | Memory | No out-of-memory kill in a 30-min trip; peak RSS ≤ 600 MB | ≤ 450 MB |
| NFR-006 | Battery | Measured and reported (no gate) | ≤ 25% per hour at max brightness |
| NFR-007 | Thermal | 30-min continuous navigation in Bengaluru daytime ambient without a crash; AR may degrade per the ladder | 45 min without dropping below thermal level L2 |
| NFR-008 | Data | Measured and reported (no gate) | ≤ 5 MB per 30-min trip excluding any map basemap |
| NFR-009 | Device support | Android 10 (API 29)+, ARM64, OpenGL ES 3.1+. Works on both reference devices: 1 mid-range (Snapdragon 6/7-series or Dimensity 7000-series class) and 1 flagship | 3+ additional popular Bengaluru OEM devices |
| NFR-010 | Availability | No crash in 50 consecutive replay runs | 100 runs; ≥ 99.5% crash-free sessions in the internal pilot |
| NFR-011 | Privacy | **Hard gate**: no camera frames leave the device; location sent only to the navigation provider | — |
| NFR-012 | Accessibility | Colour + shape redundancy; HUD text ≥ 18 sp | High-contrast mode |
| NFR-013 | Sunlight readability | Measured and reported (no gate) | Mean subjective score ≥ 4/5 from 5 testers in direct sun |
| NFR-014 | Cost | **Hard gate**: ₹0 provider spend during the POC. All provider usage stays inside free monthly caps, enforced by Cloud quotas, a budget alert and the in-app usage guard (FR-025) | — |

### Key Entities *(data involved)*

- **CameraDescriptor**: a rear camera/lens the OS exposes (id, physical/logical, focal length, FOV, capabilities, supported sizes/fps).
- **CalibrationResult / CalibrationProfile**: per-camera test scores and the chosen camera-to-road geometry (intrinsics, distortion, height, pitch, roll, yaw offset).
- **VehicleProfile**: vehicle type, mount position, camera height.
- **Destination / Place**: place id, label, coordinates.
- **Route**: geometry, steps/manoeuvres, lane guidance, speed limits (where provided), ETA.
- **RouteProgress**: snapped position, current step, next manoeuvre, distance, off-route flag.
- **PerceptionFrame**: timestamped drivable mask, lane lines, median/kerb boundaries, objects, signal detections, confidences.
- **EgoCorridor**: the ego carriageway/lane region and centreline on the road plane.
- **GuidancePath**: smooth road-plane curve the chevrons follow, plus style (colour, mode).
- **GuidanceMode**: Full AR / Straight-only AR / Fallback / Paused.
- **SignalState**: per-indication state and confidence, associated movement.
- **TripLog** (dev only): recorded frames + sensor/location stream.

See [data-model.md](./data-model.md) for fields and state machines.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

Evaluation sets are defined in [quickstart.md](./quickstart.md).

### Acceptance policy (decided 2026-11-22)

Numbers in this feature fall into three kinds, and each kind is treated differently:

1. **Safety gates** are hard pass/fail criteria. They limit how often the app can show *wrong* guidance (wrong junction, wrong branch, footpath/oncoming leakage, false green, failure to fall back). Failing any one blocks supervised public-road testing and POC acceptance. They are deliberately strict because a wrong arrow is worse than no arrow. The app can always meet them by falling back more often.
2. **Quality gates** are pass/fail too, but set at a level a first POC on mid-range hardware can realistically reach. They measure how *often* the app gives full AR guidance, plus calibration usability, stability and performance.
3. **Targets** are the original, more ambitious numbers. They steer tuning and are reported with every evidence pack, but missing them doesn't block the POC. Once the POC is done, targets become the next phase's quality gates.

**Design parameters** in the FRs (spacing, distances, thresholds, opacities) are **defaults**. They live in `GuidanceConfig` and are tuned with replay data. They are never pass/fail criteria themselves.

**Order**: safety gates must pass in **replay** before any supervised public-road run, and must pass again in the **supervised field** runs before POC acceptance. Changing a gate needs an entry in the Decision log.

| ID | Criterion | Kind | POC gate | Target |
|----|-----------|------|----------|--------|
| SC-001 | **Footpath/oncoming leakage**: share of guidance-visible frames with > 5% of chevron pixels outside the labelled ego-carriageway region (Bengaluru evaluation set) | Safety | ≤ 1% | ≤ 0.5% |
| SC-002a | **Wrong-junction curve** rate over ≥ 100 manoeuvres in supervised field runs on the agreed routes | Safety | ≤ 1% | 0% |
| SC-002b | **Correct-junction curve** rate (rest falls back to straight-only + pointer) | Quality | ≥ 80% | ≥ 95% |
| SC-003a | **Wrong-branch** rate at split points (Hebbal, Silk Board, Electronic City, KR Puram, Mekhri Circle) | Safety | ≤ 2% | 0% |
| SC-003b | **Correct-branch** rate at those split points (rest falls back with a label) | Quality | ≥ 75% | ≥ 90% |
| SC-004 | **Glance reduction** versus Google Maps on the same mount (≥ 10 participants, simulator or supervised closed road) | Target only | Study protocol written; pilot of ≥ 5 participants run and reported | Measurable reduction in total off-road glance time, plus better self-reported confidence |
| SC-005a | **Calibration completion**: test users finish camera selection + calibration on the first attempt | Quality | ≥ 80% within 3 min | ≥ 90% within 2 min |
| SC-005b | **Lens recommendation** agrees with expert choice across device/light combinations | Quality | ≥ 75% | ≥ 85% |
| SC-006 | **Fail-safe**: correct degraded mode entered in fault-injection replay within 0.5 s (perception loss) / 2 s (GPS loss, off-route) | Safety | 100% of injected cases | 100% |
| SC-007 | **Performance**: NFR-001…NFR-007 and NFR-014 POC gates met on both reference devices | Quality (NFR-011/014: safety/hard) | All gates met | All targets met |
| SC-008 | **Signal colouring**, applies only if US7 is enabled. False-green: rendered green while the ground truth for the route movement isn't green, over ≥ 500 approach sequences | Safety (conditional) | 0 false greens, and ≥ 95% per-movement accuracy when a colour is shown; otherwise the feature stays OFF | Same |
| SC-009a | **Late lane-change prompts**: cues issued below the safe minimum | Safety | 0 | 0 |
| SC-009b | **Lane-cue timing**: cue starts within the planned distance window (replay) | Quality | ≥ 80% | ≥ 90% |
| SC-010 | **Stability**: overlay jitter on straight-road replay segments, plus no flicker between modes (hysteresis respected) | Quality | ≤ 15 px p95 at 1080p | ≤ 10 px p95 |

**POC accepted** when all Safety and Quality gates pass with labelled evidence (quickstart §8) and every target has a reported measurement. A target may be marked *not verified* only with a reason.

---

## Assumptions

1. The POC runs on Android only. The user owns a reasonably recent phone (2021+) with at least one usable rear camera.
2. The device is mounted firmly in landscape, roughly forward-facing, with a clear view of the road. Handheld use is not supported.
3. The user's installed Google Maps app can't share live route data with third-party apps, so the POC embeds a navigation SDK and keeps it **free**. Primary: **Google Navigation SDK** + Places API (New), used only inside their free monthly caps (1,000 navigation destinations a month), with a hard guard (FR-025, NFR-014). Free fallback: **MapLibre Navigation Android + OpenStreetMap** routing (GraphHopper/Valhalla), open-source with no usage fees. A Google Cloud billing account is needed to enable the free caps; with quotas and the guard in place it is never charged. See research.md R-01.
4. Network connectivity is available for search and routing. Perception works offline.
5. Monocular camera + flat-ground approximation within ~60 m is sufficient for overlay placement. Gradients (flyover ramps) are handled by perception-anchored guidance rather than precise 3D maps.
6. The POC is tested in Bengaluru under a supervised field-test protocol. It is not released to the public.

## Clarifications

### Session 2026-11-22

- **Q1 — Navigation provider?** → **Use the best free option.** Primary: Google Navigation SDK + Places API (New), within their free monthly caps, enforced by Cloud quotas, a budget alert and the in-app guard (FR-025, NFR-014). Free fallback: MapLibre Navigation Android with OpenStreetMap routing. Mappls is dropped from the POC. Its free tier for navigation is not clearly documented, so it stays a post-POC option for junction views.
- **Q2 — Do the numeric targets become pass/fail?** → Delegated to the spec author. Decision: the tiered *Acceptance policy* above. Hard safety gates; realistic quality gates; original numbers kept as reported targets; design parameters are tunable defaults.
- **Q3 — Signal colours in the demo?** → **Nice to have, not mandatory.** US7 stays P3, behind the `signal_colouring` flag, default OFF, and is not part of POC acceptance. If built, it can only be switched on after SC-008 passes. A detection-only debug view is optional too.

## Remaining open items (do not block implementation)

- Legal counsel review of Indian navigation-device rules and app safety wording (research R-16). Owner: product owner. Needed before any public release, not before supervised testing.
- Commercial-use status of training datasets and models (licence register, T006). Owner: ML lead. Needed before any model ships outside the team.

## Decision log

| Date | Decision | By | Notes |
|------|----------|----|-------|
| 2026-11-22 | Android-only POC, landscape-only | Draft (spec author) | Pending sign-off |
| 2026-11-22 | Navigation via Navigation SDK behind a provider interface (not the Google Maps consumer app) | Draft | See research R-01 |
| 2026-11-22 | Guidance near-field anchored to perception; route data decides *what* to do and *when* | Draft | See research R-05 |
| 2026-11-22 | Uncertain signal state never renders green | Draft | Constitution I.4 |
| 2026-11-22 | Navigation provider: best free option. Google Navigation SDK + Places within free caps (hard ₹0 guard), MapLibre/OSM as free fallback, Mappls removed from POC | Product owner | Q1; research R-01 |
| 2026-11-22 | Acceptance policy: safety gates (hard), quality gates (realistic), targets (reported), FR numbers are tunable defaults | Spec author (delegated by product owner) | Q2 |
| 2026-11-22 | Signal colouring is optional for the POC; flag default OFF; not in POC acceptance | Product owner | Q3 |
