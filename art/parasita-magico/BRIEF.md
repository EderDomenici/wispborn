# Wisp — revisão para a M3b

A frente é **−Z** no Blockbench e no export Bedrock. No preview de `walking`, a parte
superior de `body` inclina para −Z e os fragmentos do rastro ficam em +Z.

Os dois planos cruzados deram lugar a **10 fragmentos volumétricos** em `ray_1` a `ray_10`,
filhos de `rays`. Os cuboides têm seção de 1×1 ou 2×2 px e comprimento de 4 a 7 px.
A textura ciano tem corpo opaco e pontas translúcidas. A máscara emissiva segue as
faixas claras. Em `walking`, a escala Z de `rays` chega a 1,1.

A animação `animation.wisp.possess` dura 1,00 s e toca uma vez. O quadro de 0,00 s
repete a pose inicial de `idle`.

| Intervalo | Movimento |
| --- | --- |
| 0,00–0,35 s | Satélites espiralam para o núcleo. Os raios contraem a 0,6 e o núcleo pulsa até 1,22. |
| 0,35–0,55 s | O corpo recua 1,75 px em +Z e se achata. |
| 0,55–0,85 s | O corpo avança a 7,5 px da origem em −Z. Os raios esticam no eixo Z e os quatro fragmentos do rastro se alongam em +Z. |
| 0,85–1,00 s | O corpo encolhe a 0,1 no ponto da investida, enquanto o rastro se recolhe. |

Capturas do Blockbench em 0,55 s (repouso), 0,37 s (movimento) e 0,70 s (possessão).
A validação em jogo depende da integração da M3b.
