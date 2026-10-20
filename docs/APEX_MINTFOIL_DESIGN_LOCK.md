# Apex Achievement Board — Canvas Design Lock

The approved Apex Achievement Board reference is stored at
`artifacts/design-lock/apex-achievement-board/v1/approved_canvas_reference.png`.

The twelve production medal assets in `app/src/main/res/drawable-nodpi/apex_badge_*.png`
are direct 248 × 242 pixel crops of that approved source. They are used as raster artwork
by `MintfoilBadgeView`; the application must not substitute native-drawn glyphs, generic
medals, or alternate icon sets for these badges.

The application may scale these exact assets responsively, dim a locked badge, and animate
the approved medal when it is acquired. It may not alter their illustration, framing,
materials, or colour treatment without a new approved Canvas reference.
