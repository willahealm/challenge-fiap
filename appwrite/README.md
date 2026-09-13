# Appwrite Cloud — MVP demonstrável

Projeto `6aa6f5a70039265e6516`, endpoint `https://fra.cloud.appwrite.io/v1`.

## Recursos ativos

| Recurso | ID | Uso |
|---|---|---|
| Database | `embarque` | persistência do estado demo |
| Collection | `mvp_state` | documento privado `demo` |
| Function Node 22 | `embarque-api` | API, sessão demo e regras do MVP |
| Site | `embarque-dashboard` | painel operacional React |
| Site | `embarque-totem` | experiência kiosk React |

A chave de implantação `codex-deploy-fiap` expira em 12/12/2026 e possui somente escopos de Database, Functions e Sites. O valor é secreto, não está versionado e nunca deve ser enviado aos clientes.

## Autenticação desta entrega

A autenticação demo está deliberadamente dentro da Function para que o pitch seja reproduzível no plano gratuito. Ela emite tokens opacos temporários, valida papel no servidor e mantém sessões no documento privado. Credenciais fictícias:

- operador: `operador@demo.local` / `Operador123!`;
- passageiro: `lucas@demo.local` / `Demo123!`;
- identidade do totem: `totem@demo.local` / `Totem123!`.

Isto é demonstrável, mas não é o desenho de produção. O próximo incremento deve migrar usuários reais para Appwrite Auth, separar documentos por usuário/equipe e aplicar permissões por recurso.

## Configuração pública dos Sites

```dotenv
VITE_APPWRITE_ENDPOINT=https://fra.cloud.appwrite.io/v1
VITE_APPWRITE_PROJECT_ID=6aa6f5a70039265e6516
VITE_APPWRITE_FUNCTION_ID=embarque-api
VITE_TOTEM_ID=totem-tiete-01
```

O navegador cria execuções síncronas da Function e converte `responseBody`, status e headers de volta para o contrato HTTP normal. Nenhuma API key aparece no bundle.
