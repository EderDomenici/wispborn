# Fatia 3 — Habilidades (F8) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Habilidades liberadas passam a funcionar: 3 espaços de ativas com tecla, mana e recarga (Teia, Pular de
corpo), passivas sempre ligadas (Tomar conta do corpo), adaptações com dano de ambiente (Calor, Frio), HUD com os
espaços e a aba Habilidades para equipar.

**Architecture:** Regras puras (`AbilityRules`, `AdaptationRules`) decidem uso, equipamento e exposição; os efeitos
de cada ativa ficam em `AbilityEffect` registrados por id em `AbilityEffects`. Estado no attachment `ABILITIES`
(espaços e recargas). Os números de cada grau ficam no `params` do JSON da fatia 2. Pilotar o hospedeiro é o último
task, isolado, porque depende de mixin.

**Tech Stack:** Minecraft 1.21.1, NeoForge 21.1.x, Java 21, JUnit 5, Mixin (só no Task 7).

**Spec:** `docs/superpowers/specs/2026-11-20-fagogenia-design.md` §5 (inteira), §8 (aba Habilidades, espaços no
HUD). Arte: `docs/prompts/A5-fagogenia-art.md` (Habilidades, `hud_ability_slot`, `hud_ability_cooldown` e os ícones
de habilidade, confirmados aqui como contrato).

**Branch:** `feat/F8-habilidades`, da `main` com a F7 mergeada.

## Global Constraints

- Mesmas do plano da fatia 1, mais: mixin só no Task 7, registrado em `wispborn.mixins.json`, seguindo
  `.agents/rules/neoforge-1.21.1.md`.
- Toda ativa tem `mana` e `cooldown_ticks` no `params` de cada grau; nada de número de efeito no código.

## Review Focus

1. **Tecla apertada com o grimório ou o chat abertos:** nada acontece (as teclas só valem com `mc.screen == null`).
   Conferência no Task 5.
2. **Habilidade equipada que deixou de existir depois de `/reload`** (removida do JSON): o espaço fica vazio no uso,
   sem exceção. Teste no Task 1.
3. **Pacote de uso forjado** (espaço fora de 0..2, habilidade não liberada, recarga): o servidor recusa. Teste no Task 1.
4. **Pular de corpo para um mob que o wisp não pode possuir** (nível, adaptação): recusa com a mesma mensagem da
   infestação e não gasta a recarga. Teste no Task 3 (regra) e conferência no jogo.
5. **Entrar no Nether como wisp sem Calor II:** dano em intervalos, com aviso; dentro de um hospedeiro, nenhum.
   Teste no Task 4.

## Passo zero

- [ ] Confira na `main`: `AssimilationRules.abilityDegree(Assimilation, ResourceLocation)`, `WispData.ABILITIES`,
  `AbilityDef/AbilityDegree(params)`, `UnlockNames.displayName`, `ClientAssimilation`, `ClientWispData`,
  `GrimoireTab`/`GrimoirePage`, `InfestRules.check(..., boolean adaptationsMet, float mana, int minMana)`,
  `HostManager.Reason`. Divergência → pare e anote em `docs/specs/F8-habilidades.md`, seção "Dúvidas do implementador".

---

## Arquivos

| Arquivo | Responsabilidade | Task |
|---|---|---|
| `ability/AbilityLoadout.java`, `AbilityState.java`, `AbilityRules.java`, `UseResult.java`, `EquipResult.java`, `AbilityParams.java` | estado e regras de uso e equipamento | 1 |
| `ability/AbilityEffect.java`, `AbilityEffects.java`, `WebEffect.java`, `BodyJumpEffect.java`, `AbilityManager.java` | efeitos e ligação ao jogo | 2, 3 |
| `ability/AdaptationRules.java`, `ability/ExposureManager.java`, `data/wispborn/damage_type/exposure.json` | dano de ambiente | 4 |
| `network/UseAbilityPayload.java`, `EquipAbilityPayload.java`, `SyncAbilitiesPayload.java` | rede | 2 |
| `client/KeyBindings.java`, `client/ClientAbilities.java`, `client/hud/WispHud.java`, `HudMath.java` (mod.) | teclas e HUD | 5 |
| `client/grimoire/AbilitiesPage.java`, `GrimoireTab` (mod.) | aba Habilidades | 6 |
| `mixin/HostControlMixin.java`, `host/HostControl.java` | pilotar o hospedeiro | 7 |
| `data/wispborn/wisp_abilities/web.json`, `body_jump.json` (mod.) | `params` dos graus | 2, 3 |

---

### Task 1: Estado e regras puras

**Files:**
- Create: os arquivos do Task 1 acima, em `src/main/java/dev/eder/wispborn/ability/`
- Modify: `registry/ModAttachments.java`
- Test: `src/test/java/dev/eder/wispborn/ability/AbilityRulesTest.java`, `AbilityStateTest.java`, `AbilityParamsTest.java`

**Interfaces:**
- Produces:
  - `record AbilityLoadout(List<Optional<ResourceLocation>> slots)`: sempre 3 posições; `EMPTY`; `get(int)`;
    `with(int slot, Optional<ResourceLocation> id)` (equipar uma habilidade que já está em outro espaço tira de lá)
  - `record AbilityState(AbilityLoadout loadout, Map<ResourceLocation, Cooldown> cooldowns)` com
    `record Cooldown(long readyAt, int total)`; `EMPTY`; `CODEC`; `STREAM_CODEC`
  - `ModAttachments.ABILITIES` (padrão `AbilityState.EMPTY`, `serialize`, `copyOnDeath`)
  - `enum UseResult { OK, BAD_SLOT, EMPTY_SLOT, UNKNOWN, NOT_UNLOCKED, NOT_ACTIVE, BLOCKED_IN_HOST, ON_COOLDOWN, NO_MANA }`
  - `AbilityRules.checkUse(int slot, AbilityState state, Optional<AbilityDef> def, int unlockedDegree, boolean inHost, boolean hasTakeOver, float mana, long gameTime): UseResult`
    Ordem: BAD_SLOT, EMPTY_SLOT, UNKNOWN (def vazio), NOT_ACTIVE, NOT_UNLOCKED (grau 0), BLOCKED_IN_HOST (`inHost && !hasTakeOver`),
    ON_COOLDOWN (`gameTime < readyAt`), NO_MANA (`mana < params.mana` do grau liberado), OK.
  - `enum EquipResult { OK, BAD_SLOT, UNKNOWN, NOT_ACTIVE, NOT_UNLOCKED }`
  - `AbilityRules.checkEquip(int slot, Optional<ResourceLocation> id, Optional<AbilityDef> def, int unlockedDegree): EquipResult`
    (`id` vazio = desequipar → OK se o espaço é válido)
  - `AbilityParams`: `mana(AbilityDegree): double`, `cooldownTicks(AbilityDegree): int`, `get(AbilityDegree, String key): OptionalDouble`
  - `AbilityParams.missingActiveKeys(AbilityDef): List<String>` (ACTIVE sem `mana` ou `cooldown_ticks` em algum grau)

- [ ] **Step 1: Write the failing tests**

```java
// AbilityRulesTest (web(): ACTIVE com graus {mana 15, cooldown_ticks 160} e {mana 20, cooldown_ticks 160})
@Test void useFollowsThePrecedence() {
    // BAD_SLOT (slot 3); EMPTY_SLOT; UNKNOWN (equipada mas sem def, ex.: removida no /reload); NOT_ACTIVE (heat);
    // NOT_UNLOCKED (grau 0); BLOCKED_IN_HOST (inHost, sem take_over); ON_COOLDOWN (gameTime 99, readyAt 100);
    // NO_MANA (14.9 de mana para 15); OK (15 de mana, gameTime 100)
}
@Test void takeOverUnblocksActivesInsideTheHost() { /* inHost true, hasTakeOver true → OK */ }
@Test void secondDegreeCostsItsOwnMana() { /* grau 2 liberado, 19.9 de mana → NO_MANA; 20 → OK */ }
@Test void equipMovesAnAbilityBetweenSlots() {
    // web no espaço 0; equipar web no 2 → espaço 0 vazio, 2 com web
}
@Test void equipRejectsPassivesAndLockedAbilities() { /* NOT_ACTIVE para take_over; NOT_UNLOCKED para web grau 0; OK para desequipar */ }

// AbilityStateTest: CODEC e STREAM_CODEC ida e volta com um espaço vazio e uma recarga
// AbilityParamsTest
@Test void shippedActivesDeclareManaAndCooldown() {
    // carrega os JSON reais de wisp_abilities (mesmo leitor dos testes da F7): missingActiveKeys vazio em todos
}
```

- [ ] **Step 2: Run them and confirm they fail** — `./gradlew test --tests "dev.eder.wispborn.ability.*"` (FAIL de compilação).
- [ ] **Step 3: Implement.** No mesmo task, preencha o `params` dos JSON: `web` grau 1 `{mana 15, cooldown_ticks 160, range 12, amplifier 1, duration_ticks 100}`,
  grau 2 `{mana 20, cooldown_ticks 160, range 12, amplifier 2, duration_ticks 160}`; `body_jump` `{mana 0, cooldown_ticks 400, range 6}`.
- [ ] **Step 4: Run them and confirm they pass.**
- [ ] **Step 5: Commit** — `feat(ability): estado, espacos de ativas e regras de uso`.

---

### Task 2: Usar e equipar no servidor, com a Teia

**Files:**
- Create: `ability/AbilityEffect.java`, `AbilityEffects.java`, `WebEffect.java`, `AbilityManager.java`,
  `network/UseAbilityPayload.java`, `EquipAbilityPayload.java`, `SyncAbilitiesPayload.java`, `event/AbilityEvents.java`
- Modify: `network/ModPayloads.java`, `WispbornMod.java`, lang
- Test: `src/test/java/dev/eder/wispborn/network/AbilityPayloadsTest.java`, `ability/AbilityEffectsTest.java`

**Interfaces:**
- Consumes: Task 1; `AssimilationManager.state`, `AssimilationRules.abilityDegree`, `ProgressManager.getMana/setMana`.
- Produces:
  - `interface AbilityEffect { boolean apply(ServerPlayer player, AbilityDegree degree); }` (devolve `false` se não
    houve efeito, como alvo ausente; aí não gasta mana nem recarga, e o efeito manda a própria mensagem)
  - `AbilityEffects.get(ResourceLocation): Optional<AbilityEffect>`; `AbilityEffects.KNOWN: Set<ResourceLocation>`
    (`wispborn:web`, `wispborn:body_jump`)
  - `AbilityManager.use(ServerPlayer, int slot): UseResult`, `AbilityManager.equip(ServerPlayer, int slot, Optional<ResourceLocation>): EquipResult`,
    `AbilityManager.sync(ServerPlayer)`; o efeito roda a partir do olho do **hospedeiro** quando o jogador está nele
    (`AbilityManager.origin(ServerPlayer): LivingEntity`)
  - `UseAbilityPayload(int slot)` e `EquipAbilityPayload(int slot, Optional<ResourceLocation> id)` (cliente → servidor);
    `SyncAbilitiesPayload(AbilityState state)` (servidor → cliente), enviado no login, no respawn e a cada mudança
  - `AbilityEvents` (registrado em `WispbornMod`): login e respawn → `AbilityManager.sync`; o Task 4 acrescenta o tick de exposição
  - `WebEffect`: raio a partir de `origin` até `range` (mesma busca de `HostManager.computeTargetInfo`, excluindo o
    próprio hospedeiro); acertou um `LivingEntity` → `MobEffects.MOVEMENT_SLOWDOWN` com `duration_ticks` e
    `amplifier`, partículas `ParticleTypes.ITEM` de teia ao longo do raio e som `SPIDER_AMBIENT`; não acertou →
    `false` e `wispborn.ability.no_target` na barra de ação.

- [ ] **Step 1: Write the failing tests** — `AbilityPayloadsTest`: ida e volta dos três payloads.
  `AbilityEffectsTest.everyShippedActiveHasAnEffect`: os ids `ACTIVE` dos JSON reais estão todos em `AbilityEffects.KNOWN`.
- [ ] **Step 2: Run them and confirm they fail** — `./gradlew test --tests dev.eder.wispborn.network.AbilityPayloadsTest --tests dev.eder.wispborn.ability.AbilityEffectsTest`.
- [ ] **Step 3: Implement.** `use`: `checkUse` com o grau liberado, `HostManager.isHost`, grau de `wispborn:take_over`
  ≥ 1, mana e `gameTime`; falha → `wispborn.ability.<resultado>` na barra de ação; OK → `effect.apply`; se `true`,
  desconta `mana`, grava `Cooldown(gameTime + cooldown_ticks, cooldown_ticks)` e sincroniza.

  Lang (pt_br / en_us): `wispborn.ability.empty_slot` Espaço vazio / Empty slot; `.unknown` Habilidade desconhecida /
  Unknown ability; `.not_unlocked` Ainda não liberada / Not unlocked yet; `.not_active` Não é ativa / Not an active
  ability; `.blocked_in_host` Libere Tomar conta do corpo para usar aqui / Unlock Take Over to use it here;
  `.on_cooldown` Recarregando / On cooldown; `.no_mana` Mana insuficiente / Not enough mana; `.no_target` Nenhum
  alvo / No target; `.bad_slot` Espaço inválido / Invalid slot.
- [ ] **Step 4: Build** (`./gradlew build`, verde).
- [ ] **Step 5: Commit** — `feat(ability): usar e equipar ativas no servidor, com a Teia`.

---

### Task 3: Pular de corpo

**Files:**
- Create: `ability/BodyJumpEffect.java`
- Modify: `host/HostManager.java`, `ability/AbilityEffects.java`, lang
- Test: `src/test/java/dev/eder/wispborn/host/HostTransferRulesTest.java`, `host/HostTransferRules.java` (novo)

**Interfaces:**
- Produces:
  - `HostManager.enterHost(ServerPlayer, Mob)`: o trecho de sucesso que hoje fica no fim do `tick` (forma, invisível,
    montar, `DRAIN_TICKS`, furtividade, som, partículas, mensagem), extraído sem mudar o comportamento; o `tick`
    passa a chamá-lo.
  - `HostManager.Reason.JUMP` (sai do hospedeiro antigo sem teleportar e sem a mensagem de saída)
  - `HostTransferRules.check(boolean inHost, boolean targetIsCurrentHost, InfestResult targetResult): InfestResult`:
    fora do hospedeiro → `NOT_A_MOB`; alvo = hospedeiro atual → `NOT_A_MOB`; senão `targetResult`
  - `BodyJumpEffect`: só funciona dentro do hospedeiro; alvo pelo raio de `range` a partir do olho do hospedeiro;
    `InfestRules.check` com `mana = minMana` (pular é grátis, spec §5); resultado de `HostTransferRules.check`
    diferente de OK → mensagem da infestação (`sendInfestFailure`, que passa a ser package-private) e `false`;
    OK → `release(player, JUMP)`, `enterHost(player, alvo)`, partículas `END_ROD` entre os dois mobs, `true`.

- [ ] **Step 1: Write the failing test** — `HostTransferRulesTest`: fora do hospedeiro → `NOT_A_MOB`; alvo é o
  atual → `NOT_A_MOB`; `TOO_STRONG` e `MISSING_ADAPTATION` passam adiante; `OK` → `OK`.
- [ ] **Step 2: Run it and confirm it fails** — `./gradlew test --tests dev.eder.wispborn.host.HostTransferRulesTest`.
- [ ] **Step 3: Implement**, primeiro a extração do `enterHost` (rode `./gradlew build` e confira a possessão no
  jogo antes de seguir), depois o efeito.
- [ ] **Step 4: Check in game:** com Tomar conta do corpo e Pular de corpo liberados e equipados, dentro de uma
  vaca, olhar para uma ovelha a até 6 blocos e apertar a tecla: o wisp passa para a ovelha na hora, sem
  conjuração e sem gastar mana, e fica 20 s em recarga. Olhar para um mob forte demais mostra a mensagem e não
  gasta a recarga.
- [ ] **Step 5: Commit** — `feat(ability): Pular de corpo transfere o wisp sem conjuracao`.

---

### Task 4: Adaptações e dano de ambiente

**Files:**
- Create: `ability/AdaptationRules.java`, `ability/ExposureManager.java`, `data/wispborn/damage_type/exposure.json`,
  `registry/ModDamageTypes.java` (mod.: `EXPOSURE`)
- Modify: `config/WispbornConfig.java`, `event/AbilityEvents.java`, lang
- Test: `src/test/java/dev/eder/wispborn/ability/AdaptationRulesTest.java`

**Interfaces:**
- Produces:
  - `AdaptationRules.requiredHeat(boolean ultraWarmDimension, float biomeTemperature, double heatThreshold, int maxHeatDegree): int`
    → `maxHeatDegree` no Nether (`dimensionType().ultraWarm()`); 1 com `temperatura >= heatThreshold`; senão 0
  - `AdaptationRules.requiredCold(float biomeTemperature, double coldThreshold1, double coldThreshold2): int`
    → 2 com `temperatura < coldThreshold2`; 1 com `< coldThreshold1`; senão 0
  - `AdaptationRules.exposed(int required, int owned): boolean` = `owned < required`
  - `ExposureManager.tick(ServerPlayer)`: a cada `environment.intervalTicks`, **só na forma wisp** (dentro do
    hospedeiro nada acontece, spec §5.6), lê `biome.value().getBaseTemperature()` na posição do jogador; se
    exposto ao calor ou ao frio, aplica `environment.damage` com o tipo `wispborn:exposure` e mostra
    `wispborn.exposure.heat`/`.cold` com o nome e o grau que faltam. `maxHeatDegree` = número de graus de
    `wispborn:heat` no `WispData`.
- Config: `environment.heatThreshold` (double 2.0), `environment.coldThreshold1` (0.15), `environment.coldThreshold2`
  (-0.5), `environment.damage` (1.0), `environment.intervalTicks` (int 40).
- `exposure.json`: `{"message_id": "wispborn.exposure", "exhaustion": 0.0, "scaling": "never"}`.

- [ ] **Step 1: Write the failing test**

```java
@Test void netherAlwaysNeedsTheHighestHeatDegree() { assertEquals(2, AdaptationRules.requiredHeat(true, 2.0F, 2.0D, 2)); }
@Test void hotBiomesNeedHeatOne() {
    assertEquals(1, AdaptationRules.requiredHeat(false, 2.0F, 2.0D, 2));   // deserto
    assertEquals(0, AdaptationRules.requiredHeat(false, 0.8F, 2.0D, 2));   // planície
}
@Test void coldHasTwoBands() {
    assertEquals(0, AdaptationRules.requiredCold(0.8F, 0.15D, -0.5D));
    assertEquals(1, AdaptationRules.requiredCold(0.0F, 0.15D, -0.5D));     // planície nevada
    assertEquals(2, AdaptationRules.requiredCold(-0.7F, 0.15D, -0.5D));    // picos congelados
}
@Test void ownedDegreeCoversLowerRequirements() {
    assertFalse(AdaptationRules.exposed(1, 2));
    assertTrue(AdaptationRules.exposed(2, 1));
}
```

- [ ] **Step 2: Run it and confirm it fails** — `./gradlew test --tests dev.eder.wispborn.ability.AdaptationRulesTest`.
- [ ] **Step 3: Implement.** Lang: `wispborn.exposure.heat` Calor demais: exige %s / Too hot: requires %s;
  `wispborn.exposure.cold` Frio demais: exige %s / Too cold: requires %s; `death.attack.wispborn.exposure`
  %1$s não resistiu ao ambiente / %1$s succumbed to the environment.
- [ ] **Step 4: Check in game:** como wisp sem Calor no deserto, 1 de dano a cada 2 s, com aviso. Possuindo um
  husk, nenhum dano. Com Calor I liberado, nada. No Nether com Calor I, dano de novo, pedindo Calor II.
- [ ] **Step 5: Commit** — `feat(ability): adaptacoes ao calor e ao frio com dano de ambiente`.

---

### Task 5: Teclas e espaços no HUD

**Files:**
- Create: `client/ClientAbilities.java`, sprites `hud/hud_ability_slot.png`, `hud/hud_ability_cooldown.png`,
  `ability/<path>.png` para `web`, `heat`, `cold`, `take_over`, `body_jump` (16×16)
- Modify: `client/KeyBindings.java`, `client/ClientPayloadHandler.java`, `client/hud/WispHud.java`,
  `client/hud/HudMath.java`, `client/hud/HudSprites.java`, lang
- Test: `client/hud/HudMathTest.java`

**Interfaces:**
- Produces:
  - `KeyBindings.ABILITY_1/2/3` (padrões `GLFW_KEY_Z`, `GLFW_KEY_X`, `GLFW_KEY_G`; lang `key.wispborn.ability_1..3`
    Habilidade 1..3 / Ability 1..3); no tick do cliente, com `mc.screen == null` e forma parasita, `consumeClick` →
    `UseAbilityPayload(i)`
  - `ClientAbilities.get(): AbilityState` (reset no logout)
  - `HudMath.cooldownHeight(long gameTime, long readyAt, int total, int fullHeight): int`: 0 pronto; senão
    `ceil(fullHeight × restante / total)`, limitado a `fullHeight`
  - `HudSprites.ABILITY_SLOT`, `ABILITY_COOLDOWN`, `HudSprites.abilityIcon(ResourceLocation id): ResourceLocation`
    (`wispborn:ability/<path>`)
- Layout: três espaços de 20×20 em `x = cx - 100 - 6 - 64 + i × 22` (i = 0..2) e `y = h - 28 - 10 × (healthRows - 1)`,
  à esquerda das linhas de vida e DNA. Ícone 16×16 em `(+2, +2)`. Recarga: `ABILITY_COOLDOWN` recortado de cima
  para baixo com a altura de `cooldownHeight`. Sem mana para usar: o ícone com `RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, 1.0F)`
  (e volta a 1 depois). A tecla (`getTranslatedKeyMessage`, primeiro caractere) no canto inferior direito, com sombra.
  Bloqueado dentro do hospedeiro sem Tomar conta do corpo: mesmo escurecimento.
- Sprites: da A5 ou placeholders (`hud_ability_slot` de `grimoire/button`, `hud_ability_cooldown` de
  `hud/cast_vignette`, ícones de `hud/dna_icon`).

- [ ] **Step 1: Write the failing test** — `cooldownShrinksToZero`: `(100, 100, 160, 20)` → 0; `(20, 100, 160, 20)` → 10;
  `(99, 100, 160, 20)` → 1; `(0, 1000, 160, 20)` → 20.
- [ ] **Step 2: Run it and confirm it fails**, **implement**, **run again** (PASS).
- [ ] **Step 3: Check in game:** com Teia equipada no espaço 1, Z num mob lança a teia, deixa o mob lento e mostra a
  sombra de recarga descendo; com o chat aberto, Z não faz nada.
- [ ] **Step 4: Commit** — `feat(hud): espacos de ativas com tecla e recarga`.

---

### Task 6: Aba Habilidades

**Files:**
- Create: `client/grimoire/AbilitiesPage.java`, sprites `grimoire/ability_slot.png`, `ability_slot_selected.png`,
  `ability_slot_empty.png` (22×22), `ability_row.png` + `ability_row_selected.png` (nine-slice, 100×18)
- Modify: `client/grimoire/GrimoireTab.java` (ABILITIES com página), `GrimoireSprites.java`, lang

Layout (relativo ao painel):
- Espaços equipados no topo à direita: três `ability_slot` 22×22 em x 116, 142, 168, y 24, com ícone 16×16 e a
  tecla embaixo à direita. O espaço selecionado usa `ability_slot_selected`; um vazio, `ability_slot_empty`.
  Clique esquerdo seleciona; clique direito → `EquipAbilityPayload(i, Optional.empty())`.
- Lista à esquerda, x 8..108, y 24 em diante: as ativas liberadas (`ability_row`, 18 px: ícone + nome com o grau),
  rolagem com a roda. Clique numa linha equipa a habilidade no espaço selecionado (`EquipAbilityPayload`).
- Painel direito, y 52 em diante: `wispborn.abilities.passives` e uma linha por passiva ou adaptação liberada
  (ícone + nome + pips de grau `pip_filled`/`pip_empty` até o total de graus), só leitura.
- Rodapé, y 181: a habilidade sob o mouse (ou a do espaço selecionado) com `wispborn.abilities.cost`
  (`%s mana · %s s`), a partir do `params` do grau liberado.
- Sem nenhuma ativa liberada: `wispborn.abilities.none` na lista.

Lang: `wispborn.abilities.passives` Passivas / Passives; `.cost` %s mana · %s s / %s mana · %s s; `.none`
Libere ativas no Bestiário / Unlock actives in the Bestiary.

- [ ] **Step 1: Implement** a página e os sprites (da A5 ou placeholders de `button`, `button_hover` e `pip_empty`).
- [ ] **Step 2: Build** (`./gradlew build`, verde).
- [ ] **Step 3: Check in game:** equipar Teia no espaço 2, trocar para o 3 (o 2 esvazia), clique direito no 3
  esvazia; as passivas aparecem com os graus; o HUD acompanha na hora.
- [ ] **Step 4: Commit** — `feat(grimoire): aba Habilidades para equipar ativas`.

---

### Task 7: Pilotar o hospedeiro (Tomar conta do corpo)

Decisão do Claude para o "o que Tomar conta do corpo controla" (spec, "Deixado para quem implementa"): **andar**.
Atacar fica com as ativas. "Primeiro passageiro, depois piloto" (spec §5.5).

**Files:**
- Create: `mixin/HostControlMixin.java`, `host/HostControl.java`
- Modify: `src/main/resources/wispborn.mixins.json`

**Interfaces:**
- Produces:
  - `HostControl.controller(Mob mob): Optional<Player>`: o primeiro passageiro, se for um jogador na forma HOST
    com grau ≥ 1 de `wispborn:take_over`. No servidor o grau vem do attachment; no cliente, de `ClientAssimilation`
    para o jogador local (outros jogadores: vazio).
  - `HostControlMixin` em `Mob`:
    - `getControllingPassenger` devolve `HostControl.controller(this)` quando presente;
    - `getRiddenInput(Player, Vec3)` devolve `(player.xxa × 0.5, 0, player.zza)` como o cavalo;
    - `getRiddenSpeed(Player)` devolve o atributo `MOVEMENT_SPEED` do mob;
    - `tickRidden(Player, Vec3)` copia o `yRot` do jogador para o mob (corpo e cabeça).
  
  Confira os nomes e as assinaturas em `LivingEntity`/`Mob` no 1.21.1 com Parchment antes de escrever o mixin.

- [ ] **Step 1: Implement** e rode `./gradlew build`.
- [ ] **Step 2: Check in game:** sem Tomar conta do corpo, a vaca possuída anda sozinha (como hoje). Depois de
  liberar, WASD conduz a vaca para onde a câmera olha. A drenagem, o consumo e o Shift para sair continuam
  funcionando. Um segundo jogador vê a vaca andar sem tremer.
- [ ] **Step 3:** Se a IA do mob brigar com o controle ou o movimento tremer e não houver correção simples, **pare**:
  reverta o task, anote em "Dúvidas do implementador" e entregue a fatia sem pilotar. As ativas dentro do
  hospedeiro já são o efeito de Tomar conta do corpo dos Tasks 1 e 2.
- [ ] **Step 4: Commit** — `feat(host): Tomar conta do corpo permite conduzir o hospedeiro`.

---

### Task 8: Documentação

- [ ] `docs/specs/F8-habilidades.md` (aponta para este plano; dúvidas e relatório, com os placeholders de sprite e o
  resultado do Task 7), `CHANGELOG.md`, linha F8 do `docs/PLAN.md`, pacote `ability/` na árvore do `AGENTS.md`.
- [ ] `./gradlew build` verde, e commit: `docs(F8): relatorio das habilidades, CHANGELOG e PLAN`.
