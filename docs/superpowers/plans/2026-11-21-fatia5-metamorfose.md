# Fatia 5 — Metamorfose: genes, casulo e forma simples (F10) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** O wisp monta uma receita de genes, se encasula (como pupa no mundo ou dentro de uma criatura) e nasce
na **forma simples**: um corpo visível, com a base neutra ou quadrúpede e as peças dos genes. Ele parasita
agarrado por fora do hospedeiro, os mobs o veem e atacam, e ele pode largar o corpo ou perdê-lo sem perder o núcleo.

**Architecture:** Nova forma `FormType.SIMPLE`. "Parasitar" deixa de ser só `HOST` e passa a ser
`HostManager.isParasitizing` (HOST, ou SIMPLE agarrada a um mob), usado pela drenagem, pelo consumo, pela mana e
pelas ativas. Estado do corpo no attachment `BODY` (receita, fase alcançada, base atual). O casulo é controlado
pelo `CocoonManager`: no hospedeiro, o próprio mob; como pupa, uma `CocoonEntity`. As regras ficam em
`BodyRules`/`CocoonRules` (puras). O corpo é desenhado por um renderer GeckoLib por postura, que esconde e mostra
os ossos de gene.

**Tech Stack:** Minecraft 1.21.1, NeoForge 21.1.x, Java 21, GeckoLib 4.9.3, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-20-fagogenia-design.md` §1 (atributos em qualquer forma), §4.4 (forma
simples agarrada conta como dentro), §7 (inteira), §8 (aba Corpo). Arte: `docs/prompts/A6-corpos-art.md`.

**Branch:** `feat/F10-metamorfose`, da `main` com a F9 mergeada.

## Global Constraints

- Mesmas do plano da fatia 1; APIs da GeckoLib conforme `.agents/rules/geckolib-4.9.md`.
- A juvenil e a alfa ficam fora (spec §8, fatia 5). Também ficam fora: as posturas bípede e voadora e a aparência
  no corpo (o corpo usa a textura da A6; cores e padrões continuam só no wisp).

## Decisões de Eder (21/11/2026)

- **Largar o corpo por vontade própria:** sim, e o corpo some. Funciona como a morte do corpo: volta a ser wisp,
  não gera nuvem, a receita fica e um casulo novo devolve a fase.
- **Posturas na primeira entrega:** neutra e quadrúpede.

## Decisões do Claude

- **Encaixes da forma simples:** 2, em quaisquer dois encaixes diferentes à escolha do jogador.
- **Casulo começa pelo botão "Encasular" na aba Corpo:** como wisp, vira pupa no lugar; dentro de um hospedeiro,
  o casulo é o próprio hospedeiro.
- **Durante o casulo, o wisp não briga com o sistema imune:** a mana não cai e a drenagem, o consumo e as ativas
  ficam parados. Sair (Shift), a morte do hospedeiro ou da pupa e o logout interrompem: a fome gasta se perde e a
  receita fica.
- **Fim do casulo dentro de uma criatura:** o hospedeiro morre (estilo Alien, dano `wispborn:burst`, com drops
  normais e sem EP). Fim como pupa: a casca some.
- **"Fome e saturação cheias":** fome 20 e saturação ≥ `body.cocoonSaturation` (10). O casulo zera as duas.
- **Base:** a postura vem do campo `posture` da espécie (padrão neutra); a vida base é a vida máxima do hospedeiro
  (no casulo dentro de uma criatura) ou `body.neutralHealth` (na pupa); a escala vem da postura (config).
- **Efeitos de gene:** modificadores de atributo do JSON, mais duas flags tratadas no código: `water_breathing` e
  `night_vision`.
- **Largar o corpo:** segurar a tecla `LEAVE_BODY` (padrão B) por 1 s, para não perder o corpo sem querer.

## Review Focus

1. **Morrer na forma simples com `/kill`:** só o corpo morre (o jogador não morre, não há nuvem, volta wisp com a
   vida do wisp). Um segundo `/kill`, já como wisp, é morte real. Teste no Task 1 (regra) e conferência no Task 5.
2. **Logout durante o casulo:** ao voltar, o jogador é wisp, a receita está salva, a fome continua zerada e não
   sobra pupa no mundo. Conferência no Task 4.
3. **Hospedeiro morto por outro jogador no meio do casulo:** a interrupção sai pela mesma rota da morte do
   hospedeiro, sem exceção. Conferência no Task 4.
4. **Receita forjada** (gene não liberado, dois genes no mesmo encaixe, mais encaixes que a fase): o servidor recusa.
   Teste no Task 1.
5. **Forma simples desagarrada perto de mobs hostis:** é alvo normal (não fica invisível como o HOST). Conferência no Task 5.

## Passo zero

- [ ] Confira na `main`: `GeneDef`/`GeneSlot`, `SpeciesDef`, `AssimilationRules`, `HostManager.enterHost`,
  `HostManager.Reason` (com `JUMP` e `EXPELLED`), `AbilityRules.checkUse(..., boolean inHost, ...)`,
  `GrimoireTab.BODY`, `FormManager.applyModifiers`, `WispRenderHandler` (onde o renderer da forma é escolhido) e
  como o `WispAnimatable` é registrado (`SingletonGeoAnimatable.registerSyncedAnimatable`). Divergência → anote em
  `docs/specs/F10-metamorfose.md`, seção "Dúvidas do implementador".
- [ ] Confira que a A6 entregou os modelos e as texturas listados no Task 5. Sem eles, os Tasks 1 a 4 seguem
  (o corpo aparece com o modelo do wisp até o Task 5).

---

## Arquivos

| Arquivo | Responsabilidade | Task |
|---|---|---|
| `body/Posture.java`, `BodyBase.java`, `BodyState.java`, `BodyRules.java`, `RecipeResult.java`, `CocoonRules.java`, `CocoonResult.java`, `GeneEffects.java` | estado e regras puras | 1 |
| `assimilation/SpeciesDef.java`, `GeneDef.java` (mod.), JSON de espécies e genes (mod.) | postura e efeitos de gene | 1 |
| `form/FormType.java`, `FormManager.java` (mod.), `host/HostManager.java`, `event/HostEvents.java`, `event/FormEvents.java`, `ability/AbilityManager.java` (mod.) | forma simples e "parasitar" | 2 |
| `body/BodyManager.java`, `event/BodyEvents.java`, `network/SetRecipePayload.java`, `LeaveBodyPayload.java`, `SyncBodyPayload.java` | corpo no servidor | 3 |
| `body/CocoonManager.java`, `body/CocoonEntity.java`, `registry/ModEntities.java`, `network/StartCocoonPayload.java`, `CocoonStatePayload.java`, `data/wispborn/damage_type/burst.json` | casulo | 4 |
| `client/BodyAnimatable.java`, `BodyModel.java`, `BodyPlayerRenderer.java`, `CocoonRenderer.java`, `ClientBodies.java`, `WispRenderHandler.java` (mod.) | desenhar o corpo | 5 |
| `client/grimoire/BodyPage.java`, `client/KeyBindings.java`, `client/hud/WispHud.java` (mod.) | aba Corpo, tecla e barra do casulo | 6 |

---

### Task 1: Regras puras do corpo e do casulo

**Files:**
- Create: os arquivos de regra acima, em `src/main/java/dev/eder/wispborn/body/`
- Modify: `assimilation/SpeciesDef.java` (campo `posture`, opcional, padrão `NEUTRAL`), `assimilation/GeneDef.java`
  (campos opcionais `attributes: Map<ResourceLocation, Double>` e `flags: List<String>`), os JSON de
  `wisp_species` (`posture: "quadruped"` em `cow`, `pig`, `sheep`, `spider`, `polar_bear`) e de `wisp_genes`
- Test: `src/test/java/dev/eder/wispborn/body/BodyRulesTest.java`, `CocoonRulesTest.java`, `BodyStateTest.java`, `GeneEffectsTest.java`

**Interfaces:**
- Produces:
  - `enum Posture { NEUTRAL, QUADRUPED }` (`StringRepresentable`)
  - `record BodyBase(Posture posture, double baseHealth, double scale)` com `CODEC` e `STREAM_CODEC`
  - `enum Phase { WISP, SIMPLE }` (a juvenil e a alfa entram depois)
  - `record BodyState(Map<GeneSlot, ResourceLocation> recipe, Phase reached, Optional<BodyBase> current)` com
    `EMPTY`, `CODEC`, `STREAM_CODEC`
  - `enum RecipeResult { OK, UNKNOWN_GENE, GENE_LOCKED, WRONG_SLOT, TOO_MANY_GENES, COCOONING }`
  - `BodyRules.checkRecipeChange(BodyState s, GeneSlot slot, Optional<ResourceLocation> gene, Optional<GeneDef> def, boolean unlocked, int phaseSlots, boolean cocooning): RecipeResult`.
    Ordem: COCOONING; remover (`gene` vazio) → OK; UNKNOWN_GENE; WRONG_SLOT (`def.slot != slot`); GENE_LOCKED;
    TOO_MANY_GENES (encaixe novo com a receita já cheia); OK. Trocar o gene de um encaixe já ocupado não conta como encaixe novo.
  - `BodyRules.metabolism(Collection<GeneDef> genes, double hungerPerComplexity): double` = `1 + hungerPerComplexity × Σ complexidade`
  - `enum CocoonResult { OK, ALREADY, WRONG_FORM, LEVEL_TOO_LOW, MISSING_GENES, GENE_LOCKED, NOT_FED }`
  - `CocoonRules.check(boolean cocooning, boolean wispOrHost, int level, int minLevel, int recipeSize, int phaseSlots, boolean allGenesUnlocked, int food, float saturation, double minSaturation): CocoonResult`
    (nessa ordem; `MISSING_GENES` quando `recipeSize < phaseSlots`; `NOT_FED` quando `food < 20` ou `saturation < minSaturation`)
  - `GeneEffects.KNOWN_FLAGS = Set.of("water_breathing", "night_vision")`, `GeneEffects.unknownFlags(GeneDef): List<String>`
- Config: `body.simpleMinLevel` (int 5), `body.simpleSlots` (int 2), `body.cocoonTicks` (int 1200),
  `body.cocoonSaturation` (double 10.0), `body.hungerPerComplexity` (double 0.1), `body.neutralHealth` (double 16),
  `body.neutralScale` (double 0.6), `body.quadrupedScale` (double 0.8), `body.pupaHealth` (double 20).
- Efeitos da primeira leva (`wisp_genes`): `spider_legs` `{minecraft:generic.movement_speed: 0.02, minecraft:generic.step_height: 0.5}`;
  `spider_fangs` `{minecraft:generic.attack_damage: 2}`; `cow_hide` `{minecraft:generic.armor: 3}`;
  `cod_gills` flag `water_breathing`; `bat_ears` flag `night_vision`.

- [ ] **Step 1: Write the failing tests**

```java
// BodyRulesTest (legs = GeneDef(LOCOMOTION, 2, 600), hide = GeneDef(COVERING, 1, 300), fangs = GeneDef(WEAPON, 2, 600))
@Test void recipeChangeFollowsThePrecedence() {
    // COCOONING; remover → OK; UNKNOWN_GENE; WRONG_SLOT (legs em COVERING); GENE_LOCKED;
    // TOO_MANY_GENES (receita com legs e hide, 2 encaixes, tentar fangs em WEAPON); OK
}
@Test void swappingAGeneInAFilledSlotIsAllowedWhenFull() { /* receita cheia, trocar o gene do LOCOMOTION → OK */ }
@Test void metabolismGrowsWithComplexity() { assertEquals(1.3D, BodyRules.metabolism(List.of(legs, hide), 0.1D), 1e-9); }
// CocoonRulesTest: uma asserção por resultado, na ordem; OK com nível 5, 2 genes, fome 20 e saturação 10
// BodyStateTest: CODEC e STREAM_CODEC ida e volta com receita, fase SIMPLE e base QUADRUPED
// GeneEffectsTest.shippedGenesUseKnownFlags: JSON reais de wisp_genes → unknownFlags vazio em todos
```

- [ ] **Step 2: Run them and confirm they fail** — `./gradlew test --tests "dev.eder.wispborn.body.*"`.
- [ ] **Step 3: Implement** (e ajuste os testes de codec da F7 se o campo novo pedir).
- [ ] **Step 4: Run them and confirm they pass**, depois `./gradlew build`.
- [ ] **Step 5: Commit** — `feat(body): receita de genes, metabolismo e requisitos do casulo`.

---

### Task 2: Forma simples e o que é "parasitar"

**Files:**
- Modify: `form/FormType.java`, `form/FormState.java`, `form/FormManager.java`, `host/HostManager.java`,
  `event/HostEvents.java`, `event/FormEvents.java`, `ability/AbilityManager.java`, `ability/ExposureManager.java`,
  `progress/ProgressManager.java`, `registry/ModAttachments.java`, lang
- Test: `src/test/java/dev/eder/wispborn/form/FormStateTest.java`, `host/ParasiteRulesTest.java` (novo), `host/ParasiteRules.java` (novo)

**Interfaces:**
- Produces:
  - `FormType.SIMPLE("simple")` (no fim do enum, para não mudar os ordinais de rede); `FormState.SIMPLE`
  - `FormManager.isSimple(Player)`; `isParasite` passa a incluir SIMPLE
  - `ModAttachments.BODY` (padrão `BodyState.EMPTY`, `serialize`, `copyOnDeath`)
  - `ParasiteRules.isParasitizing(FormType type, boolean ridingMob): boolean` = `HOST || (SIMPLE && ridingMob)`
  - `HostManager.isParasitizing(Player)`. Substitui `isHost` em: `tickDrain`, `tryConsume`,
    `applyImmuneDrain` (e o bloco de mana do `FormEvents`), `computeState` (`hostConsumeReady`, `hostEpFactor`,
    `hostManaDrainPerSecond`), `AbilityManager.use` (`inHost`) e `HostEvents.onLivingDeath` (passageiro de mob
    que morre). **Continuam só HOST:** invulnerabilidade (`onLivingIncomingDamage`), furtividade
    (`StealthRules`, `clearPursuers`), invisibilidade e partículas de possuído.
  - `HostManager.latch(ServerPlayer, Mob)`: a forma simples agarra o mob (`startRiding(mob, true)`, visível, mesma
    forma); `tryInfest` e o fim da conjuração aceitam WISP (→ `enterHost`) e SIMPLE desagarrada (→ `latch`).
  - `release` para SIMPLE agarrada: só `stopRiding` e o teleporte para cima do mob; a forma continua SIMPLE.
  - `FormManager.applyModifiers` para SIMPLE: `SCALE` = `base.scale - 1` (ADD_MULTIPLIED_TOTAL, no lugar do do
    wisp); `MAX_HEALTH` = `base.baseHealth - 20` + Vitalidade (spec §1); sem a gravidade, a queda e a velocidade
    do wisp; modificadores de cada gene da receita com id `wispborn:gene/<slot>` (ADD_VALUE). Flags (no tick do
    `BodyEvents`, Task 3): `water_breathing` mantém o ar cheio; `night_vision` aplica `NIGHT_VISION` de 15 s, renovado.
  - `ExposureManager`: SIMPLE desagarrada também sofre o ambiente; agarrada, não (conta como dentro).
  - Exaustão de fome: WISP **e** SIMPLE; na SIMPLE, multiplicada por `BodyRules.metabolism`.

- [ ] **Step 1: Write the failing tests** — `ParasiteRulesTest`: HOST sem montaria → true; SIMPLE sem montaria →
  false; SIMPLE montada → true; WISP montada → false. `FormStateTest`: ida e volta de `FormState.SIMPLE`, e um save
  antigo com `"type": "wisp"` continua WISP.
- [ ] **Step 2: Run them and confirm they fail**, **implement**, **run again** (PASS).
- [ ] **Step 3: Build** (`./gradlew build`, verde). Com `/wispborn form simple` (novo, no comando `form`), confira:
  - a forma simples anda e é atacada por zumbis;
  - R num porco agarra o porco, visível;
  - a drenagem e a mana funcionam como no HOST;
  - Shift solta o porco.
- [ ] **Step 4: Commit** — `feat(form): forma simples, que parasita agarrada por fora do hospedeiro`.

---

### Task 3: Corpo no servidor (receita, largar, morte do corpo)

**Files:**
- Create: `body/BodyManager.java`, `event/BodyEvents.java`, `network/SetRecipePayload.java`, `LeaveBodyPayload.java`, `SyncBodyPayload.java`, `SyncBodyStatePayload.java`
- Modify: `network/ModPayloads.java`, `WispbornMod.java`, `event/FormEvents.java` (`onStartTracking`, login), lang
- Test: `src/test/java/dev/eder/wispborn/network/BodyPayloadsTest.java`

**Interfaces:**
- Produces:
  - `SetRecipePayload(GeneSlot slot, Optional<ResourceLocation> gene)` (cliente → servidor) → `BodyManager.setRecipe`:
    `checkRecipeChange` com o gene liberado em `Assimilation` e `body.simpleSlots`; falha → `wispborn.body.recipe.<resultado>`
  - `LeaveBodyPayload()` (cliente → servidor) → `BodyManager.leaveBody`: só SIMPLE; solta o mob se estiver agarrada,
    `current = empty`, `setForm(WISP)`, `wispborn.body.left`
  - `BodyManager.onBodyDeath(ServerPlayer)`: igual a `leaveBody`, com a mensagem `wispborn.body.died` e a vida do
    wisp cheia depois do `setForm`
  - `BodyEvents.onLivingDeath` com `@SubscribeEvent(priority = EventPriority.HIGH)`: jogador SIMPLE → `event.setCanceled(true)`,
    `setHealth(1)` e `onBodyDeath`. Como o evento é cancelado, o handler `LOWEST` da nuvem de DNA (F6) não roda (spec §7: corpo morto não gera nuvem).
  - `SyncBodyPayload(int entityId, Optional<BodyView> body)`, com `record BodyView(Posture posture, Map<GeneSlot, ResourceLocation> genes)`:
    vai para quem vê o jogador (`sendToPlayersTrackingEntityAndSelf`) ao mudar de forma, no `StartTracking` e no
    login. Vazio fora da SIMPLE.
  - `SyncBodyStatePayload(BodyState state)` (servidor → só o dono): o estado inteiro, para a aba Corpo; vai no login
    e a cada mudança de receita, base ou fase.

  Lang (pt_br / en_us): `wispborn.body.left` Você largou o corpo. / You left your body.; `wispborn.body.died`
  Seu corpo morreu. O núcleo sobreviveu. / Your body died. The core survived.; `wispborn.body.recipe.unknown_gene`
  Gene desconhecido / Unknown gene; `.gene_locked` Gene ainda não liberado / Gene not unlocked yet; `.wrong_slot`
  Esse gene não vai nesse encaixe / That gene does not fit this slot; `.too_many_genes` Todos os encaixes da fase
  estão ocupados / All slots for this phase are taken; `.cocooning` Não dá para mudar durante o casulo / Cannot change
  while in the cocoon.

- [ ] **Step 1: Write the failing test** — ida e volta de `SetRecipePayload`, `LeaveBodyPayload`, `SyncBodyPayload` (com e sem corpo) e `SyncBodyStatePayload`.
- [ ] **Step 2: Run it and confirm it fails**, **implement**, **run again** (PASS).
- [ ] **Step 3: Build** e **confira:** com `/wispborn form simple`, `/kill` volta a wisp sem morrer e sem nuvem;
  o segundo `/kill` é morte com nuvem.
- [ ] **Step 4: Commit** — `feat(body): receita no servidor, largar o corpo e morte do corpo sem nuvem`.

---

### Task 4: Casulo

**Files:**
- Create: `body/CocoonManager.java`, `body/CocoonEntity.java`, `registry/ModEntities.java`, `network/StartCocoonPayload.java`,
  `CocoonStatePayload.java`, `data/wispborn/damage_type/burst.json`
- Modify: `registry/ModDamageTypes.java` (`BURST`), `host/HostManager.java` (pausas e interrupção), `ability/AbilityManager.java`,
  `WispbornMod.java`, `command/WispbornCommand.java`, lang

**Interfaces:**
- Produces:
  - `CocoonEntity extends Mob`: sem IA (`setNoAi(true)`), vida `body.pupaHealth`, sem drops, dono por UUID; não é
    salva no chunk (`shouldBeSaved` → false), porque logout interrompe. Registrada em `ModEntities` como `wispborn:cocoon`,
    com atributos padrão de `Mob` e vida do config aplicada ao nascer.
  - `CocoonManager.start(ServerPlayer): CocoonResult`: `CocoonRules.check` (forma WISP ou HOST, `level`,
    `body.simpleMinLevel`, receita, genes liberados, fome, saturação). OK → zera fome e saturação; HOST → registra o
    casulo no hospedeiro atual; WISP → cria a `CocoonEntity` na posição, `FormState.host(wispborn:cocoon)`,
    invisível, `startRiding(cocoon, true)`. Guarda `(início, fim = início + body.cocoonTicks, tipo, id do mob)` num
    mapa por UUID. Manda `CocoonStatePayload(início, duração)`.
  - `CocoonManager.isCocooning(ServerPlayer): boolean`. Enquanto verdadeiro, `HostManager.tickDrain`,
    `applyImmuneDrain`, `tryConsume` e `AbilityManager.use` retornam logo no começo (sem efeito nem mensagem).
    O `tickDrain` não solta a pupa por falta de traços.
  - `CocoonManager.interrupt(ServerPlayer, Component reason)`: tira do mapa, `CocoonStatePayload.NONE`, descarta a
    pupa se houver, mensagem `wispborn.cocoon.interrupted`. Chamado por `HostManager.release` (qualquer motivo
    durante o casulo) e no logout.
  - `CocoonManager.tick(ServerPlayer)` no fim: tira do mapa **antes** de soltar (para não virar interrupção); define
    a base (dentro de criatura: postura da espécie, vida máxima do mob e escala da postura; pupa: `NEUTRAL`,
    `body.neutralHealth` e `body.neutralScale`); `release(player, JUMP)`; grava `BodyState(current = base,
    reached = SIMPLE)`; `setForm(SIMPLE)`; mata o hospedeiro com dano `wispborn:burst` ou descarta a pupa; som
    `TURTLE_EGG_HATCH`, partículas e mensagem `wispborn.cocoon.done`.
  - `StartCocoonPayload()` (cliente → servidor) → `start`; falha → `wispborn.cocoon.<resultado>`.
  - Comando de teste: `/wispborn cocoon finish [player]` termina o casulo na hora.

  Lang (pt_br / en_us): `wispborn.cocoon.started` A metamorfose começou. / The metamorphosis began.; `.done` Você
  nasceu na forma simples. / You emerged in your simple form.; `.interrupted` O casulo foi interrompido. / The cocoon
  was interrupted.; `.already` Você já está no casulo. / You are already in a cocoon.; `.wrong_form` Encasule como
  wisp ou dentro de um hospedeiro / Cocoon as a wisp or inside a host; `.level_too_low` Exige nível %s / Requires level
  %s; `.missing_genes` Escolha %s genes / Choose %s genes; `.gene_locked` Um gene da receita não está liberado / A recipe
  gene is not unlocked; `.not_fed` Exige fome cheia e saturação %s / Requires full hunger and %s saturation;
  `death.attack.wispborn.burst` %1$s explodiu de dentro para fora / %1$s burst from within; `entity.wispborn.cocoon` Casulo / Cocoon.

- [ ] **Step 1: Implement** e rode `./gradlew build`.
- [ ] **Step 2: Check in game** (`/wispborn level set 5`, genes liberados e na receita, fome e saturação cheias por comando):
  - Encasular dentro de uma vaca: a mana para de cair. Com `/wispborn cocoon finish`, a vaca morre e o jogador
    nasce quadrúpede com a vida da vaca + Vitalidade.
  - Encasular como wisp cria a pupa. Bater nela até matar interrompe: a fome fica zerada e a receita salva.
  - Logout durante o casulo: na volta, você é wisp e não sobrou pupa.
  - Shift durante o casulo interrompe.
- [ ] **Step 3: Commit** — `feat(body): casulo como pupa ou dentro de uma criatura`.

---

### Task 5: Desenhar o corpo e a pupa

**Files:**
- Create: `client/BodyAnimatable.java`, `client/BodyModel.java`, `client/BodyPlayerRenderer.java`, `client/CocoonRenderer.java`,
  `client/ClientBodies.java`; assets da A6: `geo/entity/body_neutral.geo.json`, `body_quadruped.geo.json`,
  `cocoon.geo.json`, `animations/entity/body_neutral.animation.json`, `body_quadruped.animation.json`,
  `cocoon.animation.json`, `textures/entity/body_neutral.png`, `body_quadruped.png`, `cocoon.png`
- Modify: `client/WispRenderHandler.java`, `client/ClientPayloadHandler.java`, registro do renderer da `CocoonEntity`

**Interfaces:**
- Produces:
  - `ClientBodies.get(UUID): Optional<BodyView>` (do `SyncBodyPayload`; reset no logout)
  - `BodyAnimatable`: mesmo padrão do `WispAnimatable` (singleton sincronizado), com os clipes `idle`, `walking` e
    `latched` (agarrada a um mob)
  - `BodyModel`: geo, textura e animação pela postura do jogador desenhado
  - `BodyPlayerRenderer extends GeoReplacedEntityRenderer<AbstractClientPlayer, BodyAnimatable>`: no `preRender`,
    para cada `GeneSlot`, mostra o osso `gene_<slot>_<path do gene>` escolhido e esconde os outros `gene_<slot>_*`;
    sem gene no encaixe, mostra `base_<slot>`. Use `getBone(...)`/`setHidden(...)` da GeckoLib 4.9.3 (confira os nomes).
  - `WispRenderHandler`: jogador SIMPLE → `BodyPlayerRenderer`; os outros casos como hoje.
  - `CocoonRenderer` (GeckoLib) com o clipe `pulse`.

- [ ] **Step 1: Implement** e rode `./gradlew build`.
- [ ] **Step 2: Check in game:**
  - Quadrúpede com `spider_legs` e `cow_hide` mostra as patas e o couro.
  - Remover o couro da receita só muda o corpo no próximo casulo.
  - Um segundo jogador vê o mesmo corpo.
  - Agarrada a um porco, toca o clipe `latched`.
  - A pupa pulsa.
- [ ] **Step 3: Commit** — `feat(client): corpo modular por postura e casulo`.

---

### Task 6: Aba Corpo, tecla de largar e barra do casulo

**Files:**
- Create: `client/grimoire/BodyPage.java`, sprites `grimoire/gene_slot.png`, `gene_slot_selected.png` (22×22),
  `icon_locomotion.png`, `icon_covering.png`, `icon_breathing.png`, `icon_weapon.png`, `icon_senses.png` (9×9),
  `complexity_bar_background.png`, `complexity_bar.png` (90×5)
- Modify: `client/grimoire/GrimoireTab.java` (BODY com página), `GrimoireSprites.java`, `client/KeyBindings.java`,
  `client/hud/WispHud.java`, `client/ClientPayloadHandler.java`, lang

Layout da `BodyPage` (relativo ao painel):
- Encaixes no topo da direita: cinco `gene_slot` em `x = 116 + i × 30`, y 24, na ordem do `GeneSlot`, com o ícone do
  encaixe (9×9) no centro e o nome do gene embaixo em uma linha de texto cortada (`ellipsize` em 28 px). O
  selecionado usa `gene_slot_selected`. Com a receita cheia, os encaixes vazios ficam escurecidos (`setShaderColor`
  0.5) e sem clique. Clique direito num encaixe ocupado → `SetRecipePayload(slot, empty)`.
- Lista à esquerda, x 8..108, y 24 em diante: os genes liberados do encaixe selecionado (`species_row`, 14 px), com a
  complexidade à direita. Clique → `SetRecipePayload(slot, gene)`.
- Metabolismo, y 64: `wispborn.body.metabolism` com o multiplicador (uma casa) e `complexity_bar` preenchida pela
  soma de complexidade sobre `5 × 3` (teto visual).
- Requisitos do casulo, y 80 em diante, uma linha de 11 px cada, com `req_ok`/`req_missing`: nível mínimo; genes
  escolhidos / encaixes; fome cheia; saturação mínima.
- Rodapé: `wispborn.body.phase` com a fase alcançada em x 116, y 181; botão `Encasular` 92×18 em x 216, y 176
  (`BUTTON_PRIMARY` com tudo atendido e forma WISP ou HOST, senão desativado) → `StartCocoonPayload`.
- Na forma SIMPLE, a página mostra a receita, mas o botão fica desativado com `wispborn.body.in_body` (a juvenil vem depois).

Tecla: `KeyBindings.LEAVE_BODY` (`GLFW_KEY_B`, lang `key.wispborn.leave_body` Largar o corpo / Leave body). Na forma
SIMPLE, sem tela aberta, segurar 20 ticks seguidos manda `LeaveBodyPayload`. Soltar antes zera a contagem. Enquanto
a tecla está segurada, a barra de conjuração do HUD mostra o progresso (mesmo slot central).

Barra do casulo: com `CocoonStatePayload` ativo, o slot central inferior desenha `cast_bar_background`/`cast_bar_progress`
pelo tempo decorrido e o texto `wispborn.cocoon.progress` acima. A prioridade fica: conjuração > casulo > Consumir > nuvem.

Lang (pt_br / en_us): `wispborn.body.metabolism` Metabolismo ×%s / Metabolism ×%s; `.phase` Fase: %s / Phase: %s;
`.phase.wisp` Wisp / Wisp; `.phase.simple` Forma simples / Simple form; `.cocoon` Encasular / Cocoon; `.in_body`
Largue o corpo para encasular / Leave your body to cocoon; `.req.level` Nível %s / Level %s; `.req.genes` Genes %s/%s
/ Genes %s/%s; `.req.food` Fome cheia / Full hunger; `.req.saturation` Saturação %s / Saturation %s;
`wispborn.cocoon.progress` Metamorfose / Metamorphosis; `wispborn.gene_slot.<slot>` Locomoção, Cobertura, Respiração,
Arma, Sentidos / Locomotion, Covering, Breathing, Weapon, Senses.

- [ ] **Step 1: Implement** (sprites da A6 ou placeholders: `gene_slot*` de `ability_slot*`, ícones de `hud/dna_icon`,
  barra de `grimoire/bar_background_short`/`bar_hunger`).
- [ ] **Step 2: Build** (`./gradlew build`, verde) e **confira no jogo** o fluxo inteiro sem comandos:
  - consumir aranhas e vacas até descobrir os genes;
  - liberar os genes no Bestiário;
  - montar a receita na aba Corpo;
  - drenar até a fome e a saturação ficarem cheias;
  - encasular dentro de uma vaca;
  - nascer quadrúpede;
  - agarrar um porco e drenar;
  - segurar B para largar o corpo.
- [ ] **Step 3: Commit** — `feat(grimoire): aba Corpo com receita e casulo; tecla para largar o corpo`.

---

### Task 7: Documentação

- [ ] `docs/specs/F10-metamorfose.md` (aponta para este plano; dúvidas e relatório), `CHANGELOG.md`, linha F10 do
  `docs/PLAN.md`, pacote `body/` e `FormType.SIMPLE` no `AGENTS.md`.
- [ ] `./gradlew build` verde, e commit: `docs(F10): relatorio da metamorfose, CHANGELOG e PLAN`.
