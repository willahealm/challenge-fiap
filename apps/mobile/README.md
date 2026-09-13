# Embarque Fácil — Android

Aplicativo passageiro do MVP em Android nativo: Java, Android SDK e layouts XML. O app funciona contra a API local do monorepo ou inteiramente offline com um roteiro determinístico para o pitch.

## O que está implementado

- login e sessão persistente protegida por Android Keystore;
- URL da API e modo offline configuráveis no login e no Perfil;
- próxima viagem, status, plataforma e detalhe consolidado;
- linha do tempo e checklist persistente;
- scanner CameraX + ML Kit com fallback de código manual;
- rota textual, leitura por Text-to-Speech e seta por `TYPE_ROTATION_VECTOR`;
- confiança `Boa / Regular / Baixa`, instrução de recalibração e fallback sem sensor;
- polling da jornada a cada 2 segundos, botão Atualizar e alerta crítico 18 → 21;
- QR/código temporário para handoff ao totem, expiração visível e regeneração;
- conclusão da jornada e feedback de 1–5;
- estados de carregamento, erro e recuperação nas integrações principais.

O cliente segue `packages/contracts/openapi.json`. Não contém API keys, senha de produção, localização contínua, biometria ou credenciais administrativas. As contas em `demo.local` são dados públicos fictícios do ambiente demo.

## Requisitos

- JDK 17 ou superior (o build foi validado com JDK 25);
- Android SDK Platform 36 e Build Tools 36.0.0;
- Android Studio ou `ANDROID_HOME` apontando para um SDK válido.

O Gradle Wrapper 9.1.0 está versionado; não é necessário instalar Gradle globalmente.

## Build e testes

No PowerShell, a partir de `apps/mobile`:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --console=plain
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Se o OneDrive bloquear a limpeza de arquivos gerados, redirecione somente a saída do build:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug "-PbuildOutput=$env:LOCALAPPDATA/Embarque/mobile-build"
```

Nesse caso, o APK fica em `$env:LOCALAPPDATA/Embarque/mobile-build/app/outputs/apk/debug/app-debug.apk`.

## Rodar no modo offline do pitch

1. Abra `apps/mobile` no Android Studio e execute a configuração `app` em um aparelho/emulador com Android 8+.
2. Mantenha **Usar dados offline do pitch** ativado.
3. Toque em **Entrar no modo demonstração**.
4. Abra a viagem São Paulo → Rio e marque o checklist.
5. Em **Confirmar localização por QR**, permita a câmera ou digite `TIETE-TOTEM-01`.
6. Abra a direção. Em emulador/sem bússola, o app preserva texto, distância e navegação por etapas.
7. Volte à jornada. Após a terceira atualização automática, o seed muda a plataforma 18 → 21 e abre o alerta crítico.
8. Em **Continuar em um totem**, mostre o QR ou use o código `LUCAS1` no totem offline.
9. Confirme **Cheguei à plataforma** e envie a avaliação.

Em **Perfil e ambiente → Reiniciar roteiro demo**, plataforma, alertas e estágio voltam ao início. O checklist permanece no aparelho para demonstrar persistência; pode ser desmarcado manualmente.

## Rodar com a API local

Primeiro inicie a API em outro terminal:

```powershell
cd ..\api
.\gradlew.bat bootRun --console=plain
```

No app, desative o modo offline e use:

- emulador Android: `http://10.0.2.2:8080/`;
- aparelho USB: execute `adb reverse tcp:8080 tcp:8080` e use `http://127.0.0.1:8080/`;
- rede local controlada: inicie a API com `API_BIND_ADDRESS=0.0.0.0` e informe `http://IP_DO_COMPUTADOR:8080/`.

Credenciais de passageiro: `lucas@demo.local` / `Demo123!`. Ao trocar ambiente, o app encerra a sessão local e pede novo login para não reutilizar token no servidor errado.

Para instalar o APK por USB:

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r .\app\build\outputs\apk\debug\app-debug.apk
```

HTTP sem TLS é permitido neste MVP para localhost/LAN de desenvolvimento. Em produção, use somente HTTPS e troque o perfil demo por autenticação Appwrite/API real.

## Estrutura

```text
app/src/main/java/br/com/fiap/embarquefacil/
  data/       repositórios remoto/offline, sessão e DTOs
  ui/         ViewModel compartilhado e fragments por etapa
  util/       formatação e cálculo angular testável
app/src/main/res/layout/  telas XML
```

O polling é a estratégia explícita do MVP porque Realtime não está habilitado na API local. `Atualizar` sempre força a mesma visão consolidada, mantendo mobile e totem compatíveis com a troca publicada pelo dashboard.
