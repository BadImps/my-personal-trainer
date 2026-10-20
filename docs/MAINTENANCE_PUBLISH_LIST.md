# Maintenance publish list — v5.31

## Safe fixes completed

- Corrected Apex unlocks so a tier needs both its required level and the required number of sessions inside its stated time window. Lifetime sessions can no longer unlock a recent-window tier.
- Added unit coverage for the Apex time-window lock and unlock cases.
- Added explicit Android backup/data-extraction rules. Private profile, health, workout, and route records remain on-device rather than being silently backed up.
- Added a GitHub Android quality workflow to run unit tests, release lint, and a debug build on pushes and pull requests.
- Replaced debug release signing with a protected local production signing configuration. The keystore and its credentials are excluded from Git.
- Moved 88 exercise loops out of the APK into two SHA-256-verified optional coach-motion packs. The core APK now falls back to its native motion guide until a selected pack is downloaded.
- Added a versioned in-app update prompt and complete in-app update-history page.

## Verification completed

- 69 unit tests passed with no failures or errors.
- Release lint completed with no errors or fatal findings.
- Debug and release APK builds completed successfully.
- v5.30 launch smoke test passed on the S23 Ultra Android virtual device; password input reports as a masked password field.
- v5.31 debug launch smoke test passed on the Android virtual device with no fatal exception.

## Must resolve before an OTA release

- v5.31 is signed with a new production certificate. Android will reject a direct update from the previously distributed debug-signed app, so the first v5.31 install must replace the older app manually. Future protected OTAs will install normally from v5.31 onward.
- The core release APK is now about 120 MB; optional motion packs total about 19 MB and are no longer re-downloaded with every OTA. Remaining size is primarily Coach Hero and meal artwork, which is retained to preserve the design lock.

## Deferred maintenance candidates

- Move retained workout and endurance history from fixed in-memory/preferences limits to a durable database with export and retention controls.
- Address remaining non-blocking lint findings: custom-map accessibility actions, RTL/string resources, density-specific icons, and drawing allocations.
- Split the large main activity into feature components while preserving the Coach Hero design lock.
