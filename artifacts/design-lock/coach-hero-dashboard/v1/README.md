# Coach Hero Dashboard v1 — approved visual reference

These eight source images are the approved visual contract for the signed-in Coach Hero
Dashboard when Coach Mia is selected. They are intentionally kept outside the installable APK:
they are comparison references, not static screen replacements.

| Screen | Reference |
| --- | --- |
| Home | `01-home-coach-mia.png` |
| Workouts | `02-workouts-coach-mia.png` |
| Progress | `03-progress-coach-mia.png` |
| Nutrition | `04-nutrition-coach-mia.png` |
| Endurance | `05-endurance-coach-mia.png` |
| Scheduler | `06-scheduler-coach-mia.png` |
| Body | `07-body-coach-mia.png` |
| Settings | `08-settings-coach-mia.png` |

## Locked rules

- This is one dashboard system, not eight unrelated visual treatments.
- When Coach Mia is selected, every in-app coach image must be Mia. A different saved coach
  changes only the portrait asset and coach name; it must not change layout, copy hierarchy,
  spacing, palette, or navigation.
- The signed-out Full Hero Coach login remains a separate locked template governed by
  `docs/DESIGN_LOCK.md`.
- Main dashboard pages use the Coach Hero header and one labelled bottom navigation system.
- Rebuilds may not use these images as non-interactive full-screen placeholders. Functional
  native controls remain required for plans, settings, tracking, reminders, and data changes.

## Target and validation

The source artwork is a portrait Android composition. Before release, capture the same signed-in
state on the selected Android device profile and compare it against the matching source above.
Record any responsive-only adjustment in `docs/DESIGN_LOCK.md` before merging it.
