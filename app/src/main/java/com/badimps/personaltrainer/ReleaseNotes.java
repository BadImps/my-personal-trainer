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
            new Entry("5.90", "Original Mintfoil collection restored", "Achievement cards are back to their full tall iron-frame artwork, with distinct milestone symbols and the original tap-to-flip collection details."),
            new Entry("5.89", "Mintfoil board & watch handoff", "Restores the full iron-framed Mintfoil achievement cards and wakes the installed Wear OS companion when a guided workout starts."),
            new Entry("5.88", "Gym Lobby", "Adds Gym Lobby, Gym Buddies, private-lobby invitations and join requests, public achievement profiles, and friendly Momentum Points leaderboards."),
            new Entry("5.86", "Schedule past dates", "Choose an unfinished past calendar day on Home, then schedule a guided or endurance workout for that exact date."),
            new Entry("5.85", "Mintfoil achievement-card refinement", "Achievement cards now use Coach-card-ratio plates that preserve true circular badge mounts. Card copy has more clearance from the lower iron chevron, the flip-back is fully opaque, and Steady Return's shield is optically centred."),
            new Entry("5.84", "Achievement cards, production finish", "Achievement cards now use the approved Mintfoil plate as the single mount and landscape, with new transparent icon-only inserts for every family. Fronts show a clear locked or unlocked state, while exact requirements and progress stay on the flip-back."),
            new Entry("5.83", "Achievement Board composition corrected", "Achievement cards now use one physical Mintfoil mount only: the new themed art fills its recessed center instead of nesting a second rimmed coin. The original portrait card ratio is restored and angled corners stay transparent on the board."),
            new Entry("5.82", "Screenshots enabled", "You can now use Android's normal screenshot gesture anywhere in My Personal Trainer. The Achievement Board also removes the rectangular matte around its Mintfoil card frames so the angled steel cards sit directly on the display surface."),
            new Entry("5.81", "Canvas Mintfoil medal collection", "Replaces every legacy achievement coin with original Canvas-style Mintfoil artwork: brushed forged-metal rims, navy enamel centers, luminous inlay, and purpose-made mountain, calendar, lightning, pulse, dumbbell, star, trophy, compass, shoe, bicycle, shield, and flame medals."),
            new Entry("5.80", "Distinct Mintfoil achievement art", "Every achievement now carries its own named Mintfoil medal: First Step uses the mountain path, Three in Motion uses the calendar, Steady Seven uses the lightning crest, and each family has meaningful collectible artwork."),
            new Entry("5.79", "Premium achievement presentation", "Tightens every Mintfoil achievement-card crop to the steel frame, gives the collection a dark premium display plinth, and adds a haptic halo-and-card reveal to achievement rewards."),
            new Entry("5.78", "Canvas Mintfoil card lock", "Achievement collection cards and rewards now use the approved full portrait Mintfoil steel frame, recessed coin mount, navy texture, divider, and exact First Step master artwork."),
            new Entry("5.77", "Mintfoil achievement rewards", "Unlocked achievements now arrive in a premium iron-edged Mintfoil card with a mounted spinning badge, double foil frame, and direct path into Apex."),
            new Entry("5.76", "Live health and completed-week polish", "Guided workouts now use a pulsing heart and calorie-change fire motion without a Watch label; Home Your Week shows each completed day’s actual training area with its matching activity icon."),
            new Entry("5.75", "Watch synchronization hardening", "Orders every phone workout update, rejects delayed watch snapshots after sleep or reconnect, and keeps Health Services tied to the correct guided-workout session."),
            new Entry("5.74", "Watch launch crash hotfix", "Prevents guided strength workouts from requesting a location foreground-service type, so the watch opens reliably, stays open, starts Health Services, and survives screen sleep."),
            new Entry("5.73", "Watch clock and Health Services sync", "Makes phone deadlines authoritative for active sets and recovery, restores watch state after sleep, retries sensors after permission, and streams heart rate and measured calories into guided workouts and reports."),
            new Entry("5.72", "Reliable phone-to-watch launch", "Replaces the background watch launch with the official phone-initiated Wear OS remote-activity handoff, so a new guided workout reliably opens its synchronized watch screen."),
            new Entry("5.71", "Synchronized countdown and clocks", "Phone and watch now share the same five-to-one count-in and canonical set clock, keeping workout timing accurate when either screen sleeps or the watch activity is reopened."),
            new Entry("5.70", "Automatic watch handoff", "Starting a guided workout on the phone now synchronizes and opens its workout controls on the watch once per session, without reopening for routine set, pause, or health updates."),
            new Entry("5.69", "Phone-authoritative watch workouts", "Wear OS now waits for a live phone workout, rejects stale or duplicated controls, disables actions while disconnected, and binds heart-rate and calorie measurements to the exact guided session."),
            new Entry("5.68", "Watch Health Services", "Adds live watch heart rate and measured calories to guided workouts, synchronizes completed measurements into workout and weekly reports, and adds standalone watch endurance tracking."),
            new Entry("5.67", "Live workout eligibility", "Checks whether Android actually permits promoted Live Updates, offers the required system setting when a workout starts, and adds a clear Live workout display control under Settings."),
            new Entry("5.66", "Premium dumbbell launcher", "Replaces the launcher artwork with the approved minimal mint-and-coral dumbbell on a solid Coach Hero navy tile, including full-bleed adaptive masking that prevents white corners."),
            new Entry("5.65", "Live workout surfaces", "Active guided workouts now remain visible through Android's lock-screen Live Update surface, with elapsed time, exercise progress, set controls, and a synchronized Coach Hero Wear OS companion."),
            new Entry("5.64", "Workout length for Daily & Weekly", "Daily and Weekly planning now include Workout Length before equipment, so every saved session is sized around your chosen 45-minute, 60-minute, 1-hour-50-minute, custom, or untimed format."),
            new Entry("5.63", "Duration-aware guided plans", "Coach now shapes exercise blocks around your chosen workout length, rotates selected movements when needed, and clearly flags manual plans estimated to run over time."),
            new Entry("5.62", "Clearer weight and monthly reports", "Fixes Progress report text collisions and adds a private, check-in-only possible seven-day weight trend once two dated weigh-ins are available."),
            new Entry("5.59", "Weekly Progress refinement", "Progress now has aligned weekly Endurance, Guided Workout, activity, energy, and conservative weight-load references; report-style icon and ring motion; and Home uses centered Completed ribbons with direct historical report access."),
            new Entry("5.58", "Cool-down before report", "Selected five-minute cool-down choices now persist for the active guided session, including after navigation or app recreation, so recovery always appears before the completion report."),
            new Entry("5.57", "Guaranteed cool-down access", "Every completed guided session now exposes the five-minute tailored cool-down if it was not shown before the completion report, without repeating it after completion."),
            new Entry("5.56", "Guided workout preparation", "Custom exercise selection now has Library search and filters; Home clearly distinguishes today from historical days; guided workouts offer optional five-minute warm-up and cool-down flows, recovery recommendations, and a coach suggestion staged for Scheduler approval."),
            new Entry("5.55", "Route camera and pan controls", "Offline route maps now frame the route more tightly, support direct one-finger panning and zoom gestures, and stop auto-centering after you take control."),
            new Entry("5.54", "Every endurance completion reported", "Live, guided, scheduled, and timer-only endurance completions now all open the same endurance report; GPS sessions additionally replay their private route."),
            new Entry("5.53", "Completed endurance reports", "Completed walks, runs, and cycles now receive an animated endurance report, calorie range, Home Done ribbon, and private route replay."),
            new Entry("5.52", "Private training intelligence", "Adds local recovery guidance, transparent progression suggestions, and a symmetrical barbell plate guide without automatically changing your plan."),
            new Entry("5.51", "Map view rendering hotfix", "Corrected the MapLibre route-view lifecycle so the selected vector streets render on the live endurance screen instead of leaving the fallback surface visible."),
            new Entry("5.50", "Choose your Trinidad map detail", "MapLibre maps now offers Low Impact for a lighter download or Enhanced for richer streets, parks, water, and labels—both optional and separate from OTA size."),
            new Entry("5.49", "Clear MapLibre map controls", "Replaces legacy offline-map wording and file controls with one clean MapLibre vector-pack experience."),
            new Entry("5.48", "MapLibre offline vector maps", "Adds a polished MapLibre vector-map surface for Trinidad & Tobago with a resumable country download and a reliable on-device fallback."),
            new Entry("5.47", "Move placement editor", "Move to now opens one clear popup with Body Area and Equipment selectors together, followed by Save placement."),
            new Entry("5.46", "Shoulder Tap motion repair", "Plank Shoulder Tap now uses a stable-plank loop that alternates the opposite hand to opposite shoulder, with no hip-lift motion."),
            new Entry("5.45", "Reliable exercise moves", "Workout Library actions now open on a normal tap as well as a press and hold, so permanent Move to corrections are always reachable."),
            new Entry("5.44", "Permanent Library placements", "Manual Move to choices now permanently update Workout Library and future Daily and Weekly plan selections."),
            new Entry("5.43", "Library placement controls", "Adds a distinct premium Workout Library icon and a manual Move to control for filing exercises by body area and equipment."),
            new Entry("5.42", "Correct equipment groupings", "Bodyweight now means no equipment; dumbbell, barbell, and household-item plans each contain only movements matching that choice."),
            new Entry("5.41", "Workout Library scheduling", "Holding a library movement now offers Preview or Add to a Schedule, with multi-select additions for saved future workouts."),
            new Entry("5.39", "Human coach voices", "Adds six selected coach voice packs for synchronized guided-workout cues, with Android speech retained for dynamic exercise names."),
            new Entry("5.38", "Complete workout refinement", "Adds persistent guided-workout controls, custom and untimed sessions, refined cooldown navigation, expanded training and equipment choices, improved nutrition content, and unified neutral Strength Level exercise artwork."),
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
