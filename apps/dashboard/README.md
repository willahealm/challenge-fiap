# Dashboard operacional

Painel React + TypeScript do Embarque Fácil. Lista as viagens do dia, publica troca de plataforma e recebe pedidos de ajuda contextuais do totem.

## Executar

Na raiz do repositório:

```bash
npm install
npm run dev
```

O dashboard abre em `http://localhost:5173`. O comando também inicia o totem e o servidor de demonstração compartilhado. Para executar apenas este app, use `npm run dev:dashboard`.

Conta demo: `operador@demo.local` / `Operador123!` (o botão de demonstração preenche e entra automaticamente).

Sem configuração, o app usa `http://localhost:3100`, que sincroniza a troca de plataforma e os chamados com o totem. Para usar a API Java, crie `apps/dashboard/.env.local`:

```env
VITE_API_BASE_URL=http://127.0.0.1:8080
```

Se a API não responder, o app sinaliza o modo offline e mantém uma cópia seed local para o pitch. `VITE_API_URL` continua aceito como alias legado.
