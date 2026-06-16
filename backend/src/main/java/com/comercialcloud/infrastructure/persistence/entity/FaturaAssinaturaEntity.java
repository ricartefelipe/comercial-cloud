package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fatura_assinatura")
public class FaturaAssinaturaEntity extends TenantScopedEntity {

    @Column(name = "assinatura_id", nullable = false)
    public UUID assinaturaId;

    @Column(name = "competencia", nullable = false, length = 7)
    public String competencia;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    public BigDecimal valor;

    @Column(name = "vencimento", nullable = false)
    public LocalDate vencimento;

    @Column(name = "status", nullable = false, length = 20)
    public String status;

    @Column(name = "tentativas", nullable = false)
    public int tentativas;

    @Column(name = "transacao_ref", length = 100)
    public String transacaoRef;

    @Column(name = "motivo_recusa", length = 255)
    public String motivoRecusa;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "paga_em")
    public Instant pagaEm;
}
