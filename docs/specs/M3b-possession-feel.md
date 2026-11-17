# M3b — Possessão: tecla, conjuração e feedback

**Objetivo:** possuir deixa de ser clique do mouse e vira uma **conjuração de 1 s segurando uma tecla**,
com indicador do alvo, barra de conjuração e animação nova; o mob possuído mostra um efeito visível para
todos; o wisp para de fazer som de passo e passa a ter um brilho mágico suave ao se mover.
**Estimativa:** 2–3 dias. **Branch:** `feat/M3b-possession-feel`. **Depende de:** M3 (mergeado na `main`)
e da entrega de arte do Codex (`docs/prompts/codex-art-prompts.md`: raios novos + clipe `animation.wisp.possess`).

Origem: teste de Eder em jogo depois do M3 (17/11/2026). Decisões de Eder: segurar para canalizar,
conjuração de 1 s, brilho mágico no lugar dos passos.

Leia antes: `AGENTS.md`, `.agents/rules/neoforge-1.21.1.md`, `.agents/rules/geckolib-4.9.md`, `docs/ART_PIPELINE.md`.

## 1. Arte nova (copiar, sem editar)
Copiar de `art/parasita-magico/` para `assets/wispborn/` (mesmos destinos do M1b):
`wisp.geo.json`, `wisp.animation.json`, `wisp.png`, `wisp_glowmask.png`.
- `WispAnimations.POSSESS` passa a `"animation.wisp.possess"` (o clipe `attack` deixa de existir).
- Atualizar `WispAnimationsTest` para o nome novo (continua exigindo `POSSESS` sem loop).
- Se o clipe `animation.wisp.possess` não existir em `art/`, **pare** e registre em "Dúvidas do implementador".

## 2. Som: brilho mágico no lugar dos passos
**Causa do bug atual:** `event/WispSounds` só trata `PlayLevelSoundEvent.AtPosition`. O passo do jogador
**local** sai por `LocalPlayer#playSound`, que dispara `PlayLevelSoundEvent.AtEntity` (conferido nos fontes
do NeoForge 21.1.266). Por isso os outros jogadores pararam de ouvir os passos, mas o próprio jogador não.

- `registry/ModSounds.java`: `DeferredRegister<SoundEvent>` (`Registries.SOUND_EVENT`) com
  `WISP_DRIFT = register("entity.wisp.drift", () -> SoundEvent.createVariableRangeEvent(rl("entity.wisp.drift")))`.
  Registrar no construtor do mod.
- `assets/wispborn/sounds.json`:
  ```json
  { "entity.wisp.drift": { "subtitle": "subtitles.wispborn.entity.wisp.drift",
      "sounds": [ { "name": "block.amethyst_block.chime", "type": "event", "volume": 0.35, "pitch": 1.4 } ] } }
  ```
  (Eder pode trocar por um `.ogg` próprio depois, só neste arquivo.)
- `WispSounds` trata **`AtEntity` e `AtPosition`**:
  - som `block.*.fall` de um wisp → cancelar;
  - som `block.*.step` de um wisp → trocar por `ModSounds.WISP_DRIFT` (`event.setSound(...)`), no máximo
    1 vez a cada `WispTuning.DRIFT_SOUND_INTERVAL_TICKS = 10` por jogador; nos outros passos, cancelar.
  - `AtEntity`: o wisp é `event.getEntity()`. `AtPosition`: wisp na mesma posição (regra atual).
- Regra pura em `WispSoundRules`: `classify(String soundPath) → STEP | FALL | OTHER` e
  `shouldPlayDrift(long lastTick, long now, int interval)`.
- Lang: `subtitles.wispborn.entity.wisp.drift` (en: "Wisp shimmers", pt: "Wisp cintila").

## 3. Tecla e conjuração (substitui o clique direito do M3)
- `client/KeyBindings.java`: `KeyMapping POSSESS` (`key.wispborn.possess`, padrão `GLFW.GLFW_KEY_R`,
  categoria `key.categories.wispborn`), registrada em `RegisterKeyMappingsEvent` (mod bus, `Dist.CLIENT`).
- Clique direito num mob **não** possui mais: `WispRestrictions` continua cancelando `EntityInteract` para
  parasitas e o gatilho do M3 sai do `HostEvents`.
- Rede (`network/`, registrados no `ModPayloads`):
  - `PossessStartPayload(int entityId)` client → server;
  - `PossessCancelPayload()` client → server;
  - `CastStatePayload(int targetEntityId, int durationTicks)` server → client; `targetEntityId = -1` = sem conjuração.
- Client (tick, jogador local em forma WISP):
  - tecla **pressionada** sem conjuração ativa e com alvo válido no indicador (seção 4) → envia `PossessStartPayload`;
  - tecla **solta** com conjuração ativa → envia `PossessCancelPayload`.
- Servidor (`HostManager`):
  - `PossessStartPayload`: valida com `InfestRules.check` + alcance + `player.hasLineOfSight(mob)`. Falhou →
    mensagem/som atuais do M3. OK → pendente com `executeAt = now + possess.castTicks`,
    `WispAnimatable.playPossess(player)` e `CastStatePayload(alvo, castTicks)`.
  - Cada tick com pendente, cancelar se: forma ≠ WISP, alvo morto/sumiu, distância > `possess.range`,
    sem linha de visão, ou o jogador tomou dano. Cancelar = remover pendente,
    `WispAnimatable.stopPossess(player)` (novo: `INSTANCE.stopTriggeredAnim(player, CONTROLLER_POSSESS, TRIGGER_POSSESS)`),
    `CastStatePayload(-1, 0)`, mensagem `wispborn.infest.cancelled`. Sem gasto de mana.
  - `PossessCancelPayload`: mesmo cancelamento, sem mensagem.
  - No fim: revalidar tudo e completar como no M3 (mana, `startRiding`, HOST, invisível) + `CastStatePayload(-1, 0)`.
- Regra pura `host/CastRules.cancelReason(boolean isWisp, boolean targetAlive, double distSqr, double rangeSqr,
  boolean hasLineOfSight, boolean tookDamage) → Optional<CancelReason>`.
- Config (seção nova `possess`): `possess.castTicks` (int, 20), `possess.range` (double, 4.0).
  Remover `HostManager.POSSESS_DELAY_TICKS` e `MAX_INFEST_DISTANCE_SQR` (passam a vir do config).

## 4. Indicador de alvo (HUD)
O servidor decide; o client só mostra.
- Servidor, a cada 4 ticks, para cada jogador WISP: alvo = mob na mira até `possess.range`
  (`ProjectileUtil.getEntityHitResult` com o vetor do olhar). Calcula o `InfestResult` e envia
  `TargetInfoPayload(int entityId, InfestResult result, int tier, int manaCost)` **só quando mudar**
  (`entityId = -1` = sem alvo).
- Client:
  - `client/ClientPossession`: guarda o último `TargetInfoPayload` e o `CastStatePayload` (com o tick de início).
  - Camada de HUD `wispborn:possess_target` (`RegisterGuiLayersEvent#registerAboveAll`), logo abaixo da mira:
    - nome do mob (traduzido) + `Tier N · X mana`;
    - status: verde `wispborn.target.ready` ("Segure [R] para possuir", com o nome real da tecla) ou
      vermelho com o motivo (reaproveitar as chaves `wispborn.infest.*` do M3);
    - durante a conjuração: barra que enche de 0 a 100% em `durationTicks`.
  - Marca no mundo: a cada 4 ticks, 6 partículas `DustParticleOptions` em anel nos pés do alvo
    (ciano `WispTuning.TARGET_OK_COLOR` se OK, vermelho `TARGET_FAIL_COLOR` se não), só no client local.
- Lang: `key.wispborn.possess`, `key.categories.wispborn`, `wispborn.target.ready`, `wispborn.target.info`.

## 5. Efeito visível no mob possuído
- Servidor, a cada 10 ticks, para cada jogador HOST: `serverLevel.sendParticles(
  ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, WispTuning.POSSESSED_PARTICLE_COLOR), ...)` com
  3 partículas espalhadas pela hitbox do hospedeiro (o redemoinho de poção, na cor do wisp).
  `POSSESSED_PARTICLE_COLOR = 0xFF40E0D0`. Visível para todos.

## Testes (JUnit)
- `WispSoundRulesTest`: `classify` (step, fall, outros) e `shouldPlayDrift` (intervalo).
- `CastRulesTest`: cada motivo de cancelamento e o caso sem cancelamento.
- Round-trip do `StreamCodec` de `PossessStartPayload`, `CastStatePayload` e `TargetInfoPayload` (`Unpooled.buffer()`).
- `WispAnimationsTest` com `animation.wisp.possess`.

## Checklist manual (Eder)
- [ ] Wisp andando: sem passos; um brilho suave de vez em quando. Player2 ouve o mesmo. Como humano, passos normais.
- [ ] Mirando num mob a até 4 blocos: aparece nome, tier e custo; anel ciano nos pés se dá para possuir,
      vermelho com o motivo se não (tier alto, sem mana, imune).
- [ ] Clique direito num mob como wisp: nada acontece.
- [ ] Segurar R por 1 s: barra enche, animação de possessão toca, wisp entra no mob; mana é gasta.
- [ ] Soltar R antes, se afastar, perder a mira atrás de um bloco ou tomar dano: cancela sem gastar mana e a animação para.
- [ ] Mob possuído solta o redemoinho ciano; Player2 também vê.
- [ ] Raios novos do wisp não aparecem como "X" de papel de perto (idle e walking).
- [ ] Tecla aparece em Controles → Wispborn e pode ser trocada.
- [ ] Servidor dedicado sobe sem crash.

## Fora de escopo
Drenagem (M4), consumo (M5), tela de atributos (M6), controlar o hospedeiro, som próprio em `.ogg`.

## Dúvidas do implementador
_(preencher se houver)_

## Relatório de implementação
_(preencher ao terminar)_
