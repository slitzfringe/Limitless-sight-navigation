# Contract: Camera & Calibration

**Modules**: `feature:camera`, `feature:calibration` | **Covers**: FR-010…FR-016, US1, Edge E10/E14/E15

## Interfaces (Kotlin, illustrative)

```kotlin
interface CameraEnumerator {
    /** All rear-facing lenses incl. physical lenses of logical multi-cameras, ordered by focal length ascending. */
    fun listRearCameras(): List<CameraDescriptor>
}

interface CameraSessionController {
    /** Opens the given lens with a locked configuration; emits frames for display (OES) and processing (YUV). */
    suspend fun open(desc: CameraDescriptor, config: CaptureConfig): CameraSession
}

interface CameraSession : AutoCloseable {
    val displayTexture: SurfaceTexture
    val frames: Flow<ProcessingFrame>          // YUV_420_888 at processingSize, timestamped (sensor timestamp → monotonic)
    val stats: StateFlow<CameraStats>          // measured fps, dropped frames, exposure, ISO
}

data class CaptureConfig(
    val processingSize: Size = Size(640, 360),
    val targetFps: Int = 30,
    val focusInfinity: Boolean = true,
    val maxExposureNs: Long = 8_333_333,       // 1/120 s day profile; relaxed at night
    val videoStabilization: Boolean = false,   // MUST be false (geometry)
    val allowOis: Boolean = true,
)

interface CalibrationRunner {
    fun run(desc: CameraDescriptor, vehicle: VehicleProfile): Flow<CalibrationProgress>   // live check updates
}

interface CalibrationScorer { fun score(checks: List<CalibrationCheck>, desc: CameraDescriptor): Int }

interface CalibrationRepository {
    suspend fun save(profile: CalibrationProfile)
    suspend fun find(deviceModel: String, mount: MountType): CalibrationProfile?
}

interface QuickRecheck {
    /** ≤ 3 s. Returns PASS if |Δpitch|, |Δroll| ≤ 3° and the road is visible. */
    suspend fun verify(profile: CalibrationProfile): RecheckResult
}
```

## Rules

| # | Rule |
|---|------|
| C1 | Enumeration MUST include physical lenses when `getPhysicalCameraIds()` is non-empty and the physical stream can be configured. Otherwise the lens is reached via zoom ratio and `canLockLens = false` (warning shown). |
| C2 | Labels: smallest focal → `ULTRA_WIDE` if hFOV ≥ 100°; the camera reported as default back camera → `MAIN`; longer focal than main → `TELE`. |
| C3 | During navigation the session MUST NOT change lens, zoom, or stabilization. Any OEM-forced change triggers a `LensChanged` event → Fallback mode + recalibration prompt. |
| C4 | Calibration MUST NOT pass unless the device is landscape with \|roll\| ≤ 5° and the horizon lies in the configured band. |
| C5 | Score weights (research R-03) are configuration, not code constants. They are versioned with the profile. |
| C6 | "Test all cameras" runs lenses sequentially (never concurrently) to avoid thermal bias. A 2 s cool-down between lenses. |
| C7 | Intrinsics MUST be scaled to the processing size. Distortion is used in shaders when \|k1\| > 0.02. |
| C8 | Profiles are invalidated when the app's model version changes the latency/road-visibility scoring. Quick re-check catches mount changes. |
| C9 | Calibration report text MUST explain the recommendation in one sentence (e.g., "Main camera: sharper image and 2× faster processing than Ultra-wide"). |

## Test obligations
- Unit: scorer, label inference, FOV computation, profile validation.
- Instrumented on reference devices: enumeration snapshot, lens lock across 10 min, sustained fps.
- Device quirk table maintained in `feature:camera/quirks.json`.
