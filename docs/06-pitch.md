# Roteiro do pitch

## Narrativa principal de até 3 minutos

O pitch separa claramente o que funciona hoje da evolução IoT. A demonstração do MVP ocupa a maior parte do tempo; ESP32 e MQTT entram como próximo experimento mensurável, não como funcionalidade já entregue.

### 0:00–0:25 — problema

"Comprar a passagem é simples. O difícil começa depois: quando sair, onde entrar, qual plataforma e o que fazer quando ela muda. Em um terminal desconhecido, essas decisões viram ansiedade e atraso."

Apresentar uma fala real de entrevista, não uma estatística sem fonte.

### 0:25–0:45 — solução

"O Embarque Fácil cria uma jornada única entre a passagem e o assento. Ela continua no celular, no totem e na operação sem obrigar o passageiro a recomeçar ou repetir dados."

### 0:45–1:45 — demonstração do MVP atual

1. Abrir a próxima viagem e o checklist no Android.
2. Gerar o QR temporário e transferir a jornada para o totem.
3. Totem confirmar o ponto físico e mostrar o próximo passo acessível.
4. Android abrir a seta estilo Finder, sem câmera, mantendo texto e QR como fallback.
5. Operador mudar a plataforma no dashboard.
6. Android e totem receberem o alerta e recomporem a rota.

Narrar uma única história; evitar explicar cada clique.

### 1:45–2:15 — valor e tecnologia

Mostrar o funil real do teste: participantes, conclusão sem ajuda, tempo e principais erros. Se a pesquisa ainda não tiver ocorrido, dizer "hipótese a validar".

Resumir a base técnica: Android nativo em Java, React para dashboard e totem, backend Java/Spring Boot e Appwrite para autenticação, dados, realtime, arquivos, mensagens, Function e hospedagem. Destacar handoff de uso único, sessão pública efêmera e ausência de biometria ou localização contínua.

### 2:15–2:50 — evolução IoT plausível

"Hoje, o totem e os QR codes confirmam pontos físicos, enquanto a bússola aponta a direção. O próximo experimento adiciona marcos ESP32 por BLE para confirmar proximidade e sensores direcionais sem câmera para medir fluxo agregado. Os dispositivos publicam saúde e contagens por MQTT; o backend Java agrega os eventos no Appwrite. Assim, a operação identifica corredores congestionados ou dispositivos fora do ar e recomenda uma rota melhor, sem rastrear uma pessoa."

Exibir um único diagrama ou uma gravação curta da simulação. Rotular na tela: **roadmap pós-MVP — simulação**, se ainda não houver hardware físico.

### 2:50–3:00 — fechamento

"Primeiro provamos que a jornada conectada reduz dúvida e atraso. Depois fazemos o terminal responder ao fluxo real. O Embarque Fácil não substitui o atendimento: faz cada canal compartilhar contexto e levar o passageiro ao lugar certo."

## Extensão opcional para banca de 5 minutos

Se houver dois minutos adicionais, usar um minuto para dados de validação e um minuto para demonstrar a simulação MQTT: ESP32 publica heartbeat e contagem, o Java processa, o Appwrite atualiza e o dashboard exibe o estado. Não ampliar a quantidade de funcionalidades do MVP durante a fala.

## Preparação da demo

- Duas contas: passageiro e operador.
- Viagem com horário futuro e plataforma 18.
- QR impresso para `TIE-ENTRADA-A`.
- Alerta template muda para plataforma 21.
- Botão/script de reset idempotente.
- Android físico carregado, notificações liberadas e brilho alto.
- Dashboard já autenticado em outra máquina/aba.
- Totem em tela cheia e QR de handoff testado.
- Hotspot e rede alternativa.
- Vídeo de backup de 60–90 segundos.
- Um slide de roadmap IoT ou vídeo curto da simulação, sempre identificado como futuro.

## Respostas difíceis

**"Vocês integram com a ClickBus ou viações?"**

Ainda não. O MVP usa dados controlados para validar a jornada; o passo comercial seguinte é integrar o inventário de uma viação.

**"Como sabem a posição no terminal?"**

No MVP, cada totem tem um ponto físico cadastrado e QR codes intermediários complementam a rota. A seta usa a bússola para apontar o próximo marco, mas não finge rastrear a pessoa. No futuro, marcos ESP32 por BLE poderão confirmar proximidade; QR e texto continuarão como fallback.

**"O IoT já funciona?"**

Ainda não faz parte do MVP validado. O próximo passo é simular mensagens MQTT e o consumidor Java; depois, testar poucos ESP32 em bancada. A implantação no terminal só ocorre se precisão, confiabilidade, privacidade e benefício forem comprovados.

**"Como o fluxo será medido sem vigiar passageiros?"**

Sensores direcionais infravermelhos ou ToF contam entradas e saídas de forma agregada. Não há câmera, reconhecimento facial, identificação Bluetooth do celular ou vínculo entre a contagem e uma viagem.

**"Por que não usar GPS dentro do terminal?"**

GPS perde precisão em ambientes internos. BLE serve para reconhecer proximidade de um marco conhecido, e a bússola indica a direção. Nenhuma dessas fontes trabalha sozinha: sequência de rota, confiança do sinal e fallback por QR evitam avanço incorreto.

**"E se um ESP32 ou a rede cair?"**

Heartbeat MQTT sinaliza a falha no dashboard. O sistema ignora a fonte indisponível e mantém totem, QR e instrução textual. A camada IoT melhora a experiência, mas não pode ser requisito para embarcar.

**"E quem não tem celular?"**

O totem aceita o código/localizador da passagem demo, orienta e pode gerar um resumo para fotografar ou imprimir. O sistema não substitui painel e atendimento; cria mais um canal e mede onde a ajuda humana continua necessária.

**"Por que Appwrite e Java?"**

Appwrite reduz o tempo de infraestrutura para autenticação, dados, arquivos, realtime, push e deploy. A API Java/Spring Boot mantém regras privilegiadas e cria uma fronteira estável para futuras integrações, usando tecnologias alinhadas ao curso. Na evolução IoT, o Java também valida e agrega as mensagens MQTT antes de persistir estado útil no Appwrite.

**"O que há de realmente inovador?"**

Hoje, é preservar contexto entre celular, totem e operação com handoff temporário, orientação sem câmera e recomposição sincronizada da rota. A evolução transforma o terminal em uma fonte de contexto: marcos confirmam proximidade, fluxo agregado indica atrito e a operação recomenda caminhos melhores sem rastrear indivíduos.
