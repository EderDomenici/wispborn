# Feedback da drenagem e cliques do parasita — design curto

**Data:** 20/11/2026. **Contexto:** teste da F3. A drenagem usa `setHealth` (sem golpe, para o hospedeiro
não reagir nem fugir) e o único sinal são 4 partículas de alma por pulso: Eder não percebeu a vaca perdendo
vida. No mesmo teste, o wisp balançava o braço e o bloco sumia e voltava ao clicar.

## 1. Feedback da drenagem

**Por quê:** o jogador precisa ver que o pulso aconteceu, e no multiplayer outros jogadores devem
reconhecer que aquela criatura está possuída e sendo drenada. Isso é desejado (decisão de Eder).

**Objetivo:** mostrar dano sem ser dano. **Anti-objetivo:** qualquer coisa que faça o mob reagir (fugir,
mirar, entrar em pânico). Por isso nada de `hurt()`, nem dano zero: a fuga dos animais reage a qualquer
golpe, mesmo de jogador no criativo.

### Abordagens consideradas
- **A. Piscada vermelha + onda em volta + som baixo** ✔ (escolha de Eder). Só servidor, todos veem,
  funciona em primeira pessoa, sem arte nova.
- B. Piscada verde na cor do wisp: identidade própria, mas o vanilla só tinge de vermelho; exige mexer na
  renderização dos mobs no cliente (risco com shaders e outros mods). Fica como melhoria possível na V1.
- C. Só partículas: o mais discreto, fraco em primeira pessoa e de longe.

### Comportamento
1. **A cada pulso de drenagem** (o mesmo ponto em que `tickDrain` aplica `setHealth`):
   - **Piscada:** enviar `ClientboundHurtAnimationPacket(mob)` para quem rastreia o mob. Só a animação
     (tinta vermelha e tranco); o mob não registra golpe, não muda de alvo, não foge, sem som de dano.
   - **Onda:** um anel de partículas na cor do wisp (`WispTuning.POSSESSED_PARTICLE_COLOR`) nasce no
     meio da altura do mob e se expande para fora, de cerca de `largura × 0,6` até `largura + 1` blocos,
     em uns 0,5 s. Substitui as 4 partículas de alma atuais.
   - **Som:** baixo e curto, diferente do som de consumo. Registrado como evento próprio
     `wispborn:host.drain_pulse` apontando para um som vanilla, para a arte poder trocar depois sem código.
     O tom sobe conforme o hospedeiro enfraquece (≈ 0,8 com vida cheia até ≈ 1,3 perto do piso).
2. **No piso de 20%** (pronto para consumir): sem pulsos, portanto sem piscada nem som. Um anel fica
   girando devagar e contínuo em volta do mob até ele ser consumido ou o wisp sair.
3. As partículas de "possuído" que já existem (a cada 10 ticks) continuam.

### Decisões minhas (corrigíveis)
- O som e a partícula exata do anel ficam com quem implementar, dentro das regras acima; a cor é a do wisp.
- Valores de volume, tamanho e duração em `WispTuning` (constantes), não em config.

## 2. Cliques do parasita

**Regra (Eder):** enquanto parasita (WISP ou HOST), os botões do mouse não fazem nada, nem animação.
Interações virão no futuro.

**Causa do problema atual:** as travas da M1 (`WispRestrictions`) rodam no servidor. O resultado já é
bloqueado, mas o cliente prevê a ação: balança o braço, mostra as rachaduras e apaga o bloco até o servidor
corrigir.

**Comportamento:**
- No cliente, `InputEvent.InteractionKeyMappingTriggered`: se o jogador local é parasita, cancelar
  ataque, uso e escolher bloco (botão do meio), com `setSwingHand(false)`.
- `WispRestrictions` no servidor continua como rede de segurança.

## Fora de escopo
Piscada verde (B), interações do wisp com o mouse, HUD em sprites (F4).

## Execução
Mudança pequena. Implementada por Claude na própria `feat/F3-drain-consume`, em commits separados, para
Eder testar junto com a F3.
