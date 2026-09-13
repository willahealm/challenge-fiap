# Embarque Fácil

MVP de assistência à jornada rodoviária: o passageiro recebe a informação certa antes de sair de casa, encontra a plataforma no terminal e é avisado quando algo muda. A operação acompanha viagens, alertas e pontos de atrito em um dashboard.

O repositório começou como um protótipo visual em `generated-page.html`. A análise mostrou uma boa intenção, mas também um escopo inviável para um primeiro pitch (AR, biometria, NFC, IA, trânsito, filas e parceiros externos ao mesmo tempo). O planejamento abaixo reduz a proposta a uma jornada demonstrável, útil e tecnicamente honesta.

## Decisão de MVP

**Promessa:** "Da confirmação da passagem até a plataforma, sem ansiedade e sem se perder."

**Fluxo principal do pitch:**

1. Passageiro entra com e-mail e senha pelo Appwrite Auth.
2. Adiciona uma viagem manualmente ou abre a viagem de demonstração.
3. Vê horário recomendado de saída e checklist de embarque.
4. Ao chegar, escaneia um QR code do terminal.
5. Recebe instruções simples até a plataforma e uma mudança em tempo real.
6. Encosta em um totem, transfere a jornada por QR temporário e confirma sua posição.
7. Operador publica o alerta no dashboard React.
8. Totem e Android exibem a nova plataforma; o passageiro confirma que chegou e avalia a experiência.

QR code foi escolhido como ponte principal entre mobile e totem porque funciona em qualquer Android com câmera, custa pouco e pode ser demonstrado sem leitores especiais. NFC pode ser um segundo método no totem físico, mas o QR continua sendo o fallback universal.

## Arquitetura alvo

Este será um **monorepo**:

```text
apps/
  api/          Java + Spring Boot
  dashboard/    React + TypeScript
  totem/        React PWA em modo kiosk/touch
  mobile/       Android nativo + Java + layouts XML
packages/
  contracts/    OpenAPI e schemas compartilhados entre as aplicações
appwrite/       modelo, permissões e checklist de configuração
docs/           produto, telas, backlog, arquitetura e pitch
```

O Appwrite será a plataforma central: autenticação, Teams, dados, Storage, Realtime, Messaging, Functions, permissões e hospedagem web. A API Java concentra regras privilegiadas, validações, cálculo de status e integrações. Chaves de API nunca entram no dashboard, totem ou app Android.

## Documentos executáveis

- [Produto e validação](docs/01-produto-e-validacao.md)
- [Escopo e critérios do MVP](docs/02-escopo-mvp.md)
- [Telas e jornadas](docs/03-telas-e-ux.md)
- [Arquitetura e Appwrite](docs/04-arquitetura.md)
- [Backlog e plano de execução](docs/05-backlog.md)
- [Roteiro do pitch](docs/06-pitch.md)
- [Checklist do Appwrite](appwrite/README.md)

## Estado atual

- Protótipo HTML: existente, navegável e útil como referência visual.
- Produto: escopo revisado e priorizado.
- Appwrite: projeto existe, mas ainda não tem apps, usuários, banco ou chave.
- API, dashboard e Android: estrutura planejada; implementação ainda não iniciada.

## Regra de entrega

Uma funcionalidade só conta como pronta quando possui: estado de carregamento, vazio e erro; validação; regra de permissão; teste do caminho feliz; e evidência visual para o pitch.
