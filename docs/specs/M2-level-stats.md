# M2 — Nível, atributos e mana

**Objetivo:** o wisp tem EP, nível, pontos de atributo e mana, tudo persistente. Cada mob vanilla do
MVP tem um tier por tag; **só o nível** define o tier máximo infectável. Os 4 atributos existem e são
distribuídos por comando. Tudo ajustável em config. HUD na action bar.
**Estimativa:** 2–3 dias. **Branch:** `feat/M2-level-stats`. **Depende de:** M1.

## Contratos

### `config/WispbornConfig.java` (`ModConfigSpec`, tipo **SERVER**)
Registrar no construtor do mod: `modContainer.registerConfig(ModConfig.Type.SERVER, WispbornConfig.SPEC)`
(o construtor do `@Mod` recebe `IEventBus modEventBus, ModContainer modContainer`).

| Chave | Tipo | Padrão | Usado em |
|---|---|---|---|
| `levels.thresholds` | lista de int | `[300, 800, 1600, 2800, 4500, 6800, 9800, 13600, 18300]` | M2 (EP total para Nv 2…10) |
| `tiers.minLevel` | lista de int | `[1, 3, 5]` | M2 (nível mínimo dos tiers 1, 2, 3) |
| `stats.pointsPerLevel` | int | 2 | M2 |
| `stats.maxPerStat` | int | 10 | M2 |
| `stats.absorptionPerPoint` | double | 0.005 | M4 |
| `stats.efficiencyPerPoint` | double | 0.10 | M4, M5 |
| `stats.reservePerPoint` | int | 10 | M2 |
| `stats.vitalityHeartsPerPoint` | int | 1 | M2 |
| `mana.base` | int | 50 | M2 |
| `mana.regenPerSecond` | double | 1.0 | M2 |
| `mana.infestCostPerTier` | int | 15 | M3 |
| `drain.intervalTicks` | int | 100 (5s) | M4 |
| `drain.basePercentPerPulse` | double | 0.02 | M4 |
| `drain.floorPercent` | double | 0.20 | M4, M5 |
| `ep.perPulsePerTier` | int | 10 | M4 |
| `ep.consumeBonusPerTier` | int | 100 | M5 |

Os valores de M3–M5 já entram aqui para o arquivo de config nascer completo.
EP fica em escala ×10 de propósito: com Eficiência +10%, `10 → 11` é visível; `1 → 1.1` sumiria no arredondamento.

### Dados do jogador (`ModAttachments`, todos com `.serialize(CODEC)` e `.copyOnDeath()`, **sem sync** neste módulo)
```java
public record WispProgress(int totalEp) { START = (0) }   // nível NÃO é guardado: ProgressManager.level(p) = LevelCurve.levelFor(totalEp, thresholds)
public record WispStats(int absorption, int efficiency, int reserve, int vitality) {
    ZERO; int spent(); int get(Stat s); WispStats with(Stat s, int value);
}
// MANA: AttachmentType<Float>, default 50f (mana.base), Codec.FLOAT
```
`progress/Stat.java`: enum `ABSORPTION, EFFICIENCY, RESERVE, VITALITY` com `StringRepresentable`
(ids `absorption`, `efficiency`, `reserve`, `vitality`) e chave de lang `wispborn.stat.<id>`.

**Pontos disponíveis não são guardados**: `available = pointsPerLevel × (level − 1) − stats.spent()`.
Assim `ep set`, reset e mudança de config nunca deixam pontos inconsistentes.

### Funções puras (sem Minecraft, com JUnit)
- `progress/LevelCurve`: `levelFor(totalEp, thresholds)`, `nextThreshold(totalEp, thresholds) → OptionalInt`.
- `progress/StatMath`:
  - `availablePoints(level, pointsPerLevel, spent)`
  - `drainPercent(basePercent, absorptionPts, perPoint)` → `0.02 + 0.005 × pts`
  - `epMultiplier(efficiencyPts, perPoint)` → `1 + 0.10 × pts`
  - `applyEp(baseEp, multiplier)` → `Math.round(baseEp × multiplier)`
  - `maxMana(base, reservePts, perPoint)` → `50 + 10 × pts`
  - `vitalityBonusHealth(vitalityPts, heartsPerPoint)` → `2 × hearts × pts` (HP)
- `progress/TierResolver`: `maxTierFor(level, minLevels)` (puro); `tierOf(EntityType<?>) → OptionalInt` (usa tags).

### Tags (`src/main/resources/data/wispborn/tags/entity_type/`)
- `tier_1.json`: `minecraft:chicken`, `minecraft:pig`, `minecraft:cow`, `minecraft:sheep`, `minecraft:rabbit`
- `tier_2.json`: `minecraft:zombie`, `minecraft:skeleton`, `minecraft:spider`
- `tier_3.json`: `minecraft:creeper`, `minecraft:enderman`
`ModTags.TIER_1..3 = TagKey.create(Registries.ENTITY_TYPE, rl("tier_N"))`; `tierOf` = maior tier cuja tag contém o tipo (`type.is(tag)`).

### `progress/ProgressManager.java` (servidor)
- `addEp(ServerPlayer, int baseEp)` — aplica `epMultiplier` (Eficiência), soma, recalcula nível. Se subiu:
  mensagem `wispborn.level_up` (novo nível, pontos ganhos e, se mudou, o tier liberado) + `SoundEvents.PLAYER_LEVELUP`.
  Retorna o EP efetivamente ganho (o M4/M5 usam para mensagens).
- `setEp(ServerPlayer, int total)` — debug; recalcula nível sem mensagem. Se os pontos disponíveis ficarem negativos
  (nível caiu), chama `resetStats` e avisa no chat.
- `allocate(ServerPlayer, Stat) → AllocateResult` (`OK, NO_POINTS, MAXED`) — +1 no atributo, depois `refreshStats`.
- `resetStats(ServerPlayer)` — zera os atributos (debug), `refreshStats`.
- `refreshStats(ServerPlayer)` — reaplica a Vitalidade (via `FormManager.applyModifiers`) e limita a mana ao novo máximo.
- `addMana / spendMana(ServerPlayer, float) → boolean / getMaxMana(ServerPlayer)`.

### Vitalidade
`FormManager.applyModifiers` passa a aplicar, só nas formas WISP e HOST, `MAX_HEALTH` `ADD_VALUE`
`+vitalityBonusHealth` com id `wispborn:wisp_vitality` (soma com o `-14` do M1). Ao subir, curar a diferença.

### Mana
`PlayerTickEvent.Post`, servidor, a cada 20 ticks: se forma == WISP, `mana += regenPerSecond`, até o máximo.
Na forma HOST **não** regenera. Na forma HUMAN também não (mana é do wisp).

### HUD (servidor → action bar, a cada 20 ticks, forma WISP)
`Wisp · Nv 2 · EP 450/800 · Mana 35/60 · 2 pts` (sem `· N pts` quando 0; no nível máximo `EP 18450 · máx`).

### Comandos (estender `WispbornCommand`, permissão 2)
- `/wispborn ep add|set <n> [player]`
- `/wispborn stat add <absorption|efficiency|reserve|vitality> [player]` (com sugestões no tab)
- `/wispborn stat reset [player]`
- `/wispborn mana set <n> [player]`
- `/wispborn info [player]` → forma, nível, EP, pontos disponíveis, cada atributo com o efeito atual, mana, tier máximo.

### Lang (en_us e pt_br)
`wispborn.level_up`, `wispborn.hud.wisp`, `wispborn.hud.max`, `wispborn.stat.*` (pt: Absorção, Eficiência,
Reserva de mana, Vitalidade), mensagens dos comandos e de `AllocateResult`.

## Testes (JUnit)
- `LevelCurveTest`: 0 → Nv 1; 299 → 1; 300 → 2; 800 → 3; 100_000 → 10; `nextThreshold` no máximo vazio.
- `TierResolverTest` (`maxTierFor`): Nv 1 → 1; Nv 2 → 1; Nv 3 → 2; Nv 5 → 3; Nv 10 → 3.
- `StatMathTest`: `availablePoints(1,2,0)=0`, `(3,2,1)=3`; `drainPercent(0.02,4,0.005)=0.04`;
  `applyEp(10, epMultiplier(1,0.1))=11`; `maxMana(50,2,10)=70`; `vitalityBonusHealth(3,1)=6`.

## Checklist manual
- [ ] Wisp novo: action bar `Wisp · Nv 1 · EP 0/300 · Mana 50/50`.
- [ ] `/wispborn ep add 300` → Nv 2, mensagem com `+2 pontos`, som. `/wispborn ep add 500` → Nv 3 e aviso de tier 2 liberado.
- [ ] `/wispborn stat add vitality` duas vezes → wisp passa de 3 para 5 corações. Terceira vez com 0 pontos → "sem pontos".
- [ ] `/wispborn stat add reserve` → mana máxima 60; HUD mostra.
- [ ] `/wispborn mana set 0` → mana volta 1/s até o máximo; como HUMAN não regenera.
- [ ] Com pontos gastos, `/wispborn ep set 0` → atributos zerados automaticamente com aviso (nunca pontos negativos).
- [ ] Relog e morte mantêm EP, nível, atributos e mana; a Vitalidade continua aplicada após respawn.
- [ ] Editar `serverconfig/wispborn-server.toml` (em `run/saves/<mundo>/`) e recarregar: curva e valores mudam.

## Ajustes pós-revisão (17/11/2026, Claude)
Problemas achados na revisão do Claude, corrigidos a pedido de Eder:
1. **Nível guardado ficava desatualizado ao mudar a curva.** `WispProgress` guardava `level`, que só era
   recalculado ao ganhar EP; editar `levels.thresholds` deixava HUD, pontos e tier com o nível antigo
   (falhava o último item do checklist). Agora `WispProgress` guarda só `totalEp` e o nível é sempre
   `ProgressManager.level(p)` = `LevelCurve.levelFor(totalEp, thresholds)`. Saves com o campo `level`
   continuam carregando (campo ignorado; teste `WispProgressTest`).
2. **Pontos negativos após mudar a curva.** `ProgressManager.validateStats(p)` (o mesmo reset automático
   do `ep set`) roda no tick de 20 em 20 ticks para todo jogador: cobre login e recarga do config.
3. **Textos fora da lang no `/wispborn info` e `/wispborn form`.** O nome da forma usa
   `wispborn.form.<human|wisp|host>` e o EP no nível máximo usa `wispborn.hud.max`.
4. **`levels.thresholds` fora de ordem.** `WispbornConfig.levelThresholds()` devolve a lista ordenada e sem repetição.

## Fora de escopo
Infestação, custo de mana em uso, drenagem, consumo, sync de progresso para o client, tela de atributos (M6).

## Dúvidas do implementador
Nenhuma dúvida.

## Relatório de implementação
- **Arquivos criados/alterados:**
  - `src/main/java/dev/eder/wispborn/config/WispbornConfig.java`: `ModConfigSpec` (`SERVER`) com todas as chaves das seções `levels`, `tiers`, `stats`, `mana`, `drain` e `ep`.
  - `src/main/java/dev/eder/wispborn/WispbornMod.java`: registro de `WispbornConfig.SPEC` como `ModConfig.Type.SERVER`.
  - `src/main/java/dev/eder/wispborn/progress/Stat.java`: enum `ABSORPTION`, `EFFICIENCY`, `RESERVE`, `VITALITY` (`StringRepresentable`, `CODEC`, `STREAM_CODEC`, `translationKey()`, `byName(String)`).
  - `src/main/java/dev/eder/wispborn/progress/WispProgress.java`: record `(int totalEp, int level)` com `START = (0, 1)` e `CODEC`.
  - `src/main/java/dev/eder/wispborn/progress/WispStats.java`: record `(int absorption, int efficiency, int reserve, int vitality)` com `ZERO`, `spent()`, `get(Stat)`, `with(Stat, int)` e `CODEC`.
  - `src/main/java/dev/eder/wispborn/progress/AllocateResult.java`: enum `OK, NO_POINTS, MAXED`.
  - `src/main/java/dev/eder/wispborn/progress/LevelCurve.java`: funções puras `levelFor` e `nextThreshold`.
  - `src/main/java/dev/eder/wispborn/progress/StatMath.java`: funções puras `availablePoints`, `drainPercent`, `epMultiplier`, `applyEp`, `maxMana` e `vitalityBonusHealth`.
  - `src/main/java/dev/eder/wispborn/progress/TierResolver.java`: `maxTierFor(level, minLevels)` (puro) e `tierOf(EntityType<?>)` via tags `ModTags`.
  - `src/main/java/dev/eder/wispborn/progress/ProgressManager.java`: `addEp`, `setEp`, `allocate`, `resetStats`, `refreshStats`, `addMana`, `spendMana`, `getMana`, `setMana`, `getMaxMana` e `buildWispHud`.
  - `src/main/java/dev/eder/wispborn/registry/ModAttachments.java`: adicionados attachments `PROGRESS`, `STATS` e `MANA` com `.serialize(...)` e `.copyOnDeath()`.
  - `src/main/java/dev/eder/wispborn/registry/ModTags.java` e `src/main/resources/data/wispborn/tags/entity_type/tier_1..3.json`: tags de tier dos mobs vanilla do MVP.
  - `src/main/java/dev/eder/wispborn/form/WispTuning.java` e `FormManager.java`: adicionado modificador `wispborn:wisp_vitality` (`ADD_VALUE` em `MAX_HEALTH`) nas formas `WISP` e `HOST`.
  - `src/main/java/dev/eder/wispborn/event/FormEvents.java`: `PlayerTickEvent.Post` regenerando mana (1/s na forma `WISP`) e exibindo o HUD na action bar a cada 20 ticks.
  - `src/main/java/dev/eder/wispborn/command/WispbornCommand.java`: subcomandos `/wispborn ep add|set`, `/wispborn stat add|reset`, `/wispborn mana set` e `/wispborn info`.
  - `src/main/resources/assets/wispborn/lang/en_us.json` e `pt_br.json`: chaves de HUD, level up, atributos e comandos.
  - `src/test/java/dev/eder/wispborn/progress/LevelCurveTest.java`, `TierResolverTest.java` e `StatMathTest.java`: suíte JUnit 5 cobrindo todas as funções puras do M2.
- **Decisões tomadas:**
  - `ProgressManager.refreshStats` cura a diferença positiva de `MAX_HEALTH` quando a Vitalidade sobe, conforme especificado em "Vitalidade: Ao subir, curar a diferença".
  - `ProgressManager.setEp` verifica se `availablePoints < 0` após recalcular o nível e chama `resetStats` automaticamente com aviso no chat (`wispborn.stat.auto_reset`).
- **Saída final de `./gradlew build`:**
  ```text
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  > Task :check
  > Task :build
  BUILD SUCCESSFUL in 32s
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
  [08:19:04] [Server thread/DEBUG] [ne.ne.fm.co.ConfigTracker/CONFIG]: Loaded TOML config file D:\Projetos\Mine Mod\run\config\wispborn-server.toml
  [08:19:07] [Server thread/INFO] [minecraft/DedicatedServer]: Done (3.238s)! For help, type "help"
  [08:19:07] [Server thread/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```
- **Pendências:**
  - Validação do checklist manual in-game por Eder (`runClient`).
