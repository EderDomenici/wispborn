# M3 — Possessão (passageiro, sem controle)

**Objetivo:** o wisp entra num mob permitido pelo seu nível e fica dentro dele como passageiro
invisível e invulnerável; o mob mantém a própria IA. Sair, morte do mob e logout expulsam o wisp
de forma limpa.
**Estimativa:** 2 dias. **Branch:** `feat/M3-possession`. **Depende de:** M2 (nível, tiers, mana).

## Regras
1. Gatilho: wisp (forma WISP) clica com o botão direito num `Mob` (`PlayerInteractEvent.EntityInteract`,
   só lado servidor, só `InteractionHand.MAIN_HAND`).
2. Validações no servidor, nesta ordem (falhou → mensagem na action bar + `SoundEvents.FIRE_EXTINGUISH`, cancela o evento):
   - alvo é `Mob`, está vivo e **não tem passageiros**;
   - `TierResolver.tierOf(alvo)` presente → senão `wispborn.infest.immune`;
   - `tier ≤ maxTierFor(nível)` → senão `wispborn.infest.too_strong` (mostra tier e nível necessário);
   - `mana ≥ infestCostPerTier × tier` → senão `wispborn.infest.no_mana` (`Mana insuficiente: 12/30`).
3. Animação de possessão (M1b): validações ok → `WispAnimatable.playPossess(player)` (animação `attack`, 0,8 s)
   e a possessão só se completa **16 ticks depois** (o render da forma HOST esconde o wisp, então a
   animação precisa tocar antes). No fim da espera, revalidar tudo da regra 2 (mob vivo, sem passageiro,
   distância ≤ 4 blocos, mana) e o jogador continuar WISP; se algo falhar, cancela sem gastar mana.
   Uma possessão pendente por jogador (guardar alvo + tick de início no servidor, não em attachment).
   Detalhes (onde guardar o pendente, mensagem de cancelamento) ficam com o implementador do M3.
   Sucesso:
   - `ProgressManager.spendMana(player, custo)`;
   - `player.startRiding(mob, true)`;
   - `FormManager.setForm(player, FormState.host(<id do tipo do mob>))`;
   - `player.setInvisible(true)`;
   - som `SoundEvents.ALLAY_ITEM_GIVEN` e partículas `END_ROD` subindo no mob.
4. O mob **continua com a IA** (jogador passageiro não controla mob comum). Não mexer em `getControllingPassenger`.
5. Enquanto HOST:
   - invulnerável: `LivingIncomingDamageEvent` cancelado para o jogador, **exceto** fontes com
     `DamageTypeTags.BYPASSES_INVULNERABILITY` (void, `/kill`);
   - restrições do M1 continuam valendo (`isParasite`);
   - HUD (action bar, a cada 20 ticks): `Hospedeiro: Vaca · 100%` (nome traduzido do mob + vida %).
     No M4 esse texto ganha EP. A mana não regenera enquanto HOST (regra do M2).
6. Saída (todas passam por `host/HostManager.release(ServerPlayer, Reason)`):
   - `SNEAK` — desmontar vanilla com shift. Capturar com `EntityMountEvent` (`isDismounting()`, entidade = nosso jogador HOST).
   - `HOST_DIED` — `LivingDeathEvent` de um mob que tem um passageiro nosso em forma HOST → `release` **na hora**
     (não esperar a animação de morte ejetar o passageiro). Mensagem `wispborn.host.died`.
     O M5 usa o mesmo ponto com o motivo `CONSUMED`.
   - `LOGOUT` — `PlayerLoggedOutEvent`: chamar `player.stopRiding()` antes de salvar, para o mob não ser salvo junto com o jogador.
   - `release` é **idempotente**: se a forma já não é HOST, não faz nada (o dismount vanilla que vem depois
     da morte chama de novo).
   - `release` faz: `setForm(WISP)`, `setInvisible(false)`, teleporta o wisp para o topo da hitbox do mob
     (`mob.getY() + mob.getBbHeight() + 0.1`) para não ficar preso em bloco, e som de saída.
7. Rede de segurança no login e no respawn: se a forma é HOST mas `player.getVehicle() == null` → `setForm(WISP)` + `setInvisible(false)`.
8. A forma HOST é sincronizada pelo `SyncFormPayload` do M1 (o render do client já esconde o jogador e a mão).

## Contratos
- `host/HostManager.java`: `tryInfest(ServerPlayer, Mob) → InfestResult`, `release(ServerPlayer, Reason)`,
  `Optional<LivingEntity> hostOf(Player)` (vehicle, se forma HOST e vehicle é `LivingEntity`).
- `host/InfestResult.java`: enum `OK, NOT_A_MOB, HAS_PASSENGER, IMMUNE, TOO_STRONG, NO_MANA`.
- `event/HostEvents.java`: listeners de `EntityInteract`, `EntityMountEvent`, `LivingDeathEvent`, `PlayerLoggedOutEvent`,
  `LivingIncomingDamageEvent`, HUD do host.
- Em `WispRestrictions`, o cancelamento de `EntityInteract` do M1 sai e passa a ser a infestação.

## Lang
`wispborn.infest.immune`, `wispborn.infest.too_strong`, `wispborn.infest.no_mana`, `wispborn.infest.ok`, `wispborn.host.died`,
`wispborn.host.left`, `wispborn.hud.host`.

## Testes
- JUnit: lógica de validação extraída para função pura
  `InfestRules.check(boolean isMob, boolean alive, boolean hasPassenger, OptionalInt tier, int maxTier, float mana, int costPerTier)`
  → `InfestResult`, cobrindo todos os casos e a ordem das validações (tier é checado antes da mana).

## Checklist manual
- [ ] Nv 1: entra em vaca, porco, galinha, ovelha, coelho. Fica invisível, a câmera segue o mob, o mob anda sozinho.
- [ ] Nv 1: zumbi → "forte demais (tier 2, Nv 3)". Aldeão (sem tag) → "imune".
- [ ] Entrar numa vaca gasta 15 de mana (HUD). Sair, `/wispborn mana set 10`, tentar de novo → "Mana insuficiente".
- [ ] `/wispborn ep set 800` (Nv 3): entra em zumbi, esqueleto e aranha (custo 30 de mana).
- [ ] Shift sai: wisp aparece em cima do mob, visível, com a forma WISP.
- [ ] Matar o mob com `/kill @e[type=cow]` estando dentro: wisp é expulso com a mensagem de morte do hospedeiro.
- [ ] Dentro do zumbi ao amanhecer: o zumbi queima; o wisp não toma dano.
- [ ] Lava/cacto/queda com o hospedeiro: wisp não toma dano.
- [ ] Deslogar dentro do mob e relogar: volta como wisp fora do mob; o mob continua no mundo.
- [ ] Multiplayer (server + 2 clients): Player2 não vê o wisp dentro do mob, nem nameplate.
- [ ] Anotar qualquer comportamento estranho por mob (ex.: aranha com passageiro) na seção de relatório.

## Fora de escopo
Drenagem, EP por tempo, consumo, controlar o mob.

## Dúvidas do implementador
Nenhuma dúvida.

## Relatório de implementação
- **Arquivos criados/alterados:**
  - `src/main/java/dev/eder/wispborn/host/InfestResult.java`: enum `OK, NOT_A_MOB, HAS_PASSENGER, IMMUNE, TOO_STRONG, NO_MANA`.
  - `src/main/java/dev/eder/wispborn/host/InfestRules.java`: função pura `check(boolean isMob, boolean alive, boolean hasPassenger, OptionalInt tier, int maxTier, float mana, int costPerTier) → InfestResult`.
  - `src/main/java/dev/eder/wispborn/host/HostManager.java`: `tryInfest` (validação inicial, disparo de `WispAnimatable.playPossess` e agendamento de 16 ticks em mapa em memória por `UUID`), `tick` (revalidação de distância ≤ 4 blocos, vida, passageiro, tier e mana ao fim dos 16 ticks; cobrança de mana, `startRiding(mob, true)`, `setForm(FormState.host(id))`, `setInvisible(true)`, som `ALLAY_ITEM_GIVEN`, partículas `END_ROD` e HUD da forma `HOST`), `release(ServerPlayer, Reason)` (idempotente, restaura `WISP`, `setInvisible(false)` e posiciona em `mob.getY() + mob.getBbHeight() + 0.1`), `hostOf(Player)` e `ensureValidHostState`.
  - `src/main/java/dev/eder/wispborn/event/HostEvents.java`: listeners para `PlayerInteractEvent.EntityInteract`, `EntityMountEvent` (`SNEAK`), `LivingDeathEvent` (`HOST_DIED`), `PlayerLoggedOutEvent` (`LOGOUT`), `LivingIncomingDamageEvent` (invulnerabilidade exceto `BYPASSES_INVULNERABILITY`) e `PlayerTickEvent.Post`.
  - `src/main/java/dev/eder/wispborn/event/WispRestrictions.java`: removido o listener de `EntityInteract` (transferido para `HostEvents`) e bloqueado o uso de `wisp_essence` enquanto na forma `HOST` (permitido apenas na forma `WISP`).
  - `src/main/java/dev/eder/wispborn/event/FormEvents.java`: chamada a `HostManager.ensureValidHostState` em login e respawn (rede de segurança da regra 7).
  - `src/main/java/dev/eder/wispborn/form/FormManager.java` e `command/WispbornCommand.java`: limpeza de invisibilidade/montaria caso a forma seja alterada via comando estando em `HOST`.
  - `src/main/java/dev/eder/wispborn/WispbornMod.java`: registro de `HostEvents.class` no `NeoForge.EVENT_BUS`.
  - `src/main/resources/assets/wispborn/lang/en_us.json` e `pt_br.json`: chaves `wispborn.infest.*`, `wispborn.host.died`, `wispborn.host.left` e `wispborn.hud.host`.
  - `src/test/java/dev/eder/wispborn/host/InfestRulesTest.java`: suíte JUnit 5 cobrindo todos os casos e a precedência das validações de `InfestRules.check`.
- **Decisões tomadas:**
  - Quando o jogador sai com Shift (`Reason.SNEAK`), o `EntityMountEvent` dispara antes de `LivingEntity.dismountVehicle` reposicionar o passageiro no chão; por isso o `HostManager.release` agenda a coordenada do topo da hitbox (`mob.getY() + mob.getBbHeight() + 0.1`) para ser aplicada no `PlayerTickEvent.Post` do mesmo tick, garantindo que o wisp sempre apareça sobre o mob.
  - Enquanto `HOST`, chamas residuais no jogador (`remainingFireTicks > 0`) são limpas em `HostManager.tick` para evitar overlay de fogo em primeira pessoa quando o hospedeiro queima ao sol ou passa por lava.
- **Saída final de `./gradlew build`:**
  ```text
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  > Task :check
  > Task :build
  BUILD SUCCESSFUL in 12s
  ```
- **Saída final de `./gradlew test`:**
  ```text
  > Task :compileJava
  > Task :classes
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  BUILD SUCCESSFUL in 12s
  ```
- **Saída final de `./gradlew runServer`:**
  ```text
  [08:55:16] [Server thread/INFO] [minecraft/DedicatedServer]: Done (3.094s)! For help, type "help"
  [08:55:16] [Server thread/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```
- **Pendências:**
  - Validação do checklist manual in-game por Eder (`runClient` / `runClient2`).
