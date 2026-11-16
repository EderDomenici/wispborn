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
public record WispProgress(int totalEp, int level) { START = (0, 1) }
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

## Fora de escopo
Infestação, custo de mana em uso, drenagem, consumo, sync de progresso para o client, tela de atributos (M6).

## Dúvidas do implementador
_(preencher se houver)_

## Relatório de implementação
_(preencher ao terminar)_
