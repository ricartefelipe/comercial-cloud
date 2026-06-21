package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pix_cobranca_pdv")
public class PixCobrancaPdvEntity extends TenantScopedEntity {

    @Column(name = "venda_id", nullable = false)
    public UUID vendaId;

    @Column(name = "gateway_payment_id", length = 100)
    public String gatewayPaymentId;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    public BigDecimal valor;

    @Column(name = "status", nullable = false, length = 30)
    public String status;

    @Column(name = "checkout_url", length = 500)
    public String checkoutUrl;

    @Column(name = "pix_payload", columnDefinition = "text")
    public String pixPayload;

    @Column(name = "pix_encoded_image", columnDefinition = "text")
    public String pixEncodedImage;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
