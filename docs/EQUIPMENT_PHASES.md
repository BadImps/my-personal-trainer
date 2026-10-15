# Equipment-library production plan

## Scope

This is a movement catalogue, not a list of every manufacturer's SKU. A chest press, for example, may be selectorized, plate-loaded, converging, or iso-lateral; those are useful variants of the same primary movement pattern. The app will name the equipment style where it materially changes setup or movement, but will not create duplicate selections for brand labels.

The catalogue is built in four complete phases:

1. **Gym machines and non-dumbbell gym equipment**
2. **Dumbbells**
3. **Bodyweight**
4. **At-home equipment**

A movement only becomes selectable once it has its target area, safety/setup cue, concise description, Coach Mia poster and loop, Coach Leo poster and loop, and automated resource coverage. Generated source sheets may be GIF sequences during production, but the Android app ships only a compact poster and a local, 30-fps H.264 MP4 loop. This preserves the smooth in-app playback introduced in v2.5.

## Evidence used for the gym-machine phase

- [ACE's selectorized-machine library](https://www.acefitness.org/resources/everyone/exercise-library/equipment/weight-machines-selectorized/?page=%7B0%7D) separates common machine movements across chest, arms, shoulders, hips, calves, and core.
- [Life Fitness Insignia selectorized equipment](https://www.lifefitness.com/en-eu/catalog/strength-training/selectorized/insignia-series-pulldown) documents upper-body, lower-body, and torso machine families.
- [Life Fitness plate-loaded equipment](https://www.lifefitness.com/en-us/catalog/strength-training/plate-loaded) shows the distinct plate-loaded, Smith, squat, row, press, calf, and pulldown families found on many gym floors.
- [Precor Resolute selectorized equipment](https://www.precor.com/en-US/strength/selectorized/resolute) confirms hip, glute, thigh, calf, rear-delt/pec-fly, abdominal, rotary-torso, and back-extension stations.
- [Matrix strength equipment](https://world.matrixfitness.com/eng/strength) confirms the wider gym taxonomy: selectorized, multi-station, plate-loaded, racks/rigs, and functional cable training.

## Phase 1 — gym machines and equipment, excluding dumbbells

### Chest

| Movement | Equipment family | Primary area |
| --- | --- | --- |
| Seated chest press | Selectorized press | Chest / triceps |
| Incline chest press | Plate-loaded or selectorized press | Upper chest / shoulders |
| Pec deck fly | Selectorized fly station | Chest |
| Cable fly | Dual adjustable pulley | Chest |
| Assisted dip | Assist dip/chin station | Chest / triceps |

### Back and shoulders

| Movement | Equipment family | Primary area |
| --- | --- | --- |
| Lat pulldown | Selectorized pulldown | Lats / upper back |
| Seated cable row | Low cable or row station | Mid back |
| High row | Selectorized or plate-loaded row | Upper back |
| Low row | Plate-loaded or cable row | Lats / mid back |
| Machine pullover | Selectorized pullover | Lats |
| Assisted chin-up | Assist dip/chin station | Lats / biceps |
| Shoulder press | Selectorized or plate-loaded press | Shoulders / triceps |
| Lateral raise | Selectorized lateral-raise station | Side delts |
| Rear-delt fly | Reverse pec deck | Rear delts / upper back |

### Arms

| Movement | Equipment family | Primary area |
| --- | --- | --- |
| Biceps curl | Selectorized curl station | Biceps |
| Cable biceps curl | Cable station | Biceps |
| Triceps extension | Selectorized station | Triceps |
| Cable pressdown | Cable station | Triceps |

### Legs, glutes, and hips

| Movement | Equipment family | Primary area |
| --- | --- | --- |
| Leg press | Sled or selectorized press | Quads / glutes / hamstrings |
| Hack squat | Plate-loaded squat | Quads / glutes |
| Smith squat | Smith machine | Legs / glutes |
| Leg extension | Selectorized station | Quads |
| Seated leg curl | Selectorized station | Hamstrings |
| Lying leg curl | Selectorized station | Hamstrings |
| Standing or seated calf raise | Calf station | Calves |
| Hip abduction | Selectorized station | Outer hips / glute medius |
| Hip adduction | Selectorized station | Inner thighs |
| Glute drive | Hip-thrust / glute-drive station | Glutes |
| Glute extension | Standing glute station | Glutes |

### Core and integrated work

| Movement | Equipment family | Primary area |
| --- | --- | --- |
| Abdominal crunch | Selectorized crunch station | Abs |
| Rotary torso | Selectorized torso station | Obliques |
| Back extension | Selectorized back-extension station | Lower back |
| Cable wood chop | Dual adjustable pulley | Obliques / full body |
| Smith split squat | Smith machine | Legs / glutes |

## Phase 2 — dumbbells

Chest press, incline press, fly, pullover; one-arm row, chest-supported row, rear-delt fly; shoulder press, lateral raise; biceps curl, hammer curl, triceps extension; goblet squat, Romanian deadlift, split squat, step-up, hip thrust, calf raise; suitcase carry, weighted dead bug, Russian twist; squat-to-press, clean-to-press, and renegade row.

## Phase 3 — bodyweight

Push-up, incline and decline push-up, pike push-up, bench dip, pull-up, inverted row, squat, split squat, reverse lunge, walking lunge, single-leg hinge, glute bridge, calf raise, wall sit, dead bug, bird dog, plank, side plank, mountain climber, superman, burpee, jumping jack, cat-cow, 90/90 hip switch, and shoulder wall slide.

## Phase 4 — at-home equipment

This phase uses only safe, readily identifiable items: a stable chair/bench, a step, a backpack with a secured load, towel, resistance band, water bottles, and countertop. It does not recommend improvised unstable furniture or uncontrolled heavy household loads. Each movement names the required item explicitly, for example backpack floor press, chair-assisted split squat, countertop incline push-up, towel curl, backpack row, band pull-apart, bottle front raise, step-up, and backpack hip hinge.

## Media and release gates

For every selectable movement:

1. Coach Mia and Coach Leo each have a five-or-more-pose, same-camera source sequence.
2. The loop uses the full range of motion and returns without a hard end-to-start jump.
3. The final MP4 is 30 fps, H.264 baseline, muted, local, and paired with a compact poster.
4. A test resolves the poster and MP4 for both trainers.
5. Device QA checks the selected trainer, equipment, and visible movement all agree before the phase is merged into the library.

The first validated machine pairs are **Pec Deck Fly** and **Leg Extension** for both coaches. They are the reference quality bar for the remaining machine catalogue.
