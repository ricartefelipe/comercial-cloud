# Checklist jurídico — ComercialCloud

Use com advogado antes de abrir comercialmente ou assinar white-label.

## Documentos no produto

| Página | URL piloto | Status |
|--------|------------|--------|
| Termos de uso | `/termos` | Rascunho técnico — revisar |
| Privacidade | `/privacidade` | Rascunho técnico — revisar |

## Pontos a validar

- [ ] Razão social e CNPJ do operador SaaS nos termos
- [ ] Base legal LGPD (execução de contrato, legítimo interesse, consentimento onde couber)
- [ ] Papéis: controlador (cliente/tenant) vs operador (ComercialCloud)
- [ ] Retenção e exclusão de dados ao cancelar assinatura
- [ ] Subprocessadores (AWS, Asaas, Focus NFe, Keycloak hospedado)
- [ ] Emissão fiscal: responsabilidade do lojista perante SEFAZ
- [ ] SLA, limitação de responsabilidade e foro
- [ ] Trial 14 dias e cobrança recorrente (CDC / direito de arrependimento se aplicável)
- [ ] White-label: licença de marca, suporte N1/N2, propriedade intelectual

## Contrato piloto

Ver `docs/PILOTO-WHITELABEL.md` — anexo com escopo, prazo, valor e dados demo.

## Após revisão

1. Atualizar textos em `frontend/src/app/termos/page.tsx` e `privacidade/page.tsx`
2. Data “Última atualização” coerente
3. Marcar seção **Comercial / jurídico** em `docs/PENDENCIAS-OPERADOR.md`
