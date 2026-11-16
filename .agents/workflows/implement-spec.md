---
description: Implementar uma spec do Wispborn de ponta a ponta
---

# /implement-spec

Uso: `/implement-spec M1-wisp-form`

1. Leia `AGENTS.md`, `.agents/rules/neoforge-1.21.1.md` e `docs/specs/<spec>.md` por inteiro.
2. Confirme que a working tree está limpa (`git status`). Se não estiver, pare e avise.
3. Crie a branch `feat/<spec>` a partir de `main`.
4. Gere um plano curto (lista de arquivos a criar/alterar) e siga a ordem de tarefas da spec.
5. Implemente. Compile cedo e com frequência: `./gradlew compileJava`.
6. Rode `./gradlew build` (e `./gradlew test` se a spec tiver testes). Corrija até ficar verde.
7. Rode `./gradlew runData` se a spec mexer em lang/modelos/tags e commite os arquivos gerados.
8. Preencha "Relatório de implementação" na spec (arquivos, decisões, pendências, dúvidas).
9. Commits pequenos em português (`feat(form): ...`, `fix(net): ...`). Push e abra PR para `main`
   com o checklist de testes manuais da spec no corpo.
10. Não faça merge. Avise que está pronto para revisão.
