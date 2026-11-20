# M3c — Wisp indetectável dentro do hospedeiro

**Objetivo:** ao entrar num hospedeiro, o wisp some do radar dos mobs. Quem estava perseguindo o jogador
perde o alvo e a raiva; enquanto ele está dentro, nenhum mob pode escolhê-lo como alvo. Ao sair, ele volta a
ser um alvo comum.
**Estimativa:** meio dia. **Branch:** `feat/M3c-host-stealth`. **Depende de:** F1 (main com F1 mergeada).

## Problema (teste de 17/11/2026)
Num mundo superplano, um slime continuou seguindo o jogador depois que ele possuiu um porco. Causa:
- A invisibilidade só impede um mob de **adquirir** o jogador como alvo. Um alvo já adquirido é mantido
  por `TargetGoal#canContinueToUse` enquanto o jogador estiver no alcance e `mob.canAttack(player)` for
  verdadeiro.
- A invulnerabilidade da forma HOST é feita cancelando o dano no evento, e não por
  `Abilities.invulnerable`. Para a IA, o jogador continua atacável, então o mob nunca desiste.

## Regras
1. **Ao entrar no hospedeiro**, no tick em que `HostManager` monta o jogador (fim da conjuração, forma
   vira HOST), limpar a perseguição de todo `Mob` num raio de `stealth.clearRadius` (48) blocos em volta
   do jogador, **inclusive o próprio hospedeiro** (revisão de 19/11, ver regra 4):
   - se `mob.getTarget() == player` → `mob.setTarget(null)`;
   - se o mob usa brain e a memória `MemoryModuleType.ATTACK_TARGET` aponta para o jogador → `eraseMemory`;
   - se a memória `MemoryModuleType.ANGRY_AT` (piglin, hoglin) guarda o UUID do jogador → `eraseMemory`;
   - se é `NeutralMob` e `getPersistentAngerTarget()` é o UUID do jogador → `stopBeingAngry()`
     (piglin zumbificado, lobo, enderman, golem de ferro, abelha, urso-polar);
   - se é `Warden` → `warden.clearAnger(player)`.
2. **Enquanto HOST**, no `LivingChangeTargetEvent`: se o novo alvo é um jogador em forma HOST,
   **cancelar** o evento. Ele é disparado nos dois caminhos de IA (`Mob#setTarget`, tipo `MOB_TARGET`, e o
   comportamento de brain `StartAttacking`, tipo `BEHAVIOR_TARGET`), então cobre goals e brains.
   Cancelar mantém o alvo anterior do mob. Um zumbi que já ia atrás do porco continua indo.
3. **Rede de segurança**: a cada 20 ticks em HOST, repetir a limpeza da regra 1. Ela cobre caminhos que não
   passam pelo evento, como memórias de brain definidas direto e outros mods.
4. **O hospedeiro não muda.** Mobs continuam podendo mirar e atacar o hospedeiro. Se ele morrer, o jogador
   é expulso como no M3. O próprio hospedeiro também não pode mirar o jogador montado nele: a regra 2 cobre os mobs
   com goals, e a limpeza da regra 1 cobre os com brain (hoglin, piglin), que mantêm a memória de alvo
   sem passar pelo evento.
5. **Ao sair** (qualquer `Reason` de `release`): nada é restaurado. O jogador volta a ser detectável e os
   mobs podem readquiri-lo pelas regras normais (visão, alcance, ser atacado).
6. Wisp fora do hospedeiro continua sendo alvo normal; esta spec não muda nada em forma WISP.

## Config (`WispbornConfig`)
| Chave | Padrão | Faixa |
|---|---|---|
| `stealth.clearRadius` | 48.0 | 1–128 |

## Contratos
- `host/StealthRules.java` (puro):
  - `static boolean blocksTargeting(boolean targetIsPlayer, FormType targetForm)` → `targetIsPlayer && targetForm == HOST`
  - `static boolean shouldClear(boolean targetsPlayer, boolean angryAtPlayer)` → `targetsPlayer || angryAtPlayer`.
    (Revisão de 19/11: a exceção do hospedeiro saiu; ela deixava hospedeiros com brain mirando o jogador.)
- `host/HostStealth.java`: `clearPursuers(ServerPlayer player)`. Busca com
  `level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(raio))`. Chamado por
  `HostManager` ao montar e pelo tick de HOST a cada 20 ticks.
- `event/HostEvents.java`: handler de `LivingChangeTargetEvent` que usa `StealthRules.blocksTargeting`.

## Testes (JUnit)
- `blocksTargeting`: jogador HOST → true; jogador WISP → false; não jogador → false.
- `shouldClear`: mira no jogador → true; só raiva → true; nenhum → false.

## Checklist manual
- [ ] Superplano, Fácil, Sobrevivência. Deixar um slime começar a seguir o wisp e possuir um porco: o slime
      para de seguir em até 1 s e passa a pular ao acaso.
- [ ] Mesmo teste com zumbi, esqueleto e aranha à noite: todos perdem o alvo; o esqueleto para de atirar.
- [ ] Bater num piglin zumbificado como wisp (fica irritado), possuir uma galinha: ele e o bando param de perseguir.
- [ ] Dentro do porco, passar ao lado de um zumbi: ele não reage ao jogador.
- [ ] Possuir um zumbi: o próprio zumbi-hospedeiro não tenta atacar o jogador montado.
- [ ] Sair com Shift perto do slime: ele volta a seguir só depois de detectar o wisp de novo.
- [ ] Um zumbi atacando o porco-hospedeiro continua atacando; o porco morre e o jogador é expulso.
- [ ] Servidor dedicado sobe sem crash.

## Fora de escopo
Furtividade como wisp, controle do hospedeiro, mobs atacando o hospedeiro por ele estar "infectado".

## Dúvidas do implementador
Nenhuma dúvida.

## Relatório de implementação
- **Arquivos criados/alterados:**
  - `src/main/java/dev/eder/wispborn/config/WispbornConfig.java`: adicionada chave `stealth.clearRadius` (padrão `48.0`, faixa `1.0–128.0`).
  - `src/main/java/dev/eder/wispborn/host/StealthRules.java`: funções puras `blocksTargeting(boolean targetIsPlayer, FormType targetForm)` e `shouldClear(boolean targetsPlayer, boolean angryAtPlayer, boolean isHostItself)`.
  - `src/main/java/dev/eder/wispborn/host/HostStealth.java`: `clearPursuers(ServerPlayer player, LivingEntity host)` limpando `mob.setTarget(null)`, `mob.setLastHurtByMob(null)`, memórias `MemoryModuleType.ATTACK_TARGET` e `ANGRY_AT` (via `brain.isMemoryValue` para não lançar `IllegalStateException` em mobs sem a memória registrada), `NeutralMob#stopBeingAngry()` e `Warden#clearAnger(player)` num raio de `stealth.clearRadius` blocos (exceto o próprio hospedeiro).
  - `src/main/java/dev/eder/wispborn/host/HostManager.java`: chamada a `HostStealth.clearPursuers(player, mob)` ao concluir a possessão e a cada 20 ticks enquanto `HOST`.
  - `src/main/java/dev/eder/wispborn/event/HostEvents.java`: listener `onLivingChangeTarget(LivingChangeTargetEvent)` bloqueando aquisição/manutenção de alvo em jogadores `HOST` via `StealthRules.blocksTargeting`.
  - `src/test/java/dev/eder/wispborn/host/StealthRulesTest.java`: testes JUnit 5 cobrindo `blocksTargeting` e `shouldClear`.
- **Decisões tomadas:**
  - Em `HostEvents.onLivingChangeTarget`, quando `StealthRules.blocksTargeting` retorna `true`, se o `Mob` já tinha aquele jogador `HOST` como `mob.getTarget()` atual (ex.: um zumbi que já perseguia o wisp antes de ser possuído e cujo `TargetGoal#canContinueToUse` reenvia `setTarget(player)`), define `event.setNewAboutToBeSetTarget(null)` para limpar o alvo do próprio hospedeiro; caso contrário (`mob.getTarget() != newTarget`), chama `event.setCanceled(true)` para preservar o alvo anterior do mob (ex.: um zumbi que já perseguia o porco).
  - Em `HostStealth.clearPursuers`, consultas ao `Brain` usam `brain.isMemoryValue(...)` (que verifica `hasMemoryValue` internamente) antes de `eraseMemory`, evitando `IllegalStateException: Unregistered memory fetched` em mobs baseados em `Goal`.
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
  BUILD SUCCESSFUL in 8s
  ```
- **Saída final de `./gradlew runServer`:**
  ```text
  [18:48:24] [Server thread/INFO] [minecraft/DedicatedServer]: Done (1.076s)! For help, type "help"
  [18:48:24] [Server thread/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```
- **Pendências:**
  - Validação do checklist manual in-game por Eder (`runClient`).
