# Prompt A5 — Arte da fagogenia, fatias 1 a 4 (para o Codex)

Prompt para o Codex (Blockbench/Python). Continua o A4 (`docs/prompts/codex-art-prompts.md`, na branch da F5).
Os nomes e tamanhos abaixo são contrato: os planos das fatias 1 a 4 (`docs/superpowers/plans/2026-11-21-fatia*`)
usam exatamente esses ids. A fatia 5 (Corpo) fica para um prompt próprio, depois do brainstorm dela.

```
Você vai produzir a arte da fagogenia do mod Wispborn (Minecraft 1.21.1). Trabalhe em art/hud/ e, só para a
máscara de partes, em art/parasita-magico/.

Antes de começar, leia por inteiro:
- docs/superpowers/specs/2026-11-20-fagogenia-design.md (o que cada sistema é; seção 8 = telas e HUD)
- docs/superpowers/specs/2026-11-20-hud-b-grimorio-design.md (paleta B "Pergaminho e tinta", valores exatos)
- docs/specs/F5-hud-b-grimorio.md (layout atual do HUD e do grimório 316x200, contrato de sprites da A4)
- os layouts nos planos docs/superpowers/plans/2026-11-21-fatia1..4-*.md (coordenadas de cada página)
- art/hud/LAYOUT.md, art/hud/src/generate_hud.py e art/hud/src/validate_hud.py (estenda, não reescreva)
- art/parasita-magico/BRIEF.md (modelo A3: ossos e atlas 64x64)

CONTEXTO
- O grimório passa a ter as abas Wisp, Bestiário, Habilidades, Aparência e Corpo (Corpo fica desativada).
- EP virou moeda: o jogador compra o nível no grimório. Ao morrer, o saldo cai numa nuvem de DNA recuperável.
- Dentro do hospedeiro a mana cai sem parar; ao zerar, o hospedeiro expulsa o wisp.
- Paleta B em tudo; nenhuma cor pode lembrar Deep Dark, Nether, End ou o cobre das Trial Chambers. A cor do
  wisp é independente da interface: nenhum sprite de interface usa a cor do wisp.
- Estilo de moldura igual ao do HUD (chanfro de 3 px, contorno de 1 px, cantos com 1 px claro). Nine-slice
  sempre com .png.mcmeta, borda 3 salvo indicação.

ENTREGAS — HUD (art/hud/sprites/ → wispborn:hud/*)
| Sprite | Tamanho | O que é | Fatia |
|---|---|---|---|
| dna_bar_ready | 200x6, 64 quadros de 1 tick | A hélice do DNA mais clara, com um brilho correndo da esquerda para a direita: o saldo paga o próximo nível | 1 |
| mana_bar_warning | 80x6, 64 quadros de 1 tick | A onda da mana em ferrugem #C4573A com núcleo claro; o código alterna com a normal para piscar | 1 |
| dna_cloud_badge | nine-slice, base 40x16 | Selo da nuvem de DNA no centro inferior (família do consume_badge) | 1 |
| dna_cloud_arrow_0 … _7 | 9x9 | Seta; _0 para cima (à frente), cada número gira 45° no sentido horário (_2 direita, _4 baixo, _6 esquerda). Ponta osso, contorno escuro de 1 px | 1 |
| discovery_toast | nine-slice, base 160x32 | Aviso de descoberta no canto superior direito (duas linhas de texto) | 2 |
| hud_ability_slot | 20x20 | Espaço de ativa no HUD (o ícone 16x16 entra em +2,+2) | 3 |
| hud_ability_cooldown | 20x20, translúcido | Sombra da recarga; o código recorta de cima para baixo | 3 |

ENTREGAS — GRIMÓRIO (art/hud/grimoire/ → wispborn:grimoire/*)
| Sprite | Tamanho | O que é | Fatia |
|---|---|---|---|
| bar_ep_ready | 120x5 | Barra de EP quando dá para subir de nível | 1 |
| species_row, species_row_selected | nine-slice, base 100x14 | Linha da lista do Bestiário (só texto, sem ícone) | 2 |
| milestone_hidden, milestone_discovered, milestone_unlocked | 9x9 | Estado do marco: oculto (contorno apagado), descoberto (osso), liberado (ferrugem cheio) | 2 |
| req_ok, req_missing | 9x9 | Exigência de possessão atendida / faltando | 2 |
| ability_slot, ability_slot_selected, ability_slot_empty | 22x22 | Espaço de ativa na aba Habilidades | 3 |
| ability_row, ability_row_selected | nine-slice, base 100x18 | Linha de ativa liberada (ícone 16x16 + nome) | 3 |
| part_chip, part_chip_active | nine-slice, base 56x14 | Chips Núcleo / Halo / Rastro | 4 |
| pattern_frame, pattern_frame_selected | 24x24 | Moldura da miniatura de padrão | 4 |
| swatch_frame, swatch_selected | 11x11 | Moldura da amostra de cor | 4 |
| swatch_fill | 9x9, branco opaco | Miolo da amostra; o código tinge com a cor | 4 |

ENTREGAS — ÍCONES
| Sprite | Tamanho | O que é | Fatia |
|---|---|---|---|
| ability/web, ability/heat, ability/cold, ability/take_over, ability/body_jump | 16x16 | Ícones de habilidade (teia; chama; floco; mão sobre núcleo; seta saltando entre dois núcleos), na paleta B | 3 |
| pattern/solid, stripes, spots, veins, gradient, rings | 22x22, tons de cinza | Miniatura de cada padrão; o desenho segue as fórmulas do plano da fatia 4 (PatternMath) | 4 |

ENTREGA — MÁSCARA DE PARTES (art/parasita-magico/wisp_partmask.png, fatia 4)
- 64x64 RGBA, alinhada pixel a pixel com o atlas wisp.png do A3.
- Vermelho puro (255,0,0) nos pixels usados por core; verde puro nos de halo_inner e halo_outer; azul puro nos
  de trail_1..4, wake e satellite_1..4; transparente no resto.
- Gere a partir das UVs do wisp.geo.json (não pinte à mão) e estenda validate_a3.py: todo pixel opaco do
  wisp.png está em exatamente uma parte da máscara.

PRANCHAS (art/hud/, 316x200 de GUI em escala 2 sobre fundo escuro de jogo, com a barra de abas nova)
1. mockup_grimoire_wisp.png — fatia 1: retrato encurtado, botão "Subir · 500 EP" (92x18 em 12,170), "Lvl 3" e
   "1240 EP" no topo, barra de EP com bar_ep_ready.
2. mockup_bestiary.png — fatia 2: Trilha do Parasita no topo da lista, aranha selecionada com 5 consumos,
   exigências, marcos 3 (liberado), 5 (descoberto, "Liberar · 400"), 8 e 12 ocultos.
3. mockup_abilities.png — fatia 3: Teia no espaço 1, espaço 2 selecionado, espaço 3 vazio; lista de ativas;
   passivas Calor II e Tomar conta do corpo com os pips.
4. mockup_appearance.png — fatia 4: chip Núcleo ativo, padrão Manchas com três vagas, paleta com 8 cores.
5. mockup_hud_fagogenia.png — HUD da F5 com dna_bar_ready, o selo da nuvem "DNA · 23 m" com a seta, os três
   espaços de ativas à esquerda das linhas de vida e DNA (um em recarga) e o aviso de descoberta "Descoberto:
   Teia I / Libere no grimório".

REGRAS
- Tudo gerado pelo script, determinístico: rodar duas vezes dá os mesmos bytes. Nada de imagem por IA, nada
  redimensionado com filtro.
- Todo PNG é RGBA; alfa só 0 ou 255, exceto preenchimentos translúcidos de moldura e hud_ability_cooldown.
- Arte original; não copie sprites de outros jogos ou mods.

CONFERÊNCIA
- python art/hud/src/validate_hud.py e python art/parasita-magico/src/validate_a3.py passam, já estendidos
  para todas as tabelas acima.
- Abra as pranchas em 100% e 200%: nada borrado, nada ciano, texto da fonte do jogo legível.
- Atualize LAYOUT.md (páginas novas; slot central inferior com prioridade conjuração > Consumir > nuvem).
- Liste no fim os arquivos criados e alterados.
```
