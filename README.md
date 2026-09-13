# Embarque Fácil

MVP de assistência à jornada rodoviária: o passageiro recebe a informação certa antes de sair de casa, encontra a plataforma no terminal e é avisado quando algo muda. A operação acompanha viagens, alertas e pontos de atrito em um dashboard.

O repositório começou como um protótipo visual em `generated-page.html`. A análise mostrou uma boa intenção, mas também um escopo inviável para um primeiro pitch (AR, biometria, NFC, IA, trânsito, filas e parceiros externos ao mesmo tempo). O planejamento abaixo reduz a proposta a uma jornada demonstrável, útil e tecnicamente honesta.

## Decisão de MVP

**Promessa:** "Da confirmação da passagem até a plataforma, sem ansiedade e sem se perder."

**Fluxo principal do pitch:**

1. Passageiro entra com credenciais demo validadas pela Function no Appwrite.
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

Nesta entrega, o Appwrite é a plataforma hospedada: Sites serve as duas interfaces, a Function Node 22 concentra API/autenticação demo e o Database mantém o estado compartilhado. A API Java permanece como implementação local de referência e suíte contratual. Chaves de API nunca entram no dashboard, totem ou app Android.

## Documentos executáveis

- [Produto e validação](docs/01-produto-e-validacao.md)
- [Escopo e critérios do MVP](docs/02-escopo-mvp.md)
- [Telas e jornadas](docs/03-telas-e-ux.md)
- [Arquitetura e Appwrite](docs/04-arquitetura.md)
- [Backlog e plano de execução](docs/05-backlog.md)
- [Roteiro do pitch](docs/06-pitch.md)
- [Checklist do Appwrite](appwrite/README.md)

## Executar o MVP

Versão pública gratuita:

- Dashboard: <https://6aa714cc2cb488fb8cbc.appwrite.network/>
- Totem: <https://6aa7174f16d81c6df2d9.appwrite.network/> — use `EF4821`

No dashboard, use o botão **Entrar no modo demonstração** ou `operador@demo.local` / `Operador123!`.

### Ambiente local

Inicie a API em um terminal:

```powershell
cd apps/api
.\gradlew.bat bootRun --console=plain
```

Depois, na raiz do repositório:

```powershell
npm install
$env:VITE_API_BASE_URL = "http://127.0.0.1:8080"
npm run dev
```

- Dashboard operacional: `http://localhost:5173`.
- Totem: `http://localhost:5174`.
- Operador: `operador@demo.local` / `Operador123!`.
- Passageiro: `lucas@demo.local` / `Demo123!`.
- Totem técnico: `totem@demo.local` / `Totem123!`.
- Sem a API, o modo demo web usa o servidor local sincronizado incluído no monorepo.
- APK Android: `apps/mobile/app/build/outputs/apk/debug/app-debug.apk`.

Use `npm run build`, `npm run lint` e `npm test` para validar a Web. Os READMEs de `apps/api` e `apps/mobile` documentam smoke test, Android Studio/ADB, modo offline e o contorno para bloqueios de arquivos do OneDrive.

## Estado atual

- Protótipo HTML: preservado como referência visual.
- API Java/Spring Boot: executável com seed em memória, autorização por papel, handoff, alertas, ajuda, feedback, reset e 21 testes.
- Dashboard e totem: implementados em React + TypeScript, integrados à API real e com fallback local sincronizado.
- Android Java/XML: fluxo completo, modo offline, QR/código, orientação por sensor e APK validado para Android 8–16.
- Contratos: OpenAPI, JSON Schema e tipos TypeScript em `packages/contracts`.
- Appwrite Cloud: Database, Function Node 22 e dois Sites ativos; autenticação demo demonstrável e estado persistente compartilhado. Auth real por usuário permanece como evolução pós-MVP.

## Regra de entrega

Uma funcionalidade só conta como pronta quando possui: estado de carregamento, vazio e erro; validação; regra de permissão; teste do caminho feliz; e evidência visual para o pitch.
