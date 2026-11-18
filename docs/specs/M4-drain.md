# M4 — Drenagem e EP passivo

> **Substituída pela F3** (`docs/specs/F3-drain-consume.md`, rodada de fundação de 17/11/2026). As regras
> abaixo valem só onde a F3 manda herdar. Não implemente esta spec.

**Objetivo:** enquanto está dentro do hospedeiro, o wisp drena a vida dele aos poucos e ganha EP.
A drenagem para em 20% da vida máxima; daí em diante não há mais EP passivo (incentivo para consumir).
**Estimativa:** 1 dia. **Branch:** `feat/M4-drain`. **Depende de:** M3 (e Absorção/Eficiência do M2).

## Regras
- A cada `drain.intervalTicks` (padrão 100 = 5s), no servidor, para cada jogador HOST com hospedeiro vivo:
  - `floor = maxHealth × drain.floorPercent`
  - se `health > floor`:
    - `pct = StatMath.drainPercent(drain.basePercentPerPulse, absorção, stats.absorptionPerPoint)`
    - `novaVida = max(floor, health − maxHealth × pct)`
    - `mob.setHealth(novaVida)` — **`setHealth`, não `hurt`**: sem knockback, sem aggro, sem som de dano, sem matar.
    - `ProgressManager.addEp(player, ep.perPulsePerTier × tier)` (a Eficiência é aplicada dentro do `addEp`)
    - partículas `ParticleTypes.SOUL` saindo do mob (servidor: `serverLevel.sendParticles`).
  - se `health ≤ floor`: nada (sem drenagem e sem EP).
- Se o mob tomar dano de outra fonte, a vida cai normalmente (pode chegar a ≤20% mais rápido, ou morrer → expulsão do M3, sem bônus).
- Contagem: use um contador próprio por jogador (campo transitório: attachment **sem** serializer,
  ou `player.tickCount % intervalo`). Entrar num hospedeiro não deve drenar no mesmo tick.

## HUD (substitui o HUD do host do M3)
`Vaca · 45% · EP 1200/1600 · Nv 3 · Mana 20/50`; quando `health ≤ floor`, acrescenta `· [C] consumir` em destaque
(cor verde, `ChatFormatting.GREEN`). O texto `[C]` é fixo: o servidor não sabe qual tecla o jogador configurou.

## Contratos
- `host/DrainMath.java` (puro):
  - `static float nextHealth(float health, float max, double percent, double floorPercent)`
  - `static boolean atFloor(float health, float max, double floorPercent)`
  - `static int epPerPulse(int tier, int perPulsePerTier)`
- `event/HostEvents.java`: `PlayerTickEvent.Post` (servidor) chama `HostManager.tickDrain(ServerPlayer)`.

## Testes (JUnit)
- `nextHealth(10, 10, 0.02, 0.2)` = 9.8; `nextHealth(10, 10, 0.04, 0.2)` = 9.6; `nextHealth(2.1, 10, 0.02, 0.2)` = 2.0 (não passa do piso); no piso não muda.
- `atFloor(2.0, 10, 0.2)` = true; `atFloor(2.01, 10, 0.2)` = false.
- Quantidade de pulsos de 100% até 20% com 2%/pulso = 40.

## Checklist manual
- [ ] Na config, para testar rápido: `intervalTicks = 20`. Dentro da vaca, a % cai 2 pontos por segundo e o EP sobe 10 por pulso.
- [ ] Com 2 pontos em Absorção, a % cai 3 pontos por pulso. Com 1 ponto em Eficiência, o EP sobe 11 por pulso.
- [ ] Ao chegar a 20%: para de cair, para de ganhar EP, aparece `[C] consumir` em verde.
- [ ] Vaca não corre como se tivesse levado dano (sem pânico) durante a drenagem.
- [ ] Zumbi (tier 2) dá 20 EP por pulso.
- [ ] A mana não sobe enquanto está dentro do hospedeiro.
- [ ] Nível sobe durante a drenagem com a mensagem do M2.
- [ ] Sair com shift e entrar de novo no mesmo mob: continua da % em que estava.
- [ ] Voltar `intervalTicks = 100` e medir: ~3min20s de 100% a 20%.

## Fora de escopo
Consumir, tecla, matar o mob pela drenagem.

## Dúvidas do implementador
_(preencher se houver)_

## Relatório de implementação
_(preencher ao terminar)_
