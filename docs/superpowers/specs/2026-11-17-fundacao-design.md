# Wispborn — Design da rodada de fundação

**Data:** 17/11/2026 · **Status:** aprovado por Eder · **Substitui:** M4, M5 (e pausa o M6)

## Por que esta rodada existe

O conceito foi validado em jogo até o M3b: o wisp conjura, entra no mob e vira passageiro. Antes de
empilhar drenagem, consumo e tela de atributos, Eder decidiu firmar as mecânicas base e o HUD, porque
tudo o que vier depois (menu RPG, fenótipos, árvore de habilidades, reprodução do wisp) depende delas.

O loop que esta rodada precisa entregar, jogável e legível:

```
explorar (gasta fome) → achar presa do seu nível → infestar (gasta mana)
→ drenar (EP + fome de volta) → consumir (bônus de EP + fome) → subir de nível → presas maiores
```

## Objetivos

1. O jogador **nasce wisp**. Não há forma humana nem item de transformação.
2. **Morrer = renascer wisp**, mantendo nível, EP e atributos.
3. **Fome do wisp**: pressão para parasitar em vez de só explorar. Drena mais rápido quanto maior o nível
   e só volta drenando ou consumindo.
4. **Todo mob vanilla tem nível e sexo.** O nível sai de uma faixa por espécie e cada mob sorteia o seu
   ao nascer. O nível limita quem o wisp pode infestar e quanto EP a presa rende.
5. **Reprodução vanilla só entre macho e fêmea**: é a base para o wisp se reproduzir no futuro.
6. **HUD temático definitivo**, 100% sprites PNG em pixel art, substituindo o HUD vanilla enquanto o
   jogador é parasita.
7. Corrigir o **visual do wisp** (o brilho virou "estalactites") e confirmar a animação de possessão.

## Não-objetivos (ficam para depois)

- Menu RPG: gerenciamento de nível, fenótipos, árvore de habilidades. Terá brainstorm próprio.
- Tela de atributos do M6 (tecla K): fica pausada, porque será absorvida pelo menu RPG.
- A mecânica de reprodução do wisp (filhotes infectados, lacaios, espécie própria). Esta rodada só deixa
  o sexo pronto.
- Herança de nível e sexo dos pais. O filhote sorteia de novo.
- Controle do hospedeiro.

## Anti-objetivos (falharia mesmo "funcionando")

- HUD com barra ou ícone desenhado por código (`fill`, gradiente): tudo é sprite PNG.
- Texto cru na action bar como HUD permanente.
- Números de balanceamento fixos no código: tudo vai para config ou JSON de dados.
- Fome que nunca mata o wisp no Fácil/Normal (perderia a urgência).

## Decisões de Eder

| Tema | Decisão |
|---|---|
| Nível dos mobs | Faixa por espécie; cada mob sorteia o seu ao nascer; placa com nível e sexo |
| Level cap | Limita os dois: infestar exige nível do mob ≤ nível do wisp; presa muito abaixo rende pouco ou nada de EP |
| Forma | Nasce wisp; morrer = renascer wisp |
| Fome | Existe; escala com o nível; volta dentro do hospedeiro e ao consumir; usa a fome nativa por baixo com ícone próprio |
| HUD | Substitui o vanilla; temático; 100% sprites PNG pixel art; mana com cor de mana; EP = pontos de DNA com ícone de DNA no lugar do símbolo de XP |
| Sexo | Macho/fêmea em toda espécie com nível; base para a reprodução do wisp |
| Escopo | Fundação + HUD agora; visual do wisp em paralelo; menu RPG depois |

## Abordagens consideradas

- **Nível do mob guardado em cada mob** (attachment sorteado ao nascer e sincronizado) ✔. A alternativa,
  derivar o nível do UUID, dispensa armazenamento, mas qualquer ajuste de faixa embaralharia o nível de
  todos os mobs do mundo.
- **HUD em sprites PNG** ✔ (escolha de Eder). Descartados o híbrido com barras desenhadas por código e o
  HUD todo em código.
- **Ícone de fome próprio desenhado pela nossa camada** ✔. A alternativa, sobrescrever os sprites
  `minecraft:hud/food_*`, mudaria o ícone em qualquer contexto, sem controle.
- **Trava de reprodução nos pontos em que o par é escolhido** (`BreedGoal` e `AnimalMakeLove`) ✔. Uma
  trava só em `Animal#canMate` não pega as espécies que reescrevem esse método sem chamar a base (lobo,
  papagaio e a família dos cavalos: cavalo, burro, lhama, camelo). Cancelar o nascimento sozinho gastaria a comida e
  poria o casal em cooldown de 5 min sem explicação, então isso fica só como rede de segurança.
- **Ícones dentro de texto (♂/♀, DNA) como fonte bitmap do mod** ✔. Assim o mesmo PNG aparece na placa
  sobre o mob, no painel do alvo e no chat.

## Regras e números propostos

Todos os números ficam em config (`WispbornConfig`) ou em JSON de dados. Os valores abaixo são o ponto
de partida para o primeiro playtest.

### Fome (F1)
- Enquanto WISP (não HOST): `exaustão/s = hunger.exhaustionBasePerSecond (0,05) + hunger.exhaustionPerLevelPerSecond (0,01) × nível`.
  Com 4,0 de exaustão = 1 ponto, a barra cheia (20 + 5 de saturação) dura ≈ 28 min no Nv 1 e ≈ 7 min no Nv 20.
- Em HOST não drena.
- **Inanição do wisp:** com fome 0, fora do Pacífico, o wisp toma 1 de dano a cada 80 ticks até morrer,
  em qualquer dificuldade. O vanilla só mata no Difícil, e o wisp tem 6 de vida, então no Fácil a fome
  vanilla nem chega a causar dano.
- O wisp não come comida comum.

### Nível do wisp
- Máximo passa de 10 para **20**. Os limites de EP seguem a forma da curva atual (os incrementos sobem
  100 a cada nível): `300, 800, 1600, 2800, 4500, 6800, 9800, 13600, 18300, 24000, 30800, 38800, 48100,
  58800, 71000, 84800, 100300, 117600, 136800`.

### Nível e sexo dos mobs (F2)
- Faixas em `data/wispborn/wisp_mob_levels/vanilla.json`. A tabela inicial completa está na spec F2.
  Resumo: passivos 1–3, neutros 3–7, hostis comuns 4–10, creeper e bruxa 8–13, enderman 12–16,
  Nether 10–18, shulker 16–20. Chefes e jogadores são imunes.
- Sexo: 50/50 (`traits.femaleChance = 0,5`).
- Infestar exige `nível do mob ≤ nível do wisp + infest.levelGateMargin (0)`.
- Custo de mana: `mana.infestCostPerLevel (5) × nível do mob`.
- Fator de EP pelo nível: 1,0 até 2 níveis abaixo do wisp; cai 25% por nível; 0 a partir de 6 abaixo
  (`ep.levelGrace = 2`, `ep.falloffPerLevel = 0,25`).

### Drenagem e consumo (F3)
- Pulso a cada `drain.intervalTicks` (100); piso em 20% da vida.
- EP por pulso: `ep.perPulsePerLevel (3) × nível do mob × fator`; consumo: `ep.consumePerLevel (30) × nível do mob × fator`
  (Eficiência aplicada por cima, como hoje).
- Fome por pulso: +1 ponto e +0,5 de saturação. Fome ao consumir: `+ (2 + nível do mob / 2)` pontos com
  modificador de saturação 0,3.
- Referência de ritmo: uma galinha Nv 1 rende 150 EP, então duas levam o wisp ao Nv 2. Uma vaca Nv 2 rende
  300 EP e um zumbi Nv 9 rende 1350 EP (sem atributos, fator 1). Cada presa leva ≈ 3 min 20 s do 100% ao piso.

## Specs e ordem

| Ordem | Spec | Entrega | Depende de |
|---|---|---|---|
| 1 | `docs/specs/F1-born-wisp.md` | Nascer/renascer wisp, fome, inanição | — |
| 2 | `docs/specs/F2-mob-traits.md` | Nível e sexo por mob, fim dos tiers, gate de infestação, mixin de reprodução | F1 |
| 3 | `docs/specs/F3-drain-consume.md` | Drenagem e consumo com nível e fome | F2 |
| 4 | `docs/specs/F4-hud.md` | Sync de estado + HUD em sprites | F3 + arte A2 |
| paralelo | `docs/specs/V1-wisp-visual.md` | Diagnóstico da animação, feedback em primeira pessoa, novo brilho | arte A3 |

Arte: prompts **A2** (sprites do HUD, fonte de ícones e mockup) e **A3** (redesign do brilho do wisp) em
`docs/prompts/codex-art-prompts.md`.

## Deixado para o implementador

Nomes internos além dos citados nas specs, organização interna dos payloads, faixas de mobs não listados,
detalhes de partículas. Qualquer número novo vai para config ou JSON.
