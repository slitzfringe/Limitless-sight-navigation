# Limitless Sight — Specifications

Limitless Sight is a lightweight phone app that adds route guidance on top of the live camera view of the road ahead. The intended users are two-wheeler riders and car drivers in Bengaluru (Bangalore). Riders follow chevrons drawn on their own carriageway, inspired by the arrows in racing games, instead of looking at a map or listening to voice prompts. The long-term target is a wearable glass. In this proof of concept (POC), the phone screen stands in for the glass.

These artifacts follow the **GitHub Spec Kit** workflow: constitution → specify → clarify → plan → tasks → analyze → implement → converge. Nothing here authorizes a release, road testing on public roads without the safety protocol in [quickstart.md](./001-ar-road-navigation-poc/quickstart.md), or production API spending.

## Reading order

| # | Artifact | Purpose | Spec Kit equivalent |
|---|----------|---------|---------------------|
| 1 | [constitution.md](./constitution.md) | Non-negotiable project principles (safety, performance, privacy) | `.specify/memory/constitution.md` |
| 2 | [product-vision.md](./product-vision.md) | Problem, users, long-term roadmap (glass, hazards) | Product context |
| 3 | [001-ar-road-navigation-poc/spec.md](./001-ar-road-navigation-poc/spec.md) | **What & why** — user stories, requirements, success criteria | `/speckit-specify` + `/speckit-clarify` |
| 4 | [001-ar-road-navigation-poc/research.md](./001-ar-road-navigation-poc/research.md) | Decisions, alternatives, unknowns | `/speckit-plan` Phase 0 |
| 5 | [001-ar-road-navigation-poc/plan.md](./001-ar-road-navigation-poc/plan.md) | **How** — architecture, stack, budgets, constitution check | `/speckit-plan` |
| 6 | [001-ar-road-navigation-poc/data-model.md](./001-ar-road-navigation-poc/data-model.md) | Entities, state machines | `/speckit-plan` Phase 1 |
| 7 | [001-ar-road-navigation-poc/contracts/](./001-ar-road-navigation-poc/contracts/) | Internal module interfaces | `/speckit-plan` Phase 1 |
| 8 | [001-ar-road-navigation-poc/quickstart.md](./001-ar-road-navigation-poc/quickstart.md) | Build, bench and field validation | `/speckit-plan` Phase 1 |
| 9 | [001-ar-road-navigation-poc/tasks.md](./001-ar-road-navigation-poc/tasks.md) | Dependency-ordered implementation tasks | `/speckit-tasks` |
| 10 | [001-ar-road-navigation-poc/checklists/requirements.md](./001-ar-road-navigation-poc/checklists/requirements.md) | Requirements quality checklist | `/speckit-checklist` |

## Feature index

| ID | Feature | Status | Risk tier |
|----|---------|--------|-----------|
| 001 | AR road navigation POC (camera selection & calibration, route via free-tier provider, on-road chevrons, direction pointer, speed, lane pre-guidance; optional: signal colouring, minimap) | Draft — clarified 2026-11-22 | HIGH RISK (safety-of-life user context) |
| 002+ | Hazards (pedestrians, animals, potholes), wearable glass target, voice, offline maps | Not started — see [product-vision.md](./product-vision.md) | — |

## Using with the Spec Kit CLI

If the repository is later initialized with `specify init --integration copilot --script ps`:

1. Copy `constitution.md` to `.specify/memory/constitution.md`. Review and adopt it; do not regenerate it.
2. Keep the feature folder as `specs/001-ar-road-navigation-poc/`.
3. Continue from `/speckit-analyze` to check spec, plan and tasks for consistency, then `/speckit-implement`.

## Conventions

- `FR-xxx` = functional requirement, `NFR-xxx` = non-functional requirement, `SC-xxx` = success criterion, `US-x` = user story, `Txxx` = task.
- Success criteria and NFRs use the **Acceptance policy** in the spec: **safety gates** (hard pass/fail), **quality gates** (pass/fail, realistic for a POC), and **targets** (measured and reported, not blocking). Numbers inside FRs are **tunable defaults** in `GuidanceConfig`, not acceptance criteria.
- Decisions and answered questions are recorded in the spec's *Clarifications* and *Decision log* sections.
