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
_(preencher se houver)_

## Relatório de implementação
_(preencher ao terminar)_
