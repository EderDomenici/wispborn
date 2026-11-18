# F2 — Nível e sexo dos mobs (fim dos tiers)

**Objetivo:** todo mob vanilla infestável ganha **nível** (sorteado numa faixa da espécie) e **sexo**
(macho/fêmea), guardados no próprio mob e visíveis ao jogador. O nível substitui os tiers: limita quem o
wisp pode infestar, define o custo de mana e (na F3) quanto EP a presa rende. A reprodução vanilla passa a
exigir macho + fêmea, base para a reprodução do wisp no futuro.
**Estimativa:** 2–3 dias. **Branch:** `feat/F2-mob-traits`. **Depende de:** F1.
**Design:** `docs/superpowers/specs/2026-11-17-fundacao-design.md`.

## Regras

### Dados das faixas
1. Arquivo de dados `data/wispborn/wisp_mob_levels/vanilla.json`, lido por um reload listener
   (`AddReloadListenerEvent`, `SimpleJsonResourceReloadListener` sobre a pasta `wisp_mob_levels`).
   Formato, validado por Codec:
   ```json
   { "entries": { "minecraft:zombie": { "min": 4, "max": 8 }, "...": {} } }
   ```
   Vários arquivos na pasta são somados (o último carregado vence em conflito). `min ≥ 1`, `max ≥ min`;
   entrada inválida é ignorada com log `WARN` dizendo o arquivo e a chave.
2. Espécie fora da tabela = **imune** (`InfestResult.IMMUNE`), sem nível e sem sexo.
3. Tabela inicial (ponto de partida para balanceamento; chefes, `giant` e jogadores ficam de fora):

| Faixa | Espécies |
|---|---|
| 1–1 | tadpole |
| 1–2 | chicken, rabbit, bat, cod, salmon, tropical_fish |
| 1–3 | pig, sheep, cow, frog, parrot |
| 2–3 | pufferfish |
| 2–4 | squid, turtle, axolotl, armadillo, bee, cat, ocelot, allay, snow_golem |
| 3–5 | glow_squid, mooshroom, silverfish |
| 3–6 | fox, goat, wolf, horse, donkey, mule, llama, trader_llama, villager |
| 3–7 | slime |
| 4–6 | wandering_trader |
| 4–7 | camel, panda, dolphin |
| 4–8 | zombie, zombie_villager, spider |
| 5–8 | sniffer, skeleton_horse, zombie_horse |
| 5–9 | husk, drowned, skeleton, cave_spider |
| 6–8 | endermite |
| 6–9 | strider, polar_bear |
| 6–10 | stray, bogged |
| 7–11 | pillager |
| 8–10 | vex |
| 8–12 | creeper, phantom |
| 9–13 | witch |
| 10–13 | piglin, zombified_piglin |
| 10–14 | vindicator, magma_cube |
| 12–15 | guardian, hoglin |
| 12–16 | enderman, iron_golem |
| 13–16 | blaze, breeze, zoglin |
| 14–17 | ghast, evoker, illusioner |
| 15–18 | ravager, piglin_brute, wither_skeleton |
| 16–20 | shulker |

Imunes (fora do arquivo): `ender_dragon`, `wither`, `warden`, `elder_guardian`, `giant`.

### Traços por mob
4. Novo attachment `ModAttachments.MOB_TRAITS` (`MobTraits`), serializado, em entidades `Mob`.
   `MobTraits(int level, Sex sex)` com `CODEC` e `STREAM_CODEC`. `Sex { MALE, FEMALE }` com
   `StringRepresentable` (`"male"`, `"female"`) e `STREAM_CODEC`.
5. Sorteio no servidor em `EntityJoinLevelEvent`: se a entidade é `Mob`, **não** tem o attachment
   (`hasData`) e a espécie está na tabela → `MobTraitRoller.roll(range, femaleChance, random)` usando o
   `RandomSource` do mob, e `setData`. Mob carregado do disco já tem os traços e não sorteia de novo.
   Espécie que entrar na tabela depois sorteia na próxima vez que o mob carregar.
6. Filhote de reprodução entra no mundo sem traços → sorteia normalmente. Herança dos pais: fora de
   escopo (vai para o design da reprodução do wisp).
7. Mudar a faixa no JSON **não** altera mobs já sorteados.
8. Sync manual: `SyncMobTraitsPayload(int entityId, int level, Sex sex)` (play-to-client) enviado em
   `PlayerEvent.StartTracking` quando o alvo é `Mob` com traços. No client, o handler guarda no próprio
   entity (`setData` no client) para leitura do renderer.

### Nível do wisp
9. Máximo passa de 10 para **20**. Padrão de `levels.thresholds`:
   `300, 800, 1600, 2800, 4500, 6800, 9800, 13600, 18300, 24000, 30800, 38800, 48100, 58800, 71000, 84800, 100300, 117600, 136800`.
   Os pontos de atributo por nível continuam `stats.pointsPerLevel`.

### Infestação
10. `InfestRules.check` troca tier por nível, mantendo a ordem de precedência do M3:
    mob vivo → sem passageiro → tem traços (senão `IMMUNE`) → `mobLevel ≤ wispLevel + infest.levelGateMargin`
    (senão `TOO_STRONG`) → mana ≥ `mana.infestCostPerLevel × mobLevel` (senão `NO_MANA`) → `OK`.
11. `TOO_STRONG` mostra o nível do mob e o do wisp: `wispborn.infest.too_strong` = `Forte demais (Nv %s; você Nv %s)`.
12. Custo de mana debitado no fim da conjuração (fluxo do M3b): `infestCostPerLevel × mobLevel`.
13. `TargetInfoPayload` passa a `(entityId, result, mobLevel, Optional<Sex> sex, manaCost)`; quando imune,
    `mobLevel = 0` e `sex` vazio (`ByteBufCodecs.optional`).
    `wispborn.target.info` = `%s %s · Nv %s · %s mana` (nome, símbolo do sexo, nível, custo).

### Fator de EP (usado pela F3)
14. `EpMath.levelFactor(int wispLevel, int mobLevel, int grace, double falloffPerLevel)`:
    `diff = wispLevel − mobLevel`; `diff ≤ grace` → 1.0; senão `max(0, 1 − falloffPerLevel × (diff − grace))`.
    Esta spec só cria a função e a config; a F3 usa.

### Placa sobre o mob
15. No client, para mobs com traços a até `client.nameplateRange` (8) blocos e com linha de visão, desenhar
    acima da cabeça (acima do nome, se houver) uma linha `♀ Nv 6` / `♂ Nv 6`, no estilo da placa vanilla
    (`RenderLivingEvent.Post`, `Font.drawInBatch`, billboard para a câmera, fundo translúcido vanilla).
    Não aparece com F1 (`hideGui`) nem para o mob em que o próprio jogador está montado.
16. Nesta spec os símbolos são os caracteres Unicode `♂`/`♀` e a cor é neutra (branco). A F4 troca pelos
    glifos em pixel art da fonte do mod e colore por situação (precisa do nível do wisp no client).

### Reprodução só macho + fêmea
17. Mixin (primeiro do projeto; esta spec autoriza):
    - Config `wispborn.mixins.json` (`package: dev.eder.wispborn.mixin`, `compatibilityLevel: JAVA_21`,
      `injectors.defaultRequire: 1`) declarado em `neoforge.mods.toml` (`[[mixins]] config = "wispborn.mixins.json"`).
    - Envolver **toda** chamada a `Animal#canMate(Animal)` dentro de `BreedGoal` (`getFreePartner`) e de
      `AnimalMakeLove` (as duas chamadas, incluindo a do lambda de `findValidBreedPartner`), com
      MixinExtras `@WrapOperation` (vem no NeoForge 1.21.1): resultado original **e**
      `BreedingRules.canMate(sexoA, sexoB)`.
    - Por que no ponto de chamada e não em `Animal#canMate`: `Wolf`, `Parrot` e a família dos cavalos
      (`AbstractHorse`, `Horse`, `Donkey`, `Llama`, `Camel`) reescrevem `canMate` sem chamar a base.
      Todas as espécies escolhem o par por `BreedGoal` ou `AnimalMakeLove`.
18. `BreedingRules.canMate(Optional<Sex> a, Optional<Sex> b)`: true se algum lado não tem sexo (espécie
    fora da tabela, compatibilidade com outros mods); senão true só se os sexos forem diferentes.
19. Rede de segurança: `BabyEntitySpawnEvent` cancelado se os pais têm o mesmo sexo (cobre caminhos
    fora dos dois pontos acima, como outros mods). Não deve disparar no jogo vanilla.

### Remoções
20. Sai: `TierResolver.tierOf`, `TierResolver.maxTierFor`, `ModTags.TIER_1..3`, as tags
    `data/wispborn/tags/entity_type/tier_*.json`, `tiers.minLevel`, `mana.infestCostPerTier`, a mensagem
    `wispborn.level_up.tier_unlocked`. O `/wispborn info` troca "Tier máx" por nada (o nível já aparece).
    `ep.perPulsePerTier` e `ep.consumeBonusPerTier` ficam até a F3 trocá-los.

### Debug
21. `/wispborn traits <entidade>`: mostra nível e sexo. `/wispborn traits <entidade> set <nível> <male|female>`:
    sobrescreve e reenvia o sync. Só para teste.

## Config (`WispbornConfig`)
| Chave | Padrão | Faixa |
|---|---|---|
| `levels.thresholds` | lista do item 9 | — |
| `infest.levelGateMargin` | 0 | 0–100 |
| `mana.infestCostPerLevel` | 5 | 0–100000 |
| `traits.femaleChance` | 0.5 | 0–1 |
| `ep.levelGrace` | 2 | 0–100 |
| `ep.falloffPerLevel` | 0.25 | 0–1 |

Client (`ModConfig.Type.CLIENT`, novo): `client.nameplateRange` = 8 (1–64).

## Contratos
- `traits/Sex.java`, `traits/MobTraits.java`, `traits/MobLevelRange.java` (record `min, max` + Codec).
- `traits/MobLevelTable.java`: reload listener; `Optional<MobLevelRange> rangeFor(EntityType<?>)`.
- `traits/MobTraitRoller.java` (puro): `static MobTraits roll(MobLevelRange range, double femaleChance, RandomSource random)`.
- `traits/BreedingRules.java` (puro).
- `traits/MobTraitsManager.java`: `Optional<MobTraits> traitsOf(Mob)`, `set(Mob, MobTraits)` (com sync).
- `progress/EpMath.java` (puro): `levelFactor`.
- `event/TraitEvents.java`: `EntityJoinLevelEvent`, `StartTracking`, `BabyEntitySpawnEvent`.
- `network/SyncMobTraitsPayload.java`; `TargetInfoPayload` alterado.
- `mixin/BreedGoalMixin.java`, `mixin/AnimalMakeLoveMixin.java`.
- `client/MobNameplateRenderer.java` (só `Dist.CLIENT`).

## Testes (JUnit)
- `MobTraitRoller`: com `RandomSource.create(42)`, 1000 sorteios em `4–8` ficam sempre em `[4, 8]` e
  cobrem os 5 valores; `femaleChance 0` → sempre MALE; `1` → sempre FEMALE; `0.5` → entre 40% e 60% de fêmeas.
- `BreedingRules`: M+F true, F+M true, M+M false, F+F false, vazio + M true.
- `InfestRules` (reescrito): precedência completa; nível igual ao do wisp → OK; nível +1 → TOO_STRONG;
  margem 1 → OK; sem traços → IMMUNE; mana exata → OK; mana − 0,01 → NO_MANA.
- `EpMath.levelFactor` com grace 2 e falloff 0,25: (7,7)=1; (7,5)=1; (7,4)=0,75; (7,3)=0,5; (7,2)=0,25;
  (7,1)=0; (5,9)=1 (mob acima).
- `MobLevelRange` Codec: `{min:4,max:8}` decodifica; `min > max` falha.
- `LevelCurve` com a lista nova: EP 0 → Nv 1; 136800 → Nv 20; 136799 → Nv 19.
- Payloads: round-trip de `SyncMobTraitsPayload` e `TargetInfoPayload`.

## Checklist manual
- [ ] Mundo novo: placas `♂/♀ Nv X` sobre mobs próximos; galinhas Nv 1–2, zumbis Nv 4–8.
- [ ] Salvar, sair e voltar: os mesmos mobs mantêm nível e sexo (`/wispborn traits` antes e depois).
- [ ] Wisp Nv 1: galinha Nv 1 → infesta; galinha Nv 2 → "Forte demais (Nv 2; você Nv 1)".
- [ ] Custo: galinha Nv 1 = 5 de mana; zumbi Nv 6 (com `ep set` para Nv 6) = 30.
- [ ] Ender dragon, warden e jogador: imunes.
- [ ] Duas vacas: `/wispborn traits` para deixar as duas MALE, dar trigo → corações aparecem, mas elas
      **não** se aproximam nem fazem filhote. Trocar uma para FEMALE e dar trigo de novo → filhote, que
      ganha nível e sexo próprios.
- [ ] Repetir com lobos domados (override de `canMate`) e com cabras ou sapos (`AnimalMakeLove`).
- [ ] Editar o JSON (`/reload`) mudando a faixa da galinha: galinhas novas seguem a faixa nova, as antigas não mudam.
- [ ] Multiplayer (`runClient2`): o Player2 vê as mesmas placas e números.
- [ ] Servidor dedicado sobe sem crash (renderer só no client).

## Fora de escopo
Herança de traços, reprodução do wisp, EP por nível (F3), cor das placas e glifos em pixel art (F4).

## Dúvidas do implementador
Nenhuma dúvida.

## Relatório de implementação
- **Arquivos criados/alterados/removidos:**
  - `src/main/resources/data/wispborn/wisp_mob_levels/vanilla.json`: tabela inicial com as 77 espécies vanilla e faixas de nível da spec.
  - `src/main/java/dev/eder/wispborn/traits/`: criados `Sex`, `MobTraits`, `MobLevelRange` (`CODEC` validando `min >= 1` e `max >= min`), `MobLevelTable` (`SimpleJsonResourceReloadListener` sobre `wisp_mob_levels` somando arquivos em ordem de carregamento e ignorando entradas inválidas com `WARN`), `MobTraitRoller`, `BreedingRules` e `MobTraitsManager`.
  - `src/main/java/dev/eder/wispborn/registry/ModAttachments.java` e `ModTags.java`: registrado `MOB_TRAITS` (`MobTraits.CODEC`) e removidas as tags `TIER_1..3` (assim como `TierResolver.java`, `TierResolverTest.java` e `data/wispborn/tags/entity_type/tier_*.json`).
  - `src/main/java/dev/eder/wispborn/config/{WispbornConfig,WispbornClientConfig}.java`: curva `levels.thresholds` expandida até Nv 20 (`136800`); removidos `tiers.minLevel` e `mana.infestCostPerTier`; adicionados `infest.levelGateMargin` (`0`), `mana.infestCostPerLevel` (`5`), `traits.femaleChance` (`0.5`), `ep.levelGrace` (`2`), `ep.falloffPerLevel` (`0.25`) e `client.nameplateRange` (`8`, `ModConfig.Type.CLIENT`).
  - `src/main/java/dev/eder/wispborn/progress/{EpMath,ProgressManager}.java`: criado `EpMath.levelFactor` e removido o sufixo `wispborn.level_up.tier_unlocked`.
  - `src/main/java/dev/eder/wispborn/host/{InfestRules,HostManager}.java`: `InfestRules.check` migrado de tier para nível + margem e custo `infestCostPerLevel × mobLevel`; `HostManager` atualizado (incluindo resolução de `PartEntity` para o Ender Dragon e jogadores como `IMMUNE` no indicador de alvo).
  - `src/main/java/dev/eder/wispborn/network/{SyncMobTraitsPayload,TargetInfoPayload,ModPayloads}.java` e `client/ClientPayloadHandler.java`: sincronização S2C de traços e atualização de `TargetInfoPayload`.
  - `src/main/java/dev/eder/wispborn/event/TraitEvents.java`: registro de `MobLevelTable` em `AddReloadListenerEvent`, sorteio em `EntityJoinLevelEvent` (`!hasData`), sync em `StartTracking` e rede de segurança em `BabyEntitySpawnEvent`.
  - `src/main/java/dev/eder/wispborn/mixin/{BreedGoalMixin,AnimalMakeLoveMixin}.java`, `src/main/resources/wispborn.mixins.json` e `neoforge.mods.toml`: `@WrapOperation` em `BreedGoal#getFreePartner` e nas duas chamadas de `Animal#canMate` em `AnimalMakeLove` (`canStillUse` e `lambda$findValidBreedPartner$0`).
  - `src/main/java/dev/eder/wispborn/client/{MobNameplateRenderer,WispRenderHandler}.java`: placa billboard `♀/♂ Nv X` em `RenderLivingEvent.Post` até `client.nameplateRange` com linha de visão (posicionada acima do nome customizado, escondida no F1 e no hospedeiro atual) e HUD de alvo atualizado.
  - `src/main/java/dev/eder/wispborn/command/WispbornCommand.java` e `lang/{en_us,pt_br}.json`: adicionados `/wispborn traits <entity>` e `/wispborn traits <entity> set <level> <male|female>`; removido `Max Tier` de `/wispborn info`.
  - `src/test/java/dev/eder/wispborn/`: criados `MobTraitRollerTest`, `BreedingRulesTest`, `MobLevelRangeTest`, `EpMathTest` e atualizados `InfestRulesTest`, `LevelCurveTest` e `PossessionPayloadsTest`.
- **Decisões tomadas:**
  - `TargetInfoPayload` inclui `wispLevel` como 6º campo (mantendo construtor sobrecarregado de 5 parâmetros) porque no client o progresso do wisp só será sincronizado globalmente na `F4` (`SyncWispStatePayload`), e o painel de mira renderiza `wispborn.infest.too_strong` (`Forte demais (Nv %s; você Nv %s)`) no client antes do jogador apertar `R`.
  - Em `HostManager.computeTargetInfo`, entidades multipart (`PartEntity`, como `EnderDragonPart`) são resolvidas para o `Mob` pai e jogadores vivos na mira retornam `InfestResult.IMMUNE`, garantindo feedback visual consistente de imunidade para Ender Dragon, Warden e jogadores.
- **Saída final de `./gradlew build`:**
  ```text
  > Task :jar
  > Task :assemble
  > Task :build
  BUILD SUCCESSFUL in 3s
  ```
- **Saída final de `./gradlew test`:**
  ```text
  > Task :compileJava
  > Task :classes
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  BUILD SUCCESSFUL in 10s
  ```
- **Saída final de `./gradlew runServer`:**
  ```text
  [00:11:06] [main/INFO] [de.ed.wi.WispbornMod/]: Carregadas 77 faixas de nível de mobs em 'wisp_mob_levels'
  [00:11:08] [Server thread/INFO] [minecraft/DedicatedServer]: Done (1.112s)! For help, type "help"
  [00:11:08] [Server thread/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```
- **Pendências:**
  - Validação do checklist manual in-game por Eder (`runClient` / `runClient2`).
