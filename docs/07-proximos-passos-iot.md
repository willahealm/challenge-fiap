# Próximos passos de IoT

## Objetivo

Evoluir o Embarque Fácil de uma jornada conectada por QR e totem para uma orientação assistida pela infraestrutura do terminal. ESP32 e MQTT devem melhorar confirmação de proximidade, visibilidade operacional e resposta ao fluxo — nunca virar uma dependência para o passageiro embarcar.

## Hipóteses que precisam ser provadas

1. Um marco BLE reduz confirmações manuais sem avançar a rota no ponto errado.
2. Contagem direcional agregada identifica concentração de pessoas com precisão suficiente para apoiar a operação.
3. Recomendar um caminho alternativo reduz tempo ou pedidos de ajuda.
4. O benefício justifica instalação, calibração e manutenção dos dispositivos.

## Arquitetura do experimento

- **Marco de proximidade:** ESP32-C3 ou ESP32-S3 anuncia por BLE um identificador rotativo do ponto físico. O Android Java avalia intensidade, estabilidade e sequência da rota localmente.
- **Fluxo agregado:** dois sensores ToF ou infravermelhos em sequência inferem entrada/saída. A mensagem contém contagem, zona, instante e confiança, sem imagem ou identidade.
- **Transporte:** MQTT sobre TLS, credencial exclusiva por dispositivo, ACL por tópico e heartbeat periódico.
- **Ingestão:** Spring Boot assina os tópicos, valida esquema, dispositivo, timestamp e duplicidade, agrega os eventos e atualiza o Appwrite.
- **Experiência:** Realtime entrega estado operacional ao dashboard; mudanças de rota continuam exigindo aprovação humana no primeiro piloto.
- **Fallback:** QR, código digitável, totem e passos textuais funcionam mesmo sem IoT.

## Plano por fases

### Fase 0 — medir a base atual

Prazo sugerido: 1 semana.

- Testar o MVP com 5–8 participantes em um percurso representativo.
- Registrar conclusão sem ajuda, tempo, erros de direção, checkpoints repetidos e CSAT.
- Congelar o fluxo e criar uma linha de base antes de introduzir sensores.

**Saída:** relatório de validação e decisão sobre qual problema IoT atacar primeiro.

### Fase 1 — simulação sem hardware

Prazo sugerido: 2–3 dias.

- Simular ESP32, Wi-Fi e publicação MQTT no Wokwi.
- Publicar heartbeat, perda de dispositivo, passagem de entrada/saída e ruído de leitura.
- Implementar consumidor MQTT no backend Java.
- Criar coleções IoT no Appwrite e cards de saúde/fluxo no dashboard.
- Usar um `SimulatedBeaconProvider` no Android, pois a simulação não substitui teste BLE físico com o celular.
- Identificar visualmente todo dado sintético no dashboard e no pitch.

**Saída:** vídeo reproduzível de ESP32 simulado → MQTT → Java → Appwrite → dashboard.

### Fase 2 — bancada física

Prazo sugerido: 1 semana.

- Montar 3 marcos ESP32 para representar entrada, corredor e plataforma.
- Montar 1 contador direcional com dois sensores ToF/IR.
- Adicionar fonte USB estável, caixas, etiquetas de identificação e inventário.
- Implementar `BleBeaconProvider` no Android e calibrar limiares em distâncias reais.
- Comparar contagem automática com contagem manual em passagens controladas.
- Testar perda de energia, Wi-Fi, MQTT e um dispositivo com credencial revogada.

**Saída:** bancada demonstrável, relatório de calibração e lista de falhas conhecidas.

### Fase 3 — piloto controlado no terminal

Prazo sugerido: 2–4 semanas, dependente de autorização.

- Fazer levantamento de planta, materiais, interferência de rádio, energia, rede e pontos seguros de fixação.
- Validar acessibilidade, sinalização, evacuação, limpeza e manutenção com a administração do terminal.
- Publicar aviso de privacidade simples e confirmar que sensores não coletam identidade.
- Começar com uma única rota e poucos horários, acompanhado por equipe humana.
- Comparar grupo com QR/totem e grupo com apoio BLE; registrar benefício e incidentes.

**Saída:** decisão de continuar, recalibrar ou descartar a hipótese.

### Fase 4 — escala e operação

- Provisionamento e revogação remota de dispositivos.
- Atualização de firmware assinada e inventário por versão.
- Alertas de indisponibilidade, manutenção preventiva e observabilidade.
- Regras graduais de recomendação de rota baseadas em fluxo e confiança.
- Avaliação de fornecedor, custo total, homologação aplicável e suporte em campo.

## Critérios de passagem entre fases

| Métrica | Meta inicial do experimento |
|---|---|
| Detecção do marco correto | pelo menos 90% em até 5 s na zona marcada |
| Avanço no marco errado | no máximo 5% em percurso controlado |
| Erro da contagem direcional | no máximo 15% contra contagem manual |
| Detecção de dispositivo offline | em até 90 s |
| Atualização operacional | em até 3 s após o backend aceitar o evento |
| Operação sem IoT | 100% do fluxo essencial disponível por QR/totem/texto |
| Privacidade | zero identificadores pessoais na telemetria MQTT |

As metas são hipóteses iniciais, não garantias comerciais. Devem ser recalibradas com o formato do terminal e o hardware escolhido.

## Materiais mínimos para bancada

- 3 placas ESP32-C3 ou ESP32-S3 com BLE e Wi-Fi.
- 2 sensores ToF ou infravermelhos para contagem direcional.
- Cabos, fontes USB certificadas, protoboard e caixas não condutivas.
- Celular Android físico com BLE e sensores de orientação.
- Broker MQTT de teste ou Mosquitto local com TLS.
- Notebook executando backend Java, dashboard e ferramentas de observação.

## Segurança, privacidade e operação

- Uma credencial MQTT por ESP32; nunca compartilhar uma senha entre a frota.
- TLS, ACL por tópico, rotação e revogação de credenciais.
- Identificador rotativo nos anúncios BLE para dificultar correlação indevida.
- Sem MAC do celular, passagem, nome, imagem, áudio ou biometria em mensagens IoT.
- Retenção curta para telemetria bruta; guardar agregados e incidentes necessários.
- Watchdog, heartbeat, horário sincronizado e fila local limitada para quedas de rede.
- Mudança de rota sugerida ao operador antes de qualquer automação plena.
- Aprovação da administração do terminal antes de instalação; verificar requisitos elétricos, de rádio, fixação, incêndio, LGPD e homologação aplicável ao equipamento final.

## Riscos e respostas

| Risco | Resposta |
|---|---|
| RSSI BLE oscila com paredes e pessoas | janela móvel, histerese, sequência de rota e confirmação por QR |
| Bússola perde precisão perto de metal | indicador de confiança, recalibração e instrução textual |
| Contagem mistura grupos ou malas | calibrar altura/intervalo e exibir confiança, não número absoluto como verdade |
| Rede ou energia cai | heartbeat, cache limitado e fluxo principal independente de IoT |
| Hardware vira escopo infinito | um corredor, quatro eventos e critérios de saída por fase |
| Dado sintético parece produção | selo "simulado" em tela, roteiro e vídeo |

## Definition of Done do experimento simulado

- Firmware ou projeto Wokwi versionado.
- Contrato JSON e tópicos MQTT documentados.
- Consumidor Java com testes para mensagem válida, duplicada, antiga e dispositivo revogado.
- Estado agregado persistido no Appwrite, sem telemetria pessoal.
- Dashboard diferencia online, atrasado, offline e simulado.
- Procedimento de reset e vídeo de backup do fluxo completo.
- Decisão registrada: seguir para bancada, ajustar hipótese ou parar.
