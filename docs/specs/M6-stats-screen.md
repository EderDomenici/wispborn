# M6 — Tela de atributos

**Objetivo:** o jogador vê nível, EP, mana e pontos numa tela própria (tecla `K`) e distribui os pontos
pelos botões +, sem comando. Primeiro módulo que sincroniza progresso para o client.
**Estimativa:** 1–2 dias. **Branch:** `feat/M6-stats-screen`. **Depende de:** M5.

## Regras
- O servidor continua sendo a autoridade: a tela só mostra o que recebeu e pede alocação; quem valida é `ProgressManager.allocate`.
- O client **não** lê a config do servidor. Todo número exibido (efeito atual e efeito com +1) vem calculado do servidor.
- A tela não pausa o jogo (`isPauseScreen() = false`) e fecha com `K` ou `Esc`.

## Contratos

### Rede
- `network/SyncProgressPayload` (server → client):
  `(int totalEp, int level, int nextThreshold /* -1 = máximo */, int availablePoints, float mana, int maxMana, List<StatView> stats)`
- `progress/StatView`: `(Stat stat, int points, int maxPoints, double current, double next)` com `StreamCodec`.
  `current`/`next` por atributo: Absorção = % por pulso; Eficiência = multiplicador de EP; Reserva = mana máxima; Vitalidade = corações extras.
- `network/AllocateStatPayload` (client → server): `(Stat stat)`. Handler: `ProgressManager.allocate`, mensagem do resultado na action bar.

### Quando sincronizar (`ProgressManager.sync(ServerPlayer)` → `PacketDistributor.sendToPlayer`)
- Login, respawn, troca de dimensão.
- Toda mudança de EP, nível, atributos (`addEp`, `setEp`, `allocate`, `resetStats`).
- Mana: no máximo 1 vez por segundo (junto com a regeneração do M2) e logo após gastar ao infestar.
- Só para o próprio jogador (progresso não é visível para outros).

### Client
- `client/ClientProgress`: guarda o último `SyncProgressPayload` do jogador local (estático, limpo ao desconectar — `ClientPlayerNetworkEvent.LoggingOut`).
- `client/KeyBindings`: nova `KeyMapping STATS`, padrão `K`, categoria Wispborn. `while (STATS.consumeClick())` abre `StatsScreen` se `ClientProgress` tiver dados.
- `client/StatsScreen extends Screen`:
  - topo: `Nível 3` · barra de EP até o próximo nível (`450/800`, ou "máximo") · `Mana 35/60` · `Pontos: 2`;
  - 4 linhas, uma por atributo: nome traduzido, `pontos/máximo`, efeito `atual → próximo` (ex.: `3,0% → 3,5% por pulso`),
    botão `+` ativo só se `availablePoints > 0` e `points < maxPoints`;
  - clicar `+` envia `AllocateStatPayload`; a tela se redesenha quando chega o novo sync (lê `ClientProgress` a cada frame);
  - layout centralizado, só widgets vanilla (`Button`, texto, retângulos com `GuiGraphics.fill`). Sem textura nova.

### Lang
`key.wispborn.stats`, `screen.wispborn.stats.title`, `screen.wispborn.stats.level`, `.ep`, `.ep_max`, `.mana`, `.points`,
`screen.wispborn.stats.effect.absorption` (`%s%% por pulso`), `.efficiency` (`×%s EP`), `.reserve` (`%s de mana`),
`.vitality` (`+%s corações`).

## Testes (JUnit)
- Round-trip do `StreamCodec` de `StatView` e de `SyncProgressPayload` com `Unpooled.buffer()`.
- `StatViewFactory` (puro): para pontos e config dados, gera `current`/`next` corretos para os 4 atributos;
  no máximo, `next == current`.

## Checklist manual
- [ ] `K` abre a tela com os mesmos números do HUD e do `/wispborn info`.
- [ ] Com 2 pontos: `+` em Vitalidade → corações sobem na hora, pontos vão a 1, linha mostra o novo atual/próximo.
- [ ] Com 0 pontos todos os `+` ficam desativados.
- [ ] Atributo em 10/10: `+` desativado e efeito sem seta.
- [ ] Tela aberta enquanto drena: EP e mana se atualizam sozinhos.
- [ ] Multiplayer: Player2 abrindo a própria tela vê só os próprios dados; alocação de um não afeta o outro.
- [ ] Servidor dedicado sobe sem crash.

## Fora de escopo
HUD no client (continua a action bar), animações, ícones, reset pela tela, novos atributos.

## Dúvidas do implementador
_(preencher se houver)_

## Relatório de implementação
_(preencher ao terminar)_
