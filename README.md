# Limitless Sight

Limitless Sight is an Android proof of concept for eyes-forward augmented-reality road navigation. A landscape phone camera acts as the future wearable display: on-device vision identifies road pixels, while a navigation provider will supply route intent so guidance can eventually be drawn on the correct carriageway.

> **Safety:** The current build is for stationary bench testing only. It is not safe or ready for use while driving or riding. Its live road overlay is experimental, and its chevrons are simulated rather than route-aware.

## Current POC

- Native Android app for Android 10+ (`app.limitlesssight.poc`)
- Camera2 lens discovery, selection, rotation correction, and calibration checks
- Xiaomi 14 Civi support with Camera 3 physical ultra-wide selected by default
- Full-field 4:3 landscape preview; narrow side bars are retained to avoid cropping
- On-device FFNet-78S-LowRes UINT8 road segmentation through LiteRT/NNAPI
- Confidence-gated road overlay and simulated guidance chevrons
- Private rolling diagnostics; no camera frames, audio, or location are recorded

## Architecture

```text
Camera2 selected lens
        |
        +--> TextureView live preview --------------------------+
        |                                                       |
        +--> latest-frame sampling --> LiteRT/NNAPI FFNet       |
                                      --> road mask             |
                                      --> connected road region |
                                      --> confidence gate       |
                                                              overlay

Future navigation provider --> manoeuvre + route progress --> guidance planner
Device pose/calibration ----> road-plane projection --------> perspective renderer
```

The camera/perception pipeline decides **where** road guidance may be drawn. The navigation-provider adapter will decide **what direction** to show and **when**. Keeping these responsibilities separate allows Google Navigation SDK or Mappls to be selected later without coupling routing to the vision model.

## Build

Requirements: Android SDK 35, JDK 17+, and Gradle 8.11.1.

```powershell
gradle :app:assembleDebug :app:lintDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

On the current Xiaomi test setup, ADB uses the isolated server on port `5038`:

```powershell
$env:ANDROID_ADB_SERVER_PORT = "5038"
adb devices
```

See [Specs/001-ar-road-navigation-poc/quickstart.md](Specs/001-ar-road-navigation-poc/quickstart.md) for bench validation and diagnostics collection.

## Verified status and limitations

- Camera 3 physical ultra-wide is approximately 99° horizontal FOV.
- FFNet-78S-LowRes runs at roughly 0.66–0.74 seconds per inference on the Xiaomi through NNAPI (about 1.4 FPS).
- The current model is trained on Cityscapes and is not sufficiently accurate for Bengaluru roads.
- Destination search, GPS route progress, rerouting, road-plane calibration, and route-aware chevrons are not implemented.
- Google Navigation SDK and Mappls both require credentials and commercial/terms review; no provider key is committed.

The detailed requirements and target architecture are in [Specs/001-ar-road-navigation-poc/](Specs/001-ar-road-navigation-poc/).
