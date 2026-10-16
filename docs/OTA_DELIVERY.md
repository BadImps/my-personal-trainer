# OTA delivery: mobile-data reliability and release safety

## 3.7 release note

Version 3.7 adds the browseable home schedule calendar. It is presentation-only: selecting a future week on Home never rewrites the independently stored plan for the current week. Future weeks with no saved schedule are explicitly shown as open, preserving the distinction between an intentional rest day and a week that has not yet been planned.

## What changed in 3.6

The direct-APK updater now delegates package transfer to Android `DownloadManager` rather than keeping a one-shot network stream inside the activity. Android retains the transfer when the app backgrounds, temporarily loses a cellular signal, or recreates the process. The app persists the update version, URL, SHA-256, byte size, and download ID so the update screen can restore an in-progress or verified package even if refreshing the release manifest temporarily fails.

- A metered connection asks before mobile data is used.
- Roaming is disabled for update downloads.
- A Wi-Fi-only transfer that is switched to mobile data explicitly restarts; ordinary temporary interruptions remain under Android's retry/resume handling.
- The app checks storage headroom, exact byte count, SHA-256, package ID, and version before opening the Android installer.
- If Android requires the normal **Install unknown apps** approval, the verified handoff is persisted and resumes on return.

## Why the former path failed on mobile data

The former activity-owned `HttpURLConnection` transfer had no durable download ID, resume support, cellular-data consent, storage preflight, or return path after Android's install-permission settings page. A signal loss or process recreation restarted from zero or left the interface unable to describe the actual state.

## Distribution boundary

This direct APK flow is for installations distributed outside Google Play. A Play-distributed build should instead use Play In-App Updates; Google Play normally does not allow apps to use a self-managed APK installer to update themselves.

Before any public direct-APK release, migrate from the current development signing setup to a protected release key and plan the signing continuity/rotation carefully. Android will reject an update signed by a different certificate, so changing that key without a migration plan can strand already installed copies. A production release channel should also use signed release metadata and a controlled primary/fallback artifact host.

## Mobile-data regression checklist

1. Start on mobile data with Wi-Fi disabled and explicitly approve the data use prompt.
2. Interrupt mobile data midway, restore it, and verify Android resumes the same transfer.
3. Enable Data Saver and verify the paused state has a clear recovery action.
4. Background or recreate the app during transfer; return to the update screen and verify progress is restored.
5. Deny then grant **Install unknown apps** and verify the installer resumes without re-downloading.
6. Test insufficient space, a bad SHA-256, a wrong package/version, and a declined installer confirmation.
