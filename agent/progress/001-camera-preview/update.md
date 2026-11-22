# Update 001: Camera Preview and Repository State

Date: 2026-11-22

## Requested outcome

- Make the app preview use the available landscape region like Xiaomi Camera without losing field of view.
- Remove the Limitless Sight logo from inside the app.
- Add a concise GitHub README with architecture.
- Establish persistent agent progress and knowledge records.

## Changes

- Changed the Xiaomi default preview rotation offset from 270° to 0° so the 4:3 stream is presented upright in its wide landscape orientation.
- Raised the preferred 4:3 preview target from 1280×960 to 1920×1440 for better displayed detail.
- Retained fit-center scaling. A 4:3 camera image cannot fill a wider display without either cropping field of view or distorting pixels, so narrow side bars remain by design.
- Removed the in-app banner logo. The launcher icon remains unchanged.
- Added the root `README.md`.
- Added the `agent/progress` state hierarchy and startup instructions.

## Validation

An initial 180° build filled the region correctly but the device screenshot showed
the image upside down. The corrected 0° build retains the wide 4:3 presentation
while restoring the upright orientation.

- `:app:assembleDebug`: passed.
- `:app:lintDebug`: passed.
- APK installation through isolated ADB port 5038: passed.
- Device launch: passed with no fatal Android or camera error.
- Diagnostics confirmed Camera 3 (`0/3`), 1920×1440, approximately 99.44° FOV,
  default offset 0°, preview configured, and first frame delivered.
- Device screenshot confirmed the in-app logo is absent and the preview uses the
  broad 4:3 area with only the expected narrow side bars.
