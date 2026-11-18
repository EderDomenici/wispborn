# Changelog — Wispborn

Documento de revisão técnica das implementações por spec para auditoria (Claude / Eder).

---

## [M3c-host-stealth] — Wisp Indetectável Dentro do Hospedeiro (`feat/M3c-host-stealth`)

**Spec de referência:** `docs/specs/M3c-host-stealth.md`

### 1. Resumo das Alterações (`feat/F2-mob-traits..feat/M3c-host-stealth`)
- Ao entrar num hospedeiro e a cada 20 ticks na forma `HOST`, `HostStealth.clearPursuers(player, host)` limpa a perseguição e a raiva de todos os `Mob` num raio de `stealth.clearRadius` (`48.0`) blocos ao redor do jogador (exceto o próprio hospedeiro), cobrindo `Mob#setTarget(null)`, `Mob#setLastHurtByMob(null)`, memórias de brain `ATTACK_TARGET` e `ANGRY_AT`, `NeutralMob#stopBeingAngry()` e `Warden#clearAnger(player)`.
- Enquanto `HOST`, `HostEvents.onLivingChangeTarget` (`LivingChangeTargetEvent`) bloqueia qualquer tentativa de mirar o jogador usando `StealthRules.blocksTargeting`: cancela o evento para manter o alvo anterior do mob (ex.: zumbi que já perseguia o porco) ou zera o alvo (`setNewAboutToBeSetTarget(null)`) caso o próprio hospedeiro já estivesse mirando o jogador antes da possessão.

### 2. Detalhamento por Arquivo

| Arquivo | Ação | Detalhes da Implementação |
|---|---|---|
| `src/main/java/dev/eder/wispborn/config/WispbornConfig.java` | Modificado | Adicionado `stealth.clearRadius` (`48.0`, faixa `1.0–128.0`). |
| `src/main/java/dev/eder/wispborn/host/StealthRules.java` | Criado | Funções puras `blocksTargeting` e `shouldClear`. |
| `src/main/java/dev/eder/wispborn/host/HostStealth.java` | Criado | `clearPursuers(ServerPlayer, LivingEntity)` limpando `target`, `lastHurtByMob`, brain (`ATTACK_TARGET`, `ANGRY_AT`), `NeutralMob` e `Warden`. |
| `src/main/java/dev/eder/wispborn/host/HostManager.java` | Modificado | Chama `HostStealth.clearPursuers` ao montar no hospedeiro e a cada 20 ticks em `HOST`. |
| `src/main/java/dev/eder/wispborn/event/HostEvents.java` | Modificado | Handler `onLivingChangeTarget` bloqueando mira em jogadores `HOST`. |
| `src/test/java/dev/eder/wispborn/host/StealthRulesTest.java` | Criado | Testes unitários JUnit 5 para `StealthRules.blocksTargeting` e `StealthRules.shouldClear`. |
| `docs/specs/M3c-host-stealth.md` | Modificado | Preenchido "Dúvidas do implementador" e "Relatório de implementação". |

### 3. Evidências de Verificação

- **`./gradlew build --no-daemon`:** `BUILD SUCCESSFUL in 3s`
- **`./gradlew test --rerun-tasks --no-daemon`:** `BUILD SUCCESSFUL in 8s`
- **`./gradlew runServer --no-daemon`:**
  ```text
  [18:48:24] [Server thread/INFO] [minecraft/DedicatedServer]: Done (1.076s)! For help, type "help"
  ```

### 4. Pendências
- [ ] **Checklist manual in-game (`./gradlew runClient`):** validação com slime, zumbi, esqueleto, aranha e piglin zumbificado a cargo de Eder.

---

## [F2-mob-traits] — Nível e Sexo dos Mobs / Fim dos Tiers (`feat/F2-mob-traits`)

**Spec de referência:** `docs/specs/F2-mob-traits.md`

### 1. Resumo das Alterações (`feat/F1-born-wisp..feat/F2-mob-traits`)
- Substituição integral dos tiers por **nível e sexo por mob** (`MobTraits(level, sex)` persistido em `ModAttachments.MOB_TRAITS`), sorteados em `EntityJoinLevelEvent` a partir de `data/wispborn/wisp_mob_levels/vanilla.json` (77 espécies vanilla carregadas por `MobLevelTable`) e sincronizados via `SyncMobTraitsPayload` em `StartTracking`.
- Expansão do nível máximo do wisp para **20** (`levels.thresholds` até `136800`), atualização de `InfestRules.check` e `HostManager` (`mobLevel <= wispLevel + infest.levelGateMargin`, custo `infestCostPerLevel * mobLevel`), criação de `EpMath.levelFactor` para a `F3` e remoção de `TierResolver` e `tier_1..3.json`.
- Placa sobre mobs próximos no client (`MobNameplateRenderer`, `RenderLivingEvent.Post`) até `client.nameplateRange` (`8`) com linha de visão, exibindo `♂/♀ Nv X` no estilo vanilla.
- Reprodução vanilla restrita a macho + fêmea via MixinExtras `@WrapOperation` em `BreedGoal#getFreePartner` e nas duas chamadas de `AnimalMakeLove` (`canStillUse` e `lambda$findValidBreedPartner$0`), com rede de segurança em `BabyEntitySpawnEvent`.
- Subcomando de debug `/wispborn traits <entity> [set <level> <male|female>]`.

### 2. Detalhamento por Arquivo

| Arquivo | Ação | Detalhes da Implementação |
|---|---|---|
| `src/main/resources/data/wispborn/wisp_mob_levels/vanilla.json` | Criado | Faixas iniciais de nível das 77 espécies vanilla infestáveis (chefes, `giant` e jogadores fora da tabela = imunes). |
| `src/main/java/dev/eder/wispborn/traits/{Sex,MobTraits,MobLevelRange,MobLevelTable,MobTraitRoller,BreedingRules,MobTraitsManager}.java` | Criado | Modelo, codecs, reload listener `wisp_mob_levels`, sorteio puro, regra de reprodução e gerenciador com sync S2C. |
| `src/main/java/dev/eder/wispborn/registry/{ModAttachments,ModTags}.java` e `progress/TierResolver.java` | Mod./Rem. | Adicionado `MOB_TRAITS`; removidos `TierResolver`, `ModTags.TIER_1..3` e `data/wispborn/tags/entity_type/tier_*.json`. |
| `src/main/java/dev/eder/wispborn/config/{WispbornConfig,WispbornClientConfig}.java` | Mod./Criado | Curva até Nv 20; adicionados `infest.levelGateMargin`, `mana.infestCostPerLevel`, `traits.femaleChance`, `ep.levelGrace`, `ep.falloffPerLevel` e `client.nameplateRange`. |
| `src/main/java/dev/eder/wispborn/progress/{EpMath,ProgressManager}.java` | Criado/Mod. | `EpMath.levelFactor` e remoção do aviso de tier desbloqueado no level-up. |
| `src/main/java/dev/eder/wispborn/host/{InfestRules,HostManager}.java` | Modificado | Gate de nível e custo por nível do mob; suporte a `PartEntity` (Ender Dragon) e jogadores como `IMMUNE` no raycast de alvo. |
| `src/main/java/dev/eder/wispborn/network/{SyncMobTraitsPayload,TargetInfoPayload,ModPayloads}.java` | Criado/Mod. | `SyncMobTraitsPayload` registrado; `TargetInfoPayload` atualizado com `mobLevel`, `Optional<Sex>` e `wispLevel`. |
| `src/main/java/dev/eder/wispborn/event/TraitEvents.java` | Criado | Listeners para `AddReloadListenerEvent`, `EntityJoinLevelEvent`, `PlayerEvent.StartTracking` e `BabyEntitySpawnEvent`. |
| `src/main/java/dev/eder/wispborn/mixin/{BreedGoalMixin,AnimalMakeLoveMixin}.java`, `wispborn.mixins.json`, `neoforge.mods.toml` | Criado/Mod. | `@WrapOperation` exigindo sexos opostos em `BreedGoal` e `AnimalMakeLove`. |
| `src/main/java/dev/eder/wispborn/client/{MobNameplateRenderer,WispRenderHandler,ClientPayloadHandler}.java` | Criado/Mod. | Placa `♂/♀ Nv X` sobre mobs próximos, recepção de `SyncMobTraitsPayload` e HUD de alvo atualizado. |
| `src/main/java/dev/eder/wispborn/command/WispbornCommand.java` e `assets/wispborn/lang/{en_us,pt_br}.json` | Modificado | Subcomando `/wispborn traits` e traduções atualizadas. |
| `src/test/java/dev/eder/wispborn/**` | Criado/Mod. | Testes unitários JUnit 5 para `MobTraitRoller`, `BreedingRules`, `MobLevelRange`/`MobLevelTable`, `EpMath`, `InfestRules`, `LevelCurve` e payloads. |

### 3. Evidências de Verificação

- **`./gradlew build --no-daemon`:** `BUILD SUCCESSFUL in 3s`
- **`./gradlew test --rerun-tasks --no-daemon`:** `BUILD SUCCESSFUL in 10s`
- **`./gradlew runServer --no-daemon`:**
  ```text
  [00:11:06] [main/INFO] [de.ed.wi.WispbornMod/]: Carregadas 77 faixas de nível de mobs em 'wisp_mob_levels'
  [00:11:08] [Server thread/INFO] [minecraft/DedicatedServer]: Done (1.112s)! For help, type "help"
  ```

### 4. Pendências
- [ ] **Checklist manual in-game (`./gradlew runClient` / `runClient2`):** validação das placas, gate de nível, custo de mana, reprodução M+F (vacas, lobos, cabras/sapos) e `/reload` a cargo de Eder.

---

## [F1-born-wisp] — Nascer Wisp, Renascer Wisp e Fome do Wisp (`feat/F1-born-wisp`)

**Spec de referência:** `docs/specs/F1-born-wisp.md`

### 1. Resumo das Alterações (`docs/fundacao-specs..feat/F1-born-wisp`)
- Jogador nasce e renasce em `FormState.WISP` (`ModAttachments.FORM` com default `FormState.WISP`); mana restaurada ao máximo em `PlayerRespawnEvent`.
- Remoção completa da forma humana (`FormType.HUMAN`, `FormState.HUMAN`, `/wispborn form human`, `WispEssenceItem`, `ModItems`, texturas/modelos de `wisp_essence` e exceções em `WispRestrictions`), mantendo compatibilidade de saves antigos em `FormType.CODEC` (`"human"` decodifica como `WISP`).
- Pressão de fome escalonada por nível (`HungerMath.exhaustionPerSecond`) aplicada a cada 20 ticks na forma `WISP` fora do Pacífico e inanição letal em qualquer dificuldade fora do Pacífico (`HungerMath.vanillaStarves` e `HungerMath.wispStarves`) a cada `hunger.starveIntervalTicks` sem duplicar o dano vanilla de `FoodData.tick`.

### 2. Detalhamento por Arquivo

| Arquivo | Ação | Detalhes da Implementação |
|---|---|---|
| `src/main/java/dev/eder/wispborn/form/FormType.java` | Modificado | Removido `HUMAN`; `CODEC` usa `Codec.STRING.comapFlatMap` aceitando `"human"` → `WISP` na leitura e serializando apenas `"wisp"` ou `"host"`. |
| `src/main/java/dev/eder/wispborn/form/FormState.java` | Modificado | Removida constante `FormState.HUMAN`. |
| `src/main/java/dev/eder/wispborn/registry/ModAttachments.java` | Modificado | Default de `FORM` alterado para `FormState.WISP`. |
| `src/main/java/dev/eder/wispborn/{form/WispEssenceItem,registry/ModItems}.java` e `assets/wispborn/{models,textures}/item/wisp_essence.*` | Removido | Item de transformação e registro de itens removidos; `WispbornMod` limpo. |
| `src/main/java/dev/eder/wispborn/event/WispRestrictions.java` | Modificado | Cancelamento incondicional de `RightClickItem` e `RightClickBlock` para parasitas (impede comer comida comum). |
| `src/main/java/dev/eder/wispborn/command/WispbornCommand.java` e `assets/wispborn/lang/{en_us,pt_br}.json` | Modificado | Removido `/wispborn form human` e chaves `item.wispborn.wisp_essence` / `wispborn.form.human`. |
| `src/main/java/dev/eder/wispborn/config/WispbornConfig.java` | Modificado | Adicionados `hunger.exhaustionBasePerSecond` (0.05), `hunger.exhaustionPerLevelPerSecond` (0.01), `hunger.starveIntervalTicks` (80) e `hunger.starveDamage` (1.0). |
| `src/main/java/dev/eder/wispborn/form/HungerMath.java` | Criado | Funções puras `exhaustionPerSecond`, `vanillaStarves` e `wispStarves`. |
| `src/main/java/dev/eder/wispborn/event/FormEvents.java` | Modificado | `onPlayerRespawn` enche a mana; `onPlayerTickPost` aplica exaustão a cada 20 ticks em `WISP` e dano `starve()` a cada `hunger.starveIntervalTicks` quando `wispStarves` é true. |
| `src/test/java/dev/eder/wispborn/form/{FormStateTest,HungerMathTest}.java` | Mod./Criado | Testes de alias legado `"human"` → `WISP` em `FormStateTest` e cobertura unitária completa de `HungerMath`. |
| `docs/specs/F1-born-wisp.md` | Modificado | Preenchido "Dúvidas do implementador" e "Relatório de implementação". |

### 3. Evidências de Verificação

- **`./gradlew build --no-daemon`:**
  ```text
  > Task :jar
  > Task :assemble
  > Task :build
  BUILD SUCCESSFUL in 10s
  ```
- **`./gradlew test --rerun-tasks --no-daemon`:**
  ```text
  > Task :compileJava
  > Task :classes
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  BUILD SUCCESSFUL in 17s
  ```
- **`./gradlew runServer --no-daemon`:**
  ```text
  [23:13:00] [Server thread/INFO] [minecraft/DedicatedServer]: Done (8.759s)! For help, type "help"
  [23:13:00] [Server thread/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```

### 4. Pendências
- [ ] **Checklist manual in-game (`./gradlew runClient`):** validação de nascimento/renascimento como wisp, bloqueio de comida, drenagem de fome e morte por inanição a cargo de Eder.

---

## [M3b-possession-feel] — Tecla, Conjuração e Feedback da Possessão (`feat/M3b-possession-feel`)

**Spec de referência:** `docs/specs/M3b-possession-feel.md`  
**Commits:**
- `0f42433` — `feat(art): atualizar modelo e animacao animation.wisp.possess e adicionar stopPossess`
- `2e465c7` — `feat(sound): trocar passos do wisp por entity.wisp.drift em AtEntity e AtPosition`
- `e2bc03f` — `feat(host): conjuracao canalizada com tecla R, indicador de alvo e particulas do host` (une os antigos `4995220` e `edd8716`, que nao compilavam separados)
- `bad472c` — `docs(host): preencher relatorio da spec M3b e atualizar CHANGELOG.md`

### 1. Resumo das Alterações (`main..feat/M3b-possession-feel`)
- Atualização dos assets 3D do wisp (`art/parasita-magico/` → `assets/wispborn/`) com fragmentos volumétricos nos raios e clipe `animation.wisp.possess` (1,0 s, sem loop), além de `WispAnimatable.stopPossess(ServerPlayer)`.
- Substituição dos sons de passo do wisp (`PlayLevelSoundEvent.AtEntity` e `AtPosition`) por `ModSounds.WISP_DRIFT` (`entity.wisp.drift` → `block.amethyst_block.chime`) a cada 10 ticks e cancelamento de `block.*.fall`, governados por `WispSoundRules`.
- Substituição do clique direito na entidade pela tecla `R` (`KeyBindings.POSSESS`) com conjuração canalizada (`possess.castTicks = 20`, `possess.range = 4.0`), payloads `PossessStartPayload`, `PossessCancelPayload`, `CastStatePayload` e `TargetInfoPayload`, e regra pura `CastRules.cancelReason`.
- Indicador de alvo no HUD (`wispborn:possess_target` via `RegisterGuiLayersEvent#registerAboveAll`) com nome, tier, custo de mana, status e barra de progresso, anel de 6 partículas `DustParticleOptions` nos pés do alvo e efeito `ColorParticleOption(ENTITY_EFFECT, 0xFF40E0D0)` no mob possuído a cada 10 ticks.

### 2. Detalhamento por Arquivo

| Arquivo | Ação | Detalhes da Implementação |
|---|---|---|
| `art/parasita-magico/*` e `assets/wispborn/{geo,animations,textures}/entity/wisp*` | Modificado | Arte atualizada do Prompt A1 (raios volumétricos e `animation.wisp.possess` substituindo `attack`). |
| `src/main/java/dev/eder/wispborn/form/WispAnimations.java` | Modificado | `POSSESS` alterado para `"animation.wisp.possess"`. |
| `src/main/java/dev/eder/wispborn/form/WispAnimatable.java` | Modificado | Adicionado `stopPossess(ServerPlayer)` chamando `INSTANCE.stopTriggeredAnim`. |
| `src/main/java/dev/eder/wispborn/form/WispTuning.java` | Modificado | Adicionadas constantes `DRIFT_SOUND_INTERVAL_TICKS = 10`, `TARGET_OK_COLOR = 0x40E0D0`, `TARGET_FAIL_COLOR = 0xFF4444` e `POSSESSED_PARTICLE_COLOR = 0xFF40E0D0`. |
| `src/main/java/dev/eder/wispborn/registry/ModSounds.java` e `assets/wispborn/sounds.json` | Criado | Registro de `WISP_DRIFT` (`entity.wisp.drift`) apontando para `block.amethyst_block.chime` (`volume 0.35`, `pitch 1.4`). |
| `src/main/java/dev/eder/wispborn/form/WispSoundRules.java` e `event/WispSounds.java` | Modificado | `classify` (`STEP`, `FALL`, `OTHER`) e `shouldPlayDrift`; listeners para `AtEntity` e `AtPosition`. |
| `src/main/java/dev/eder/wispborn/config/WispbornConfig.java` | Modificado | Adicionados `possess.castTicks` (20) e `possess.range` (4.0). |
| `src/main/java/dev/eder/wispborn/host/{CancelReason,CastRules,InfestResult}.java` | Criado/Mod. | Enum `CancelReason`, função pura `CastRules.cancelReason` e `InfestResult.STREAM_CODEC`. |
| `src/main/java/dev/eder/wispborn/network/{PossessStartPayload,PossessCancelPayload,CastStatePayload,TargetInfoPayload,ModPayloads}.java` | Criado/Mod. | Payloads C2S/S2C de conjuração e indicador de alvo registrados em `ModPayloads`. |
| `src/main/java/dev/eder/wispborn/host/HostManager.java` | Modificado | `handlePossessStart`, `cancelCast`, `notifyDamageTaken`, raycast de alvo a cada 4 ticks com envio só quando muda, e partículas `ENTITY_EFFECT` no hospedeiro a cada 10 ticks. |
| `src/main/java/dev/eder/wispborn/event/{HostEvents,WispRestrictions}.java` | Modificado | `EntityInteract` volta a ser apenas cancelado em `WispRestrictions`; `HostEvents.onLivingIncomingDamage` notifica dano no wisp durante conjuração. |
| `src/main/java/dev/eder/wispborn/client/{KeyBindings,ClientPossession,ClientPayloadHandler,WispRenderHandler}.java` | Criado/Mod. | Tecla `R`, estado client-side, camada de HUD `wispborn:possess_target` e anel de partículas `DustParticleOptions` nos pés do alvo. |
| `src/test/java/dev/eder/wispborn/{form/WispSoundRulesTest,host/CastRulesTest,network/PossessionPayloadsTest}.java` | Criado/Mod. | Testes JUnit 5 cobrindo regras de som, cancelamento de conjuração e `StreamCodec` dos 3 payloads. |
| `docs/specs/M3b-possession-feel.md` | Modificado | Preenchido "Dúvidas do implementador" e "Relatório de implementação". |

### 3. Evidências de Verificação

- **`./gradlew build --no-daemon`:**
  ```text
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  > Task :check
  > Task :build
  BUILD SUCCESSFUL in 14s
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
  [09:29:43] [Server thread/INFO] [minecraft/DedicatedServer]: Done (3.712s)! For help, type "help"
  [09:29:43] [Server thread/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```

### 4. Alterações fora do escopo / Decisões de borda
| Arquivo | Alteração / Decisão | Motivo / Impacto |
|---|---|---|
| `WispSounds.java` | Mapas de cooldown separados por lado (`CLIENT_LAST_DRIFT` e `SERVER_LAST_DRIFT`) | Em singleplayer, o cliente local dispara `AtEntity` e o servidor integrado dispara `AtPosition` no mesmo tick; separar os mapas evita que um lado consuma o cooldown do outro. |
| `HostManager.java` | Clamping da distância do raycast de entidade pelo `player.pick(range, 1.0F, false)` de blocos | Garante que o indicador de alvo não selecione criaturas atrás de paredes/blocos sólidos antes mesmo do `hasLineOfSight`. |

### 5. Pendências
- [ ] **Checklist manual in-game (`./gradlew runClient` / `runClient2`):** validação visual dos novos raios, som de cintilação, indicador de alvo, barra de conjuração e partículas do hospedeiro a cargo de Eder.

---

## [M3-possession] — Possessão como Passageiro (`feat/M3-possession`)

**Spec de referência:** `docs/specs/M3-possession.md`  
**Commits:**
- `d91a9e2` — `feat(host): implementar InfestResult, InfestRules e HostManager com animacao de 16 ticks`
- `5ff8646` — `feat(event): adicionar HostEvents, seguranca de login/respawn e traducoes de possessao`
- `1b6b8d8` — `test(host): adicionar InfestRulesTest cobrindo precedencia das validacoes`
- `6a5ce04` — `docs(host): preencher relatorio da spec M3 e atualizar CHANGELOG.md`

### 1. Resumo das Alterações (`main..feat/M3-possession`)
- **Total:** 12 arquivos alterados (`560 insertions(+)`, `18 deletions(-)`), sem contar CHANGELOG e docs. Os hashes acima são os de depois do rebase sobre `372d365` (spec M3b).
- Implementação da validação pura de possessão (`InfestRules.check` → `InfestResult`) respeitando a ordem exata da spec (mob vivo → sem passageiro → tier presente → tier ≤ maxTier → mana suficiente), coberta por `InfestRulesTest`.
- Implementação de `HostManager`: validação inicial em `tryInfest`, disparo de `WispAnimatable.playPossess(player)` com espera de 16 ticks em memória (`Map<UUID, PendingInfest>`), revalidação completa ao fim dos 16 ticks (incluindo distância ≤ 4 blocos), gasto de mana (`infestCostPerTier × tier`), montagem como passageiro invisível (`startRiding(mob, true)`, `FormState.host(id)`, `setInvisible(true)`), HUD na action bar (`Hospedeiro: <Mob> · <Vida>%`) e expulsão idempotente via `release(ServerPlayer, Reason)`.
- Tratamento de todas as saídas e proteções em `HostEvents` e `FormEvents`: `SNEAK` (`EntityMountEvent`), `HOST_DIED` (`LivingDeathEvent`), `LOGOUT` (`PlayerLoggedOutEvent`), rede de segurança em login/respawn (`ensureValidHostState`) e invulnerabilidade na forma `HOST` (exceto `DamageTypeTags.BYPASSES_INVULNERABILITY`).

### 2. Detalhamento por Arquivo

| Arquivo | Ação | Detalhes da Implementação |
|---|---|---|
| `src/main/java/dev/eder/wispborn/host/InfestResult.java` | Criado | Enum `OK`, `NOT_A_MOB`, `HAS_PASSENGER`, `IMMUNE`, `TOO_STRONG`, `NO_MANA`. |
| `src/main/java/dev/eder/wispborn/host/InfestRules.java` | Criado | Função pura `check(boolean isMob, boolean alive, boolean hasPassenger, OptionalInt tier, int maxTier, float mana, int costPerTier) → InfestResult`. |
| `src/main/java/dev/eder/wispborn/host/HostManager.java` | Criado | `tryInfest`, `tick` (conclusão após 16 ticks + HUD a cada 20 ticks), `release(ServerPlayer, Reason)` (idempotente), `hostOf(Player)`, `isHost(Player)`, `ensureValidHostState` e `clearPending`. |
| `src/main/java/dev/eder/wispborn/event/HostEvents.java` | Criado | Listeners no `NeoForge.EVENT_BUS` para `EntityInteract`, `EntityMountEvent`, `LivingDeathEvent`, `PlayerLoggedOutEvent`, `LivingIncomingDamageEvent` e `PlayerTickEvent.Post`. |
| `src/main/java/dev/eder/wispborn/event/WispRestrictions.java` | Modificado | Removido `onEntityInteract` (substituído por `HostEvents.onEntityInteract`); restringido o uso de `wisp_essence` apenas à forma `WISP` (bloqueado em `HOST`). |
| `src/main/java/dev/eder/wispborn/event/FormEvents.java` | Modificado | Adicionado `HostManager.ensureValidHostState(serverPlayer)` em `onPlayerLoggedIn` e `onPlayerRespawn`. |
| `src/main/java/dev/eder/wispborn/form/FormManager.java` | Modificado | `setForm` limpa `setInvisible(false)` quando sai da forma `HOST`. |
| `src/main/java/dev/eder/wispborn/command/WispbornCommand.java` | Modificado | `/wispborn form` chama `HostManager.release` se o alvo estiver em `HOST` antes de trocar a forma. |
| `src/main/java/dev/eder/wispborn/WispbornMod.java` | Modificado | Registrado `HostEvents.class` no `NeoForge.EVENT_BUS`. |
| `src/main/resources/assets/wispborn/lang/{en_us,pt_br}.json` | Modificado | Adicionadas chaves `wispborn.infest.*`, `wispborn.host.died`, `wispborn.host.left` e `wispborn.hud.host`. |
| `src/test/java/dev/eder/wispborn/host/InfestRulesTest.java` | Criado | Testes unitários JUnit 5 validando todos os resultados e a ordem de avaliação de `InfestRules.check`. |
| `docs/specs/M3-possession.md` | Modificado | Preenchido "Dúvidas do implementador" e "Relatório de implementação". |

### 3. Evidências de Verificação

- **`./gradlew build --no-daemon`:**
  ```text
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  > Task :check
  > Task :build
  BUILD SUCCESSFUL in 12s
  ```
- **`./gradlew test --rerun-tasks --no-daemon`:**
  ```text
  > Task :compileJava
  > Task :classes
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  BUILD SUCCESSFUL in 12s
  ```
- **`./gradlew runServer --no-daemon`:**
  ```text
  [08:55:16] [Server thread/INFO] [minecraft/DedicatedServer]: Done (3.094s)! For help, type "help"
  [08:55:16] [Server thread/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```

### 4. Alterações fora do escopo / Decisões de borda
| Arquivo | Alteração / Decisão | Motivo / Impacto |
|---|---|---|
| `HostManager.java` | Aplicação da posição de saída de `Reason.SNEAK` no `PlayerTickEvent.Post` do mesmo tick (`PENDING_DISMOUNT_POS`) | No Minecraft 1.21.1, `EntityMountEvent` dispara dentro de `Entity.removeVehicle()` antes de `LivingEntity.dismountVehicle(vehicle)` calcular a posição lateral/chão do dismount. Reaplicar `teleportTo(mob.getX(), mob.getY() + mob.getBbHeight() + 0.1, mob.getZ())` no `PlayerTickEvent.Post` garante que o wisp sempre saia no topo da hitbox do mob. |
| `HostManager.java` | `player.clearFire()` enquanto `isHost(player)` | Evita que o passageiro invisível fique com chamas visuais na tela ao possuir um zumbi queimando ao sol ou um mob em lava/fogo. |
| `WispRestrictions.java` | `wisp_essence` liberada apenas quando `FormManager.isWisp`, não quando `HOST` | Impede que o jogador volte à forma humana ainda montado e invisível dentro de um hospedeiro. |

### 5. Pendências
- [ ] **Checklist manual in-game (`./gradlew runClient` / `runClient2`):** validação da possessão, IA do hospedeiro, invulnerabilidade e saídas a cargo de Eder.
- [ ] **CI:** `main` enviada em `6a5ce04` (com a M3 mergeada); acompanhar o run.
- [ ] **Merge antes do checklist:** a M3 foi mergeada na `main` antes do checklist in-game. O primeiro teste de Eder (17/11/2026) gerou a [M3b](docs/specs/M3b-possession-feel.md): tecla com conjuração no lugar do clique direito, indicador de alvo, efeito no mob possuído e correção do som de passos.

### 6. Revisão (Claude, 17/11/2026)
Código conferido sem bugs: `release` é idempotente (o `stopRiding` reentra no `EntityMountEvent`, mas a forma já é WISP); logout e morte do hospedeiro soltam o wisp no topo do mob; a rede de segurança de login/respawn cobre save com HOST sem veículo; a invulnerabilidade só deixa passar `BYPASSES_INVULNERABILITY`; um mob removido sem morrer (conversão, `discard`) cai no `release` pelo dismount do vanilla. O gatilho por clique direito e o atraso fixo de 16 ticks serão substituídos na M3b.

---

## [M2-level-stats] — Nível, Atributos e Mana (`feat/M2-level-stats`)

**Spec de referência:** `docs/specs/M2-level-stats.md`  
**Commits:**
- `c1cd6bc` — `feat(config): adicionar WispbornConfig SERVER e tags de tier de entidade (M2)`
- `266ff17` — `feat(progress): implementar nivel, EP, atributos, mana, funcoes puras e ProgressManager`
- `a8a881d` — `feat(command): adicionar HUD na action bar, regeneracao de mana e comandos ep/stat/mana/info`
- `0a5c766` — `test(progress): adicionar LevelCurveTest, TierResolverTest e StatMathTest`
- `ccf0102` — `docs(progress): preencher relatorio da spec M2 e atualizar CHANGELOG.md`
- `fac1c58` — `fix(progress): derivar nivel do EP e validar pontos apos mudanca da curva`

### 1. Resumo das Alterações (`main..feat/M2-level-stats`)
- **Total (implementação original, até `0a5c766`):** 26 arquivos alterados (`1012 insertions(+)`, `5 deletions(-)`), contando CHANGELOG e spec.
- Configuração completa de servidor (`WispbornConfig`, `ModConfig.Type.SERVER`) cobrindo `levels`, `tiers`, `stats`, `mana`, `drain` e `ep`.
- Persistência de progresso (`WispProgress`), atributos (`WispStats`) e mana (`Float`) via Data Attachments (`ModAttachments.PROGRESS`, `STATS`, `MANA`) com `copyOnDeath`.
- Funções puras testadas via JUnit 5: `LevelCurve`, `StatMath` e `TierResolver` (`maxTierFor`), além da resolução de tier por tags de entidade (`ModTags.TIER_1..3` e `data/wispborn/tags/entity_type/tier_1..3.json`).
- Gerenciamento no servidor via `ProgressManager` (`addEp`, `setEp`, `allocate`, `resetStats`, `refreshStats`, `addMana`, `spendMana`, `getMaxMana`, `buildWispHud`), aplicação do bônus de Vitalidade (`wispborn:wisp_vitality`) em `FormManager.applyModifiers`, regeneração de mana e HUD na action bar a cada 20 ticks na forma `WISP`, e extensão de `/wispborn` com `ep`, `stat`, `mana` e `info`.

### 2. Detalhamento por Arquivo

| Arquivo | Ação | Detalhes da Implementação |
|---|---|---|
| `src/main/java/dev/eder/wispborn/config/WispbornConfig.java` | Criado | `ModConfigSpec` (`SERVER`) com todas as 16 chaves da tabela da spec (`levels.thresholds`, `tiers.minLevel`, `stats.*`, `mana.*`, `drain.*`, `ep.*`). |
| `src/main/java/dev/eder/wispborn/WispbornMod.java` | Modificado | Registrado `modContainer.registerConfig(ModConfig.Type.SERVER, WispbornConfig.SPEC)`. |
| `src/main/java/dev/eder/wispborn/progress/Stat.java` | Criado | Enum `ABSORPTION`, `EFFICIENCY`, `RESERVE`, `VITALITY` com `StringRepresentable`, `CODEC`, `STREAM_CODEC`, `translationKey()` (`wispborn.stat.<id>`) e `byName(String)`. |
| `src/main/java/dev/eder/wispborn/progress/WispProgress.java` | Criado | Record `(int totalEp, int level)` com `START = new WispProgress(0, 1)` e `CODEC`. |
| `src/main/java/dev/eder/wispborn/progress/WispStats.java` | Criado | Record `(int absorption, int efficiency, int reserve, int vitality)` com `ZERO`, `spent()`, `get(Stat)`, `with(Stat, int)` e `CODEC`. |
| `src/main/java/dev/eder/wispborn/progress/AllocateResult.java` | Criado | Enum `OK`, `NO_POINTS`, `MAXED`. |
| `src/main/java/dev/eder/wispborn/progress/LevelCurve.java` | Criado | Funções puras `levelFor(int totalEp, List<Integer> thresholds)` e `nextThreshold(int totalEp, List<Integer> thresholds) → OptionalInt`. |
| `src/main/java/dev/eder/wispborn/progress/StatMath.java` | Criado | Funções puras `availablePoints`, `drainPercent`, `epMultiplier`, `applyEp`, `maxMana` e `vitalityBonusHealth`. |
| `src/main/java/dev/eder/wispborn/progress/TierResolver.java` | Criado | `maxTierFor(int level, List<Integer> minLevels)` (puro) e `tierOf(EntityType<?>) → OptionalInt` (maior tier entre `TIER_3`, `TIER_2`, `TIER_1`). |
| `src/main/java/dev/eder/wispborn/progress/ProgressManager.java` | Criado | `addEp` (com Eficiência, level-up, mensagem e som), `setEp` (com auto-reset se pontos disponíveis < 0), `allocate`, `resetStats`, `refreshStats` (reaplica Vitalidade curando a diferença e limita mana), `getMana`/`setMana`/`addMana`/`spendMana`/`getMaxMana` e `buildWispHud`. |
| `src/main/java/dev/eder/wispborn/registry/ModAttachments.java` | Modificado | Adicionados `PROGRESS` (`WispProgress.START`), `STATS` (`WispStats.ZERO`) e `MANA` (`50.0F`), todos com `.serialize(...)` e `.copyOnDeath()`. |
| `src/main/java/dev/eder/wispborn/registry/ModTags.java` | Criado | Tags `TIER_1`, `TIER_2`, `TIER_3` (`Registries.ENTITY_TYPE`). |
| `src/main/resources/data/wispborn/tags/entity_type/tier_1..3.json` | Criado | JSONs de tags para mobs tier 1 (galinha, porco, vaca, ovelha, coelho), tier 2 (zumbi, esqueleto, aranha) e tier 3 (creeper, enderman). |
| `src/main/java/dev/eder/wispborn/form/WispTuning.java` | Modificado | Adicionado `VITALITY_ID = wispborn:wisp_vitality`. |
| `src/main/java/dev/eder/wispborn/form/FormManager.java` | Modificado | `applyModifiers` remove e reaplica `wispborn:wisp_vitality` (`ADD_VALUE` `+vitalityBonusHealth`) nas formas `WISP` e `HOST`. |
| `src/main/java/dev/eder/wispborn/event/FormEvents.java` | Modificado | `onPlayerTickPost` (servidor, a cada 20 ticks): se `isWisp`, regenera mana (`mana.regenPerSecond`) e envia HUD para a action bar. |
| `src/main/java/dev/eder/wispborn/command/WispbornCommand.java` | Modificado | Adicionados `/wispborn ep add\|set <n> [player]`, `/wispborn stat add <stat> [player]` (com tab-complete), `/wispborn stat reset [player]`, `/wispborn mana set <n> [player]` e `/wispborn info [player]`. |
| `src/main/resources/assets/wispborn/lang/{en_us,pt_br}.json` | Modificado | Adicionadas traduções de atributos (`wispborn.stat.*`), level-up, HUD (`wispborn.hud.*`) e comandos. |
| `src/test/java/dev/eder/wispborn/progress/{LevelCurveTest,TierResolverTest,StatMathTest}.java` | Criado | Testes unitários JUnit 5 cobrindo todos os cenários da spec M2. |
| `docs/specs/M2-level-stats.md` | Modificado | Preenchido "Dúvidas do implementador" e "Relatório de implementação". |

### 3. Evidências de Verificação

- **`./gradlew build --no-daemon`:**
  ```text
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  > Task :check
  > Task :build
  BUILD SUCCESSFUL in 32s
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
  [08:19:04] [Server thread/DEBUG] [ne.ne.fm.co.ConfigTracker/CONFIG]: Loaded TOML config file D:\Projetos\Mine Mod\run\config\wispborn-server.toml
  [08:19:07] [Server thread/INFO] [minecraft/DedicatedServer]: Done (3.238s)! For help, type "help"
  [08:19:07] [Server thread/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```

### 4. Alterações fora do escopo / Decisões de borda
| Arquivo | Alteração / Decisão | Motivo / Impacto |
|---|---|---|
| `ProgressManager.java` | Composição do level-up com `wispborn.level_up` + `wispborn.level_up.tier_unlocked` opcional | Evita exibir texto de tier quando o novo nível não destrava um novo tier (ex.: Nv 1 → Nv 2 continua tier 1; Nv 2 → Nv 3 anexa `(Tier 2 liberado!)`). |
| `ProgressManager.java` | Composição do HUD com `wispborn.hud.wisp`, `wispborn.hud.max` e `wispborn.hud.points` | Garante formatação idêntica aos exemplos da spec (`Wisp · Nv 1 · EP 0/300 · Mana 50/50`, `Wisp · Nv 2 · EP 450/800 · Mana 35/60 · 2 pts` e `EP 18450 · máx` no nível máximo). |

### 4.1 Ajustes pós-revisão (`fac1c58`, Claude)
Problemas achados na revisão do Claude e corrigidos a pedido de Eder. Detalhes na seção "Ajustes pós-revisão" da spec.

| Arquivo | Alteração | Motivo / impacto |
|---|---|---|
| `WispProgress.java`, `ProgressManager.java`, `WispbornCommand.java` | `WispProgress` guarda só `totalEp`; o nível vem sempre de `ProgressManager.level(p)` = `LevelCurve.levelFor(totalEp, thresholds)` | Com o nível guardado, editar `levels.thresholds` deixava HUD, pontos e tier com o nível antigo até o próximo ganho de EP, o que falhava o último item do checklist. A falha vinha do contrato da spec (escrito pelo Claude), agora atualizado. Saves com o campo `level` continuam carregando. |
| `ProgressManager.java`, `FormEvents.java` | `validateStats(p)` no tick de 20 em 20 ticks (mesmo reset automático do `ep set`) | Se a curva nova baixar o nível, os atributos excedentes são zerados com aviso no login ou na recarga do config. Nunca há pontos negativos. |
| `WispbornCommand.java`, `lang/{en_us,pt_br}.json` | Nome da forma via `wispborn.form.<human\|wisp\|host>`; EP máximo via `wispborn.hud.max` | `/wispborn info` e `/wispborn form` mostravam `wisp` cru e `" (max)"` em inglês, fora da lang. |
| `WispbornConfig.java` | `levelThresholds()` devolve a lista ordenada e sem repetição | `LevelCurve` assume limites crescentes; uma lista fora de ordem no config travava o nível. |
| `WispProgressTest.java` (novo) | Round-trip do codec, save legado com `level` e nível seguindo a curva | 3 testes; suíte total: 19 testes verdes. |

Fica como está (por decisão): a mana inicial do attachment é `50.0F` fixo, porque o config SERVER ainda não está carregado no registro. Com `mana.base` diferente, o jogador novo começa em 50 e regenera até o máximo.

### 5. Pendências
- [ ] **Checklist manual in-game (`./gradlew runClient`):** validação do HUD, comandos, atributos e recarregamento de `wispborn-server.toml` a cargo de Eder (repetir o item de recarregar config depois de `fac1c58`).
- [ ] **CI:** depende do push da `feat/M2-level-stats`.

---

## [M1b-wisp-model] — Modelo e Animações do Wisp com GeckoLib (`feat/M1b-wisp-model`)

**Spec de referência:** `docs/specs/M1b-wisp-model.md`  
**Commits:**
- `69f421d` — `build(deps): adicionar dependencia obrigatoria GeckoLib 4.9.3`
- `f4451ed` — `feat(assets): copiar modelo, animacoes e texturas do wisp e remover billboards antigos`
- `77c9c58` — `feat(form): implementar WispAnimations, WispAnimatable, ajuste de escala e /wispborn anim possess`
- `a9ae193` — `feat(client): renderizar wisp com WispPlayerRenderer, WispModel e adicionar WispAnimationsTest`
- `713d257` — `docs(form): preencher relatorio da spec M1b e atualizar CHANGELOG.md`
- `ff98f9a` — `fix(wisp): aumentar visual 2x, silenciar passos e remover particulas END_ROD`

### 1. Resumo das Alterações (`main..feat/M1b-wisp-model`)
- **Total:** 23 arquivos alterados (`2348 insertions(+)`, `105 deletions(-)`), sem contar CHANGELOG e docs; quase todo o volume são os JSONs do modelo e das animações.
- Integração com **GeckoLib 4.9.3** (`software.bernie.geckolib:geckolib-neoforge-1.21.1:4.9.3`) no `build.gradle`, `gradle.properties` e `neoforge.mods.toml`.
- Ajuste de `WispTuning.SCALE_MODIFIER` de `-0.6` para `-0.52` (escala `0.48`, ~20% maior que no M1).
- Cópia dos assets exportados do Blockbench (`art/parasita-magico/`) para `assets/wispborn/` (`geo/entity/wisp.geo.json`, `animations/entity/wisp.animation.json`, `textures/entity/wisp.png`, `textures/entity/wisp_glowmask.png`) e remoção das texturas antigas `wisp_core.png` e `wisp_halo.png`.
- Implementação de `WispAnimations` (constantes puras), `WispAnimatable` (`GeoReplacedEntity` singleton registrado com `SingletonGeoAnimatable.registerSyncedAnimatable`), `WispModel`, `WispPlayerRenderer` (com `AutoGlowingGeoLayer`), atualização de `WispRenderHandler`, subcomando `/wispborn anim possess [player]` e teste JUnit `WispAnimationsTest`.

### 2. Detalhamento por Arquivo

| Arquivo | Ação | Detalhes da Implementação |
|---|---|---|
| `gradle.properties` | Modificado | Adicionado `geckolib_version=4.9.3`. |
| `build.gradle` | Modificado | Adicionado repositório Cloudsmith do GeckoLib, dependência `geckolib-neoforge-1.21.1:${geckolib_version}` e propriedade `geckolib_version` em `generateModMetadata`. |
| `src/main/templates/META-INF/neoforge.mods.toml` | Modificado | Adicionado bloco `[[dependencies.${mod_id}]]` obrigatório para `geckolib` com `versionRange="[${geckolib_version},)"` e `side="BOTH"`. |
| `src/main/java/dev/eder/wispborn/form/WispTuning.java` | Modificado | `SCALE_MODIFIER` alterado de `-0.6` para `-0.52`. |
| `src/main/java/dev/eder/wispborn/form/WispAnimations.java` | Criado | Constantes `IDLE`, `WALK`, `POSSESS`, `CONTROLLER_MOVEMENT`, `CONTROLLER_POSSESS` e `TRIGGER_POSSESS` sem dependência do GeckoLib. |
| `src/main/java/dev/eder/wispborn/form/WispAnimatable.java` | Criado | Singleton `GeoReplacedEntity` (`EntityType.PLAYER`) com controllers `movement` (transição 5 ticks, `WALK`/`IDLE`) e `possess` (transição 0, trigger `possess` → `POSSESS`), mais `playPossess(ServerPlayer)`. |
| `src/main/java/dev/eder/wispborn/WispbornMod.java` | Modificado | Adicionada chamada `SingletonGeoAnimatable.registerSyncedAnimatable(WispAnimatable.INSTANCE)` no construtor. |
| `src/main/java/dev/eder/wispborn/client/WispModel.java` | Criado | Subclasse de `DefaultedEntityGeoModel<WispAnimatable>` (`wispborn:wisp`) com `getRenderType` retornando `RenderType.entityTranslucent(texture)`. |
| `src/main/java/dev/eder/wispborn/client/WispPlayerRenderer.java` | Criado | Subclasse de `GeoReplacedEntityRenderer<AbstractClientPlayer, WispAnimatable>` adicionando `AutoGlowingGeoLayer`. |
| `src/main/java/dev/eder/wispborn/client/WispRenderHandler.java` | Modificado | Instancia `WispPlayerRenderer` em `EntityRenderersEvent.AddLayers` e renderiza o modelo 3D em `RenderPlayerEvent.Pre` (removendo os quads billboard do M1); ignora render se `renderer == null` ou `player.isInvisible()`. |
| `src/main/java/dev/eder/wispborn/command/WispbornCommand.java` | Modificado | Adicionado `/wispborn anim possess [player]` (permissão 2) chamando `WispAnimatable.playPossess(target)`. |
| `src/main/resources/assets/wispborn/` | Modificado | Copiados `geo/entity/wisp.geo.json`, `animations/entity/wisp.animation.json`, `textures/entity/wisp.png` e `textures/entity/wisp_glowmask.png`; removidos `textures/entity/wisp_core.png` e `wisp_halo.png`; adicionada chave `commands.wispborn.anim.success` em `en_us.json` e `pt_br.json`. |
| `src/test/java/dev/eder/wispborn/form/WispAnimationsTest.java` | Criado | Teste JUnit 5 que lê `/assets/wispborn/animations/entity/wisp.animation.json` via Gson e valida existência e flag `loop` de `IDLE`, `WALK` e `POSSESS`. |
| `docs/specs/M1b-wisp-model.md` | Modificado | Preenchido "Dúvidas do implementador" e "Relatório de implementação". |

### 3. Evidências de Verificação

- **`./gradlew build --no-daemon`:**
  ```text
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  > Task :check
  > Task :build
  BUILD SUCCESSFUL in 44s
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
  [22:15:00] [Server thread/INFO] [minecraft/DedicatedServer]: Done (3.136s)! For help, type "help"
  [22:15:00] [Server thread/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```

### 4. Alterações fora do escopo / Decisões de borda
| Arquivo | Alteração / Decisão | Motivo / Impacto |
|---|---|---|
| `WispModel.java` | Override de `getRenderType` retornando `RenderType.entityTranslucent(texture)` | O padrão de `GeoModel` é `entityCutoutNoCull` (que descarta alfa parcial). Como `wisp.png` possui feixes semitransparentes e o checklist exige translucidez frente a água/vidro/partículas, `entityTranslucent` habilita o blending alfa correto. |

### 4.1 Ajustes pós-teste em jogo (`ff98f9a`, Claude)
Primeiro teste de Eder em jogo: o modelo renderiza no jogador com `idle`/`walking` (prova de conceito ok), mas com três problemas. Detalhes e decisões na seção "Ajustes pós-teste" da spec.

| Arquivo | Alteração | Motivo / impacto |
|---|---|---|
| `WispTuning.java`, `WispPlayerRenderer.java` | `MODEL_VISUAL_SCALE = 2.0F` aplicado com `withScale` | O modelo ficava pequeno demais (núcleo ~0,15 bloco). Decisão de Eder: visual 2× só no render; a hitbox (atributo `SCALE`, 0,48) não muda e continua passando em vão de 1 bloco. |
| `event/WispSounds.java` (novo), `form/WispSoundRules.java` (novo), `WispbornMod.java` | Cancela `PlayLevelSoundEvent.AtPosition` de `block.*.step`/`block.*.fall` na posição de um jogador WISP | Passos de bípede quebravam a imersão. `setSilent` não serve porque `Player#playSound` ignora a flag. O evento dispara no client (passo próprio) e no servidor (transmissão aos outros). |
| `WispRenderHandler.java` | Removidas as partículas `END_ROD` do `ClientTickEvent.Post` | Apareciam como "fumaça" branca grande ao lado do wisp pequeno. O modelo já tem rastro próprio (`trail_1..4`). |
| `WispSoundRulesTest.java` (novo) | Testes JUnit da regra de som | 2 testes; total da suíte: 8 testes verdes. |

### 5. Pendências
- [x] **Prova de conceito do render no jogador:** confirmada por Eder no primeiro teste em jogo.
- [ ] **Checklist manual in-game (`./gradlew runClient` / `runClient2`):** repetir depois de `ff98f9a`, com tamanho 2×, passos silenciosos e sem partículas, além dos itens originais (glowmask, translucidez, direção do movimento, multiplayer).
- [x] **CI:** verde em `82ab8ba` na `feat/M1b-wisp-model` (GitHub Actions, 17/11/2026).

---

## [M1-wisp-form] — Forma Wisp (`feat/M1-wisp-form`)

**Spec de referência:** `docs/specs/M1-wisp-form.md`  
**Commits:**
- `c6ded5b` — `feat(form): implementar FormType, FormState, WispTuning, FormManager, ModAttachments e wisp_essence`
- `3593c1b` — `feat(net): adicionar SyncFormPayload, eventos de forma, restricoes do wisp e comando /wispborn`
- `adc4743` — `feat(client): implementar WispRenderHandler, texturas placeholder e traducoes do M1`
- `c9c881a` — `test(form): adicionar FormStateTest e configurar classpath de testes no build.gradle`
- `c930c67` — `docs(form): preencher relatorio da spec M1 e atualizar CHANGELOG.md`
- `60ddc3a` — `docs(form): atualizar hashes do CHANGELOG apos rebase e registrar pendencia de vida`
- `ee30b2b` — `fix(form): preservar proporcao de vida ao alternar forma em FormManager.setForm`

### 1. Resumo das Alterações (`main..feat/M1-wisp-form`)
- **Total:** 24 arquivos alterados (`741 insertions(+)`, `4 deletions(-)`), sem contar o próprio CHANGELOG.
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
| `FormManager.java` | Preservação da proporção `health / maxHealth` em `setForm` | Atende ao item do checklist *"Usar `wisp_essence` como wisp volta a humano com vida e tamanho normais"* (6/6 HP → 20/20 HP) sem permitir cura grátis ao alternar formas com vida baixa. |
| `WispRestrictions.java` | `event.setUseBlock(TriState.FALSE)` em `RightClickBlock` quando segurando `wisp_essence` | Garante que clicar com o botão direito olhando para um baú/bloco segurando `wisp_essence` não abra o baú, mas permita que o item `wisp_essence` seja ativado normalmente. |

### 5. Pendências
- [ ] **Checklist manual in-game (`./gradlew runClient` / `runClient2`):** validação visual e funcional a cargo de Eder.
- [x] **CI:** verde em `ee30b2b` na `main` e na `feat/M1-wisp-form` (GitHub Actions).
- [x] **Balanceamento (revisão do M1):** corrigido em `FormManager.setForm` para preservar a proporção `health / maxHealth` na troca de forma.

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
- `9a40048` — `docs(setup): registrar CI verde e correcao do gradlew`

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
