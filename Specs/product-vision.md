# Limitless Sight — Product Vision

## 1. Problem statement

Riders and drivers in Bengaluru usually navigate with Google Maps on a phone mounted on the handlebar or dashboard, plus voice prompts. This is dangerous and often fails:

- **Attention loss**: every map glance takes the eyes off a dense, unpredictable mixed-traffic road with two-wheelers, autos, buses, pedestrians and animals.
- **Voice is unreliable**: helmets, traffic noise and honking mask prompts, and instructions like "in 200 m keep left" are hard to map onto the actual road.
- **Parallel-road confusion**: Bengaluru has many places where a flyover, a service road and a main road (or an underpass) run side by side and then split. Examples: Hebbal flyover, Silk Board junction, the Electronic City elevated expressway, Outer Ring Road service roads, KR Puram, and Mekhri Circle underpass. Riders often realize too late which one they should be on.
- **Late lane positioning**: drivers learn about a turn too late and cut across lanes, a known accident cause.

## 2. Vision

> **Put the route on the road.** Draw guidance chevrons directly on the user's own carriageway in the live camera view, the way racing games show the racing line. Draw them so the user never has to look away from the road.

Inspiration (visual references supplied by the product owner):

| Reference | What we take from it | What we avoid |
|-----------|---------------------|---------------|
| Need for Speed: Underground 2 / Most Wanted / Carbon | Neon chevrons that clearly signal a turn direction; minimap with route line; speed readout | Large opaque wall-of-arrow barriers that block the scene |
| Forza Horizon (drivatar line / drag-race chevrons) | Translucent chevrons **flat on the road surface**, following the curve of the road, fading with distance; checkpoint distance at the top | Racing-line colour semantics tied to braking (not applicable) |

## 3. Target users (POC)

| Persona | Vehicle | Mount | Key need |
|---------|---------|-------|----------|
| Commuter rider | Scooter/motorcycle (most Bengaluru trips) | Handlebar phone mount, landscape | Clear turns and flyover/service-road choice without looking down |
| Car driver | Hatchback/sedan | Dashboard/windshield mount, landscape | Lane positioning before turns, signal awareness |
| Delivery/ride-hailing rider | Two-wheeler | Handlebar mount | Many unfamiliar destinations per day; low battery and data usage |

## 4. Roadmap (indicative, not committed)

| Phase | Theme | Highlights |
|-------|-------|-----------|
| **001 — POC (this spec)** | Phone-as-glass AR navigation | Camera selection & calibration; destination & route; on-road chevrons on the ego carriageway; top direction pointer; speed; lane pre-guidance; signal-aware colouring (stretch); minimap (stretch) |
| 002 | Hazard awareness | Pedestrian/animal crossing alerts, pothole and speed-breaker warnings, wrong-way vehicle cues |
| 003 | Robustness | Night/rain models, offline routing, battery-saver mode, Kannada/Hindi UI, low-end device tier |
| 004 | Wearable glass | Split architecture: glass for camera + display + speaker, phone or compute puck for inference. Optical see-through calibration (no video passthrough), eye-box alignment |
| 005 | Community & data | Opt-in anonymized road-condition reporting, junction difficulty learning |

## 5. Guiding product principles

1. **Glanceless**: the most important information sits in the scene itself (on the road), not in a corner panel.
2. **Calm**: translucent, minimal, animated only when it carries meaning (e.g., flow speed matches vehicle speed).
3. **Honest**: when unsure, show less and say so.
4. **Local**: designed for left-hand traffic, two-wheeler-heavy mixed traffic, Bengaluru junction patterns, free-left turns, median U-turn openings, and signal countdown timers.
