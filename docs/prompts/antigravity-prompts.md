# Prompts para o Antigravity — M0 a M6

Use um prompt por vez, numa conversa nova do agente, com o repositório aberto como workspace.
Modo recomendado: **Planning** (ele mostra o plano antes de editar). Revise o plano antes de aprovar.

---

## Prompt 1 — M0 (setup)

```
Você é o implementador do mod Wispborn (NeoForge 1.21.1).

Leia por inteiro, nesta ordem, antes de qualquer edição:
1. AGENTS.md
2. .agents/rules/neoforge-1.21.1.md
3. docs/specs/M0-setup.md

Execute o workflow /implement-spec para a spec M0-setup.

Restrições:
- Faça somente o que está na spec M0. Nada de itens, blocos ou features.
- Busque a versão 21.1.x mais recente do NeoForge e do Parchment para 1.21.1 nas fontes
  indicadas na spec; não use números de memória.
- Ao final, rode ./gradlew build e ./gradlew test, e cole a saída final de ambos no
  "Relatório de implementação" da spec.
- Se algo falhar 3 vezes seguidas pelo mesmo motivo, pare e descreva o erro em
  "Dúvidas do implementador" em vez de tentar soluções aleatórias.
```

---

## Prompt 2 — M1 (forma wisp)

```
Implemente a spec docs/specs/M1-wisp-form.md do mod Wispborn.

Antes de editar, releia AGENTS.md e .agents/rules/neoforge-1.21.1.md. A seção PROIBIDO
das regras é obrigatória: se o seu código usar qualquer item dela, está errado.

Execute o workflow /implement-spec para M1-wisp-form.

Pontos críticos desta spec:
- Estado em Data Attachment (ModAttachments.FORM) com copyOnDeath. Sync é MANUAL via
  SyncFormPayload — em 1.21.1 não existe sync automático de attachment.
- Atributos aplicados com addOrReplacePermanentModifier e ids ResourceLocation; applyModifiers
  deve ser idempotente (chamar 2x não pode acumular).
- Tudo de render fica em client/ e não pode ser carregado no servidor dedicado. Confirme
  rodando ./gradlew runServer e verificando que não há crash.
- Gere as texturas placeholder (wisp_essence.png 16x16, wisp_core.png e wisp_halo.png 32x32) com um script
  simples; não deixe referência a textura inexistente.

Ao terminar: ./gradlew build, ./gradlew test e ./gradlew runServer (até "Done") sem erro.
Preencha o "Relatório de implementação" e abra o PR com o checklist manual da spec no corpo.
Não faça merge.
```

---

## Prompt 2b — M1b (modelo e animações do wisp com GeckoLib)

Só depois do merge do M1 na `main`.

```
Implemente a spec docs/specs/M1b-wisp-model.md do mod Wispborn.

Antes de editar, releia AGENTS.md, .agents/rules/neoforge-1.21.1.md, .agents/rules/geckolib-4.9.md
e docs/ART_PIPELINE.md. As seções PROIBIDO das duas regras são obrigatórias. O GeckoLib muda muito
de API entre versões: use só o que existe na 4.9.3 (se não achar no jar, pare e escreva a dúvida).

Execute o workflow /implement-spec para M1b-wisp-model.

Pontos críticos desta spec:
- Faça PRIMEIRO a tarefa 0 (prova de conceito: modelo + idle no lugar do jogador). Se falhar,
  pare e registre em "Dúvidas do implementador" com log/print, sem tentar contornar com outra lib.
- WispAnimatable e WispAnimations são código comum: nada de import de client/.
  Renderer e modelo ficam em client/. Confirme com ./gradlew runServer.
- Copie os assets de art/parasita-magico/ sem editar o conteúdo e remova wisp_core.png/wisp_halo.png.
- Não use withScale para compensar a hitbox: o renderer já aplica o atributo SCALE.

Ao terminar: ./gradlew build, ./gradlew test e ./gradlew runServer (até "Done") sem erro.
Preencha o "Relatório de implementação" e abra o PR com o checklist manual da spec no corpo.
Não faça merge.
```

---

## Prompts 3 a 7 — M2 a M6 (mesmo modelo)

Troque `<SPEC>` por `M2-level-stats`, `M3-possession`, `M4-drain`, `M5-consume` ou `M6-stats-screen`, sempre em ordem
e só depois do merge do anterior.

```
Implemente a spec docs/specs/<SPEC>.md do mod Wispborn.

Antes de editar, releia AGENTS.md e .agents/rules/neoforge-1.21.1.md (a seção PROIBIDO é obrigatória)
e leia o código já existente dos módulos anteriores: reutilize FormManager, ProgressManager,
HostManager, StatMath e WispbornConfig em vez de criar paralelos.

Execute o workflow /implement-spec para <SPEC>.

Regras deste projeto que mais quebram:
- Regras de jogo em funções puras (*Rules, *Math, LevelCurve) com teste JUnit; managers só chamam.
- Nenhum número de balanceamento hardcoded: tudo vem de WispbornConfig.
- Config SERVER não pode ser lida em static init nem nos testes.
- Nada de client/ referenciado por código comum; ./gradlew runServer precisa subir sem crash.

Ao terminar: ./gradlew build, ./gradlew test e ./gradlew runServer (até "Done") sem erro.
Preencha o "Relatório de implementação" e abra o PR com o checklist manual da spec no corpo.
Não faça merge.
```

---

## Depois de cada PR

Mande o link do PR (ou o diff) para o Claude revisar, com a frase:
`Revise o PR <link> contra a spec <id>.`
