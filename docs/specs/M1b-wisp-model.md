# M1b — Modelo e animações do wisp (GeckoLib)

**Objetivo:** o wisp passa a ser renderizado com o modelo do Blockbench (`art/parasita-magico/`),
animado com GeckoLib (`idle` parado, `walking` em movimento, `attack` pronta para a possessão do M3),
e fica 20% maior que no M1.
**Estimativa:** 1–2 dias. **Branch:** `feat/M1b-wisp-model`. **Depende de:** M1 (mergeado na `main`).

Antes de começar, leia `.agents/rules/geckolib-4.9.md`: as APIs do GeckoLib mudam muito entre
versões, e só vale o que está lá. Organização de arquivos de arte: `docs/ART_PIPELINE.md`.

## Tarefas

### 0. Prova de conceito (faça primeiro)
Renderizar o modelo no lugar do jogador com `GeoReplacedEntityRenderer` chamado de dentro do
`RenderPlayerEvent.Pre` é o ponto de maior risco. Faça só isso primeiro (modelo + `idle`, sem glowmask
e sem trigger). Se não funcionar (crash, modelo invisível, posição/rotação errada que você não
consiga corrigir), **pare** e descreva o problema em "Dúvidas do implementador", com log/print.

### 1. Dependência
Conforme `.agents/rules/geckolib-4.9.md` → "Build":
- `gradle.properties`: `geckolib_version=4.9.3`
- `build.gradle`: repositório cloudsmith + `implementation "software.bernie.geckolib:geckolib-neoforge-1.21.1:${geckolib_version}"`;
  `geckolib_version` no `replaceProperties` do `generateModMetadata`.
- `neoforge.mods.toml`: dependência obrigatória `geckolib` `[${geckolib_version},)`.

### 2. Tamanho
`WispTuning.SCALE_MODIFIER`: `-0.6` → `-0.52` (escala 0,48; ~0,86 bloco de altura; ainda passa em vão de 1 bloco).

### 3. Assets
Copiar de `art/parasita-magico/` (sem editar o conteúdo):

| Origem | Destino |
|---|---|
| `wisp.geo.json` | `src/main/resources/assets/wispborn/geo/entity/wisp.geo.json` |
| `wisp.animation.json` | `src/main/resources/assets/wispborn/animations/entity/wisp.animation.json` |
| `wisp.png` | `src/main/resources/assets/wispborn/textures/entity/wisp.png` |
| `wisp_glowmask.png` | `src/main/resources/assets/wispborn/textures/entity/wisp_glowmask.png` |

Remover `textures/entity/wisp_core.png` e `textures/entity/wisp_halo.png` (não são mais usadas).
Versionar a pasta `art/parasita-magico/` inteira (fonte da arte; os PNGs `idle/walking/attack.png` são só referência).

### 4. `form/WispAnimations.java` (comum, sem dependência de GeckoLib)
Constantes `String` com os nomes, para o código e o teste usarem a mesma fonte:
- `IDLE = "animation.wisp.idle"`, `WALK = "animation.wisp.walking"`, `POSSESS = "animation.wisp.attack"`
- `CONTROLLER_MOVEMENT = "movement"`, `CONTROLLER_POSSESS = "possess"`, `TRIGGER_POSSESS = "possess"`

### 5. `form/WispAnimatable.java` (comum)
Singleton `GeoReplacedEntity` conforme o exemplo das regras:
- `getReplacingEntityType()` → `EntityType.PLAYER`; `getTick(entity)` → `((Entity) entity).tickCount`.
- Controller `movement` (transição 5 ticks): `state.isMoving()` → loop `WALK`, senão loop `IDLE`.
- Controller `possess` (transição 0): handler devolve `PlayState.STOP`; `triggerableAnim(TRIGGER_POSSESS, RawAnimation.begin().thenPlay(POSSESS))`.
- `RawAnimation` em constantes `static final`.
- No construtor do `WispbornMod`: `SingletonGeoAnimatable.registerSyncedAnimatable(WispAnimatable.INSTANCE)`.
- Método de conveniência para o servidor: `static void playPossess(ServerPlayer p)` →
  `INSTANCE.triggerAnim(p, CONTROLLER_POSSESS, TRIGGER_POSSESS)`. **Não é chamado no M1b** além do comando de debug; o M3 usa.

### 6. Client
- `client/WispModel.java`: `DefaultedEntityGeoModel<WispAnimatable>` com id `wispborn:wisp`.
- `client/WispPlayerRenderer.java`: `GeoReplacedEntityRenderer<AbstractClientPlayer, WispAnimatable>`
  com `WispModel`, `WispAnimatable.INSTANCE` e `AutoGlowingGeoLayer`. Sem `withScale` (a escala vem do atributo `SCALE`).
- Criar o renderer em `EntityRenderersEvent.AddLayers` (mod bus, `Dist.CLIENT`) via `event.getContext()`;
  guardar numa referência estática no `WispRenderHandler`.
- `WispRenderHandler.onRenderPlayerPre`, forma `WISP`: cancelar e chamar `renderer.render(player, yaw, partialTick, poseStack, bufferSource, packedLight)`
  com os dados do evento (`yaw` = `Mth.lerp(partialTick, player.yRotO, player.getYRot())`).
  **Remover** os quads billboard, as constantes deles e `drawBillboardQuad`/`addQuadVertex`.
  Se o renderer ainda for `null` (antes do `AddLayers`), só cancela.
- Também não renderizar se `player.isInvisible()` (só cancela). Forma `HOST` e `RenderHandEvent`: como no M1.
- Partículas `END_ROD` do `ClientTickEvent.Post`: manter como no M1.

### 7. Comando de debug
`/wispborn anim possess [player]` (permissão 2): chama `WispAnimatable.playPossess(target)`.
Feedback via lang `commands.wispborn.anim.success` (en_us e pt_br).

## Testes
JUnit `src/test/java/dev/eder/wispborn/form/WispAnimationsTest.java`:
- lê `/assets/wispborn/animations/entity/wisp.animation.json` do classpath (Gson) e verifica que
  `IDLE`, `WALK` e `POSSESS` existem em `animations`;
- `IDLE` e `WALK` têm `"loop": true`; `POSSESS` não tem loop.

O teste usa só `WispAnimations` (sem carregar classes do GeckoLib).

## Checklist manual (Eder)
- [ ] O jogo abre com o GeckoLib carregado (aparece na lista de mods); sem GeckoLib, o jogo avisa a dependência faltando.
- [ ] `/wispborn form wisp` + F5: aparece o modelo do Blockbench (núcleo, raios, satélites) com a animação `idle`.
- [ ] Andando: troca suave para `walking`; parando: volta para `idle`.
- [ ] O modelo fica de frente para a direção do movimento (girar a câmera e andar para os lados/para trás).
- [ ] Translucidez: raios aparecem corretos na frente de água, vidro e partículas.
- [ ] Leitura de perto e de longe, em primeira (sem mão) e terceira pessoa.
- [ ] Partes da glowmask brilham no escuro (testar à noite/caverna).
- [ ] O wisp está ~20% maior que no M1 e ainda passa em vão de 1 bloco.
- [ ] `/wispborn anim possess`: toca a animação `attack` uma vez e volta ao `idle`/`walking`.
- [ ] Multiplayer (`runServer` + `runClient` + `runClient2`): Player2 vê o modelo, as animações e o `anim possess` do Dev.
- [ ] `/wispborn form human`: volta ao jogador normal, sem resto do modelo.
- [ ] Servidor dedicado sobe sem crash (`runServer`) — nenhuma classe de `client/` carregada.

## Fora de escopo (não fazer)
Voo, possessão (o M3 dispara a `attack`), modelo para outras criaturas, editar a arte, mudar as partículas.

## Dúvidas do implementador
Nenhuma dúvida.

## Relatório de implementação
- **Arquivos criados/alterados:**
  - `gradle.properties`: adicionado `geckolib_version=4.9.3`.
  - `build.gradle`: adicionado repositório Cloudsmith do GeckoLib, dependência `software.bernie.geckolib:geckolib-neoforge-1.21.1:${geckolib_version}` e `geckolib_version` em `generateModMetadata`.
  - `src/main/templates/META-INF/neoforge.mods.toml`: adicionada dependência obrigatória `geckolib` (`[${geckolib_version},)`, `side="BOTH"`).
  - `src/main/java/dev/eder/wispborn/form/WispTuning.java`: `SCALE_MODIFIER` atualizado de `-0.6` para `-0.52`.
  - `src/main/java/dev/eder/wispborn/form/WispAnimations.java`: constantes de nomes de clipes (`IDLE`, `WALK`, `POSSESS`) e controllers/triggers (`CONTROLLER_MOVEMENT`, `CONTROLLER_POSSESS`, `TRIGGER_POSSESS`).
  - `src/main/java/dev/eder/wispborn/form/WispAnimatable.java`: singleton `GeoReplacedEntity` substituindo `EntityType.PLAYER` com controllers `movement` (transição 5 ticks) e `possess` (transição 0, triggerable `possess`), além do helper `playPossess(ServerPlayer)`.
  - `src/main/java/dev/eder/wispborn/WispbornMod.java`: registrado `SingletonGeoAnimatable.registerSyncedAnimatable(WispAnimatable.INSTANCE)`.
  - `src/main/java/dev/eder/wispborn/client/WispModel.java`: `DefaultedEntityGeoModel<WispAnimatable>` (`wispborn:wisp`) sobrescrevendo `getRenderType` com `RenderType.entityTranslucent(texture)`.
  - `src/main/java/dev/eder/wispborn/client/WispPlayerRenderer.java`: `GeoReplacedEntityRenderer<AbstractClientPlayer, WispAnimatable>` com `AutoGlowingGeoLayer`.
  - `src/main/java/dev/eder/wispborn/client/WispRenderHandler.java`: substituídos os quads billboard do M1 pelo `WispPlayerRenderer` instanciado em `EntityRenderersEvent.AddLayers`, mantendo ocultação para `HOST`/`isInvisible()`, ocultação de mão e partículas `END_ROD`.
  - `src/main/java/dev/eder/wispborn/command/WispbornCommand.java`: subcomando `/wispborn anim possess [player]` (permissão 2).
  - `src/main/resources/assets/wispborn/`: copiados `geo/entity/wisp.geo.json`, `animations/entity/wisp.animation.json`, `textures/entity/wisp.png` e `textures/entity/wisp_glowmask.png` de `art/parasita-magico/`; removidos `wisp_core.png` e `wisp_halo.png`; adicionada chave `commands.wispborn.anim.success` em `lang/en_us.json` e `pt_br.json`.
  - `src/test/java/dev/eder/wispborn/form/WispAnimationsTest.java`: teste JUnit validando a existência e configuração de `loop` de `IDLE`, `WALK` e `POSSESS` em `wisp.animation.json`.
- **Decisões tomadas:**
  - `WispModel.getRenderType` retorna `RenderType.entityTranslucent(texture)` para renderizar corretamente os feixes translúcidos da textura `wisp.png` sobre água, vidro e partículas (item 5 do checklist manual).
- **Saída final de `./gradlew build`:**
  ```text
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  > Task :check
  > Task :build
  BUILD SUCCESSFUL in 44s
  ```
- **Saída final de `./gradlew test`:**
  ```text
  > Task :compileJava
  > Task :classes
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  BUILD SUCCESSFUL in 16s
  ```
- **Saída final de `./gradlew runServer`:**
  ```text
  [22:15:00] [Server thread/INFO] [minecraft/DedicatedServer]: Done (3.136s)! For help, type "help"
  [22:15:00] [Server thread/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```
- **Pendências:**
  - Validação visual do checklist manual in-game por Eder (`runClient` / `runClient2`).
