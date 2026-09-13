# Roteiro do pitch

## Narrativa de 5 minutos

### 0:00–0:40 — problema

"Comprar a passagem é simples. O difícil começa depois: quando sair, onde entrar, qual plataforma e o que fazer quando ela muda. Em um terminal desconhecido, essas decisões viram ansiedade e atraso."

Apresentar uma fala real de entrevista, não uma estatística sem fonte.

### 0:40–1:10 — solução

"O Embarque Fácil cria uma linha do tempo única entre a passagem e o assento. A jornada continua no celular, no totem e na operação sem o passageiro repetir dados."

### 1:10–3:20 — demonstração

1. Login e próxima viagem.
2. Checklist e recomendação de saída identificada como estimativa.
3. App gera QR temporário e a jornada passa para o totem.
4. Totem confirma a localização e mostra a rota acessível.
5. Android abre a seta estilo Finder para o próximo marco, sem câmera.
6. Colega publica troca de plataforma no dashboard.
7. Alerta chega no Android e no totem; a rota é recomposta.
8. Passageiro envia a rota de volta ao celular, confirma chegada e avalia.

### 3:20–4:10 — valor e evidência

Mostrar o funil real do teste: participantes, conclusão sem ajuda, tempo e principais erros. Se ainda não houver pesquisa, dizer "hipótese a validar".

### 4:10–4:40 — tecnologia

Android nativo para o passageiro, dois apps React separados para operação e totem, Express para regras e Appwrite para autenticação, dados e eventos em tempo real. Destacar token de uso único, limpeza da sessão pública e ausência de biometria/localização contínua.

### 4:40–5:00 — próximos passos

Piloto controlado em um terminal, medição da taxa de jornada concluída e integração com uma viação. NFC entra apenas se o piloto provar necessidade; a seta direcional evolui com calibração real do ambiente.

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

## Respostas difíceis

**"Vocês integram com a ClickBus ou viações?"**

Ainda não. O MVP usa dados controlados para validar a jornada; o passo comercial seguinte é integrar o inventário de uma viação.

**"Como sabem a posição no terminal?"**

Cada totem tem um ponto físico cadastrado; ao vincular a jornada, ele confirma a posição. QR codes intermediários complementam a rota. A seta usa a bússola apenas para apontar o próximo marco e declara sua confiança; ela não finge rastrear a posição dentro do prédio.

**"E quem não tem celular?"**

O totem aceita o código/localizador da passagem demo, orienta e pode gerar um resumo para fotografar ou imprimir. O sistema não substitui painel e atendimento; cria mais um canal e mede onde a ajuda humana continua necessária.

**"Por que Appwrite e Express?"**

Appwrite reduz o tempo de infraestrutura para auth, dados e realtime. Express mantém regras privilegiadas e cria uma fronteira estável para futuras integrações.

**"O que há de realmente inovador?"**

Não é colocar mais uma tela no terminal. É preservar o contexto entre celular, totem e operação com um handoff temporário: o totem ancora a posição, o celular aponta o próximo marco sem câmera, mudanças operacionais recompõem a rota em todas as superfícies e pedidos de ajuda chegam com localização e viagem.
