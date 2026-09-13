# Appwrite Functions em Java

Functions pequenas e orientadas a evento/agendamento, sem duplicar a API Spring Boot:

- `expire-handoffs`: invalida sessões temporárias vencidas.
- `dispatch-trip-alert`: converte alertas críticos em mensagens push.
- `aggregate-journey-metrics`: calcula métricas anônimas para o dashboard.
- `totem-heartbeat-monitor`: marca totens sem heartbeat como offline.

Cada Function deve ter chave/escopos próprios, variáveis de ambiente separadas, execução idempotente e logs sem dados pessoais ou tokens.
