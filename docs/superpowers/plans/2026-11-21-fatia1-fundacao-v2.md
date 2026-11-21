# Fatia 1 — Fundação v2 (F6) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** EP vira moeda (saldo gasto para subir de nível, com nuvem de DNA recuperável na morte), e a mana vira o
sistema imune do hospedeiro (entrada grátis com mana mínima, gasto contínuo lá dentro, expulsão ao zerar,
regeneração em qualquer forma menos dentro do hospedeiro), com HUD e aba Wisp mostrando isso.

**Architecture:** Regras novas em funções puras testadas (`LevelCurve`, `LevelRules`, `WispProgress`,
`ImmuneMath`, `DnaCloudRules`, `HudMath`); `ProgressManager`, `HostManager` e o novo `DnaCloudManager` só ligam
essas regras ao jogo. Estado em Data Attachments (`PROGRESS` muda de formato com migração no codec; `DNA_CLOUD`
novo), sincronizado pelo `SyncWispStatePayload` ampliado. Cliente só desenha e envia a intenção `LevelUpPayload`.

**Tech Stack:** Minecraft 1.21.1, NeoForge 21.1.x, Java 21, Mojang mappings + Parchment, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-20-fagogenia-design.md` (seções 3, 4, 8 e "Mudanças na fundação",
linhas da fatia 1). Arte: `docs/prompts/A5-fagogenia-art.md` (linhas da fatia 1 nas tabelas de entregas).

**Branch:** `feat/F6-fundacao-v2`, criada da `main` **depois** do merge da F5 (ver Pré-requisitos).

## Global Constraints

- Stack fixa: Minecraft 1.21.1, NeoForge 21.1.x, Java 21, GeckoLib 4.9.3. modid `wispborn`, pacote `dev.eder.wispborn`.
- Servidor decide tudo; o cliente só renderiza e envia intenções.
- Regras de jogo em funções puras testadas com JUnit; managers só ligam ao jogo.
- Números de balanceamento em `WispbornConfig` (servidor) ou `WispbornClientConfig` (cliente); nada no código.
- Texto visível só via lang, sempre nos dois arquivos (`en_us.json` e `pt_br.json`).
- HUD e grimório só com `blitSprite` e a fonte do jogo, paleta B, nenhum `fill`.
- Comentários e commits em português; identificadores em inglês.
- Antes de entregar: `./gradlew build` e `./gradlew test` verdes.

## Review Focus

1. **Save antigo** (`{"total_ep": N}`, com ou sem `level`) carrega sem perder EP: nível pela curva antiga, o
   resto vira saldo. Teste no Task 1.
2. **Curva encurtada no config** depois de o jogador passar do novo máximo: o nível exibido e usado é limitado
   ao máximo da curva, e os atributos excedentes são zerados como hoje (`validateStats`). Teste no Task 1.
3. **Saldo perto de `Integer.MAX_VALUE`** (comando, nuvem somada ao saldo): soma satura, nunca estoura para
   negativo. Teste no Task 1.
4. **Morte no vazio ou abaixo do mundo:** a nuvem fica dentro da altura do mundo, onde dá para alcançar. Teste
   no Task 5.
5. **Morte com saldo zero, com uma nuvem antiga no mundo:** a nuvem antiga some e nenhuma nova aparece. Teste
   no Task 5.

---

## Pré-requisitos (antes de criar a branch)

### Task 0: Corrigir o estouro de `int` em `AllocationRules` (na branch `feat/F5-hud-b-grimorio`)

**Files:**
- Modify: `src/main/java/dev/eder/wispborn/progress/AllocationRules.java`
- Test: `src/test/java/dev/eder/wispborn/progress/AllocationRulesTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void rejectsHugeDeltaThatWouldOverflow() {
    // Cliente modificado manda VAR_INT enormes: a soma dá a volta e ficaria <= pontos disponíveis.
    StatDelta delta = new StatDelta(Integer.MAX_VALUE, Integer.MAX_VALUE, 2, 0);
    assertEquals(BatchAllocateResult.OVER_MAX, AllocationRules.check(4, WispStats.ZERO, delta, 10));
}
```

- [ ] **Step 2: Run it and confirm it fails**

Run: `./gradlew test --tests dev.eder.wispborn.progress.AllocationRulesTest`
Expected: FAIL (o resultado atual não é `OVER_MAX`).

- [ ] **Step 3: Fix `check`**: logo depois do laço que rejeita negativos, um laço que devolve `OVER_MAX` se
  qualquer `delta.get(stat) > maxPerStat`. Só depois soma `total()` e compara `current + delta`.

- [ ] **Step 4: Run the tests and confirm they pass** (mesmo comando, todos os testes da classe passam).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/eder/wispborn/progress/AllocationRules.java src/test/java/dev/eder/wispborn/progress/AllocationRulesTest.java
git commit -m "fix(progress): rejeitar delta acima do maximo antes de somar (estouro de int)"
```

Depois: Eder roda o checklist da F5 no jogo e aprova o merge F4 → F5 → `main`. Só então:
`git switch main && git pull && git switch -c feat/F6-fundacao-v2`.

---

## Arquivos

| Arquivo | Responsabilidade | Task |
|---|---|---|
| `progress/LevelCurve.java` (mod.) | preço do próximo nível, nível máximo, limitação do nível | 1 |
| `progress/WispProgress.java` (mod.) | `(level, ep)` + migração do save antigo + soma saturada | 1 |
| `progress/LevelRules.java`, `progress/LevelUpResult.java` (novos) | pode comprar o nível? | 1 |
| `progress/ProgressManager.java` (mod.) | saldo, compra de nível, estado sincronizado | 2 |
| `network/LevelUpPayload.java` (novo), `network/SyncWispStatePayload.java` (mod.), `network/ModPayloads.java` (mod.) | intenção e estado | 2, 4, 6 |
| `command/WispbornCommand.java` (mod.) | `ep add/set` sobre o saldo, `level set` novo | 2 |
| `client/hud/HudMath.java`, `client/hud/WispHud.java`, `client/hud/HudSprites.java` (mod.) | barra de DNA, mana piscando, marcador da nuvem | 2, 4, 6 |
| `client/grimoire/GrimoireScreen.java`, `GrimoireSprites.java` (mod.) | abas novas, botão Subir nível | 3 |
| `host/ImmuneMath.java` (novo), `host/InfestRules.java`, `host/HostManager.java` (mod.) | mana como sistema imune | 4 |
| `event/FormEvents.java` (mod.) | regeneração/gasto de mana por segundo | 4 |
| `progress/DnaCloud.java`, `progress/DnaCloudRules.java`, `progress/DnaCloudManager.java`, `event/ProgressEvents.java` (novos) | nuvem de DNA | 5 |
| `network/DnaCloudMarker.java` (novo) | nuvem vista pelo cliente | 6 |
| `config/WispbornConfig.java`, `config/WispbornClientConfig.java`, `registry/ModAttachments.java`, `WispbornMod.java` (mod.) | config, attachment, registro de eventos | 2, 4, 5 |
| `assets/wispborn/lang/en_us.json`, `pt_br.json` (mod.) | textos | 2–6 |
| `textures/gui/sprites/hud/*`, `grimoire/*` (novos) | sprites da fatia 1 na A5 | 2, 3, 4, 6 |

**Sprites:** cada task que usa sprite novo copia o PNG (e o `.png.mcmeta`) de `art/hud/sprites/` ou
`art/hud/grimoire/` para `src/main/resources/assets/wispborn/textures/gui/sprites/hud/` ou `.../grimoire/`. Se
a A5 ainda não tiver sido entregue, cria um **placeholder** copiando o sprite indicado na task, e anota no
relatório para trocar depois.

---

### Task 1: Regras puras do EP como moeda

**Files:**
- Modify: `src/main/java/dev/eder/wispborn/progress/LevelCurve.java`, `WispProgress.java`
- Create: `src/main/java/dev/eder/wispborn/progress/LevelRules.java`, `LevelUpResult.java`
- Test: `src/test/java/dev/eder/wispborn/progress/LevelCurveTest.java`, `WispProgressTest.java`, `LevelRulesTest.java` (novo)

**Interfaces:**
- Produces:
  - `LevelCurve.levelFor(int totalEp, List<Integer> thresholds): int` (fica, usado na migração)
  - `LevelCurve.maxLevel(List<Integer> thresholds): int` = `thresholds.size() + 1`
  - `LevelCurve.clampLevel(int level, List<Integer> thresholds): int` entre 1 e `maxLevel`
  - `LevelCurve.priceToNext(int level, List<Integer> thresholds): OptionalInt`: vazio no nível máximo ou acima;
    senão `thresholds[level-1] - (level >= 2 ? thresholds[level-2] : 0)`
  - Saem `nextThreshold` e `levelStartEp`, e os testes deles.
  - `record WispProgress(int level, int ep)`: o construtor compacto força `level >= 1` e `ep >= 0`;
    `START = new WispProgress(1, 0)`; `withEpAdded(int amount): WispProgress` (soma saturada em
    `Integer.MAX_VALUE`; `amount <= 0` devolve `this`); `withEp(int ep)`; `withLevel(int level)`;
    `afterLevelUp(int price): WispProgress` = `(level + 1, ep - price)`.
  - `WispProgress.LEGACY_THRESHOLDS: List<Integer>`: cópia congelada da curva padrão de hoje
    (`300, 800, 1600, 2800, 4500, 6800, 9800, 13600, 18300, 24000, 30800, 38800, 48100, 58800, 71000, 84800,
    100300, 117600, 136800`). Comentário: só a migração usa, e ela não segue o config.
  - `WispProgress.CODEC`: grava `{"level": L, "ep": E}`; lê esse formato ou o antigo `{"total_ep": N}` (o
    campo `level` antigo é ignorado). Antigo → `level = levelFor(N, LEGACY_THRESHOLDS)`,
    `ep = N - limiar do nível` (0 no nível 1). Use `Codec.withAlternative(CURRENT, LEGACY)`; se a DFU do 1.21.1
    não tiver, `Codec.either(CURRENT, LEGACY).xmap(Either::unwrap, Either::left)`.
  - `enum LevelUpResult { OK, MAX_LEVEL, NOT_ENOUGH_EP }`
  - `LevelRules.check(int level, int ep, List<Integer> thresholds): LevelUpResult`

- [ ] **Step 1: Write the failing tests**

```java
// LevelCurveTest (DEFAULT_THRESHOLDS já existe na classe)
@Test void priceIsTheGapBetweenThresholds() {
    assertEquals(OptionalInt.of(300), LevelCurve.priceToNext(1, DEFAULT_THRESHOLDS));
    assertEquals(OptionalInt.of(500), LevelCurve.priceToNext(2, DEFAULT_THRESHOLDS));
    assertEquals(OptionalInt.of(800), LevelCurve.priceToNext(3, DEFAULT_THRESHOLDS));
    assertEquals(OptionalInt.of(19_200), LevelCurve.priceToNext(19, DEFAULT_THRESHOLDS));
    assertEquals(OptionalInt.empty(), LevelCurve.priceToNext(20, DEFAULT_THRESHOLDS));
    assertEquals(OptionalInt.empty(), LevelCurve.priceToNext(25, DEFAULT_THRESHOLDS));
    assertEquals(OptionalInt.empty(), LevelCurve.priceToNext(1, List.of()));
}
@Test void levelIsClampedToTheCurrentCurve() {
    assertEquals(20, LevelCurve.maxLevel(DEFAULT_THRESHOLDS));
    assertEquals(3, LevelCurve.clampLevel(7, List.of(100, 200)));   // curva encurtada no config
    assertEquals(1, LevelCurve.clampLevel(0, DEFAULT_THRESHOLDS));
    assertEquals(1, LevelCurve.clampLevel(5, List.of()));
}

// WispProgressTest (substitui os três testes atuais)
@Test void codecRoundTrip() { /* new WispProgress(3, 200) codifica e decodifica igual */ }
@Test void encodesInTheNewFormat() { /* JSON gerado tem "level": 3 e "ep": 200 e não tem "total_ep" */ }
@Test void legacySaveMigratesToLevelAndBalance() {
    assertEquals(new WispProgress(3, 200), parse("{\"total_ep\": 1000}"));
    assertEquals(new WispProgress(2, 150), parse("{\"total_ep\": 450, \"level\": 2}"));
    assertEquals(new WispProgress(1, 299), parse("{\"total_ep\": 299}"));
    assertEquals(new WispProgress(20, 63_200), parse("{\"total_ep\": 200000}"));
}
@Test void epAddSaturatesInsteadOfOverflowing() {
    assertEquals(Integer.MAX_VALUE, new WispProgress(1, Integer.MAX_VALUE - 5).withEpAdded(100).ep());
    assertEquals(new WispProgress(1, 10), new WispProgress(1, 10).withEpAdded(-50));
}
@Test void levelUpSpendsThePrice() {
    assertEquals(new WispProgress(3, 40), new WispProgress(2, 540).afterLevelUp(500));
}

// LevelRulesTest (novo)
@Test void buyingNeedsTheFullPriceAndAMissingLevel() {
    assertEquals(LevelUpResult.OK, LevelRules.check(1, 300, DEFAULT));
    assertEquals(LevelUpResult.NOT_ENOUGH_EP, LevelRules.check(1, 299, DEFAULT));
    assertEquals(LevelUpResult.OK, LevelRules.check(2, 9_999, DEFAULT));
    assertEquals(LevelUpResult.MAX_LEVEL, LevelRules.check(20, Integer.MAX_VALUE, DEFAULT));
    assertEquals(LevelUpResult.MAX_LEVEL, LevelRules.check(1, 0, List.of()));
}
```

`parse` é um helper privado do teste: `WispProgress.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow()`.
Atualize `ConsumeRulesTest` só se ele parar de compilar (ele usa `levelFor`, que fica).

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew test --tests "dev.eder.wispborn.progress.*"`
Expected: FAIL de compilação (`priceToNext`, `LevelRules`, construtor de dois campos não existem).

- [ ] **Step 3: Implement** as assinaturas do bloco Interfaces. Nesta task o projeto ainda não compila fora de
  `progress/` (os chamadores usam `totalEp()`); isso se resolve no Task 2. Para rodar só esta task, faça os
  Tasks 1 e 2 no mesmo ciclo de build, com commits separados.

- [ ] **Step 4: Run them and confirm they pass** (mesmo comando, depois do Task 2 compilar).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/eder/wispborn/progress/ src/test/java/dev/eder/wispborn/progress/
git commit -m "feat(progress): EP como saldo, preco do nivel e migracao do save antigo"
```

---

### Task 2: Saldo de EP ligado ao jogo (servidor, payloads, comandos, barra de DNA)

**Files:**
- Modify: `progress/ProgressManager.java`, `network/SyncWispStatePayload.java`, `network/ModPayloads.java`,
  `command/WispbornCommand.java`, `client/hud/HudMath.java`, `client/hud/WispHud.java`,
  `client/hud/HudSprites.java`, `client/grimoire/GrimoireScreen.java` (só `renderBars`), lang
- Create: `network/LevelUpPayload.java`, sprite `hud/dna_bar_ready.png` + `.mcmeta`
- Test: `src/test/java/dev/eder/wispborn/network/SyncWispStatePayloadTest.java` (novo), `client/hud/HudMathTest.java`

**Interfaces:**
- Consumes: tudo do Task 1.
- Produces:
  - `ProgressManager.level(ServerPlayer): int` = `LevelCurve.clampLevel(progress.level(), thresholds)`
  - `ProgressManager.epBalance(ServerPlayer): int`
  - `ProgressManager.addEp(ServerPlayer, int baseEp): int` soma ao saldo (multiplicador da Eficiência como
    hoje), **sem** subir nível nem mensagem de nível
  - `ProgressManager.setEpBalance(ServerPlayer, int)` (substitui `setEp`)
  - `ProgressManager.setLevel(ServerPlayer, int)`: limita com `clampLevel`, chama `validateStats` e `refreshStats`
  - `ProgressManager.buyLevel(ServerPlayer): LevelUpResult`: exige `FormManager.isParasite`; em `OK` grava
    `afterLevelUp(price)`, chama `refreshStats`, manda a mensagem `wispborn.level_up` e o som
    `PLAYER_LEVELUP` (os que hoje estão em `addEp`); em falha, mensagem na barra de ação e `syncState(player, true)`
  - `SyncWispStatePayload`: troca `totalEp, levelStartEp, nextLevelEp` por `int epBalance, int levelPrice`
    (`-1` no nível máximo). Ordem dos campos: `level, epBalance, levelPrice, mana, maxMana, availablePoints,
    stats, maxPerStat, hostConsumeReady, hostEpFactor`. `DEFAULT` = `(1, 0, 300, 50, 50, 0, ZERO, 10, false, 1.0F)`.
  - `record LevelUpPayload()` com `INSTANCE`, `TYPE` = `wispborn:level_up`, `STREAM_CODEC = StreamCodec.unit(INSTANCE)`;
    registrado em `ModPayloads` como `playToServer` chamando `ProgressManager.buyLevel`.
  - `HudMath.levelUpReady(int balance, int price): boolean` = `price >= 0 && balance >= price`
  - `HudMath.dnaFill(int balance, int price, int fullWidth): int`: `fullWidth` se `price < 0`; senão
    `progressWidth(min(balance, price), price, fullWidth)`
  - `HudSprites.DNA_BAR_READY` = `hud("dna_bar_ready")`

- [ ] **Step 1: Write the failing tests**

```java
// HudMathTest
@Test void levelUpIsReadyOnlyWhenTheBalancePays() {
    assertTrue(HudMath.levelUpReady(500, 500));
    assertFalse(HudMath.levelUpReady(499, 500));
    assertFalse(HudMath.levelUpReady(99_999, -1));
}
@Test void dnaBarFillsTowardThePrice() {
    assertEquals(100, HudMath.dnaFill(150, 300, 200));
    assertEquals(200, HudMath.dnaFill(9_000, 300, 200));
    assertEquals(200, HudMath.dnaFill(0, -1, 200));
    assertEquals(0, HudMath.dnaFill(0, 300, 200));
}

// SyncWispStatePayloadTest (novo)
@Test void streamCodecRoundTrip() {
    // um estado com level 7, epBalance 1240, levelPrice 3000 e um com levelPrice -1:
    // encode + decode devolve igual (mesmo padrão de PossessionPayloadsTest)
}
@Test void levelUpPayloadRoundTrip() { /* encode + decode de LevelUpPayload.INSTANCE devolve INSTANCE */ }
```

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew test --tests dev.eder.wispborn.client.hud.HudMathTest --tests dev.eder.wispborn.network.SyncWispStatePayloadTest`
Expected: FAIL de compilação.

- [ ] **Step 3: Implement** o bloco Interfaces e ajuste os chamadores:
  - `ProgressManager.computeState`: `levelPrice = LevelCurve.priceToNext(level, thresholds).orElse(-1)`.
    `epDisplay` vira `saldo/preço` ou `wispborn.hud.max` com o saldo.
  - `WispHud.renderWispDna`: `fill = HudMath.dnaFill(...)`; se `levelUpReady`, desenha `DNA_BAR_READY` na
    largura toda no lugar do progresso. O número acima do orbe passa a ser `epBalance`.
  - `GrimoireScreen.renderBars`: o rótulo da direita vira `wispborn.grimoire.ep_balance` com o saldo (ou
    `ep_max` no nível máximo); a barra de EP usa `HudMath.dnaFill` com `EP_BAR_WIDTH`. O sprite pronto do
    grimório entra no Task 3.
  - Comandos: `ep add` soma ao saldo, `ep set` define o saldo, `level set <n> [player]` novo
    (`IntegerArgumentType.integer(1)`); `info` mostra `Nv`, saldo e preço via `epDisplay`.
  - Sprite `hud/dna_bar_ready`: da A5 ou placeholder copiado de `hud/dna_bar_progress.png` (+ `.mcmeta`).

  Lang (os dois arquivos):

  | Chave | pt_br | en_us |
  |---|---|---|
  | `wispborn.grimoire.ep_balance` (nova) | `%s EP` | `%s EP` |
  | `wispborn.grimoire.ep` (remover) | — | — |
  | `wispborn.levelup.not_enough_ep` (nova) | `EP insuficiente: %s/%s` | `Not enough EP: %s/%s` |
  | `wispborn.levelup.max_level` (nova) | `Você já está no nível máximo.` | `You are already at max level.` |
  | `commands.wispborn.ep.add.success` | `+%s EP para %s (Saldo: %s, Nível: %s)` | `Added %s EP to %s (Balance: %s, Level: %s)` |
  | `commands.wispborn.ep.set.success` | `Saldo de EP de %s definido para %s (Nível: %s)` | `Set %s EP balance to %s (Level: %s)` |
  | `commands.wispborn.level.set.success` (nova) | `Nível de %s definido para %s` | `Set %s level to %s` |

- [ ] **Step 4: Run the full build and tests**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, todos os testes passam (inclusive os do Task 1).

- [ ] **Step 5: Commit**

```bash
git add -A src/
git commit -m "feat(progress): saldo de EP, compra de nivel no servidor e barra de DNA como saldo"
```

---

### Task 3: Aba Wisp — abas novas e botão "Subir nível"

**Files:**
- Modify: `client/grimoire/GrimoireScreen.java`, `client/grimoire/GrimoireSprites.java`, lang
- Create: sprite `grimoire/bar_ep_ready.png`

**Interfaces:**
- Consumes: `SyncWispStatePayload.epBalance()/levelPrice()`, `LevelUpPayload.INSTANCE`, `HudMath.levelUpReady`, `HudMath.dnaFill`.
- Produces: `GrimoireSprites.BAR_EP_READY`.

Layout (coordenadas relativas a `left`/`top` do painel 316×200):
- `TAB_KEYS` = `wispborn.grimoire.tab.wisp`, `.bestiary`, `.abilities`, `.appearance`, `.body`; só a primeira ativa.
- Retrato: a área do modelo passa a ser `(12, 28)` a `(104, 166)`; a moldura `PORTRAIT_FRAME` continua igual.
- Botão `LEVEL_UP_BTN` 92×18 em `(12, 170)`: `BUTTON_PRIMARY` / `_HOVER` quando `levelUpReady`, senão
  `BUTTON_PRIMARY_DISABLED`. Texto centralizado (`ellipsize` em 86 px): `wispborn.grimoire.level_up` com o
  preço, ou `wispborn.grimoire.level_max` quando `levelPrice < 0`.
- Clique esquerdo no botão com `levelUpReady` → `PacketDistributor.sendToServer(LevelUpPayload.INSTANCE)` +
  `playClickSound()`. Sem `levelUpReady`, o clique é consumido e não faz nada.
- Barra de EP: `BAR_EP_READY` no lugar de `BAR_EP` quando `levelUpReady`.

- [ ] **Step 1: Implement** o layout acima. Sprite `grimoire/bar_ep_ready`: da A5 ou placeholder copiado de
  `grimoire/bar_ep.png`.

  Lang:

  | Chave | pt_br | en_us |
  |---|---|---|
  | `wispborn.grimoire.tab.bestiary` | `Bestiário` | `Bestiary` |
  | `wispborn.grimoire.tab.abilities` (nova) | `Habilidades` | `Abilities` |
  | `wispborn.grimoire.tab.appearance` (nova) | `Aparência` | `Appearance` |
  | `wispborn.grimoire.tab.body` (nova) | `Corpo` | `Body` |
  | `wispborn.grimoire.tab.traits`, `.aspects`, `.skills` | remover | remover |
  | `wispborn.grimoire.level_up` (nova) | `Subir · %s EP` | `Level up · %s EP` |
  | `wispborn.grimoire.level_max` (nova) | `Nível máximo` | `Max level` |

- [ ] **Step 2: Build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Check in game** (`./gradlew runClient`, mundo de teste): `/wispborn ep set 250` deixa o botão
  desativado e mostra "Subir · 300 EP". Com `/wispborn ep set 300`, o botão acende e a barra fica pronta.
  Um clique leva ao Lvl 2, ao saldo 0, ao "+2" e à mensagem de nível. As cinco abas cabem na barra do painel
  em pt_br e en_us, sem passar da borda.

- [ ] **Step 4: Commit**

```bash
git add -A src/
git commit -m "feat(grimoire): abas da fagogenia e botao Subir nivel na aba Wisp"
```

---

### Task 4: Mana como sistema imune

**Files:**
- Create: `src/main/java/dev/eder/wispborn/host/ImmuneMath.java`, sprite `hud/mana_bar_warning.png` + `.mcmeta`
- Modify: `host/InfestRules.java`, `host/HostManager.java`, `network/TargetInfoPayload.java`,
  `network/SyncWispStatePayload.java`, `progress/ProgressManager.java` (`computeState`), `event/FormEvents.java`,
  `config/WispbornConfig.java`, `config/WispbornClientConfig.java`, `client/hud/HudMath.java`,
  `client/hud/WispHud.java`, `client/hud/HudSprites.java`, lang
- Test: `src/test/java/dev/eder/wispborn/host/ImmuneMathTest.java` (novo), `host/InfestRulesTest.java`,
  `network/PossessionPayloadsTest.java`, `network/SyncWispStatePayloadTest.java`, `client/hud/HudMathTest.java`

**Interfaces:**
- Produces:
  - `ImmuneMath.drainPerSecond(int wispLevel, int mobLevel, double base, double perLevelDiff, double minFactor): double`
    = `base * max(minFactor, 1 + perLevelDiff * (mobLevel - wispLevel))`
  - `InfestRules.check(boolean isMob, boolean alive, boolean hasPassenger, Optional<MobTraits> traits, int wispLevel, int levelGateMargin, float mana, int minMana): InfestResult`
    (a mesma ordem de precedência; `NO_MANA` quando `mana < minMana`)
  - `TargetInfoPayload` perde o campo `manaCost` (o cliente não usa)
  - `SyncWispStatePayload` ganha `float hostManaDrainPerSecond` no fim (0 fora do hospedeiro); `DEFAULT` com `0.0F`
  - `HostManager.Reason.EXPELLED`; `HostManager.applyImmuneDrain(ServerPlayer)`
  - `HudMath.manaWarning(float mana, float drainPerSecond, double warnSeconds): boolean` = `drainPerSecond > 0 && mana <= drainPerSecond * warnSeconds`
  - `HudSprites.MANA_BAR_WARNING` = `hud("mana_bar_warning")`
- Config (servidor): sai `mana.infestCostPerLevel`; entram `possess.minMana` (int, padrão 20, 0..100000),
  `mana.hostDrainBasePerSecond` (double, 1.0, 0..10000), `mana.hostDrainPerLevelDiff` (double, 0.5, 0..100),
  `mana.hostDrainMinFactor` (double, 0.1, 0..1).
- Config (cliente): `hud.manaWarnSeconds` (double, 5.0, 0..60).

- [ ] **Step 1: Write the failing tests**

```java
// ImmuneMathTest
@Test void strongerHostDrainsFasterAndWeakPreyBarelyResists() {
    assertEquals(1.0D, ImmuneMath.drainPerSecond(1, 1, 1.0D, 0.5D, 0.1D), 1e-9);
    assertEquals(1.5D, ImmuneMath.drainPerSecond(5, 6, 1.0D, 0.5D, 0.1D), 1e-9);
    assertEquals(0.1D, ImmuneMath.drainPerSecond(5, 3, 1.0D, 0.5D, 0.1D), 1e-9);
    assertEquals(0.1D, ImmuneMath.drainPerSecond(10, 1, 1.0D, 0.5D, 0.1D), 1e-9);
}

// InfestRulesTest: troque o último argumento (custo por nível) pela mana mínima e ajuste os casos
@Test void entryIsFreeButNeedsMinimumMana() {
    Optional<MobTraits> lv5 = Optional.of(new MobTraits(5, Sex.MALE));
    assertEquals(InfestResult.OK, InfestRules.check(true, true, false, lv5, 5, 0, 20.0F, 20));
    assertEquals(InfestResult.NO_MANA, InfestRules.check(true, true, false, lv5, 5, 0, 19.99F, 20));
    assertEquals(InfestResult.TOO_STRONG, InfestRules.check(true, true, false, lv5, 4, 0, 999.0F, 20));
}

// HudMathTest
@Test void manaWarnsWhenExpulsionIsNear() {
    assertTrue(HudMath.manaWarning(5.0F, 1.0F, 5.0D));
    assertFalse(HudMath.manaWarning(6.0F, 1.0F, 5.0D));
    assertFalse(HudMath.manaWarning(0.0F, 0.0F, 5.0D));
}
```

`PossessionPayloadsTest` e `SyncWispStatePayloadTest`: ajuste os construtores ao campo removido/adicionado; o
round-trip continua.

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew test --tests "dev.eder.wispborn.host.*" --tests dev.eder.wispborn.client.hud.HudMathTest`
Expected: FAIL de compilação.

- [ ] **Step 3: Implement**
  - `HostManager.tryInfest` e a rechecagem no fim do `tick`: passam `POSSESS_MIN_MANA`; some o
    `spendMana(costPerLevel × nível)`. `computeTargetInfo` sem `manaCost`. `sendInfestFailure` em `NO_MANA`
    mostra `Math.round(mana) + "/" + minMana`.
  - `applyImmuneDrain(player)`: só se `isHost` e o hospedeiro for `Mob` vivo com traços (senão retorna); gasto
    = `ImmuneMath.drainPerSecond(level(player), nível do mob, config...)`; `setMana(max(0, mana - gasto))`; se
    a mana chegou a 0 → `release(player, Reason.EXPELLED)`.
  - `release`: `EXPELLED` sai como `HOST_DIED` (desmonta e teleporta na hora) e mostra `wispborn.host.expelled`
    na barra de ação.
  - `FormEvents.onPlayerTickPost`, bloco de cada 20 ticks: `isHost` → `HostManager.applyImmuneDrain`; senão
    → regenera `MANA_REGEN_PER_SECOND`. A exaustão de fome continua só com `isWisp`.
  - `computeState`: `hostManaDrainPerSecond` com a mesma conta, 0 fora do hospedeiro.
  - `WispHud.renderWispMana`: com `isHost` e `manaWarning(...)`, alterna o sprite do progresso entre
    `MANA_BAR_PROGRESS` e `MANA_BAR_WARNING` a cada 5 ticks de GUI (`(guiTicks / 5) % 2`).
  - Sprite `hud/mana_bar_warning`: da A5 ou placeholder copiado de `hud/mana_bar_progress.png` (+ `.mcmeta`).

  Lang:

  | Chave | pt_br | en_us |
  |---|---|---|
  | `wispborn.host.expelled` (nova) | `O hospedeiro te expulsou!` | `Your host expelled you!` |

- [ ] **Step 4: Run the full build and tests**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, todos os testes passam.

- [ ] **Step 5: Check in game:** como wisp Nv 1 com 50 de mana, possua uma galinha Nv 1. A mana não cai na
  entrada, cai cerca de 1 por segundo dentro, a barra pisca nos últimos 5 s, e com 0 o wisp é expulso com a
  mensagem. Fora do hospedeiro a mana volta a subir. Com menos de 20 de mana, a possessão é recusada.

- [ ] **Step 6: Commit**

```bash
git add -A src/
git commit -m "feat(host): mana como sistema imune (entrada gratis, gasto continuo e expulsao)"
```

---

### Task 5: Nuvem de DNA (servidor)

**Files:**
- Create: `progress/DnaCloud.java`, `progress/DnaCloudRules.java`, `progress/DnaCloudManager.java`,
  `event/ProgressEvents.java`
- Modify: `registry/ModAttachments.java`, `WispbornMod.java` (registrar `ProgressEvents`),
  `config/WispbornConfig.java`, `form/WispTuning.java`, lang
- Test: `src/test/java/dev/eder/wispborn/progress/DnaCloudRulesTest.java` (novo), `DnaCloudTest.java` (novo)

**Interfaces:**
- Consumes: `WispProgress`, `ProgressManager.syncState`.
- Produces:
  - `record DnaCloud(ResourceKey<Level> dimension, Vec3 pos, int ep)` com `CODEC` (`ResourceKey.codec(Registries.DIMENSION)`, `Vec3.CODEC`, `Codec.INT`)
  - `ModAttachments.DNA_CLOUD: Supplier<AttachmentType<DnaCloud>>`, `serialize(DnaCloud.CODEC)`, `copyOnDeath()`.
    Ausência = sem nuvem (`getExistingData` / `removeData`); o padrão do builder nunca é lido.
  - `record DnaCloudRules.DeathOutcome(WispProgress progress, int cloudEp)`
  - `DnaCloudRules.onDeath(WispProgress progress): DeathOutcome`: saldo vai para a nuvem; o progresso fica com `ep = 0`
  - `DnaCloudRules.cloudY(double deathY, int minBuildHeight, int maxBuildHeight): double` limitado a `[min + 1, max - 1]`
  - `DnaCloudRules.canPickUp(boolean sameDimension, double distanceSqr, double radius): boolean`
  - `DnaCloudManager.onDeath(ServerPlayer)`, `DnaCloudManager.tick(ServerPlayer)`,
    `DnaCloudManager.cloudOf(ServerPlayer): Optional<DnaCloud>`
- Config: `dnaCloud.pickupRadius` (double, 1.5, 0.5..16).
- `WispTuning`: `DNA_CLOUD_COLOR = 0xE8C872` (âmbar) e `DNA_CLOUD_VIEW_DISTANCE = 64.0D` (visual, não é balanceamento).

- [ ] **Step 1: Write the failing tests**

```java
// DnaCloudRulesTest
@Test void deathMovesTheWholeBalanceToTheCloud() {
    DnaCloudRules.DeathOutcome out = DnaCloudRules.onDeath(new WispProgress(4, 730));
    assertEquals(new WispProgress(4, 0), out.progress());
    assertEquals(730, out.cloudEp());
}
@Test void deathWithZeroBalanceLeavesNoCloud() {
    assertEquals(0, DnaCloudRules.onDeath(new WispProgress(4, 0)).cloudEp());
}
@Test void cloudStaysInsideTheWorldHeight() {
    assertEquals(-63.0D, DnaCloudRules.cloudY(-90.0D, -64, 320), 1e-9);
    assertEquals(70.5D, DnaCloudRules.cloudY(70.5D, -64, 320), 1e-9);
    assertEquals(319.0D, DnaCloudRules.cloudY(400.0D, -64, 320), 1e-9);
}
@Test void pickupNeedsSameDimensionAndRadius() {
    assertTrue(DnaCloudRules.canPickUp(true, 2.25D, 1.5D));
    assertFalse(DnaCloudRules.canPickUp(true, 2.26D, 1.5D));
    assertFalse(DnaCloudRules.canPickUp(false, 0.0D, 1.5D));
}

// DnaCloudTest
@Test void codecRoundTrip() {
    // DnaCloud(ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_nether")),
    //          new Vec3(1.5, 64.0, -3.25), 730): JSON ida e volta igual
}
```

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew test --tests "dev.eder.wispborn.progress.DnaCloud*"`
Expected: FAIL de compilação.

- [ ] **Step 3: Implement**
  - `ProgressEvents.onLivingDeath(LivingDeathEvent)` com `@SubscribeEvent(priority = EventPriority.LOWEST)`;
    só `ServerPlayer` → `DnaCloudManager.onDeath`. (Totem impede o evento; outro mod que cancele também, porque
    `receiveCanceled` fica `false`.)
  - `onDeath`: aplica `DnaCloudRules.onDeath`. Com `cloudEp > 0`, grava `DnaCloud(dimensão, (x, cloudY(y), z), cloudEp)`,
    substituindo a anterior, e manda a mensagem de sistema `wispborn.dna_cloud.dropped` com x, y, z e EP. Com 0,
    `removeData(DNA_CLOUD)`. Grava o progresso e sincroniza. Comentário: na fatia 1 toda morte do jogador é morte
    do wisp; a fatia 5 separa a morte do corpo, que não gera nuvem (spec §7).
  - `ProgressEvents.onPlayerTickPost` → `DnaCloudManager.tick`: a cada 4 ticks, jogador vivo, nuvem na mesma
    dimensão e `canPickUp(..., pickupRadius)` → `withEpAdded(cloud.ep())`, `removeData`, som
    `EXPERIENCE_ORB_PICKUP`, barra de ação `wispborn.dna_cloud.recovered`, `syncState`. A cada 10 ticks, se a
    nuvem estiver na mesma dimensão e a até `DNA_CLOUD_VIEW_DISTANCE`, manda partículas só para o dono:
    `serverLevel.sendParticles(player, new DustParticleOptions(cor, 1.2F), true, x, y, z, 6, 0.3, 0.3, 0.3, 0.0)`.
    Vale em qualquer forma, inclusive dentro do hospedeiro.

  Lang:

  | Chave | pt_br | en_us |
  |---|---|---|
  | `wispborn.dna_cloud.dropped` | `Seu DNA ficou para trás em %s, %s, %s (%s EP).` | `Your DNA was left behind at %s, %s, %s (%s EP).` |
  | `wispborn.dna_cloud.recovered` | `DNA recuperado: +%s EP` | `DNA recovered: +%s EP` |

- [ ] **Step 4: Run the full build and tests**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, todos os testes passam.

- [ ] **Step 5: Commit**

```bash
git add -A src/
git commit -m "feat(progress): nuvem de DNA recuperavel na morte do wisp"
```

---

### Task 6: Marcador da nuvem no HUD

**Files:**
- Create: `network/DnaCloudMarker.java`, sprites `hud/dna_cloud_badge.png` + `.mcmeta`, `hud/dna_cloud_arrow_0..7.png`
- Modify: `network/SyncWispStatePayload.java`, `progress/ProgressManager.java` (`computeState`),
  `client/hud/HudMath.java`, `client/hud/WispHud.java`, `client/hud/HudSprites.java`, lang
- Test: `client/hud/HudMathTest.java`, `network/SyncWispStatePayloadTest.java`

**Interfaces:**
- Consumes: `DnaCloudManager.cloudOf`.
- Produces:
  - `record DnaCloudMarker(boolean sameDimension, double x, double y, double z, int ep)` com `STREAM_CODEC`
    (`StreamCodec.composite` com BOOL, DOUBLE, DOUBLE, DOUBLE, VAR_INT); fora da dimensão, `x, y, z = 0`
  - `SyncWispStatePayload` ganha `Optional<DnaCloudMarker> cloud` no fim (`ByteBufCodecs.optional`); `DEFAULT` com `Optional.empty()`
  - `HudMath.cloudArrowIndex(double dx, double dz, float yawDegrees): int` de 0 a 7. Ângulo do alvo =
    `Math.toDegrees(Math.atan2(-dx, dz))` (convenção de yaw do Minecraft); relativo = `Mth.wrapDegrees(alvo - yaw)`;
    índice = `Math.floorMod(Math.round(relativo / 45.0F), 8)`. 0 = à frente, 2 = à direita, 4 = atrás, 6 = à esquerda.
  - `HudSprites.DNA_CLOUD_BADGE`, `HudSprites.DNA_CLOUD_ARROWS: ResourceLocation[8]`

- [ ] **Step 1: Write the failing tests**

```java
// HudMathTest — yaw 0 olha para +Z (sul); yaw 90 olha para -X (oeste)
@Test void cloudArrowPointsRelativeToTheView() {
    assertEquals(0, HudMath.cloudArrowIndex(0.0D, 10.0D, 0.0F));
    assertEquals(2, HudMath.cloudArrowIndex(-10.0D, 0.0D, 0.0F));
    assertEquals(4, HudMath.cloudArrowIndex(0.0D, -10.0D, 0.0F));
    assertEquals(6, HudMath.cloudArrowIndex(10.0D, 0.0D, 0.0F));
    assertEquals(0, HudMath.cloudArrowIndex(-10.0D, 0.0D, 90.0F));
    assertEquals(1, HudMath.cloudArrowIndex(-10.0D, 10.0D, 0.0F));
    assertEquals(0, HudMath.cloudArrowIndex(0.0D, 10.0D, 360.0F));
}
// SyncWispStatePayloadTest: round-trip com cloud presente (mesma dimensão e outra) e vazio
```

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew test --tests dev.eder.wispborn.client.hud.HudMathTest --tests dev.eder.wispborn.network.SyncWispStatePayloadTest`
Expected: FAIL de compilação.

- [ ] **Step 3: Implement**
  - `computeState`: `cloud = cloudOf(player).map(c -> mesma dimensão ? new DnaCloudMarker(true, x, y, z, ep) : new DnaCloudMarker(false, 0, 0, 0, ep))`.
  - Camada nova `wisp:dna_cloud_marker` registrada acima de `EXPERIENCE_LEVEL`. Desenha só como **wisp** (não
    hospedeiro), fora da conjuração (`!ClientPossession.isCasting()`) e com nuvem. Assim o slot central inferior
    fica com a prioridade conjuração > Consumir > nuvem, sem sobreposição.
  - Selo: `DNA_CLOUD_BADGE` em nine-slice, 16 de altura, largura `HudMath.cardWidth(9 + 3 + largura do texto, 5, 40, 120)`,
    centralizado, com a base em `guiHeight - BOTTOM_SLOT_OFFSET - 10 * (healthRows - 1)`. Seta 9×9 em `(x + 5, y + 4)`,
    texto em `(x + 17, y + 5)`. O texto é `wispborn.hud.cloud_distance` com a distância 3D arredondada em blocos,
    ou `wispborn.hud.cloud_elsewhere` quando `!sameDimension`; nesse caso a seta não aparece e o texto começa em `x + 5`.
  - Sprites: da A5 ou placeholders (`dna_cloud_badge` copiado de `consume_badge` + `.mcmeta`; as oito setas
    copiadas de `target_ok`).

  Lang:

  | Chave | pt_br | en_us |
  |---|---|---|
  | `wispborn.hud.cloud_distance` | `DNA · %s m` | `DNA · %s m` |
  | `wispborn.hud.cloud_elsewhere` | `DNA em outra dimensão` | `DNA in another dimension` |

- [ ] **Step 4: Run the full build and tests**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, todos os testes passam.

- [ ] **Step 5: Check in game:** `/wispborn ep set 500`, depois `/kill`. Depois do respawn, aparece a
  mensagem com as coordenadas; o selo mostra a distância, e a seta gira quando você vira a câmera. Perto da
  nuvem aparecem partículas âmbar, e encostar devolve os 500. Morrer de novo antes de recuperar troca a nuvem;
  morrer com saldo 0 faz o selo sumir. Numa morte no Nether, o selo no Mundo Normal mostra "DNA em outra dimensão".

- [ ] **Step 6: Commit**

```bash
git add -A src/
git commit -m "feat(hud): marcador de direcao e distancia da nuvem de DNA"
```

---

### Task 7: Documentação da entrega

**Files:**
- Create: `docs/specs/F6-fundacao-v2.md` (spec curta no formato do AGENTS.md: aponta para este plano e para o
  design; seções "Dúvidas do implementador" e "Relatório de implementação" preenchidas)
- Modify: `CHANGELOG.md`, `docs/PLAN.md` (linha F6; e acerta o status de M3c, F1–F5, que está desatualizado),
  `AGENTS.md` (linha "Specs:" com F5 e F6)

- [ ] **Step 1:** Escreva os três arquivos. O relatório lista os placeholders de sprite que ainda faltam
  trocar pela A5 e qualquer desvio deste plano.
- [ ] **Step 2: Run** `./gradlew build`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Commit**

```bash
git add docs/ CHANGELOG.md AGENTS.md
git commit -m "docs(F6): relatorio da fundacao v2, CHANGELOG e PLAN atualizados"
```

Depois: Eder repete as conferências dos Tasks 3, 4 e 6 num mundo com save antigo, para validar a migração:
o nível e o saldo batem com "nível pela curva antiga, resto vira saldo".
