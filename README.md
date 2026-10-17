# My Personal Trainer

## 1.4 animated exercise library

The guided workout and exercise library now play coach-specific, looping three-phase GIF demonstrations for every prescribed movement, including the no-equipment substitutions. Coach Mia and Coach Leo each have their own animation set. A Machines category adds Machine Chest Press, Lat Pulldown, Leg Press, and Seated Cable Row. These are form-reference animations; follow the written cue, use a comfortable range, and stop if anything hurts.

## 1.5 structured training flow

My Personal Trainer now uses an original five-area layout: Today, Plans, Library, Progress, and Profile. It follows the familiar journey of choosing a goal and training setup, following a guided session, reviewing exercises, and tracking progress—without copying another product's branding, screens, text, or assets.

An independent, offline Android fitness coach. It builds a guided workout from a chosen goal and focus, gives movement cues and spoken rep guidance, runs a rest countdown after every set, and includes meal ideas.

## Included workout focuses

- Upper: Dumbbell Press, One-arm Row, Incline Push-up, Band Pull-apart
- Lower: Goblet Squat, Romanian Deadlift, Reverse Lunge, Glute Bridge
- Arms: Biceps Curl, Overhead Triceps Extension, Hammer Curl, Bench Dip
- Chest: Push-up, Floor Press, Chest Fly, Incline Push-up
- Legs: Bodyweight Squat, Walking Lunge, Calf Raise, Wall Sit
- Full Body: Squat to Press, Burpee, Renegade Row, Mountain Climber

Each entry has a plain-language purpose and coach cue in the in-app exercise library. Guided sessions also provide a looping offline movement demo.

## Build

Set `JAVA_HOME` to JDK 21 and `ANDROID_HOME` to your Android SDK, then run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon
```

This app provides general fitness and meal ideas, not medical or dietary treatment.

## 1.6 polished motion and workout selection

Coach GIFs now use smoother ping-pong motion with blended in-between frames. The workout wizard adds a live animated movement preview for the currently selected focus, while polished surfaces, icons, tactile buttons, and elevated cards make the native Android experience easier to scan and use.

## 1.7 complete account and planning flow

The app now opens at a dedicated Welcome page with separate Sign In and Create Account paths. Plan setup is a four-step wizard: goal, workout focus with an animated preview, training context and duration, then a review screen. After a coach is selected, the device launcher uses that coach’s visual as the My Personal Trainer app icon.

## 1.8 coach-led visual experience

The Welcome page now introduces Coach Mia and Coach Leo side by side beneath “Your Health is Your Wealth.” After sign-in, a persistent left-side coach rail identifies the selected trainer throughout the app. The launcher icon uses the selected coach’s polished portrait artwork.

## 1.9 dashboard visual rebuild

Home, My Plan and Guided Workout now share the dark coach-rail design system from the approved reference: bright action cards, a compact training calendar, exercise rows, animated movement panels, and set/rest counters.

## 2.0 interactive coach rail

The coach rail is now real navigation—Home, Plan, Progress, Library, and Account controls are available directly from the persistent left edge of signed-in screens.

## 2.6 five-step plan builder

The Workout page is now a focused five-step builder: Plan type, Training area, Equipment, Exercises, and Sets. Weekly plans include a persisted Monday-to-Sunday scheduler with explicit Rest Days. Training areas no longer include a generic Machines option; instead, Machine-assisted is an equipment category with tailored choices such as chest press, cable press, pec deck, assisted dip, lat pulldown, cable row, leg press, leg curl, and leg extension.

## 3.0 coach and media customization

Account Settings now includes Customize your Coach with six choices: Mia and Leo (Spanish delivery), Amy and Mark (US delivery), and Claudia and Stephen (UK delivery). Audio can be set to Full Dialogue, Minimal, No Dialogue with low 3–1 beeps plus a high start beep, or No Sound. Keep Phone Awake prevents the display from sleeping while the app is open.

Workout planning accepts one or two equipment categories and merges compatible exercises without duplicates. Training-area and equipment tiles use new high-resolution artwork. Press and hold an exercise to open a detailed animated form preview. All 88 offline demonstrations use looping 30 fps local video for stable, flicker-free motion.

## 3.1 verified coach voices and fitted media

Coach audio now follows Taskmaster’s proven Android Text-to-Speech workflow. The Voice Organizer previews every installed offline English voice and lets the listener label it Male, Female, or Unlabelled. Those device-local labels are matched to the selected coach’s gender and regional accent; a known female voice can never be substituted for Leo, Mark, or Stephen, and a known male voice can never be substituted for Mia, Amy, or Claudia.

Exercise posters, training-area artwork, equipment artwork, and looping videos preserve their source aspect ratios inside clipped tiles. Cached artwork and a transparent video preparation frame remove stretched first frames and reduce visual flicker.

## 3.2 connected wellness nexus

Workouts, Progress, and Nutrition now share one private device-local journey. Completed guided sessions update weekly workout totals and guided minutes; simple nutrition check-ins update the same Progress view; and each page offers a direct next step into the others. An optional adult body profile captures age, height, current weight, and a personal target with clear privacy and safety boundaries.

Rest between sets is adjustable from 45 seconds through 8 minutes in 15-second steps. The last set of an exercise uses the dedicated cooldown dialogue before the next exercise begins.

The evidence review and product decisions are documented in `docs/WORKOUT_PROGRESS_NUTRITION_NEXUS.md`.

## 3.3 separate recovery timers

Set rest and exercise cooldown are now deliberately separate controls. Set rest remains fully adjustable from 45 seconds to 8 minutes in 15-second steps and appears only between sets. Exercise cooldown appears only after the final set of an exercise, using 1-, 3-, or 5-minute presets. The guided screens identify each timer clearly, persist both choices in the saved plan, and keep pause, skip, and emergency-stop controls available.

## 3.6 free-first private Coach

Nutrition is a private, offline fuel companion: daily breakfast and lunch ideas rotate automatically, any idea can be saved locally, and three simple food-and-hydration check-ins connect to Progress beside completed workouts. It does not estimate calories, use a model, send food or body information anywhere, or prescribe portions.

Guided workouts now preserve a local **Set Times** leaderboard for every normally completed exercise. The recovery screen places it directly above Emergency stop, orders completed sets from fastest to slowest, marks the personal best, and shows active-set totals. Those details appear under the corresponding completed workout in Progress; emergency-stopped workouts are never written there.

Direct APK updates now use Android’s persistent download service rather than a one-shot in-app stream. The update screen makes mobile-data use explicit, blocks roaming, restores progress after app backgrounding or a short signal loss, verifies SHA-256 before installation, and resumes the Android installer after the normal install-permission screen.

The delivery rationale, production boundary, and mobile-data regression checklist are in `docs/OTA_DELIVERY.md`.

## 3.7 home schedule calendar

The home screen now has a full weekly schedule calendar: every confirmed workout day shows its planned focus in mint, saved rest days are clearly labelled, and today receives a coral outline. The previous/next controls and horizontal **Upcoming weeks** cards browse up to eight future weeks without changing the week currently being planned. An unscheduled future week is shown as open rather than being mistaken for a rest week.

## 4.0 mobile-data update recovery

The updater now automatically retries the verified release manifest through GitHub's contents API when GitHub Pages times out on a mobile carrier. If neither secure route can be reached, the app keeps all local data intact and offers a manual link to the latest signed release instead of showing a raw connection trace.
