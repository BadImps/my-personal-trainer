# Exercise media pipeline

Every exercise ships with two offline resources for each selected coach:

- a compact WebP poster used in the plan collage, selection list, plan summary, and library;
- a local H.264 MP4 used only when an exercise is in focus.

The old low-frame-rate GIF renderer has been removed. Each loop is muted, constrained to its rounded card, fit-centered without changing its native aspect ratio, repeated by `MediaPlayer`, and releases its decoder when the screen leaves the window. A video stays transparent until its size transform is ready, preventing a stretched first frame. This prevents the ghosted transitional frames and the many-concurrent-animation problem that made the library flicker.

Training-area and equipment artwork uses the same fit-centered rule. Source crops are never stretched to the card dimensions, and repeated source sheets are cached once to avoid decode jank while the wizard is opening.

`ExerciseVideosTest` verifies that every movement the plan builder can prescribe has an MP4 for both coaches. `ExerciseVisualsTest` verifies its matching poster.

New demonstrations are produced from a same-camera, five-or-more-pose source sheet for Mia and Leo. The source may be exported as a GIF during production, but the shipped asset remains an H.264 loop; we do not interpolate with ghosted cross-fades or ship the old GIF renderer.
