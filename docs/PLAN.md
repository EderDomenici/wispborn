# Wispborn — plano resumido

Plano completo (fonte da verdade): https://claude.ai/artifact/3H3tDWH91WeUUiaNozhw7G
Mapa visual (Whiteboard): https://claude.ai/artifact/2GCsrb2iiwEWh7P1kp9Lz4

## Conceito
O jogador é um wisp: um orbe de energia flutuante que parasita criaturas. O nível do wisp define
quais criaturas ele consegue infectar. Dentro do hospedeiro, sem fazer nada, drena a vida dele aos
poucos e ganha EP. A criatura só pode ser consumida (morta) com 20% da vida ou menos. Cada nível dá
pontos para melhorar o próprio parasita.
Depois do MVP: controle do hospedeiro, traços (fagogenia), arquétipo, covil, espécie própria.

## MVP 0.1 — possessão, consumo e progressão com mobs vanilla
1. Wisp — orbe pequeno, frágil, flutua, não ataca.
2. Infestar — botão direito; tier do mob (tag) ≤ tier liberado pelo **nível**; custa 15 × tier de mana; vira passageiro invisível e invulnerável. **Sem controle do mob.**
3. Drenar — (2% + Absorção) da vida máxima a cada 5s, +10 × tier EP (× Eficiência) por pulso; para em 20%.
4. Consumir — tecla C só com ≤20%; mata com dano `wispborn:consumed`, sem drops, +100 × tier EP (× Eficiência).
5. Nível — EP total sobe o nível (Nv 2…10: 300, 800, 1600, 2800, 4500, 6800, 9800, 13600, 18300); 2 pontos por nível.
6. Falha — mob morre por outra causa: expulso sem bônus. Shift sai. Logout expulsa.

| Tier | Mobs | Nível mínimo |
|---|---|---|
| 1 | galinha, porco, vaca, ovelha, coelho | 1 |
| 2 | zumbi, esqueleto, aranha | 3 |
| 3 | creeper, enderman | 5 |

| Atributo | Por ponto (máx. 10) | Base |
|---|---|---|
| Absorção | +0,5% de drenagem por pulso | 2% |
| Eficiência | +10% de EP | 100% |
| Reserva de mana | +10 de mana máxima | 50 (regenera 1/s como wisp) |
| Vitalidade | +1 coração no wisp | 3 corações |

Todos os números ficam em config de servidor.

## Módulos
| Módulo | Spec | Entrega |
|---|---|---|
| M0 | `specs/M0-setup.md` | Projeto, CI, AGENTS.md, client2 |
| M1 | `specs/M1-wisp-form.md` | Forma wisp + sync |
| M1b | `specs/M1b-wisp-model.md` | GeckoLib, modelo e animações do wisp, tamanho +20% |
| M2 | `specs/M2-level-stats.md` | EP, nível, tiers, pontos, atributos, mana, config, HUD |
| M3 | `specs/M3-possession.md` | Possessão como passageiro, custo de mana, saídas |
| M3b | `specs/M3b-possession-feel.md` | Tecla com conjuração, indicador de alvo, efeito no possuído, som do wisp |
| M4 | `specs/M4-drain.md` | Drenagem até 20% + EP passivo |
| M5 | `specs/M5-consume.md` | Tecla C, consumo, bônus |
| M6 | `specs/M6-stats-screen.md` | Tela de atributos (tecla K) |

Pós-MVP: 0.2 controle do hospedeiro · 0.3 traços + tela de evolução · 0.4 arquétipo + covil · 0.5 espécie própria, lacaios, loop de vilão.
