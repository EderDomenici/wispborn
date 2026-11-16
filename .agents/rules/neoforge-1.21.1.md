---
trigger: always_on
---

# Regras de API — NeoForge 1.21.1

Modelos frequentemente misturam APIs de Forge 1.20 / NeoForge antigo / Fabric. Siga esta lista.

## PROIBIDO (não existe ou é legado em NeoForge 1.21.1)
- `ICapabilityProvider`, `LazyOptional`, `AttachCapabilitiesEvent`, `CapabilityManager`
- `FMLJavaModLoadingContext.get()` → use o `IEventBus modEventBus` do construtor do `@Mod`
- `SimpleChannel`, `NetworkRegistry`, `NetworkEvent`, `PacketDistributor.PLAYER.with(...)`
- `new ResourceLocation(ns, path)` → `ResourceLocation.fromNamespaceAndPath(ns, path)`
- `CompoundTag` cru para guardar estado do jogador (`getPersistentData()`) → Data Attachments
- `ForgeRegistries`, `RegistryObject` → `DeferredRegister`, `DeferredHolder`, `DeferredItem`
- `AttributeModifier` com `UUID` → em 1.21 o id é `ResourceLocation`
- `@Mod.EventBusSubscriber` com `bus = Bus.MOD` ainda funciona, mas prefira registrar no construtor
- Qualquer API Fabric (`net.fabricmc.*`) ou Mixin sem spec pedindo

## USE

### Registro
```java
public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
public static final DeferredItem<Item> WISP_ESSENCE = ITEMS.registerSimpleItem("wisp_essence");
// no construtor do mod: ITEMS.register(modEventBus);
```

### Data Attachments (estado do jogador)
```java
public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
    DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);

public static final Supplier<AttachmentType<FormState>> FORM = ATTACHMENT_TYPES.register("form",
    () -> AttachmentType.builder(() -> FormState.HUMAN).serialize(FormState.CODEC).copyOnDeath().build());

FormState s = player.getData(ModAttachments.FORM);
player.setData(ModAttachments.FORM, novo);
```
- **Não há sync automático em 1.21.1.** Mandar payload manualmente (ver Rede).
- `copyOnDeath` copia o attachment no respawn; **modificadores de atributo não são copiados** —
  reaplique em `PlayerEvent.PlayerRespawnEvent`.

### Rede
```java
// mod bus
@SubscribeEvent
public static void register(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar("1");
    registrar.playToClient(SyncFormPayload.TYPE, SyncFormPayload.STREAM_CODEC, ClientPayloadHandler::handleSyncForm);
    registrar.playToServer(ConsumeRequestPayload.TYPE, ConsumeRequestPayload.STREAM_CODEC, ServerPayloadHandler::handleConsume);
}

public record SyncFormPayload(int entityId, FormState state) implements CustomPacketPayload {
    public static final Type<SyncFormPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sync_form"));
    public static final StreamCodec<ByteBuf, SyncFormPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, SyncFormPayload::entityId,
        FormState.STREAM_CODEC, SyncFormPayload::state,
        SyncFormPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```
- Handlers recebem `(payload, IPayloadContext ctx)` e rodam na main thread por padrão.
- Envio: `PacketDistributor.sendToServer(p)`, `sendToPlayer(serverPlayer, p)`,
  `sendToPlayersTrackingEntityAndSelf(entity, p)`, `sendToAllPlayers(p)`.
- Handler de client fica em `client/` e só é referenciado via registrar (não importar em código comum
  fora do registro). Se o compilador/servidor dedicado reclamar, isole com `FMLEnvironment.dist`.
- **Nunca confie no client**: o servidor revalida distância, tier, cooldown.

### Atributos (1.21)
```java
AttributeInstance inst = player.getAttribute(Attributes.SCALE);
inst.addOrReplacePermanentModifier(new AttributeModifier(
    ResourceLocation.fromNamespaceAndPath(MOD_ID, "wisp_scale"), -0.6,
    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
inst.removeModifier(ResourceLocation.fromNamespaceAndPath(MOD_ID, "wisp_scale"));
```
- Operações: `ADD_VALUE`, `ADD_MULTIPLIED_BASE`, `ADD_MULTIPLIED_TOTAL`.
- Atributos úteis já existentes em 1.21: `SCALE` (tamanho + hitbox), `GRAVITY`, `MAX_HEALTH`,
  `MOVEMENT_SPEED`, `ATTACK_DAMAGE`, `JUMP_STRENGTH`, `SAFE_FALL_DISTANCE`, `STEP_HEIGHT`.
- Depois de reduzir `MAX_HEALTH`, faça `player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()))`.

### Eventos
- Mod bus (`modEventBus.addListener` / `@EventBusSubscriber(bus = MOD)`): registro, payloads,
  `RegisterKeyMappingsEvent`, `EntityRenderersEvent`, `DataPackRegistryEvent.NewRegistry`.
- Game bus (`NeoForge.EVENT_BUS`): `PlayerEvent.PlayerLoggedInEvent`, `PlayerEvent.PlayerRespawnEvent`,
  `PlayerEvent.PlayerChangedDimensionEvent`, `PlayerEvent.StartTracking`, `AttackEntityEvent`,
  `PlayerInteractEvent.*`, `BlockEvent.BreakEvent`, `RegisterCommandsEvent`, `PlayerTickEvent.Post`.
  Do host: `EntityMountEvent` (`isMounting()`/`isDismounting()`), `LivingDeathEvent`, `LivingIncomingDamageEvent`,
  `LivingDropsEvent`, `LivingExperienceDropEvent`, `PlayerEvent.PlayerLoggedOutEvent`.
- Client game bus: `RenderPlayerEvent.Pre`, `RenderHandEvent`, `ClientTickEvent.Post`.

### Config
```java
public static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
public static final ModConfigSpec.IntValue DRAIN_INTERVAL = B.defineInRange("drain.intervalTicks", 100, 1, 72000);
public static final ModConfigSpec SPEC = B.build();
// construtor do @Mod: modContainer.registerConfig(ModConfig.Type.SERVER, SPEC);
```
Config SERVER só existe com mundo carregado: não ler em static init nem em teste JUnit (passe os valores como parâmetro).

### Damage types (1.21.1)
- Damage type é registry de datapack: JSON em `data/<ns>/damage_type/<nome>.json` + `ResourceKey<DamageType>`.
- Holder: `level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(KEY)`
  (em 1.21.2+ esses métodos mudam de nome — aqui use estes).

### Montaria
- `entity.startRiding(vehicle, true)` força montar; `player.stopRiding()` desmonta.
- Jogador passageiro de `Mob` comum não controla o mob; não sobrescrever isso no MVP.

### Datagen
- `GatherDataEvent` (mod bus) para lang, modelos de item, tags. Rodar com `./gradlew runData`.

## Testes
- Lógica pura (cálculos, codecs): JUnit 5 em `src/test/java`, rodando com `./gradlew test`.
  Se precisar de registries do jogo, habilite `unitTest` do ModDevGradle (`neoForge { unitTest { enable(); testedMod = mods.wispborn } }`) e use `junit-fml`.
- Comportamento in-game: checklist manual da spec (GameTests ficam para depois do MVP).

## Se não tiver certeza de uma API
Não chute. Consulte `https://docs.neoforged.net/docs/1.21.1/` e, se ainda assim não achar,
registre a dúvida na spec e pare.
