package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "conta_receber")
public class ContaReceberEntity extends TenantScopedEntity {

    @Column(name = "venda_id")
    public UUID vendaId;

    @Column(name = "cliente_id")
    public UUID clienteId;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    public BigDecimal valor;

    @Column(name = "vencimento", nullable = false)
    public LocalDate vencimento;

    @Column(name = "status", nullable = false, length = 20)
    public String status;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
