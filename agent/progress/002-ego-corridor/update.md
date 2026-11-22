# Update 002: Conservative Ego-Road Corridor

Date: 2026-11-22

## Request

Continue the next product step after reviewing the camera presentation and
smart-glasses positioning. The immediate goal is to move the bench POC toward
the useful product claim: helping identify the road surface to follow.

## Change

`RoadOverlayView` still retains the complete bottom-connected segmentation
region for diagnostics, but now derives a narrower, center-biased corridor
inside that region. The simulated chevrons are rendered only inside this
corridor. The overlay label explicitly calls it an **ego corridor candidate**.

The corridor is intentionally conservative:

- it uses the detected road bounds at each row;
- it biases the center 25% toward the image center;
- it uses approximately 56% of the detected row width;
- it never extends outside the detected region;
- it remains behind the existing confidence and hysteresis gate.

## Evidence status

This is a stationary bench heuristic, not lane detection or a safety result.
It has not been validated on Bengaluru roads, moving vehicles, intersections,
or route-aware navigation. Build and lint must pass before installation.
