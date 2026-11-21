# Fatia 4 — Aparência (F9) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** O jogador pinta o wisp com as cores e os padrões que descobriu: um padrão matemático com uma cor por
vaga no núcleo, uma cor no halo e uma no rastro, com variação por indivíduo, vista por todos os jogadores.

**Architecture:** `PatternMath` (puro) decide qual vaga de cor cada pixel recebe; `WispTextureBaker` (puro, sobre
`int[]` ARGB) recolore a textura do modelo A3 usando uma máscara de partes; o cliente transforma o resultado em
`DynamicTexture` com cache. A escolha mora no attachment `APPEARANCE`, é validada no servidor e chega a quem está
vendo o jogador (como o `SyncFormPayload`).

**Tech Stack:** Minecraft 1.21.1, NeoForge 21.1.x, Java 21, GeckoLib 4.9.3, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-20-fagogenia-design.md` §6 (inteira), §8 (aba Aparência). Arte:
`docs/prompts/A5-fagogenia-art.md` (Aparência, máscara de partes e miniaturas de padrão).

**Branch:** `feat/F9-aparencia`, da `main` com a F8 mergeada **e a V1 (modelo A3) rebaseada, validada no jogo e
mergeada** (spec, Ordem de construção).

## Global Constraints

- Mesmas do plano da fatia 1, mais: sem shader (compatível com mods de shader, spec §6.6); a cor do wisp não
  entra em nenhum sprite de interface.
- APIs da GeckoLib conforme `.agents/rules/geckolib-4.9.md`; nada de API inventada.

## Decisões do Claude (spec §6 deixa em aberto)

- **Onde vai cada coisa:** o padrão e as vagas de cor valem para o **núcleo**. Halo e rastro recebem uma cor cada,
  porque são cascas translúcidas e linhas finas onde um padrão não aparece. "Rastro" = ossos `trail_1..4`, `wake`
  e os quatro `satellite_*` do A3.
- **Semente por indivíduo** = os 64 bits baixos do UUID do jogador. Não é salva (a prole, no futuro, terá a
  própria semente).
- **Sem aparência escolhida,** o wisp continua com a textura original do A3.

## Review Focus

1. **Cor liberada que sumiu do JSON depois de `/reload`,** com a aparência já salva: o servidor mantém a aparência,
   o cliente desenha a vaga com a cor original daquele pixel, sem exceção. Teste no Task 2.
2. **Trocar de aparência muitas vezes:** as texturas antigas são fechadas (`DynamicTexture.close` e
   `TextureManager.release`) e a memória não cresce. Conferência no Task 4.
3. **Dois jogadores com a mesma escolha:** os wisps ficam parecidos, mas não iguais (sementes diferentes). Teste no Task 1.
4. **Pacote forjado** (padrão bloqueado, cor não liberada, número errado de cores): o servidor recusa. Teste no Task 2.
5. **Resource pack que troque `wisp.png`:** o baker relê a textura no recarregamento de recursos. Conferência no Task 4.

## Passo zero

- [ ] Confira na `main`: geo do A3 com os ossos `core`, `halo_inner`, `halo_outer`, `trail_1..4`, `wake` e
  `satellite_1..4`, e atlas 64×64; `Assimilation`, `ClientAssimilation`, `ClientWispData` (cores com `ColorDef.rgb`),
  `GrimoireTab`/`GrimoirePage`, `WispRenderHandler.setRenderingGrimoirePortrait`. Confira também como o
  `WispPlayerRenderer` escolhe a textura e desenha a glowmask (`AutoGlowingGeoLayer` ou outra camada). Divergência →
  anote em `docs/specs/F9-aparencia.md`, seção "Dúvidas do implementador".
- [ ] Confira que a A5 entregou `art/parasita-magico/wisp_partmask.png` (64×64, vermelho = núcleo, verde = halo,
  azul = rastro, transparente = não pinta). Sem ela, **pare**: a fatia depende da máscara.

---

## Arquivos

| Arquivo | Responsabilidade | Task |
|---|---|---|
| `appearance/PatternType.java`, `PatternMath.java` | padrões matemáticos | 1 |
| `appearance/WispAppearance.java`, `AppearanceRules.java`, `AppearanceResult.java`, `AppearanceDraft.java` | escolha, validação e rascunho | 2 |
| `appearance/WispTextureBaker.java`, `BakeInput.java` | recolorir a textura | 3 |
| `appearance/AppearanceManager.java`, `network/ApplyAppearancePayload.java`, `SyncAppearancePayload.java` | servidor e rede | 2 |
| `client/ClientAppearances.java`, `client/WispTextures.java`, `client/WispPlayerRenderer.java` (mod.) | texturas no cliente | 4 |
| `client/grimoire/AppearancePage.java`, `GrimoirePortrait.java` | aba Aparência | 5 |
| `assets/wispborn/textures/entity/wisp_partmask.png` | máscara de partes | 3 |

---

### Task 1: Padrões matemáticos

**Files:**
- Create: `src/main/java/dev/eder/wispborn/appearance/PatternType.java`, `PatternMath.java`
- Test: `src/test/java/dev/eder/wispborn/appearance/PatternMathTest.java`, `PatternIdsTest.java`

**Interfaces:**
- Produces:
  - `enum PatternType` com `id()` (`wispborn:<nome>`) e `colorCount()`: `SOLID(1)`, `STRIPES(2)`, `SPOTS(3)`, `VEINS(2)`,
    `GRADIENT(2)`, `RINGS(2)`; `byId(ResourceLocation): Optional<PatternType>`
  - `PatternMath.hash(int x, int y, long seed): long`: mistura de 64 bits (SplitMix64 de
    `seed ^ x × 0x9E3779B97F4A7C15L ^ y × 0xC2B2AE3D27D4EB4FL`)
  - `PatternMath.slotAt(PatternType type, int x, int y, double nu, double nv, long seed): int`. `x, y` = pixel do
    atlas; `nu, nv` = posição normalizada [0, 1) dentro da caixa do núcleo na máscara; `o = Math.floorMod(seed, 7)`
    e `h(a, b) = hash(a, b, seed)`:
    - `SOLID` → 0
    - `STRIPES` → `Math.floorDiv(y + o, 2) % 2`
    - `SPOTS` → `r = Math.floorMod(h(x / 2, y / 2), 6)`: 1 se `r == 0`, 2 se `r == 1`, senão 0
    - `VEINS` → 1 se `Math.floorMod(x × 3 + y × 5 + Math.floorMod(h(0, y / 3), 4), 7) == 0`, senão 0
    - `GRADIENT` → 1 se `nv + (Math.floorMod(h(x, y), 100) - 50) / 400.0 >= 0.5`, senão 0
    - `RINGS` → `((int) Math.floor(Math.hypot(nu - 0.5, nv - 0.5) × 10 + o)) % 2`

- [ ] **Step 1: Write the failing tests**

```java
@Test void slotAlwaysFitsThePattern() {
    // para cada PatternType, seeds 0, 1 e -42, todo (x, y) de 0..63 e nu/nv derivados: 0 <= slot < colorCount
}
@Test void multiColorPatternsUseEverySlot() {
    // numa grade 16×16, cada PatternType com colorCount > 1 usa todas as vagas ao menos uma vez (seed 1)
}
@Test void sameSeedSameDrawing() { /* duas chamadas iguais → mesmo resultado em toda a grade */ }
@Test void differentSeedsDifferForEveryPatternButSolid() {
    // seeds 1 e 2: pelo menos um pixel diferente em STRIPES, SPOTS, VEINS, GRADIENT e RINGS
}
// PatternIdsTest
@Test void shippedPatternIdsExist() {
    // todo Unlockable PATTERN nos JSON reais (wisp_species e wisp_parasite_trail) tem PatternType.byId presente
}
```

- [ ] **Step 2: Run them and confirm they fail** — `./gradlew test --tests "dev.eder.wispborn.appearance.*"`.
- [ ] **Step 3: Implement** as fórmulas acima.
- [ ] **Step 4: Run them and confirm they pass.** Se `differentSeeds...` falhar para STRIPES por coincidência de
  `o`, troque a seed 2 do teste por outra com `floorMod(seed, 7)` diferente de 1 e anote.
- [ ] **Step 5: Commit** — `feat(appearance): padroes matematicos com semente por individuo`.

---

### Task 2: Escolha, validação e rede

**Files:**
- Create: `appearance/WispAppearance.java`, `AppearanceRules.java`, `AppearanceResult.java`, `AppearanceDraft.java`,
  `AppearanceManager.java`, `network/ApplyAppearancePayload.java`, `SyncAppearancePayload.java`
- Modify: `registry/ModAttachments.java`, `network/ModPayloads.java`, `event/FormEvents.java` (`onStartTracking`, login),
  `command/WispbornCommand.java`, lang
- Test: `src/test/java/dev/eder/wispborn/appearance/AppearanceRulesTest.java`, `AppearanceDraftTest.java`, `network/AppearancePayloadsTest.java`

**Interfaces:**
- Produces:
  - `record WispAppearance(ResourceLocation pattern, List<ResourceLocation> coreColors, ResourceLocation haloColor, ResourceLocation trailColor)`
    com `CODEC` e `STREAM_CODEC`
  - `ModAttachments.APPEARANCE: AttachmentType<WispAppearance>` (`serialize`, `copyOnDeath`; ausência = textura original,
    via `getExistingData`/`removeData`)
  - `enum AppearanceResult { OK, UNKNOWN_PATTERN, PATTERN_LOCKED, WRONG_COLOR_COUNT, UNKNOWN_COLOR, COLOR_LOCKED }`
  - `AppearanceRules.validate(WispAppearance a, Assimilation state, Set<ResourceLocation> knownColors): AppearanceResult`.
    `wispborn:solid` está sempre liberado; os outros padrões precisam de `Unlockable(PATTERN, id, 0)` em `unlocked`;
    número de cores do núcleo = `colorCount`; cada cor (núcleo, halo, rastro) precisa estar em `knownColors` e em `unlocked`.
  - `record AppearanceDraft(PatternType pattern, List<ResourceLocation> coreColors, ResourceLocation haloColor, ResourceLocation trailColor)`:
    `withPattern(PatternType)` (corta ou completa as cores do núcleo repetindo a primeira), `withCoreColor(int slot, ResourceLocation)`,
    `withHaloColor`, `withTrailColor`, `toAppearance()`, e `AppearanceDraft.from(Optional<WispAppearance> current, ResourceLocation fallbackColor)`
    (sem aparência → `SOLID` e `fallbackColor` em tudo)
  - `ApplyAppearancePayload(Optional<WispAppearance>)` (cliente → servidor; vazio = voltar ao original)
  - `SyncAppearancePayload(int entityId, Optional<WispAppearance>)` (servidor → cliente)
  - `AppearanceManager.apply(ServerPlayer, Optional<WispAppearance>): AppearanceResult`: só como parasita; valida;
    grava ou remove; manda `SyncAppearancePayload` com `PacketDistributor.sendToPlayersTrackingEntityAndSelf`; falha →
    `wispborn.appearance.<resultado>` na barra de ação
  - `FormEvents`: no `StartTracking` de um jogador e no login, manda a aparência dele (mesmo lugar do `SyncFormPayload`)
  - Comando: `/wispborn appearance reset [player]`

- [ ] **Step 1: Write the failing tests**

```java
// AppearanceRulesTest (state com spider_black, husk_sand e o padrão spots liberados)
@Test void solidIsAlwaysAllowed() { /* solid + [spider_black], halo e rastro spider_black → OK */ }
@Test void validationFollowsThePrecedence() {
    // UNKNOWN_PATTERN (wispborn:plaid); PATTERN_LOCKED (stripes); WRONG_COLOR_COUNT (spots com 2 cores);
    // UNKNOWN_COLOR (cor fora de knownColors); COLOR_LOCKED (cow_brown conhecida mas não liberada)
}
// AppearanceDraftTest
@Test void changingPatternResizesTheCoreColors() {
    // SPOTS [a, b, c] → GRADIENT dá [a, b]; SOLID [a] → SPOTS dá [a, a, a]
}
@Test void draftFromNothingStartsSolid() { /* from(empty, x) → SOLID, [x], halo x, rastro x */ }
// AppearancePayloadsTest: ida e volta dos dois payloads, com e sem aparência
```

- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement.** Lang (pt_br / en_us): `wispborn.appearance.unknown_pattern` Padrão desconhecido /
  Unknown pattern; `.pattern_locked` Padrão ainda não descoberto / Pattern not discovered yet; `.wrong_color_count`
  Número de cores errado / Wrong number of colors; `.unknown_color` Cor desconhecida / Unknown color;
  `.color_locked` Cor ainda não descoberta / Color not discovered yet; `.ok` Aparência aplicada / Appearance applied;
  `commands.wispborn.appearance.reset.success` Aparência de %s restaurada / Reset %s appearance.
- [ ] **Step 4: Run the full build and tests** (`./gradlew build`, verde).
- [ ] **Step 5: Commit** — `feat(appearance): escolha validada no servidor e sincronizada com quem ve o jogador`.

---

### Task 3: Recolorir a textura

**Files:**
- Create: `appearance/WispTextureBaker.java`, `appearance/BakeInput.java`, `assets/wispborn/textures/entity/wisp_partmask.png`
  (cópia de `art/parasita-magico/wisp_partmask.png`)
- Test: `src/test/java/dev/eder/wispborn/appearance/WispTextureBakerTest.java`

**Interfaces:**
- Produces:
  - `record BakeInput(PatternType pattern, int[] coreRgb, OptionalInt haloRgb, OptionalInt trailRgb, long seed)`:
    cor ausente (vazia ou `coreRgb[i] < 0`) = a vaga mantém o pixel original
  - `WispTextureBaker.coreBounds(int[] maskArgb, int width, int height): int[]` (`{minX, minY, maxX, maxY}` dos pixels de núcleo)
  - `WispTextureBaker.bake(int[] baseArgb, int[] maskArgb, int width, int height, BakeInput in): int[]`. Para cada
    pixel com alfa > 0: a parte vem da máscara (canal vermelho > 127 = núcleo, verde > 127 = halo, azul > 127 =
    rastro; nenhum = mantém). Cor do núcleo = `coreRgb[slotAt(pattern, x, y, nu, nv, seed)]`, com `nu, nv` dentro
    de `coreBounds`. Luminância `L = (0,299 r + 0,587 g + 0,114 b) / 255` do pixel original; saída
    `rgb = Math.round(cor × (0,35 + 0,65 × L))` por canal, **alfa original**. Usado igual para a textura e para a glowmask.

- [ ] **Step 1: Write the failing tests**

```java
@Test void unmaskedPixelsAreUntouched() { /* máscara transparente → saída == entrada */ }
@Test void alphaIsPreserved() { /* pixel de halo com alfa 86 → saída com alfa 86 */ }
@Test void shadingFollowsTheOriginalLuminance() {
    // núcleo SOLID com cor 0xFF0000: pixel branco → vermelho 255; pixel preto → vermelho 89 (0,35 × 255)
}
@Test void missingColorKeepsTheOriginalPixel() { /* coreRgb {-1} → pixel de núcleo inalterado */ }
@Test void coreBoundsCoverOnlyCorePixels() { /* máscara 4×4 com núcleo em (1,1)-(2,2) → {1, 1, 2, 2} */ }
```

- [ ] **Step 2: Run them and confirm they fail**, **implement**, **run again** (PASS).
- [ ] **Step 3: Commit** — `feat(appearance): recolorir a textura do wisp pela mascara de partes`.

---

### Task 4: Texturas no cliente e no renderer

**Files:**
- Create: `client/ClientAppearances.java`, `client/WispTextures.java`
- Modify: `client/WispPlayerRenderer.java` (e a camada de glow, se for preciso), `client/ClientPayloadHandler.java`,
  `WispbornMod.java` ou a classe de eventos do cliente (listener de recarga de recursos)

**Interfaces:**
- Produces:
  - `ClientAppearances.get(UUID): Optional<WispAppearance>`, `set(int entityId, Optional<WispAppearance>)` (resolve o
    UUID pelo `mc.level.getEntity`), `reset()` no logout
  - `ClientAppearances.setPreview(Optional<WispAppearance>)`: aparência de prévia do jogador local, usada só
    enquanto `WispRenderHandler.isRenderingGrimoirePortrait()` for verdadeiro
  - `WispTextures.textureFor(UUID owner, WispAppearance a): ResourceLocation` e `glowFor(...)`: monta o `BakeInput`
    (cores pelo `ClientWispData`; cor sumida → `-1`), chama o `bake` com `wisp.png`, `wisp_glowmask.png` e
    `wisp_partmask.png` lidos uma vez do `ResourceManager`, cria `DynamicTexture` e registra como
    `wispborn:dynamic/wisp/<hash hex>` e `.../<hash hex>_glowmask`. Cache por `(aparência, seed)`; quando um dono troca
    de aparência ou sai, a textura velha sem outro dono é fechada e liberada. `clear()` na recarga de recursos
    (`RegisterClientReloadListenersEvent`) e no logout.
  - `WispPlayerRenderer`: a textura do jogador sendo desenhado (`currentEntity` do `GeoReplacedEntityRenderer`; confira o nome na
    GeckoLib 4.9.3) vem de `WispTextures` quando há aparência; senão, a textura padrão. A glowmask acompanha. Se
    a camada atual for `AutoGlowingGeoLayer` (que procura o arquivo `_glowmask` no resource pack e não acha textura
    dinâmica), troque por uma `WispGlowLayer` que desenha com `RenderType.eyes(glowFor(...))`.
  - Se houver partículas de rastro no cliente (procure em `WispRenderHandler`), use a cor do rastro nelas.

- [ ] **Step 1: Implement** e rode `./gradlew build`.
- [ ] **Step 2: Check in game** (com o `ApplyAppearancePayload` mandado por um comando temporário ou já pela aba do Task 5):
  - O núcleo com Manchas em três cores, o halo âmbar e o rastro violeta aparecem no jogo e no retrato.
  - A glowmask segue as cores.
  - Um segundo jogador vê o mesmo esquema, com o desenho um pouco diferente.
  - `/wispborn appearance reset` volta ao A3 original.
  - Trocar de aparência 30 vezes não faz a memória do F3 crescer sem parar.
  - F3+T (recarregar recursos) não quebra as texturas.
- [ ] **Step 3: Commit** — `feat(client): texturas dinamicas do wisp por aparencia`.

---

### Task 5: Aba Aparência

**Files:**
- Create: `client/grimoire/AppearancePage.java`, `client/grimoire/GrimoirePortrait.java`, sprites
  `grimoire/part_chip.png` e `part_chip_active.png` (nine-slice, 56×14), `pattern_frame.png` e
  `pattern_frame_selected.png` (24×24), `swatch_frame.png`, `swatch_selected.png` (11×11), `swatch_fill.png` (9×9,
  branco opaco), `pattern/<nome>.png` (22×22, um por `PatternType`, em tons de cinza)
- Modify: `client/grimoire/WispPage.java` (passa a usar `GrimoirePortrait`), `GrimoireTab.java` (APPEARANCE com página),
  `GrimoireSprites.java`, lang

**Interfaces:**
- Produces: `GrimoirePortrait.render(GuiGraphics, int left, int top, int x0, int y0, int x1, int y1, int mouseX, int mouseY, Player)`
  (moldura + modelo, o código que hoje está em `WispPage`).

Layout (relativo ao painel):
- Retrato à esquerda, `(12, 28)` a `(104, 188)`, mostrando a **prévia** (`ClientAppearances.setPreview(draft)` ao
  abrir e a cada mudança; `setPreview(empty)` ao sair da página ou fechar o grimório).
- Chips no topo da direita, y 24: Núcleo, Halo e Rastro (`part_chip`, 56×14 em x 116, 176 e 236; o selecionado
  com `part_chip_active`). Começa no Núcleo.
- Núcleo:
  - `wispborn.appearance.pattern` em y 42.
  - Seis `pattern_frame` em `x = 116 + i × 30`, y 52, com a miniatura `pattern/<nome>`. Padrão bloqueado: miniatura
    com `setShaderColor(0.35F, 0.35F, 0.35F, 1F)` e sem clique.
  - `wispborn.appearance.colors` em y 82.
  - As vagas do padrão em `x = 116 + i × 15`, y 92: `swatch_frame` + `swatch_fill` tingido com a cor; a vaga
    selecionada usa `swatch_selected`.
- Halo e Rastro: a mesma linha, com uma vaga só.
- Paleta, y 110 a 170: as cores liberadas, 14 por linha, passo de 13 px, como `swatch_frame` + `swatch_fill`
  tingido. Clique pinta a vaga selecionada.
- Tingir: `RenderSystem.setShaderColor(r, g, b, 1F)` antes do `blitSprite` do `swatch_fill` e
  `setShaderColor(1F, 1F, 1F, 1F)` logo depois. Nada de `fill`.
- Rodapé (o mesmo da `WispPage`): em x 116, o nome da cor ou do padrão sob o mouse (ou `wispborn.appearance.hint`);
  Cancelar (190) volta o rascunho para a aparência atual; Aplicar (252) manda `ApplyAppearancePayload`. Os dois
  ficam desativados sem mudança pendente. Sem nenhuma cor liberada: `wispborn.appearance.no_colors` e Aplicar
  desativado.

Lang (pt_br / en_us): `wispborn.appearance.part.core` Núcleo / Core; `.part.halo` Halo / Halo; `.part.trail`
Rastro / Trail; `.pattern` Padrão / Pattern; `.colors` Cores / Colors; `.hint` Escolha uma vaga e uma cor / Pick a
slot and a color; `.no_colors` Descubra cores consumindo criaturas / Discover colors by consuming creatures;
`wispborn.grimoire.apply` Aplicar / Apply.

- [ ] **Step 1: Implement** a extração do `GrimoirePortrait` e confira que a aba Wisp ficou igual.
- [ ] **Step 2: Implement** a `AppearancePage` e os sprites (da A5 ou placeholders: chips de `button`/`button_hover`,
  molduras de `pip_empty`/`pip_pending` desenhadas no tamanho pedido, `swatch_fill` branco gerado 9×9, miniaturas de
  `hud/dna_icon`).
- [ ] **Step 3: Build** (`./gradlew build`, verde).
- [ ] **Step 4: Check in game:**
  - Com cores e padrões liberados (`/wispborn assimilation add minecraft:spider 3`, `minecraft:bee 8`,
    `minecraft:slime 8`), montar Listras com duas cores mostra a prévia no retrato.
  - Cancelar volta ao que estava; Aplicar muda o wisp no mundo.
  - Um padrão bloqueado não deixa clicar.
- [ ] **Step 5: Commit** — `feat(grimoire): aba Aparencia com padrao, vagas e paleta`.

---

### Task 6: Documentação

- [ ] `docs/specs/F9-aparencia.md` (aponta para este plano; dúvidas e relatório), `CHANGELOG.md`, linha F9 do
  `docs/PLAN.md`, pacote `appearance/` na árvore do `AGENTS.md`.
- [ ] `./gradlew build` verde, e commit: `docs(F9): relatorio da aparencia, CHANGELOG e PLAN`.
