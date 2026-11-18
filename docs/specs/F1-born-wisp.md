# F1 — Nascer wisp, renascer wisp e fome do wisp

**Objetivo:** o jogador é wisp desde o primeiro tick e volta wisp ao morrer. A fome passa a ser a pressão
do jogo: drena enquanto o wisp explora (mais rápido em nível alto), não drena dentro do hospedeiro e,
zerada, mata o wisp em qualquer dificuldade.
**Estimativa:** 1 dia. **Branch:** `feat/F1-born-wisp`. **Depende de:** M3b (main atual).
**Design:** `docs/superpowers/specs/2026-11-17-fundacao-design.md`.

## Regras

### Forma
1. O default do attachment `FORM` passa a ser `FormState.WISP`. Jogador novo entra no mundo já wisp, com
   os modificadores de `FormManager.applyModifiers` aplicados no login (já acontece em `FormEvents`).
2. Remover a forma humana: `FormType.HUMAN`, `FormState.HUMAN`, `WispEssenceItem`, `ModItems` (registro,
   aba criativa, modelo, textura, lang `item.wispborn.wisp_essence`) e as exceções da essência em
   `WispRestrictions`.
3. **Saves antigos:** o codec de `FormType` aceita `"human"` como alias legado e decodifica como `WISP`.
   Mundo de teste antigo carrega sem erro e o jogador aparece wisp.
4. Comando de debug: remover `/wispborn form human`. Manter `/wispborn form wisp` (útil para sair de HOST
   travado). Lang `wispborn.form.human` sai.
5. `WispRestrictions`: o wisp não usa nenhum item com botão direito (sem a exceção da essência). Isso
   inclui comida: o wisp não come.

### Morte e renascimento
6. Ao renascer (`PlayerRespawnEvent`): forma WISP, `applyModifiers`, sync (já acontece) e **mana cheia**
   (`ProgressManager.setMana(player, getMaxMana(player))`). EP, nível e atributos já são `copyOnDeath`.
   A fome volta cheia pelo vanilla.
7. Morrer dentro do hospedeiro segue o fluxo do M3 (`ensureValidHostState`).

### Fome
8. A cada 20 ticks, no servidor, se a forma é **WISP** (não HOST), fora do Pacífico:
   `player.causeFoodExhaustion(HungerMath.exhaustionPerSecond(nível, base, porNível))`.
9. Em HOST não há exaustão extra. A exaustão vanilla de andar ou pular continua valendo como wisp.
10. **Inanição do wisp:** com `foodLevel == 0`, forma WISP, fora do Pacífico, a cada
    `hunger.starveIntervalTicks` (80) o wisp toma `hunger.starveDamage` (1,0) de `damageSources().starve()`,
    **sem limite de vida**, para que possa morrer de fome. Para não somar com o vanilla, só aplique quando
    o vanilla **não** aplicaria. A condição vanilla (`FoodData.tick`) é
    `vida > 10 || Difícil || (Normal && vida > 1)`; `HungerMath.vanillaStarves` replica exatamente isso.
11. Regeneração de vida por fome alta continua vanilla.
12. O HUD em texto do M2 (`buildWispHud`) continua por enquanto; a F4 o substitui.

## Config (`WispbornConfig`)
| Chave | Padrão | Faixa |
|---|---|---|
| `hunger.exhaustionBasePerSecond` | 0.05 | 0–10 |
| `hunger.exhaustionPerLevelPerSecond` | 0.01 | 0–10 |
| `hunger.starveIntervalTicks` | 80 | 1–72000 |
| `hunger.starveDamage` | 1.0 | 0–1000 |

## Contratos
- `form/HungerMath.java` (puro, sem classes do Minecraft além de `Difficulty`):
  - `static float exhaustionPerSecond(int level, double base, double perLevel)` → `(float)(base + perLevel × level)`
  - `static boolean vanillaStarves(Difficulty difficulty, float health)`
  - `static boolean wispStarves(boolean isWisp, Difficulty difficulty, int foodLevel, float health)` →
    `isWisp && difficulty != PEACEFUL && foodLevel == 0 && !vanillaStarves(difficulty, health)`
- `event/HungerEvents.java` (ou dentro de `FormEvents`, a critério): `PlayerTickEvent.Post` no servidor.
  Contador da inanição: transitório por jogador (mapa em memória limpo no logout, ou `tickCount % intervalo`).
- `FormType.CODEC`: aceita `"human"` → `WISP` (decodificação); encode nunca produz `"human"`.

## Testes (JUnit)
- `exhaustionPerSecond(1, 0.05, 0.01)` = 0.06; `(20, 0.05, 0.01)` = 0.25.
- `vanillaStarves`: HARD/vida 1 → true; NORMAL/vida 1 → false; NORMAL/vida 2 → true; EASY/vida 6 → false; EASY/vida 11 → true.
- `wispStarves`: EASY, fome 0, vida 6 → true; HARD, fome 0 → false (o vanilla cuida); fome 1 → false; não-wisp → false; PEACEFUL → false.
- `FormStateTest`: decodificar `"human"` resulta em `WISP`; round-trip de WISP e HOST continua.
- Remover os testes de HUMAN.

## Checklist manual
- [ ] Mundo novo: o jogador aparece wisp no primeiro frame, sem item nenhum no inventário.
- [ ] Mundo antigo (salvo como humano) carrega e o jogador aparece wisp, sem erro no log.
- [ ] `/wispborn form human` não existe mais.
- [ ] Botão direito com comida na mão não come.
- [ ] Config com `exhaustionBasePerSecond = 1.0`: a fome cai visivelmente rápido como wisp e **para** de cair dentro de um hospedeiro.
- [ ] `/wispborn ep set Dev 136800` (Nv 20): a fome cai mais rápido que no Nv 1 (comparar com a mesma config).
- [ ] Fácil, fome 0: o wisp toma dano a cada 4 s até morrer. Normal e Difícil: também morre (sem dano dobrado no Difícil).
- [ ] Ao morrer: renasce wisp, mesmo nível e EP, mana cheia, fome cheia.
- [ ] Pacífico: nada de dano por fome.
- [ ] Servidor dedicado (`./gradlew runServer`) sobe sem crash.

## Fora de escopo
Recuperar fome (vem na F3), HUD novo (F4), nível dos mobs (F2).

## Dúvidas do implementador
Nenhuma dúvida.

## Relatório de implementação
- **Arquivos criados/alterados/removidos:**
  - `src/main/java/dev/eder/wispborn/form/FormType.java` e `FormState.java`: removido `FormType.HUMAN` e `FormState.HUMAN`; `FormType.CODEC` implementado com `Codec.STRING.comapFlatMap` para decodificar `"human"` como `WISP` em saves legados e nunca serializar `"human"`.
  - `src/main/java/dev/eder/wispborn/registry/ModAttachments.java`: default do attachment `FORM` alterado para `FormState.WISP`.
  - Removidos `src/main/java/dev/eder/wispborn/form/WispEssenceItem.java`, `src/main/java/dev/eder/wispborn/registry/ModItems.java`, `src/main/resources/assets/wispborn/models/item/wisp_essence.json` e `src/main/resources/assets/wispborn/textures/item/wisp_essence.png`; limpadas referências em `WispbornMod.java`.
  - `src/main/java/dev/eder/wispborn/event/WispRestrictions.java`: removida a exceção de `wisp_essence` em `RightClickItem` e `RightClickBlock` (bloqueia todo uso de item com botão direito, inclusive comida).
  - `src/main/java/dev/eder/wispborn/command/WispbornCommand.java` e `src/main/resources/assets/wispborn/lang/{en_us,pt_br}.json`: removido o subcomando `/wispborn form human` e as chaves `item.wispborn.wisp_essence` e `wispborn.form.human`.
  - `src/main/java/dev/eder/wispborn/config/WispbornConfig.java`: adicionadas `hunger.exhaustionBasePerSecond` (0.05), `hunger.exhaustionPerLevelPerSecond` (0.01), `hunger.starveIntervalTicks` (80) e `hunger.starveDamage` (1.0).
  - `src/main/java/dev/eder/wispborn/form/HungerMath.java`: criado helper puro com `exhaustionPerSecond`, `vanillaStarves` e `wispStarves`.
  - `src/main/java/dev/eder/wispborn/event/FormEvents.java`: restauração de mana cheia em `onPlayerRespawn`; em `onPlayerTickPost`, aplicação de `causeFoodExhaustion` a cada 20 ticks enquanto `WISP` fora do Pacífico e dano `damageSources().starve()` a cada `hunger.starveIntervalTicks` quando `HungerMath.wispStarves` é verdadeiro.
  - `src/test/java/dev/eder/wispborn/form/FormStateTest.java` e `HungerMathTest.java`: atualizados testes de `FormState` (alias legado `"human"` → `WISP`, remoção de `HUMAN`) e adicionada suíte unitária de `HungerMath`.
- **Decisões tomadas:**
  - A lógica de fome e inanição foi integrada diretamente em `FormEvents.onPlayerTickPost` usando `serverPlayer.tickCount % starveInterval == 0` (conforme permitido pelo contrato da spec), mantendo o pacote `event/` enxuto e sem estado transitório extra em memória.
- **Saída final de `./gradlew build`:**
  ```text
  > Task :jar
  > Task :assemble
  > Task :build
  BUILD SUCCESSFUL in 10s
  ```
- **Saída final de `./gradlew test`:**
  ```text
  > Task :compileJava
  > Task :classes
  > Task :compileTestJava
  > Task :testClasses
  > Task :test
  BUILD SUCCESSFUL in 17s
  ```
- **Saída final de `./gradlew runServer`:**
  ```text
  [23:13:00] [Server thread/INFO] [minecraft/DedicatedServer]: Done (8.759s)! For help, type "help"
  [23:13:00] [Server thread/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: [wispborn]
  ```
- **Pendências:**
  - Validação do checklist manual in-game por Eder (`runClient`).
