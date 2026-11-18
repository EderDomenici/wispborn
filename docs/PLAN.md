# Wispborn — plano resumido

Plano completo original: https://claude.ai/artifact/3H3tDWH91WeUUiaNozhw7G
Mapa visual (Whiteboard): https://claude.ai/artifact/2GCsrb2iiwEWh7P1kp9Lz4
**Rodada de fundação (17/11/2026), fonte da verdade atual:** `docs/superpowers/specs/2026-11-17-fundacao-design.md`

## Conceito
O jogador **nasce wisp**: um orbe de energia que parasita criaturas. Explorar gasta **fome**; só
parasitando o wisp se alimenta. Todo mob vanilla tem **nível** e **sexo**. O nível do wisp define quais
criaturas ele consegue infestar, e presas fracas demais não fazem ele evoluir. Dentro do hospedeiro, drena
a vida dele aos poucos e ganha EP (pontos de DNA) e fome. Com 20% da vida ou menos, a criatura pode ser
consumida. Cada nível dá pontos para melhorar o próprio parasita.
Depois da fundação: menu RPG (nível, fenótipos, árvore de habilidades), reprodução do wisp, controle do
hospedeiro, covil, espécie própria.

## MVP 0.1 — loop da fundação com mobs vanilla
1. Wisp — nasce e renasce wisp; orbe pequeno, frágil, anda no chão, não ataca, não come.
2. Fome — drena enquanto wisp (mais rápido em nível alto); zerada, mata. Volta drenando e consumindo.
3. Infestar — segurar a tecla de possessão; nível do mob ≤ nível do wisp; custa 5 × nível do mob de mana;
   vira passageiro invisível e invulnerável. **Sem controle do mob.**
4. Drenar — (2% + Absorção) da vida máxima a cada 5 s; +3 × nível do mob EP (× fator de nível × Eficiência)
   e +1 de fome por pulso; para em 20%.
5. Consumir — tecla C só com ≤ 20%; mata com dano `wispborn:consumed`, sem drops; +30 × nível do mob EP
   (× fator × Eficiência) e +(2 + nível/2) de fome.
6. Fator de nível — presa até 2 níveis abaixo rende EP cheio; −25% por nível além disso; 0 a 6 abaixo.
7. Nível do wisp — 1 a 20 pela curva de EP do config; 2 pontos por nível.
8. Reprodução vanilla só entre macho e fêmea (base para a reprodução do wisp).
9. HUD próprio em pixel art substituindo o vanilla enquanto parasita.

Faixas de nível dos mobs: `docs/specs/F2-mob-traits.md` (ex.: galinha 1–2, zumbi 4–8, enderman 12–16,
shulker 16–20; chefes imunes).

| Atributo | Por ponto (máx. 10) | Base |
|---|---|---|
| Absorção | +0,5% de drenagem por pulso | 2% |
| Eficiência | +10% de EP | 100% |
| Reserva de mana | +10 de mana máxima | 50 (regenera 1/s como wisp) |
| Vitalidade | +1 coração no wisp | 3 corações |

Todos os números ficam em config de servidor ou em JSON de dados.

## Módulos
| Módulo | Spec | Entrega | Status |
|---|---|---|---|
| M0 | `specs/M0-setup.md` | Projeto, CI, AGENTS.md, client2 | feito |
| M1 | `specs/M1-wisp-form.md` | Forma wisp + sync | feito |
| M1b | `specs/M1b-wisp-model.md` | GeckoLib, modelo e animações do wisp | feito |
| M2 | `specs/M2-level-stats.md` | EP, nível, pontos, atributos, mana, config | feito |
| M3 | `specs/M3-possession.md` | Possessão como passageiro, custo de mana, saídas | feito |
| M3b | `specs/M3b-possession-feel.md` | Conjuração, indicador de alvo, som do wisp | feito |
| M3c | `specs/M3c-host-stealth.md` | Wisp indetectável dentro do hospedeiro (mobs perdem o alvo) | a fazer, depois da F1 |
| F1 | `specs/F1-born-wisp.md` | Nascer/renascer wisp, fome, inanição | a fazer |
| F2 | `specs/F2-mob-traits.md` | Nível e sexo dos mobs, fim dos tiers, reprodução macho + fêmea | a fazer |
| F3 | `specs/F3-drain-consume.md` | Drenagem e consumo com nível e fome | a fazer |
| F4 | `specs/F4-hud.md` | HUD temático em sprites | a fazer (depende da arte A2) |
| V1 | `specs/V1-wisp-visual.md` | Brilho do wisp, animação de possessão, feedback em 1ª pessoa | paralelo (arte A3) |
| ~~M4~~ | `specs/M4-drain.md` | — | substituída pela F3 |
| ~~M5~~ | `specs/M5-consume.md` | — | substituída pela F3 |
| M6 | `specs/M6-stats-screen.md` | Tela de atributos (tecla K) | pausada; vai para o menu RPG |

Pós-fundação: menu RPG (nível, fenótipos, árvore de habilidades) · reprodução do wisp · controle do
hospedeiro · arquétipo + covil · espécie própria, lacaios, loop de vilão.
