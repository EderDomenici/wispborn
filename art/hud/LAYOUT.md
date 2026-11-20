# A4 — Palette B HUD and Grimoire art contract

## Objective and scope

A4 extends the existing A2 generator with the approved **Parchment and ink** palette,
new HUD proportions, and the complete F5 Grimoire sprite kit. Only `art/hud/` is
owned by this art task. Antigravity owns F5 Java, runtime resource integration and
in-game verification. These mockups are original schematic previews, not screenshots.

Reference documents: `docs/specs/F5-hud-b-grimorio.md` and
`docs/superpowers/specs/2026-11-20-hud-b-grimorio-design.md`. The linked external
canvas could not be opened; the local tables are the source of truth.

## Palette B

| Role | Colors |
| --- | --- |
| Outline | `#120F0C` |
| Frame fill | `#1D1915`, alpha 225 |
| Bone border / corner highlight | `#CDBB94` / `#FFF3D6` |
| Text / secondary / disabled | `#F3EAD6` / `#B5A88F` / `#6F655A` |
| Rust emphasis | `#C4573A`, `#E2775F` |
| Bar rail / highlight / ticks | `#26211C` / `#3A332B` / `#4A4137` |
| Health | `#4A1B16`, `#7D2A21`, `#B5402F`, `#E2775F`, `#F7CBB8` |
| Hunger | `#6E5A2A`, `#C9A55A`, `#F2E2B0` |
| Empty / blinking | `#2B2621` / `#B5A88F` |
| Mana | `#3F5AA8`, `#9FB4EE`, `#24346A`, `#8AA0E6`, `#6A80CC`, `#D9E2FF` |
| DNA | `#A8976A`, `#FFF6D8`, `#6F644A`, `#D9C99C` |
| Target allowed / blocked | `#A8C46A` / `#D0563C` |

No cyan/teal remains in HUD or Grimoire sprites. The creature itself is independent
of interface colors: the Grimoire mockup contains a schematic cyan wisp portrait.
It is not a new texture or a replacement for the animated 3D player renderer.

## Bottom HUD

Coordinates are GUI pixels relative to bottom center. X grows right; Y grows up.
Each position is the **top-left**: `screenX = floor(W/2) + x`, `screenY = H - y`.
The mockups use logical 427×240 pixels, enlarged to 854×480 with nearest neighbor.

| Element | X | Y | Logical size / rule |
| --- | ---: | ---: | --- |
| DNA background/progress | -100 | 14 | 200×6; crop to progress |
| Health containers/foreground | -100 | 28 | 9×9, advance 8 px |
| DNA orb | -9 | 34 | 19×19; centered using integer pixels |
| Total EP text | centered | 43 | Example `8920`; `#FFF6D8` |
| Hunger | 20 | 36 | Ten 9×9 slots, advance 8 px |
| Mana background/progress | 20 | 26 | 80×6; no numerical label |
| Central consume badge | -42 | 61 | 84×16, bottom at H-45 |
| Consume-ready icon | -37 | 57 | 9×9 inside badge |
| Consume text | -24 | 56 | Font; configured key in game |
| Central cast bar | -66 | 49 | 132×4, bottom at H-45 |

The central slot ends 2 px above the EP text; WISP casting and HOST consuming are
mutually exclusive. Level and available attribute points appear only in the Grimoire.
Health beyond ten icons grows upward by 10 px per row. The upper central elements
and hunger follow F5's row-offset rules; runtime code must reserve space for air bubbles.

Hunger depletes **left to right**. In the 14/20 example, three empty slots are on
the left and seven full slots on the right. Slot `i` (0–9 from left) uses
`clamp(food - 2*(9-i), 0, 2)`; `hunger_half` retains its bright right half.

## Top cards

Behavior and source sizes remain those delivered for F4, now recolored:

- `target_frame`: 150×26 source, nine-slice border 4. Mockup displays 96×36 at
  `(cx-48, 2)`. Name at +6,+5; sex badge at +6,+14; level at +18,+15;
  status icon at +6,+24 and action text at +18,+25.
- `host_frame`: 128×24, nine-slice border 4. Mockup displays `(cx-64, 4)`.
  Name, sex badge, level and percent occupy the header. Health is 118×5 at
  `(cx-59, 19)`; the 3×8 floor marker is centered at 20% of its width.
- Move the shared top slot below boss bars in runtime. Target and host cards never
  coexist. The host example is 18% health to demonstrate the consume badge.

## Animation and source dimensions

| Sprite | Frame size | Frames | Frame time | PNG strip |
| --- | --- | ---: | ---: | --- |
| `mana_bar_progress` | 80×6 | 64 | 1 tick | 80×384 |
| `dna_bar_progress` | 200×6 | 64 | 1 tick | 200×384 |
| `consume_badge` | 84×16 | 16 | 2 ticks | 84×256 |

Mana wave and DNA helix use 1 px amplitude to fit the 6 px height without touching
the outer outline. The progress animations loop at 20 fps over 3.2 seconds.
Copy each `.png.mcmeta` next to its PNG. `manifest.json` records logical frame sizes.
Texture motion is decorative; the actual resource values still determine the crop width.

## Grimoire sprite kit

All 19 files in `grimoire/` map to `wispborn:grimoire/<name>`. Their metadata describes
`gui.scaling.type = nine_slice` where specified. The preview applies nine-slice
scaling separately to the nine regions; corners retain their pixel size.

| Sprites | Source size | Border |
| --- | --- | ---: |
| `panel` | 64×64 | 6 |
| `portrait_frame` | 32×32 | 4 |
| `tab_underline` | 16×3 | 1 |
| `pip_empty`, `pip_filled`, `pip_pending` | 7×7 | none |
| `button`, `button_hover`, `button_disabled` | 20×20 | 3 |
| `button_primary`, `button_primary_hover`, `button_primary_disabled` | 20×20 | 3 |
| `points_box_active`, `points_box_empty` | 24×24 | 4 |
| `bar_background`, `bar_life`, `bar_mana`, `bar_hunger`, `bar_ep` | 120×5 | none |

## Grimoire mockup layout

Panel: 316×200 GUI pixels, centered at (55,20) in the logical 427×240 scene.
The final preview is 854×480; the panel itself occupies 632×400 physical pixels.
All coordinates below are relative to the panel's top-left.

| Element | Position | Size / notes |
| --- | --- | --- |
| Panel | 0,0 | 316×200 |
| Title | 8,7 | Text |
| Tabs | starting 74,7 | Wisp active; others disabled; 6 px gap (allowed -2 adjustment) |
| Active underline | tab X,17 | Text width ×3 |
| Portrait frame | 8,24 | 100×168 |
| Level / EP label | 116,26 / 154,26 | Lvl 7; EP 8920/9800 |
| EP bar | 116,38 | 120×5 |
| Life / mana / hunger labels | 116,48 / 58 / 68 | Values 5/6, 42/70, 14/20 |
| Resource bars | 146,49 / 59 / 69 | 120×5; values centered at X=206 over the bars |
| Points box | 270,24 | 38×28; +2 remaining |
| Points caption | centered at 289,53 and 61 | Two lines avoid the resource column |
| Attribute rows | 116,84 + 22*i | Name; effect 10 px below |
| Minus / pips / plus | 202 / 215 / 297 | 11×11 buttons; ten 7×7 pips at 8 px pitch |
| Cancel / confirm | 190,176 / 252,176 | 56×18 |
| Pending hint | 116,181 | One pending point |

Example: Absorption has two committed pips plus one pending; Efficiency 3,
Reserve 4, Vitality 1. The +2 box means **two remaining after the displayed pending
point** (three unallocated before the staged click). This avoids presenting a counter
that ignores pending allocations. Numeric effects are illustrative, not balance changes;
runtime must calculate them through StatMath and config.

## Font and alpha

The three legacy glyphs and two 9×9 badges keep their shapes/colors and codepoints.
Badges remain blue `#3D8FD6` and pink `#D9628F`, with white symbols; their existing
outline is preserved. Render their text components in white to avoid multiplying colors.
The DNA text glyph stays white for text tinting. UI DNA sprites use palette B.

All PNGs are RGBA. Alpha is binary except for frame interiors (HUD frames, Grimoire
panel/portrait/points boxes) and the vignette. Buttons and pips are not translucent.
The cast vignette now uses rust-colored edges fading to a transparent center.

## Reproduction and delivery

```sh
/usr/bin/python3 art/hud/src/generate_hud.py
/usr/bin/python3 art/hud/src/validate_hud.py
```

Python 3 + Pillow. No AI imagery, antialiasing, blur, or filtered resizing.

- `sprites/`: 26 HUD sprites, including the new `dna_orb`, animation and frame metadata.
- `grimoire/`: 19 sprites and 11 nine-slice metadata files.
- `font/`: unchanged glyph contract, regenerated deterministically.
- `mockup.png`, `mockup_host.png`, `mockup_grimoire.png`: the three scenes.
- `review_200.png`, `grimoire_200.png`: nearest-neighbor 200% inspection sheets.
- `bars_animated.gif`: animated HUD preview; resource values are fixed.
- `variants.png`, `gender_symbols.png`, `vignette_preview.png`: updated review material.
- `manifest.json`, `grimoire_manifest.json`: logical dimensions.

Runtime integration is owned by F5: copy `sprites/` to
`assets/wispborn/textures/gui/sprites/hud/` and `grimoire/` to
`assets/wispborn/textures/gui/sprites/grimoire/`, including all `.png.mcmeta` files.
Do not copy mockups or review sheets to the atlas. A4 does not modify Java or resources.
