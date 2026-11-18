# Prompts de arte para o Codex (Blockbench)

O Codex trabalha só em `art/`. Copiar para `src/main/resources/assets/wispborn/` é tarefa da spec
correspondente, nunca do prompt de arte.

---

## Prompt A1 — Wisp: raios novos e animação de possessão (para a M3b)

```
Você vai revisar o wisp do mod Wispborn no Blockbench. Trabalhe só em art/parasita-magico/.

Antes de começar, leia docs/ART_PIPELINE.md (direção visual, nomes, fluxo de entrega) e abra
art/parasita-magico/wisp.bbmodel (formato Bedrock/GeckoLib, textura 64x64 + wisp_glowmask.png).

CONTEXTO DO JOGO
- O wisp é o jogador transformado: um núcleo de energia ciano com satélites e rastro, que anda no
  chão (não voa). No jogo ele é desenhado com escala ~0,96 (hitbox 0,48 x visual 2x).
- Teste em jogo (print de Eder): de perto, os dois planos de raios (ray_xy e ray_yz) aparecem como um
  "X" de papel translúcido e borrado, esticado pela animação walking. Planos grandes, chatos e
  translúcidos não funcionam em 3D visto de perto.
- A possessão agora é uma conjuração de 1,0 s segurando uma tecla, mirando num mob a até 4 blocos.
  No fim dela o wisp some (entra no mob). Se o jogador soltar a tecla antes, o jogo corta a animação
  e volta para idle/walking.

TAREFA 1 — RAIOS
- Substitua os filhos do osso `rays` (ray_xy e ray_yz) por 8 a 12 fragmentos volumétricos: cuboides
  finos e alongados (ex.: 1x1x4 a 1x1x8 px, alguns 2x2x6), irradiando do núcleo em ângulos variados,
  em ossos ray_1..ray_N filhos de `rays`.
- Mantenha o osso `rays` com o mesmo nome e pivô (as animações idle/walking animam `rays`).
- Textura: pixels opacos ou quase opacos no corpo dos fragmentos, com transparência só nas pontas.
  Atualize wisp_glowmask.png para os fragmentos brilharem.
- Na animation walking, reduza o esticamento de `rays` (escala Z hoje vai a 1.25) para no máximo 1.1.

TAREFA 2 — ANIMAÇÃO DE POSSESSÃO
- Crie `animation.wisp.possess`: 1,0 s exatos, sem loop (toca uma vez), e REMOVA `animation.wisp.attack`.
- Antes, descubra a frente do modelo: no clipe walking, `body` inclina para a frente (rotação X 14–18°).
  Confira no preview para que lado ele se inclina e use esse lado como "frente" na investida.
- Fases (tempos em segundos):
  - 0,00–0,35 Recolhimento: satélites espiralam para perto do núcleo; raios contraem (~0,6); núcleo cresce e pulsa (~1,2).
  - 0,35–0,55 Antecipação: corpo recua 1–2 px e achata (squash), como quem toma impulso.
  - 0,55–0,85 Investida: corpo avança 6–8 px para a frente; raios esticam no eixo da frente como um
    cometa (frente ~1,6, laterais ~0,5); trail_1..4 alongam para trás.
  - 0,85–1,00 Entrada: tudo encolhe até ~0,1 na posição da investida (o wisp "entra" no hospedeiro).
- A pose de 0,0 s deve casar com idle (o jogo faz transição de 0 tick).

REGRAS
- Nomes de ossos em inglês e snake_case. Não renomeie root, body, core, rays, orbit, satellite_1..4,
  wake, trail_1..4.
- Arte original; não copie modelo, textura ou animação de outro jogo.
- Não edite nada fora de art/parasita-magico/.

ENTREGA
- Salve wisp.bbmodel e exporte wisp.geo.json, wisp.animation.json, wisp.png e wisp_glowmask.png
  (sobrescrevendo os atuais).
- Capturas novas: idle.png, walking.png e possess.png (o quadro de 0,7 s, no meio da investida).
  Apague attack.png.
- Escreva art/parasita-magico/BRIEF.md: qual eixo é a frente, número de fragmentos, tempos de cada
  fase da possessão e o que mudou nos raios.
- Confira: animation.wisp.possess existe, length 1.0, sem loop; animation.wisp.attack não existe;
  todo osso citado nas animações existe no geo.
```

---

## Prompt A2 — Sprites do HUD, fonte de ícones e mockup (para a F4 e a V1)

```
Você vai criar a arte do HUD do mod Wispborn (Minecraft 1.21.1). Trabalhe só em art/hud/.

Antes de começar, leia docs/ART_PIPELINE.md (direção visual) e docs/specs/F4-hud.md (contrato de
sprites e regras do HUD). A tabela "Contrato de sprites" da F4 é a lista obrigatória de entregas.

CONTEXTO DO JOGO
- O jogador é um wisp: um orbe de energia ciano/branco com bordas verde-esmeralda que parasita criaturas.
- Enquanto parasita, o HUD vanilla some (hotbar, corações, coxinhas, armadura, XP) e entra este HUD.
- Recursos na tela: vida do wisp, fome do wisp, mana, pontos de DNA (EP, a "experiência" do wisp,
  com nível), painel do hospedeiro (quando está dentro de um mob) e painel do alvo (mob na mira).

DIREÇÃO VISUAL
- Pixel art nítida no estilo do HUD do Minecraft: sem antialias, sem gradiente suave, sem desfoque,
  contorno escuro de 1 px como os ícones vanilla, paleta curta por ícone.
- Vida do wisp = NÚCLEO (não coração): um cristal/orbe de energia ciano e branco. O vazio é o contorno
  apagado.
- Fome do wisp = uma "faísca de essência" (chama pequena de energia verde-água que vai se apagando),
  nada de comida.
- Mana = cor de mana clássica: azul-royal puxando para violeta, com brilho claro no topo da barra.
  Precisa contrastar com o ciano do wisp.
- DNA = dupla hélice. dna_icon é uma hélice vertical de 9x9. A barra de DNA pode ter um padrão discreto
  de degraus de hélice na textura de progresso; cores verde-esmeralda e branco.
- Painel do hospedeiro e do alvo: molduras escuras translúcidas com borda de 1 px na cor do wisp.
- Arte original; não copie sprites de outros jogos ou mods.

ENTREGAS (todas em art/hud/)
1. sprites/<nome>.png para cada sprite da tabela da F4, nos tamanhos dela. Onde a F4 diz "definido no
   LAYOUT", você define o tamanho e registra no LAYOUT.md.
2. sprites/cast_vignette.png: vinheta de tela para a conjuração (V1), 256x256, borda ciano/esmeralda
   translúcida que esvai para totalmente transparente no centro. Será esticada para a tela inteira.
3. font/sex_male.png, font/sex_female.png, font/dna.png: glifos 7 px de altura (8 com a linha de base),
   brancos sobre transparente (o jogo colore pelo texto), para uma fonte bitmap.
4. LAYOUT.md: posição (x, y) e tamanho de cada elemento em pixels de GUI, relativos ao centro inferior
   da tela (x = 0 no centro, y = 0 na borda de baixo, y cresce para cima), mais o painel do hospedeiro
   e do alvo. Regras: vida à esquerda e fome à direita, como no vanilla; DNA e mana ocupam o espaço que
   era da hotbar e da barra de XP; nada pode cobrir a mira.
5. mockup.png: composição da tela inteira em 854x480 (escala de GUI 2) sobre um fundo neutro de jogo,
   mostrando o HUD do wisp com valores de exemplo (vida 5/6, fome 14/20, mana 42/70, Nv 7, DNA a 60%)
   E uma segunda versão com o painel do hospedeiro (Zumbi, fêmea, Nv 6, 62% de vida, marca de 20%).
6. variants.png: duas alternativas para o ícone de núcleo e duas para a faísca de fome, lado a lado,
   para Eder escolher.
7. Se você gerar os PNGs por script (ex.: Python + Pillow desenhando pixel a pixel), guarde o script em
   art/hud/src/ para dar para regenerar. Nada de imagem gerada por IA ou redimensionada com filtro.

CONFERÊNCIA
- Todo PNG é RGBA, sem pixels semitransparentes fora dos lugares de propósito (vinheta, molduras).
- Os nomes batem exatamente com a tabela da F4.
- Abra mockup.png em 100% e em 200%: os ícones continuam nítidos.
```

---

## Prompt A3 — Wisp: brilho em vez de espinhos (para a V1)

```
Você vai refazer o brilho do wisp do mod Wispborn no Blockbench. Trabalhe só em art/parasita-magico/.

Antes de começar, leia docs/ART_PIPELINE.md e docs/specs/V1-wisp-visual.md, e abra
art/parasita-magico/wisp.bbmodel (Bedrock/GeckoLib, textura 64x64 + wisp_glowmask.png).

O PROBLEMA
- No teste em jogo, os 10 fragmentos ray_1..ray_10 (varetas de 1x1x4 a 2x2x7 px) aparecem como
  espinhos ou estalactites claras saindo de um cubo. O wisp deveria ler como um ORBE DE ENERGIA QUE
  BRILHA, não como um cubo espinhoso. Varetas finas e longas sempre vão ler como espinho; não use mais.

TAREFA 1 — CORPO E BRILHO
- Remova ray_1..ray_10. MANTENHA o osso `rays` com o mesmo nome e pivô (idle, walking e possess animam
  `rays`): ele passa a ser o HALO.
- Núcleo (`core`): mais arredondado na silhueta (degraus de voxel, ex.: 5x5x5 com chanfros de 1 px
  feitos por cubos menores), branco no centro, ciano nas bordas, opaco.
- Halo (filhos de `rays`): no máximo duas cascas em volta do núcleo, maiores (ex.: 7x7x7 e 9x9x9),
  giradas 45° uma em relação à outra para quebrar a silhueta quadrada. Textura translúcida: mais
  opaca perto das arestas e quase transparente no meio das faces, em ciano e verde-água.
  Glowmask: o halo e o núcleo brilham; a casca externa brilha menos.
- Satélites e rastro: mantenha os ossos e nomes; ajuste o tamanho se o halo maior pedir.
- Teste de leitura: em vista de 1 bloco e de 16 blocos, a forma principal é um globo luminoso.

TAREFA 2 — ANIMAÇÕES
- Revise idle, walking e possess para o halo: em idle, `rays` pulsa a escala de 1,0 a 1,08 em 4 s;
  em walking, o halo estica no máximo 1,1 no eixo de movimento; em possess, mantenha as fases e tempos
  documentados no BRIEF.md (o halo contrai a 0,6, estica como cometa na investida e some com o corpo).
- Nenhum clipe pode citar osso que não exista mais.

REGRAS
- Nomes de ossos em inglês e snake_case. Não renomeie root, body, core, rays, orbit, satellite_1..4,
  wake, trail_1..4.
- Arte original. Não edite nada fora de art/parasita-magico/.

ENTREGA
- Salve wisp.bbmodel e exporte wisp.geo.json, wisp.animation.json, wisp.png e wisp_glowmask.png
  (sobrescrevendo os atuais).
- Capturas novas idle.png, walking.png e possess.png (quadro de 0,7 s), com fundo escuro e com fundo claro.
- Atualize BRIEF.md: o que mudou, tamanhos do halo, e a lista de ossos citados por cada clipe.
```
