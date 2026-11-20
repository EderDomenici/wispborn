# A4 — Implementation report

## Outcome

Implemented the A4 art prompt in `art/hud/` only, extending the A2 generator.
Palette B replaces cyan/teal across the HUD and Grimoire UI. The independent wisp
portrait retains its creature colors. Runtime Java and resource copying belong to F5.
No branch switch was performed because Antigravity is implementing F5 concurrently.

## Deliverables

- 26 HUD sprites: inherited assets recolored, DNA 200×6, mana 80×6, new 19×19 DNA orb.
- 64-frame, 1-tick mana/DNA animation strips; 1px amplitude and seamless wrap.
- 19 Grimoire sprites and 11 nine-slice metadata files matching the F5 table.
- Normal, hover and disabled button states; empty, filled and pending pips;
  active/empty points boxes and five static resource bars.
- Three mockups, 200% inspection images, animated HUD preview, updated variants.
- Updated layout and validators; deterministic reproduction retained.
- Gender badge colors and shapes unchanged pixel-for-pixel.

## Verification

- `/usr/bin/python3 art/hud/src/validate_hud.py`: PASS.
- Both F5 sprite tables and inherited F4 dimensions validated.
- PNG mode/alpha, font codepoints, animation dimensions/timing and nine-slice borders: PASS.
- Cyan/teal exclusion in HUD/Grimoire sprites: PASS (gender glyphs are separate).
- 78 generated PNG/JSON/metadata/GIF files: identical hashes after a second generation.
- Mana and DNA each have 64 distinct frames; wrap delta does not exceed normal transitions.
- Mockups inspected at 100% and 200%; no filtered resizing.
- `./gradlew build`: BUILD SUCCESSFUL in 9s on the concurrent checkout.
  This does not constitute runtime rendering validation of the A4 assets.

## Integration for Antigravity

1. Copy `art/hud/sprites/` to `src/main/resources/assets/wispborn/textures/gui/sprites/hud/`.
2. Copy `art/hud/grimoire/` to `src/main/resources/assets/wispborn/textures/gui/sprites/grimoire/`.
3. Include **all `.png.mcmeta` files** with their corresponding PNGs.
4. Keep the current sex badge font providers; preserve white text tint on the badges.
5. Use `LAYOUT.md`, `manifest.json` and `grimoire_manifest.json` for logical sizes.
6. Do not copy mockups, GIFs or review sheets to runtime resources.

## Decisions and limitations

- External reference canvas was inaccessible; local F5/design tables were followed.
- Header tab gaps use the allowed -2px adjustment (6px) to fit the panel.
- Resource values are centered over their bars so they do not collide with the points box.
- Mockup shows two points remaining plus one pending Absorption point: three unallocated
  before that pending click. Stats are Absorption 2+1, Efficiency 3, Reserve 4, Vitality 1.
- Wisp portrait is a schematic illustration, not a screenshot of the live 3D renderer.
- F5 runtime integration, UI behavior and in-game visual approval remain with Antigravity/Eder.
- No files outside `art/hud/` were edited by this A4 task. The concurrent F5 source
  changes are not part of this art report or art commit.

## Created files

- `art/hud/REPORT.md`
- `art/hud/grimoire/bar_background.png`
- `art/hud/grimoire/bar_ep.png`
- `art/hud/grimoire/bar_hunger.png`
- `art/hud/grimoire/bar_life.png`
- `art/hud/grimoire/bar_mana.png`
- `art/hud/grimoire/button.png`
- `art/hud/grimoire/button.png.mcmeta`
- `art/hud/grimoire/button_disabled.png`
- `art/hud/grimoire/button_disabled.png.mcmeta`
- `art/hud/grimoire/button_hover.png`
- `art/hud/grimoire/button_hover.png.mcmeta`
- `art/hud/grimoire/button_primary.png`
- `art/hud/grimoire/button_primary.png.mcmeta`
- `art/hud/grimoire/button_primary_disabled.png`
- `art/hud/grimoire/button_primary_disabled.png.mcmeta`
- `art/hud/grimoire/button_primary_hover.png`
- `art/hud/grimoire/button_primary_hover.png.mcmeta`
- `art/hud/grimoire/panel.png`
- `art/hud/grimoire/panel.png.mcmeta`
- `art/hud/grimoire/pip_empty.png`
- `art/hud/grimoire/pip_filled.png`
- `art/hud/grimoire/pip_pending.png`
- `art/hud/grimoire/points_box_active.png`
- `art/hud/grimoire/points_box_active.png.mcmeta`
- `art/hud/grimoire/points_box_empty.png`
- `art/hud/grimoire/points_box_empty.png.mcmeta`
- `art/hud/grimoire/portrait_frame.png`
- `art/hud/grimoire/portrait_frame.png.mcmeta`
- `art/hud/grimoire/tab_underline.png`
- `art/hud/grimoire/tab_underline.png.mcmeta`
- `art/hud/grimoire_200.png`
- `art/hud/grimoire_manifest.json`
- `art/hud/mockup_grimoire.png`
- `art/hud/sprites/dna_orb.png`

## Modified files

- `art/hud/LAYOUT.md`
- `art/hud/bars_animated.gif`
- `art/hud/font/sex_female_badge.png`
- `art/hud/gender_symbols.png`
- `art/hud/manifest.json`
- `art/hud/mockup.png`
- `art/hud/mockup_host.png`
- `art/hud/review_200.png`
- `art/hud/sprites/cast_bar_background.png`
- `art/hud/sprites/cast_bar_progress.png`
- `art/hud/sprites/cast_vignette.png`
- `art/hud/sprites/consume_badge.png`
- `art/hud/sprites/consume_ready.png`
- `art/hud/sprites/core_container.png`
- `art/hud/sprites/core_container_blinking.png`
- `art/hud/sprites/core_full.png`
- `art/hud/sprites/core_half.png`
- `art/hud/sprites/dna_bar_background.png`
- `art/hud/sprites/dna_bar_progress.png`
- `art/hud/sprites/dna_bar_progress.png.mcmeta`
- `art/hud/sprites/dna_icon.png`
- `art/hud/sprites/host_floor_marker.png`
- `art/hud/sprites/host_frame.png`
- `art/hud/sprites/host_health_background.png`
- `art/hud/sprites/host_health_progress.png`
- `art/hud/sprites/hunger_empty.png`
- `art/hud/sprites/hunger_full.png`
- `art/hud/sprites/hunger_half.png`
- `art/hud/sprites/mana_bar_background.png`
- `art/hud/sprites/mana_bar_progress.png`
- `art/hud/sprites/mana_bar_progress.png.mcmeta`
- `art/hud/sprites/no_ep.png`
- `art/hud/sprites/target_blocked.png`
- `art/hud/sprites/target_frame.png`
- `art/hud/sprites/target_ok.png`
- `art/hud/src/generate_hud.py`
- `art/hud/src/validate_hud.py`
- `art/hud/variants.png`
- `art/hud/vignette_preview.png`
