# Knowledge 001: Camera Preview

Date: 2026-11-22

## What was observed

The Xiaomi Camera app showed the ultra-wide image as a broad 4:3 landscape frame. Limitless Sight showed the same scene in a narrow 3:4 region with large empty areas on both sides. An even orientation made the camera region substantially wider.

## What was tried previously

- A 1280×720 16:9 stream reduced visible sensor area and did not match the camera app.
- A 1280×960 4:3 stream preserved more field of view.
- Center-crop scaling filled the view but discarded image edges, so it was rejected.
- Fit-center scaling preserved the entire frame but exposed that the Xiaomi default 270° transform was treating the stream as portrait.
- A Xiaomi-specific 270° offset made the scene appear upright in earlier testing, but produced the narrow 3:4 presentation visible in the latest screenshots.

## Current decision

Use the physical ultra-wide Camera 3, a 4:3 stream near 1920×1440, a 0° default offset, and fit-center scaling.

This combination was selected because it:

- presents the sensor frame in wide landscape;
- preserves all horizontal and vertical camera content;
- improves preview detail without digital zoom;
- avoids geometric distortion;
- leaves only the narrow bars required by the difference between the 4:3 camera frame and the wider app region.

The camera is not inherently a “horizontal camera.” Camera sensors expose a native orientation through Camera2 metadata, and the app must transform that buffer for the current display rotation. Rotation itself does not reduce capture quality; any perceived loss was mainly caused by enlarging a low-resolution 1280×960 preview, which is why the target was raised. A device check of the first fix proved that 180° produced the desired width but left the image upside down, so 0° was selected as the upright wide transform.

## Rejected alternatives

- **Stretch to fill:** rejected because it distorts the scene and would corrupt perception geometry.
- **Center crop:** rejected because it removes field of view, directly conflicting with the requirement.
- **Keep the 270° default:** rejected because it presents the 4:3 stream as 3:4 and wastes most of the available landscape width.
- **Use Xiaomi Camera output directly:** unavailable through standard Android APIs; OEM camera processing and UI are proprietary to the system camera app.
