# Contract: PerceptionEngine

**Modules**: `perception:api`, `perception:litert`, `perception:postprocess` | **Covers**: FR-040, FR-041, FR-050 (inputs), FR-080, US3, US4, US7

## Interfaces (Kotlin, illustrative)

```kotlin
interface PerceptionEngine : AutoCloseable {
    val modelInfo: ModelInfo                       // name, version, sha256, input size, classes, licence id
    suspend fun warmUp()
    /** Non-blocking submit; engine drops stale frames (single-slot queue). */
    fun submit(frame: ProcessingFrame, pose: CameraPose)
    val results: SharedFlow<PerceptionFrame>
    fun setRate(hz: Int, processingSize: Size)     // governor control
}

interface EgoCorridorEstimator {                   // pure Kotlin
    fun estimate(bev: BevGrid, lanes: List<LaneLine>, vehicle: VehicleProfile, previous: EgoCorridor?): EgoCorridor?
}

interface SignalPerception {                       // stretch, feature-flagged
    fun submit(frame: ProcessingFrame, pose: CameraPose, egoPath: GuidancePath?)
    val detections: SharedFlow<List<SignalDetection>>
}
```

## Model I/O contract (segmentation)

| Item | Value |
|------|-------|
| Input | RGB, 512×288 or 640×360 (config), float16 or uint8 normalized per `ModelInfo` |
| Output | Logits or softmax `[H/k, W/k, C]`, C = 9 classes in fixed order: `0 other, 1 road, 2 footpath, 3 kerb_median, 4 lane_marking, 5 vehicle, 6 person, 7 animal, 8 barrier` |
| Runtime | LiteRT GPU delegate FP16. CPU XNNPACK fallback (reduced rate) |
| Size | ≤ 8 MB |
| Latency | ≤ 15 ms p95 mid-range (budget, plan) |
| Metadata | Embedded TFLite metadata: version, class map, normalization, licence id |

## Rules

| # | Rule |
|---|------|
| P1 | Results carry the **capture** timestamp of their input frame, not the completion time. |
| P2 | Temporal smoothing MUST be ego-motion-compensated (IMU yaw + speed) before blending. No blending across a lens change or calibration change. |
| P3 | `EgoCorridor.rule` MUST follow research R-07 precedence: MEDIAN > CENTRE_LINE > (FULL_NARROW \| LEFT_PORTION). LANE refines inside the chosen carriageway. |
| P4 | The estimator MUST never return a corridor that overlaps footpath/kerb/median cells by > 2% of its area. If it cannot satisfy this, it returns `null` (→ low confidence). |
| P5 | Footpath vs road confusion: cells with road confidence < 0.6 adjacent to footpath are excluded from the corridor (conservative edge). |
| P6 | Occluder mask = classes {vehicle, person, animal, barrier}, dilated by 2 px at mask resolution. |
| P7 | `quality` flags MUST be raised for low light (mean luma < threshold), blur (Laplacian variance drop), obstruction (> 40% of lower half non-road non-object with low confidence). |
| P8 | The signal detector MUST output `associatedToEgo = false` unless all association conditions in research R-11 hold. |
| P9 | Model swaps require: updated `ModelInfo`, eval report on the frozen Bengaluru set, licence register entry. |

## Test obligations
- Golden-mask unit tests for `EgoCorridorEstimator` (synthetic BEV grids: divided, undivided with/without centre line, narrow lane, footpath adjacent, bus occluding).
- Offline eval (Python): mIoU, ego-corridor IoU, leakage metric (SC-001), signal false-green (SC-008).
- On-device benchmark: latency p50/p95 per reference device and delegate.
