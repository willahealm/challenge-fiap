# Totem de autoatendimento

Aplicação React + TypeScript para navegador Chromium em modo kiosk/touch. O fluxo inclui atração, entrada do código, confirmação mínima, orientação acessível, mudança crítica de plataforma, pedido de ajuda e limpeza automática em 30 segundos.

Publicado no Appwrite Sites: <https://6aa713f546bfdd6cc784.appwrite.network/>. Use `EF4821` para a demonstração hospedada.

## Executar

Na raiz:

```bash
npm install
npm run dev
```

Abra `http://localhost:5174` e use o código `EF4821`. O comando também inicia o dashboard e o servidor demo compartilhado. Uma troca para a plataforma 21 publicada no dashboard aparece no totem imediatamente.

Para usar a API Java, crie `apps/totem/.env.local`:

```env
VITE_API_BASE_URL=http://127.0.0.1:8080
VITE_TOTEM_ID=totem-tiete-01
```

O totem não persiste token, passageiro ou localizador. O token kiosk fica apenas em memória e é descartado ao encerrar ou expirar a sessão. `VITE_TOTEM_API_URL` continua aceito como alias legado.

O botão **Ouvir instruções** solicita explicitamente síntese `pt-BR`, prioriza vozes brasileiras locais e usa uma voz portuguesa como fallback. Em instalações kiosk, mantenha ao menos uma voz portuguesa instalada no sistema operacional.

Em produção, o navegador invoca `embarque-api` pelo gateway oficial do Appwrite; a identidade demo do totem é validada na Function e nenhum segredo de servidor é distribuído.
