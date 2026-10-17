# Canvas Design-Lock Policy

## Purpose

An approved Canvas or ChatGPT design is the visual source of truth for My Personal Trainer. It is not inspiration for a similar screen. It is the implementation contract for the agreed target Android device.

## Non-negotiable rule

For every locked screen, the released app must reproduce the approved reference:

- word-for-word copy;
- approved images, icons, and video/poster crops;
- colour, type scale, spacing, corner radius, borders, and shadow treatment;
- component order and information hierarchy; and
- the same screen state on the agreed target device.

No substitute asset, visual redesign, copy rewrite, or "close enough" interpretation may be introduced without an explicitly approved replacement reference.

## Scope of a locked reference

Each approved reference must record:

1. A full-resolution source image, named by screen and state.
2. The target device profile: Android version, density, logical screen width and height, font scale, display-size setting, and navigation mode.
3. Exact visible copy, including capitalization and punctuation.
4. Every non-code asset used by the screen, at its intended crop and quality.
5. Required interaction states: default, selected, pressed, empty, loading, error, and scroll position when applicable.

If one of these is missing, the screen remains **unlocked** and may not be described as pixel-matched.

## Implementation procedure

1. Store approved reference screens under a versioned `artifacts/design-lock/` directory before implementation.
2. Create or import the exact artwork into `app/src/main/res/`; do not replace it with a generic placeholder or a differently cropped image.
3. Implement the layout with reusable dimensions, colours, and typography values. Avoid random per-screen offsets.
4. Capture the same state on the target Android device or emulator profile.
5. Compare the capture with the approved reference at full size. Resolve text collisions, crop mismatches, missing shadows, incorrect icon masks, and spacing deviations.
6. Repeat the comparison until the screen is approved, then record the capture beside the reference.

## Release gate

A release that changes a locked screen requires all of the following:

- successful Android build and automated tests;
- a real-device or matched-emulator screenshot for every changed locked screen;
- a visual comparison against the approved reference; and
- user approval if the reference itself has changed.

Functional changes must not silently redesign a locked screen. A visual change must supply a new reference first.

## Platform boundary

"Pixel-for-pixel" means the specified reference state on the agreed target device. Android still needs responsive behavior for other screen sizes, accessibility font scales, orientation, system bars, and launcher masks. Those variants must preserve the same visual system and content hierarchy; they are not permission to change the approved design concept.

## Current application of this policy

- The Full Hero Coach login screen is a single locked template. Settings may change only the selected coach portrait; its layout, logo, copy, fields, actions, and spacing stay fixed.
- The selected premium launcher icon is treated as a design asset and must be previewed within Android's launcher mask before release.
- The Coach Hero Dashboard v1 references live in `artifacts/design-lock/coach-hero-dashboard/v1/`. Home, Workouts, Progress, Nutrition, Endurance, Scheduler, Body, and Settings share one coach-led header and one labelled bottom navigation system. When Coach Mia is selected, Mia is the only coach image used across those pages. A different selected coach may replace only the coach portrait and its name.
