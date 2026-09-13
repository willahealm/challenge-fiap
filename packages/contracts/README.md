# Contratos Embarque v1

- `openapi.json`: OpenAPI 3.1, endpoints P0, autenticação e auxiliares da demo.
- `schemas/v1.schema.json`: JSON Schema 2020-12 com modelos em `$defs`.
- `index.ts`: tipos wire, disponíveis como `@embarque/contracts` (somente importações de tipos).

Implementação: `apps/api/src/main/java/br/com/fiap/embarque/Models.java` e `ApiController.java`. Mudanças de formato devem atualizar estes três artefatos e testes juntos. O contrato descreve a demo em memória; não promete Appwrite ativo.

Base: `http://localhost:8080`. JSON, timestamps ISO-8601 UTC e `Authorization: Bearer <accessToken>`. Listagens de operação retornam arrays diretos. Visão da jornada: `{ journey, trip, route, alerts }`. Sucessos usam 200, incluindo POST/PATCH/DELETE. Erros de negócio: `{code,message,timestamp}`. Não renovar KIOSK por polling; `/v1/auth/session/touch` só para atividade humana.

Consulte `apps/api/README.md` para contas, enums, seed, reset e limitações. Sincronização local: consulte a visão a cada dois segundos; em 401, remova o bearer e solicite nova sessão.
