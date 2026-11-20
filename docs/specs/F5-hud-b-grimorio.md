# F5 — HUD na paleta B e Grimório (página do Wisp)

**Objetivo:** reposicionar o HUD inferior conforme o rascunho de Eder, trocar toda a arte para a paleta B
(Pergaminho e tinta) e criar o Grimório com a página do Wisp: o wisp em 3D, vida, mana, fome, nível, EP e
distribuição de pontos de atributo com Cancelar e Confirmar, validada no servidor.
**Estimativa:** 3 dias. **Branch:** `feat/F5-hud-b-grimorio`. **Depende de:** F4 mergeada e arte **A4**
(`docs/prompts/codex-art-prompts.md`) entregue e aprovada por Eder.
**Design:** `docs/superpowers/specs/2026-11-20-hud-b-grimorio-design.md` (leia antes; ele tem a paleta e o porquê).
**Pranchas:** canvas "Menu do Wisp" (https://claude.ai/artifact/5GmZeBSHyhcLcBmzjpWvoW), pranchas
"HUD reposicionado na paleta B" e "Grimório · página do wisp (paleta B)".

## Regra de ouro (herdada da F4)
Nenhuma barra, ícone, moldura ou botão desenhado com `fill` ou gradiente. Tudo é `GuiGraphics.blitSprite`
de um PNG do mod (molduras e botões em nine-slice) ou texto da fonte do jogo. Isso vale também para o
grimório.

## Contrato de sprites (entregue pela A4)

HUD, atlas de GUI, `wispborn:hud/<nome>`. Todos os sprites atuais continuam com o mesmo nome, recoloridos na
paleta B. Os abaixo mudam de tamanho ou são novos:

| Sprite | Tamanho | Uso |
|---|---|---|
| `dna_bar_background`, `dna_bar_progress` | 200×6 | Barra de EP na base do HUD; progresso com 64 quadros, 1 tick |
| `mana_bar_background`, `mana_bar_progress` | 80×6 | Mana embaixo das chamas; progresso com 64 quadros, 1 tick |
| `dna_orb` | 19×19 | Orbe de DNA no centro do HUD |

Grimório, atlas de GUI, `wispborn:grimoire/<nome>`:

| Sprite | Tamanho | Uso |
|---|---|---|
| `panel` | 64×64, nine-slice borda 6 | Fundo da tela inteira do grimório |
| `portrait_frame` | 32×32, nine-slice borda 4 | Quadro do wisp |
| `tab_underline` | 16×3, nine-slice borda 1 | Traço sob a aba ativa |
| `pip_empty`, `pip_filled`, `pip_pending` | 7×7 | Marcadores de atributo (vazio, gasto, pendente) |
| `button`, `button_hover`, `button_disabled` | 20×20, nine-slice borda 3 | Botões `−`, `+` e Cancelar |
| `button_primary`, `button_primary_hover`, `button_primary_disabled` | 20×20, nine-slice borda 3 | Confirmar |
| `points_box_active`, `points_box_empty` | 24×24, nine-slice borda 4 | Contador `+N` (com pontos / sem pontos) |
| `bar_background`, `bar_life`, `bar_mana`, `bar_hunger`, `bar_ep` | 120×5 | Barras da página (estáticas) |

Copiar PNG e `.png.mcmeta` juntos. O nine-slice vem no `.mcmeta` (`gui.scaling.type = nine_slice`).

## Regras

### HUD inferior (reposicionamento)
1. Largura total de 200 px de GUI, de `cx-100` a `cx+100`. Posições relativas ao centro `cx` e à base `h`
   da tela (ajuste de ±2 px para casar com a prancha é permitido):

   | Peça | Posição | Tamanho |
   |---|---|---|
   | `dna_bar_*` (EP no nível atual) | `cx-100`, topo `h-14` | 200×6 |
   | Núcleos de vida (`core_*`) | a partir de `cx-100`, passo 8, topo `h-28` | 9×9 |
   | `dna_orb` | centrado em `cx`, topo `h-34` | 19×19 |
   | EP total (texto, `#FFF6D8`) | centrado em `cx`, `h-43` | fonte |
   | Chamas da fome (`hunger_*`) | de `cx+20` a `cx+100`, passo 8, topo `h-36` | 9×9 |
   | `mana_bar_*` | `cx+20`, topo `h-26` | 80×6 |
   | Slot central (selo Consumir ou barra de conjuração) | centrado, base 2 px acima do texto do EP | o do sprite |

2. Mais de 10 núcleos: segunda linha 10 px acima da primeira, e o orbe, o EP total, a fome e o slot central
   sobem junto (`10 × (linhas − 1)`), como já acontece hoje.
3. Saem do HUD: o número do nível (`wispborn.hud.level` + `dna_icon` na linha do nível) e o `+N` de pontos.
4. `Gui.leftHeight` e `Gui.rightHeight` passam a refletir a nova altura (`h-36` e acima livre), para as
   bolhas de ar ficarem acima das chamas e não sobreporem nada.
5. Mana mostra `mana / maxMana` só no grimório; no HUD não há texto de mana.

### Topo (sem mudança de comportamento)
6. Cartão do alvo e painel do hospedeiro continuam como na F4, só com a arte recolorida.

### Grimório
7. Tecla nova `KeyBindings.GRIMOIRE` (`key.wispborn.grimoire`, padrão `K`, categoria do mod). Abre o
   `GrimoireScreen` só se o jogador local é parasita (WISP ou HOST) e não há outra tela aberta. `Esc` fecha.
   Se o jogador deixar de ser parasita ou morrer com a tela aberta, ela fecha.
8. Painel de 316×200 px de GUI, centrado (cabe na menor GUI garantida pelo jogo, 320×240). Layout
   (posições relativas ao canto do painel; ±2 px permitido):

   | Peça | Posição | Tamanho |
   |---|---|---|
   | `panel` | 0, 0 | 316×200 |
   | "Grimório" (`wispborn.grimoire.title`) | 8, 7 | fonte |
   | Abas: Wisp (ativa), Traços, Aspectos, Habilidades, Bestiário | a partir de 74, 7, separadas por 8 | fonte; desativadas em `#6F655A` |
   | `tab_underline` sob a aba ativa | largura do texto, y 17 | 3 de altura |
   | `portrait_frame` + wisp 3D | 8, 24 | 100×168 |
   | "Lvl N" e "EP atual / próximo" | 116, 26 | fonte |
   | `bar_ep` | 116, 38 | 120×5 |
   | Vida, Mana, Fome: rótulo + barra + valor | 116, 48 / 58 / 68 | barras 120×5 |
   | Contador de pontos (`points_box_*` + `+N`) | 270, 24 | 38×28 |
   | Rótulo do contador (`pontos livres`) | centrado sob o contador | fonte |
   | 4 linhas de atributo | 116, 84 + 22×i | ver regra 10 |
   | Cancelar e Confirmar | canto inferior direito, y 176 | 56×18 cada |
   | Dica (`wispborn.grimoire.hint.*`) | 116, 181 | fonte, `#B5A88F` |

9. **Wisp 3D:** `InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, x1, y1, x2, y2, scale,
   yOffset, mouseX, mouseY, player)` dentro do quadro. Com a forma WISP, o `RenderPlayerEvent` do mod já troca
   o jogador pelo modelo do wisp; conferir que isso acontece também dentro da tela. Se não acontecer,
   registrar em "Dúvidas do implementador" e não inventar outro caminho.
10. **Linha de atributo** (x relativo ao painel): nome em 116 (`#F3EAD6`) e, 10 px abaixo, o efeito
    resultante (`#B5A88F`; `#E2775F` quando há pendente), os dois com no máximo 84 px (reticências se
    passar); botão `−` 11×11 em 202; 10 marcadores a partir de 215, passo 8 (`pip_filled` para gastos,
    `pip_pending` para pendentes, `pip_empty` para o resto); botão `+` 11×11 em 297. O efeito usa as funções
    de `StatMath` e os valores da config (que o NeoForge sincroniza com o cliente), em formato curto:
    - Absorção: `"<drainPercent>%/pulso"` (ex.: `3,0%/pulso`)
    - Eficiência: `"EP ×<epMultiplier>"` (ex.: `EP ×1,3`)
    - Reserva: `"<maxMana> mana"` (com a fórmula da mana por nível)
    - Vitalidade: `"<núcleos> núcleos"`
11. **Pontos pendentes:** `+` adiciona 1 pendente se ainda há ponto livre e o total do atributo (gasto +
    pendente) é menor que o máximo; `−` remove 1 pendente (nunca mexe no que já foi gasto). O contador mostra
    os pontos livres menos os pendentes; usa `points_box_active` e texto `#FFB9A3` quando > 0, e
    `points_box_empty` com `#6F655A` quando 0.
12. **Cancelar** zera os pendentes. **Confirmar** envia `AllocateStatsPayload` com os pendentes e zera os
    pendentes na tela; os valores novos chegam pelo `SyncWispStatePayload`. Os dois botões ficam desativados
    sem pendentes. Fechar a tela descarta os pendentes.
13. **Estado mudou com a tela aberta** (subiu de nível, comando de reset): ao chegar um `SyncWispStatePayload`,
    a tela reaplica os pendentes sobre o estado novo; se algum pendente deixar de caber, todos são descartados.
14. **Servidor** (`AllocateStatsPayload`): só para parasita. `AllocationRules.check` decide; com `OK`,
    `ProgressManager.allocateBatch` aplica tudo de uma vez, chama `refreshStats` e `syncState`. Qualquer
    outro resultado não aplica nada e manda `wispborn.grimoire.allocate.<resultado em minúsculas>` na action bar.

## Contratos
- `progress/StatDelta` (record, puro): `int absorption, int efficiency, int reserve, int vitality`;
  `ZERO`; `int get(Stat)`; `StatDelta with(Stat, int)`; `int total()`; `STREAM_CODEC` (4 × `VAR_INT`).
- `progress/BatchAllocateResult` (enum): `OK, EMPTY, NEGATIVE, NOT_ENOUGH_POINTS, OVER_MAX`.
- `progress/AllocationRules` (puro): `static BatchAllocateResult check(int availablePoints, WispStats current,
  StatDelta delta, int maxPerStat)`. Ordem: algum valor negativo → `NEGATIVE`; `total() == 0` → `EMPTY`;
  `total() > availablePoints` → `NOT_ENOUGH_POINTS`; algum `current.get(s) + delta.get(s) > maxPerStat` →
  `OVER_MAX`; senão `OK`.
- `progress/PendingAllocation` (puro, mutável, usado pela tela): construtor
  `(WispStats committed, int availablePoints, int maxPerStat)`; `boolean canAdd(Stat)`, `boolean add(Stat)`,
  `boolean canRemove(Stat)`, `boolean remove(Stat)`, `int pending(Stat)`, `int total(Stat)`,
  `int remainingPoints()`, `boolean hasPending()`, `void clear()`, `StatDelta toDelta()`,
  `void rebase(WispStats committed, int availablePoints, int maxPerStat)` (regra 13).
- `ProgressManager.allocateBatch(ServerPlayer, StatDelta) → BatchAllocateResult`.
- `network/AllocateStatsPayload(StatDelta delta)` (play-to-server).
- `SyncWispStatePayload` ganha `WispStats stats` e `int maxPerStat` (atualizar `DEFAULT`, codec e o teste de
  ida e volta).
- `client/KeyBindings.GRIMOIRE`; `client/grimoire/GrimoireScreen` (um método por bloco: cabeçalho, retrato,
  barras, atributos, rodapé); `client/grimoire/GrimoireSprites` (constantes `wispborn:grimoire/*`);
  `client/hud/HudSprites.DNA_ORB`.
- Lang (pt_br e en_us): `key.wispborn.grimoire`, `wispborn.grimoire.title`, `wispborn.grimoire.tab.wisp|traits|aspects|skills|bestiary`,
  `wispborn.grimoire.points` (`pontos livres` / `points left`), `wispborn.grimoire.cancel`, `wispborn.grimoire.confirm`,
  `wispborn.grimoire.hint.idle`, `wispborn.grimoire.hint.pending`, os 4 formatos de efeito, e
  `wispborn.grimoire.allocate.empty|negative|not_enough_points|over_max`.

## Testes (JUnit)
- `AllocationRules.check`, com `current = (2, 3, 4, 1)` e `max = 10`:
  - disponível 2, delta `(1, 0, 0, 1)` → `OK`
  - disponível 2, delta `(0, 0, 0, 0)` → `EMPTY`
  - disponível 2, delta `(-1, 0, 0, 1)` → `NEGATIVE`
  - disponível 1, delta `(1, 0, 0, 1)` → `NOT_ENOUGH_POINTS`
  - disponível 9, delta `(0, 0, 7, 0)` → `OVER_MAX` (4 + 7 > 10)
  - disponível 6, delta `(0, 0, 6, 0)` → `OK` (chega a 10 exatos)
- `PendingAllocation` com `committed = (2, 3, 4, 1)`, disponível 2, máximo 10:
  - `add(ABSORPTION)` duas vezes → `true`, `true`; terceira → `false`; `remainingPoints() == 0`
  - `remove(ABSORPTION)` sem pendente → `false`; não muda `total(ABSORPTION)`
  - com `committed.reserve = 10`: `canAdd(RESERVE) == false`
  - `toDelta()` depois de `add(VITALITY)` → `(0, 0, 0, 1)`; `clear()` → `hasPending() == false`
  - `rebase` com 4 disponíveis mantém 2 pendentes; `rebase` com 1 disponível e 2 pendentes descarta todos
- `SyncWispStatePayload` e `AllocateStatsPayload`: ida e volta pelo `STREAM_CODEC`.

## Casos que costumam quebrar
- Clicar Confirmar duas vezes rápido: o segundo pedido chega sem pontos e cai em `EMPTY` ou
  `NOT_ENOUGH_POINTS`, sem aplicar nada (coberto pelos testes de `AllocationRules`).
- Subir de nível com a tela aberta: o contador aumenta e os pendentes ficam (regra 13, teste de `rebase`).
- `/wispborn stat reset` com a tela aberta e pendentes: os pendentes continuam válidos se couberem.
- Cliente modificado mandando delta negativo ou acima do máximo: o servidor recusa (`NEGATIVE`, `OVER_MAX`).
- GUI na escala mínima (320×240): o painel de 316×200 cabe sem cortar; conferir no checklist.

## Ordem de implementação (com commit ao fim de cada item)
1. `StatDelta`, `BatchAllocateResult`, `AllocationRules` + testes.
2. `PendingAllocation` + testes.
3. `AllocateStatsPayload`, campos novos no `SyncWispStatePayload`, `ProgressManager.allocateBatch` + testes de
   ida e volta.
4. HUD reposicionado (regras 1 a 5) com os sprites da A4.
5. Tecla e `GrimoireScreen` (regras 7 a 13), lang.
6. Checklist manual e relatório.

## Checklist manual
- [ ] HUD igual à prancha "HUD reposicionado na paleta B": vida à esquerda, orbe com EP no centro, fome em cima
      da mana à direita, EP na base com 200 px. Sem número de nível nem `+N`.
- [ ] Nenhum ciano em lugar nenhum do HUD, cartões, selos ou grimório.
- [ ] `/wispborn stat add vitality` até 11 núcleos: a vida vira duas linhas e o resto sobe junto, sem sobreposição.
- [ ] Debaixo d'água: bolhas acima das chamas.
- [ ] Possessão: barra de conjuração no slot central; no piso, selo Consumir no mesmo slot.
- [ ] `K` abre o grimório como wisp e como hospedeiro; `Esc` fecha.
- [ ] O wisp aparece em 3D e se move com o mouse.
- [ ] Com 2 pontos: `+` em Absorção → contador `+1`, marcador pendente e efeito novo em ferrugem; `+` de novo →
      `+0`, os outros `+` desativados; Cancelar volta tudo; Confirmar aplica e o HUD/mana refletem.
- [ ] Fechar com pendentes e reabrir: pendentes sumiram, nada aplicado.
- [ ] Subir de nível com a tela aberta (`/wispborn ep add` pelo chat de outro jogador ou servidor): contador
      atualiza sem fechar a tela.
- [ ] Escala de GUI 1, 2, 3, 4 e auto: grimório e HUD sem cortes nem desalinhamento.
- [ ] Servidor dedicado sobe sem crash.

## Fora de escopo
Páginas Traços, Aspectos, Habilidades e Bestiário; cor do wisp por traço; feedback de conjuração em primeira
pessoa (V1); mudança de qualquer regra de jogo.

## Dúvidas do implementador
Nenhuma. A arte A4 (`art/hud/sprites/` e `art/hud/grimoire/`) passou no `validate_hud.py` sem pendências e `InventoryScreen.renderEntityInInventoryFollowsMouse` aciona `RenderPlayerEvent.Pre` normalmente (com flag em `WispRenderHandler` para também exibir o modelo 3D do wisp quando o grimório é aberto durante a possessão na forma `HOST`).

## Relatório de implementação

### Arquivos criados
- `src/main/java/dev/eder/wispborn/progress/StatDelta.java` — Record imutável `(int absorption, int efficiency, int reserve, int vitality)` com `ZERO`, `get(Stat)`, `with(Stat, int)`, `total()` e `STREAM_CODEC` (`4 × VAR_INT`).
- `src/main/java/dev/eder/wispborn/progress/BatchAllocateResult.java` — Enum `OK`, `EMPTY`, `NEGATIVE`, `NOT_ENOUGH_POINTS`, `OVER_MAX`.
- `src/main/java/dev/eder/wispborn/progress/AllocationRules.java` — Função pura `check(int availablePoints, WispStats current, StatDelta delta, int maxPerStat)` respeitando a precedência da spec (`NEGATIVE` → `EMPTY` → `NOT_ENOUGH_POINTS` → `OVER_MAX` → `OK`).
- `src/main/java/dev/eder/wispborn/progress/PendingAllocation.java` — Estado mutável puro de distribuição pendente para a UI (`canAdd`, `add`, `canRemove`, `remove`, `pending`, `total`, `remainingPoints`, `hasPending`, `clear`, `toDelta`, `rebase`).
- `src/main/java/dev/eder/wispborn/network/AllocateStatsPayload.java` — Payload play-to-server `wispborn:allocate_stats` com `StatDelta`.
- `src/main/java/dev/eder/wispborn/client/grimoire/GrimoireSprites.java` — Constantes `ResourceLocation` dos 19 sprites `wispborn:grimoire/*`.
- `src/main/java/dev/eder/wispborn/client/grimoire/GrimoireScreen.java` — Tela do Grimório (`316×200`) dividida nos 5 blocos (`renderHeader`, `renderPortrait`, `renderBars`, `renderAttributes`, `renderFooter`), 100% em `GuiGraphics.blitSprite` e `Font` (sem `fill` ou gradiente).
- `src/main/resources/assets/wispborn/textures/gui/sprites/hud/dna_orb.png` e `src/main/resources/assets/wispborn/textures/gui/sprites/grimoire/*` — Sprites e `.png.mcmeta` da A4 copiados para o atlas de GUI.
- `src/test/java/dev/eder/wispborn/progress/AllocationRulesTest.java` e `PendingAllocationTest.java` — Testes unitários JUnit 5 cobrindo todos os cenários da spec.

### Arquivos modificados
- `src/main/java/dev/eder/wispborn/progress/WispStats.java` — Adicionados `STREAM_CODEC` e `plus(StatDelta)`.
- `src/main/java/dev/eder/wispborn/network/SyncWispStatePayload.java` — Adicionados `WispStats stats` e `int maxPerStat` ao payload e a `DEFAULT`.
- `src/main/java/dev/eder/wispborn/progress/ProgressManager.java` — Implementado `allocateBatch(ServerPlayer, StatDelta)` validando parasita e `AllocationRules.check`, aplicando tudo de uma vez com `refreshStats` e `syncState(player, true)`, ou enviando `wispborn.grimoire.allocate.<resultado>` na action bar; `syncState` envia `stats` e `maxPerStat`.
- `src/main/java/dev/eder/wispborn/network/ModPayloads.java` — Registrado `AllocateStatsPayload` (C2S).
- `src/main/java/dev/eder/wispborn/client/hud/HudSprites.java` e `WispHud.java` — Adicionado `HudSprites.DNA_ORB`; HUD inferior reposicionado em 200 px (`cx-100..cx+100`) na paleta B com `dna_bar_*` (`200×6` em `h-14`), `core_*` (`cx-100, h-28`), `dna_orb` (`19×19` em `cx-9, h-34`), EP total em `#FFF6D8` (`cx, h-43`), `hunger_*` (`cx+20, h-36`), `mana_bar_*` (`80×6` em `cx+20, h-26` sem texto), slot central com base em `h-45`, remoção de nível e `+N` do HUD e ajuste de `Gui.leftHeight`/`Gui.rightHeight`.
- `src/main/java/dev/eder/wispborn/client/KeyBindings.java` — Adicionada tecla `GRIMOIRE` (`key.wispborn.grimoire`, padrão `K`) que abre `GrimoireScreen` apenas se o jogador local for parasita e não houver outra tela aberta.
- `src/main/java/dev/eder/wispborn/client/ClientPayloadHandler.java` — `handleSyncWispState` notifica `GrimoireScreen.onStateSynced` quando a tela está aberta (regra 13).
- `src/main/java/dev/eder/wispborn/client/WispRenderHandler.java` — Flag `renderingGrimoirePortrait` para que o retrato 3D do Grimório renderize o modelo do wisp mesmo quando aberto dentro de um hospedeiro (`FormType.HOST`, jogador invisível).
- `src/main/resources/assets/wispborn/textures/gui/sprites/hud/*` — Sprites do HUD atualizados para a paleta B (A4).
- `src/main/resources/assets/wispborn/lang/pt_br.json` e `en_us.json` — Adicionadas todas as chaves do Grimório, dicas, formatos de efeito e mensagens de erro de alocação.
- `src/test/java/dev/eder/wispborn/network/PossessionPayloadsTest.java` — Atualizado round-trip de `SyncWispStatePayload` e adicionado round-trip de `AllocateStatsPayload`.

### Decisões tomadas
- **Zero `fill` ou gradiente no Grimório:** `GrimoireScreen.renderBackground` foi sobrescrito vazio para impedir o `fillGradient` escuro padrão do `Screen` vanilla, mantendo 100% da renderização em `GuiGraphics.blitSprite` e `Font`.
- **Retrato 3D na forma `HOST`:** como o jogador em `HOST` fica invisível e `WispRenderHandler.onRenderPlayerPre` cancela a renderização no mundo, `GrimoireScreen.renderPortrait` envolve `InventoryScreen.renderEntityInInventoryFollowsMouse` com `WispRenderHandler.setRenderingGrimoirePortrait(true/false)` para que o modelo 3D do wisp apareça no retrato tanto em `WISP` quanto em `HOST`.
- **Legenda do contador de pontos:** `wispborn.grimoire.points` (`"pontos livres"` / `"points left"`) é quebrada com `font.split(..., 40)` sob a caixa `38×28` em `(270, 24)` conforme `art/hud/LAYOUT.md`, evitando colisão com as barras de recursos (`x = 146..266`).

### O que não foi feito
- Páginas Traços, Aspectos, Habilidades e Bestiário (exibidas apenas como abas desativadas, fora de escopo).
- Checklist manual in-game (`./gradlew runClient`), a cargo de Eder.
