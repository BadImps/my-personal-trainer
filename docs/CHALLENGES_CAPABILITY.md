# My Personal Trainer — Challenges, Achievements, Feats, and Coach Tiers

**Status:** implementation-ready capability blueprint  
**Prepared:** 2026-10-18  
**Research confidence:** high for the behavioural and safety guardrails; medium for the exact point economy, which must be tuned from real use.

## Capability

My Personal Trainer will add a private, offline-first progression system that makes completed movement more visible and motivating. Members earn **Achievement Points (AP)** for genuinely completed workouts and endurance sessions, gain levels, collect badges for varied accomplishments, and optionally attempt time-bound **Feats**. Coach cards become collectible training cards: Mia and Leo are available from the start, while the remaining coaches unlock through safe, demonstrated progress. The system rewards consistency, breadth, and planned recovery; it never rewards emergency stops, starvation, unsafe speed, or ignoring recovery.

## Research synthesis

Digital physical-activity interventions with gamification can improve activity adherence, but results vary and individual game elements are rarely proven in isolation. The appropriate product response is a small, transparent set of feedback loops—progress, attainable challenges, badges, and choice—rather than a coercive streak economy ([systematic review/meta-analysis](https://pmc.ncbi.nlm.nih.gov/articles/PMC11701442/), [mHealth gamification review](https://pmc.ncbi.nlm.nih.gov/articles/PMC8855282/)).

WHO frames physical-activity recommendations around frequency, intensity, and duration for distinct populations; the app should therefore personalize goals but avoid presenting a game level as medical clearance ([WHO guideline](https://www.who.int/publications/i/item/9789240014886%E2%81%A0)). ACSM’s evidence-based resources distinguish resistance-training goals and progression; shorter recovery is a programming variable, not a universal “harder is better” reward ([ACSM position-stand index](https://acsm.org/education-resources/pronouncements-scientific-communications/position-stands/)).

**Product inference:** unlocks must represent access to a coaching style and optional challenge templates—not pressure the member to train beyond their plan. Every high-intensity template remains adjustable, has a visible recovery requirement, and may be exited without losing historic achievements.

## Product diagnostic

| Diagnostic | Decision |
|---|---|
| Member | A person using the app solo who wants visible evidence of consistent training, not a public competition. |
| Job | Turn completed plans into a satisfying long-term progression path without obscuring safe plan choices. |
| First value | After one normal completed session, the member sees AP, their first level meter, and a next achievable challenge. |
| 10-star version | Coach cards, evolving challenge paths, personal milestones, and a Progress timeline that tell a coherent training story. |
| MVP | Local AP, levels, 12 badges, 6 challenge templates, coach-card locks, and a private Challenges view. |
| Anti-goals | No global competitive leaderboard, calorie/weight-loss points, pay-to-unlock, points for unsafe rest skipping, or punishment for a missed day. Optional private Gym Lobby rankings use only Momentum Points. |
| Success metrics | Challenge completion rate, voluntary return to the Challenges view, plan completion rate, and zero emergency-stop rewards. |

## Fixed rules and safety invariants

1. **Completion is the source of truth.** Award AP only after a normal guided-workout or endurance completion has been recorded in `JourneyStore`. Emergency-stopped workouts and abandoned live-tracking sessions award nothing.
2. **No scarcity or punishment.** Badges stay earned permanently. Streaks, if shown, are “momentum” information only; missing a day never removes AP, a badge, or a coach.
3. **No body-size scoring.** Weight, calorie intake, physique, age, sex, and body measurements do not award points or unlock coaches.
4. **Safety gates precede intensity.** A higher-tier challenge requires completed prerequisite sessions and at least one scheduled recovery day. It may never force a rest or cooldown time below the member’s selected safe setting.
5. **Difficulty is opt-in and editable.** A coach unlock gives access to a card and challenge library; it does not change the user’s existing plan, weights, reps, or reminder schedule without confirmation.
6. **Private by default.** AP, badge state, and coach unlocks are local app data. Verified members may publish a narrow social summary (display name, level, Momentum Points, Apex lane, and earned badge names) to the Gym Lobby; no underlying activity or health records are uploaded.
7. **Plain-language guardrail.** Every Feat says: “Adjust or skip if it does not feel right. This is not medical advice.”

## Progression model

### Achievement Points and levels

| Event | AP | Reason |
|---|---:|---|
| Complete a guided strength workout | 100 | Core planned action |
| Complete a manual or live-tracked endurance session | 80 | Equal recognition for endurance |
| Complete a scheduled rest/recovery day check-in | 20, maximum 2/week | Rewards recovery without gaming it |
| Complete a weekly plan target | 150 bonus | Rewards planning + follow-through |
| Earn a new badge | 50 bonus | Celebrates a distinct milestone once |
| Complete a Feat | 300–900 | Scales by tier and duration |

Levels use a gentle curve: **Level 1 begins at 0 AP; each next level requires 250 + (current level × 75) AP.** The UI always shows the member’s current level, total AP, and AP remaining—not a punitive streak.

### Challenge types

| Type | Cadence | Example | Completion rule |
|---|---|---|---|
| Daily Spark | Daily, optional | Complete your planned 20+ minute session | One verified completion |
| Weekly Route | Weekly | Finish 3 scheduled sessions with one recovery day | Completed sessions plus scheduled recovery presence |
| Balanced Week | Weekly | 2 strength + 1 walk/run/cycle | Different verified movement types |
| Form & Control | One session | Finish a selected plan without rest skips | No rest/cooldown skip events in that session |
| Endurance Landmark | Rolling | Accumulate 10 km walking/running/cycling | Local distance/time record where available |
| Feat | 14–28 days | Finish an advanced plan path with recovery built in | Prerequisites, then verified milestones |

### Badge families

- **First Steps:** First Plan, First Guided Finish, First Endurance Track.
- **Consistency:** Three This Week, Four-Week Rhythm, 12-Session Month.
- **Range:** Upper/Lower Balance, Strength + Endurance, Home/Gym Explorer.
- **Craft:** Controlled Recovery, Set-Time Personal Best, Plan Builder.
- **Recovery:** Rest Day Respected, Comeback (returns after 7+ inactive days without penalty).
- **Feats:** Summit I, Summit II, Apex Path.

Badges describe what happened and show the date, never a body-weight result or calorie total.

## Coach-card unlock system

Coach cards retain the approved Coach Hero trading-card styling and use the existing six coaches.

| Tier | Coaches | Unlock | Training-card identity | Challenge access |
|---|---|---|---|---|
| 1 — Foundation | Mia, Leo | Available immediately | Form, confidence, repeatable routines | Daily Spark, First Steps |
| 2 — Momentum | Amy, Mark | Level 4 **and** 8 completed sessions | Consistency and pacing | Weekly Route, Balanced Week |
| 3 — Performance | Claudia, Stephen | Level 8 **and** 20 completed sessions **and** 2 Balanced Weeks | Focused, longer progressive blocks | Performance Paths, Summit I |
| 4 — Apex Feats | A mode on unlocked Tier-3 coaches; not a separate paid coach | Level 12, 36 completed sessions, 1 recovery-complete four-week block, confirmation | Long-form milestone training | Summit II and Apex Path |

The extreme/“Apex” coach mode offers longer, progressive sessions and challenge structure. It must start from the member’s chosen rest and cooldown settings; it can recommend, but never silently set, shorter rest. A clear “Use my current recovery settings” control is the default.

## Feat readiness contract

A Feat is visible but locked until all of these are true:

1. the member completed the required number of normal sessions in the last 28 days;
2. the last 7 days include a recovery/rest day or no more than the member’s chosen weekly training-day limit;
3. there is no active emergency-stopped workout that day;
4. the member explicitly accepts the workload, time, and recovery summary;
5. the app presents alternatives: “Choose a lower tier,” “Adjust recovery,” and “Not now.”

Examples: **Summit I** = 10 completed sessions across 21 days with one strength and one endurance completion; **Summit II** = three weeks meeting the member’s own schedule, not a universal rep target; **Apex Path** = a 28-day plan that inserts recovery weeks and never awards points for skipped rest.

## Experience surfaces

1. **Home:** compact `Level · AP · active challenge` card under This Week. Tapping opens Challenges.
2. **Progress:** new `Challenges & Feats` tab with total AP, level runway, badge gallery, activity history, and completed Feats.
3. **Coach settings:** `Coach Profile Cards` becomes a card collection. Locked cards show the exact, achievable requirement; unlocked cards preserve current coach-selection controls.
4. **Plan Ready:** optional, non-blocking `Apply challenge` row. The user can build any ordinary plan without joining a challenge.
5. **Workout completion:** one clean result sheet: AP earned, newly earned badge(s), next challenge progress. No confetti during recovery or timer flow.

## Data and engineering contract

### New local stores

- `ChallengeStore`: total AP, awarded event IDs, current level, challenge enrollments, challenge progress, unlocked coach tiers, earned badge IDs/timestamps, completed Feat IDs.
- `ChallengeCatalog`: code-defined, versioned challenge/badge definitions. Definitions remain readable offline and can migrate by stable ID.
- `CoachUnlockPolicy`: maps coach name to tier and validates whether a coach is selectable.

### Existing integration points

- `JourneyStore.recordWorkout(...)`: submit one idempotent completion event after the record is persisted.
- `LiveTrackingService`: submit endurance AP only when its existing normal completion path records a workout.
- `SetTimeStore`: supplies personal-best badge eligibility; it cannot independently award AP.
- `PlanStore`, `WeeklyScheduleStore`, and `SchedulerStore`: supply planned-session and recovery context only.
- `CoachPreferences`: refuses a locked coach selection and continues to honor the selected voice/dialogue options for unlocked cards.

### Required invariants

- Event awards are idempotent: app restart, back navigation, or repeated completion rendering cannot duplicate AP.
- Deleting a workout in Progress removes only its history record. It must not retroactively delete a badge, AP, or completed Feat without an explicit future “recalculate progression” action.
- Unlocks are monotonic locally: a member never loses an unlocked coach because a plan is edited or a streak breaks.
- Challenge evaluation must run off the UI thread once event volume grows; current MVP may be synchronous only while the local history is small.

## Phased delivery

### Phase 1 — Foundation

`ChallengeStore`, AP/levels, 12 badges, Home summary card, Progress challenge tab, and Mia/Leo Foundation cards. Wire real completion events and tests for duplicate-event prevention.

### Phase 2 — Progression

Weekly/Daily challenges, Momentum coach cards, unlocked-card selection, Challenge details, and weekly-plan completion evaluation.

### Phase 3 — Feats

Performance/Apex coach modes, readiness checks, 14–28 day Feat paths, explicit safety confirmation, and full Feat history.

### Phase 4 — Tuning

Add only aggregated local diagnostics—challenge start/completion/abandonment and safety exits—then tune AP thresholds. The separate 2026-10-28 Gym Lobby privacy decision permits only the narrow social summary documented above.

## Non-goals

- Medical diagnosis, fitness clearance, or injury-risk prediction.
- Public competition, social feeds, purchases, ads, or paid unlocks.
- Automatic changes to weight, reps, rest, cooldown, or coach dialogue settings.
- Rewarding food restriction, weigh-ins, or skipped recovery.

## Open product decisions

1. Should the primary on-screen label be **Achievement Points (AP)** or simply **Trainer Points**?
2. Should Apex be called **Apex**, **Elite**, or your preferred name? “Extreme” can be retained in the copy, but it is less clear as a permanent tier label.
3. Should coach unlocks be purely earned, or may the owner manually unlock all coaches in Settings for accessibility/preference reasons? Recommended: an optional owner-only “unlock all coaches” switch that does not change Feat eligibility.
4. Should inactive/returning members see a gentle Comeback challenge automatically? Recommended: yes, but never a missed-streak warning.

## Handoff

**Ready for direct implementation after naming decisions.** Start with Phase 1. The first engineering slice should add idempotent completion events and local progression state, then render a read-only Progress/Coach Cards experience before challenge enrollment or Apex templates.

