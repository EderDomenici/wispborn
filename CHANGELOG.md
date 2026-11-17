# Changelog — Wispborn

Documento de revisão técnica das implementações por spec para auditoria (Claude / Eder).

---

## [M0-setup] — Setup do Projeto (`feat/M0-setup`)

**Spec de referência:** `docs/specs/M0-setup.md`  
**Commits:**
- `0ddd9f1` — `chore: template MDK 1.21.1 e wispborn-kit iniciais`
- `b0d872a` — `feat(setup): implementar spec M0 com pacotes base, CI e JUnit 5`
- `d6d3f3d` — `docs(setup): registrar saidas de build, test e runServer na spec M0`

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
- [ ] **CI:** o workflow nunca foi executado — o repositório ainda não tem remote. O critério "build verde no CI" da spec continua em aberto até o primeiro push.
- [ ] **`./gradlew runClient`:** validação visual (mod "Wispborn" na lista de mods) a cargo de Eder.
