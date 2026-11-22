# Limitless Sight — Project Constitution

**Version**: 1.0.0 | **Ratified**: _pending product-owner sign-off_ | **Last amended**: 2026-11-22

This constitution sets the principles that every Limitless Sight feature spec, plan, task and code change must satisfy. When a principle conflicts with a feature request, the principle wins unless an explicit amendment is recorded (see *Governance*).

---

## Core Principles

### I. Eyes on the road (safety first) — NON-NEGOTIABLE

1. The product exists to **reduce** driver/rider distraction. Any feature that increases glance time, cognitive load or occlusion of the real scene is rejected.
2. Overlays MUST NOT hide hazards. Translucency, the maximum share of the screen covered, and keep-clear zones are mandatory, configurable only within safe bounds, and never removable entirely.
3. **Fail safe, never fail confident.** When perception, positioning or routing confidence drops below the threshold, the app MUST degrade to simpler, clearly marked guidance (screen-fixed arrow + distance). It MUST NOT draw on-road guidance it cannot place correctly.
4. Guidance is **advisory**. The app MUST never imply that a manoeuvre is safe or legal. Examples: a green overlay never means "go", and a lane-change prompt never means "the lane is clear". Traffic rules, signals, police instructions and the user's own judgement always take precedence.
5. No text input, menus or multi-step interactions while the vehicle is moving (speed > 5 km/h, default). Only "stop/pause navigation", "mute" and "recenter" stay available, as large touch targets.
6. India-specific traffic conventions apply: **left-hand traffic**. Guidance MUST keep to the user's own carriageway (left side) and MUST NEVER be drawn on footpaths, medians, or oncoming lanes.

### II. Lightweight, efficient, fast

1. All perception (camera understanding) runs **on-device**. No camera frames are streamed to a server for inference.
2. Every feature declares a frame-time, memory and battery budget in its plan. A change that breaks a budget on the reference mid-range device needs a recorded exception.
3. Adapt rather than crash: under thermal throttling or low battery, the app lowers inference rate and resolution in documented steps and tells the user when quality is reduced.
4. Install size is kept minimal: models are quantized, unused native ABIs are stripped, and dependencies are added only with a size impact note.

### III. Privacy by design (India DPDP Act 2023 aware)

1. Camera frames are processed in memory and discarded by default. Recording is opt-in, developer/test-mode only, stored locally, and clearly indicated on screen.
2. Location is shared only with the selected navigation provider, and only for routing/navigation purposes. Destinations and history stay on-device unless the user exports them.
3. No account is required for this app. Third-party terms and licences, such as Google Maps Platform terms and OpenStreetMap (ODbL) attribution, are shown and accepted where required.
4. Secrets (API keys) are never committed to source control. Keys are restricted by package name and signing certificate.

### IV. Provider independence

Navigation/routing, map display and ML models sit behind internal interfaces (contracts). Swapping Google Navigation SDK for the MapLibre/OpenStreetMap provider, or swapping one segmentation model for another, MUST NOT require changes to rendering or UI code.

### V. Testable, measurable behaviour

1. Every functional requirement maps to at least one acceptance scenario and one verification method: unit, replay (recorded drive), bench, or supervised field test.
2. Perception quality is measured on a **versioned Bengaluru evaluation set** (recorded drives with labels). Field anecdotes don't count as evidence.
3. Simulation and replay come first. Any new perception or guidance logic MUST pass replay tests on recorded drives before supervised road testing.

### VI. Honest evidence

1. A passing checklist, a mocked test or an AI-generated summary is not proof of road safety or real-world accuracy. Results are labelled *bench*, *replay*, *supervised field*, or *not verified*.
2. Road testing follows the safety protocol: device mounted, a passenger or safety operator interacts with the device, legal compliance, no testing on high-risk stretches without a plan.

### VII. Simplicity and incremental delivery

Build the smallest end-to-end slice first: camera → calibration → route → on-road chevrons. Advanced features such as signal colouring, minimap and hazards are added only after the core slice meets its acceptance criteria. Each user story must be independently demonstrable.

---

## Additional Constraints

| Area | Constraint |
|------|-----------|
| Platform (POC) | Android first (largest share of the Bengaluru market; multi-camera enumeration via Camera2). iOS is out of scope for the POC. |
| Orientation | Landscape only during navigation (camera held/mounted horizontally). |
| Locale | English UI for the POC; Kannada/Hindi strings planned (externalized from day one). Units: metric (km/h, m, km). |
| Legal | The app must not encourage handheld phone use while driving. It must be used mounted. Final wording to be reviewed by legal counsel. |
| Licensing | Every ML model, dataset and SDK must have a recorded licence compatible with commercial distribution before release. Research-only assets may be used for internal benchmarks only. |

## Development Workflow & Quality Gates

1. **Spec gate**: user stories, FRs, edge cases and success criteria reviewed by product owner + tech lead.
2. **Plan gate**: constitution check passes (see each plan), budgets declared, provider licensing confirmed.
3. **Implementation gate**: unit + replay tests green; performance budgets measured on the reference devices.
4. **Field gate**: supervised field test on agreed Bengaluru routes with evidence retained (screen recordings, logs, metrics).
5. **Release gate**: legal/privacy review, provider terms compliance, store policy review.

## Governance

- This constitution supersedes conflicting guidance in feature artifacts.
- Amendments require: written rationale, impact analysis on existing specs, product-owner approval, and a version bump (MAJOR for removed/redefined principles, MINOR for added principles, PATCH for clarifications).
- Every plan includes a *Constitution Check* table. Violations must be justified in *Complexity Tracking*, or the plan is rejected.
