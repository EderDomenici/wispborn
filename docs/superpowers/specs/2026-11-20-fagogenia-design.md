# Fagogenia: assimilação, características, habilidades e metamorfose (design)

**Data:** 20/11/2026. **Status:** aprovado por Eder em 20/11/2026.
**Base:** fundação (`2026-11-17-fundacao-design.md`), F3 (drenagem e consumo), F4 e F5 (HUD e grimório).
**Adiado:** herança e prole (o que passa para a próxima geração, espécie própria, lacaios).

## Por quê

O grimório da F5 tem as abas Traços, Aspectos, Habilidades e Bestiário, mas nenhum sistema por trás. Antes de
desenhar telas, Eder decidiu fechar o que cada coisa é e como se ligam, para a fundação ficar bem definida.

A base é a **fagogenia** (HxH, Formigas Quimera): o wisp se alimenta de seres vivos e absorve o que eles têm. A
identidade própria do Wispborn está no parasita: possuir, adaptar-se ao mundo, evoluir o próprio corpo por
metamorfose (referência: Alien) e, no futuro, virar uma espécie própria. Também entram referências de Slime
Datta Ken (aprender habilidades consumindo uma espécie) e de soulslike (EP como moeda que se pode perder).

## Objetivos

1. Cada sistema com **uma origem e um papel**, sem sobreposição: atributo, habilidade, gene, aparência.
2. **Assimilação** por trilhas de espécie, com marcos: comer variado faz o wisp crescer.
3. **EP como moeda**: cada gasto é uma escolha (nível, gene, habilidade).
4. **Possessão como caçada**: mana como luta contra o sistema imune do hospedeiro.
5. **Habilidades** passivas e ativas, incluindo as do próprio parasita.
6. **Metamorfose** do jogador: wisp → forma simples → juvenil → alfa, com corpo montado por genes.
7. Grimório e HUD mostrando tudo isso, construídos junto com cada mecânica.

## Não objetivos

- Herança, prole, reprodução do wisp, espécie própria, lacaios, covil (adiados por Eder em 20/11).
- Lista final de espécies, marcos, habilidades e números: conteúdo e balanceamento, vão para JSON e config nas
  specs.
- Temperatura do casulo (as adaptações já cobrem o ambiente).

## Anti-objetivos (falha mesmo funcionando)

- Dois sistemas entregando o mesmo efeito (ex.: um traço e uma habilidade que reduzem o mesmo dano).
- Qualquer coisa fora dos atributos melhorando o parasita em números (drenagem, EP, mana).
- Um item central do jogo preso a uma única fonte rara, ou uma adaptação sem fonte alcançável sem ela.
- Telas mostrando dados falsos à espera da mecânica (o que aconteceu com o M6).
- Números de balanceamento no código: tudo em config ou JSON de dados (regra da fundação).

## Mapa

```
Wispborn: parasita com fagogenia
├─ Alimentação: drenar + consumir                          (fundação)
├─ Nível ──(EP)──▶ Atributos: o parasita em números
└─ Assimilação: trilhas por espécie + Trilha do Parasita
     marco DESCOBRE ──(EP)──▶ LIBERA
     ├─ Aparência: cores e padrões (grátis ao descobrir)   → wisp agora, prole depois
     ├─ Gene: mora no CORPO                                → montar o corpo na metamorfose
     └─ Habilidade: mora no NÚCLEO (passiva | ativa)       → o wisp usa em qualquer forma
          └─ Adaptação: passiva com graus → possuir espécies + sobreviver no ambiente
```

## 1. O que é o quê

| Sistema | De onde vem | Onde mora | Para que serve |
|---|---|---|---|
| **Atributo** | Nível (comprado com EP) | Núcleo | Melhorar o parasita em números. São os 4 de hoje (Absorção, Eficiência, Reserva, Vitalidade) |
| **Habilidade** | Assimilação | Núcleo | Poder usado pelo wisp em qualquer forma, passiva ou ativa (estilo Slime Datta Ken) |
| **Gene** | Assimilação | Corpo | Peça do corpo montado na metamorfose; vale só enquanto o jogador está naquele corpo |
| **Aparência** | Assimilação | Núcleo | Cores e padrões do wisp (e da prole, no futuro); só visual |

**Fronteira:** só atributo melhora o parasita em números. Gene é do corpo: some quando o corpo morre e volta no
próximo casulo (ex.: patas de aranha → escalar; carapaça → armadura; guelras → respirar). Habilidade é do
núcleo: vale em qualquer forma, inclusive como wisp, e nunca se perde (ex.: teia, adaptação ao calor). Um efeito
nunca existe em dois sistemas ao mesmo tempo.

**Atributos valem em qualquer forma** (moram no núcleo; decisão do Claude): Absorção e Eficiência valem sempre
que o jogador drena ou consome; Reserva define a mana em qualquer forma; Vitalidade soma corações à vida base
do corpo (a do wisp, a do hospedeiro ou a base neutra).

"Aspecto" deixou de ser sistema próprio (virou Aparência, um tipo de característica). "Traço" deixou de existir
como nome: o que era traço virou gene ou habilidade, conforme onde mora.

## 2. Assimilação

1. **Consumir** uma criatura avança a **trilha** da espécie dela. Presas fracas demais para dar EP também
   contam (separa a coleção da progressão de nível).
2. Cada espécie declara no JSON de dados os seus **marcos** (quantidade consumida) e o que cada marco descobre:
   uma cor, um padrão, um gene, uma habilidade ou o grau seguinte de uma habilidade. A quantidade varia por
   espécie: presas de fazenda pedem mais (ex.: galinha 10), presas difíceis pedem menos (ex.: enderman 3).
3. **Descobrir ≠ liberar.** O marco descobre; liberar custa EP (seção 3). Cores e padrões são exceção: liberam
   de graça ao descobrir.
4. As trilhas se ligam: o que uma espécie dá abre caminho para outras (teia ajuda a capturar presas rápidas;
   Calor I do deserto prepara para o Nether).
5. **Trilha do Parasita:** trilha própria cujos marcos contam **espécies diferentes assimiladas** (com pelo
   menos um consumo). Dá as habilidades de parasita (ex.: marco 5 → Tomar conta do corpo; marco 10 → Pular de
   corpo, que exige a anterior). Pré-requisitos seguem a ordem dos marcos.
6. O progresso das trilhas e tudo o que foi descoberto e liberado **fica ao morrer**, como o nível
   (decisão do Claude; revisar quando o tópico Herança voltar).

**Abordagens consideradas:** uma presa basta (descartada: vira checklist); número fixo para toda espécie
(descartado: presas de fazenda viram atalho); **marcos por espécie no JSON** ✔. Habilidades de parasita:
marcos por nível (descartado: foge da regra "habilidade vem da assimilação"); espécies temáticas (descartado:
prende o essencial a mobs raros); **diversidade** ✔.

## 3. EP como moeda (estilo soulslike)

1. EP deixa de subir o nível sozinho. É **saldo**, gasto em: subir de nível, liberar genes, liberar
   habilidades e graus.
2. **Preço do nível** = diferença entre os limites da curva atual (Nv 2 = 300, Nv 3 = 500, Nv 4 = 800…), em
   config. Subir de nível continua dando 2 pontos de atributo e o nível continua sendo chave de possessão.
3. **Nuvem de DNA:** quando o **wisp** morre (morte real, com respawn), o EP não gasto cai no local como uma
   nuvem recuperável (tocar nela devolve o EP). Morrer de novo antes de recuperar perde a primeira nuvem; a
   nova morte deixa a sua. A morte do **corpo** não gera nuvem (§7): a perda ali é o próprio corpo.
4. Preços de genes, habilidades e graus ficam no JSON de cada item.

**Abordagens consideradas:** EP sobe nível sozinho (descartado: o jogador nunca escolhe); EP sem perda na morte
(descartado: vira loja, sem tensão de carregar saldo); **nuvem recuperável** ✔.

## 4. Possessão e mana

1. **Entrar é grátis**, mas exige um mínimo de mana (config); a conjuração de 1 s continua. Some o custo de
   entrada `mana.infestCostPerLevel × nível do mob`.
2. **Dentro do hospedeiro a mana cai continuamente** (o parasita lutando contra o sistema imune). O gasto cresce
   com a força do hospedeiro em relação ao wisp: presa bem abaixo do nível quase não resiste. Mana zerou → o
   hospedeiro expulsa o wisp.
3. **Drenar até o fim não precisa caber numa possessão só.** No começo o wisp é expulso, ronda a vítima,
   recupera mana e entra de novo: isso é a caçada. A vida drenada fica (a maioria dos mobs vanilla não
   regenera), mas a vítima anda e pode escapar.
4. **Mana regenera em qualquer forma, menos dentro do hospedeiro** (hoje só regenera na forma wisp).
   "Dentro do hospedeiro" inclui a forma simples **agarrada** a uma criatura: as duas são parasitar, então as
   duas gastam mana, não regeneram e bloqueiam ativas sem "Tomar conta do corpo" (decisão do Claude).
5. **Possuir exige:** nível do mob ≤ nível do wisp (regra atual) **e** as adaptações que a espécie declarar no
   JSON (ex.: Blaze exige Calor II).

**Abordagens consideradas:** custo de entrada + gasto contínuo (descartado: trava o começo, com 50 de mana);
possessão grátis sem gasto (descartado: vira botão de pânico, já que dentro o wisp é invulnerável e invisível);
**entrada grátis + gasto contínuo** ✔.

## 5. Habilidade

1. Origem: marcos das trilhas de espécie e da Trilha do Parasita. Liberar custa EP.
2. **Ativas em 3 espaços** com tecla fixa; trocar o que está equipado só no grimório. **Passivas** liberadas
   valem sempre.
3. **Ativa custa mana + recarga**, os dois no JSON da habilidade.
4. **Graus:** um marco mais adiante na mesma trilha melhora a habilidade em vez de dar outra (Teia I → Teia
   II); cada grau custa EP.
5. **Dentro do hospedeiro as ativas ficam bloqueadas** até o jogador liberar "Tomar conta do corpo". Primeiro
   passageiro, depois piloto.
6. **Adaptação** é passiva com graus e vale para duas coisas: **possuir** as espécies que a exigem e
   **sobreviver no ambiente**. Sem a adaptação, o ambiente causa dano (decisões do Claude):
   - Só **calor** e **frio**. Quem decide é a **temperatura do bioma** (a mesma do vanilla) e a dimensão: o
     Nether inteiro é zona de calor do grau mais alto. Os limites de temperatura de cada grau ficam na config.
   - **Respirar debaixo d'água não é adaptação:** é efeito de gene (encaixe Respiração). Tê-lo nos dois
     sistemas quebraria a fronteira. O wisp sem corpo segue a regra vanilla de ar.
   - Dentro do hospedeiro o ambiente não afeta o wisp, que já é invulnerável lá dentro (M3c).
7. **Regra de alcance:** toda adaptação tem pelo menos uma fonte alcançável sem ela (ex.: Husk no deserto →
   Calor I; Cubo de magma no Nether → Calor II). Onde o vanilla não tiver fonte, um mob próprio do mod cobre.
   Deve ser validada por teste sobre o JSON.

**Abordagens consideradas:** tudo ligado (descartado: canivete suíço no fim do jogo); espaços também para
passivas (descartado: adaptação viraria exceção); custo em fome (descartado: a fome já paga exploração, genes e
casulo); só recarga (descartado: sem pressão). Impasse das adaptações: "o ambiente cobra aos poucos"
(descartado como regra principal); **graus** ✔.

## 6. Aparência

1. Cores e padrões vêm dos marcos das trilhas (ex.: abelha → âmbar e listras; slime → verde-limo e gradiente;
   cubo de magma → brasa e veios). Liberam de graça ao descobrir.
2. O wisp tem três partes: **núcleo, halo e rastro** (partículas), conforme o modelo A3 (V1).
3. **Padrão é um preset matemático** (listras, manchas, veios, gradiente, anéis…). O jogador escolhe o padrão e
   uma cor liberada para cada vaga de cor do padrão; o desenho é gerado e **não é editável**. Cada padrão
   declara quantas cores usa (ex.: gradiente 2, manchas 3).
4. **Semente por indivíduo:** mesmo padrão e mesmas cores dão wisps parecidos, não idênticos.
5. Só visual. A cor do wisp continua independente da paleta B da interface.
6. Técnica proposta: textura gerada em Java no cliente, calculada uma vez e guardada em cache; sem shader
   (compatível com mods de shader). Depende de como o modelo A3 separa núcleo, halo e rastro na textura.

## 7. Genes e metamorfose

### Fases

O jogador não fica wisp até o fim: passa por fases, cada uma alcançada por um casulo.

| Fase | Como se alimenta | Mobs | Encaixes de gene |
|---|---|---|---|
| Wisp | Possui por dentro, invisível; drena e consome | Não percebem (M3c) | — |
| Forma simples | Parasita **agarrada por fora** do hospedeiro (estilo facehugger); drena e consome | Veem e atacam | 2 |
| Juvenil | Caça: ataca e consome com o próprio corpo | Normal | 4 |
| Alfa | Caça; forma final livre | Normal | Todos |

### Genes

1. O corpo tem **encaixes por parte**: Locomoção, Cobertura, Respiração, Arma, Sentidos. Cada encaixe recebe um
   gene liberado; deixar vazio é permitido.
2. Cada gene tem **complexidade**; a soma define o **metabolismo** (multiplicador da fome) do corpo.
3. Cada gene é uma **peça do modelo GeckoLib**: o corpo é um boneco modular sobre uma base.
4. **Base do corpo:** casulo dentro de uma criatura → a base vem do hospedeiro (tamanho, postura, vida base;
   estilo Alien). Casulo como pupa no mundo → **base neutra** própria do wisp, mediana e sem especialidade.
   As bases são poucas posturas (quadrúpede, bípede, voador…), não uma por espécie.

### Casulo

1. **Requisito:** nível mínimo da fase (config) + **N genes liberados**, N = encaixes da fase (forma simples 2,
   juvenil 4).
2. **Custo:** fome e saturação cheias (a saturação é a "gordura"); a metamorfose consome as duas. Duração em
   config.
3. **Segurança vem do lugar:** a pupa pode ser atacada; o hospedeiro pode morrer. Casulo interrompido → o wisp
   sai, a fome gasta se perde, a receita (genes escolhidos) fica.

### Morte do corpo

O núcleo sempre sobrevive: o jogador volta a ser wisp no mesmo lugar, com o EP intacto (sem nuvem), e a
receita fica guardada. Um novo casulo restaura direto a última fase alcançada, com o custo normal de casulo.
São duas camadas de risco: o corpo funciona como uma vida extra (perdê-lo custa o casulo), e só a morte do
wisp custa o EP. Corpo morto gerando nuvem foi descartado: o wisp nasce em cima dela e a recolhe na hora, e
mandá-lo ao respawn contradiz "o núcleo sobrevive".

**Abordagens consideradas:** lista livre de genes (descartado: empilhamento sem escolha e corpo impossível de
modelar); hospedeiro só como incubadora (descartado: possessão perde o segundo propósito); requisito só de
nível (descartado: premia grind, não variedade); perder a fase ao morrer (descartado: punitivo demais);
renascer na mesma forma (descartado: o wisp deixa de ser o núcleo); pupa com a base do último hospedeiro
(descartado: tira o motivo de encasular dentro de uma criatura).

## 8. Grimório e HUD

### Abas do grimório (substituem Wisp, Traços, Aspectos, Habilidades, Bestiário da F5)

| Aba | Conteúdo | Fatia |
|---|---|---|
| **Wisp** | Nível, saldo de EP, **Subir nível** com o preço, atributos, vida, mana, fome | 1 |
| **Bestiário** | Espécies encontradas; trilha de cada uma com marcos (oculto, descoberto, liberado); **Liberar** com o preço; o que a espécie exige para possuir (nível, adaptações). Trilha do Parasita no topo | 2 |
| **Habilidades** | 3 espaços de ativas, ativas liberadas para equipar, passivas (só leitura) | 3 |
| **Aparência** | Wisp 3D, padrão e uma cor por vaga (núcleo, halo, rastro) | 4 |
| **Corpo** | Encaixes, genes, complexidade → fome; desativada até a fatia 5 | 5 |

### HUD

- **Barra de DNA:** saldo de EP em relação ao preço do próximo nível; brilha quando dá para comprar.
- **3 espaços de ativas** com tecla e recarga visível.
- **Mana dentro do hospedeiro:** a barra atual, piscando perto da expulsão.
- **Nuvem de DNA:** direção e distância enquanto houver nuvem não recuperada.
- **Aviso de descoberta** ao atingir um marco ("Descoberto: Teia I — libere no grimório").

Continuam valendo as regras da F4/F5: tudo em sprites PNG na paleta B, texto na fonte do jogo, nada com `fill`.

## Ordem de construção

**Fatias verticais** (mecânica + tela, testável no jogo a cada entrega), **arte adiantada em paralelo**
(pranchas e sprites de todas as páginas antes do código, como a A4).

| Fatia | Mecânica | Tela / HUD |
|---|---|---|
| 1. Fundação v2 | EP como moeda, nuvem de DNA, mana como sistema imune, regeneração de mana | Barra de DNA como saldo; "Subir nível" na aba Wisp |
| 2. Assimilação | Trilhas, marcos, descobrir × liberar, Trilha do Parasita, chave de adaptação na possessão | Aba Bestiário; aviso de descoberta |
| 3. Habilidades | 3 espaços, mana + recarga, graus, Teia, Calor I/II e dano de ambiente, Tomar conta do corpo | Espaços no HUD; aba Habilidades |
| 4. Aparência | Cores e padrões no wisp | Aba Aparência |
| 5. Metamorfose | Genes, casulo, forma simples (juvenil e alfa depois) | Aba Corpo |

Antes da fatia 1: mergear F4 e F5 (com a correção do estouro de `int` em `AllocationRules`); feito em 21/11.
Antes da fatia 4: validar no jogo o modelo A3, que já está na `main` desde 19/11 (versão arredondada); as fatias
4 e 5 dependem de como o A3 separa núcleo, halo e rastro.

Cada fatia vira uma spec própria (`docs/specs/`), uma de cada vez, como manda o AGENTS.md. O primeiro plano
cobre só a fatia 1 e o briefing de arte de todas as abas.

**Fatia 5, decidido por Eder em 21/11:** o jogador pode largar o corpo de propósito, e o corpo some (como a morte
do corpo: sem nuvem, receita guardada). A primeira entrega tem as posturas neutra e quadrúpede. **Ainda em aberto,
para quando a juvenil entrar:** como um jogador já com corpo faz casulo dentro de uma criatura.

## Mudanças na fundação

| Regra atual | Regra nova | Fatia |
|---|---|---|
| EP total acumula e deriva o nível (`LevelCurve`) | Nível guardado; EP é saldo gasto. Saves antigos migram no codec: nível pela curva antiga, o resto vira saldo | 1 |
| Morrer mantém tudo | Morte do wisp: EP não gasto vira nuvem de DNA (morte do corpo não gera nuvem) | 1 |
| Infestar custa `5 × nível do mob` de mana | Entrar é grátis com mana mínima; mana cai dentro do hospedeiro | 1 |
| Mana regenera só como wisp | Regenera em qualquer forma, menos dentro do hospedeiro | 1 |
| Infestar depende só do nível | Nível + adaptações exigidas pela espécie | 2 |
| Abas Traços e Aspectos no grimório | Bestiário, Habilidades, Aparência, Corpo | 2–5 |

## Restrições

- Minecraft 1.21.1, NeoForge 21.1.x, Java 21, GeckoLib 4.9.3 (stack fixa).
- Servidor decide tudo; cliente só renderiza e envia intenções. Estado em Data Attachments com sync manual.
- Regras de jogo em funções puras testadas com JUnit; managers só ligam ao jogo.
- Marcos, preços, exigências e efeitos em JSON de dados; números gerais em config.

## Deixado para quem implementa (nas specs)

- Conteúdo: espécies com trilha, marcos, cores, padrões, genes e habilidades de cada uma; a primeira leva de
  conteúdo de cada fatia é definida na spec dela.
- Números: preços em EP, mana mínima para entrar, curva do gasto de mana por diferença de nível, mana e recarga
  das ativas, dano de ambiente, duração do casulo, níveis mínimos das fases.
- O que "Tomar conta do corpo" controla (andar, atacar ou os dois).
- Formato da nuvem de DNA no mundo (entidade ou partículas) e como ela se comporta em lava, vazio e chunks
  descarregados.
- Organização de classes, payloads e attachments.
