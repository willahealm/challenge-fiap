# Arquitetura

## Responsabilidades

```text
Android (passageiro) ─┐
React Totem (kiosk) ──┼─ Appwrite Auth + Realtime
React (operador) ─────┘
        │
        └──── HTTPS ─── Java API ────── Appwrite (credencial de servidor)
                                      ├─ dados
                                      ├─ storage
                                      └─ messaging (P1)
```

## Sequência da jornada entre superfícies

```mermaid
sequenceDiagram
    actor P as Passageiro
    participant M as Android
    participant T as Totem
    participant A as Java API
    participant W as Appwrite
    participant D as Dashboard
    P->>M: Abre a jornada
    M->>A: Solicita handoff temporário
    A->>W: Salva hash + expiração
    A-->>M: QR de uso único (60 s)
    P->>T: Apresenta QR
    T->>A: Consome handoff + totemId
    A->>W: Confirma checkpoint físico
    W-->>M: Realtime atualiza a posição
    T-->>P: Mostra rota e acessibilidade
    M-->>P: Seta aponta o próximo marco
    D->>A: Altera plataforma
    A->>W: Atualiza viagem + cria alerta
    W-->>M: Realtime recompõe rota
    W-->>T: Realtime recompõe rota
```

### Android

Java, Android SDK, Activities/Fragments, layouts XML, ViewModel/LiveData, CameraX + leitor QR e sensores Android (`TYPE_ROTATION_VECTOR`, com fallback de magnetômetro/acelerômetro). Um adapter Java usa REST para Account/Database/Storage/Messaging e OkHttp WebSocket para Realtime, mantendo sessão/cookies em armazenamento seguro. O app chama a API Java com a sessão do usuário.

### Dashboard

React + TypeScript + Vite, React Router e TanStack Query. Usa o SDK Web para login/sessão, Realtime e arquivos permitidos. Mutações administrativas passam pela API Java.

### Totem

React + TypeScript + Vite em PWA/kiosk. Usa identidade técnica limitada por dispositivo e sessões de jornada efêmeras emitidas pela API. Não armazena credenciais administrativas, PII ou token da jornada após timeout. Um `TOTEM_ID` identifica o ponto físico e permite que o checkpoint seja confirmado automaticamente.

### API Java

Java LTS, Spring Boot, Gradle, Bean Validation, Spring Security e `RestClient`/`WebClient` para a API REST do Appwrite. Camadas: controllers → filtros de autenticação → services → repositories Appwrite. A API valida a sessão recebida, verifica membership no time `operators`, aplica regras transacionais de negócio e expõe contrato OpenAPI.

### Appwrite

- **Auth/Account:** cadastro, e-mail/senha, sessão, recuperação de senha e logout.
- **Users:** administração server-side de contas de demonstração pela API Java.
- **Teams:** `operators` e papéis para autorização do dashboard.
- **Database:** fonte de verdade para viagens, jornadas, rotas, totens, alertas e feedback.
- **Realtime:** sincronização de checkpoint, plataforma, ajuda e alertas entre Android, totem e dashboard.
- **Storage:** mapas esquemáticos, imagens dos marcos, áudio acessível e assets versionados do terminal.
- **Messaging:** push de mudança de plataforma, embarque e pedido de ajuda; exige provedor push configurado.
- **Functions em Java:** expiração de handoffs, limpeza programada, fan-out de notificações e agregação de métricas.
- **Sites:** deploy do dashboard e do totem React, mantendo builds e domínios separados.
- **Permissions:** acesso por usuário, time e recurso; nenhuma coleção sensível fica pública.
- **Activity/Usage:** evidências técnicas e monitoramento durante testes e pitch.

## Modelo de dados mínimo

| Tabela/coleção | Campos principais | Escrita |
|---|---|---|
| `profiles` | `userId`, `name`, `accessibilityPrefs` | próprio usuário |
| `terminals` | `name`, `city`, `timezone` | operador |
| `terminal_points` | `terminalId`, `code`, `name`, `kind` | operador |
| `totems` | `terminalId`, `pointId`, `label`, `status`, `lastSeenAt` | API/operador |
| `routes` | `terminalId`, `fromPointId`, `toPointId`, `steps[]`, `headingDegrees`, `distanceMeters`, `accessible` | operador |
| `trips` | `ownerId`, `origin`, `destination`, `departureAt`, `platform`, `status` | API/operador |
| `journeys` | `tripId`, `userId`, `currentPointId`, `stage`, `checklist` | próprio usuário/API |
| `alerts` | `tripId`, `type`, `severity`, `message`, `platform`, `createdAt` | operador |
| `feedback` | `journeyId`, `rating`, `tags`, `comment` | próprio usuário |
| `help_requests` | `journeyId?`, `totemId`, `category`, `status`, `createdAt` | totem/operador |
| `handoff_sessions` | `journeyId`, `totemId?`, `tokenHash`, `expiresAt`, `consumedAt` | API |

O Appwrite usa IDs nativos como chave. Criar índices para `ownerId + departureAt`, `tripId + createdAt`, `terminalId + code` e `userId + stage`.

## Endpoints da API Java P0

| Método | Endpoint | Uso |
|---|---|---|
| GET | `/actuator/health` | prontidão da API |
| GET | `/v1/me/trips/next` | próxima viagem do passageiro |
| GET | `/v1/trips/:id/journey` | visão consolidada da jornada |
| PATCH | `/v1/journeys/:id/checklist` | atualizar checklist |
| POST | `/v1/journeys/:id/checkpoints` | validar QR/código |
| POST | `/v1/journeys/:id/handoffs` | gerar QR/token temporário |
| POST | `/v1/totems/:id/handoffs/consume` | vincular jornada ao totem uma vez |
| POST | `/v1/totems/:id/help-requests` | pedir ajuda com contexto |
| POST | `/v1/journeys/:id/complete` | concluir jornada |
| POST | `/v1/feedback` | registrar avaliação |
| GET | `/v1/ops/trips` | painel do operador |
| POST | `/v1/ops/trips/:id/alerts` | publicar alerta/mudar plataforma |

## Permissões

- Passageiro lê apenas seus `trips`, `journeys`, `alerts` relacionados e `feedback`.
- Dados de terminal/rota podem ser leitura pública autenticada.
- Operador escreve viagens, rotas e alertas via API.
- Chave Appwrite server-side existe apenas em secret do Spring Boot e das Functions.
- Dashboard e Android recebem endpoint e project ID públicos, nunca a API key.
- Totem só acessa dados da sessão efêmera; não consegue listar passageiros/viagens.
- Token de handoff é aleatório, uso único, expira em 60 s e é armazenado somente como hash.
- Sessão visível do totem expira por inatividade; logs não registram localizador ou token.

## Orientação direcional

Cada trecho curado possui um azimute esperado (`headingDegrees`). O Android calcula a menor diferença angular entre o azimute e a orientação filtrada do aparelho, anima a seta e exibe a instrução textual. Um filtro de suavização reduz tremor. A confiança considera precisão reportada pelo sensor e variação das últimas leituras. O recurso é auxílio visual: checkpoint e instrução textual continuam sendo a fonte de verdade.

## Ambientes

Usar três configurações: local, demo/pitch e produção. Para o pitch, congelar seed e versão pelo menos 24 horas antes. O project ID fornecido pode ser usado como ambiente demo; não misturar dados pessoais reais.

## Decisão de monorepo

Manter tudo junto reduz coordenação, centraliza documentação e permite uma única esteira de CI. Gradle organiza `apps/api` e `apps/mobile`; npm workspaces cuidam apenas de `dashboard`, `totem` e tipos gerados do OpenAPI. Dashboard e totem compartilham componentes visuais, mas são builds, domínios e permissões separados.

## Limite entre Java e Appwrite

O Spring Boot não replica Auth, banco, arquivos ou entrega de eventos. Ele concentra regras que exigem confiança de servidor: validar handoff, alterar plataforma de forma consistente, verificar papel do operador, emitir auditoria de negócio e integrar futuras viações. Appwrite permanece responsável pela infraestrutura gerenciada e pelos eventos.

Não há dependência de um SDK server-side Java não oficial. As integrações Java ficam atrás de interfaces próprias (`AppwriteAccountGateway`, `AppwriteDatabaseGateway`, `AppwriteStorageGateway`, `AppwriteMessagingGateway`), o que também facilita testes e acompanha versões da API REST sem espalhar detalhes pelos controllers.
