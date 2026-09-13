# Backlog e plano de execução

Plano-base para 10 dias úteis, com quatro integrantes. Se houver menos tempo, executar apenas P0 na ordem abaixo.

## Papéis

- **Produto/UX:** entrevistas, fluxos, textos, seed e roteiro do pitch.
- **Mobile:** Android em Java, autenticação, jornada, QR, direção por sensores e Realtime.
- **Backend:** Java/Spring Boot, Appwrite, permissões, Functions, seed e testes.
- **Dashboard/Totem/QA:** React, experiência kiosk, operação, E2E e evidências.

Todo mundo revisa PR; uma pessoa é dona de cada história, não de uma camada inteira.

## Ordem de implementação

### Dia 1 — alinhamento e fundação

- Rodar 3–5 entrevistas rápidas.
- Fechar promessa, persona e história do pitch.
- Criar apps Web/Android/server no Appwrite, database, buckets, Messaging e Sites demo.
- Inicializar monorepo, Gradle, npm workspaces, lint, formatação, CI e `.env.example`.
- Seedar terminal, totem, pontos, azimutes da rota e uma viagem.

**Marco:** login nos clientes e `/actuator/health` funcionando.

### Dias 2–3 — fatia vertical

- Home Android com próxima viagem real.
- Jornada e checklist persistido.
- Dashboard lista a mesma viagem.
- Totem abre em modo atração e responde ao healthcheck do dispositivo.
- Testes de autorização: passageiro não acessa operação.

**Marco:** um dado alterado pelo seed aparece nos dois clientes.

### Dias 4–5 — orientação

- QR + entrada manual de código.
- Handoff mobile → totem com token de uso único.
- Endpoint de checkpoint.
- Passos da rota, seta direcional e confirmação de chegada.
- Estados de erro, câmera negada, sensor impreciso, sessão expirada e código inválido.

**Marco:** passageiro sai da home e chega à plataforma.

### Dias 6–7 — loop operacional

- Formulário de alerta no dashboard.
- Atualização atômica da plataforma + registro de alerta.
- Realtime no Android e fallback de refresh.
- Appwrite Messaging envia push quando o Android está em segundo plano.
- Mudança aparece simultaneamente no totem vinculado.
- Pedido de ajuda no totem aparece no dashboard.
- Feedback pós-jornada.

**Marco:** alteração no dashboard aparece no Android ao vivo.

### Dia 8 — qualidade

- Testes unitários das regras e integração da API.
- Testes Java de controller/service e permissões Appwrite.
- Teste E2E web do login → publicar alerta.
- Teste Android do caminho crítico em aparelho/emulador.
- Revisão de acessibilidade e contraste.

### Dia 9 — ensaio

- Testar em rede diferente e com API reiniciada.
- Gravar vídeo curto de backup.
- Preparar QR impresso, contas demo e script de reset.
- Totem em tela cheia, com webcam/leitor testado e sessão anterior limpa.
- Ensaiar fala com cronômetro.

### Dia 10 — congelamento

- Somente correções críticas.
- Tag `pitch-v1` e build final.
- Checklist do equipamento e plano B offline.

## Histórias P0

| ID | História | Critério principal | Depende de |
|---|---|---|---|
| P0-01 | Como passageiro, entro e mantenho sessão | reabrir app mantém login | Appwrite apps/Auth |
| P0-02 | Vejo minha próxima viagem | apenas dados do usuário | seed/permissões |
| P0-03 | Acompanho o plano de embarque | status e checklist persistem | API/modelo |
| P0-04 | Confirmo onde estou por QR/código | inválido não avança | pontos/rota |
| P0-05 | Sigo passos até a plataforma | instrução atual é inequívoca | rota seedada |
| P0-06 | Operador vê viagens do dia | filtro e estado vazio | dashboard/API |
| P0-07 | Operador muda plataforma | operação auditável | team operator |
| P0-08 | Recebo alerta em tempo real | aparece em até 3 s | Realtime |
| P0-09 | Concluo e avalio | uma avaliação por jornada | feedback |
| P0-10 | Demo pode ser restaurada | comando idempotente | seed |
| P0-11 | Transfiro jornada ao totem | token único expira em 60 s | API/totem |
| P0-12 | Totem orienta sem app | passagem/código abre rota mínima | seed/totem |
| P0-13 | Peço ajuda no totem | operador vê ponto e categoria | Realtime/dashboard |
| P0-14 | Totem protege privacidade | inatividade limpa sessão | kiosk/API |
| P0-15 | Seta aponta próximo marco | sensor ruim aciona fallback | rota/sensores |
| P0-16 | Recebo alerta fora do app | push abre a jornada correta | Messaging/FCM |
| P0-17 | Assets e rotinas são gerenciados | Storage e Function Java testados | Appwrite |

## Processo de Git

- `main` sempre demonstrável.
- Branch curta: `feat/P0-04-qr-checkpoint`.
- PR pequeno com contexto, screenshots e como testar.
- Pelo menos uma revisão antes do merge.
- Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`).
- Issues ligadas aos IDs acima; bugs do pitch têm prioridade máxima.

## Definition of Done

- Critério de aceite atendido.
- Loading/vazio/erro/offline considerados.
- Permissão testada.
- Sem secret no código ou log.
- Teste automatizado proporcional ao risco.
- Evidência visual atualizada.
- Fluxo ensaiado em conta demo limpa.

## Riscos e mitigação

| Risco | Mitigação |
|---|---|
| Integração externa inexistente | seed/manual e discurso explícito |
| Realtime instável no pitch | botão atualizar + vídeo de backup |
| Câmera negada | entrada manual do código |
| Bússola oscila no terminal | calibração, confiança e modo passo a passo |
| Escopo voltar a crescer | P1 só começa após todos os P0 verdes |
| Appwrite mal configurado | checklist versionado e teste de permissão |
| Ausência de designer | reutilizar tokens do protótipo e validar tarefas |

## Próximos passos após o pitch

1. Congelar e medir a versão atual antes de adicionar hardware.
2. Executar testes com passageiros e registrar tempo, abandono, pedidos de ajuda e erros de direção.
3. Construir a simulação ESP32/MQTT com eventos marcados como sintéticos.
4. Integrar o consumidor MQTT ao backend Java e refletir saúde/fluxo no Appwrite e dashboard.
5. Fazer bancada com poucos dispositivos físicos e comparar leituras com contagem manual.
6. Só então propor piloto controlado no terminal, com autorização, levantamento de energia, rede, fixação, segurança, acessibilidade e privacidade.

As fases, métricas, materiais e critérios de decisão estão em [Próximos passos de IoT](07-proximos-passos-iot.md).
