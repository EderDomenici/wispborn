# F3 — Drenagem e consumo com nível e fome

**Objetivo:** fechar o loop da fundação. Dentro do hospedeiro, o wisp drena a vida dele aos poucos, ganha
EP e recupera fome. Com o hospedeiro em ≤ 20% da vida, uma tecla o consome (bônus de EP e de fome).
O EP depende do **nível do mob** e cai quando a presa é fraca demais para o wisp.
**Estimativa:** 2–3 dias. **Branch:** `feat/F3-drain-consume`. **Depende de:** F2.
**Substitui:** `M4-drain.md` e `M5-consume.md`. As regras deles valem aqui, com as mudanças abaixo;
onde houver conflito, esta spec vence.

## O que muda em relação a M4/M5
| Tema | M4/M5 | F3 |
|---|---|---|
| Base do EP | `tier` | `mobLevel` (traços da F2) × `EpMath.levelFactor` |
| EP por pulso | `ep.perPulsePerTier × tier` | `ep.perPulsePerLevel × mobLevel × fator` |
| Bônus de consumo | `ep.consumeBonusPerTier × tier` | `ep.consumePerLevel × mobLevel × fator` |
| Fome | — | volta a cada pulso e no consumo |
| HUD | texto na action bar | texto provisório (igual ao M4) até a F4 |

## Regras

### Drenagem (herda M4)
1. A cada `drain.intervalTicks` (100), no servidor, para cada jogador HOST com hospedeiro vivo: aplicar
   exatamente a regra do M4 (`DrainMath.nextHealth`, `setHealth` e não `hurt`, piso
   `drain.floorPercent`, partículas `SOUL`, sem drenar no tick de entrada, contador transitório).
2. Acima do piso, cada pulso:
   - `fator = EpMath.levelFactor(wispLevel, mobLevel, ep.levelGrace, ep.falloffPerLevel)`
   - `baseEp = DrainMath.epPerPulse(mobLevel, ep.perPulsePerLevel, fator)` = `round(perPulsePerLevel × mobLevel × fator)`
   - se `baseEp > 0`: `ProgressManager.addEp(player, baseEp)` (Eficiência aplicada lá dentro)
   - **fome:** `player.getFoodData().eat(hunger.restorePerPulse, hunger.restorePerPulseSaturation)`
     (padrões 1 e 0,25 → +1 ponto, +0,5 de saturação)
3. No piso: sem drenagem, sem EP e **sem fome**. Isso empurra o jogador a consumir.
4. Presa sem EP (`fator == 0`) continua drenando e devolvendo fome: o wisp pode usar presa fraca para
   comer, só não evolui com ela.
5. Mob sem traços (não deveria acontecer, porque a F2 não deixa infestar) → `release` com motivo `SNEAK`.

### Consumo (herda M5)
6. Tecla **Consumir** (padrão `C`), `ConsumeRequestPayload`, revalidação no servidor, dano
   `wispborn:consumed`, sem drops e sem XP, `release(CONSUMED)` depois do bônus: tudo como no M5.
7. Bônus: `baseEp = ConsumeRules.consumeEp(mobLevel, ep.consumePerLevel, fator)`; `addEp` se `> 0`.
8. **Fome:** `eat(hunger.consumeBase + mobLevel / 2, hunger.consumeSaturation)` (padrões 2 e 0,3; divisão inteira).
9. Mensagem `wispborn.consume.ok` = `Você consumiu %s (+%s EP)`; com fator 0:
   `wispborn.consume.ok_no_ep` = `Você consumiu %s (fraco demais para evoluir)`.

### HUD provisório
10. Mesmo formato do M4, trocando tier por nível:
    `Vaca Nv 2 · 45% · EP 1200/1600 · Nv 3 · Mana 20/50`, mais `· [C] consumir` em verde no piso e
    `· sem EP` em cinza quando o fator é 0. A F4 substitui por sprites.

## Config (`WispbornConfig`)
| Chave | Padrão | Faixa | Observação |
|---|---|---|---|
| `ep.perPulsePerLevel` | 3 | 0–100000 | substitui `ep.perPulsePerTier` (remover) |
| `ep.consumePerLevel` | 30 | 0–1000000 | substitui `ep.consumeBonusPerTier` (remover) |
| `hunger.restorePerPulse` | 1 | 0–20 | |
| `hunger.restorePerPulseSaturation` | 0.25 | 0–10 | modificador do `FoodData.eat` |
| `hunger.consumeBase` | 2 | 0–20 | |
| `hunger.consumeSaturation` | 0.3 | 0–10 | |

`drain.intervalTicks`, `drain.basePercentPerPulse` e `drain.floorPercent` continuam como estão.

## Dados
Iguais ao M5: `data/wispborn/damage_type/consumed.json`, as tags `bypasses_armor` e `bypasses_effects`,
`ModDamageTypes.CONSUMED` e as linhas de lang (`death.attack.wispborn.consumed`, `key.wispborn.consume`,
`wispborn.consume.ok`, `wispborn.consume.ok_no_ep`, `wispborn.consume.not_ready`).

## Contratos
- `host/DrainMath.java`: `nextHealth`, `atFloor` (como no M4) e
  `static int epPerPulse(int mobLevel, int perPulsePerLevel, double factor)`.
- `host/ConsumeRules.java`: `check(...)` → `ConsumeResult` (como no M5) e
  `static int consumeEp(int mobLevel, int consumePerLevel, double factor)`.
- `form/HungerMath.java` (da F1) ganha `static int consumeFood(int mobLevel, int base)` = `base + mobLevel / 2`.
- `HostManager.tickDrain(ServerPlayer)`, `HostManager.tryConsume(ServerPlayer)`, `Reason.CONSUMED` (já existe).
- `network/ConsumeRequestPayload`, `KeyBindings.CONSUME`.

## Testes (JUnit)
- Os testes de `DrainMath.nextHealth`/`atFloor` do M4.
- `epPerPulse(2, 3, 1.0)` = 6; `(9, 3, 1.0)` = 27; `(9, 3, 0.5)` = 14 (arredondado); `(9, 3, 0.0)` = 0.
- `consumeEp(2, 30, 1.0)` = 60; `(9, 30, 0.25)` = 68 (arredondado).
- `consumeFood(1, 2)` = 2; `(9, 2)` = 6; `(20, 2)` = 12.
- `ConsumeRules.check`: os casos do M5.
- Ritmo (teste de documentação): galinha Nv 1 com wisp Nv 1, sem atributos: 40 pulsos × 3 + 30 = **150 EP**,
  então duas galinhas levam ao Nv 2 (300). Vaca Nv 2 com wisp Nv 2: 40 × 6 + 60 = 300 EP.

## Checklist manual
- [ ] `drain.intervalTicks = 20` para testar rápido. Fome do wisp em 10 (`/effect` de fome ou esperar).
- [ ] Wisp Nv 1 dentro de galinha Nv 1: a % cai 2 pontos por pulso, +3 EP por pulso e a fome sobe 1 por pulso.
- [ ] No piso de 20%: para de drenar, de dar EP e de dar fome; aparece `[C] consumir`.
- [ ] `C` com 80% → "Ainda resiste: 80%". `C` no piso → galinha morre sem drops, `+30 EP`, fome sobe 2 pontos, wisp sai visível.
- [ ] `/wispborn ep set Dev 13600` (Nv 10) e infestar galinha Nv 1: drena, devolve fome, **sem EP** (`· sem EP` no HUD); consumo mostra "fraco demais para evoluir".
- [ ] Wisp Nv 10 em zumbi Nv 7 (3 abaixo): EP por pulso = 21 × 0,75 = 16.
- [ ] Hospedeiro morto por outra causa antes de 20%: expulso, sem bônus (regra do M3).
- [ ] Ponta a ponta com `intervalTicks = 100`: começando no Nv 1 com fome cheia, chegar ao Nv 3 só
      parasitando, sem morrer de fome.
- [ ] Multiplayer: o Player2 vê o mob morrer e a mensagem `... foi consumido por Dev`.
- [ ] Servidor dedicado sobe sem crash.

## Fora de escopo
HUD em sprites (F4), animação especial de consumo, traços ganhos ao consumir (menu RPG).

## Dúvidas do implementador
Nenhuma dúvida.

## Relatório de implementação
- **Arquivos criados/alterados:**
  - `src/main/java/dev/eder/wispborn/config/WispbornConfig.java`: substituídos `ep.perPulsePerTier` e `ep.consumeBonusPerTier` por `ep.perPulsePerLevel` (`3`) e `ep.consumePerLevel` (`30`); adicionados `hunger.restorePerPulse` (`1`), `hunger.restorePerPulseSaturation` (`0.25`), `hunger.consumeBase` (`2`) e `hunger.consumeSaturation` (`0.3`).
  - `src/main/java/dev/eder/wispborn/form/HungerMath.java`: adicionado `consumeFood(int mobLevel, int base)` = `base + (mobLevel / 2)`.
  - `src/main/java/dev/eder/wispborn/host/DrainMath.java`: criado helper puro com `nextHealth`, `atFloor` (com tolerância `1e-4F` para precisão de `float`, garantindo 40 pulsos exatos de 100% a 20% a 2%/pulso) e `epPerPulse`.
  - `src/main/java/dev/eder/wispborn/host/{ConsumeResult,ConsumeRules}.java`: criados `ConsumeResult` (`OK`, `NOT_HOST`, `NOT_READY`) e `ConsumeRules` (`check` e `consumeEp`).
  - `src/main/java/dev/eder/wispborn/registry/ModDamageTypes.java`, `src/main/resources/data/wispborn/damage_type/consumed.json` e `src/main/resources/data/minecraft/tags/damage_type/{bypasses_armor,bypasses_effects}.json`: registrado damage type `wispborn:consumed`.
  - `src/main/java/dev/eder/wispborn/network/{ConsumeRequestPayload,ModPayloads}.java` e `client/KeyBindings.java`: payload C2S `ConsumeRequestPayload` e tecla `CONSUME` (`key.wispborn.consume`, padrão `C`).
  - `src/main/java/dev/eder/wispborn/host/HostManager.java` e `event/HostEvents.java`: `tickDrain` (contador transitório por jogador inicializado em `-1` ao montar para não drenar no tick de entrada, `mob.setHealth`, EP escalado por `EpMath.levelFactor`, restauração de fome, partículas `SOUL`, e `release(SNEAK)` se o mob não tiver traços), `tryConsume`, `onHostConsumed` (recompensa de EP + fome + sons `GENERIC_EAT` e `SOUL_ESCAPE` + partículas `SOUL` + mensagens `wispborn.consume.ok` / `wispborn.consume.ok_no_ep` antes de `release(CONSUMED)`), bloqueio de drops (`LivingDropsEvent`) e XP (`LivingExperienceDropEvent`), e HUD provisório em `buildHostHud`.
  - `src/main/resources/assets/wispborn/lang/{en_us,pt_br}.json`: adicionadas traduções de consumo, damage type e sufixos do HUD do hospedeiro.
  - `src/test/java/dev/eder/wispborn/`: criados `DrainMathTest` e `ConsumeRulesTest`; atualizados `HungerMathTest` e `PossessionPayloadsTest`.
- **Decisões tomadas:**
  - `DrainMath.atFloor` e `DrainMath.nextHealth` usam tolerância `FLOOR_EPSILON = 1.0e-4F` porque em ponto flutuante IEEE-754 de 32 bits (`float`), 40 subtrações de `0.2` a partir de `10.0F` acumulam `+7.6e-6F` (`2.0000076F`), o que exigiria um 41º pulso sem o snap ao piso; com `1.0e-4F`, `atFloor(2.01F, 10.0F, 0.2D)` continua `false` e a drenagem de 100% a 20% leva exatamente 40 pulsos para qualquer vida máxima.
  - Em `HostManager.onHostConsumed`, a mensagem `wispborn.consume.ok` / `wispborn.consume.ok_no_ep` é enviada tanto para a action bar quanto para o chat (`sendSystemMessage`), evitando que o tick de 20 em 20 ticks de `FormEvents` sobrescreva o feedback na action bar logo após o wisp sair do hospedeiro.
- **Saída final de `./gradlew build`:**
  ```text
  > Task :jar
  > Task :assemble
  > Task :build
  BUILD SUCCESSFUL in 9s
  ```
- **Saída final de `./gradlew test`:**
  ```text
  > Task :compileJava
  > Task :classes
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  BUILD SUCCESSFUL in 14s
  ```
- **Saída final de `./gradlew runServer`:**
  ```text
  [22:52:39] [Server thread/INFO] [minecraft/DedicatedServer]: Done (1.692s)! For help, type "help"
  [22:52:39] [Server thread/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```
- **Pendências:**
  - Validação do checklist manual in-game por Eder (`runClient` / `runClient2`).
