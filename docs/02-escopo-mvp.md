# Escopo e critérios do MVP

## Vertical slice obrigatória

O MVP só precisa provar uma história completa:

> Lucas autentica, abre a viagem São Paulo → Rio, vê o plano de embarque, transfere a jornada para o totem por QR temporário, recebe orientação a partir da localização física do totem, acompanha a troca da plataforma 18 para 21 publicada pelo operador e confirma que chegou.

## P0 — precisa funcionar no pitch

1. Autenticação por e-mail/senha com Appwrite.
2. Sessão persistente e logout.
3. Viagem de demonstração associada ao usuário.
4. Home com próxima viagem, status e CTA único.
5. Detalhe com linha do tempo e checklist persistido.
6. Scanner de QR e fallback para digitar o código.
7. Instruções por passos até a plataforma.
8. Alerta de mudança publicado pelo dashboard e recebido via Realtime.
9. Dashboard com login de operador, viagens do dia e formulário de alerta.
10. Avaliação de 1–5 e comentário opcional.
11. API Java/Spring Boot com healthcheck, autorização e endpoints de regra de negócio.
12. Totem React em modo kiosk com sessão efêmera, leitura/entrada de código e orientação.
13. Sincronização do estágio e dos alertas entre mobile e totem.
14. Pedido de ajuda que chega contextualizado ao dashboard.
15. Modo direção com seta para o próximo marco, instrução textual e indicador de confiança do sensor.
16. Push via Appwrite Messaging para alertas críticos quando o app estiver em segundo plano.
17. Assets de terminal servidos pelo Appwrite Storage e rotinas automáticas executadas por Appwrite Functions em Node.js 22.

## P1 — somente se P0 estiver estável

- Notificação push para aplicativo em segundo plano.
- Modo alto contraste e ajuste de fonte dentro do app.
- Cadastro manual de nova viagem.
- CRUD de terminais, totens, pontos e instruções no dashboard.
- Impressão de resumo da rota no totem, se houver impressora disponível.
- Indicadores simples de conclusão e CSAT.

## Fora do MVP

- Bilhetagem/pagamento, biometria, AR, gamificação, chat com IA, previsão de fila, integração com apps de transporte e integração real com transportadoras. NFC é P1 e nunca o único caminho.

## Critérios de aceite da demonstração

- O fluxo completo leva menos de 4 minutos.
- Nenhuma tela depende de editar dados no console durante o pitch.
- A troca de plataforma aparece no mobile em até 3 segundos em rede estável.
- Se Realtime falhar, o botão "Atualizar" recupera o estado.
- Se a câmera falhar, o código do QR pode ser digitado.
- A sessão do totem expira, apaga dados visíveis e volta à home em no máximo 30 segundos de inatividade.
- O mobile reconhece o checkpoint confirmado pelo totem sem nova ação do usuário.
- Um pedido de ajuda mostra no dashboard o identificador do totem e a viagem.
- A seta nunca aparece sozinha: direção textual, distância e fallback passo a passo continuam disponíveis.
- O demo possui dados seedados e um roteiro de reset.
- Nenhuma chave de servidor aparece no bundle web/mobile ou no Git.

## Guardrails éticos e de privacidade

- Coletar apenas nome, e-mail e dados necessários à jornada.
- Não coletar biometria ou localização contínua no MVP.
- Câmera usada somente ao abrir o scanner.
- Explicar que os dados da viagem são de demonstração.
- Permitir logout e documentar a exclusão de conta como etapa pós-MVP.
