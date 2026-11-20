# A2 HUD art contract

## Objective

Original pixel sprites for F4 and the V1 casting vignette. Variant A is the initial
selection for both health and hunger; `variants.png` exposes alternatives for review.
All PNGs were approved by Eder on 2026-11-19 and copied into runtime resources.
In-game HUD programming and visual testing remain pending.

## Coordinates

All measurements below are **GUI pixels**, before GUI scaling. Origin `(0, 0)` is
the bottom center; X grows right and Y grows up. Each coordinate identifies the
**top-left** of the element. Convert with `screenX = floor(W / 2) + x`,
`screenY = H - y`. W and H are scaled GUI dimensions.

The 854×480 mockups use GUI scale 2: a 427×240 canvas is enlarged with nearest
neighbor. They are original schematic scenes, not Minecraft screenshots.

## Bottom HUD

| Element | X | Y | Size | Rule |
| --- | ---: | ---: | --- | --- |
| Health containers and foreground | -91 | 44 | 9×9 each | Advance 8 px to the right; 2 HP per icon. |
| Hunger icons | 11 | 44 | 9×9 each | Ten slots, advance 8 px to the right. |
| DNA icon | -27 | 54 | 9×9 | Level row above health/hunger. |
| Level text | -14 | 52 | Font height 7–9 | `Nv 7`; grow to the right. |
| Available stat points | 38 | 52 | Font height 7–9 | `+2`; hide when zero. |
| DNA background/progress | -91 | 30 | 182×7 | Crop progress; do not stretch pixels. |
| Mana value | centered on 0 | 21 | Font height 7–9 | `Mana 42/70`; measure the localized string. |
| Mana background/progress | -91 | 12 | 182×9 | Crop progress; do not stretch pixels. |

Health examples use three containers (5/6 HP); hunger uses seven full and three
empty sparks (14/20). Hunger depletes from left to right: the three empty
slots are on the left and the seven full slots remain on the right. For slot
`i` from left to right (0–9), its fill is `clamp(food - 2 * (9 - i), 0, 2)`.
A partially depleted slot uses `hunger_half`, with its remaining bright half on the right. Extra health rows grow upward at a 10 px pitch, at most ten
icons per row. With `r` health rows, move the level row and the central bottom slot upward by
`10 * (r - 1)` GUI pixels. F4 must account for these rows in vanilla HUD stacking.
This art mockup does not implement GUI events or vanilla bubble positioning.

Health uses `core_container` underneath `core_full`/`core_half`. The right half of
`core_half` is transparent so the container remains visible. Hunger variants are
complete icons including their empty portions. Blinking containers are brighter.

## Host panel (revisado em 20/11/2026)

Compact panel at the **top center**, absolute Y from the top of the screen (not from the bottom).

| Element | X (from center) | Y (from top) | Size |
| --- | ---: | ---: | --- |
| `host_frame` (nine-slice, border 4) | -64 | top slot (2, below boss bars) | 128×24 |
| Host name | -59 | 8 | Measured, ellipsized before the suffix |
| Sex badge / level | after name + 3 px | 7 / 8 | Badge 9×9 (`U+E003`/`U+E004`); level text |
| `no_ep` (when the prey gives no EP) | left of the percentage, 2 px gap | 7 | 9×9 |
| Health percent | right aligned at 59 | 8 | Font height 7–9 |
| Health background/progress | -59 | 19 | 118×5 |
| `host_floor_marker` | `-59 + floor(118 * floorPercent) - 1` | 17 | 3×8 |

There is no consume text in the panel: the floor marker on the bar is the only hint
until the host reaches the floor.

## Central bottom slot (revisado em 20/11/2026)

A single slot centered above the level row (DNA icon + level, Y = 54 from the bottom).
Its bottom edge sits 56 px above the screen bottom and moves up with extra health rows
(`10 * (r - 1)`). HOST and WISP never use it at the same time.

| Element | When | X | Y (bottom edge, from bottom) | Size |
| --- | --- | ---: | ---: | --- |
| `consume_badge` (animated, 16 frames × 2 ticks) | HOST at the floor | -42 | 56 | 84×16 |
| `consume_ready` inside the badge | same | -33 | badge top + 4 | 9×9 |
| "Consumir [key]" text | same | -21 | badge top + 5 | Font, `#C7FFE8` |
| Cast background/progress | WISP while casting | -66 | 56 (top at 64) | 132×4 |

## Target card (opção B, 20/11/2026)

Card at the **top center**, same slot as the host panel (they never show together). Its
top is Y = 2, or 2 px below the last boss bar when one is on screen.

| Element | X (from card left) | Y (from card top) | Size |
| --- | ---: | ---: | --- |
| `target_frame` (nine-slice, border 4) | centered | 0 | width = widest line + 12, 72–160; height 36 |
| Target name | 6 | 5 | Ellipsized to 148 px |
| Sex badge + level | 6 | 14 / 15 | Badge 9×9; level in `#A7C9C4` |
| `target_ok` / `target_blocked` | 6 | 24 | 9×9 |
| "Infestar" | 18 | 25 | Green `#55DCA0` when possible, red `#FF7B7B` when not |

No key and no mana cost on the card. The cast bar lives in the central bottom slot.

## Sprite inventory and alpha

`manifest.json` lists all 25 sprite dimensions, including all 24 F4 assets plus
`cast_vignette` (256×256). All sprites are RGBA. Alpha is binary except for
`host_frame`, `target_frame`, and `cast_vignette`. Frames have opaque 1 px borders
and translucent interiors. Bars retain a dark outline and use short palettes.
The vignette fades in discrete alpha steps from cyan/emerald edges to a completely
transparent center; it is the only sprite intended to stretch across the screen.

## Bitmap font

| File | Size | Code point |
| --- | --- | --- |
| `font/sex_male.png` | 7×8 | U+E000 |
| `font/sex_female.png` | 5×8 | U+E001 |
| `font/dna.png` | 5×8 | U+E002 |

Glyph artwork occupies seven rows; the eighth is transparent. Female pixels are
pink `#F48FB1`, male pixels are blue `#64B5F6`, and DNA remains white. Render gender
glyph components with explicit white text color so their texture colors are preserved;
do not inherit the target status color. Tint only the white DNA glyph when needed. `font/icons.json` is the ready-to-copy provider descriptor
(`height: 8`, `ascent: 7`). During F4 integration place the PNGs in
`assets/wispborn/textures/font/` and the descriptor in `assets/wispborn/font/icons.json`.

## Reproduction and review

```sh
/usr/bin/python3 art/hud/src/generate_hud.py
/usr/bin/python3 art/hud/src/validate_hud.py
```

Dependencies: Python 3 and Pillow. Generation uses pixel operations and an original
5×7 bitmap alphabet for mockups. No AI imagery, blur, antialiasing or filtered resizing.

- `mockup.png`: unhosted wisp and target panel, GUI scale 2.
- `mockup_host.png`: host panel at 62% health.
- `variants.png`: two health and two hunger alternatives, enlarged with nearest neighbor.
- `review_200.png`: both mockups at 200% for pixel inspection.
- `vignette_preview.png`: vignette composited over the mockup for review only.

The editable A2 source files remain in `art/hud/`; approved runtime copies are in
`src/main/resources/assets/wispborn/`. F4 and V1 first-person effects are not yet
implemented. User-facing strings shown in mockups are examples;
production text must come from the mod's language files.

## Mana and XP/DNA animation revision

Requested size increase: mana is now 182×9 GUI pixels and XP/DNA is 182×7.
The width remains aligned with the vanilla hotbar footprint. Upper rows and host
panel move upward by 5 px to retain spacing. Mana text stays outside the bar.

Progress PNGs contain 64 vertically stacked frames: mana 182×576, DNA 182×448.
Their `.png.mcmeta` specifies frame width/height, 1 tick per frame (20 fps) and no interpolation.
Mana flows in blue-violet with small moving highlights. DNA has two counterphase
emerald strands and connecting rungs. Both loop seamlessly; frame dimensions in
`manifest.json` are logical dimensions, not the full strip dimensions.

Copy the metadata alongside each PNG during F4 integration. The GUI atlas handles
texture animation; rendering must still crop to the actual resource fraction.
This is decorative motion, not invented changes to the player's mana or XP.
`bars_animated.gif` previews the loop at 50 ms per frame with resource values fixed.
The other mockups show frame zero. No Java HUD implementation is included.

The refined loop lasts 3.2 seconds. Phase advances by half a texel per frame
instead of the previous two texels, reducing visible jumps while preserving crisp
pixel edges. The atlas has no spatial filtering or extra runtime code.

`gender_symbols.png` previews both colored gender glyphs. Gender color is independent
of infestation eligibility; target name/status keeps the F4 green/gray/red convention.
