# Gym Lobby capability

The Gym Lobby is an authenticated social layer for friendly motivation. All verified registered
users appear as **Gym Members** after opening the updated app. A member can tap or long-press another
member to add a Gym Buddy, invite them to a private lobby, request to join their lobby, or view their
public profile.

Private lobbies rank accepted members by Momentum Points and explicitly identify first, second, and
third place. The ranking is motivational only; it never changes workouts, unlocks, health guidance,
or access to the app.

## Hosted data boundary

Supabase stores only display name, level, Momentum Points, Apex lane, earned achievement names,
buddy/invitation requests, and private-lobby membership. It does not receive email through the lobby
API, profile photos, workouts, exercise logs, nutrition, body measurements, routes, schedules, or
coach preferences. Direct table access is denied; authenticated users interact through the narrow
RPC functions in `supabase/migrations/202610280001_gym_lobby.sql`.

## Interaction and accessibility

The requested long-press menu is supported. A normal tap opens the same menu so the actions remain
available to keyboard, switch-access, and users who cannot perform a press-and-hold gesture.
