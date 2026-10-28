# Next App Additions — Product Brief

## Product rule

My Personal Trainer already covers planning, guided and endurance sessions, private progress,
Mintfoil achievements, a watch companion, and optional Gym Lobby competition. The next work
should make those commitments easier to trust and use. It should not add another overlapping
planner, a public social feed, billing, calorie/weight-loss scoring, or health claims.

## Priority order

Scores use a lightweight ICE check: **Impact × Confidence ÷ Effort**. Effort is relative and
includes Android, Supabase, migration, and regression-test work.

| Rank | Addition | Member outcome | Impact | Confidence | Effort | ICE |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| 1 | Connection Center | Knows whether account, cloud profile, and watch are actually connected | 5 | 5 | 2 | 12.5 |
| 2 | Mintfoil card detail | Every achievement feels collectible and explains the next step | 4 | 5 | 2 | 10.0 |
| 3 | Gym Lobby activity inbox | Never misses a buddy or private-lobby invitation | 4 | 4 | 2 | 8.0 |
| 4 | Recovery & data hub | Can see exactly what is protected, what stays on-device, and how to recover | 5 | 4 | 3 | 6.7 |
| 5 | Smart reschedule | Keeps a plan useful when life changes without punishing a missed day | 4 | 4 | 3 | 5.3 |
| 6 | Encrypted cross-device workout backup | Restores private history on a new device | 5 | 3 | 5 | 3.0 |

## 1. Connection Center — build next

Place a small **Connection** card at the top of Settings, with a single clear status line for:

- **Account:** signed in / needs sign-in / offline.
- **Gym Lobby:** available / unavailable, with a refresh action.
- **Watch:** paired / not detected / last sync time.
- **Updates:** installed version and whether an update is ready.

The detail page should offer only direct recovery actions: **Sign in**, **Refresh Gym Lobby**,
**Open watch companion**, and **Check for update**. Do not add background polling or a new
account system. Reuse the existing authenticated session, Gym Lobby client, Wear data layer,
and OTA checker.

**Done when:** a member can tell in one screen whether their account and watch are connected,
and can retry the failing connection without hunting through the app.

## 2. Mintfoil card detail

Tapping a Mintfoil card should open a full-screen card detail instead of a toast. It keeps the
approved iron-and-mint artwork, then reveals:

- badge family and tier;
- unlocked or locked state;
- the exact, safe requirement;
- current progress (for example, `7 / 20 sessions`);
- a single contextual action such as **Plan next session** when appropriate.

Locked cards stay visible as collection goals, but must never shame a member for rest days or
body measurements. The detail screen is local-only; it does not share a card unless the member
explicitly uses Gym Lobby profile sharing already permitted by the existing privacy rules.

**Done when:** cards feel intentional and members can understand what each one recognizes
without leaving the Apex board.

## 3. Gym Lobby activity inbox

Add an **Activity** control inside Gym Lobby, with an unread count only when there is something
actionable. It lists pending buddy and private-lobby invitations with **Accept** and **Decline**.
After a decision, it shows a small history record such as “You joined Leo’s private lobby.”

Keep the current narrow social profile: display name, level, Momentum Points, Apex lane, and
earned achievement names. Never surface email addresses, workout logs, meals, body data, maps,
or routes.

**Done when:** an invite cannot disappear unnoticed and a member can resolve it in two taps.

## 4. Recovery & data hub

This is a transparency feature before it is a backup feature. Add a **Your data** page in
Settings that plainly labels each category:

| Category | Current location | Recovery expectation |
| --- | --- | --- |
| Sign-in identity and session | Supabase account/session when configured | Sign in again on another device |
| Gym Lobby summary and invitations | Supabase | Sign in again |
| Workout history, plans, nutrition, body entries, and private achievements | Device unless a dedicated encrypted backup is shipped | Remains on the current device only |

Include an export action before any cloud migration: a password-protected local backup file that
the member deliberately creates and can restore. Do not label private training history as
cloud-protected until encrypted upload, Row Level Security, conflict handling, and restore tests
exist.

**Done when:** no member has to guess whether a piece of data will survive a new phone.

## 5. Smart reschedule

When a planned session is missed, offer **Move to next suitable day** from the schedule and next
workout card. The chooser must respect the member’s selected training-day limit, planned
recovery, and any adjacent same-body-area session. It never silently changes a plan and always
offers **Keep as is** and **Remove from plan**.

**Done when:** a missed workout becomes a quick scheduling choice, not a broken routine.

## Later: encrypted cross-device backup

This is important but should not be rushed. It needs a written data model, versioned encrypted
payloads, per-user Supabase Row Level Security, restore previews, conflict rules, migration
testing, and an explicit privacy/retention policy. Start only after the Recovery & data hub has
made the current behavior clear.

## Recommended release sequence

1. **5.90 — Connection Center:** small, high-confidence clarity release; also provides a visible
   watch sync diagnostic.
2. **5.91 — Mintfoil collection detail:** completes the visual achievement experience without
   changing scoring.
3. **5.92 — Gym Lobby activity inbox:** makes the existing social actions reliable and legible.
4. **5.93 — Recovery & data hub:** export first, then separately design encrypted cloud history
   backup.

## Deliberately not adding now

- A global leaderboard, follower feed, chat, ads, or subscriptions.
- Points for food restriction, body weight, skipped rest, or risky training intensity.
- Automatic plan changes without a visible member choice.
- Claims that device-only history is already recoverable from the cloud.
