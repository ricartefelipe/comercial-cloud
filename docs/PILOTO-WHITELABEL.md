# Playbook — Piloto White-label ComercialCloud

Roteiro para fechar o **primeiro parceiro** (software house ou contabilidade) com risco controlado.

---

## 1. Perfil ideal do parceiro

- Já atende lojistas (PDV, contabilidade ou ERP regional)
- Capacidade de suporte 1º nível (telefone/WhatsApp)
- Disposto a homologar Asaas + Focus NFe **antes** de revender

---

## 2. Entrega ao parceiro

| Item | Onde |
|------|------|
| Código taggeado | `git checkout v1.x.x` |
| Pacote zip | `scripts/package-dist.sh` → `dist/comercial-cloud.zip` |
| Deploy | [`DEPLOY-PRODUCAO.md`](./DEPLOY-PRODUCAO.md) |
| Comercial | [`README-COMERCIAL.md`](../README-COMERCIAL.md) |
| Licença | [`LICENSE`](../LICENSE) |

---

## 3. Cronograma sugerido (4 semanas)

| Semana | Atividade |
|--------|-----------|
| 1 | Kick-off, domínios, Compose homologação (`docker-compose.homolog.yml`) |
| 2 | Checklist §5 DEPLOY — billing sandbox + Focus homologação + Keycloak |
| 3 | Treinamento 2h (PDV, retaguarda, fiscal, assinatura) + branding parceiro |
| 4 | 1 loja piloto do parceiro em produção; retrospectiva |

---

## 4. Homologação técnica (obrigatória)

```bash
docker compose -f docker-compose.yml -f docker-compose.homolog.yml --profile keycloak up -d
./scripts/verify-homologacao.sh
```

Validar manualmente:

- [ ] Cadastro trial + e-mail boas-vindas (MailHog :8025)
- [ ] Fatura + checkout Asaas sandbox + webhook
- [ ] NFC-e homologação Focus + cancelamento
- [ ] Login Keycloak pós-onboarding

---

## 5. Matriz de responsabilidade

| Tema | Parceiro | Você (fornecedor) |
|------|----------|-------------------|
| Suporte lojista 1º nível | ✅ | — |
| Bug plataforma / 2º nível | — | ✅ |
| Credenciamento SEFAZ / certificado | Orienta lojista | Documentação |
| Conta Asaas / Focus do lojista | Parceiro ou lojista | Adapter técnico |
| Infra (VPS, backup) | Parceiro ou lojista | Guia deploy |

---

## 6. Comercial sugerido

| Item | Faixa |
|------|-------|
| Licença white-label + implantação | R$ 15k–40k |
| Manutenção mensual | R$ 500–2k |
| Customização por loja | R$ 5k–15k |

SaaS direto ao lojista: adiar até 1 piloto white-label estável.

---

## 7. Critérios de go-live produção

- `./scripts/backup-postgres.sh` agendado (cron diário)
- HTTPS em api + app + auth
- `/cadastro` acessível no domínio do operador SaaS (`APP_URL`)
- `/termos` e `/privacidade` acessíveis no mesmo domínio do app
- 1 NFC-e autorizada em homologação
- 1 pagamento Asaas sandbox confirmado via webhook

---

## 8. Pós-piloto

- Coletar feedback (PDV, fiscal, billing)
- Definir canal de suporte (e-mail/Slack)
- Roadmap v1.3 (relatórios avançados, app mobile) só após receita do piloto
