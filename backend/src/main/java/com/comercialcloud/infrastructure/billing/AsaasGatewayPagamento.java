package com.comercialcloud.infrastructure.billing;

import com.comercialcloud.application.billing.GatewayPagamento;
import com.comercialcloud.domain.repository.UsuarioRepository;
import com.comercialcloud.infrastructure.persistence.entity.AssinaturaTenantEntity;
import com.comercialcloud.infrastructure.persistence.entity.FaturaAssinaturaEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Gateway de pagamento Asaas (PIX). Requer {@code comercial-cloud.billing.asaas.enabled=true}
 * e {@code comercial-cloud.billing.asaas.api-key} configurados. Cobranças pendentes retornam
 * URL de checkout; confirmação assíncrona via webhook {@code /api/v1/public/webhooks/asaas}.
 */
@ApplicationScoped
public class AsaasGatewayPagamento implements GatewayPagamento {

    private static final Logger LOG = Logger.getLogger(AsaasGatewayPagamento.class);

    @Inject AsaasClient asaasClient;
    @Inject UsuarioRepository usuarioRepository;

    @Override
    public String nome() {
        return "asaas";
    }

    @Override
    @Transactional
    public ResultadoCobranca cobrar(CobrancaRequest request) {
        if (!asaasClient.isConfigured()) {
            return ResultadoCobranca.recusado(
                    "Asaas não configurado. Defina billing.asaas.enabled=true e billing.asaas.api-key");
        }

        FaturaAssinaturaEntity fatura = FaturaAssinaturaEntity.findById(request.faturaId());
        if (fatura == null) {
            return ResultadoCobranca.recusado("Fatura não encontrada");
        }

        AssinaturaTenantEntity assinatura = AssinaturaTenantEntity.findById(fatura.assinaturaId);
        if (assinatura == null) {
            return ResultadoCobranca.recusado("Assinatura não encontrada");
        }

        try {
            String customerId = ensureCustomer(assinatura, request.tenantId());
            AsaasClient.PaymentResponse payment =
                    asaasClient.createPixPayment(
                            customerId,
                            request.valor(),
                            fatura.vencimento != null ? fatura.vencimento : LocalDate.now().plusDays(3),
                            request.descricao(),
                            request.faturaId().toString());

            fatura.gatewayPaymentId = payment.id;
            fatura.checkoutUrl = firstNonBlank(payment.invoiceUrl, payment.bankSlipUrl, payment.transactionReceiptUrl);

            if (isPaid(payment.status)) {
                return ResultadoCobranca.aprovado(payment.id);
            }

            return ResultadoCobranca.pendente(
                    payment.id,
                    fatura.checkoutUrl,
                    "Aguardando pagamento PIX. Acesse o link de checkout.");
        } catch (Exception e) {
            LOG.errorf(e, "Falha Asaas ao cobrar fatura %s", request.faturaId());
            return ResultadoCobranca.recusado("Asaas: " + e.getMessage());
        }
    }

    private String ensureCustomer(AssinaturaTenantEntity assinatura, UUID tenantId) throws Exception {
        if (assinatura.asaasCustomerId != null && !assinatura.asaasCustomerId.isBlank()) {
            return assinatura.asaasCustomerId;
        }

        String email = assinatura.billingEmail;
        String name = "Tenant " + tenantId.toString().substring(0, 8);
        if (email == null || email.isBlank()) {
            email =
                    usuarioRepository.list(tenantId).stream()
                            .filter(u -> "ADMIN".equalsIgnoreCase(u.role()))
                            .map(u -> u.email())
                            .findFirst()
                            .orElse(null);
            name =
                    usuarioRepository.list(tenantId).stream()
                            .filter(u -> "ADMIN".equalsIgnoreCase(u.role()))
                            .map(u -> u.nome())
                            .findFirst()
                            .orElse(name);
        }
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("E-mail de cobrança não configurado para o tenant");
        }

        String cnpj = assinatura.billingCnpj != null ? assinatura.billingCnpj : "00000000000191";
        AsaasClient.CustomerResponse customer =
                asaasClient.createCustomer(name, email, cnpj, tenantId.toString());
        assinatura.asaasCustomerId = customer.id;
        assinatura.billingEmail = email;
        return customer.id;
    }

    public static boolean isPaid(String status) {
        if (status == null) {
            return false;
        }
        return switch (status.toUpperCase()) {
            case "RECEIVED", "CONFIRMED", "RECEIVED_IN_CASH" -> true;
            default -> false;
        };
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
