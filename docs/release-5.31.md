# v5.31 — Protected release channel and optional coach motion

- Moves the production build to a protected release signing key. This is a one-time migration: debug-signed installs must be replaced manually before they can receive future protected OTAs.
- Removes 88 bundled exercise-loop videos from the core APK. Coach Mia/female and Coach Leo/male motion libraries are downloaded separately, verified by SHA-256, and remain available offline.
- Adds an in-app update prompt that names the available version and summarizes the release.
- Adds Settings → Updates → Full update history, containing the customer-facing patch notes for every current Coach Hero release.
