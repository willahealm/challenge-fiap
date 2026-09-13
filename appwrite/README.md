# Configuração do Appwrite

Projeto de demonstração informado: `6aa6f5a70039265e6516`, endpoint `https://fra.cloud.appwrite.io/v1`.

Na inspeção de 13/09/2026, o console mostrava: nenhum app conectado, nenhuma API key, nenhum usuário, nenhum database e onboarding 0/14.

## Ordem segura de configuração

1. Renomear o projeto de "My first project" para um nome do produto.
2. Registrar apps Web separados para dashboard e totem, com hostnames locais e dos Sites.
3. Registrar app Android com o application ID definitivo.
4. Ativar e-mail/senha e revisar configurações de sessão.
5. Criar team `operators`.
6. Criar database `embarque` e tabelas/coleções descritas em `docs/04-arquitetura.md`, incluindo `totems`, `handoff_sessions` e `help_requests`.
7. Criar bucket privado `terminal-assets` para mapas, imagens e áudios.
8. Configurar permissões e índices antes do seed.
9. Configurar Messaging com o provedor push do Android.
10. Criar Functions em Java para expiração, notificações e métricas.
11. Configurar Sites separados para dashboard e totem.
12. Criar uma chave de servidor restrita ao mínimo necessário para Spring Boot e chaves específicas para Functions.
13. Guardar secrets apenas no ambiente local/hosting; nunca commitar.
14. Rodar testes de isolamento com dois passageiros, um operador e um totem.

## Variáveis planejadas

```dotenv
APPWRITE_ENDPOINT=https://fra.cloud.appwrite.io/v1
APPWRITE_PROJECT_ID=6aa6f5a70039265e6516
APPWRITE_DATABASE_ID=embarque
APPWRITE_API_KEY=server-only-secret
APPWRITE_OPERATORS_TEAM_ID=operators
APPWRITE_STORAGE_BUCKET_ID=terminal-assets
```

O dashboard e o totem usam somente endpoint e project ID com prefixo público do Vite. O Android Java recebe os mesmos valores por configuração de build. `APPWRITE_API_KEY` existe apenas no Spring Boot e em Functions com escopo mínimo.

O backend Spring Boot consome a API REST do Appwrite diretamente. O aplicativo Android também usa um adapter Java sobre REST/WebSocket, evitando exigir Kotlin apenas para integrar o SDK Android orientado a coroutines.

## Uso dos produtos Appwrite

| Produto | Papel no MVP |
|---|---|
| Auth/Account | login, sessão, recuperação e logout |
| Users | contas demo administradas pela API Java |
| Teams | autorização dos operadores |
| Database | dados transacionais da jornada |
| Realtime | sincronização entre as três superfícies |
| Storage | mapas, marcos e áudio acessível |
| Messaging | push de alertas críticos no Android |
| Functions (Java) | expiração, fan-out e agregações |
| Sites | hospedagem do dashboard e totem |
| Activity/Usage | diagnóstico e evidências do pitch |

## Testes de permissão obrigatórios

- Passageiro A não lê viagem/jornada/feedback do Passageiro B.
- Passageiro não cria alertas nem altera plataforma.
- Operador autenticado publica alerta pela API.
- Usuário anônimo não acessa dados da operação.
- Realtime entrega apenas eventos que a sessão tem permissão para ler.
- Totem não consegue listar jornadas e um handoff expirado/consumido falha.
- Encerrar ou expirar a sessão do totem remove o contexto visível.

## Não fazer pelo console durante o pitch

Todas as mudanças da demonstração devem sair do dashboard. O console serve para configuração e diagnóstico, não compõe a experiência do produto.
