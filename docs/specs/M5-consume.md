# M5 — Consumo

> **Substituída pela F3** (`docs/specs/F3-drain-consume.md`, rodada de fundação de 17/11/2026). As regras
> abaixo valem só onde a F3 manda herdar. Não implemente esta spec.

**Objetivo:** com o hospedeiro em ≤20% da vida, o jogador aperta uma tecla e o consome: o mob morre
com um dano próprio, não dropa nada, o wisp ganha o bônus de EP e sai. Fecha o ciclo do MVP.
**Estimativa:** 1–2 dias. **Branch:** `feat/M5-consume`. **Depende de:** M4.

## Regras
1. Tecla **Consumir**, padrão `C`, configurável em Controles (categoria "Wispborn").
   Obs.: no vanilla, `C` é o "ativador de salvar hotbar" (só age junto com teclas numéricas); o conflito
   aparece em vermelho nos controles mas não atrapalha. Se incomodar, trocar o padrão para `V`.
2. Client: ao apertar, se o jogador local é HOST → `PacketDistributor.sendToServer(ConsumeRequestPayload.INSTANCE)`.
   Nenhuma decisão no client.
3. Servidor revalida tudo: forma HOST, hospedeiro vivo, `DrainMath.atFloor(...)`, tier presente.
   Falhou → action bar `wispborn.consume.not_ready` (`Ainda resiste: 45%`) e nada acontece.
4. Sucesso:
   - `mob.hurt(new DamageSource(<holder de wispborn:consumed>, player), Float.MAX_VALUE)`;
     holder via `level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModDamageTypes.CONSUMED)`
     (nomes de 1.21.1).
   - no `LivingDeathEvent` do mob, se `source.is(ModDamageTypes.CONSUMED)`:
     `ganho = ProgressManager.addEp(player, ep.consumeBonusPerTier × tier)` (Eficiência aplicada lá dentro),
     mensagem `wispborn.consume.ok` com o ganho real (`Você consumiu Vaca (+110 EP)`), sons `GENERIC_EAT` + `SOUL_ESCAPE`, partículas `SOUL` em volta.
   - Sem drops e sem XP: cancelar `LivingDropsEvent` e zerar `LivingExperienceDropEvent` quando a fonte é `consumed`.
   - A expulsão usa o mesmo ponto do M3 (`LivingDeathEvent` → `release`), com motivo `CONSUMED`
     (sem a mensagem de "hospedeiro morreu"). O bônus é dado **antes** do `release`.
5. HUD do M4 continua com `[C]` fixo (o servidor não conhece a tecla configurada). Melhoria futura: HUD no client.

## Dados
- `data/wispborn/damage_type/consumed.json`:
  `{ "message_id": "wispborn.consumed", "exhaustion": 0.0, "scaling": "never" }`
- `data/minecraft/tags/damage_type/bypasses_armor.json` e `bypasses_effects.json`:
  `{ "values": ["wispborn:consumed"] }` (sem `replace`).
- `registry/ModDamageTypes.java`: `ResourceKey<DamageType> CONSUMED = ResourceKey.create(Registries.DAMAGE_TYPE, rl("consumed"))`.
- Lang: `death.attack.wispborn.consumed` = `%1$s was consumed by %2$s` / `%1$s foi consumido por %2$s`,
  `key.wispborn.consume`, `key.categories.wispborn`, `wispborn.consume.ok`, `wispborn.consume.not_ready`.

## Contratos
- `network/ConsumeRequestPayload`: record vazio, `STREAM_CODEC = StreamCodec.unit(INSTANCE)`, `playToServer`.
- `client/KeyBindings.java`: `KeyMapping CONSUME` registrado em `RegisterKeyMappingsEvent`;
  `ClientTickEvent.Post` faz `while (CONSUME.consumeClick())`.
- `host/HostManager.tryConsume(ServerPlayer) → ConsumeResult` (`OK, NOT_HOST, NOT_READY`).
- `HostManager.release` ganha o motivo `CONSUMED`.

## Testes (JUnit)
- `ConsumeRules.check(boolean isHost, boolean hostAlive, float health, float max, double floor)` → `ConsumeResult`.
- Bônus base: tier 1 → 100, tier 2 → 200; com 1 ponto em Eficiência, tier 1 → 110.

## Checklist manual — teste de ponta a ponta do MVP
- [ ] `intervalTicks = 20` na config. Wisp Nv 1, EP 0.
- [ ] Entra numa vaca; apertar `C` com 80% → "Ainda resiste: 80%".
- [ ] Espera chegar a 20% → `C` → vaca morre, sem couro/carne/XP, mensagem `+100 EP`, wisp sai visível.
- [ ] Uma vaca inteira (drenagem + consumo ≈ 500 EP) → Nv 2 com +2 pontos. Gastar 1 em Eficiência e 1 em Absorção.
- [ ] Segunda vaca: mais rápida e com mais EP que a primeira → Nv 3.
- [ ] Esperar a mana voltar; Nv 3: infestar zumbi (30 de mana), drenar e consumir (+220 EP com 1 ponto em Eficiência).
- [ ] Bater no hospedeiro (com outro jogador) até ele morrer antes de 20%: expulso sem bônus.
- [ ] Multiplayer: Player2 vê a vaca morrer e a mensagem de morte `... foi consumido por Dev` no log/chat.
- [ ] Servidor dedicado sobe sem crash (keybind só no client).

## Fora de escopo
Traços, tela de evolução, controle do mob, animação especial de consumo.

## Dúvidas do implementador
_(preencher se houver)_

## Relatório de implementação
_(preencher ao terminar)_
