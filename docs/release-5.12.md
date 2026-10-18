## My Personal Trainer 5.12

- Stabilized page changes inside one permanent Activity host to eliminate full-window redraw flashes.
- Restored scroll position before first draw instead of hiding and fading the page.
- Corrected the Home coach image from stretched to proportionate Coach Hero cropping.
- Preserved the existing Coach Hero design and bottom navigation.

Validated with debug unit tests and a signed release build. No Android Virtual Device is configured on this host, so emulator capture verification remains pending.
