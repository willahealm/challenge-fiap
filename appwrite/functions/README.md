# Appwrite Function do MVP

A Function `embarque-api` está publicada no Appwrite Cloud com Node.js 22, entrada `api/src/main.js` e execução pública. Ela mantém o contrato HTTP `/v1` usado pelas aplicações, autenticação demo por papel e persistência no Database.

Recursos usados:

- database `embarque`;
- collection privada `mvp_state`;
- document `demo`, contendo o estado serializado;
- escopos dinâmicos mínimos `documents.read` e `documents.write`.

Os clientes invocam a Function pelo endpoint oficial de executions com `X-Appwrite-Project`; nenhum segredo é enviado ao navegador. As contas `lucas@demo.local`, `operador@demo.local` e `totem@demo.local` existem apenas para uma demonstração controlada, não representam autenticação de produção.

Para publicar uma nova versão a partir da raiz:

```powershell
npm exec --yes --package appwrite-cli appwrite -- functions create-deployment --function-id embarque-api --code appwrite/functions/api --activate --entrypoint src/main.js --commands "npm install" -R
```
