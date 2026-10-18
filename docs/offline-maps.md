# Offline maps

My Personal Trainer ships an optional, on-device **Trinidad & Tobago** vector map.

- Source data: OpenStreetMap contributors, via the OpenStreetData Trinidad & Tobago extract.
- License: Open Database License (ODbL) 1.0.
- Format: mapsforge vector map, rendered completely on the device after download.
- Build input: `TT.osm.pbf`, downloaded October 18, 2026.
- Output: `offline/trinidad-tobago.map` (9,972,071 bytes, SHA-256 `d8ea03c08cf99a8820c1c73519efeb3423792db98d763203ba65ddd6589375ec`).

The app verifies the file hash before storing it in its private app storage. It does not send GPS routes to the map source or any map service.
