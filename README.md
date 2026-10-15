# My Personal Trainer

## 1.4 animated exercise library

The guided workout and exercise library now play coach-specific, looping three-phase GIF demonstrations for every prescribed movement, including the no-equipment substitutions. Coach Mia and Coach Leo each have their own animation set. A Machines category adds Machine Chest Press, Lat Pulldown, Leg Press, and Seated Cable Row. These are form-reference animations; follow the written cue, use a comfortable range, and stop if anything hurts.

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
