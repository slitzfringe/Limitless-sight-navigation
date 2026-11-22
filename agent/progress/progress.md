# Project Progress

Last updated: 2026-11-22

## Completed

| Date | Milestone |
|---|---|
| 2026-11-22 | Created detailed product requirements, architecture, contracts, quickstart, and validation gates. |
| 2026-11-22 | Built the native Android stationary bench app with Camera2 lens enumeration and diagnostics. |
| 2026-11-22 | Established Xiaomi 14 Civi deployment through isolated ADB port 5038. |
| 2026-11-22 | Added Xiaomi rotation handling, true logical 0.6× access, three-camera deduplication, and Camera 3 default selection. |
| 2026-11-22 | Added full-field 4:3 preview, lens checks, simulated route scenarios, and confidence fallback. |
| 2026-11-22 | Added real FFNet semantic road segmentation with LiteRT, NNAPI, explicit CPU fallback, and runtime diagnostics. |
| 2026-11-22 | Benchmarked and rejected slower FFNet variants; selected FFNet-78S-LowRes W8A8 at approximately 1.4 FPS. |
| 2026-11-22 | Corrected the preview to use an upright wide landscape transform, raised the target stream to 1920×1440, and removed the in-app logo. |
| 2026-11-22 | Passed Android build and lint, installed the update, and verified Camera 3 at 1920×1440 with first-frame delivery on the Xiaomi. |
| 2026-11-22 | Added repository and agent-state documentation. |
| 2026-11-22 | Added a conservative center-biased ego-corridor candidate inside the detected road region; guidance remains explicitly simulated. |

## Pending

- Reach or approach the proposed 10 FPS perception target using Qualcomm QNN or a smaller INT8 model.
- Fine-tune and evaluate perception for Bengaluru/Indian roads.
- Derive the ego carriageway, kerbs, lane boundaries, and left-side traffic corridor.
- Add camera calibration and perspective road-plane projection.
- Securely configure Google Navigation SDK or Mappls and implement destination search and route progress.
- Replace simulated chevrons with route-aware, confidence-gated guidance.
- Complete replay safety gates before any closed-road or supervised field test.
