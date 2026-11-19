# Wisp — revisão A3 para a V1

## Objetivo e estado

Substituir os espinhos do A1 por núcleo voxel chanfrado e duas cascas de halo.
Arquivos integrados ao projeto principal em 19/11/2026; **aprovação visual no Blockbench
e no Minecraft pendente**. Fonte, resources processados e JAR conferidos byte a byte.

## Geometria e textura

- Frente preservada em **−Z**; rastro em +Z. Pivôs e hierarquia do rig original preservados.
- Removidos `ray_1` a `ray_10` e referências vazias antigas a `ray_xy` e `ray_yz`.
- `core`: envelope 5×5×5 px com chanfros maiores em degraus de 0,5 px, seguindo
  uma silhueta esférica. 27 cuboides sem sobreposição; regiões adjacentes agrupadas
  reduzem o custo geométrico. UVs contínuos preservados.
- `rays` conserva nome e pivô [0, 12, 0]; agora é o controlador do halo.
- `halo_inner`: uma casca 7×7×7 px. `halo_outer`: uma casca 9×9×9 px, girada 45° em Y
  no projeto Blockbench (−45° no export Bedrock, conforme a conversão de coordenadas).
- Atlas RGBA 64×64. Núcleo opaco branco/ciano. Halos com borda pixelada arredondada,
  cantos transparentes e centro das faces quase transparente. Alpha máximo da textura base:
  86/255 no halo interno, 41/255 no externo. Na glowmask: 73/255 e 20/255.
- Máscara emissiva: núcleo e halo interno fortes, externo mais fraco. A aparência efetiva
  depende do render type e da camada emissiva na integração V1.
- Satélites, rastro, suas UVs e os pixels usados por eles foram preservados.
- Total: 16 ossos e 37 cuboides. Fonte editável com textura base embutida atualizada.

## Animações

- `idle`: 4 s em loop. Halo pulsa uniformemente 1,00 → 1,04 → 1,08 → 1,04 → 1,00.
- `walking`: 1 s em loop. Esticamento Z do halo limitado a 1,10, como no export anterior.
- Corrigidos dois valores inválidos de posição X de `trail_1` em 0,50 e 0,95 s:
  eram da ordem de 10¹⁶ px; agora são zero. O restante da trajetória foi preservado.
- `possess`: 1 s, sem loop. O export preserva integralmente os valores e tempos do A1.
  O `.bbmodel` antigo tinha alguns tempos diferentes do próprio export; foram alinhados
  ao export entregue: core 0,48 → 0,50; início dos rastros 0,53/0,56 → 0,55 e 0,59/0,62 → 0,60.
- Pose inicial de `possess` coincide com `idle` em posição, rotação e escala.
- `animation.wisp.attack` não existe.

| Intervalo | Movimento preservado |
| --- | --- |
| 0,00–0,35 s | Satélites convergem; halo contrai a 0,6; núcleo pulsa até 1,22. |
| 0,35–0,55 s | Corpo recua 1,75 px em +Z e se achata. |
| 0,55–0,85 s | Investida até −7,5 px em Z; halo estica como cometa; rastro se alonga. |
| 0,85–1,00 s | Corpo encolhe até 0,1; rastro se recolhe. |

### Ossos citados por clipe

- `animation.wisp.idle`: `body`, `core`, `rays`, `orbit`, `satellite_1`, `satellite_2`, `satellite_3`, `satellite_4`, `trail_1`, `trail_2`, `trail_3`, `trail_4`.
- `animation.wisp.walking`: `body`, `core`, `rays`, `orbit`, `trail_1`, `trail_2`, `trail_3`, `trail_4`.
- `animation.wisp.possess`: `body`, `core`, `rays`, `orbit`, `satellite_1`, `satellite_2`, `satellite_3`, `satellite_4`, `wake`, `trail_1`, `trail_2`, `trail_3`, `trail_4`.

## Arquivos e reprodução

- `wisp.bbmodel`: projeto editável.
- `wisp.geo.json`, `wisp.animation.json`: exports derivados do projeto.
- `wisp.png`, `wisp_glowmask.png`: textura e máscara emissiva.
- `idle.png`, `walking.png`, `possess.png`: prévias técnicas em fundo escuro.
- `idle_light.png`, `walking_light.png`, `possess_light.png`: mesmas poses em fundo claro.
- `a3-preview.png`: comparação das seis prévias.
- `src/revise_a3.py`: reconstrução idempotente da revisão A3; altera os arquivos da arte.
- `src/validate_a3.py`: conferência dos contratos da arte.
- `src/preview_a3.py`: renderizador de inspeção dos exports com interpolação linear.

Executar na raiz do repositório, com Python 3 e Pillow:

```sh
/usr/bin/python3 art/parasita-magico/src/revise_a3.py
/usr/bin/python3 art/parasita-magico/src/validate_a3.py
/usr/bin/python3 art/parasita-magico/src/preview_a3.py
```

As prévias usam câmera ortográfica, sem bloom ou pós-processamento. Quadros: idle 0,00 s,
walking 0,37 s e possess 0,70 s. **Não são capturas do Blockbench nem do Minecraft**;
o renderizador técnico não reproduz os shaders, depth sorting ou iluminação do GeckoLib.
A interface conectada desta sessão não oferecia acesso ao Blockbench nativo.

## Validação realizada

- PASS: nomes e hierarquia dos ossos, pivô de `rays`, dimensões e UVs dentro do atlas.
- PASS: clipes só referenciam ossos existentes; keyframes fonte/export correspondem.
- PASS: duração, loop, limites do halo, contração, pose inicial e coordenadas finitas.
- PASS: PNGs RGBA 64×64; textura embutida idêntica ao PNG; halo externo menos opaco.
- PASS: comparação com A1 preserva satélites, rastro e pivôs; animação de possessão intacta.
- PASS: regeneração idempotente verificada por hash dos cinco arquivos principais.
- PASS: inspeção visual das prévias técnicas em fundos claro e escuro.
- `./gradlew build` em 19/11/2026: **BUILD SUCCESSFUL in 17s**.
- PASS: os quatro arquivos integrados são idênticos em `art/`, `src/main/resources/`,
  `build/resources/main/` e `build/libs/wispborn-0.0.1.jar`.
- PASS: geometria empacotada contém 27 cuboides no núcleo e nenhum osso `ray_*`.
- A entrega anterior estava em um worktree temporário já removido; esta revisão está
  na pasta principal do projeto, branch `feat/V1-art-refresh`.

## Pendente antes da integração/aprovação V1

- Reabrir `wisp.bbmodel` no Blockbench e conferir UVs, transparência, frente e os três clipes.
- Substituir/complementar as prévias técnicas com capturas reais do editor.
- Executar o Passo 1 da V1: comando em F5, conjuração e observação pelo Player2.
- Testar a leitura a 1 e 16 blocos, de dia/noite e sobre água.
- Conferir glowmask, transparência e eventual interseção das cascas durante contração.
- Validar sincronização, logs GeckoLib, servidor dedicado e build da integração.
