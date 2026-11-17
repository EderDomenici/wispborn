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
