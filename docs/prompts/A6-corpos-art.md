# Prompt A6 — Corpos da forma simples, peças de gene e casulo (para o Codex)

Prompt para o Codex (Blockbench). Os nomes de arquivos, ossos e clipes são contrato do plano da fatia 5
(`docs/superpowers/plans/2026-11-21-fatia5-metamorfose.md`, Tasks 5 e 6).

```
Você vai modelar os corpos da forma simples do mod Wispborn (Minecraft 1.21.1, GeckoLib 4.9.3). Trabalhe em
art/corpos/ e entregue os exports prontos para src/main/resources/assets/wispborn/.

Antes de começar, leia por inteiro:
- docs/superpowers/specs/2026-11-20-fagogenia-design.md, seção 7 (fases, genes, base do corpo, casulo)
- docs/superpowers/plans/2026-11-21-fatia5-metamorfose.md (Tasks 5 e 6)
- art/parasita-magico/BRIEF.md (o wisp A3: o núcleo chanfrado é a identidade que o corpo carrega)
- docs/ART_PIPELINE.md

CONTEXTO
- A forma simples é a primeira metamorfose do wisp. É uma criatura pequena e visível que parasita agarrada
  por fora do hospedeiro, estilo facehugger. O núcleo do wisp aparece no corpo (exposto ou sob uma membrana).
- O corpo é um boneco modular: uma base (postura) e uma peça por gene. Nesta entrega: as posturas neutra e
  quadrúpede, e os cinco genes da primeira leva.
- Paleta orgânica própria do corpo (carne, quitina, membrana); nada de ciano Deep Dark nem roxo End.

ENTREGAS
1. body_neutral.geo.json, body_neutral.animation.json, body_neutral.png (64x64): base neutra, mediana, sem
   especialidade (da pupa). Escala de referência: cabe num bloco com folga; o jogo aplica 0,6.
2. body_quadruped.geo.json, body_quadruped.animation.json, body_quadruped.png (64x64): quatro patas curtas,
   corpo baixo; o jogo aplica 0,8.
3. Em CADA base, estes ossos (nomes exatos; o código mostra e esconde):
   - base_locomotion, base_covering, base_breathing, base_weapon, base_senses: a parte "sem gene" de cada encaixe
   - gene_locomotion_spider_legs (patas de aranha), gene_weapon_spider_fangs (presas), gene_covering_cow_hide
     (couro com manchas), gene_breathing_cod_gills (guelras), gene_senses_bat_ears (orelhas de morcego)
   - core: o núcleo do wisp no corpo
   Cada gene_* ocupa o mesmo lugar do base_* do seu encaixe; os dois nunca aparecem juntos.
4. Clipes em cada base: animation.body.idle (loop), animation.body.walking (loop), animation.body.latched
   (loop, agarrada: patas fechadas em volta de um corpo, pulso de sucção). Cada clipe só cita ossos que existem.
5. cocoon.geo.json, cocoon.animation.json, cocoon.png (32x32): casulo orgânico de 1x1x1 bloco, com o núcleo
   brilhando por dentro; clipe animation.cocoon.pulse (loop de 2 s).
6. Sprites da aba Corpo em art/hud/grimoire/ (estendendo generate_hud.py, paleta B da interface):
   gene_slot e gene_slot_selected (22x22); icon_locomotion, icon_covering, icon_breathing, icon_weapon,
   icon_senses (9x9); complexity_bar_background e complexity_bar (90x5).
7. Capturas: cada base sem genes, com todos os genes, e no clipe latched; o casulo. Fundo escuro e claro.

REGRAS
- Fonte editável .bbmodel de cada modelo em art/corpos/.
- Script de validação art/corpos/src/validate_corpos.py: todo osso citado por animação existe; os 11 ossos de
  encaixe existem nas duas bases; texturas RGBA no tamanho declarado.
- Arte original; não copie modelos de outros jogos, filmes ou mods.
- Liste no fim os arquivos criados e alterados.
```
