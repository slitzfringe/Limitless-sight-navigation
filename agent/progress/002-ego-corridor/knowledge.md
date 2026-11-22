# Knowledge 002: Ego-Road Corridor

## Why this step was selected

The prior overlay treated every bottom-connected road-like pixel as an
eligible guidance surface. That is too broad for the product goal because a
segmentation model can include a shoulder, opposite carriageway, wall, or
other connected region. A narrower interior corridor is a safer intermediate
rendering policy while the project lacks lane boundaries and calibrated road
geometry.

## Current implementation

`RoadOverlayView` computes `corridorLeft` and `corridorRight` from each
detected row. The midpoint is blended 75% toward the detected center and 25%
toward the image center. The corridor occupies about 56% of the detected
width, clipped to the detected bounds. The original road region remains
visible as a diagnostic mask, while the corridor receives a blue outline and
the simulated chevrons use its midpoint.

## Rejected alternatives

- Drawing guidance across the full segmentation region was rejected because it
  can include more road than the user should follow.
- Calling the corridor a lane was rejected because no lane detector or
  calibration exists.
- Adding navigation-dependent curvature was rejected because route-provider
  integration is not implemented and the current chevrons remain simulated.

## Next validation

Use stationary recordings or replay frames with a road, shoulder, footpath,
and parallel branch. Check that the corridor remains inside the detected
region and that low-confidence hysteresis still hides all guidance.
