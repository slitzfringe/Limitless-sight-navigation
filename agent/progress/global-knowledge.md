# Global Knowledge

Last updated: 2026-11-22

## Product and safety

Limitless Sight is an Android AR road-navigation POC for a future wearable display. The current build is strictly a stationary bench application. Live chevrons are simulated and must not be used for navigation or while controlling a vehicle.

## Current hardware and build

- Test device: Xiaomi 14 Civi, model `24053PY09I`, Android serial `43df59ad`.
- Preferred lens: Camera 3, physical ultra-wide `0/3`, approximately 99° horizontal FOV.
- Android package: `app.limitlesssight.poc`, version `0.1.0-bench`.
- Android SDK 35, minimum SDK 29, Gradle 8.11.1, Android Gradle Plugin 8.9.2, JDK 17+.
- Device access uses an isolated ADB server on port `5038`.

## Current architecture

1. Camera2 enumerates logical and physical rear lenses and opens a full-field 4:3 preview.
2. `TextureView` displays the live camera while preserving the complete frame.
3. A latest-frame-only worker samples the preview for FFNet-78S-LowRes.
4. LiteRT 1.4.2 attempts NNAPI first and explicitly falls back to CPU if initialization fails.
5. Cityscapes class `0` is extracted as a road mask.
6. Bottom-connected filtering, confidence scoring, and hysteresis produce a visible road region.
7. `RoadOverlayView` retains the detected region for diagnostics, derives a
   conservative center-biased ego-corridor candidate inside it, and draws
   simulated straight chevrons only within that candidate.
8. A future navigation-provider adapter will supply route intent independently from perception and rendering.

## Known facts and constraints

- Xiaomi exposes three app options: logical 0.6×, physical main, and physical ultra-wide.
- The preview targets 1920×1440 and uses a wide landscape transform. Narrow side bars are intentional: filling a wider screen without bars would crop a 4:3 sensor frame.
- FFNet-78S-LowRes UINT8 uses input `[1,512,1024,3]` and output `[1,128,256,19]`.
- Measured NNAPI inference is approximately 662–735 ms, about 1.4 FPS; the proposed perception target is at least 10 FPS.
- Cityscapes does not adequately represent Bengaluru roads; false road classifications have been observed.
- Routing, destination search, GPS progress, road-plane projection, and route-aware chevrons are not implemented.
- Provider credentials must never be committed.
- The ego-corridor candidate is a rendering heuristic only; it is not lane
  detection, route guidance, or a safety gate.

## Validation workflow

Build and lint:

```powershell
gradle :app:assembleDebug :app:lintDebug
```

Install and inspect the device through the isolated server:

```powershell
$env:ANDROID_ADB_SERVER_PORT = "5038"
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am force-stop app.limitlesssight.poc
adb shell monkey -p app.limitlesssight.poc 1
```

Collect private rolling logs with `tools\Collect-Diagnostics.ps1` using the same ADB server environment.
