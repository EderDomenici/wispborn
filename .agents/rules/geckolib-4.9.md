---
trigger: always_on
---

# Regras de API — GeckoLib 4.9.3 (NeoForge 1.21.1)

Versão fixa: `software.bernie.geckolib:geckolib-neoforge-1.21.1:4.9.3` (MIT, exige NeoForge ≥ 21.1.150).
As regras abaixo foram conferidas nos fontes da 4.9.3. Exemplos da internet costumam ser de outras
versões, com API diferente.

## PROIBIDO
- **GeckoLib 3** (`software.bernie.geckolib3.*`, `IAnimatable`, `AnimationFactory`, `AnimationData`,
  `AnimationBuilder`, `AnimationEvent`, `GeoEntityRenderer` com construtor antigo).
- **GeckoLib 5** (para 1.21.5+): render states e qualquer API que não exista na 4.9.3.
- Chamar `GeckoLib.initialize()`: no NeoForge a inicialização é automática.
- Inventar classe ou método. Se não achar no jar da 4.9.3, pare e escreva a dúvida na spec.

## Build
```groovy
// build.gradle
repositories {
    maven { url = 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/' }
}
dependencies {
    implementation "software.bernie.geckolib:geckolib-neoforge-1.21.1:${geckolib_version}"
}
```
- `gradle.properties`: `geckolib_version=4.9.3`.
- `neoforge.mods.toml`: dependência `geckolib`, `type="required"`, `versionRange="[${geckolib_version},)"`,
  `ordering="NONE"`, `side="BOTH"`. Acrescente `geckolib_version` ao `replaceProperties` do `generateModMetadata`.

## Assets (convenção do `DefaultedEntityGeoModel`, subtipo `entity`)
Para `new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "wisp"))`:
- `assets/wispborn/geo/entity/wisp.geo.json`
- `assets/wispborn/animations/entity/wisp.animation.json`
- `assets/wispborn/textures/entity/wisp.png`
- Glowmask: mesmo nome da textura com sufixo `_glowmask` → `textures/entity/wisp_glowmask.png`
  (lida pelo `AutoGlowingGeoLayer`).

Os nomes das animações são as chaves completas do JSON (ex.: `"animation.wisp.idle"`), não só `"idle"`.
Fonte da arte: `art/<criatura>/` (`.bbmodel` do Blockbench). O código só usa as cópias em `assets/`.

## Animatable
- **Entidade própria do mod** → a entidade implementa `GeoEntity`.
- **Substituir o visual de uma entidade existente** (ex.: o jogador em forma de wisp) → um objeto
  singleton que implementa `GeoReplacedEntity` (`getReplacingEntityType()`), com o estado de animação
  separado por entidade (`getManagerForId(entity.getId())`, feito pela própria lib).

```java
public final class WispAnimatable implements GeoReplacedEntity {
    public static final WispAnimatable INSTANCE = new WispAnimatable();
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this); // singleton automático

    @Override public EntityType<?> getReplacingEntityType() { return EntityType.PLAYER; }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public double getTick(Object entity) { return ((Entity) entity).tickCount; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "possess", 0, state -> PlayState.STOP)
                .triggerableAnim("possess", POSSESS));
    }
}
```
- `RawAnimation.begin().thenLoop("animation.x.idle")`, `.thenPlay(...)`, `.thenPlayAndHold(...)`.
  Guarde os `RawAnimation` em constantes `static final`.
- `AnimationController(animatable, nome, transiçãoEmTicks, handler)`; o handler devolve `PlayState`.
- Disparar do servidor (sincroniza sozinho):
  - `GeoReplacedEntity`: `INSTANCE.triggerAnim(entity, "controller", "anim")`;
  - `GeoEntity`: `entity.triggerAnim("controller", "anim")`.
- Singleton disparado do servidor **precisa** de `SingletonGeoAnimatable.registerSyncedAnimatable(INSTANCE)`
  no construtor do mod (código comum). Sem isso o client não sabe a qual objeto o pacote se refere.
- A classe do animatable é código **comum** (o servidor dispara animações). Ela não pode importar nada de `client/`.

## Render (só `client/`)
- Modelo: `new DefaultedEntityGeoModel<>(id)` ou uma subclasse dele.
- **Entidade própria**: `GeoEntityRenderer`, registrado em `EntityRenderersEvent.RegisterRenderers`.
- **Entidade substituída**: `GeoReplacedEntityRenderer<E, T>(context, model, animatable)`.
  - O `EntityRendererProvider.Context` vem de `EntityRenderersEvent.AddLayers#getContext()` (mod bus, `Dist.CLIENT`).
  - Para o jogador: crie o renderer no `AddLayers`, guarde numa referência estática e chame
    `renderer.render(player, yaw, partialTick, poseStack, bufferSource, packedLight)` dentro do
    `RenderPlayerEvent.Pre` (depois de cancelar o evento). Os dados vêm do evento
    (`getPartialTick`, `getPoseStack`, `getMultiBufferSource`, `getPackedLight`).
- O renderer já aplica `LivingEntity#getScale()` (atributo `SCALE`). `withScale(...)` é escala visual
  **extra**: não use para compensar a hitbox.
- Brilho: `.addRenderLayer(new AutoGlowingGeoLayer<>(this))` no construtor do renderer.
