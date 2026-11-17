# Changelog — Wispborn

Documento de revisão técnica das implementações por spec para auditoria (Claude / Eder).

---

## [M1-wisp-form] — Forma Wisp (`feat/M1-wisp-form`)

**Spec de referência:** `docs/specs/M1-wisp-form.md`  
**Commits:**
- `8d15478` — `feat(form): implementar FormType, FormState, WispTuning, FormManager, ModAttachments e wisp_essence`
- `185326a` — `feat(net): adicionar SyncFormPayload, eventos de forma, restricoes do wisp e comando /wispborn`
- `1e388f3` — `feat(client): implementar WispRenderHandler, texturas placeholder e traducoes do M1`
- `7c415b2` — `test(form): adicionar FormStateTest e configurar classpath de testes no build.gradle`  

### 1. Resumo das Alterações (`main..feat/M1-wisp-form`)
- Implementação do estado `FormState` (`FormType.HUMAN`, `WISP`, `HOST`) com `Codec` e `StreamCodec`, persistido via Data Attachment `ModAttachments.FORM` (`copyOnDeath`).
- Aplicação idempotente dos 5 modificadores de atributo do wisp (`SCALE`, `MAX_HEALTH`, `GRAVITY`, `SAFE_FALL_DISTANCE`, `MOVEMENT_SPEED`) via `FormManager` e constantes em `WispTuning`.
- Sincronização manual `S2C` via `SyncFormPayload` em login, respawn, troca de dimensão e `StartTracking`.
- Restrições de parasita em `WispRestrictions`, item `wisp_essence` (aba `Ingredients`), comando `/wispborn form <human|wisp> [player]` e renderização customizada no client (`WispRenderHandler`) isolada do servidor dedicado.

### 2. Detalhamento por Arquivo

| Arquivo | Ação | Detalhes da Implementação |
|---|---|---|
| `src/main/java/dev/eder/wispborn/form/FormType.java` | Criado | Enum `HUMAN("human")`, `WISP("wisp")`, `HOST("host")` implementando `StringRepresentable`, com `CODEC` (`StringRepresentable.fromEnum`) e `STREAM_CODEC` (`ByteBufCodecs.idMapper`). |
| `src/main/java/dev/eder/wispborn/form/FormState.java` | Criado | Record `(FormType type, Optional<ResourceLocation> hostEntity)` com constantes `HUMAN`, `WISP`, fábrica `host(ResourceLocation)`, `CODEC` (`RecordCodecBuilder`) e `STREAM_CODEC` (`StreamCodec.composite`). |
| `src/main/java/dev/eder/wispborn/form/WispTuning.java` | Criado | Constantes de `ResourceLocation` (`wispborn:wisp_*`) e valores dos modificadores (`SCALE -0.6`, `MAX_HEALTH -14.0`, `GRAVITY -0.75`, `SAFE_FALL_DISTANCE +20.0`, `MOVEMENT_SPEED +0.2`). |
| `src/main/java/dev/eder/wispborn/form/FormManager.java` | Criado | Métodos `setForm`, `applyModifiers` (remove e reaplica modificadores de forma idempotente e limita `health <= maxHealth`), `sync`, `isWisp` e `isParasite`. |
| `src/main/java/dev/eder/wispborn/form/WispEssenceItem.java` | Criado | Item stack 16 que alterna `HUMAN ↔ WISP` via `FormManager.setForm`, toca `SoundEvents.ALLAY_ITEM_TAKEN`, emite partículas `END_ROD` e consome 1 unidade fora do criativo (`stack.consume(1, serverPlayer)`). |
| `src/main/java/dev/eder/wispborn/registry/ModAttachments.java` | Criado | `DeferredRegister<AttachmentType<?>>` com `FORM` (`default FormState.HUMAN`, `.serialize(FormState.CODEC)`, `.copyOnDeath()`). |
| `src/main/java/dev/eder/wispborn/registry/ModItems.java` | Criado | `DeferredRegister.Items` com `WISP_ESSENCE` e listener `addCreative` para `CreativeModeTabs.INGREDIENTS`. |
| `src/main/java/dev/eder/wispborn/network/SyncFormPayload.java` | Criado | `CustomPacketPayload` (`wispborn:sync_form`) contendo `(int entityId, FormState state)`. |
| `src/main/java/dev/eder/wispborn/network/ModPayloads.java` | Criado | Registro de `SyncFormPayload` em `RegisterPayloadHandlersEvent` (`registrar("1")`), delegando para `ClientPayloadHandler` via lambda para não carregar classes `Dist.CLIENT` no servidor dedicado. |
| `src/main/java/dev/eder/wispborn/event/FormEvents.java` | Criado | Listeners no `NeoForge.EVENT_BUS` para `PlayerLoggedInEvent`, `PlayerRespawnEvent`, `PlayerChangedDimensionEvent` e `PlayerEvent.StartTracking`. |
| `src/main/java/dev/eder/wispborn/event/WispRestrictions.java` | Criado | Cancela `AttackEntityEvent`, `BlockEvent.BreakEvent`, `PlayerInteractEvent.EntityInteract`, `RightClickItem` e `RightClickBlock` quando `isParasite(player)`, exceto uso de `wisp_essence`. |
| `src/main/java/dev/eder/wispborn/command/WispbornCommand.java` | Criado | Comando `/wispborn form <human\|wisp> [player]` com permissão 2 e feedback traduzido (`commands.wispborn.form.success`). |
| `src/main/java/dev/eder/wispborn/client/ClientPayloadHandler.java` | Criado | Handler client-side de `SyncFormPayload` que aplica `entity.setData(ModAttachments.FORM, payload.state())` se a entidade existir em `Minecraft.getInstance().level`. |
| `src/main/java/dev/eder/wispborn/client/WispRenderHandler.java` | Criado | `@EventBusSubscriber(..., value = Dist.CLIENT)`: cancela `RenderPlayerEvent.Pre` para `WISP` (desenhando 2 quads billboard emissivos `wisp_core` e `wisp_halo` com flutuação `sin(t)*0.08` e pulso `±8%`) e `HOST`; gera partículas `END_ROD` a cada 2 ticks em `ClientTickEvent.Post`; cancela `RenderHandEvent` para parasitas. |
| `src/main/java/dev/eder/wispborn/WispbornMod.java` | Modificado | Registra `ModAttachments.ATTACHMENT_TYPES`, `ModItems.ITEMS`, `ModItems::addCreative`, `ModPayloads::register`, `FormEvents`, `WispRestrictions` e `WispbornCommand`. |
| `src/main/resources/assets/wispborn/models/item/wisp_essence.json` | Criado | Modelo `minecraft:item/generated` apontando para `wispborn:item/wisp_essence`. |
| `src/main/resources/assets/wispborn/textures/` | Criado | Texturas placeholder PNG geradas por script: `item/wisp_essence.png` (16×16), `entity/wisp_core.png` (32×32) e `entity/wisp_halo.png` (32×32). |
| `src/main/resources/assets/wispborn/lang/{en_us,pt_br}.json` | Modificado | Adicionadas chaves `item.wispborn.wisp_essence` e `commands.wispborn.form.success`. |
| `src/test/java/dev/eder/wispborn/form/FormStateTest.java` | Criado | Testes JUnit 5 validando serialização de `FormType` (`"human"`, `"wisp"`, `"host"`) e round-trip de `FormState.CODEC` com `JsonOps.INSTANCE` (`HUMAN`, `WISP`, `HOST` com `minecraft:cow`). |
| `build.gradle` | Modificado | Adicionado `testCompileClasspath.extendsFrom compileClasspath` e `testRuntimeClasspath.extendsFrom runtimeClasspath` para expor DFU (`Codec`/`JsonOps`) e `ResourceLocation` aos testes JUnit. |
| `docs/specs/M1-wisp-form.md` | Modificado | Preenchido "Dúvidas do implementador" e "Relatório de implementação". |

### 3. Evidências de Verificação

- **`./gradlew build --no-daemon`:**
  ```text
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  > Task :check
  > Task :build
  BUILD SUCCESSFUL in 15s
  ```
- **`./gradlew test --rerun-tasks --no-daemon`:**
  ```text
  > Task :compileJava
  > Task :classes
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  BUILD SUCCESSFUL in 13s
  ```
- **`./gradlew runServer --no-daemon`:**
  ```text
  [16nov.2026 21:29:50.697] [modloading-worker-0/INFO] [dev.eder.wispborn.WispbornMod/]: Inicializando Wispborn (wispborn)
  [16nov.2026 21:29:55.870] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (3.096s)! For help, type "help"
  [16nov.2026 21:29:55.898] [Server thread/INFO] [net.neoforged.neoforge.gametest.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```

### 4. Alterações fora do escopo / Decisões de borda
| Arquivo | Alteração / Decisão | Motivo / Impacto |
|---|---|---|
| `build.gradle` | `testCompileClasspath.extendsFrom compileClasspath` e `testRuntimeClasspath.extendsFrom runtimeClasspath` | No ModDevGradle 2.0.x, o sourceSet `test` não herda automaticamente as bibliotecas do Minecraft/DFU (`com.mojang.serialization.Codec`, `JsonOps`, `ResourceLocation`) exigidas por `FormStateTest`. |
| `FormManager.java` | Soma do delta de `MAX_HEALTH` (`+14`) à vida atual quando `getMaxHealth()` aumenta em `setForm` | Atende ao item do checklist *"Usar `wisp_essence` como wisp volta a humano com vida e tamanho normais"* (sem isso, o jogador voltaria a humano com 3/10 corações preenchidos). Também segue o padrão previsto no M2 (*"Ao subir, curar a diferença"*). |
| `WispRestrictions.java` | `event.setUseBlock(TriState.FALSE)` em `RightClickBlock` quando segurando `wisp_essence` | Garante que clicar com o botão direito olhando para um baú/bloco segurando `wisp_essence` não abra o baú, mas permita que o item `wisp_essence` seja ativado normalmente. |

### 5. Pendências
- [ ] **Checklist manual in-game (`./gradlew runClient` / `runClient2`):** validação visual e funcional a cargo de Eder.

---

## [M0-setup] — Setup do Projeto (`feat/M0-setup`)

**Spec de referência:** `docs/specs/M0-setup.md`  
**Commits:**
- `0ddd9f1` — `chore: template MDK 1.21.1 e wispborn-kit iniciais`
- `b0d872a` — `feat(setup): implementar spec M0 com pacotes base, CI e JUnit 5`
- `d6d3f3d` — `docs(setup): registrar saidas de build, test e runServer na spec M0`
- `e80be54` — `docs: criar CHANGELOG.md detalhando implementacao da spec M0`
- `9f562e0` — `docs(setup): corrigir pendencias da revisao do M0`
- `c8ef424` — `fix(ci): marcar gradlew como executavel`
- `9f562e0` — `docs(setup): corrigir pendencias da revisao do M0`
- `c8ef424` — `fix(ci): marcar gradlew como executavel`

### 1. Resumo das Alterações (`main..feat/M0-setup`)
- **Total:** 23 arquivos alterados (`136 insertions(+)`, `389 deletions(-)`)
- Remoção integral do código e assets de exemplo (`com.example.examplemod.*` e `assets/examplemod/*`).
- Configuração das propriedades do mod, versões estáveis do NeoForge 1.21.1 e Parchment, segundo cliente (`client2`), CI no GitHub Actions e suíte JUnit 5.

### 2. Detalhamento por Arquivo

| Arquivo | Ação | Detalhes da Implementação |
|---|---|---|
| `gradle.properties` | Modificado | Configurado `mod_id=wispborn`, `mod_name=Wispborn`, `mod_group_id=dev.eder.wispborn`, `mod_version=0.0.1`, `mod_license=MIT`, `mod_authors=Eder`, `minecraft_version=1.21.1`, `neoforge_version=21.1.266`, `parchment_minecraft_version=1.21.1` e `parchment_mappings_version=2024.11.17`. A antiga `neo_version` foi removida: `neoforge_version` é a única fonte da versão do NeoForge. |
| `build.gradle` | Modificado | `neoForge.version` passa a ler `neoforge_version`, que é repassada ao template do `mods.toml` como `neo_version`; adicionada run config `client2` (`--username Player2`, diretório `run-client2`); configuradas dependências `org.junit.jupiter:junit-jupiter:5.10.2` e `org.junit.platform:junit-platform-launcher` com `test { useJUnitPlatform() }`; incluído `mod_authors` no `generateModMetadata`. |
| `.gitignore` | Modificado | Mantidos padrões do MDK (`run/`, `.idea/`, `*.iml`, `build/`), `.gradle` passou a `.gradle/` e adicionado `run-client2/`. |
| `gradlew` | Modificado | Modo alterado para 100755 (executável); sem isso o runner Linux falha com exit code 126. |
| `.github/workflows/build.yml` | Modificado | Configurado com `actions/checkout@v4`, `actions/setup-java@v4` (Temurin 21), `gradle/actions/setup-gradle@v4`, comando `./gradlew build --no-daemon` e upload de `build/libs/*.jar` via `actions/upload-artifact@v4`. |
| `src/main/templates/META-INF/neoforge.mods.toml` | Modificado | Removidos comentários e referências a `examplemod`; vinculado `authors="${mod_authors}"` e descrição do mod Wispborn. |
| `src/main/java/dev/eder/wispborn/WispbornMod.java` | Criado | Classe principal `@Mod(WispbornMod.MOD_ID)` com `public static final String MOD_ID = "wispborn";` sem blocos/itens/configs de exemplo. |
| `src/main/java/dev/eder/wispborn/<pkg>/package-info.java` | Criado | Criados pacotes vazios conforme `AGENTS.md`: `config`, `registry`, `form`, `progress`, `host`, `network`, `event`, `command`, `client`. |
| `src/main/resources/assets/wispborn/lang/en_us.json` | Criado | Inicializado com `{}`. |
| `src/main/resources/assets/wispborn/lang/pt_br.json` | Criado | Inicializado com `{}`. |
| `src/test/java/dev/eder/wispborn/SanityTest.java` | Criado | Teste unitário JUnit 5 validando `assertEquals("wispborn", WispbornMod.MOD_ID)`. |
| `docs/specs/M0-setup.md` | Modificado | Preenchidas as seções "Critérios de aceite", "Dúvidas do implementador" e "Relatório de implementação". |

### 3. Evidências de Verificação

- **`./gradlew build --no-daemon`:**
  ```text
  > Task :jar
  > Task :assemble
  > Task :test
  > Task :check
  > Task :build
  BUILD SUCCESSFUL in 22s
  ```
- **`./gradlew test --rerun-tasks --no-daemon`:**
  ```text
  > Task :compileJava
  > Task :classes
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  BUILD SUCCESSFUL in 16s
  ```
- **`./gradlew runServer --no-daemon`:**
  ```text
  [20:58:18] [Server thread/INFO] [minecraft/DedicatedServer]: Done (4.337s)! For help, type "help"
  [20:58:18] [Server thread/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```
- **Varredura de resíduos (`git grep -i example`):** zero ocorrências em código, recursos ou configs do mod (a única ocorrência restante é texto do script `gradlew`, do wrapper do Gradle).

### 4. Alterações fora do escopo da spec
Feitas durante a implementação sem estarem na spec M0. Registradas aqui para auditoria; nenhuma altera comportamento do mod.

| Arquivo | Alteração | Motivo / impacto |
|---|---|---|
| `gradle.properties` | `org.gradle.jvmargs` de `-Xmx1G` para `-Xmx2G` | Motivo não registrado pelo implementador; provavelmente dar mais memória ao Gradle na decompilação/recompilação do Minecraft. Afeta só o ambiente de build. |
| `build.gradle`, `gradle.properties` | Remoção de todos os comentários explicativos do MDK | Limpeza de template. Sem efeito funcional; os comentários continuam disponíveis no MDK original. |
| `.gitignore` | `.gradle` → `.gradle/` | Ignora só o diretório. Sem efeito prático no repositório. |

### 5. Pendências
- [x] **CI:** verde no commit `c8ef424` (https://github.com/EderDomenici/wispborn/actions/runs/43986805343), com artefato `wispborn-jar` publicado. O primeiro run falhou com exit code 126 porque o `gradlew` estava sem permissão de execução; corrigido em `c8ef424`.
- [ ] **`./gradlew runClient`:** validação visual (mod "Wispborn" na lista de mods) a cargo de Eder.
