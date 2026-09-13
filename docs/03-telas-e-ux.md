# Telas e jornadas

## Princípios

- Uma decisão principal por tela.
- Status operacional sempre em texto + cor + ícone.
- Linguagem concreta: "Plataforma alterada para 21" em vez de "Atualização na viagem".
- Toques com no mínimo 48 dp no Android.
- Estados de loading, vazio, erro e offline desenhados junto com o caminho feliz.
- Acessibilidade é comportamento da interface, não uma aba isolada.

## Mobile Android

### 1. Entrada e login

E-mail, senha, mostrar/ocultar senha, recuperar acesso e botão "Entrar no modo demonstração" disponível apenas em build de demo.

Melhoria sobre o protótipo: hoje ele começa logado com uma pessoa fictícia e não explica origem dos dados.

### 2. Home / próxima viagem

- Rota, data, hora, viação e status.
- CTA "Preparar meu embarque".
- Alerta crítico fixo quando houver mudança.
- Estado vazio com "Adicionar viagem".

Remover compra de passagem, pontos, seguro e atalhos inativos: desviam da promessa central.

### 3. Jornada

Linha do tempo com quatro blocos: preparar, sair, chegar ao terminal e embarcar. Checklist curto: documento, passagem, bagagem e horário limite. Mostrar a origem de cada horário: "estimativa de demonstração" quando não houver dado real.

### 4. Confirmar localização

Scanner de QR com explicação antes de pedir câmera. Fallback "Digitar código do ponto" e ajuda para encontrar a placa. Retorno imediato: ponto reconhecido, inválido ou de outro terminal.

### 5. Rota até a plataforma

Passos textuais curtos, seta, distância aproximada e botão "Ouvir instrução" usando recursos nativos do Android. QR intermediário opcional confirma progresso. Evitar câmera/AR no MVP.

#### Modo direção (estilo Finder)

- Seta grande em tela cheia gira em direção ao próximo marco.
- Cabeçalho diz o destino concreto: "Escada rolante do setor B".
- Texto confirma a ação: "vire à direita e siga cerca de 35 m".
- Indicador `Boa / Regular / Baixa` comunica confiança da bússola.
- Ação "Recalibrar" ensina o movimento em oito.
- Com confiança baixa persistente, trocar para instruções por etapas e pedir confirmação em outro QR/totem.

O recurso usa o azimute cadastrado em cada trecho da rota e os sensores Android acessados por Java. Não tenta estimar posição por GPS dentro do prédio.

### 6. Alerta operacional

Bottom sheet/modal interrompe apenas para mudança crítica. Exibe o que mudou, horário, nova instrução e "Entendi". O alerta permanece na jornada até ser lido.

### 7. Chegada e feedback

"Cheguei à plataforma" encerra a jornada. Avaliação de 1–5, motivos rápidos (orientação, informação, acessibilidade) e comentário opcional.

### Navegação

Somente três destinos: **Início**, **Viagens**, **Perfil**. Scanner é ação contextual dentro da jornada. Não manter abas vazias como Carteira e Acesso.

## Dashboard React

### 1. Login do operador

E-mail/senha, erro claro e acesso somente para membros do time `operators`.

### 2. Visão do dia

- KPIs: viagens monitoradas, alertas ativos, passageiros assistidos e CSAT.
- Tabela de viagens com horário, rota, plataforma, status e quantidade de jornadas ativas.
- Filtros por terminal e status.

### 3. Detalhe da viagem

Dados da viagem, plataforma atual, histórico de alterações e passageiros impactados (contagem, não lista nominal no MVP).

### 4. Publicar alerta

Tipo, mensagem, severidade, nova plataforma opcional e confirmação visual antes de publicar. Templates evitam digitação e mensagens ambíguas.

### 5. Terminal e rota demo (P1)

CRUD de pontos, códigos QR e passos. No P0, usar seed controlado para reduzir risco.

### 6. Insights (P1)

Funil da jornada, tempo mediano até plataforma e distribuição do feedback. Não inventar economia de tempo sem uma linha de base medida.

## Totem React em modo kiosk

O totem é um app React separado do dashboard, otimizado para touch, tela grande e sessão pública. Ele não compartilha a sessão administrativa do operador.

### 1. Tela de atração

- "Encontre sua plataforma" como ação principal.
- Opções: escanear QR do app, escanear/digitar passagem e explorar o terminal.
- Idioma e acessibilidade sempre visíveis.
- Nenhum dado da pessoa anterior permanece na tela.

### 2. Vincular jornada

O totem mostra uma câmera/leitor para o QR temporário gerado pelo Android. Fallback: código curto de 6 caracteres ou localizador da passagem demo. Exibir claramente que a sessão será apagada ao final.

### 3. Confirmação mínima

Mostrar somente rota, horário, viação, primeiro nome opcional e plataforma. O usuário confirma "Esta é minha viagem"; nada de e-mail, documento completo ou histórico.

### 4. Orientação

- Mapa esquemático grande e passos textuais.
- Rota acessível selecionável.
- "Enviar rota ao celular" atualiza a jornada já vinculada ou gera QR de continuidade.
- Botão "Ouvir instruções" e controle de volume.

### 5. Mudança crítica

Se a plataforma mudar, interromper o fluxo com comparação direta: **antes 18 → agora 21**, motivo e primeiro passo da nova rota. Exigir apenas "Entendi".

### 6. Pedir ajuda

Escolhas rápidas: mobilidade, visão/audição, informação da viagem ou segurança. O operador recebe `totemId`, ponto físico, viagem e categoria. Mostrar tempo/status do chamado sem prometer SLA não validado.

### 7. Encerramento e privacidade

Botões "Continuar no celular" e "Encerrar". Após inatividade, contagem regressiva curta, revogação do token efêmero, limpeza do estado local e retorno à tela de atração.

### Hardware mínimo do MVP

- Monitor touch ou notebook em suporte, webcam/leitor QR e áudio.
- Navegador Chromium em tela cheia.
- NFC e impressora são opcionais; o software não depende deles.
- Identificador físico por totem, configurado no deploy.

## Protótipo atual

`generated-page.html` continua como referência visual, mas as telas de AR, digital twin, face ID, NFC, pontos e concierge não representam o escopo comprometido para o MVP.
