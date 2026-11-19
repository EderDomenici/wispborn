# A2 HUD art contract

## Objective

Original pixel sprites for F4 and the V1 casting vignette. Variant A is the initial
selection for both health and hunger; `variants.png` exposes alternatives for review.
Art delivery is complete; in-game HUD programming and visual approval remain separate.

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
empty sparks (14/20). Extra health rows grow upward at a 10 px pitch, at most ten
icons per row. With `r` health rows, move the level row and host panel upward by
`10 * (r - 1)` GUI pixels. F4 must account for these rows in vanilla HUD stacking.
This art mockup does not implement GUI events or vanilla bubble positioning.

Health uses `core_container` underneath `core_full`/`core_half`. The right half of
`core_half` is transparent so the container remains visible. Hunger variants are
complete icons including their empty portions. Blinking containers are brighter.

## Host panel

| Element | X | Y | Size |
| --- | ---: | ---: | --- |
| `host_frame` | -91 | 99 | 182×44 |
| Host name | -82 | 93 | Available text width depends on trailing labels |
| Sex glyph / level | after measured name + 5 px | 93 | Glyph 5×8; level text |
| Health percent | right aligned at 82 | 93 | Font height 7–9 |
| Health background/progress | -82 | 80 | 164×6 |
| `host_floor_marker` | `-82 + floor(164 * floorPercent) - 1` | 81 | 3×8 |
| Consume status / configured key | -82 | 68 | Font height 7–9 |
| `consume_ready` | -82 | 69 | 9×9; shift following text right by 12 px |
| `no_ep` | 70 | 69 | 9×9; reserve its trailing space when visible |

Draw marker after both bar layers. The example uses Zombie, female, level 6,
62% health (101/164 px progress) and the 20% marker at pixel 32. At 62% the
consume-ready icon is hidden. Localized names must be measured and ellipsized;
reserve the percentage and level regions rather than letting text overlap.

## Target panel

| Element | X | Y | Size |
| --- | ---: | ---: | --- |
| `target_frame` | -75 | `H/2 + 53` | 150×32 |
| Target name, sex, level | -66 | `H/2 + 47` | Measured text; leave 21 px for status |
| `target_ok` / `target_blocked` | 55 | `H/2 + 49` | 9×9 |
| Action label | -66 | `H/2 + 37` | Font height 7–9 |
| Cast background/progress | -66 | `H/2 + 29` | 132×4 |

The panel ends 21 GUI pixels above the crosshair. No panel may cover the central
crosshair region. HOST mode normally has no target panel. Compact screens or
exceptionally many health rows require an F4 layout check; do not silently overlap.

## Sprite inventory and alpha

`manifest.json` lists all 24 sprite dimensions, including all 23 F4 assets plus
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

Glyph artwork occupies seven rows; the eighth is transparent. Pixels are white
for runtime text tinting. `font/icons.json` is the ready-to-copy provider descriptor
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

The A2 files remain in `art/hud/`. They are not a claim that F4 or V1 first-person
effects have been implemented. User-facing strings shown in mockups are examples;
production text must come from the mod's language files.

## Mana and XP/DNA animation revision

Requested size increase: mana is now 182×9 GUI pixels and XP/DNA is 182×7.
The width remains aligned with the vanilla hotbar footprint. Upper rows and host
panel move upward by 5 px to retain spacing. Mana text stays outside the bar.

Progress PNGs contain 16 vertically stacked frames: mana 182×144, DNA 182×112.
Their `.png.mcmeta` specifies frame width/height, 2 ticks per frame and no interpolation.
Mana flows in blue-violet with small moving highlights. DNA has two counterphase
emerald strands and connecting rungs. Both loop seamlessly; frame dimensions in
`manifest.json` are logical dimensions, not the full strip dimensions.

Copy the metadata alongside each PNG during F4 integration. The GUI atlas handles
texture animation; rendering must still crop to the actual resource fraction.
This is decorative motion, not invented changes to the player's mana or XP.
`bars_animated.gif` previews the loop at 100 ms per frame with resource values fixed.
The other mockups show frame zero. No Java HUD implementation is included.
