# API Embarque — demo local executável

Java 21+ (testado com JDK 25), Spring Boot 3.5.16 e Gradle Wrapper 9.1.0. Todas as operações P0 de `docs/04-arquitetura.md` funcionam sem conta, chave ou serviço externo. Dados fictícios ficam em memória e são perdidos ao reiniciar. O perfil padrão é `demo`, restrito a loopback (`127.0.0.1`) por padrão.

## Executar e verificar

No PowerShell, a partir de `apps/api`:

```powershell
.\gradlew.bat test bootJar --console=plain
.\gradlew.bat bootRun --console=plain
```

Em Linux/macOS: `sh ./gradlew test bootJar` e `sh ./gradlew bootRun`. O primeiro build baixa Gradle e dependências. JAR: `build/libs/embarque-api-0.1.0.jar`, executado com `java -jar build/libs/embarque-api-0.1.0.jar`. Health: `http://localhost:8080/actuator/health`.

Com a API em execução, `./scripts/smoke.ps1` verifica a jornada HTTP. O smoke reseta todos os dados e sessões no início e no final; use numa instância dedicada antes do pitch.

Se OneDrive bloquear remoção de arquivos de relatório no Windows, use saída fora da pasta sincronizada: `.\gradlew.bat test bootJar "-PbuildOutput=$env:LOCALAPPDATA/Embarque/api-build"`. JAR e relatórios passam a ficar nessa pasta. Não é necessário apagar arquivos do projeto.

`PORT` muda a porta. `CORS_ORIGINS` recebe origens explícitas separadas por vírgulas; o padrão aceita localhost e 127.0.0.1 nas portas 5173, 5174 e 5175. Para dispositivo físico em uma rede de demonstração controlada, `API_BIND_ADDRESS=0.0.0.0` permite conexões da rede. Esse perfil possui contas públicas e nunca deve ser exposto como produção. Spring não carrega `.env` automaticamente: exporte as variáveis no shell.

## Contas públicas fictícias

| Papel | E-mail | Senha de demonstração |
|---|---|---|
| Passageiro Lucas | `lucas@demo.local` | `Demo123!` |
| Passageira Ana (isolamento) | `ana@demo.local` | `Demo123!` |
| Operador | `operador@demo.local` | `Operador123!` |
| Identidade técnica de totem | `totem@demo.local` | `Totem123!` |

Não são segredos nem contas Appwrite. O servidor armazena hashes BCrypt das senhas, não permite escolher papel no login e emite tokens opacos aleatórios de 256 bits, armazenados somente como hash SHA-256. Login dura duas horas, com falhas genéricas e limite de 20 tentativas por IP/minuto. Nenhum token vai para URL ou log da aplicação.

`POST /v1/auth/demo` recebe `{ "email": "lucas@demo.local", "password": "Demo123!" }` e retorna `{ accessToken, tokenType, expiresAt, user: { id, name, role } }`. Demais chamadas usam `Authorization: Bearer <accessToken>`. `DELETE /v1/auth/session` revoga a sessão atual. HTTP stateless, sem cookies de autenticação; CSRF está desativado por esse motivo. Respostas autenticadas usam `Cache-Control: no-store`.

## Jornada e contratos

OpenAPI em `../../packages/contracts/openapi.json`; schemas JSON e tipos TypeScript ficam na mesma pasta. Enums são sensíveis a maiúsculas. JSON com campos desconhecidos é rejeitado. Erros de negócio retornam `{ code, message, timestamp }`: 400 validação, 401 sessão, 403 escopo, 404 recurso, 409 estado, 410 handoff indisponível, 429 limite de tentativas. Horários são ISO-8601 UTC.

Seed: `trip-demo`, `journey-demo`, `user-lucas`, `terminal-tiete`, `totem-tiete-01`. Viagem São Paulo/Tietê → Rio/Novo Rio, plataforma 18, amanhã às 18h30 em `America/Sao_Paulo`, viação e estimativas fictícias. A segunda viagem (`trip-ana`, `journey-ana`) comprova isolamento.

1. Passageiro chama `GET /v1/me/trips/next` e `GET /v1/trips/trip-demo/journey`. Visão consolidada: `{journey, trip, route, alerts}`.
2. `PATCH /v1/journeys/journey-demo/checklist` recebe `{ "checklist": { "document": true } }`. Chaves: `document`, `ticket`, `luggage`, `departureTime`.
3. `POST /v1/journeys/journey-demo/checkpoints` recebe `{ "code": "TIETE-TOTEM-01" }`. Também: `TIETE-ENTRADA`, `TIETE-SETOR-B`, `TIETE-P18`, `TIETE-P21`. `RIO-ENTRADA` demonstra rejeição de outro terminal. `GET /v1/terminal/points` lista pontos de Tietê.
4. `POST /v1/journeys/journey-demo/handoffs` retorna `{token, code, expiresAt}`. QR carrega token opaco; `code` é fallback manual de seis caracteres. Ambos expiram em 60s, são armazenados somente como hash e representam a mesma autorização. Novo handoff revoga o anterior. Limite: 10 emissões/passageiro/minuto.
5. TOTEM chama `POST /v1/totems/totem-tiete-01/handoffs/consume` com `{ "token": "<token ou code>" }`. Consumo atômico invalida ambos os aliases e confirma ponto físico, com um único vencedor mesmo em chamadas concorrentes. Limite: 10 tentativas/dispositivo/minuto. Retorna visão consolidada, `accessToken` KIOSK e `expiresAt`.
6. KIOSK só lê viagem vinculada, pede ajuda contextual e conclui. Expira em 30s. `POST /v1/auth/session/touch` renova mais 30s por interação humana, retorna `{expiresAt}`, até limite absoluto de 5min. Polling não renova. Cliente deve apagar dados visíveis em 30s de inatividade, revogar token em Encerrar e voltar à home em 401.
7. Operador chama `POST /v1/ops/trips/trip-demo/alerts` com `{ "type": "PLATFORM_CHANGE", "severity": "CRITICAL", "message": "Embarque transferido para a plataforma 21.", "platform": "21" }`. Viagem, rota e alerta mudam sob mesmo lock. Plataformas curadas: 18 e 21. Repetir plataforma retorna 409. Outros tipos: `INFO`, `DELAY`, `BOARDING`; severidades: `INFO`, `WARNING`, `CRITICAL`.
8. KIOSK chama `POST /v1/totems/totem-tiete-01/help-requests` com `{ "category": "MOBILITY" }`; API inclui jornada, viagem, totem e ponto. Categorias: `MOBILITY`, `VISION_HEARING`, `TRIP_INFO`, `SECURITY`. TOTEM pede ajuda geral sem jornada, mas não vincula jornada arbitrária. Operador consulta `GET /v1/ops/help-requests` e atualiza `PATCH /v1/ops/help-requests/:id` com `{ "status": "ACKNOWLEDGED" }` ou `RESOLVED`.
9. Passageiro ou KIOSK chama `POST /v1/journeys/journey-demo/complete`, confirmação humana de chegada idempotente. Passageiro envia `POST /v1/feedback` com `{ "journeyId": "journey-demo", "rating": 5, "tags": ["orientação"], "comment": "Encontrei a plataforma" }`. Exige conclusão; avaliação repetida substitui a anterior.

Operador vê viagens operacionais e chamados, sem listagem nominal. Passageiro não publica alertas. TOTEM não lista viagens/jornadas; KIOSK não acessa outra jornada.

## Sincronização e reset

Clientes consultam a visão consolidada e chamados a cada dois segundos, com botão Atualizar como fallback. Não há WebSocket, Appwrite Realtime, push, Messaging, Storage ou Functions ativos. Rotas são instruções estáticas curadas, não GPS.

`POST /v1/demo/reset`, autenticado como OPERATOR, limpa alertas, chamados, feedback, handoffs, checklist e estágios; restaura plataforma 18 e revoga **todas** as sessões, inclusive a do operador. Retorna `{status: "RESET", tripId: "trip-demo", journeyId: "journey-demo"}`. É idempotente no estado dos dados; cada nova chamada precisa de nova sessão do operador. Reiniciar também executa seed. Reset mantém limites de tentativas para evitar contorno; aguarde a janela de um minuto se necessário.

## Perfis Appwrite e produção

Somente `demo` está implementado. `SPRING_PROFILES_ACTIVE=appwrite` ou `production` falha explicitamente; combinar esses perfis com `demo` também é recusado. Configurar secrets não habilita persistência automaticamente.

Próxima etapa: manter contratos e extrair adapters de autenticação e persistência:

| Adapter planejado | Responsabilidade / critério de habilitação |
|---|---|
| `AppwriteAccountGateway` | Validar sessão/JWT via Account e membership confirmado em `operators` via Teams; nunca aceitar role do cliente. |
| `AppwriteDatabaseGateway` | Persistir modelos e índices da arquitetura; garantir consumo único e atualização viagem+alerta por transação/compare-and-set server-side. Lock JVM não protege múltiplas instâncias. |
| `AppwriteStorageGateway` | Assets privados e permissões por recurso; nenhuma API key em cliente. |
| `AppwriteMessagingGateway` | Fan-out/push idempotentes após commit, com provedor configurado; falha de entrega não reverte troca confirmada. |

Variáveis planejadas: `APPWRITE_ENDPOINT`, `APPWRITE_PROJECT_ID`, `APPWRITE_DATABASE_ID`, `APPWRITE_API_KEY` (somente servidor), `APPWRITE_OPERATORS_TEAM_ID`, `APPWRITE_STORAGE_BUCKET_ID`. Ambiente, coleções e permissões: `../../appwrite/README.md`. Não há client REST fictício nem SDK não oficial.

Antes de habilitar integração: validar sessão, isolamento de dois usuários, membership, revogação, consumo concorrente entre instâncias, consistência plataforma/alerta, permissões Realtime e reconexão. Contas públicas e reset devem continuar exclusivos da demo.

## Evidências

JUnit e MockMvc cobrem fluxo HTTP completo, login/logout, papéis/propriedade, JSON inválido, CORS, hash/expiração/aliases do handoff, oito consumos simultâneos com um vencedor, expiração/renovação KIOSK, limites, reset e feedback único. Relatório: `build/reports/tests/test/index.html`.

Fontes oficiais das versões: [Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html) e [Gradle 9.1 / Java 25](https://docs.gradle.org/9.1.0/release-notes.html). Wrapper verifica o SHA-256 oficial da distribuição.
