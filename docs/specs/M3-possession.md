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
_(preencher se houver)_

## Relatório de implementação
_(preencher ao terminar)_
