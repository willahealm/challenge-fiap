# Produto e validação

## Problema que vale resolver

O momento mais crítico da viagem não é a compra da passagem, mas a transição entre "tenho uma passagem" e "estou no ônibus certo". Em terminais grandes, o passageiro precisa conciliar horário, deslocamento, documentos, plataforma, alterações operacionais e orientação física. As informações costumam estar espalhadas entre e-mail, painel do terminal, atendente e aplicativo da transportadora.

## Público inicial

**Primário:** passageiro rodoviário ocasional, com Android, que viaja por um terminal grande e sente insegurança sobre horário e plataforma.

**Secundário:** operador do terminal ou da viação que precisa comunicar alterações e identificar atritos recorrentes.

**Superfície assistiva obrigatória:** totem de autoatendimento da rodoviária, usado tanto por quem está com o celular quanto por quem precisa de uma tela maior ou não instalou o app.

Não tentar atender turista internacional, pessoa sem smartphone, compra de passagem, programa de pontos e todas as necessidades de acessibilidade no primeiro MVP. A interface deve ser acessível desde o início, mas recursos especializados entram após validação com usuários reais.

## Hipóteses a validar

| Hipótese | Experimento barato | Sinal de sucesso |
|---|---|---|
| Passageiros têm ansiedade sobre plataforma e horário | 5 entrevistas + teste do protótipo | 4/5 reconhecem o problema sem indução |
| Uma linha do tempo única reduz dúvidas | Teste de tarefa no mobile | 80% chegam à plataforma sem ajuda |
| QR em pontos do terminal é aceitável | Cartaz impresso em ambiente simulado | 80% entendem que devem escanear |
| Alertas operacionais têm valor | Alterar plataforma durante o teste | 80% percebem e corrigem a rota |
| Operadores conseguem publicar rápido | Teste no dashboard | alerta publicado em menos de 30 s |
| A passagem entre mobile e totem reduz fricção | Usuário transfere jornada por QR temporário | 80% continuam sem refazer login/dados |
| Totem atende também quem não tem app | Busca por localizador/passagem em protótipo | 80% encontram a plataforma sem atendente |

## O que há de bom no protótipo atual

- Foco na jornada completa, não apenas na compra.
- Próxima viagem como informação dominante na home.
- Acessibilidade visível, alertas de plataforma e feedback pós-viagem.
- Conexão clara entre experiência do passageiro e operação.

## O que precisa mudar

### Manter no MVP

- Login e perfil básico.
- Próxima viagem e linha do tempo de embarque.
- Checklist de documentos e bagagem.
- Orientação por etapas dentro do terminal.
- QR code para confirmar localização.
- Alertas em tempo real sobre plataforma, atraso ou embarque.
- Dashboard operacional e avaliação pós-viagem.
- Totem touch com consulta, orientação, sincronização com mobile e pedido de ajuda.

### Simular de forma transparente no pitch

- Horário recomendado de saída: regra simples com duração informada/fixa e margem de segurança; não alegar trânsito em tempo real.
- Viagem: dados seedados ou cadastrados manualmente; não alegar integração com ClickBus ou viações.
- Mapa do terminal: rota curada para um terminal de demonstração.

### Retirar do MVP

- Compra de passagem e carteira/pontos.
- Preços de Uber/99 e dados de transporte público sem parceria/API.
- Estimativa automática de fila.
- Concierge de IA generativa.
- Digital twin em tempo real.
- Realidade aumentada.
- NFC como único meio de identificação.
- Reconhecimento facial.

Esses itens podem aparecer em um slide de roadmap, nunca como capacidade funcional já validada.

## Proposta de valor revisada

> O Embarque Fácil mantém a mesma jornada no celular, no totem e na operação: o passageiro continua de onde parou, encontra a plataforma e recebe mudanças sem repetir dados ou depender de um único canal.

## Inovações plausíveis

### 1. Handoff de jornada por QR temporário

O app gera um QR válido por 60 segundos. O totem lê o token, mostra apenas os dados necessários e abre exatamente no estágio atual. Ao terminar, limpa a sessão e envia ao celular o ponto do terminal onde o usuário está. A novidade não é o QR isolado, mas a continuidade segura entre superfícies.

### 2. Rota que se recompõe com eventos operacionais

Uma mudança de plataforma publicada no dashboard invalida a instrução antiga e recompõe a sequência mostrada no celular e no totem. O usuário vê "o que mudou" e "o que fazer agora", não apenas uma notificação.

### 3. Acessibilidade contextual compartilhada

Preferências como alto contraste, fonte grande, rota sem escadas e leitura em voz alta acompanham a sessão temporária no totem, sem expor diagnóstico médico. O totem volta ao padrão após encerrar.

### 4. Pedido de ajuda com contexto

O botão de ajuda envia ao operador o ponto físico do totem, a viagem e o tipo de necessidade escolhido. O passageiro não precisa explicar onde está; o dashboard abre uma ocorrência já contextualizada.

### 5. Modo sem aplicativo

Quem não tem o app pode consultar por QR/código da passagem, receber orientação no totem e imprimir ou fotografar um resumo com um QR de continuidade. Isso evita que a inovação exclua justamente quem mais precisa de apoio.

### 6. Bússola estilo Finder, sem câmera

Depois que um totem ou QR ancora a posição, o Android mostra uma seta grande apontando para o próximo marco da rota. A direção usa bússola/giroscópio e vem acompanhada por texto e distância aproximada. Se a confiança do sensor estiver baixa, o app pede calibração ou retorna ao passo a passo; portanto, não depende de AR nem promete localização indoor centimétrica.

## Métrica norte

**Taxa de jornada assistida concluída:** percentual de viagens em que o usuário abre a jornada, confirma um ponto do terminal e chega à plataforma.

Métricas auxiliares: tempo para achar a plataforma, alertas visualizados, pedidos de ajuda, conclusão do checklist e CSAT pós-viagem.
