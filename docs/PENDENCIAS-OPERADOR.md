# Pendências do operador

Itens que **dependem de você** (credenciais, domínio, contrato ou acesso externo). O restante do produto pode evoluir no repositório sem sua intervenção.

## Pagamentos (Asaas)

- [ ] Criar conta produção Asaas e gerar `ASAAS_API_KEY`
- [ ] Configurar webhook apontando para `https://api.<seu-dominio>/api/v1/public/webhooks/asaas`
- [ ] Validar checkout e confirmação de pagamento em ambiente real

## Fiscal (Focus NFe)

- [ ] Obter `FOCUSNFE_TOKEN` e dados da empresa (CNPJ, CSC, ambiente)
- [ ] Homologar emissão NFC-e/NFe no tenant piloto

## Autenticação (Keycloak)

- [ ] Subir Keycloak em produção (`auth.<seu-dominio>`)
- [ ] Ativar `COMERCIAL_CLOUD_KEYCLOAK_PROVISIONING_ENABLED=true`
- [ ] Ajustar realm, clientes web/API e redirect URIs do frontend

## E-mail transacional

- [ ] Definir SMTP (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`)
- [ ] Ativar `COMERCIAL_CLOUD_MAIL_ENABLED=true` e `COMERCIAL_CLOUD_MAIL_APP_URL=https://app.<seu-dominio>`
- [ ] Testar convites, boas-vindas e faturas na caixa real

## Infraestrutura

- [ ] VPS ou cloud com Docker Compose de produção (`docker-compose.prod.example.yml`)
- [ ] DNS: `app.`, `api.`, `auth.` + certificados HTTPS
- [ ] Cron de backup (`scripts/backup-db.sh`) e restore de teste
- [ ] Variáveis em `.env.prod` (nunca commitar secrets)

## Comercial / jurídico

- [ ] Revisão jurídica dos termos em `/termos` e `/privacidade`
- [ ] Contrato white-label com parceiro piloto (ver `docs/PILOTO-WHITELABEL.md`)
- [ ] Landing Kiwify: e-mail de contato e link de checkout definitivos

## O que já está pronto no código (v1.3+)

- Relatórios CSV (`/relatorios`) — vendas por período e estoque
- Convites de usuário (`/usuarios` → Convidar; aceite em `/convite/{token}`)
- Seed de estoque ao criar nova loja
- Branding dinâmico (`--brand-primary`) nos botões principais

Quando concluir um bloco acima, marque o checkbox e atualize o `.env` do ambiente de produção.
