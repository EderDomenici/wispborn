# Prompt A5 — Arte da fagogenia: fatia 1 (contrato) e pranchas das fatias 2 a 5 (para o Codex)

Prompt para o Codex (Blockbench/Python). Continua o A4 (`docs/prompts/codex-art-prompts.md`, na branch da F5).
A parte 1 é contrato fechado: o código da fatia 1 usa exatamente esses nomes e tamanhos. A parte 2 é
proposta: as pranchas e os kits das fatias 2 a 5 são revisados por Eder e confirmados na spec de cada fatia.

```
Você vai produzir a arte da fagogenia do mod Wispborn (Minecraft 1.21.1). Trabalhe só em art/hud/.

Antes de começar, leia por inteiro:
- docs/superpowers/specs/2026-11-20-fagogenia-design.md (o que cada sistema é; seção 8 = telas e HUD)
- docs/superpowers/specs/2026-11-20-hud-b-grimorio-design.md (paleta B "Pergaminho e tinta", valores exatos)
- docs/specs/F5-hud-b-grimorio.md (layout atual do HUD e do grimório 316x200, contrato de sprites da A4)
- art/hud/LAYOUT.md, art/hud/src/generate_hud.py e art/hud/src/validate_hud.py (estenda, não reescreva)

CONTEXTO
- O grimório passa a ter as abas Wisp, Bestiário, Habilidades, Aparência e Corpo (substituem Traços e
  Aspectos). Só a aba Wisp funciona na fatia 1; as outras chegam uma por fatia.
- EP virou moeda (estilo soulslike): o jogador junta saldo e compra o nível no grimório. Ao morrer, o saldo
  cai numa nuvem de DNA que pode ser recuperada.
- Dentro do hospedeiro a mana cai sem parar; quando zera, o hospedeiro expulsa o wisp.
- Paleta B em tudo; nenhuma cor pode lembrar Deep Dark, Nether, End ou o cobre das Trial Chambers. A cor
  do wisp é independente da interface.

PARTE 1 — CONTRATO DA FATIA 1 (nomes e tamanhos fixos; o código já usa)
| Sprite (wispborn:...)       | Tamanho          | O que é |
|---|---|---|
| hud/dna_bar_ready           | 200x6, 64 quadros de 1 tick (como dna_bar_progress) | Barra de DNA cheia quando o saldo paga o próximo nível: a mesma hélice, mais clara, com um brilho que corre da esquerda para a direita |
| hud/mana_bar_warning        | 80x6, 64 quadros de 1 tick (como mana_bar_progress) | Mana perto da expulsão: a mesma onda em ferrugem #C4573A com núcleo claro; o código alterna com a barra normal para piscar |
| hud/dna_cloud_badge         | nine-slice, base 40x16, borda 3 (com .png.mcmeta) | Selo da nuvem de DNA no centro inferior: mesma família da moldura do consume_badge |
| hud/dna_cloud_arrow_0 ... _7 | 9x9 cada | Seta de direção. _0 aponta para cima (à frente do jogador) e cada número gira 45° no sentido horário (_2 = direita, _4 = baixo, _6 = esquerda). Ponta em osso, contorno escuro de 1 px |
| grimoire/bar_ep_ready       | 120x5 | Barra de EP do grimório quando dá para subir de nível (mesma lógica de dna_bar_ready, sem animação) |
O botão "Subir nível" reusa grimoire/button_primary, button_primary_hover e button_primary_disabled.

PARTE 2 — PRANCHAS E KITS PROPOSTOS (fatias 2 a 5; Eder revisa, a spec de cada fatia confirma)
Todas as pranchas: 316x200 de GUI em escala 2, sobre fundo escuro de jogo, com a barra de abas nova
(Wisp, Bestiário, Habilidades, Aparência, Corpo) e a aba da prancha ativa.
1. mockup_grimoire_wisp.png — a aba Wisp da fatia 1: retrato do wisp encurtado (área de 28 a 166 px), botão
   "Subir · 500 EP" de 92x18 dentro da moldura do retrato (x 12, y 170), "Lvl 3" e "1240 EP" na linha do
   topo, barra de EP cheia com bar_ep_ready.
2. mockup_bestiary.png — lista de espécies à esquerda (linha de 100x14 com ícone 9x9 e nome; normal,
   selecionada e "nunca vista"), Trilha do Parasita fixa no topo da direita, trilha da espécie com marcos em
   linha: nó oculto, descoberto e liberado; botão Liberar com preço; o que a espécie exige para ser possuída
   (nível, Calor I, Frio I). Kit: species_row (nine-slice), milestone_hidden/discovered/unlocked 9x9,
   trail_segment 8x3, lock 9x9, req_level/req_heat/req_cold 9x9.
3. mockup_abilities.png — 3 espaços de ativas no topo (22x22: vazio, ocupado, selecionado), lista de ativas
   liberadas para equipar, lista de passivas só leitura, graus com os pips atuais. Kit: ability_slot,
   ability_slot_selected, ability_slot_empty 22x22, ability_row (nine-slice), key_plate (nine-slice, 9 de
   altura).
4. mockup_appearance.png — o wisp em 3D, as três partes (núcleo, halo, rastro) como chips, a grade de
   padrões (miniaturas 24x24) e as cores liberadas (amostras 11x11: normal, selecionada, bloqueada), uma
   vaga de cor por cor do padrão. Kit: part_chip (nine-slice) e part_chip_active, pattern_frame 24x24,
   swatch_frame, swatch_selected e swatch_locked 11x11.
5. mockup_body.png — aba desativada (fatia 5): os cinco encaixes (Locomoção, Cobertura, Respiração, Arma,
   Sentidos) como molduras 22x22 com ícone, o medidor de complexidade (90x5) e a tarja "Disponível na
   metamorfose". Kit: gene_slot 22x22, icon_locomotion/covering/breathing/weapon/senses 9x9,
   complexity_bar 90x5, disabled_overlay (nine-slice, translúcido).
6. mockup_hud_fagogenia.png — o HUD da F5 com: barra de DNA em dna_bar_ready e o saldo em cima do orbe;
   o selo da nuvem de DNA ("23 m" com a seta) no slot central inferior; 3 espaços de ativas 20x20 à
   esquerda das chamas, com a tecla embaixo e a recarga em sombra que desce; o aviso de descoberta no topo
   direito ("Descoberto: Teia I — libere no grimório"). Kit: hud_ability_slot 20x20,
   hud_ability_cooldown 20x20 (sombra translúcida, o código recorta na altura), discovery_toast
   (nine-slice, 160x32 de base) com vaga de ícone 16x16.

ENTREGAS
- Parte 1: os PNG em art/hud/sprites/ e art/hud/grimoire/, com .png.mcmeta onde a tabela pede (animação
  ou nine-slice).
- Parte 2: as seis pranchas em art/hud/ e os kits em art/hud/proposed/ (fora do jogo até a spec confirmar).
- LAYOUT.md atualizado (a aba Wisp nova e o slot central inferior com prioridade conjuração > Consumir >
  nuvem) e validate_hud.py estendido para a parte 1.

REGRAS
- Tudo gerado pelo script, determinístico: rodar duas vezes dá os mesmos bytes. Nada de imagem por IA,
  nada redimensionado com filtro.
- Todo PNG é RGBA; alfa só 0 ou 255, exceto preenchimentos translúcidos de moldura, o disabled_overlay e o
  hud_ability_cooldown.
- Arte original; não copie sprites de outros jogos ou mods.

CONFERÊNCIA
- python art/hud/src/validate_hud.py passa.
- Abra as pranchas em 100% e 200%: nada borrado, nada ciano, texto da fonte do jogo legível.
- Liste no fim os arquivos criados e alterados.
```
