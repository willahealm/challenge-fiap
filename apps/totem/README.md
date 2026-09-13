# Totem de autoatendimento

Aplicação React + TypeScript independente, executada em navegador Chromium em modo kiosk/touch.

Primeiro marco funcional:

1. Tela de atração e seleção de acessibilidade/idioma.
2. Consumo de QR/código temporário gerado no Android.
3. Exibição da viagem e da rota a partir do ponto físico do totem.
4. Atualização de plataforma por Realtime.
5. Pedido de ajuda contextualizado.
6. Encerramento e limpeza automática por inatividade.

O totem nunca usa a sessão do dashboard, não mantém dados pessoais em armazenamento local e deve funcionar também com código digitado quando câmera/leitor falhar.
