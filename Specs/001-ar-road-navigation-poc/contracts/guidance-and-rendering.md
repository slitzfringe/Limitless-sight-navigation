# Contract: Guidance Planning & Rendering

**Modules**: `guidance`, `render`, `platform:sensors`, `platform:governor` | **Covers**: FR-031, FR-032, FR-042…FR-047, FR-050…FR-055, FR-060…FR-063, FR-070…FR-072, FR-081, FR-082, FR-090, US3–US8

## Interfaces (Kotlin, illustrative)

```kotlin
interface GuidancePlanner {                      // pure Kotlin, deterministic
    fun plan(input: GuidanceInput): GuidanceOutput
}

data class GuidanceInput(
    val t: Long,
    val progress: RouteProgress?,
    val perception: PerceptionFrame?,
    val pose: PoseSample,
    val vehicle: VehicleProfile,
    val signals: List<SignalDetection>,
    val previous: GuidanceOutput?,
    val flags: FeatureFlags,
    val config: GuidanceConfig,
)

data class GuidanceOutput(
    val mode: GuidanceMode,
    val path: GuidancePath?,            // null unless mode ∈ {FULL_AR, STRAIGHT_ONLY_AR}
    val style: GuidanceStyle,
    val hud: HudState,
    val laneChange: LaneChangePlan?,
    val confidence: Float,
    val reasons: Set<DegradeReason>,    // for diagnostics + indicators
)

interface GuidanceModeController {       // hysteresis FSM (plan.md)
    fun next(current: GuidanceMode, confidence: Float, vetoes: Set<Veto>, navState: NavState, tNanos: Long): GuidanceMode
}

interface OverlayRenderer {              // GL thread
    fun onCameraFrame(texture: SurfaceTexture, frameT: Long)
    fun submit(output: GuidanceOutput, pose: CameraPose, masks: PerceptionMasks)
    fun setHighContrast(enabled: Boolean)
}
```

## Planner rules

| # | Rule |
|---|------|
| G1 | **Determinism**: same `GuidanceInput` → same `GuidanceOutput` (no wall-clock, no randomness). Required for replay tests. |
| G2 | **Path containment**: in FULL_AR/STRAIGHT_ONLY_AR, ≥ 95% of path samples MUST lie inside `EgoCorridor` (cruise) or the validated drivable mask (turn curves). Otherwise downgrade the mode. |
| G3 | **Smoothness**: path tangent is continuous; \|curvature\| ≤ 1 / `vehicle.minTurnRadiusM`; no sample-to-sample heading jump > 10°. |
| G4 | **Turn display distance**: curve shown only when `distanceToManeuver` ≤ min(60 m, visible range), **and** the junction opening is matched (plan: junction matching). Else STRAIGHT_ONLY_AR + pointer/edge cue. |
| G5 | **Fork/ramp/flyover**: path drawn only on the branch whose bearing matches the route's exit bearing (≤ 35°), and `branchLabel` set. Ambiguous → no branch path, label only. |
| G6 | **U-turn**: requires a perceived median opening on the right within the window. The path goes from the right-most ego lane → arc → the opposite carriageway's left side. Otherwise pointer-only. |
| G7 | **Lane change**: start at max(150 m, 8 s × v) before the manoeuvre. S-curve length ≥ 3 s × v. Suppressed if remaining < max(40 m, 3 s × v) (`TOO_LATE`). Never targets a lane outside the ego carriageway. |
| G8 | **Signal colour**: only when `flags.signalColouring` is on, association holds, and temporal vote passes (R-11). Otherwise `NEUTRAL`. RED adds `stopBarAtZ`. Free-left → NEUTRAL for left turns. |
| G9 | **Pointer**: `pointerAngle = bearing(currentPos → lookAheadLatLng) − heading`, wrapped to (−π, π], smoothed (critically damped, ~250 ms). Look-ahead distance = clamp(50 m, 6 s × v, 150 m). |
| G10 | **Speed**: shown only if `speedAccuracy ≤ 1.5 m/s`. Over-limit is amber only (no sound, no flashing). |
| G11 | **Never-assert-safety**: no strings or glyphs implying safety/clearance (FR-055). Lint check on string resources. |

## Renderer rules

| # | Rule |
|---|------|
| R1 | One GL pass per display frame: camera quad → chevrons (instanced) → HUD/minimap. No intermediate CPU readbacks. |
| R2 | Chevron vertex positions come from road-plane samples through `CameraPose.H`, **late-warped** by the gyro rotation delta between the perception frame and the displayed camera frame. |
| R3 | Fragment discard where the ego/drivable mask = 0 or occluder mask = 1 (2 px feather). |
| R4 | Opacity, size, spacing and fade per research R-09. Total overlay coverage ≤ 20% of pixels: the renderer measures coverage on a low-res proxy and scales opacity down if exceeded. |
| R5 | Keep-clear: no chevrons nearer than 4 m or inside the bottom 12% of the screen. |
| R6 | Mode transitions fade (in 200 ms / out 300 ms). FALLBACK draws a screen-fixed arrow (top-centre, beside the pointer) and **no** road-plane chevrons. |
| R7 | HUD text ≥ 18 sp equivalent; colours meet ≥ 4.5:1 contrast against a translucent dark backing. |
| R8 | Minimap (flagged): circle bottom-left, heading-up, route polyline in vehicle frame, zoom radius = clamp(300 m, 25 s × v, 500 m). |
| R9 | Frame pacing: the renderer must not block on perception. Missing perception for > 300 ms → veto → FALLBACK. |

## Test obligations
- Unit (JVM): G1–G11 with table-driven scenarios (synthetic corridors, scripted progress).
- Golden-image tests (on device, offscreen GL): chevron placement for fixed homographies and masks; pixel diff tolerance.
- Replay metrics: SC-001 leakage, SC-006 mode latency, SC-009 lane cues, SC-010 jitter.
