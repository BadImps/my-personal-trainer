# Achievement + Apex Design Pass

Generated 2026-10-20 and updated 2026-10-28. The system is local-first and free: it has no
subscription gate or paid unlock. Verified members may opt into the Gym Lobby, which publishes
only their display name, level, Momentum Points, Apex lane, and earned badge names for friendly
private-lobby rankings; the underlying activity history remains on-device.

## Design decision

Achievements recognize completed activity, guided time, training variety, endurance sessions,
and repeatable activity rhythm. They do not award body-weight loss, calorie restriction, pain
tolerance, skipped recovery, or excessive workout volume. Missing a day never removes points.

This follows the practical implication of Self-Determination Theory: support autonomy,
competence, and sustainable participation rather than pressure. Evidence for fitness-app
gamification is positive but modest, and overly feature-rich systems can lead to burnout.

## What is built

- **Apex page:** a first-class navigation section immediately before Settings.
- **Momentum level:** local points from completed sessions, guided minutes, endurance, and focus
  variety. A level never decreases.
- **Achievement families:** Consistency, Guided Time, Rhythm, Explorer, and Endurance.
- **Apex tiers:** Foundation, Momentum, Performance, and Apex. They reflect repeatable sessions
  across a time window, not a mandatory intensity target.
- **Coach link:** Foundation coaches remain available. Existing coach-card requirements continue
  to use completed session counts; no coach requires payment or an unsafe challenge.

## Rules of the system

1. Completing a saved workout is the only way to add a completed-session achievement.
2. Activity is calculated from already local `JourneyStore` records.
3. Rest, skipped days, emergency stops, food logging, and body measurements do not subtract XP.
4. Apex is optional: regular workout planning and every standard exercise remain accessible.
5. The app presents progress as guidance, never as a health diagnosis or a reason to train through
   illness, injury, pain, dizziness, or unusual shortness of breath.

## Sources reviewed

- [Digital-health gamification systematic review and meta-analysis](https://pmc.ncbi.nlm.nih.gov/articles/PMC11701442/)
  — supports only modest benefit on average, so badges remain secondary to practical training.
- [Self-determination theory and physical activity review](https://www.sciencedirect.com/science/article/pii/S1469029225000780)
  — supports autonomy and competence as motivation mechanisms.
- [Exercise adherence and gamification feature richness](https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2025.1671543/full)
  — informs the deliberately small, grouped badge catalogue rather than an overwhelming feed.
- [Google Play health-content policy](https://support.google.com/googleplay/android-developer/answer/16679511)
  — informs local-only, transparent handling of sensitive fitness context.
