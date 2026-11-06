package com.badimps.personaltrainer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The fixed guided-workout language for each original coach voice. Every line has a stable cue
 * ID, allowing the UI to stay in sync with the coach-specific local audio pack.
 */
public final class CoachVoiceScript {
    private CoachVoiceScript() { }

    private static final String[] CUES = {
            "welcome", "new_exercise", "set_opening_1", "set_opening_2", "set_opening_3", "set_opening_4", "go",
            "set_complete_1", "set_complete_2", "set_complete_3", "exercise_complete_next", "exercise_complete_final",
            "one_minute", "thirty_seconds", "twenty_seconds", "ten_seconds"
    };

    private static final Map<String, String[]> SCRIPTS = new HashMap<>();
    static {
        put("Mia", new String[]{
                "Hola, I am Coach Mia. Strong energy, beautiful control. Let us make this workout count.",
                "New move, champion. Set your position, own your form, and let us go.",
                "Ready, champion. Breathe in, brace strong, and let us begin.",
                "Vamos, we start now. Strong and steady.", "Vamos, move with confidence, champion.",
                "Get ready, champion. You are stronger than you think.", "Vamos! Let us go, champion.",
                "Beautiful work, champion. You earned this rest. Breathe deep, then we go again.",
                "Strong set. Take your recovery, stay focused, and come back ready.",
                "That set is done. Excellent focus. Recover with purpose, then we rise again.",
                "Excellent work on that exercise. Reset in your cooldown, then we own the next one.",
                "You finished that exercise, champion. That is strong work. Be proud.",
                "One minute left. Breathe deep, champion. You are nearly ready.",
                "Thirty seconds left. Stay loose, stay ready, you have this.",
                "Twenty seconds. Shake it out, reset your focus, and get ready.",
                "Ten seconds. Eyes up, strong posture, vamos."
        });
        put("Leo", new String[]{
                "Hola, I am Coach Leo. Set your foundation, drive with control, and let us build strength.",
                "New exercise, champion. Lock in your position and move with real intent.",
                "Breathe, brace, and own the first rep. We begin.", "Strong base, clear mind. Start with purpose.",
                "Time to work. Smooth power, rep by rep.", "Get set, champion. You are ready to build.", "Go! Strong and controlled.",
                "Excellent set. Catch your breath, then come back stronger.",
                "That is solid work. Recover well and keep your focus.",
                "Set finished. Good discipline. Rest, reset, and build again.",
                "Great work. Cool down, then bring that same strength to the next exercise.",
                "You finished strong. Respect the work you put in today.",
                "One minute. Breathe deep and let your strength recharge.", "Thirty seconds. Stay loose, your next set is waiting.",
                "Twenty seconds. Stand tall, find your focus.", "Ten seconds. Brace up. You are ready."
        });
        put("Amy", new String[]{
                "Hi, I am Coach Amy. One focused step at a time—we are building a habit you can trust.",
                "New exercise. Find your rhythm, keep it kind, and let us make this rep count.",
                "Ready? Breathe in, choose your pace, and begin.", "You are here and you are ready. Let us start together.",
                "Keep it simple, keep it steady, and start now.", "Get ready. Your consistency is your superpower today.", "Go! You have got this.",
                "Lovely work. Take a breather, then we keep the promise we made to ourselves.",
                "That was a strong set. Reset gently and stay with me.",
                "Set complete. You are showing up for yourself—enjoy this recovery.",
                "Wonderful progress. Cool down, then we carry that momentum into the next exercise.",
                "You did it. Every finished exercise is another vote for your stronger self.",
                "One minute. Slow your breath and notice how far you have come.", "Thirty seconds. Relax your shoulders—you are doing brilliantly.",
                "Twenty seconds. Reset your posture and bring back your spark.", "Ten seconds. Smile, breathe, and get ready to go."
        });
        put("Mark", new String[]{
                "I am Coach Mark. We train with purpose today—smart effort, real progress.",
                "New exercise. Set your standard, control the movement, then attack the rep.",
                "Lock in. Strong breath, strong brace, and begin.", "Your next level starts with this set. Let us work.",
                "Precision first, power second. Start now.", "Get ready. Your progress is built in moments like this.", "Go! Own the rep.",
                "Powerful set. Use this rest to prepare for an even better one.",
                "Good work. Recover with intent—your next set deserves your best.",
                "Set complete. You are building something real here. Reset and return sharper.",
                "Excellent execution. Cool down, then raise the bar again on the next exercise.",
                "Workout complete for this exercise. That was focused, quality work.",
                "One minute. Regain your breath and reload your focus.", "Thirty seconds. Stay composed—your next effort is close.",
                "Twenty seconds. Set your posture and picture the next clean rep.", "Ten seconds. Lock in. It is time."
        });
        put("Claudia", new String[]{
                "Hello, I am Coach Claudia. Calm control, clean technique, and one excellent rep at a time.",
                "A new exercise. Set your alignment first, then move with care and confidence.",
                "Ready. Breathe, find your position, and begin with control.", "Let us start smoothly. Quality always leads the way.",
                "Take your time, own the shape, and start now.", "Get ready. Strong posture, steady breath, beautiful movement.", "Go. Smooth and precise.",
                "Excellent control. Rest well, then we will repeat that quality.",
                "Very good. Let your breathing settle and keep your technique in mind.",
                "Set complete. That was thoughtful work—recover, then return composed.",
                "Well done. Cool down with care, then carry that control into your next exercise.",
                "That exercise is complete. You moved with real precision—well done.",
                "One minute. Relax your shoulders and take a slow, steady breath.", "Thirty seconds. Keep loose and prepare your position.",
                "Twenty seconds. Reset your alignment and focus on your first rep.", "Ten seconds. Tall posture, calm breath, and begin."
        });
        put("Stephen", new String[]{
                "Hello, I am Coach Stephen. Steady effort today becomes lasting progress tomorrow.",
                "New exercise. Settle in, move with purpose, and let us build this carefully.",
                "Take a breath, find your balance, and begin.", "A calm start makes a strong finish. Let us go.",
                "One good rep at a time. Start now.", "Get ready. You have everything you need for this set.", "Go. Steady and strong.",
                "Nicely done. Take your rest, and we will return with the same quiet confidence.",
                "Good work. Breathe easy, recover, and stay present.",
                "Set complete. You are making progress—rest now and continue when ready.",
                "Excellent. Cool down, then take that steady rhythm into your next exercise.",
                "That exercise is complete. Well earned—be proud of your steady work.",
                "One minute. Let your breathing settle and restore your energy.", "Thirty seconds. Stay relaxed; you are nearly ready.",
                "Twenty seconds. Find your footing and bring your attention back.", "Ten seconds. Calm, ready, strong."
        });
    }

    private static void put(String coach, String[] lines) { SCRIPTS.put(coach.toLowerCase(Locale.ROOT), lines); }

    public static String preview(String coach) { return line(coach, "welcome"); }

    public static String rewrite(String coach, String original) {
        String cue = originalCue(original);
        return cue == null ? original : line(coach, cue);
    }

    /** Resolves refreshed spoken copy back to its stable asset cue. */
    public static String renderedCue(String words) {
        String value = normalize(words);
        for (String[] lines : SCRIPTS.values()) {
            for (int i = 0; i < CUES.length; i++) if (normalize(lines[i]).equals(value)) return CUES[i];
        }
        return null;
    }

    private static String line(String coach, String cue) {
        String[] lines = SCRIPTS.get((coach == null ? "Mia" : coach).toLowerCase(Locale.ROOT));
        if (lines == null) lines = SCRIPTS.get("mia");
        for (int i = 0; i < CUES.length; i++) if (CUES[i].equals(cue)) return lines[i];
        return "Let us go.";
    }

    private static String originalCue(String original) {
        String value = normalize(original);
        if (value.startsWith("hi im ") || value.startsWith("hi, im ")) return "welcome";
        if (value.equals("lets start our new exercise")) return "new_exercise";
        if (value.equals("okay, lets begin")) return "set_opening_1";
        if (value.equals("lets start")) return "set_opening_2";
        if (value.equals("lets go")) return original != null && original.trim().endsWith("!") ? "go" : "set_opening_3";
        if (value.equals("lets get ready")) return "set_opening_4";
        if (value.equals("congrats, you have completed this set. time to take a rest")) return "set_complete_1";
        if (value.equals("good work, you have completed this set. time to take a rest")) return "set_complete_2";
        if (value.equals("this set is complete. well done. time to take a rest")) return "set_complete_3";
        if (value.equals("great, you have completed this exercise. cooldown before you begin next exercise")) return "exercise_complete_next";
        if (value.equals("great, you have completed this exercise")) return "exercise_complete_final";
        if (value.equals("one minute")) return "one_minute";
        if (value.equals("30 seconds remaining")) return "thirty_seconds";
        if (value.equals("20 seconds remaining")) return "twenty_seconds";
        if (value.equals("10 seconds remaining")) return "ten_seconds";
        return null;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace('\u2019', '\'').replace('\u2018', '\'')
                .replace("'", "").replaceAll("[.!?]+$", "").trim();
    }
}
