# My Personal Trainer 6.2

## Google Maps endurance support

- Adds an in-app Google Maps view for live endurance routes when a restricted Android key is configured.
- Adds Google Places address and landmark selection for endurance start and destination fields.
- Keeps the existing private MapLibre map and on-device route trace as the no-key and offline fallback.
- Keeps the Google Maps key out of version control and restricts it to the Android build configuration.

## Reliability

- Restores compatible local persistence, achievement, watch, and account helper sources required by the current app shell.
- Verifies the release source compiles with the Maps and Places dependencies.
