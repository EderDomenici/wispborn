# F4 — HUD temático em sprites

**Objetivo:** enquanto o jogador é parasita (WISP ou HOST), o HUD vanilla dá lugar a um HUD próprio,
**100% sprites PNG em pixel art**: vida do wisp (núcleo), fome do wisp, mana com cor de mana, pontos de DNA
(EP) no lugar da barra de XP, painel do hospedeiro e painel do alvo. A action bar fica livre para mensagens.
**Estimativa:** 2–3 dias. **Branch:** `feat/F4-hud`. **Depende de:** F3 e da arte **A2**
(`docs/prompts/codex-art-prompts.md`) entregue e aprovada por Eder.
**Design:** `docs/superpowers/specs/2026-11-17-fundacao-design.md`.

## Regra de ouro
Nenhuma barra, ícone ou moldura desenhada com `fill`, gradiente ou forma geométrica. Tudo é
`GuiGraphics.blitSprite` de um PNG do mod ou texto com glifos da fonte do mod. A única exceção é o texto
(números e nomes), desenhado com `Font`.

## Fonte da arte
- A arte vem do prompt **A2** em `art/hud/`, com `LAYOUT.md` (posições e tamanhos em pixels de GUI,
  relativos ao centro inferior da tela) e `mockup.png` aprovado por Eder.
- Copie os PNGs para `src/main/resources/assets/wispborn/textures/gui/sprites/hud/` (atlas de GUI do
  1.21: referenciados como `wispborn:hud/<nome>`) e os glifos para `textures/font/`.
- **Siga o `LAYOUT.md`.** Se algo no layout for impossível ou ambíguo, registre em "Dúvidas do
  implementador" em vez de inventar posição.

## Contrato de sprites (`wispborn:hud/<nome>`)
| Sprite | Tamanho | Uso |
|---|---|---|
| `core_container`, `core_container_blinking` | 9×9 | Fundo de cada "coração" do wisp (piscando ao tomar dano) |
| `core_full`, `core_half` | 9×9 | Vida do wisp (2 de vida = 1 ícone) |
| `hunger_empty`, `hunger_half`, `hunger_full` | 9×9 | Fome do wisp (2 pontos = 1 ícone) |
| `mana_bar_background`, `mana_bar_progress` | 182×9 | Barra de mana (cor de mana); progresso recortado pela largura |
| `dna_bar_background`, `dna_bar_progress` | 182×7 | EP dentro do nível atual, no lugar da barra de XP |
| `dna_icon` | 9×9 | Ícone ao lado do número de nível |
| `host_frame` | 128×24 | Moldura do painel compacto do hospedeiro (topo central) |
| `host_health_background`, `host_health_progress` | 118×5 | Vida do hospedeiro |
| `host_floor_marker` | definido no LAYOUT | Marca de 20% na barra do hospedeiro |
| `consume_ready` | 9×9 | Ícone dentro do selo de consumo |
| `consume_badge` | 84×16 | Selo "Consumir" animado (16 quadros, 2 ticks), centro inferior, só no piso |
| `no_ep` | 9×9 | Indicador "presa fraca demais para dar EP" |
| `target_frame` | 150×26 | Fundo do painel do alvo (sem a barra de conjuração) |
| `target_ok`, `target_blocked` | 9×9 | Situação do alvo |
| `cast_bar_background`, `cast_bar_progress` | 132×4 | Barra de conjuração, centro inferior, só durante o cast |

As dimensões das barras correspondem a um frame. Os sprites de progresso usam 64 frames
verticais com `.png.mcmeta` (1 tick por frame, 20 fps, sem interpolação). Copiar PNG e metadata
juntos; o atlas anima a textura, enquanto a largura recortada continua seguindo o recurso.

### Fonte de ícones (`assets/wispborn/font/icons.json`)
Provider `bitmap` com glifos em PNG na Área de Uso Privado:
| Código | Glifo |
|---|---|
| `U+E000` | ♂ (macho) |
| `U+E001` | ♀ (fêmea) |
| `U+E002` | DNA pequeno (para texto e chat) |
| `U+E003` | Selo macho 9×9 (placa e painéis) |
| `U+E004` | Selo fêmea 9×9 (placa e painéis) |

Componentes usam `Style.withFont(ResourceLocation.fromNamespaceAndPath(MOD_ID, "icons"))` para esses
caracteres. A placa da F2 e o painel do alvo passam a usar os glifos.
Os PNGs de sexo já têm cor: fêmea rosa `#F48FB1`, macho azul `#64B5F6`.
O componente do glifo deve usar cor branca explícita (`0xFFFFFF`), sem herdar a cor
de infestabilidade do texto ao redor. DNA permanece branco para tintura pelo texto.

## Regras

### Estado no client
1. `SyncWispStatePayload` (play-to-client), enviado **só quando muda** (mesmo padrão de
   `HostManager.syncTargetInfo`), checado a cada 4 ticks e forçado no login, no respawn e na troca de dimensão:
   `level`, `totalEp`, `levelStartEp`, `nextLevelEp` (`-1` no nível máximo), `mana` (inteiro arredondado),
   `maxMana`, `availablePoints`, `hostConsumeReady` (bool), `hostEpFactor` (float; 1 fora de HOST).
2. `client/ClientWispState`: guarda o último payload. Vida e fome vêm do próprio jogador local; a vida do
   hospedeiro vem do `LivingEntity` montado (já sincronizado pelo vanilla); nível e sexo dele vêm dos
   traços da F2.

### Camadas vanilla escondidas (só enquanto parasita)
3. Em `RenderGuiLayerEvent.Pre`, cancelar: `HOTBAR`, `PLAYER_HEALTH`, `FOOD_LEVEL`, `ARMOR_LEVEL`,
   `EXPERIENCE_BAR`, `EXPERIENCE_LEVEL`, `SELECTED_ITEM_NAME`, `VEHICLE_HEALTH` e `JUMP_METER`
   (estes dois aparecem quando o wisp está montado no hospedeiro).
4. Mantidas: `CROSSHAIR`, `AIR_LEVEL`, `EFFECTS`, `BOSS_OVERLAY`, `CHAT`, `OVERLAY_MESSAGE`, `TITLE` etc.
5. Ao desenhar no lugar de vida e fome, atualizar `Gui.leftHeight`/`Gui.rightHeight` com a altura usada,
   para que o ar (`AIR_LEVEL`) e camadas de outros mods empilhem certo.

### Camadas do mod (`RegisterGuiLayersEvent`)
6. Registrar as camadas **acima** das vanilla correspondentes (`registerAbove`), cada uma num método
   próprio. Todas respeitam `mc.options.hideGui` (F1) e só desenham quando o jogador local é parasita.
   - `wisp_health`: núcleos, com piscar ao tomar dano (mesma lógica de tempo do vanilla).
   - `wisp_hunger`: fome, com o tremor vanilla quando a saturação é 0.
   - `wisp_mana`: barra de mana + número `42/70`.
   - `wisp_dna`: barra de DNA + `dna_icon` + número do nível no lugar do número verde do XP;
     `+N` pontos de atributo disponíveis, se houver, conforme o LAYOUT.
   - `host_panel` (só HOST): nome do mob + glifo do sexo + `Nv X`, barra de vida com `host_floor_marker`
     em `drain.floorPercent`, `consume_ready` com a tecla configurada (`KeyBindings.CONSUME.getTranslatedKeyMessage()`),
     `no_ep` quando `hostEpFactor == 0`.
   - `possess_target` (já existe): migrar para `target_frame`, `target_ok`/`target_blocked`, glifo do
     sexo e `cast_bar_*`.
7. A placa sobre os mobs (F2) passa a usar os glifos de sexo nas cores fixas acima.
   O nome/indicador usa a cor por situação, calculada no client com o
   nível do wisp: verde = infestável e rende EP; cinza = infestável sem EP (`EpMath.levelFactor == 0`);
   vermelho = forte demais. Os parâmetros vêm da config de servidor, que o NeoForge sincroniza com o client.

### Fim do HUD em texto
8. Remover o envio periódico de `ProgressManager.buildWispHud` (FormEvents) e do HUD do host
   (HostManager/F3). As mensagens pontuais (level up, falhas de infestação, consumo) continuam na action
   bar ou no chat, como hoje.

## Contratos
- `network/SyncWispStatePayload.java`; envio em `progress/` ou `event/` (servidor).
- `client/ClientWispState.java`, `client/hud/WispHud.java` (registro + um método por camada),
  `client/hud/HudSprites.java` (constantes `ResourceLocation` dos sprites).
- Pura e testável: `client/hud/HudMath.java` (sem classes do Minecraft):
  - `static int progressWidth(float value, float max, int fullWidth)` → largura em pixels, com clamp
  - `static int[] iconStates(float value, int icons)` → `0 vazio / 1 metade / 2 cheio` por ícone
  - `static int floorMarkerX(double floorPercent, int barWidth)`

## Testes (JUnit)
- `progressWidth(42, 70, 182)` = 109; `(0, 70, 182)` = 0; `(80, 70, 182)` = 182; `max 0` → 0.
- `iconStates(5, 3)` = `[2, 2, 1]`; `(0, 3)` = `[0, 0, 0]`; `(6, 3)` = `[2, 2, 2]`.
- `floorMarkerX(0.2, 100)` = 20.
- Round-trip de `SyncWispStatePayload`.

## Checklist manual
- [ ] Wisp: não há hotbar, corações, coxinhas, armadura nem barra/número de XP vanilla. Aparecem núcleos,
      fome, mana e DNA nas posições do `LAYOUT.md`. Comparar com `art/hud/mockup.png`.
- [ ] Tomar dano: núcleos piscam. Saturação 0: ícones de fome tremem.
- [ ] Debaixo d'água: as bolhas de ar aparecem acima da fome, sem sobrepor.
- [ ] Mana gasta ao infestar: a barra cai na hora. EP sobe: a barra de DNA enche; no level up, o número troca e a barra volta.
- [ ] HOST: painel do hospedeiro com nome, sexo, nível, vida, marca de 20%; no piso aparece `consume_ready`
      com a tecla configurada (trocar a tecla nos Controles e conferir). Presa fraca: `no_ep`.
- [ ] Nenhum coração de montaria vanilla nem barra de pulo aparece em HOST.
- [ ] Action bar livre: nenhum texto periódico; só mensagens pontuais.
- [ ] F1 esconde tudo. GUI scale 1, 2, 3 e auto: nada desalinhado.
- [ ] Placas: verdes, cinzas e vermelhas corretas para um wisp Nv 7 perto de galinha, zumbi e enderman.
- [ ] Multiplayer (`runClient2`): cada jogador vê o próprio HUD.
- [ ] Servidor dedicado sobe sem crash (nada de `client/` no código comum).

## Fora de escopo
Menu RPG (nível, fenótipos, árvore de habilidades), tela de atributos, feedback de conjuração em primeira
pessoa (V1).

## Dúvidas do implementador
Nenhuma dúvida.

## Relatório de implementação

### Integração da arte aprovada — 19/11/2026

- Eder aprovou todos os PNGs do pacote A2.
- Copiados 24 sprites para `assets/wispborn/textures/gui/sprites/hud/`, incluindo
  a vinheta V1 e as barras animadas de mana/DNA.
- Copiados os dois `.png.mcmeta` das barras: 64 frames, 1 tick por frame.
- Copiados três glifos para `assets/wispborn/textures/font/` e o provider para
  `assets/wispborn/font/icons.json`. Fêmea rosa, macho azul; usar texto branco
  explícito para preservar essas cores.
- Mockups, variantes e prévias permanecem em `art/hud/`; não são recursos de runtime.
- Esta entrega integra os recursos aprovados. Código do HUD, sincronização de estado,
  camadas de GUI e testes in-game da F4 ainda pendentes.
- Verificação: `./gradlew build` aprovado em 2 s; os 30 arquivos de runtime
  correspondem byte a byte às fontes e ao JAR; referências da fonte resolvidas.

### Implementação do HUD e sincronização — 20/11/2026

- **Arquivos criados/alterados:**
  - `src/main/java/dev/eder/wispborn/network/SyncWispStatePayload.java` e `ModPayloads.java`: payload S2C (`level`, `totalEp`, `levelStartEp`, `nextLevelEp`, `mana`, `maxMana`, `availablePoints`, `hostConsumeReady`, `hostEpFactor`).
  - `src/main/java/dev/eder/wispborn/progress/{LevelCurve,ProgressManager}.java`, `event/FormEvents.java` e `host/HostManager.java`: adicionado `LevelCurve.levelStartEp`, delta-sync de `SyncWispStatePayload` checado a cada 4 ticks (e em mudanças de EP, mana, atributos e forma) e forçado em login/respawn/troca de dimensão; removido o envio periódico do HUD em texto (`buildWispHud` e `buildHostHud`) da action bar.
  - `src/main/java/dev/eder/wispborn/client/{ClientWispState,ClientPayloadHandler}.java`: armazenamento do último `SyncWispStatePayload` no client, limpo no logout.
  - `src/main/java/dev/eder/wispborn/client/hud/{HudSprites,HudMath,WispHud}.java` e `client/WispRenderHandler.java`:
    - `HudSprites`: constantes `ResourceLocation` dos sprites `wispborn:hud/*` e da fonte `wispborn:icons`.
    - `HudMath`: funções puras `progressWidth`, `iconStates` e `floorMarkerX`.
    - `WispHud`: cancela `HOTBAR`, `PLAYER_HEALTH`, `FOOD_LEVEL`, `ARMOR_LEVEL`, `EXPERIENCE_BAR`, `EXPERIENCE_LEVEL`, `SELECTED_ITEM_NAME`, `VEHICLE_HEALTH` e `JUMP_METER` em `RenderGuiLayerEvent.Pre` enquanto parasita; registra `wisp_health`, `wisp_hunger`, `wisp_mana`, `wisp_dna`, `host_panel` e `possess_target` com `registerAbove`, seguindo todas as coordenadas de `art/hud/LAYOUT.md`, recorte sem esticar via `blitSprite(sprite, fullW, fullH, 0, 0, x, y, cropW, fullH)`, piscar de vida e tremor de fome vanilla, e atualização de `Gui.leftHeight` / `Gui.rightHeight`.
  - `src/main/java/dev/eder/wispborn/traits/Sex.java` e `client/MobNameplateRenderer.java`: `Sex.glyphComponent()` com fonte `wispborn:icons` e cor explícita `0xFFFFFF` (preservando rosa `#F48FB1` / azul `#64B5F6`); placa sobre os mobs colorida por situação (verde `0x55FF55`, cinza `0xAAAAAA` quando `EpMath.levelFactor == 0`, vermelho `0xFF5555` quando forte demais).
  - `src/test/java/dev/eder/wispborn/client/hud/HudMathTest.java` e `network/PossessionPayloadsTest.java`: testes JUnit 5 de `HudMath` e round-trip de `SyncWispStatePayload`.
- **Saída final de `./gradlew build`:**
  ```text
  > Task :jar
  > Task :assemble
  > Task :build
  BUILD SUCCESSFUL in 6s
  ```
- **Saída final de `./gradlew test`:**
  ```text
  > Task :compileJava
  > Task :classes
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  BUILD SUCCESSFUL in 21s
  ```
- **Saída final de `./gradlew runServer`:**
  ```text
  [11:50:50] [Server thread/INFO] [minecraft/DedicatedServer]: Done (1.023s)! For help, type "help"
  [11:50:50] [Server thread/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```
- **Pendências:**
  - Validação do checklist manual in-game por Eder (`runClient` / `runClient2`).
