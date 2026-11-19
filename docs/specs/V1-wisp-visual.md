# V1 — Visual do wisp: brilho de verdade, animação de possessão e feedback em primeira pessoa

**Objetivo:** o wisp precisa ler como um **orbe de energia que brilha**, não como um cubo com espinhos.
A animação de possessão precisa ser vista, e o jogador precisa de feedback mesmo em primeira pessoa, onde o
próprio modelo nunca aparece.
**Estimativa:** 1–2 dias de código + arte. **Branch:** `feat/V1-wisp-visual`. **Depende de:** arte **A3**
(`docs/prompts/codex-art-prompts.md`). Corre em paralelo com F1–F4; se encostar em arquivos da F4
(`possess_target`), faça a V1 depois da F4.

## Diagnóstico (teste de 17/11/2026)
- **"Estalactites":** o modelo atual (prompt A1, M3b) é um núcleo cúbico 5×5×5 com 10 varetas `ray_*`
  de 1×1×4 a 2×2×7 px. Com `AutoGlowingGeoLayer`, as varetas viram espinhos claros e opacos. O pedido do
  A1 foi o problema: fragmentos alongados lêem como espinho, não como brilho. É arte, não código.
- **Animação de possessão "não aconteceu":** `HostManager.tryInfest` dispara `WispAnimatable.playPossess`
  no início da conjuração (1 s). Mas o modelo só é desenhado por `RenderPlayerEvent`, que **não roda em
  primeira pessoa**. Hipótese principal: tocou e não dava para ver. Validar antes de mudar código.

## Regras

### Passo 1 — Validar a animação (antes de qualquer código)
1. `./gradlew runClient`, F5 (terceira pessoa), `/wispborn anim possess`: a animação toca inteira?
2. F5 de novo, segurando a tecla de possessão num mob válido: toca durante a conjuração?
3. `./gradlew runClient2` (segundo jogador olhando o primeiro): o Player2 vê a animação?
4. Registrar o resultado dos três em "Relatório". Se algum falhar, investigar só esse caso
   (ex.: `triggerAnim` do lado do servidor não chegando ao próprio jogador) e descrever a causa.

### Passo 2 — Novo modelo (arte A3)
5. Integrar os arquivos do A3 (`geo`, `animation`, `wisp.png`, `wisp_glowmask.png`) copiando de
   `art/parasita-magico/` para `assets/wispborn/`, como no M1b.
6. Se o A3 trouxer camadas translúcidas (halo), conferir o render type: a camada emissiva do GeckoLib
   (`AutoGlowingGeoLayer`) e a textura base com transparência precisam aparecer certas de perto e de
   longe, em ambiente claro e escuro, e sobre água.

### Passo 3 — Feedback em primeira pessoa
7. Durante a conjuração (estado de `ClientPossession.castState()`), desenhar uma vinheta em sprite
   `wispborn:hud/cast_vignette` (tela inteira, esticada) com opacidade crescendo de 0 a
   `client.castVignetteMaxAlpha` (0,6) conforme o progresso. Some na hora se a conjuração for cancelada.
8. No sucesso da possessão, um flash curto do mesmo sprite (alpha 0,8 → 0 em 6 ticks).
9. Partículas no client: durante a conjuração, partículas `END_ROD` saindo da câmera em direção ao alvo
   (3 por tick, só para o jogador local), para dar a sensação de "investida" que a animação dá em F5.
10. O sprite `cast_vignette` vem do A2 (sprites do HUD). Até lá, a V1 pode ficar sem o passo 3.

## Contratos
- `client/WispFirstPersonFx.java` (só `Dist.CLIENT`): camada de GUI da vinheta + partículas.
- Config client: `client.castVignetteMaxAlpha` = 0.6 (0–1).

## Testes
Sem lógica pura nova além de `HudMath` (F4). Se houver curva de opacidade, extrair
`static float vignetteAlpha(float progress, float max)` e testar 0 → 0, 1 → max, clamp fora de [0, 1].

## Checklist manual
- [ ] Passo 1 registrado no relatório (F5, comando, conjuração, Player2).
- [ ] Wisp novo em F5: lê como orbe brilhante de perto (1 bloco) e de longe (16 blocos), de dia e de noite. Nenhum espinho.
- [ ] Idle, walking e possess rodam sem ossos faltando (log sem erro do GeckoLib).
- [ ] Primeira pessoa: a vinheta cresce durante a conjuração, some ao cancelar, pisca no sucesso; as partículas vão da câmera ao alvo.
- [ ] Player2 vê o wisp novo e a animação de possessão do Player1.
- [ ] Servidor dedicado sobe sem crash.

## Fora de escopo
Animações novas de drenagem e consumo, câmera do hospedeiro, modelos de outras criaturas.

## Dúvidas do implementador
_(preencher se houver)_

## Relatório de implementação

### Arte A3 e integração — 19/11/2026

- Atualização na pasta principal, branch `feat/V1-art-refresh`, preservando F1/F2/M3c.
- Causa do modelo antigo: fonte, resources e build da branch ativa ainda tinham
  núcleo cúbico e dez espinhos; o arredondamento anterior ficou em worktree temporário
  que não existe mais. A revisão foi reconstruída pelos scripts versionados.
- Atualizados `art/parasita-magico/wisp.bbmodel`, geo, animações, textura, glowmask,
  prévias técnicas e BRIEF. Núcleo de 5 px com 27 cuboides em degraus de 0,5 px;
  halos de 7 e 9 px. Rig e tempos da possessão preservados.
- Copiados os quatro arquivos para `src/main/resources/assets/wispborn/`, nos caminhos
  `geo/entity/`, `animations/entity/` e `textures/entity/`.
- `./gradlew build`: **BUILD SUCCESSFUL in 17s**. Arte validada por
  `art/parasita-magico/src/validate_a3.py`; fonte, resources, build e JAR idênticos.
- A2 entregue em `art/hud/`, incluindo `cast_vignette.png`; pacote artístico ainda
  não integrado ao HUD. Nomes e dimensões conferidos contra a tabela da F4.
- Não implementado nesta entrega artística: `WispFirstPersonFx`, configuração da
  vinheta, flash, partículas ou HUD F4. Nenhum código Java foi alterado.
- Pendentes: Passo 1 em F5/Player2 e revisão in-game de transparência, distância,
  água e logs. As prévias são renders técnicos, não capturas do Blockbench/jogo.
