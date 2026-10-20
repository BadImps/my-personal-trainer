# v5.32 — Protected production migration target

- Protected production build that can restore a `.mptbackup` created by the v5.31 Migration bridge.
- Keeps the premium optional Coach Motion first-open invitation and verified offline demo-pack experience.
- Uses the permanent release signing key for all future OTA updates.

## Safe upgrade order

1. Update the existing app to **v5.31 Migration** over OTA.
2. In **Settings → Data transfer**, export your private backup.
3. Uninstall the legacy bridge, manually install this protected release, then choose **Restore** on the login screen.
4. After confirmation that your data is present, the protected OTA channel can be enabled for future updates.
