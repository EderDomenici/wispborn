# AGENTS.md — Wispborn

Regras do projeto para qualquer agente (Antigravity, Claude Code, Cursor).
Regras específicas de API ficam em `.agents/rules/neoforge-1.21.1.md` e `.agents/rules/geckolib-4.9.md`
— leia antes de escrever código.

## O que é
Mod de Minecraft em que o jogador **nasce wisp** (orbe parasita) e renasce wisp ao morrer. Explorar gasta
fome; só parasitando ele se alimenta. Todo mob vanilla tem **nível e sexo** (traços sorteados ao nascer):
o nível do wisp define quais mobs ele infecta e presas fracas demais não dão EP. Dentro do hospedeiro
(passageiro invisível, sem controle) drena a vida e ganha EP e fome; com o hospedeiro em ≤20% de vida
pode consumi-lo. Subir de nível dá pontos para 4 atributos do parasita (Absorção, Eficiência, Reserva de
mana, Vitalidade); infestar custa mana. HUD próprio em sprites substitui o vanilla.
Plano: `docs/PLAN.md`. Design atual: `docs/superpowers/specs/2026-11-17-fundacao-design.md`.
Specs: `docs/specs/` (M0–M3c e F1–F5 feitas; M4/M5 substituídas pela F3; M6 substituída pela F5). Fagogenia: design `docs/superpowers/specs/2026-11-20-fagogenia-design.md`, planos F6–F10 em `docs/superpowers/plans/`.
Para modelos, animações, efeitos, estruturas e sprites, consulte [o guia de produção de arte](docs/ART_PIPELINE.md).

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
- Faixas de nível dos mobs em **JSON de dados** (`data/wispborn/wisp_mob_levels/`); números de balanceamento em **config** (nunca hardcoded).
- HUD: **só sprites PNG** (`textures/gui/sprites/hud/`) e glifos da fonte do mod; nada de barra ou ícone desenhado com `fill`.
- Regras de jogo em funções puras (`*Rules`, `*Math`, `LevelCurve`) testadas com JUnit; os managers só ligam essas funções ao jogo.

## Estrutura de pacotes
```
dev.eder.wispborn
├── WispbornMod            # @Mod(IEventBus, ModContainer): DeferredRegisters + config
├── config/                # WispbornConfig (SERVER) + config CLIENT
├── registry/              # ModAttachments, ModTags, ModDamageTypes, ModSounds
├── form/                  # FormType, FormState, FormManager, HungerMath, WispTuning, WispAnimatable, WispAnimations
├── progress/              # WispProgress, WispStats, Stat, LevelCurve, StatMath, EpMath, ProgressManager
├── traits/                # Sex, MobTraits, MobLevelRange, MobLevelTable, MobTraitRoller, BreedingRules, MobTraitsManager
├── host/                  # HostManager, InfestRules, CastRules, DrainMath, ConsumeRules
├── network/               # payloads (SyncForm, TargetInfo, CastState, Possess*, SyncMobTraits, SyncWispState, ConsumeRequest)
├── event/                 # FormEvents, WispRestrictions, HostEvents, TraitEvents (NeoForge.EVENT_BUS)
├── mixin/                 # só com spec pedindo (F2: BreedGoalMixin, AnimalMakeLoveMixin)
├── command/               # WispbornCommand (debug)
└── client/                # renderers, KeyBindings, ClientPossession, ClientWispState, hud/ (só Dist.CLIENT)
```
Arte-fonte (Blockbench) fica em `art/<criatura>/`; os arquivos usados pelo jogo são copiados para `assets/wispborn/`.

## Convenções
- Records + `Codec`/`StreamCodec` para todo estado serializável.
- Ids sempre com `ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "...")`.
- Classes em `client/` nunca são referenciadas por código comum (crash no servidor dedicado).
- Textos visíveis via lang (`assets/wispborn/lang/en_us.json` e `pt_br.json`), nunca hardcoded.
- Comentários e commits em português; nomes de código em inglês.
