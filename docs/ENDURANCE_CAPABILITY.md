# Endurance routes capability

## Capability

The Endurance page lets a signed-in person create and retain a Walking, Running, or Cycling route plan, open that route in their installed map app, and log a completed planned duration to the existing local Progress timeline.

## Fixed rules

- Route details stay in local app storage.
- A person can explicitly start a live Walking, Running, or Cycling session. Android requests location only at that moment, and a visible foreground-service notification remains while tracking is active.
- GPS points, measured distance, active duration, and a private route trace are stored locally. Pausing stops both the active timer and location sampling; finishing adds the duration to local Progress; discarding removes the session.
- The in-app route trace intentionally has no third-party street-map tiles, map SDK, or route-provider key.
- The map handoff is an explicit user action. The selected map app receives only the origin (when entered), destination, and travel mode.
- Walking and cycling use matching map travel modes. Running is intentionally handed off as a walking route because standard map intents do not offer a separate running mode.
- Completion is a manual confirmation, not GPS-verified activity.

## User flow

1. Choose Walking, Running, or Cycling.
2. Give the route a name, optional starting point, destination/loop landmark, target distance, time, and effort.
3. Open the installed map app to review or adjust the route.
4. Save the local route plan.
5. Mark the planned duration complete only after finishing; that creates a normal local Progress record.

## Non-goals

This release does not supply turn-by-turn navigation, a street-map provider, health-platform syncing, or safety guarantees about a chosen route.
