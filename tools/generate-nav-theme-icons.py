"""Generate monochrome Android vector masters from the approved icon concept geometry.

The app colors the masters by selected navigation theme at runtime. Re-run this
after editing ICONS in the concept generator; do not hand-edit generated vectors.
"""
from pathlib import Path
from xml.etree import ElementTree as ET
import importlib.util

root = Path(__file__).resolve().parents[1]
source = root / "artifacts/icon-concepts/generate-nav-options.py"
spec = importlib.util.spec_from_file_location("nav_concepts", source)
concepts = importlib.util.module_from_spec(spec)
spec.loader.exec_module(concepts)
dest = root / "app/src/main/res/drawable"


def number(value):
    return str(float(value)).rstrip("0").rstrip(".") if "." in str(value) else str(value)


def path_for(tag):
    kind = tag.tag
    a = tag.attrib
    if kind == "path":
        return a["d"]
    if kind == "circle":
        x, y, r = map(float, (a["cx"], a["cy"], a["r"]))
        return f"M{number(x-r)},{number(y)}a{number(r)},{number(r)} 0 1,0 {number(2*r)},0a{number(r)},{number(r)} 0 1,0 -{number(2*r)},0z"
    if kind == "rect":
        x, y, w, h = map(float, (a["x"], a["y"], a["width"], a["height"]))
        r = min(float(a.get("rx", 0)), w/2, h/2)
        if not r:
            return f"M{x},{y}h{w}v{h}h-{w}z"
        return (f"M{number(x+r)},{number(y)}h{number(w-2*r)}"
                f"a{number(r)},{number(r)} 0 0,1 {number(r)},{number(r)}"
                f"v{number(h-2*r)}a{number(r)},{number(r)} 0 0,1 -{number(r)},{number(r)}"
                f"h-{number(w-2*r)}a{number(r)},{number(r)} 0 0,1 -{number(r)},-{number(r)}"
                f"v-{number(h-2*r)}a{number(r)},{number(r)} 0 0,1 {number(r)},-{number(r)}z")
    raise ValueError(f"Unsupported icon primitive: {kind}")


themes = {
    "enamel": ("#FF65E4C9", "#FF153B3B"),
    "pulse": ("#FFFF8B78", "#FF3A2833"),
    "ice": ("#FFAFD6FF", "#FF223F63"),
    "champion": ("#FFFFD37D", "#FF473A2E"),
    "spectrum": ("#FFD7A7FF", "#FF29213D"),
}
spectrum = ["#FF64E7CE", "#FFFF91A1", "#FF96C4FF", "#FFFFD180"]

for theme, (default_color, counter) in themes.items():
    for index, (name, geometry) in enumerate(concepts.ICONS.items()):
        color = spectrum[index % len(spectrum)] if theme == "spectrum" else default_color
        elements = ET.fromstring(f"<icon>{geometry}</icon>")
        paths = []
        for element in elements:
            fill = element.attrib["fill"]
            paint = counter if fill == "var(--cut)" else color
            alpha = ' android:fillAlpha="0.68"' if fill == "var(--shine)" else ""
            data = path_for(element).replace("&", "&amp;").replace('"', '&quot;')
            paths.append(f'    <path android:fillColor="{paint}"{alpha} android:pathData="{data}" />')
        content = ('<vector xmlns:android="http://schemas.android.com/apk/res/android" '
                   'android:width="24dp" android:height="24dp" '
                   'android:viewportWidth="64" android:viewportHeight="64">\n'
                   + '\n'.join(paths) + '\n</vector>\n')
        (dest / f"ic_theme_{theme}_{name.lower()}.xml").write_text(content, encoding="utf-8")
    print(theme)
