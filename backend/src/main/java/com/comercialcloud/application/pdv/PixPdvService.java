package com.comercialcloud.application.pdv;

import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.domain.model.StatusVenda;
import com.comercialcloud.domain.model.Venda;
import com.comercialcloud.domain.repository.UsuarioRepository;
import com.comercialcloud.domain.repository.VendaRepository;
import com.comercialcloud.infrastructure.billing.AsaasClient;
import com.comercialcloud.infrastructure.billing.AsaasGatewayPagamento;
import com.comercialcloud.infrastructure.persistence.entity.AssinaturaTenantEntity;
import com.comercialcloud.infrastructure.persistence.entity.PixCobrancaPdvEntity;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@ApplicationScoped
public class PixPdvService {

    public record PixCobrancaResult(
            UUID id,
            UUID vendaId,
            BigDecimal valor,
            String status,
            String checkoutUrl,
            String pixPayload,
            String pixEncodedImage,
            String gatewayPaymentId) {}

    @Inject TenantContext tenantContext;

    @Inject VendaRepository vendaRepository;

    @Inject UsuarioRepository usuarioRepository;

    @Inject AsaasClient asaasClient;

    @Inject
    @ConfigProperty(name = "comercial-cloud.pdv.pix.enabled", defaultValue = "true")
    boolean pixPdvEnabled;

    public boolean pixDisponivel() {
        return pixPdvEnabled && asaasClient.isConfigured();
    }

    @Transactional
    public PixCobrancaResult criarCobranca(UUID vendaId, BigDecimal valor) {
        if (!pixDisponivel()) {
            throw new BusinessException(
                    "PIX_INDISPONIVEL",
                    "PIX dinâmico indisponível. Configure Asaas ou registre o pagamento manualmente.");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("VALOR_INVALIDO", "Valor do PIX deve ser maior que zero");
        }

        UUID tenantId = tenantContext.getTenantId();
        Venda venda = vendaRepository
                .findComItensPagamentos(tenantId, vendaId)
                .orElseThrow(() -> new NotFoundException("VENDA_NOT_FOUND", "Venda não encontrada"));
        if (venda.status() != StatusVenda.ABERTA) {
            throw new BusinessException("VENDA_INVALIDA", "Só é possível gerar PIX para vendas abertas");
        }

        try {
            String customerId = ensureCustomer(tenantId);
            AsaasClient.PaymentResponse payment =
                    asaasClient.createPixPayment(
                            customerId,
                            valor,
                            LocalDate.now(),
                            "Venda PDV " + vendaId.toString().substring(0, 8),
                            "pdv-" + vendaId);

            AsaasClient.PixQrCodeResponse qr = asaasClient.getPixQrCode(payment.id);
            Instant now = Instant.now();
            PixCobrancaPdvEntity entity = new PixCobrancaPdvEntity();
            entity.id = UUID.randomUUID();
            entity.tenantId = tenantId;
            entity.vendaId = vendaId;
            entity.gatewayPaymentId = payment.id;
            entity.valor = valor;
            entity.status = "PENDENTE";
            entity.checkoutUrl = firstNonBlank(payment.invoiceUrl, payment.bankSlipUrl);
            entity.pixPayload = qr.payload;
            entity.pixEncodedImage = qr.encodedImage;
            entity.createdAt = now;
            entity.updatedAt = now;
            entity.persist();

            return toResult(entity);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("PIX_ERRO", "Falha ao gerar cobrança PIX: " + e.getMessage());
        }
    }

    @Transactional
    public PixCobrancaResult consultarStatus(UUID cobrancaId) {
        UUID tenantId = tenantContext.getTenantId();
        PixCobrancaPdvEntity entity =
                PixCobrancaPdvEntity.<PixCobrancaPdvEntity>find("tenantId = ?1 and id = ?2", tenantId, cobrancaId)
                        .firstResultOptional()
                        .orElseThrow(() -> new NotFoundException("PIX_NOT_FOUND", "Cobrança PIX não encontrada"));

        if ("PAGO".equals(entity.status) || entity.gatewayPaymentId == null) {
            return toResult(entity);
        }

        try {
            AsaasClient.PaymentResponse payment = asaasClient.getPayment(entity.gatewayPaymentId);
            if (AsaasGatewayPagamento.isPaid(payment.status)) {
                entity.status = "PAGO";
                entity.updatedAt = Instant.now();
            }
        } catch (Exception ignored) {
            // mantém pendente
        }
        return toResult(entity);
    }

    private String ensureCustomer(UUID tenantId) throws Exception {
        AssinaturaTenantEntity assinatura =
                AssinaturaTenantEntity.<AssinaturaTenantEntity>find("tenantId", tenantId).firstResult();
        if (assinatura != null
                && assinatura.asaasCustomerId != null
                && !assinatura.asaasCustomerId.isBlank()) {
            return assinatura.asaasCustomerId;
        }

        String email =
                usuarioRepository.list(tenantId).stream()
                        .filter(u -> "ADMIN".equalsIgnoreCase(u.role()))
                        .map(u -> u.email())
                        .findFirst()
                        .orElse("pdv@" + tenantId.toString().substring(0, 8) + ".local");
        String name =
                usuarioRepository.list(tenantId).stream()
                        .filter(u -> "ADMIN".equalsIgnoreCase(u.role()))
                        .map(u -> u.nome())
                        .findFirst()
                        .orElse("Loja PDV");

        AsaasClient.CustomerResponse customer =
                asaasClient.createCustomer(name, email, "00000000000191", "pdv-tenant-" + tenantId);
        if (assinatura != null) {
            assinatura.asaasCustomerId = customer.id;
        }
        return customer.id;
    }

    private static PixCobrancaResult toResult(PixCobrancaPdvEntity entity) {
        return new PixCobrancaResult(
                entity.id,
                entity.vendaId,
                entity.valor,
                entity.status,
                entity.checkoutUrl,
                entity.pixPayload,
                entity.pixEncodedImage,
                entity.gatewayPaymentId);
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
