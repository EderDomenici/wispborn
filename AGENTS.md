# AGENTS.md — Wispborn

Regras do projeto para qualquer agente (Antigravity, Claude Code, Cursor).
Regras específicas de API ficam em `.agents/rules/neoforge-1.21.1.md` e `.agents/rules/geckolib-4.9.md`
— leia antes de escrever código.

## O que é
Mod de Minecraft em que o jogador é um **wisp** (orbe parasita): o nível define quais mobs ele
infecta; dentro do hospedeiro (passageiro invisível, sem controle) drena a vida e ganha EP; com o
hospedeiro em ≤20% de vida pode consumi-lo. Subir de nível dá pontos para 4 atributos do parasita
(Absorção, Eficiência, Reserva de mana, Vitalidade); infestar custa mana. Plano: `docs/PLAN.md`.
Specs: `docs/specs/M0..M6`.
Para modelos, animações, efeitos e estruturas, consulte [o guia de produção de arte](docs/ART_PIPELINE.md).

## Stack (fixa — não mudar sem pedido explícito)
- Minecraft **1.21.1**, NeoForge **21.1.x**, Java **21**
- Gradle com **ModDevGradle** (template `NeoForgeMDKs/MDK-1.21.1-ModDevGradle`)
- Mappings oficiais Mojang + Parchment
- **GeckoLib 4.9.3** (`geckolib-neoforge-1.21.1`) para modelos e animações de criaturas
  (dependência obrigatória também para os jogadores)
- modid `wispborn`, pacote raiz `dev.eder.wispborn`

## Papéis
- **Claude**: escreve specs (`docs/specs/`), revisa diffs, debuga.
- **Antigravity**: implementa uma spec por vez, roda build e testes, abre PR.
- **Eder**: testa no jogo e aprova merges.

## Regras de trabalho
1. Trabalhe **apenas** na spec indicada. Nada fora do escopo dela, nem "melhorias" extras.
2. Uma branch por spec: `feat/<id-da-spec>` (ex.: `feat/M1-wisp-form`).
3. Se algo na spec for impossível ou ambíguo, **pare e escreva a dúvida** em
   `docs/specs/<spec>.md` na seção "Dúvidas do implementador". Não invente API.
4. Antes de entregar: `./gradlew build` verde (e `./gradlew test` quando a spec pedir testes).
5. Ao final, preencha a seção "Relatório de implementação" da spec: arquivos criados,
   decisões tomadas, o que não foi feito.

## Arquitetura (resumo)
- **Servidor decide tudo.** O client só renderiza e envia intenções (keybinds) via payload.
- Estado do jogador em **Data Attachments**; em 1.21.1 **não há sync automático** →
  sync manual por `CustomPacketPayload`.
- Tiers dos mobs por **tags de entidade**; números de balanceamento em **config** (nunca hardcoded).
- Regras de jogo em funções puras (`*Rules`, `*Math`, `LevelCurve`) testadas com JUnit; os managers só ligam essas funções ao jogo.

## Estrutura de pacotes
```
dev.eder.wispborn
├── WispbornMod            # @Mod(IEventBus, ModContainer): DeferredRegisters + config
├── config/                # WispbornConfig (ModConfigSpec SERVER)
├── registry/              # ModAttachments, ModItems, ModTags, ModDamageTypes
├── form/                  # FormType, FormState, FormManager, WispTuning, WispAnimatable, WispAnimations
├── progress/              # WispProgress, WispStats, Stat, StatView, LevelCurve, StatMath, TierResolver, ProgressManager
├── host/                  # HostManager, InfestRules, DrainMath, ConsumeRules
├── network/               # SyncFormPayload, ConsumeRequestPayload, SyncProgressPayload, AllocateStatPayload
├── event/                 # FormEvents, WispRestrictions, HostEvents (NeoForge.EVENT_BUS)
├── command/               # WispbornCommand (debug)
└── client/                # WispRenderHandler, WispModel, WispPlayerRenderer, KeyBindings, StatsScreen, ClientProgress (só Dist.CLIENT)
```
Arte-fonte (Blockbench) fica em `art/<criatura>/`; os arquivos usados pelo jogo são copiados para `assets/wispborn/`.

## Convenções
- Records + `Codec`/`StreamCodec` para todo estado serializável.
- Ids sempre com `ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "...")`.
- Classes em `client/` nunca são referenciadas por código comum (crash no servidor dedicado).
- Textos visíveis via lang (`assets/wispborn/lang/en_us.json` e `pt_br.json`), nunca hardcoded.
- Comentários e commits em português; nomes de código em inglês.
