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
