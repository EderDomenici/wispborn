# M1 — Forma Wisp

**Objetivo:** o jogador pode virar wisp e voltar a humano; o estado persiste (relog, morte,
dimensão), aparece corretamente para ele e para outros jogadores, e o wisp tem as restrições certas.
**Estimativa:** 2–3 dias. **Branch:** `feat/M1-wisp-form`. **Depende de:** M0.

> Mudança em relação ao plano: o tamanho/hitbox do wisp usa o atributo vanilla `Attributes.SCALE`
> em vez do `EntityEvent.Size`. Isso reduz a hitbox e o modelo juntos, sem evento e sem mixin.

## Contratos

### `form/FormType.java`
```java
public enum FormType implements StringRepresentable { HUMAN, WISP, HOST; ... }
// CODEC via StringRepresentable.fromEnum; STREAM_CODEC via ByteBufCodecs.idMapper ou VAR_INT+ordinal
```

### `form/FormState.java`
```java
public record FormState(FormType type, Optional<ResourceLocation> hostEntity) {
    public static final FormState HUMAN = new FormState(FormType.HUMAN, Optional.empty());
    public static final FormState WISP  = new FormState(FormType.WISP,  Optional.empty());
    public static FormState host(ResourceLocation entityTypeId) { ... }
    public static final Codec<FormState> CODEC = RecordCodecBuilder.create(...);       // persistência
    public static final StreamCodec<ByteBuf, FormState> STREAM_CODEC = ...;           // rede
}
```
`HOST` e `hostEntity` (id do tipo do mob hospedeiro, para HUD e render) já existem para o M3, mas **não são usados no M1**.
A vida do hospedeiro **não** fica aqui: é a vida do mob real.

### `registry/ModAttachments.java`
`FORM`: `AttachmentType<FormState>`, default `FormState.HUMAN`, `.serialize(FormState.CODEC)`, `.copyOnDeath()`.

### `form/FormManager.java` (servidor)
- `static void setForm(ServerPlayer p, FormState s)` — grava o attachment, chama `applyModifiers`, chama `sync`.
- `static void applyModifiers(ServerPlayer p)` — remove todos os modificadores `wispborn:*` desta feature e aplica os do estado atual. Idempotente.
- `static void sync(ServerPlayer p)` — `PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new SyncFormPayload(p.getId(), state))`.
- `static boolean isWisp(Player p)` — forma == WISP. Usável em client e servidor.
- `static boolean isParasite(Player p)` — forma == WISP ou HOST. As restrições usam este.

### Modificadores do wisp (ids `wispborn:wisp_*`)
| Atributo | Operação | Valor |
|---|---|---|
| `SCALE` | `ADD_MULTIPLIED_TOTAL` | `-0.6` |
| `MAX_HEALTH` | `ADD_VALUE` | `-14` (20 → 6 = 3 corações; `ADD_VALUE` para a Vitalidade do M2 somar 1 coração por ponto) |
| `GRAVITY` | `ADD_MULTIPLIED_TOTAL` | `-0.75` (flutua) |
| `SAFE_FALL_DISTANCE` | `ADD_VALUE` | `+20` |
| `MOVEMENT_SPEED` | `ADD_MULTIPLIED_TOTAL` | `+0.2` |

Ponha os valores numa classe `form/WispTuning.java` (constantes), para balancear depois.

### Rede: `network/SyncFormPayload` (server → client)
`(int entityId, FormState state)`. No client: busca a entidade em `Minecraft.getInstance().level`
e faz `entity.setData(ModAttachments.FORM, state)`. Se a entidade não existir ainda, ignore
(o `StartTracking` reenvia).

### Quando sincronizar (game bus, `event/FormEvents.java`)
- `PlayerLoggedInEvent` → `applyModifiers` + `sync`
- `PlayerRespawnEvent` → `applyModifiers` + `sync` (attachment já foi copiado pelo `copyOnDeath`)
- `PlayerChangedDimensionEvent` → `sync`
- `StartTracking` com alvo `ServerPlayer` → `PacketDistributor.sendToPlayer(tracker, payload do alvo)`

### Restrições do wisp (servidor, `event/WispRestrictions.java`)
Se `isParasite(player)`:
- `AttackEntityEvent` → cancelar
- `BlockEvent.BreakEvent` → cancelar
- `PlayerInteractEvent.RightClickItem` e `RightClickBlock` → cancelar,
  **exceto** usar o item `wisp_essence` (para poder voltar a humano)
- `PlayerInteractEvent.EntityInteract` → cancelar (no M3 vira a infestação)

### Item `wisp_essence`
- Item simples, stack 16. Usar: alterna HUMAN ↔ WISP via `FormManager.setForm`, consome 1
  (em criativo não consome), toca um som (`SoundEvents.ALLAY_ITEM_TAKEN` serve) e solta partículas.
- Textura placeholder 16×16 em `assets/wispborn/textures/item/wisp_essence.png`
  (pode ser gerada por script; qualquer orbe ciano serve).
- Aparece na aba criativa "Ingredients".

### Comando de debug `command/WispbornCommand.java`
`/wispborn form <human|wisp> [player]` — nível de permissão 2. Usa `FormManager.setForm`.

### Client (`client/`)
Referência visual: orbe de energia flutuante no espírito do wisp de Warcraft 3 (núcleo brilhante,
halo suave, flutuação, rastro). Arte própria — não copiar modelo nem textura de outro jogo.
- `WispRenderHandler` (`RenderPlayerEvent.Pre`):
  - forma WISP → **cancela** o render padrão e desenha 2 quads billboard emissivos
    (`RenderType.entityTranslucentEmissive`): núcleo `textures/entity/wisp_core.png` (raio ~0.15) e
    halo `textures/entity/wisp_halo.png` (raio ~0.35, alpha ~0.5). Flutuação vertical
    `sin(tempo) × 0.08` bloco e pulso de escala ±8%.
  - forma HOST → só cancela (o wisp está escondido dentro do mob).
- `ClientTickEvent.Post`: para cada wisp visível, a cada 2 ticks, 1 partícula `ParticleTypes.END_ROD`
  levemente atrás do movimento (rastro); parado, partículas sobem devagar ao redor.
- `RenderHandEvent`: se o jogador local é parasita (WISP ou HOST), cancela (sem mão em primeira pessoa).
- Nada em `client/` pode ser carregado no servidor dedicado.

### Lang (en_us e pt_br)
`item.wispborn.wisp_essence`, mensagens do comando (`commands.wispborn.form.success`).

## Testes
JUnit (`src/test/java/dev/eder/wispborn/form/FormStateTest.java`):
- round-trip de `FormState.CODEC` com `JsonOps.INSTANCE` para HUMAN, WISP e `FormState.host(minecraft:cow)`;
- `FormType` serializa como `"human"`, `"wisp"`, `"host"`.

## Checklist manual (Eder)
- [ ] `/wispborn form wisp`: jogador fica pequeno, com ~3 corações, flutua ao pular e cai devagar.
- [ ] Wisp passa por vão de 1 bloco de altura.
- [ ] Wisp não quebra bloco, não ataca, não come, não abre baú.
- [ ] Usar `wisp_essence` como wisp volta a humano com vida e tamanho normais.
- [ ] Sair e entrar no mundo como wisp: continua wisp, com os atributos certos.
- [ ] Morrer como wisp: renasce wisp, atributos certos (não acumula modificador).
- [ ] Ir ao Nether como wisp: continua renderizando como wisp.
- [ ] Servidor dedicado (`runServer`, `online-mode=false` em `run/server.properties`) +
      `runClient` e `runClient2` conectando em `localhost`: Dev vira wisp, Player2 vê Dev como wisp;
      Player2 entrando depois que Dev já é wisp também vê.
- [ ] Primeira pessoa sem mão quando wisp; F5 mostra o orbe flutuando e pulsando, com rastro ao andar.

## Fora de escopo (não fazer)
Infestação, host, EP, níveis, HUD, keybinds, voo real (`mayfly`).

## Dúvidas do implementador
Nenhuma dúvida.

## Relatório de implementação
- **Arquivos criados/alterados:**
  - `src/main/java/dev/eder/wispborn/form/FormType.java`: enum `HUMAN`, `WISP`, `HOST` com `StringRepresentable`, `CODEC` e `STREAM_CODEC`.
  - `src/main/java/dev/eder/wispborn/form/FormState.java`: record `(FormType type, Optional<ResourceLocation> hostEntity)` com `HUMAN`, `WISP`, `host(id)`, `CODEC` e `STREAM_CODEC`.
  - `src/main/java/dev/eder/wispborn/form/WispTuning.java`: IDs `wispborn:wisp_*` e valores dos 5 modificadores (`SCALE`, `MAX_HEALTH`, `GRAVITY`, `SAFE_FALL_DISTANCE`, `MOVEMENT_SPEED`).
  - `src/main/java/dev/eder/wispborn/form/FormManager.java`: `setForm`, `applyModifiers` (idempotente via `removeModifier` + `addOrReplacePermanentModifier`), `sync`, `isWisp` e `isParasite`.
  - `src/main/java/dev/eder/wispborn/form/WispEssenceItem.java`: item com stack 16 que alterna `HUMAN ↔ WISP`, toca `SoundEvents.ALLAY_ITEM_TAKEN`, gera partículas `END_ROD` e consome 1 unidade fora do criativo.
  - `src/main/java/dev/eder/wispborn/registry/ModAttachments.java`: attachment `FORM` (`FormState.HUMAN`, `.serialize(FormState.CODEC)`, `.copyOnDeath()`).
  - `src/main/java/dev/eder/wispborn/registry/ModItems.java`: registro do item `wisp_essence` e inclusão na aba criativa `CreativeModeTabs.INGREDIENTS`.
  - `src/main/java/dev/eder/wispborn/network/SyncFormPayload.java` e `ModPayloads.java`: pacote `wispborn:sync_form` (`S2C`) isolando `ClientPayloadHandler` via lambda para não carregar classes `Dist.CLIENT` no servidor dedicado.
  - `src/main/java/dev/eder/wispborn/event/FormEvents.java`: sincronização e reaplicação de modificadores em login, respawn, troca de dimensão e `StartTracking`.
  - `src/main/java/dev/eder/wispborn/event/WispRestrictions.java`: cancelamento de ataque, quebra de bloco, interação com entidade e clique direito em bloco/item (permitindo uso de `wisp_essence` sem acionar blocos como baús via `TriState.FALSE`).
  - `src/main/java/dev/eder/wispborn/command/WispbornCommand.java`: comando `/wispborn form <human|wisp> [player]` (permissão 2).
  - `src/main/java/dev/eder/wispborn/client/ClientPayloadHandler.java` e `WispRenderHandler.java`: handler `S2C` e renderização em `Dist.CLIENT` (2 quads billboard emissivos `wisp_core` + `wisp_halo` com flutuação senoidal e pulso ±8%, partículas `END_ROD` a cada 2 ticks e ocultação da mão em 1ª pessoa).
  - `src/main/resources/assets/wispborn/`: modelo `models/item/wisp_essence.json`, texturas PNG geradas (`textures/item/wisp_essence.png` 16×16, `textures/entity/wisp_core.png` 32×32, `textures/entity/wisp_halo.png` 32×32) e traduções em `lang/en_us.json` e `pt_br.json`.
  - `src/test/java/dev/eder/wispborn/form/FormStateTest.java` e `build.gradle`: testes JUnit de serialização de `FormType` e round-trip de `FormState.CODEC` (`HUMAN`, `WISP`, `HOST` com `minecraft:cow`), expondo `compileClasspath` ao `testCompileClasspath`.
- **Decisões tomadas:**
  - Ao alternar formas em `FormManager.setForm`, a proporção `health / maxHealth` é preservada (evitando cura grátis ao alternar humano ↔ wisp e restaurando 10 corações quando o wisp estava com 100% de vida).
  - Em `WispRestrictions.onRightClickBlock`, quando o parasita segura `wisp_essence`, `event.setUseBlock(TriState.FALSE)` bloqueia a abertura de containers/blocos mas permite que o uso do item prossiga.
- **Saída final de `./gradlew build`:**
  ```text
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  > Task :check
  > Task :build
  BUILD SUCCESSFUL in 15s
  ```
- **Saída final de `./gradlew test`:**
  ```text
  > Task :compileJava
  > Task :classes
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  BUILD SUCCESSFUL in 13s
  ```
- **Saída final de `./gradlew runServer`:**
  ```text
  [16nov.2026 21:29:50.697] [modloading-worker-0/INFO] [dev.eder.wispborn.WispbornMod/]: Inicializando Wispborn (wispborn)
  [16nov.2026 21:29:55.870] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (3.096s)! For help, type "help"
  [16nov.2026 21:29:55.898] [Server thread/INFO] [net.neoforged.neoforge.gametest.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```
- **Pendências:**
  - Execução do checklist manual in-game por Eder (`runClient` / `runClient2`).
