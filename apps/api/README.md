# API

Implementar em Java com Spring Boot e Gradle conforme `docs/04-arquitetura.md`. A integração server-side usa a API REST do Appwrite por `RestClient`/`WebClient`, pois não há SDK oficial de servidor em Java. Primeiro marco: `/actuator/health`, validação da sessão Appwrite e `GET /v1/me/trips/next`.
