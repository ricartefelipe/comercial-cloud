package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "assinatura_tenant")
public class AssinaturaTenantEntity extends TenantScopedEntity {

    @Column(name = "plano_id", nullable = false)
    public UUID planoId;

    @Column(name = "status", nullable = false, length = 20)
    public String status;

    @Column(name = "inicio", nullable = false)
    public LocalDate inicio;

    @Column(name = "proxima_cobranca", nullable = false)
    public LocalDate proximaCobranca;

    @Column(name = "trial_fim")
    public LocalDate trialFim;

    @Column(name = "tentativas_falha", nullable = false)
    public int tentativasFalha;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
