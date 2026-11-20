# HUD na paleta B e Grimório (página do Wisp): design

**Data:** 20/11/2026. **Status:** aguardando aprovação de Eder.
**Pranchas de referência:** canvas "Menu do Wisp" (https://claude.ai/artifact/5GmZeBSHyhcLcBmzjpWvoW),
pranchas "HUD reposicionado na paleta B", "Identidade do HUD" e "Grimório · página do wisp (paleta B)".
**Base:** F4 (`feat/F4-hud`), com cartão do alvo no topo (opção B) e painel do hospedeiro no topo já
implementados.

## Por quê

O teste da F4 mostrou dois problemas: o HUD usava o ciano da arte A2, que lembra o Deep Dark (bioma do
Warden), e a disposição espalhava informação pela tela. Eder quer uma identidade visual própria do mod,
que não lembre nenhuma da Mojang, e reposicionou cada peça do HUD num rascunho. O grimório é a tela onde o
jogador cuida do wisp: vê o wisp, a vida, o EP e distribui os pontos de atributo com confirmação.

## Objetivos

1. Toda a interface do mod (HUD, cartões, selos, grimório) na **paleta B, Pergaminho e tinta**.
2. HUD inferior no **estilo atual** (núcleos, chamas, barras animadas, ícone de DNA), nas **posições do
   rascunho** de Eder.
3. **Grimório**, primeira página (Wisp): wisp em destaque, vida, mana, fome, nível, EP e distribuição de
   pontos com Cancelar e Confirmar.

## Não objetivos

- Páginas Traços, Aspectos, Habilidades e Bestiário do grimório: aparecem como abas desativadas. Cada uma
  terá design próprio.
- Cor do wisp variando com traços: depende do sistema de traços, que ainda não existe. A regra fica
  registrada: a cor do wisp é independente da paleta da interface.
- Mudanças de regra de jogo (custos, curvas, atributos). O grimório só expõe as regras atuais.

## Anti-objetivos (falha mesmo funcionando)

- Qualquer cor que lembre o Deep Dark (ciano/teal), o Nether (vermelho e verde-azulado), o End (roxo e
  amarelo) ou o cobre oxidado das Trial Chambers.
- Interface com cara de aplicativo web: cartões, etiquetas, painéis de estatística, fonte que não é a do jogo.
- Barra, ícone ou moldura desenhada por código (`fill`, gradiente). Continua valendo a regra de ouro da F4:
  tudo é sprite PNG, texto é a fonte do jogo.
- Distribuir pontos sem confirmação, ou o cliente decidir o resultado (o servidor valida).

## Abordagens consideradas

**Identidade visual** (decidida por Eder na prancha "Identidade do HUD"):
- A. Âmbar parasita: quente, legível à noite; dourado é o visual de RPG mais comum e o DNA verde-musgo
  fica perto da barra de XP.
- **B. Pergaminho e tinta (escolhida):** neutra, combina com qualquer cor de wisp e soa como grimório;
  menos "parasita", e a moldura clara chama um pouco mais de atenção à noite.
- C. Carne e osso: a mais única; o rosa compete com a vida e com o selo de fêmea.

**Disposição do HUD** (Eder): o rascunho define posições; a primeira versão com barras lisas foi
descartada porque as barras eram só referência de lugar. Duas larguras foram comparadas (352 e 200 px);
ficou a compacta, de 200 px.

**Painel do alvo** (Eder): opções faixa, cartão e coluna no topo; escolhido o cartão (B), já implementado.

## Paleta B (valores finais)

| Papel | Cor |
|---|---|
| Contorno escuro | `#120F0C` |
| Fundo das molduras | `#1D1915` (alfa 225) |
| Moldura (osso) / brilho do canto | `#CDBB94` / `#FFF3D6` |
| Texto / texto secundário | `#F3EAD6` / `#B5A88F` |
| Destaque (tinta ferrugem) | `#C4573A`, claro `#E2775F` |
| Trilho de barra / brilho do trilho / marcas | `#26211C` / `#3A332B` / `#4A4137` |
| Vida (núcleo) | `#4A1B16`, `#7D2A21`, `#B5402F`, `#E2775F`, `#F7CBB8` |
| Fome (chama) | `#6E5A2A`, `#C9A55A`, `#F2E2B0` |
| Vazio dos ícones | `#2B2621` (piscando: `#B5A88F`) |
| Mana | `#3F5AA8`, `#9FB4EE`, `#24346A`, onda `#8AA0E6`/`#6A80CC`, brilho `#D9E2FF` |
| DNA | `#A8976A`, `#FFF6D8`, `#6F644A`, hélice `#D9C99C` |
| Pode infestar / não pode | `#A8C46A` / `#D0563C` |
| Selos de sexo | inalterados (azul `#3D8FD6`, rosa `#D9628F`, símbolo branco) |

## HUD inferior (px de GUI, a partir do centro `cx` e da base `h`)

Largura total de 200 (`cx-100` a `cx+100`).

| Peça | Posição | Tamanho | Observação |
|---|---|---|---|
| Barra de EP (DNA, animada) | `cx-100`, `h-14` | 200 × 6 | Largura toda; sem caixa de nível |
| Núcleos de vida | a partir de `cx-100`, topo em `h-24` | 9 × 9, passo 8 | Mais de 10: segunda linha 10 px acima |
| Orbe de DNA | centrado em `cx`, topo em `h-34` | 19 × 19 | Ícone de DNA no meio |
| EP total | centrado em `cx`, acima do orbe (`h-43`) | fonte | Ex.: `8920`, cor `#FFF6D8` |
| Chamas da fome | de `cx+20` a `cx+100`, topo em `h-32` | 9 × 9, passo 8 | Esvazia da esquerda para a direita (como hoje) |
| Mana (animada) | `cx+20`, topo em `h-22` | 80 × 6 | Exatamente embaixo das chamas |
| Slot acima do orbe | centrado, base logo acima do EP total | — | Selo Consumir (HOST no piso) ou barra de conjuração (WISP conjurando) |
| Bolhas de ar | acima das chamas | vanilla | Só debaixo d'água (`Gui.rightHeight`) |

Sai do HUD: o número do nível e o "+N" de pontos de atributo (os dois ficam só no grimório).
Os valores exatos de Y são ponto de partida; quem implementa ajusta ±2 px comparando com a prancha.

## Topo (já implementado na F4, só recolorir)

- Cartão do alvo: nome; selo de sexo + nível; ícone + "Infestar" em verde ou vermelho. Moldura nine-slice.
- Painel do hospedeiro: nome, selo, nível, %, barra de vida com a marca do piso.
- Os dois descem abaixo das barras de boss.

## Grimório: página do Wisp

- **Abrir:** tecla configurável, padrão `K` (era a tecla planejada na M6). Só como parasita. `Esc` fecha.
- **Layout** (revisado em 20/11: Eder aprovou o layout compacto testado no mockup do Gemini, em
  `art/hud/mockup_grimoire.png`; barras de vida, mana e fome com 90 px e o valor à direita, sem legenda sob
  o `+N`). Cabeçalho com "Grimório" e as abas (Wisp ativa; as
  outras visíveis e desativadas). À esquerda, o wisp grande; à direita, nível, EP, vida, mana, fome, o
  contador de pontos e os quatro atributos.
- **Wisp:** o modelo 3D animado, renderizado como o boneco do jogador no inventário, girando devagar ou
  seguindo o mouse. Mostra a cor atual do wisp.
- **Pontos:** um contador grande `+N` ao lado do nível, em destaque (ferrugem) quando `N > 0` e apagado
  quando 0. Ele diminui a cada `+` clicado.
- **Atributos:** nome, 10 marcadores, efeito resultante e botões `−` e `+`. Pontos pendentes aparecem com
  outra cor (ferrugem com borda clara) e o efeito mostra o valor novo. `−` só remove pendentes.
- **Confirmar e Cancelar:** ativos só com algo pendente. Cancelar zera os pendentes. Confirmar envia um
  pedido ao servidor com a distribuição; o servidor valida (pontos disponíveis, máximo por atributo, forma
  parasita) e aplica tudo ou nada. Fechar a tela com pendentes descarta os pendentes.
- **Tudo em sprites:** molduras (nine-slice), marcadores, botões e contador são PNG; texto é a fonte do jogo.

## Restrições

- Minecraft 1.21.1, NeoForge 21.1.266; sprites no atlas de GUI (`wispborn:hud/*` e um novo
  `wispborn:grimoire/*`); pixel art gerada pelo `art/hud/src/generate_hud.py` (determinístico) e validada
  pelo `validate_hud.py`.
- Nada de `client/` no código comum; o servidor sobe sem cliente.
- O comando `/wispborn stat add` continua existindo para debug.

## Divisão do trabalho (proposta)

- **Arte (A4):** Codex (ChatGPT), estendendo o gerador da A2: recolorir tudo na paleta B, novo orbe de
  DNA, barras nos tamanhos novos (200 × 6 e 80 × 6), e o kit do grimório (moldura, abas, marcadores, botões,
  contador). Entrega mockups para Eder aprovar antes de integrar.
- **Código (F5):** Antigravity, a partir da spec F5: reposicionar o HUD, tecla e tela do grimório,
  distribuição de pontos confirmada por um payload novo, testes JUnit das regras puras (distribuição pendente, validação).
- A spec F5 traz a tabela de sprites com nomes e tamanhos, então código e arte podem andar em paralelo.

## Deixado para quem implementa

Nomes de classes além dos citados, organização das camadas, animação de abertura do grimório, posição
exata em escalas de GUI pequenas (sem sobrepor nada), e qualquer ajuste de ±2 px para alinhar com a prancha.
