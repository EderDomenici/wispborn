# Fatia 2 — Assimilação e Bestiário (F7) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consumir avança a trilha da espécie e a Trilha do Parasita; marcos descobrem cores, padrões, genes e
habilidades; genes e habilidades são liberados com EP no Bestiário; possuir passa a exigir as adaptações que a
espécie declara; um aviso aparece a cada descoberta.

**Architecture:** Definições em JSON de dados (cinco diretórios, um leitor genérico `JsonDataTable<T>`),
espelhadas no cliente por um snapshot sincronizado no login e no `/reload`. Estado do jogador no attachment
`ASSIMILATION`. Regras puras em `AssimilationRules` e `ReachRules`; `AssimilationManager` liga ao consumo e ao
pedido de liberar. O grimório ganha páginas (`GrimoirePage`); a aba Wisp vira `WispPage` e entra a `BestiaryPage`.

**Tech Stack:** Minecraft 1.21.1, NeoForge 21.1.x, Java 21, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-20-fagogenia-design.md` §2, §5.6–5.7 (só a chave de possessão e a
regra de alcance), §8 (Bestiário, aviso de descoberta). Arte: `docs/prompts/A5-fagogenia-art.md` (Bestiário e
`discovery_toast`, confirmados aqui como contrato).

**Branch:** `feat/F7-assimilacao`, da `main` com a F6 mergeada.

## Global Constraints

- Mesmas do plano da fatia 1 (`2026-11-21-fatia1-fundacao-v2.md`): stack fixa, servidor decide, regras puras
  testadas, números em config ou JSON, lang nos dois arquivos, só `blitSprite` e fonte do jogo, nenhum `fill`.
- Marcos, preços, exigências e efeitos em JSON de dados (spec, Restrições).
- Novo pacote `dev.eder.wispborn.assimilation` (atualizar a árvore de pacotes do AGENTS.md no Task 7).

## Review Focus

1. **JSON quebrado num datapack** (campo faltando, id inexistente, marcos fora de ordem): o arquivo é ignorado
   com aviso no log, o resto carrega e o jogo não cai. Teste no Task 1.
2. **`/reload` com o grimório aberto:** o cliente recebe o snapshot novo e a página redesenha sem exceção
   (definição que sumiu = linha some). Conferência no Task 6.
3. **Pedido de liberar forjado** (item não descoberto, grau pulado, sem EP, cor): o servidor recusa e
   ressincroniza. Teste no Task 2.
4. **Consumir a mesma espécie depois do último marco:** a contagem continua subindo e nada é descoberto de novo.
   Teste no Task 2.
5. **Espécie sem JSON** (mob de outro mod, por exemplo): conta para a Trilha do Parasita e aparece no Bestiário
   sem marcos. Teste no Task 2.

## Passo zero (antes do Task 1)

- [ ] Confira na `main` que existem, com estas assinaturas, as peças da F6 que este plano usa:
  `ProgressManager.epBalance(ServerPlayer)`, `WispProgress.withEp(int)`, `ProgressManager.syncState(ServerPlayer, boolean)`,
  `HostManager.onHostConsumed(ServerPlayer, Mob)`, `InfestRules.check(..., float mana, int minMana)`,
  `GrimoireScreen` com o botão Subir nível. Se algo divergir, pare e anote em "Dúvidas do implementador" da
  `docs/specs/F7-assimilacao.md` (criada no Task 7; crie o arquivo já, só com essa seção).

---

## Arquivos

| Arquivo | Responsabilidade | Task |
|---|---|---|
| `data/JsonDataTable.java` | leitor genérico de um diretório de JSON com um `Codec<T>` | 1 |
| `assimilation/UnlockKind.java`, `Unlockable.java`, `Milestone.java`, `Requirement.java`, `SpeciesDef.java`, `AbilityType.java`, `AbilityDegree.java`, `AbilityDef.java`, `GeneSlot.java`, `GeneDef.java`, `ColorDef.java`, `ParasiteTrailDef.java` | definições e codecs | 1 |
| `assimilation/WispData.java`, `WispDataSnapshot.java` | tabelas do servidor e snapshot sincronizável | 1, 3 |
| `assimilation/Assimilation.java`, `AssimilationRules.java`, `UnlockResult.java`, `ConsumeOutcome.java`, `ReachRules.java`, `DegreeNames.java` | estado e regras puras | 2 |
| `assimilation/AssimilationManager.java`, `event/AssimilationEvents.java` | liga ao jogo | 3 |
| `network/SyncWispDataPayload.java`, `SyncAssimilationPayload.java`, `DiscoveryPayload.java`, `UnlockPayload.java` | rede | 3 |
| `host/InfestResult.java`, `InfestRules.java`, `HostManager.java` (mod.) | chave de adaptação | 4 |
| `client/ClientWispData.java`, `client/ClientAssimilation.java`, `client/DiscoveryToast.java` | espelho e aviso | 5 |
| `client/grimoire/GrimoirePage.java`, `WispPage.java`, `BestiaryPage.java`, `GrimoireScreen.java` (mod.), `GrimoireSprites.java` (mod.) | páginas | 6 |
| `data/wispborn/wisp_species/*.json`, `wisp_abilities/*.json`, `wisp_genes/*.json`, `wisp_colors/*.json`, `wisp_parasite_trail/parasite.json` | primeira leva de conteúdo | 1 |

---

### Task 1: Definições em JSON e a primeira leva de conteúdo

**Files:**
- Create: os arquivos de definição acima, `data/JsonDataTable.java`, `assimilation/WispData.java`, os JSON
- Test: `src/test/java/dev/eder/wispborn/assimilation/DefinitionCodecTest.java`, `src/test/java/dev/eder/wispborn/data/JsonDataTableTest.java`

**Interfaces:**
- Produces:
  - `enum UnlockKind { COLOR, PATTERN, GENE, ABILITY }` (`StringRepresentable`, nomes em minúsculas; `isFree()` = COLOR ou PATTERN)
  - `record Unlockable(UnlockKind kind, ResourceLocation id, int degree)`: `degree` só vale para ABILITY (≥ 1); nos outros é 0. `CODEC` (`degree` opcional, padrão 0) e `STREAM_CODEC`.
  - `record Milestone(int count, Unlockable discovers)` (campo JSON `discovers`)
  - `record Requirement(ResourceLocation ability, int degree)`
  - `record SpeciesDef(ResourceLocation entity, List<Requirement> requires, List<Milestone> milestones)`. O codec
    valida: `count >= 1`, contagens estritamente crescentes. `requires` e `milestones` são opcionais (lista vazia).
  - `enum AbilityType { ACTIVE, PASSIVE, ADAPTATION }`
  - `record AbilityDegree(int price, Map<String, Double> params)` (`params` opcional, vazio; a fatia 3 usa)
  - `record AbilityDef(AbilityType type, List<AbilityDegree> degrees, List<ResourceLocation> requires)` (`degrees` com pelo menos 1)
  - `enum GeneSlot { LOCOMOTION, COVERING, BREATHING, WEAPON, SENSES }`; `record GeneDef(GeneSlot slot, int complexity, int price)`
  - `record ColorDef(int rgb)`: o codec lê e grava `"#RRGGBB"`
  - `record ParasiteTrailDef(List<Milestone> milestones)` (mesma validação de contagem)
  - `final class JsonDataTable<T> extends SimpleJsonResourceReloadListener`: `JsonDataTable(String directory, Codec<T> codec)`,
    `Map<ResourceLocation, T> entries()`, e o estático puro `parse(Map<ResourceLocation, JsonElement> files, Codec<T> codec, String directory): Map<ResourceLocation, T>`.
    Arquivo inválido: aviso no log com o id e a mensagem do codec, e segue. Mesmo padrão de log de `MobLevelTable`.
    A chave é o id do arquivo (`wispborn:spider` para `data/wispborn/wisp_species/spider.json`).
  - `WispData`: `SPECIES`, `ABILITIES`, `GENES`, `COLORS`, `PARASITE_TRAIL` (cada um `JsonDataTable`, diretórios
    `wisp_species`, `wisp_abilities`, `wisp_genes`, `wisp_colors`, `wisp_parasite_trail`);
    `speciesFor(EntityType<?>): Optional<SpeciesDef>` (índice por `entity`, refeito no `apply`);
    `parasiteTrail(): ParasiteTrailDef` (arquivo `wispborn:parasite`, vazio se faltar).
    Os cinco listeners são registrados em `TraitEvents.onAddReloadListeners`, depois do `MobLevelTable`.

Formato (exemplo de `wisp_species/spider.json`):

```json
{
  "entity": "minecraft:spider",
  "milestones": [
    { "count": 3,  "discovers": { "kind": "color",   "id": "wispborn:spider_black" } },
    { "count": 5,  "discovers": { "kind": "ability", "id": "wispborn:web", "degree": 1 } },
    { "count": 8,  "discovers": { "kind": "gene",    "id": "wispborn:spider_legs" } },
    { "count": 12, "discovers": { "kind": "ability", "id": "wispborn:web", "degree": 2 } }
  ]
}
```

Primeira leva (decisão do Claude; os números são ponto de partida, todos em JSON):

| Arquivo | Conteúdo |
|---|---|
| `wisp_abilities/web.json` | ACTIVE, graus `[400, 900]` |
| `wisp_abilities/heat.json` | ADAPTATION, graus `[300, 800]` (Calor I, II) |
| `wisp_abilities/cold.json` | ADAPTATION, graus `[300, 800]` (Frio I, II) |
| `wisp_abilities/take_over.json` | PASSIVE, grau `[1500]` (Tomar conta do corpo) |
| `wisp_abilities/body_jump.json` | ACTIVE, grau `[3000]`, `requires: ["wispborn:take_over"]` (Pular de corpo) |
| `wisp_genes/` | `spider_legs` (LOCOMOTION, 2, 600), `spider_fangs` (WEAPON, 2, 600), `cow_hide` (COVERING, 1, 300), `cod_gills` (BREATHING, 1, 300), `bat_ears` (SENSES, 1, 300) |
| `wisp_colors/` | `spider_black #1E1B22`, `chicken_cream #EFE3C2`, `cow_brown #6B4A2E`, `pig_pink #E8A0A0`, `sheep_wool #E9E6DD`, `husk_sand #C9B27A`, `stray_frost #BFD7DE`, `magma_ember #E0622C`, `blaze_flame #F2B234`, `bee_amber #E8B23A`, `slime_green #7FBF4F`, `polar_white #F4F1EA`, `parasite_violet #6A4C8C` |
| `wisp_species/` | `chicken` (10 cor creme), `cow` (8 cor marrom; 16 gene cow_hide), `pig` (8 cor rosa), `sheep` (8 cor lã; 16 padrão `wispborn:stripes`), `cod` (5 gene cod_gills), `bat` (5 gene bat_ears), `spider` (o exemplo acima + 16 gene spider_fangs), `husk` (3 Calor I; 6 cor areia), `stray` (3 Frio I; 6 cor gelo), `magma_cube` (requires Calor I; 3 Calor II; 5 cor brasa; 8 padrão `wispborn:veins`), `blaze` (requires Calor II; 3 cor chama), `polar_bear` (requires Frio I; 3 Frio II; 5 cor branco-polar), `bee` (5 cor âmbar; 8 padrão `wispborn:stripes`), `slime` (5 cor verde; 8 padrão `wispborn:gradient`) |
| `wisp_parasite_trail/parasite.json` | 3 padrão `wispborn:rings`; 5 Tomar conta do corpo; 8 cor violeta; 10 Pular de corpo |

Padrões são só ids nesta fatia (`wispborn:solid`, `stripes`, `spots`, `veins`, `gradient`, `rings`); a fatia 4
dá a matemática e valida os ids.

- [ ] **Step 1: Write the failing tests**

```java
// DefinitionCodecTest
@Test void speciesParsesTheExampleFile() { /* o JSON do spider acima → SpeciesDef com 4 marcos, requires vazio */ }
@Test void milestonesMustBeStrictlyIncreasing() { /* contagens 5, 3 → DataResult com erro */ }
@Test void colorReadsAndWritesHex() { /* "#E0622C" ↔ ColorDef(0xE0622C); "E0622C" e "#GG0000" → erro */ }
@Test void unlockableDegreeDefaultsToZero() { /* {"kind":"color","id":"wispborn:x"} → degree 0 */ }
@Test void abilityNeedsAtLeastOneDegree() { /* degrees: [] → erro */ }

// JsonDataTableTest
@Test void invalidFileIsSkippedAndTheRestLoads() {
    // dois arquivos, um válido e um sem "entity": parse devolve só o válido
}
@Test void shippedDataParsesWithoutErrors() {
    // lê src/main/resources/data/wispborn/<cada diretório>/*.json do disco, faz parse com o codec de cada tabela
    // e confere que nenhum arquivo foi descartado (contagem de entrada = contagem de saída)
}
```

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew test --tests "dev.eder.wispborn.assimilation.*" --tests "dev.eder.wispborn.data.*"`
Expected: FAIL de compilação.

- [ ] **Step 3: Implement** os tipos, o `JsonDataTable`, o `WispData`, o registro dos listeners e os JSON da tabela.

- [ ] **Step 4: Run them and confirm they pass** (mesmo comando).

- [ ] **Step 5: Commit**

```bash
git add -A src/
git commit -m "feat(assimilation): definicoes em JSON e a primeira leva de especies, habilidades, genes e cores"
```

---

### Task 2: Estado e regras puras da assimilação

**Files:**
- Create: `assimilation/Assimilation.java`, `AssimilationRules.java`, `UnlockResult.java`, `ConsumeOutcome.java`, `ReachRules.java`, `DegreeNames.java`
- Test: `src/test/java/dev/eder/wispborn/assimilation/AssimilationRulesTest.java`, `ReachRulesTest.java`, `AssimilationTest.java`, `DegreeNamesTest.java`

**Interfaces:**
- Consumes: os tipos do Task 1.
- Produces:
  - `record Assimilation(Map<ResourceLocation, Integer> consumed, Set<Unlockable> discovered, Set<Unlockable> unlocked)`:
    imutável (cópias no construtor), `EMPTY`, `CODEC` (listas no JSON), `STREAM_CODEC`, `distinctSpecies(): int`
    (= espécies com contagem ≥ 1)
  - `record ConsumeOutcome(Assimilation state, List<Unlockable> discovered)`
  - `AssimilationRules.onConsume(Assimilation state, ResourceLocation entityId, Optional<SpeciesDef> species, ParasiteTrailDef trail): ConsumeOutcome`.
    Soma 1 à contagem da espécie. Descobre os marcos cuja `count` é igual à nova contagem. Se a espécie foi
    consumida pela primeira vez, faz o mesmo com a Trilha do Parasita usando o novo `distinctSpecies()`. COLOR e
    PATTERN descobertos entram também em `unlocked`. Já descoberto não entra de novo em `discovered`.
  - `enum UnlockResult { OK, NOT_DISCOVERED, ALREADY_UNLOCKED, FREE, MISSING_PREVIOUS_DEGREE, MISSING_PREREQUISITE, NOT_ENOUGH_EP, UNKNOWN }`
  - `AssimilationRules.checkUnlock(Assimilation state, Unlockable item, OptionalInt price, List<ResourceLocation> prerequisites, int epBalance): UnlockResult`.
    Ordem: preço vazio → UNKNOWN; COLOR ou PATTERN → FREE; não descoberto → NOT_DISCOVERED; já liberado →
    ALREADY_UNLOCKED; ABILITY com `degree > 1` e sem o grau anterior → MISSING_PREVIOUS_DEGREE; algum pré-requisito
    sem grau ≥ 1 → MISSING_PREREQUISITE; saldo < preço → NOT_ENOUGH_EP; senão OK.
  - `AssimilationRules.unlock(Assimilation state, Unlockable item): Assimilation`
  - `AssimilationRules.abilityDegree(Assimilation state, ResourceLocation ability): int` (maior grau liberado, 0 se nenhum)
  - `AssimilationRules.missingRequirement(Assimilation state, List<Requirement> requires): Optional<Requirement>` (o primeiro não atendido)
  - `ReachRules.unreachable(Collection<SpeciesDef> species, ParasiteTrailDef trail): Set<Requirement>`. Ponto
    fixo: começa com as habilidades da Trilha do Parasita; repete enquanto crescer: toda espécie cujos `requires`
    estão cobertos pelos graus já alcançados contribui com as habilidades dos seus marcos. Devolve as exigências
    de possessão que nunca ficam cobertas.
  - `DegreeNames.roman(int degree): String` (1 → `I` … 5 → `V`; fora de 1..5, o número em decimal)

- [ ] **Step 1: Write the failing tests**

```java
// AssimilationRulesTest (helpers: spider() com os marcos do Task 1, trail() com 3/5/8/10)
@Test void milestoneDiscoversExactlyOnTheCount() {
    // 4 consumos de aranha → nada de habilidade; o 5º → discovered contém Unlockable(ABILITY, web, 1)
}
@Test void colorsAndPatternsUnlockForFree() {
    // 3º consumo de aranha → spider_black em discovered E em unlocked
}
@Test void consumingPastTheLastMilestoneDiscoversNothingNew() {
    // 20 consumos de aranha; do 13º em diante, ConsumeOutcome.discovered() vazio e contagem = 20
}
@Test void speciesWithoutJsonCountsForTheParasiteTrail() {
    // três espécies sem SpeciesDef, uma vez cada → o 3º consumo descobre o padrão wispborn:rings
}
@Test void parasiteTrailCountsDistinctSpeciesNotConsumptions() {
    // 10 consumos da mesma espécie → distinctSpecies() == 1 e nada da trilha do parasita além do marco 1 (não há)
}
@Test void unlockFollowsThePrecedence() {
    // FREE para cor; NOT_DISCOVERED; ALREADY_UNLOCKED; MISSING_PREVIOUS_DEGREE (web 2 sem web 1);
    // MISSING_PREREQUISITE (body_jump sem take_over); NOT_ENOUGH_EP (399 para 400); OK (400); UNKNOWN (preço vazio)
}
@Test void abilityDegreeIsTheHighestUnlocked() { /* web 1 e 2 liberados → 2; nada → 0 */ }
@Test void missingRequirementReturnsTheFirstGap() { /* Calor I liberado, exige Calor II → Optional.of(Calor II) */ }

// ReachRulesTest
@Test void chainedAdaptationsAreReachable() {
    // husk (sem requires, dá Calor I) + magma_cube (exige Calor I, dá Calor II) + blaze (exige Calor II) → vazio
}
@Test void adaptationWithOnlyLockedSourcesIsReported() {
    // blaze exige Calor II e ninguém dá Calor II → { Requirement(heat, 2) }
}
@Test void shippedSpeciesRespectTheReachRule() {
    // carrega os JSON reais (mesmo leitor do JsonDataTableTest) → unreachable(...) vazio
}

// AssimilationTest: CODEC e STREAM_CODEC ida e volta com consumed, discovered e unlocked preenchidos
// DegreeNamesTest: 1 → "I", 2 → "II", 4 → "IV", 5 → "V", 7 → "7"
```

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew test --tests "dev.eder.wispborn.assimilation.*"`
Expected: FAIL de compilação.

- [ ] **Step 3: Implement** as assinaturas acima.

- [ ] **Step 4: Run them and confirm they pass** (mesmo comando).

- [ ] **Step 5: Commit**

```bash
git add -A src/
git commit -m "feat(assimilation): estado, marcos, liberar com EP e regra de alcance"
```

---

### Task 3: Ligar ao jogo (attachment, consumo, liberar, sincronização)

**Files:**
- Create: `assimilation/AssimilationManager.java`, `assimilation/WispDataSnapshot.java`, `event/AssimilationEvents.java`,
  `network/SyncWispDataPayload.java`, `SyncAssimilationPayload.java`, `DiscoveryPayload.java`, `UnlockPayload.java`
- Modify: `registry/ModAttachments.java`, `host/HostManager.java` (`onHostConsumed`), `network/ModPayloads.java`,
  `WispbornMod.java`, `command/WispbornCommand.java`, lang
- Test: `src/test/java/dev/eder/wispborn/network/AssimilationPayloadsTest.java`

**Interfaces:**
- Consumes: Tasks 1 e 2; `ProgressManager.epBalance`, `withEp`, `syncState`.
- Produces:
  - `ModAttachments.ASSIMILATION: Supplier<AttachmentType<Assimilation>>`, padrão `Assimilation.EMPTY`, `serialize(CODEC)`, `copyOnDeath()`
    (spec §2.6: tudo fica ao morrer)
  - `record WispDataSnapshot(Map<ResourceLocation, SpeciesDef> species, Map<ResourceLocation, AbilityDef> abilities, Map<ResourceLocation, GeneDef> genes, Map<ResourceLocation, ColorDef> colors, ParasiteTrailDef parasiteTrail, Map<ResourceLocation, MobLevelRange> levelRanges)`
    com `CODEC` e `WispDataSnapshot.fromServer()` (lê `WispData` e `MobLevelTable`)
  - `SyncWispDataPayload(WispDataSnapshot)`, `STREAM_CODEC = ByteBufCodecs.fromCodec(WispDataSnapshot.CODEC)` (servidor → cliente)
  - `SyncAssimilationPayload(Assimilation)` (servidor → cliente)
  - `DiscoveryPayload(List<Unlockable> items)` (servidor → cliente)
  - `UnlockPayload(Unlockable item)` (cliente → servidor) → `AssimilationManager.unlock`
  - `AssimilationManager.onConsumed(ServerPlayer, Mob)`, `AssimilationManager.unlock(ServerPlayer, Unlockable): UnlockResult`,
    `AssimilationManager.state(ServerPlayer): Assimilation`, `AssimilationManager.priceOf(Unlockable): OptionalInt`
    (ABILITY: preço do grau no `AbilityDef`; GENE: `GeneDef.price`; COLOR/PATTERN: `OptionalInt.of(0)` se o id existir
    (PATTERN aceita qualquer id nesta fatia)), `AssimilationManager.sync(ServerPlayer)`

- [ ] **Step 1: Write the failing test**

```java
// AssimilationPayloadsTest: ida e volta de SyncAssimilationPayload, DiscoveryPayload (2 itens) e UnlockPayload;
// SyncWispDataPayload com um snapshot de 1 espécie, 1 habilidade, 1 gene, 1 cor, a trilha e 1 faixa de nível
```

- [ ] **Step 2: Run it and confirm it fails**

Run: `./gradlew test --tests dev.eder.wispborn.network.AssimilationPayloadsTest`
Expected: FAIL de compilação.

- [ ] **Step 3: Implement**
  - `HostManager.onHostConsumed`: depois do EP e da fome, `AssimilationManager.onConsumed(player, mob)`. Vale
    também com fator de EP 0 (spec §2.1).
  - `onConsumed`: `onConsume(state, id do EntityType, WispData.speciesFor(type), WispData.parasiteTrail())`; grava;
    se houver descobertas, manda `DiscoveryPayload`; `sync`.
  - `unlock`: só com `FormManager.isParasite`; `checkUnlock` com `priceOf`, os `requires` do `AbilityDef` (lista
    vazia para os outros tipos) e o saldo; em OK desconta o EP (`withEp(saldo - preço)`), grava o `unlock` e manda
    `wispborn.unlock.ok`; em falha, `wispborn.unlock.<resultado em minúsculas>` na barra de ação e `sync`. Os dois
    casos terminam com `ProgressManager.syncState(player, true)`.
  - `AssimilationEvents`: no login, `SyncWispDataPayload(fromServer())` e `sync`; em `OnDatapackSyncEvent`, o
    snapshot para `event.getRelevantPlayers()`.
  - O núcleo de `onConsumed` fica em `AssimilationManager.onConsumedType(ServerPlayer, EntityType<?>, int times)`,
    que `onConsumed(player, mob)` chama com `times = 1`. Comandos de teste:
    `/wispborn assimilation add <entity> <n> [player]` (argumento `ResourceArgument.resource(context, Registries.ENTITY_TYPE)`)
    chama `onConsumedType`; `/wispborn assimilation reset [player]` grava `Assimilation.EMPTY` e sincroniza.

  Lang:

  | Chave | pt_br | en_us |
  |---|---|---|
  | `wispborn.unlock.ok` | `Liberado: %s` | `Unlocked: %s` |
  | `wispborn.unlock.not_discovered` | `Ainda não descoberto.` | `Not discovered yet.` |
  | `wispborn.unlock.already_unlocked` | `Já liberado.` | `Already unlocked.` |
  | `wispborn.unlock.free` | `Este já é seu.` | `This is already yours.` |
  | `wispborn.unlock.missing_previous_degree` | `Libere o grau anterior primeiro.` | `Unlock the previous degree first.` |
  | `wispborn.unlock.missing_prerequisite` | `Falta um pré-requisito.` | `A prerequisite is missing.` |
  | `wispborn.unlock.not_enough_ep` | `EP insuficiente.` | `Not enough EP.` |
  | `wispborn.unlock.unknown` | `Item desconhecido.` | `Unknown item.` |
  | `commands.wispborn.assimilation.add.success` | `%s consumos de %s registrados para %s` | `Recorded %s %s consumed for %s` |
  | `commands.wispborn.assimilation.reset.success` | `Assimilação de %s zerada` | `Reset %s assimilation` |
  | nomes | `wispborn.ability.<ns>.<path>`, `wispborn.gene.<ns>.<path>`, `wispborn.color.<ns>.<path>`, `wispborn.pattern.<ns>.<path>` para todo o conteúdo do Task 1 e os seis padrões | idem |

  Nomes pt_br: Teia, Calor, Frio, Tomar conta do corpo, Pular de corpo; Patas de aranha, Presas de aranha, Couro
  de vaca, Guelras de bacalhau, Ouvidos de morcego; Preto-aranha, Creme, Marrom, Rosa, Lã, Areia, Gelo, Brasa,
  Chama, Âmbar, Verde-limo, Branco-polar, Violeta; Liso, Listras, Manchas, Veios, Gradiente, Anéis.

- [ ] **Step 4: Run the full build and tests**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add -A src/
git commit -m "feat(assimilation): consumo avanca trilhas, liberar com EP e sincronizacao com o cliente"
```

---

### Task 4: Chave de adaptação na possessão

**Files:**
- Modify: `host/InfestResult.java`, `host/InfestRules.java`, `host/HostManager.java`, lang
- Test: `src/test/java/dev/eder/wispborn/host/InfestRulesTest.java`, `network/PossessionPayloadsTest.java`

**Interfaces:**
- Produces:
  - `InfestResult.MISSING_ADAPTATION` (entra antes de `NO_MANA`; confira que o `STREAM_CODEC` do enum continua por ordinal e ajuste o teste de round-trip)
  - `InfestRules.check(boolean isMob, boolean alive, boolean hasPassenger, Optional<MobTraits> traits, int wispLevel, int levelGateMargin, boolean adaptationsMet, float mana, int minMana): InfestResult`
    Ordem: NOT_A_MOB, HAS_PASSENGER, IMMUNE, TOO_STRONG, MISSING_ADAPTATION, NO_MANA, OK.

- [ ] **Step 1: Write the failing test**

```java
@Test void missingAdaptationComesAfterLevelAndBeforeMana() {
    Optional<MobTraits> lv3 = Optional.of(new MobTraits(3, Sex.MALE));
    assertEquals(InfestResult.TOO_STRONG, InfestRules.check(true, true, false, lv3, 2, 0, false, 0.0F, 20));
    assertEquals(InfestResult.MISSING_ADAPTATION, InfestRules.check(true, true, false, lv3, 3, 0, false, 0.0F, 20));
    assertEquals(InfestResult.NO_MANA, InfestRules.check(true, true, false, lv3, 3, 0, true, 0.0F, 20));
    assertEquals(InfestResult.OK, InfestRules.check(true, true, false, lv3, 3, 0, true, 20.0F, 20));
}
```

Ajuste as chamadas antigas do teste para `adaptationsMet = true`.

- [ ] **Step 2: Run it and confirm it fails**

Run: `./gradlew test --tests dev.eder.wispborn.host.InfestRulesTest`
Expected: FAIL de compilação.

- [ ] **Step 3: Implement**: em `tryInfest`, na rechecagem do `tick` e em `computeTargetInfo`,
  `adaptationsMet = AssimilationRules.missingRequirement(state, WispData.speciesFor(type).map(SpeciesDef::requires).orElse(List.of())).isEmpty()`.
  `sendInfestFailure` em `MISSING_ADAPTATION`: `wispborn.infest.missing_adaptation` com o nome da habilidade e
  `DegreeNames.roman(grau)`. O cartão do alvo já fica vermelho por não ser OK.

  Lang: `wispborn.infest.missing_adaptation` — pt_br `Exige %s %s` · en_us `Requires %s %s`.

- [ ] **Step 4: Run the full build and tests** (`./gradlew build`, verde).

- [ ] **Step 5: Commit**

```bash
git add -A src/
git commit -m "feat(host): possuir exige as adaptacoes declaradas pela especie"
```

---

### Task 5: Espelho no cliente e aviso de descoberta

**Files:**
- Create: `client/ClientWispData.java`, `client/ClientAssimilation.java`, `client/DiscoveryToast.java`, sprite `hud/discovery_toast.png` + `.mcmeta`
- Modify: `client/ClientPayloadHandler.java`, `network/ModPayloads.java`, `client/hud/HudSprites.java`, lang

**Interfaces:**
- Consumes: os payloads do Task 3.
- Produces:
  - `ClientWispData.get(): WispDataSnapshot` (vazio até o primeiro pacote; `reset` no logout, como `ClientWispState`)
  - `ClientAssimilation.get(): Assimilation` (mesmo ciclo)
  - `DiscoveryToast implements Toast`, 160×32: `discovery_toast` em nine-slice, título `wispborn.toast.discovered` +
    nome do item na primeira linha, e `wispborn.toast.unlock_hint` (gene, habilidade) ou `wispborn.toast.free` (cor,
    padrão) na segunda. Fica 5000 ms. Os itens de um mesmo `DiscoveryPayload` viram um toast cada, pelo
    `Minecraft.getInstance().getToasts().addToast`.
  - `ClientPayloadHandler`: os três handlers (guardar o snapshot, guardar o estado, abrir os toasts). O aviso ao
    grimório aberto entra no Task 6.

Nome de exibição: `UnlockNames.displayName(Unlockable): Component` (cliente e servidor) = a chave de lang do tipo +
` ` + `DegreeNames.roman(degree)` para ABILITY. Crie em `assimilation/UnlockNames.java` com teste
`UnlockNamesTest.abilityShowsTheDegree` (o `Component` traduzível tem a chave `wispborn.ability.wispborn.web` e o
sufixo ` II`).

Lang: `wispborn.toast.discovered` `Descoberto: %s` / `Discovered: %s`; `wispborn.toast.unlock_hint`
`Libere no grimório` / `Unlock it in the grimoire`; `wispborn.toast.free` `Já disponível na Aparência` /
`Ready in Appearance`.

Sprite `hud/discovery_toast`: da A5 ou placeholder copiado de `hud/target_frame.png` (+ `.mcmeta`).

- [ ] **Step 1: Write the failing test** (`UnlockNamesTest`), **run** (`./gradlew test --tests dev.eder.wispborn.assimilation.UnlockNamesTest`, FAIL),
  **implement**, **run again** (PASS).
- [ ] **Step 2: Build** (`./gradlew build`, verde) e **confira no jogo**: `/wispborn assimilation add minecraft:spider 5`
  mostra dois toasts (Preto-aranha, Teia I).
- [ ] **Step 3: Commit**

```bash
git add -A src/
git commit -m "feat(client): espelho da assimilacao e aviso de descoberta"
```

---

### Task 6: Grimório em páginas e a aba Bestiário

**Files:**
- Create: `client/grimoire/GrimoirePage.java`, `WispPage.java`, `BestiaryPage.java`, sprites do Bestiário
- Modify: `client/grimoire/GrimoireScreen.java`, `GrimoireSprites.java`, lang

**Interfaces:**
- Produces:
  - `interface GrimoirePage { void render(GuiGraphics g, Font font, int left, int top, int mouseX, int mouseY, float partialTick); boolean mouseClicked(double mouseX, double mouseY, int button, int left, int top); default boolean mouseScrolled(double mouseX, double mouseY, double scrollY, int left, int top) { return false; } default void onStateSynced(SyncWispStatePayload state) {} default void onDataSynced() {} }`
  - `WispPage implements GrimoirePage`: tudo o que hoje é a página do Wisp (retrato, barras, atributos, rodapé,
    botão Subir nível), movido sem mudar o comportamento. `GrimoireScreen` fica com moldura, título, abas e a página atual.
  - `enum GrimoireTab { WISP, BESTIARY, ABILITIES, APPEARANCE, BODY }` com a chave de lang e `Optional<Supplier<GrimoirePage>>`;
    sem página = aba cinza e sem clique. Nesta fatia: WISP e BESTIARY.
  - Clique numa aba com página troca a página atual; o retângulo de clique é o texto da aba + 2 px (largura do texto, altura 12).
  - `GrimoireScreen.onDataSynced()` repassa à página atual; `ClientPayloadHandler` chama esse método quando chega
    `SyncAssimilationPayload` ou `SyncWispDataPayload` com o grimório aberto (`mc.screen instanceof GrimoireScreen`).
  - Sprites (`grimoire/…`): `species_row`, `species_row_selected` (nine-slice, 100×14), `milestone_hidden`,
    `milestone_discovered`, `milestone_unlocked` (9×9), `req_ok`, `req_missing` (9×9). Da A5 ou placeholders:
    linhas de `button`/`button_hover`, marcos de `pip_empty`/`pip_pending`/`pip_filled` (9×9 desenhados em 9×9),
    exigências de `hud/target_ok`/`hud/target_blocked`.

Layout da `BestiaryPage` (relativo ao painel):
- Lista à esquerda, x 8..108, a partir de y 24, linhas de 14 px, até 11 visíveis, rolagem com a roda. A primeira
  linha é sempre a Trilha do Parasita (`wispborn.bestiary.parasite`, valor à direita = `distinctSpecies()`); depois,
  as espécies com contagem ≥ 1, em ordem alfabética do nome traduzido, com a contagem à direita (`×7`). Linha
  selecionada usa `species_row_selected`. Começa com a Trilha do Parasita selecionada.
- Painel direito, x 116..308:
  - y 26: nome (primário) e, à direita, `wispborn.bestiary.consumed` com a contagem.
  - y 38: exigências de possessão: `wispborn.bestiary.level_range` com a faixa (`MobLevelRange` do snapshot) e, para
    cada `Requirement`, ícone `req_ok`/`req_missing` + nome com grau. Sem exigências: `wispborn.bestiary.no_requirements`.
    Na Trilha do Parasita a linha mostra `wispborn.bestiary.parasite_hint`.
  - y 54 em diante: um marco por linha de 16 px, até 7 visíveis (rolagem própria): ícone de estado + `count` +
    nome (oculto: `???` em cinza; descoberto ou liberado: nome com o tipo entre parênteses, `wispborn.kind.<kind>`).
    Descoberto, pago e ainda não liberado: botão `Liberar · %s` de 64×14 em x 244, com `BUTTON_PRIMARY` se o
    saldo paga, `BUTTON_PRIMARY_DISABLED` se não. Clique (habilitado) → `UnlockPayload` + som de clique.
  - Rodapé, y 181: `wispborn.bestiary.balance` com o saldo de EP.
- Sem nenhuma espécie consumida, o painel mostra só a Trilha do Parasita e `wispborn.bestiary.empty` na lista.

Lang (pt_br / en_us): `wispborn.bestiary.parasite` Trilha do Parasita / Parasite Trail; `.consumed` Consumidos: %s /
Consumed: %s; `.level_range` Nv %s–%s / Lv %s–%s; `.no_requirements` Sem exigências / No requirements;
`.parasite_hint` Conta espécies diferentes / Counts different species; `.balance` Saldo: %s EP / Balance: %s EP;
`.empty` Consuma uma criatura / Consume a creature; `.unlock` Liberar · %s / Unlock · %s;
`wispborn.kind.color` cor / color; `.pattern` padrão / pattern; `.gene` gene / gene; `.ability` habilidade / ability.

- [ ] **Step 1: Implement** a divisão em páginas sem mudar a aba Wisp e **confira no jogo** que ela está igual
  (atributos, confirmar, Subir nível). **Commit:** `refactor(grimoire): paginas do grimorio, aba Wisp em WispPage`.
- [ ] **Step 2: Implement** a `BestiaryPage` e os sprites.
- [ ] **Step 3: Build** (`./gradlew build`, verde).
- [ ] **Step 4: Check in game:**
  - `/wispborn assimilation add minecraft:spider 5` e `/wispborn ep set 1000`: no Bestiário aparece a aranha ×5;
    os marcos 3 (liberado) e 5 (descoberto, `Liberar · 400`) aparecem e os outros como `???`.
  - Liberar desconta 400 e o marco fica liberado.
  - Possuir um cubo de magma sem Calor I falha com "Exige Calor I", e passa depois de liberar Calor I (husk ×3).
  - Rodar `/reload` com a aba aberta não derruba o jogo.
  - Em en_us, os textos cabem na tela.
- [ ] **Step 5: Commit**

```bash
git add -A src/
git commit -m "feat(grimoire): aba Bestiario com trilhas, marcos e liberar"
```

---

### Task 7: Documentação

- [ ] Crie/complete `docs/specs/F7-assimilacao.md` (aponta para este plano e para o design; "Dúvidas do
  implementador" e "Relatório de implementação" preenchidos, com os placeholders de sprite que faltam trocar),
  atualize `CHANGELOG.md`, a linha F7 do `docs/PLAN.md` e a árvore de pacotes e a linha "Specs:" do `AGENTS.md`
  (pacotes `assimilation/` e `data/`).
- [ ] `./gradlew build` verde, e commit: `docs(F7): relatorio da assimilacao, CHANGELOG e PLAN`.
