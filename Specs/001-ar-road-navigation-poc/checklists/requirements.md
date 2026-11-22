# Requirements Quality Checklist: AR Road Navigation POC

**Purpose**: check that [spec.md](../spec.md) is complete, unambiguous and testable before implementation (Spec Kit `/speckit-checklist`).
**Created**: 2026-11-22
**Note**: a checked item means the *requirement text* is adequate. It is **not** evidence that the behaviour is implemented or safe.

## Content quality

- [x] CHK001 User stories are prioritized (P1–P3) and each is independently testable.
- [x] CHK002 Spec focuses on *what/why*. Technology choices are confined to plan/research (the spec mentions providers only in Assumptions).
- [x] CHK003 Scope and explicit exclusions are stated (hazards, iOS, glass hardware, voice, offline routing).
- [x] CHK004 All mandatory sections present (User Scenarios, Requirements, Success Criteria, Assumptions).

## Requirement completeness

- [x] CHK010 Camera enumeration covers physical lenses of logical multi-cameras (FR-010) and the OEM-restriction case (E14).
- [x] CHK011 Calibration checks are enumerated and each has a measurable method (FR-011, research R-03).
- [x] CHK012 Left-hand-traffic ego-carriageway rules cover divided, undivided-with-centre-line, wide undivided, narrow roads (FR-041).
- [x] CHK013 Never-draw zones are explicit: footpath, median, oncoming, near-field, beyond range (FR-041/045, R-07).
- [x] CHK014 Turn, fork/ramp/flyover, U-turn and roundabout behaviours are specified (US3-5/6/7, E8).
- [x] CHK015 Lane pre-positioning timing and too-late suppression are specified (FR-070…072).
- [x] CHK016 Degradation triggers, modes, hysteresis and indicators are specified (FR-050/051, plan FSM).
- [x] CHK017 Signal colouring defines the "uncertain ≠ green" rule, arrow-signal movement mapping, free-left and police-controlled cases (US7, FR-081).
- [x] CHK018 Interaction-while-moving restrictions are specified (FR-023, Constitution I.5).
- [x] CHK019 Privacy: frame handling, recording consent, location sharing scope (Constitution III, NFR-011).
- [x] CHK020 Navigation provider decided: Google Navigation SDK within free caps + MapLibre/OSM free fallback; ₹0 guard specified (Clarifications Q1, FR-025, NFR-014).
- [x] CHK021 Acceptance policy decided: safety gates / quality gates / reported targets; FR numbers are tunable defaults (Clarifications Q2).
- [x] CHK022 Signal colouring is optional and outside POC acceptance (Clarifications Q3).
- [ ] CHK023 Legal counsel confirmation of Indian navigation-device rules and app safety wording (research R-16).
- [ ] CHK024 Licence register complete with commercial-use status for datasets/models (T006).

## Requirement clarity & measurability

- [x] CHK030 Every SC has a numeric threshold, a dataset/environment, and a measurement method (quickstart §8).
- [x] CHK031 Every SC and NFR is classified as safety gate, quality gate or target, with both gate and target values (spec → Acceptance policy).
- [x] CHK034 Zero-cost constraint is testable: Cloud billing report = ₹0, usage guard unit tests (NFR-014, T034a, T036a).
- [x] CHK032 Edge cases E1–E18 each map to an expected behaviour.
- [x] CHK033 SC-004 (glance reduction) classified as a target; the POC deliverable is the study protocol + pilot.

## Consistency

- [x] CHK040 Every FR maps to at least one task (tasks.md traceability table).
- [x] CHK041 Contracts reference the FRs they implement.
- [x] CHK042 Constitution Check in plan.md passes, or deviations are tracked (licensing ⚠️).
- [ ] CHK043 `/speckit-analyze` run after the first implementation increment.

## Safety review (high-risk tier)

- [x] CHK050 Failure modes that could show wrong guidance (wrong junction, wrong branch, footpath leakage, false green) each have a mitigation and a success criterion (SC-001/002/003/008).
- [x] CHK051 Field testing requires a safety operator and an abort criterion (quickstart §7).
- [ ] CHK052 Independent safety review of the plan by someone other than the author — **pending reviewer assignment**.
