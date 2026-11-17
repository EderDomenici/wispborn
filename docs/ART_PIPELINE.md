# Wispborn — guia de produção de arte

Use este guia ao planejar, criar ou revisar modelos, animações, efeitos e estruturas. O [plano local](PLAN.md) e a spec da entrega definem a jogabilidade. Registre neste guia decisões de arte que se apliquem a várias entregas. Decisões específicas de um recurso ficam na sua pasta ou na spec correspondente.

## Estado atual

- A stack é Minecraft 1.21.1, NeoForge 21.1.x e Java 21. O jogador assume a forma de um wisp, infesta hospedeiros vanilla, drena vida e consome o hospedeiro quando a vida chega a 20% ou menos. O MVP usa a IA e os modelos dos hospedeiros vanilla.
- O [M1](specs/M1-wisp-form.md) usa dois planos voltados para a câmera com render emissivo, pulsação, flutuação e rastro de `END_ROD` no [WispRenderHandler](../src/main/java/dev/eder/wispborn/client/WispRenderHandler.java). A forma HOST esconde a representação do jogador e a mão acompanha as regras das formas parasitas.
- O [Parasita Mágico](../art/parasita-magico/wisp.bbmodel) é um protótipo 3D editável do mesmo personagem. Tem núcleo cúbico, planos cruzados, quatro satélites, quatro fragmentos de rastro e máscara emissiva. A pasta reúne o projeto Blockbench, os JSONs exportados, os PNGs e três [capturas de validação](../art/parasita-magico/idle.png). A [spec M1b](specs/M1b-wisp-model.md) prevê a integração com GeckoLib 4.9.3, ainda pendente no código. Para implementar, consulte as [regras GeckoLib do projeto](../.agents/rules/geckolib-4.9.md).
- **Decisões de Eder (16/11/2026):** o GeckoLib 4.9.3 é a base de modelos e animações de criaturas do mod, pensando nas criaturas mais complexas do pós-MVP. O wisp fica 20% maior que no M1 (escala 0,48, ~0,86 bloco) e **não voa**: anda no chão com gravidade reduzida, e a animação `walking` dá a leitura de deslocamento.

| Clipe do protótipo | Duração | Uso visual previsto |
| --- | --- | --- |
| `animation.wisp.idle` | 4 s, loop | Flutuação, pulsação e órbita em repouso. |
| `animation.wisp.walking` | 1 s, loop | Deslocamento no chão, com inclinação e rastro. |
| `animation.wisp.attack` | 0,8 s, uma execução | Antecipação visual da possessão no [M3](specs/M3-possession.md). O nome é o ID do clipe exportado. O wisp do MVP não ataca. |

## Direção visual

- Use formas e texturas originais, com silhuetas reconhecíveis à distância, geometria voxel e pixels nítidos. O wisp tem núcleo branco e ciano, bordas verde-esmeralda e feixes verde-água translúcidos. Mantenha o centro compacto e os satélites legíveis em movimento.
- Faça cada evento ter um movimento próprio. Como proposta, a infestação converge para o alvo, a drenagem emite um pulso curto a partir do hospedeiro e o consumo combina contração com uma ruptura breve. Forma, direção e ritmo ajudam a distinguir esses eventos mesmo em ambientes claros ou com muitas partículas.
- Para mobs próprios, defina primeiro a silhueta, a escala em blocos, a função e a leitura em movimento. Para estruturas, comece com uma maquete de blocos vanilla e avalie altura, acesso e percurso em jogo antes de detalhar.

## Organização dos arquivos

| Entrega | Fonte e revisão | Integração, quando prevista na spec |
| --- | --- | --- |
| Criatura ou prop animado | Uma pasta por recurso em `art/`, como `art/parasita-magico/`. Guarde `.bbmodel` (formato Bedrock/GeckoLib), PNG original, máscara emissiva quando houver, exports e capturas. | Copie só os arquivos que o GeckoLib carrega: `geo/entity/<id>.geo.json`, `animations/entity/<id>.animation.json`, `textures/entity/<id>.png` e `textures/entity/<id>_glowmask.png`, todos em `src/main/resources/assets/wispborn/`. A spec indica os caminhos. |
| Efeito | `art/efeitos/<id>/`, se houver sprite, estudo de movimento ou captura para versionar. | Use os efeitos vanilla já especificados quando suficientes. Para partículas registradas que usam sprites, guarde a definição em `assets/wispborn/particles/` e os PNGs em `assets/wispborn/textures/particle/`. |
| Estrutura | `art/estruturas/<id>/`, com captura, dimensões, ponto de entrada e instruções para editar ou regenerar a maquete. | Para uma estrutura salva no Minecraft 1.21.1, versione o template em `src/main/resources/data/wispborn/structure/<id>.nbt`. A spec de geração decide posicionamento, regras e arquivos adicionais de worldgen. |

Use IDs e nomes de ossos em inglês e `snake_case`. Preserve nomes e pivôs usados pelos clipes ao revisar a geometria. Se um nome mudar, atualize projeto, exports e referências na mesma entrega. Descrições, briefs e relatórios ficam em português. Os nomes exibidos no jogo seguem os arquivos de idioma do mod.

## Fluxo para novas entregas

1. **Brief.** Registre a spec, finalidade, referência visual, escala, ponto de origem, eixo frontal e ponto de ancoragem. Para cada clipe ou efeito, anote gatilho, duração, loop ou execução única e o quadro em que a ação deve ficar legível.
2. **Modelo e movimento.** Faça a silhueta em escala de jogo antes da textura. Separe em grupos as peças que se movem de forma independente. No Blockbench, confira pivôs, UVs, junção dos loops e pose final dos clipes que tocam uma vez. Para efeitos, defina quais partes seguem a entidade e quais ficam no mundo, como o rastro.
3. **Exportação.** Salve a fonte editável e gere os arquivos finais a partir dela. Reabra o projeto e confira dimensões e transparência dos PNGs, referências dos clipes aos ossos e enquadramento. Capture repouso, movimento e o quadro principal da ação. Para estruturas, capture a entrada e uma vista geral e teste a colocação manual com o ponto de origem escolhido.
4. **Validação no Minecraft.** Quando houver integração, confira a leitura em primeira e terceira pessoa, de perto e de longe, em ambientes claros e escuros. Para efeitos translúcidos, teste sobreposição com água e outros efeitos. Para ações de jogo, confira em dois clientes o gatilho correto e o fim do visual. O servidor confirma possessão, drenagem e consumo. O cliente renderiza com base nos estados e eventos autorizados.
5. **Entrega.** Registre na spec os caminhos, as decisões e o que foi validado. Marque como pendente qualquer verificação que só possa ser feita em jogo. Eder valida a arte no cliente e aprova a integração.

## Ordem de trabalho recomendada

1. **Aprovar a direção do wisp.** Revisar os três clipes no Blockbench e comparar o protótipo com o visual do M1 em jogo, principalmente escala, brilho e legibilidade. Usar o wisp como primeira referência para o restante do mod.
2. **Validar a integração 3D no M1b.** Começar pela prova de conceito prevista na spec, substituindo o visual do jogador pelo modelo com `idle`. Depois, conferir máscara emissiva, transparência, direção e transição do movimento, comportamento em HOST e partida do servidor dedicado. O comando de debug do M1b permite revisar o clipe de possessão isoladamente.
3. **Priorizar a leitura do ciclo do MVP.** No M3, conferir se a antecipação da possessão permanece visível antes da transição para HOST e se a animação é vista pelos outros jogadores. Nos módulos M4 e M5, distinguir pulso de drenagem, parada no limite de consumo e consumo. Começar pelos efeitos vanilla previstos nas specs e personalizar onde a revisão em jogo mostrar que dois eventos se confundem. Testar com um hospedeiro pequeno e um alto para acertar os pontos de emissão.
4. **Abrir frentes pós-MVP conforme o plano.** Para o covil (0.4), começar com uma estrutura pequena colocada manualmente e validar circulação e ponto de entrada antes de discutir geração no mundo. Para espécie própria e lacaios (0.5), fazer um único mob completo como referência de rig, textura, animação e revisão em jogo antes de produzir variantes.
