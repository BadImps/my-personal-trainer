package com.badimps.personaltrainer;

/** Customer-facing history for the self-managed update channel. Keep summaries concise and local
 * so the history remains available when a person is offline. */
public final class ReleaseNotes {
    public static final class Entry {
        public final String version;
        public final String title;
        public final String summary;
        Entry(String version, String title, String summary) {
            this.version = version;
            this.title = title;
            this.summary = summary;
        }
    }

    public static final Entry[] HISTORY = {
            new Entry("5.36", "Canvas Mintfoil & coach-audio fix", "Achievement badges now use the approved Mintfoil artwork directly, and coach dialogue no longer goes silent when Android has no manually labelled voice."),
            new Entry("5.31", "Smaller updates & clearer release history", "Adds optional coach-motion downloads, update prompts that name the available version, and this complete in-app patch-note history."),
            new Entry("5.30", "Apex accuracy & maintenance safeguards", "Apex tiers now require their stated recent-session window. Added quality checks and stronger on-device backup rules."),
            new Entry("5.29", "Smooth exercise loops", "Refined the local coach exercise-loop playback path and presentation sizing."),
            new Entry("5.28", "Guided-start & map-gesture hotfix", "Fixed guided Workout and Endurance countdown starts; completed route maps support two-finger pan and zoom."),
            new Entry("5.27", "Date-first Scheduler", "Tap a calendar date, then schedule Workout, Endurance, or both without losing the chosen day."),
            new Entry("5.26", "Meal visuals & Eating Out", "Added matched Trinidad & Tobago and balanced-meal images plus practical fast-food guidance."),
            new Entry("5.25", "Coach Hub", "Added Coach Collection, Mint Foil coach cards, unlock rules, flip details, and Coach Hero face crops."),
            new Entry("5.24", "Live endurance map", "Added an OpenStreetMap live route surface with private route overlay and viewed-map cache."),
            new Entry("5.23", "Reliable offline street drawing", "Replaced the unreliable offline tile renderer with direct local street geometry."),
            new Entry("5.22", "Offline-map position fix", "Corrected first layout positioning for embedded route maps and added a street-data regression check."),
            new Entry("5.21", "Offline-map cache repair", "Moved the preview cache to reliable app memory so valid map packages no longer fall back to blank traces."),
            new Entry("5.20", "Offline map rendering repair", "Separated country caches and forced the first valid street redraw after download."),
            new Entry("5.19", "Country offline maps", "Added Settings → Offline maps with verified Trinidad & Tobago download and country selection."),
            new Entry("5.18", "Offline routes", "Added a verified Trinidad & Tobago street map for private routes after one download."),
            new Entry("5.17", "Completed endurance history", "Completed walks, runs, and rides retain private distance, duration, pace, and route records."),
            new Entry("5.16", "Secure local sign-in", "Upgraded local password storage and stabilized Live Endurance when navigating away."),
            new Entry("5.15", "Full-width Coach Hero pages", "Progress, Nutrition, Scheduler, Body, and Settings now share the full Coach Hero background treatment."),
            new Entry("5.14", "Navigation continuity", "Bottom navigation retains its horizontal position when switching between dashboard tabs."),
            new Entry("5.13", "Shared Coach Hero backdrop", "Dashboard pages and wizards now use the same selected-coach backdrop, crop, and overlay."),
            new Entry("5.12", "Flicker and scroll repair", "Stabilized page changes inside a permanent activity host and restored scroll before draw."),
            new Entry("5.11", "Dashboard position retention", "Each dashboard tab restores the reading position where you left it."),
            new Entry("5.10", "Clean planning drafts", "Daily plans open Training Areas before dates and no longer carry unselected body areas forward."),
            new Entry("5.9", "Truthful Home schedule", "Your Week shows only saved workout days, rest days, and endurance sessions."),
            new Entry("5.8", "Premium Scheduler calendar", "Added monthly calendar scheduling and shared local schedule data across Home, Workouts, and Endurance."),
            new Entry("5.7", "Clear navigation selection", "Added the coral-orange selected-navigation outline across the app."),
            new Entry("5.6", "Endurance and stable updates", "Added guided endurance tracking, visual count-ins, and durable verified OTA downloads."),
            new Entry("5.5", "Premium Coach Hero & Endurance", "Refined the Coach Hero suite and added planning and tracking for walks, runs, and cycles."),
            new Entry("5.4", "Coach Hero foundation", "Introduced the shared Coach Hero dashboard, saved selected coach, and labelled bottom navigation.")
    };

    private ReleaseNotes() { }
}
