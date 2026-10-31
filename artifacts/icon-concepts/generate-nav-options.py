"""Build five self-contained, full-navigation visual concepts for review only."""
from pathlib import Path
from html import escape

HERE = Path(__file__).resolve().parent

# Original, solid geometric symbols. Each uses --ink for the body and --cut for
# counters/detail, so every option can be judged at its real 24 px nav size.
ICONS = {
    "Home": '''<path fill="var(--ink)" d="M32 5 5 28a4 4 0 0 0 5 6l3-2v20a5 5 0 0 0 5 5h28a5 5 0 0 0 5-5V32l3 2a4 4 0 0 0 5-6z"/><path fill="var(--cut)" d="M27 39h10v18H27z"/>''',
    "Workouts": '''<rect fill="var(--ink)" x="3" y="27" width="7" height="10" rx="2"/><rect fill="var(--ink)" x="11" y="17" width="13" height="30" rx="4"/><rect fill="var(--ink)" x="22" y="28" width="20" height="8" rx="3"/><rect fill="var(--ink)" x="40" y="17" width="13" height="30" rx="4"/><rect fill="var(--ink)" x="54" y="27" width="7" height="10" rx="2"/><path fill="var(--shine)" d="M14 21h3v22h-3zm29 0h3v22h-3z"/>''',
    "Library": '''<path fill="var(--ink)" d="M8 11a5 5 0 0 1 5-5h15a7 7 0 0 1 6 3 7 7 0 0 1 6-3h12a5 5 0 0 1 5 5v38a4 4 0 0 1-4 4H40a7 7 0 0 0-6 3 7 7 0 0 0-6-3H12a4 4 0 0 1-4-4z"/><path fill="var(--cut)" d="M31 14h5v34h-5z"/><path fill="var(--shine)" d="M14 18h12v3H14zm0 7h12v3H14zm25-7h12v3H39zm0 7h12v3H39z"/>''',
    "Progress": '''<rect fill="var(--ink)" x="8" y="38" width="10" height="18" rx="2"/><rect fill="var(--ink)" x="23" y="29" width="10" height="27" rx="2"/><rect fill="var(--ink)" x="38" y="19" width="10" height="37" rx="2"/><path fill="var(--ink)" d="m46 6 13 1-2 13-4-5-20 19-8-8-12 10-5-6 18-15 7 8 16-15z"/>''',
    "Nutrition": '''<path fill="var(--ink)" d="M32 16c-7-8-21-3-24 9-4 15 5 31 16 32 5 0 5-2 8-2s3 2 8 2c11-1 20-17 16-32-3-12-17-17-24-9z"/><path fill="var(--ink)" d="M32 17c-1-8 2-13 8-17l5 5c-5 2-8 5-9 12z"/><path fill="var(--shine)" d="M18 24c-3 4-4 10-2 16l4-1c-1-5 0-9 2-12z"/>''',
    "Endurance": '''<circle fill="var(--ink)" cx="40" cy="9" r="7"/><path fill="var(--ink)" d="m24 20 12-4a8 8 0 0 1 9 3l7 8 9 2-2 7-11-3-8-7-6 9 10 7 10 16-7 4-13-16-10-5-8 17-8-4 10-23-4-9-10 8-5-6 14-12a8 8 0 0 1 11-2z"/>''',
    "Scheduler": '''<rect fill="var(--ink)" x="6" y="10" width="52" height="48" rx="7"/><rect fill="var(--cut)" x="12" y="25" width="40" height="26" rx="2"/><rect fill="var(--shine)" x="17" y="5" width="6" height="14" rx="3"/><rect fill="var(--shine)" x="41" y="5" width="6" height="14" rx="3"/><path fill="var(--ink)" d="m19 39 7 7 18-17-4-4-14 13-3-3z"/>''',
    "Body": '''<circle fill="var(--ink)" cx="32" cy="12" r="9"/><path fill="var(--ink)" d="M19 25c-9 0-14 7-14 15v8h8v-8c0-2 1-4 3-5v22h11V43h10v14h11V35c2 1 3 3 3 5v8h8v-8c0-8-5-15-14-15l-7 5H26z"/><path fill="var(--shine)" d="M27 30h10v5H27z"/>''',
    "Apex": '''<path fill="var(--ink)" d="M20 6h24v25c0 9-5 16-12 16s-12-7-12-16z"/><path fill="var(--ink)" d="M18 11H7v10c0 11 7 15 15 17l2-7c-7-2-10-5-10-11v-2h4zm28 0h11v10c0 11-7 15-15 17l-2-7c7-2 10-5 10-11v-2h-4zM29 44h6v9h-6zm-9 9h24v6H20z"/><path fill="var(--cut)" d="m32 14 3 7 8 1-6 5 2 8-7-4-7 4 2-8-6-5 8-1z"/>''',
    "Settings": '''<path fill="var(--ink)" d="M27 4h10l2 7 6 3 7-3 7 10-5 5v7l5 5-7 10-7-3-6 3-2 7H27l-2-7-6-3-7 3-7-10 5-5v-7l-5-5 7-10 7 3 6-3z"/><circle fill="var(--cut)" cx="32" cy="32" r="10"/><circle fill="var(--ink)" cx="32" cy="32" r="5"/>''',
}

OPTIONS = [
    ("01", "Enamel sport", "Rich filled glyphs on individual enamel tiles.", "enamel"),
    ("02", "Pulse performance", "Coral-led, high-energy active navigation.", "pulse"),
    ("03", "Ice alloy", "Cool metallic highlights, precise geometry.", "ice"),
    ("04", "Championship", "Warm gold and mint, designed as club insignia.", "champion"),
    ("05", "Night spectrum", "Colorful luminous symbols, restrained dark shell.", "spectrum"),
]

NAMES = list(ICONS)


def icon(name, cls=""):
    return (f'<svg class="{cls}" viewBox="0 0 64 64" role="img" '
            f'aria-label="{escape(name)} icon">{ICONS[name]}</svg>')


def preview(number, title, detail, theme):
    nav = "".join(
        f'<div class="nav-item {"active" if name == "Workouts" else ""}" '
        f'style="--idx:{i}">{icon(name)}<span>{name}</span></div>'
        for i, name in enumerate(NAMES)
    )
    inventory = "".join(
        f'<div class="icon-sample" style="--idx:{i}"><div class="icon-plate">{icon(name)}</div>'
        f'<strong>{name}</strong></div>' for i, name in enumerate(NAMES)
    )
    return f'''<article class="option {theme}">
      <div class="option-title"><div><div class="serial">CONCEPT {number} / FULL NAVIGATION SET</div>
        <h2>{title}</h2><p>{detail}</p></div><div class="status">10 / 10 icons</div></div>
      <div class="phone">
        <div class="topline"><span>9:41</span><span>●●● ▰</span></div>
        <div class="welcome"><span>TRAIN SMARTER</span><strong>Good morning, athlete.</strong><small>Your next session is ready when you are.</small></div>
        <div class="session"><div class="session-icon">{icon("Workouts")}</div><div><small>UP NEXT · TODAY</small><strong>Full body strength</strong><span>45 min · Guided workout</span></div><b>↗</b></div>
        <div class="phone-space"></div>
        <div class="navigation">{nav}</div>
      </div>
      <div class="inventory"><div class="inventory-heading">ICON SYSTEM <span>Large preview · solid shapes</span></div>
      <div class="icon-grid">{inventory}</div></div>
    </article>'''


STYLE = """
:root {font-family:Inter,ui-sans-serif,system-ui,sans-serif;color:#f2faf9;background:#071523}
*{box-sizing:border-box} body{margin:0;background:radial-gradient(ellipse at 50% -8%,#20475b 0%,#081928 42%,#071523 90%)}
main{max-width:1230px;margin:auto;padding:44px 24px 70px}
.intro{max-width:840px;margin-bottom:30px}.intro small,.serial{letter-spacing:.21em;font-weight:800;font-size:11px;color:#67e3ca}
h1{font-size:clamp(30px,4.5vw,51px);letter-spacing:-.05em;margin:12px 0} .intro p{line-height:1.6;color:#a9bfcc;margin:0}
.option{--ink:#69e4cc;--shine:#d7fff5;--cut:#153b3b;--tile:#164a48;--selected:#24574f;--halo:#40edc66a;--back:#091b2b;--text:#b7d4db;--line:#365a66;
 background:linear-gradient(150deg,#1a3546,#102635 68%);border:1px solid #376073;border-radius:30px;padding:25px;margin-bottom:24px;box-shadow:0 24px 50px #0006,inset 0 1px #ffffff16}
.option-title{display:flex;justify-content:space-between;align-items:start;gap:20px}.option h2{font-size:25px;letter-spacing:-.04em;margin:7px 0 3px}.option-title p{font-size:13px;color:#b7cbd4;margin:0}.status{color:#a7ddce;border:1px solid #ffffff29;padding:8px 12px;border-radius:99px;white-space:nowrap;font-size:11px}
.phone{position:relative;margin:22px 0 20px;min-height:310px;padding:17px 18px 15px;display:flex;flex-direction:column;overflow:hidden;border-radius:24px;background:radial-gradient(ellipse at 80% 0%,#245565 0%,var(--back) 46%);border:1px solid #ffffff25}
.topline{font-size:11px;font-weight:800;display:flex;justify-content:space-between;color:#e6f9fa}.welcome{display:flex;flex-direction:column;gap:5px;margin:28px 0 18px}.welcome span{font-size:10px;font-weight:800;letter-spacing:.18em;color:var(--ink)}.welcome strong{font-size:22px;letter-spacing:-.04em}.welcome small{color:#bbd0d7;font-size:12px}
.session{display:flex;align-items:center;gap:14px;background:#ffffff12;border:1px solid #ffffff26;border-radius:17px;padding:13px 15px;max-width:430px}.session-icon{display:grid;place-items:center;width:49px;height:49px;border-radius:14px;background:var(--selected);color:var(--ink);flex-shrink:0}.session-icon svg{width:27px;height:27px}.session div:nth-child(2){display:flex;flex-direction:column;gap:2px}.session small{font-size:9px;letter-spacing:.1em;color:var(--ink);font-weight:800}.session strong{font-size:14px}.session span{font-size:11px;color:#bad2db}.session b{margin-left:auto;color:var(--ink)}
.phone-space{flex:1;min-height:30px}.navigation{display:flex;gap:4px;overflow-x:auto;scrollbar-width:none;background:#071521e8;border:1px solid #ffffff24;border-radius:18px;padding:7px}.navigation::-webkit-scrollbar{display:none}
.nav-item{min-width:87px;height:61px;border-radius:12px;flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:4px;color:#b1cad2;font-size:10px;white-space:nowrap}.nav-item.active{background:var(--selected);color:#fafffb;border:1px solid var(--halo);box-shadow:0 3px 13px var(--halo);font-weight:800}.nav-item svg{height:23px;width:23px;--ink:var(--text);--shine:var(--text);--cut:#112536}.nav-item.active svg{--ink:var(--accent,var(--ink));--shine:#fff5dc;--cut:#133934}
.inventory{background:#091a29a3;border:1px solid #ffffff19;border-radius:20px;padding:18px}.inventory-heading{font-size:11px;font-weight:800;letter-spacing:.15em;color:#8bd9d3;display:flex;justify-content:space-between;gap:10px}.inventory-heading span{font-weight:500;letter-spacing:0;color:#a6bbc5}.icon-grid{display:grid;grid-template-columns:repeat(10,minmax(0,1fr));gap:10px;margin-top:18px}.icon-sample{text-align:center;font-size:10px;color:#c7d8df;min-width:0}.icon-plate{width:54px;height:54px;margin:0 auto 8px;display:grid;place-items:center;border-radius:15px;background:var(--tile);border:1px solid #ffffff31;box-shadow:inset 0 1px #ffffff24,0 6px 13px #0005}.icon-plate svg{width:30px;height:30px}.icon-sample strong{font-size:10px;font-weight:600;overflow-wrap:anywhere}.option svg{--ink:var(--accent,#69e4cc);--shine:#ffffffad;--cut:#0e2d33}
.enamel{--accent:#65e4c9;--tile:#1f5b53;--selected:#1d544b;--halo:#6eefd54f}
.pulse{--accent:#ff8b78;--tile:#653c47;--selected:#704149;--halo:#ff8c7777;--back:#1b1729;--text:#ffc1b4}
.pulse .icon-plate{border-radius:10px;transform:skewX(-5deg)}.pulse .icon-plate svg{transform:skewX(5deg) rotate(-7deg)}.pulse .nav-item.active{transform:skewX(-5deg)}.pulse .nav-item.active>*{transform:skewX(5deg)}
.ice{--accent:#afd6ff;--tile:#294b71;--selected:#2b5379;--halo:#8cbfff73;--back:#0d1d3c;--text:#d0e6ff}.ice .icon-plate{border-radius:12px;background:linear-gradient(145deg,#5078a4,#203b61 68%);box-shadow:inset 0 2px #e6f7ff60,0 7px 16px #0008}
.champion{--accent:#ffd37d;--tile:#594b35;--selected:#62503a;--halo:#ffca6f71;--back:#181d2b;--text:#f5dca1}.champion .icon-plate{border-radius:50%;background:radial-gradient(circle at 35% 20%,#846b43,#3d362e 75%);border:2px solid #ddba7777}.champion .nav-item.active{border-radius:22px}
.spectrum{--accent:#d7a7ff;--tile:#3d3055;--selected:#493759;--halo:#d6a3ff75;--back:#151428;--text:#b9a5de}.spectrum .icon-sample:nth-child(4n+1){--accent:#64e7ce;--tile:#1e504d}.spectrum .icon-sample:nth-child(4n+2){--accent:#ff91a1;--tile:#663547}.spectrum .icon-sample:nth-child(4n+3){--accent:#96c4ff;--tile:#30486a}.spectrum .icon-sample:nth-child(4n){--accent:#ffd180;--tile:#614b36}.spectrum .icon-plate{box-shadow:0 4px 18px color-mix(in srgb,var(--accent),transparent 70%),inset 0 1px #ffffff38}
footer{color:#9fb6c1;font-size:12px;line-height:1.6;padding:0 4px}footer a{color:#78e7d1}
@media(max-width:800px){main{padding:27px 13px}.option{padding:17px}.icon-grid{grid-template-columns:repeat(5,1fr)}.navigation{margin-right:-3px}.phone{min-height:330px}}
"""

cards = "\n".join(preview(*opt) for opt in OPTIONS)
page = f'''<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Coach Hero · full navigation icon study</title><style>{STYLE}</style></head><body><main>
<header class="intro"><small>MY PERSONAL TRAINER / ICON STUDY</small><h1>A complete navigation, five distinct directions.</h1><p>Each preview includes all ten destinations—Home, Workouts, Workout Library, Progress, Nutrition, Endurance, Scheduler, Body, Apex and Settings. Every icon is a solid, self-contained SVG, shown on a phone-style navigation strip and as a larger set. Concept only; no app icons have been replaced.</p></header>
{cards}
<footer>Research references: <a href="https://github.com/phosphor-icons/core">Phosphor</a> (MIT), <a href="https://github.com/Templarian/MaterialDesign">Material Design Icons</a> (Pictogrammers license), and <a href="https://github.com/tabler/tabler-icons">Tabler</a> (MIT). Symbols in these mockups are original concept drawings, not copied assets. Under <a href="../../docs/DESIGN_LOCK.md">the Canvas design lock</a>, a selected direction needs approval and a matched-device comparison before release.</footer>
</main></body></html>'''

output = HERE / "full-navigation-options.html"
output.write_text(page, encoding="utf-8")
print(output)
