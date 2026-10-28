# Apex Achievement Board — Canvas Design Lock

The approved Apex Achievement Board reference is stored at
`artifacts/design-lock/apex-achievement-board/v1/approved_canvas_reference.png`.

The production presentation is the original tall, two-column Mintfoil collectible card:
`achievement_mintfoil_card_coach_ratio.png` for the front and
`achievement_mintfoil_card_back_coach_ratio.png` for the reverse. They contain the
approved iron/mint frame, mountain-and-lake scene, circular mount, jewel, and portrait
proportions. `MintfoilPlateView` clips only the exterior matte; it must not redraw the
frame as generic native UI.

The icon inserts in `achievement_symbol_*.webp` are the approved distinct symbols for
each milestone. `MintfoilBadgeView` places those into the existing circular mount. It
must not substitute generic medals or reuse a single icon across different achievements.

The application may scale these exact assets responsively, dim a locked card, and animate
the approved coin when it is acquired. It may not alter their illustration, framing,
materials, or colour treatment without a new approved Canvas reference.
