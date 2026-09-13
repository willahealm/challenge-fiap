# Configuração do Appwrite

Projeto de demonstração informado: `6aa6f5a70039265e6516`, endpoint `https://fra.cloud.appwrite.io/v1`.

Na inspeção de 13/09/2026, o console mostrava: nenhum app conectado, nenhuma API key, nenhum usuário, nenhum database e onboarding 0/14.

## Ordem segura de configuração

1. Renomear o projeto de "My first project" para um nome do produto.
2. Registrar app Web do dashboard com hostname local e do deploy.
3. Registrar app Android com o application ID definitivo.
4. Ativar e-mail/senha e revisar configurações de sessão.
5. Criar team `operators`.
6. Criar database `embarque` e tabelas/coleções descritas em `docs/04-arquitetura.md`, incluindo `totems`, `handoff_sessions` e `help_requests`.
7. Configurar permissões e índices antes do seed.
8. Criar uma chave de servidor restrita ao mínimo necessário para a API.
9. Guardar secrets apenas no ambiente local/hosting; nunca commitar.
10. Rodar testes de isolamento com dois passageiros e um operador.

## Variáveis planejadas

```dotenv
APPWRITE_ENDPOINT=https://fra.cloud.appwrite.io/v1
APPWRITE_PROJECT_ID=6aa6f5a70039265e6516
APPWRITE_DATABASE_ID=embarque
APPWRITE_API_KEY=server-only-secret
APPWRITE_OPERATORS_TEAM_ID=operators
```

O dashboard usa somente endpoint e project ID com prefixo público do Vite. O Android recebe os mesmos dois valores via configuração de build. `APPWRITE_API_KEY` existe apenas na API Express.

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
