# M0 — Setup do projeto

**Objetivo:** repositório compilando, rodando o client vazio com o mod carregado, CI verde.
**Estimativa:** 1 dia. **Branch:** `feat/M0-setup`.

## Tarefas
1. Criar o repositório a partir do template `NeoForgeMDKs/MDK-1.21.1-ModDevGradle`
   (botão "Use this template" no GitHub). Nome sugerido: `wispborn`.
2. Copiar para a raiz deste kit: `AGENTS.md`, `.agents/`, `docs/`.
3. `gradle.properties`:
   - `mod_id=wispborn`
   - `mod_name=Wispborn`
   - `mod_group_id=dev.eder.wispborn`
   - `mod_version=0.0.1`
   - `mod_license=MIT` (Eder pode trocar)
   - `mod_authors=Eder`
   - `neoforge_version` = **a última 21.1.x estável** em https://projects.neoforged.net/neoforged/neoforge
   - `parchment_minecraft_version=1.21.1` e a última `parchment_mappings_version` para 1.21.1
4. Renomear o pacote do exemplo para `dev.eder.wispborn`, classe principal `WispbornMod`
   com `public static final String MOD_ID = "wispborn";`. Remover o conteúdo de exemplo do MDK
   (blocos/itens/config de exemplo).
5. Criar os pacotes vazios listados em `AGENTS.md` (pode ser só `package-info.java` onde fizer sentido).
6. Criar `assets/wispborn/lang/en_us.json` e `pt_br.json` com `{}`.
7. GitHub Actions `.github/workflows/build.yml`:
   - `actions/checkout@v4`, `actions/setup-java@v4` (temurin 21), `gradle/actions/setup-gradle@v4`
   - `./gradlew build --no-daemon`
   - Upload do jar de `build/libs/` como artifact.
8. `.gitignore` do MDK + `run/`, `.idea/`, `*.iml`, `.gradle/`, `build/`.
9. Em `build.gradle`, dentro de `neoForge { runs { ... } }`, adicionar um segundo client para testes
   de multiplayer: `client2 { client(); programArguments.addAll('--username', 'Player2'); gameDirectory = project.file('run-client2') }`
   (adicionar `run-client2/` ao `.gitignore`).
10. Preparar JUnit 5: `testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'`,
    `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'` e `test { useJUnitPlatform() }`.
    Um teste trivial (`SanityTest`) para provar que `./gradlew test` roda.

## Critérios de aceite
- [ ] `./gradlew build` verde localmente e no CI.
- [ ] `./gradlew runClient` abre o jogo; o mod "Wispborn" aparece na lista de mods.
- [ ] `./gradlew runServer` sobe sem crash (aceitar EULA em `run/eula.txt`).
- [ ] Nenhum resto do mod de exemplo (`examplemod`, `EXAMPLE_BLOCK` etc.).

## Dúvidas do implementador
Nenhuma dúvida.

## Relatório de implementação
- **Arquivos criados/alterados:**
  - `gradle.properties`: configurado `mod_id=wispborn`, `mod_name=Wispborn`, `mod_group_id=dev.eder.wispborn`, `mod_version=0.0.1`, `mod_license=MIT`, `mod_authors=Eder`, `neoforge_version=21.1.266` e `parchment_mappings_version=2024.11.17`.
  - `build.gradle`: adicionada run configuration `client2` (`--username Player2`, `run-client2`), dependência JUnit 5 (`org.junit.jupiter:junit-jupiter:5.10.2` + `junit-platform-launcher`) e `test { useJUnitPlatform() }`.
  - `.gitignore`: incluído `run-client2/`.
  - `.github/workflows/build.yml`: configurado workflow com Temurin 21, `./gradlew build --no-daemon` e upload do artefato `build/libs/*.jar`.
  - `src/main/templates/META-INF/neoforge.mods.toml`: limpo conteúdo de exemplo e vinculado `${mod_authors}`.
  - `src/main/java/dev/eder/wispborn/WispbornMod.java` e `package-info.java` nos subpacotes (`config`, `registry`, `form`, `progress`, `host`, `network`, `event`, `command`, `client`).
  - `src/main/resources/assets/wispborn/lang/en_us.json` e `pt_br.json` inicializados com `{}`.
  - `src/test/java/dev/eder/wispborn/SanityTest.java`: teste unitário JUnit 5 validando `WispbornMod.MOD_ID`.
- **Decisões tomadas:**
  - Removido completamente o pacote `com.example.examplemod` e os recursos `assets/examplemod`.
  - Mantido `neo_version` e `neoforge_version` sincronizados em `21.1.266` no `gradle.properties`.
- **Saída de `./gradlew build test`:**
  - `BUILD SUCCESSFUL` com todas as tasks (`compileJava`, `jar`, `compileTestJava`, `test`, `check`, `build`) executadas sem erros.
- **Pendências manuais:**
  - Validação visual do cliente (`./gradlew runClient`) e aceite do EULA em `./gradlew runServer`.
