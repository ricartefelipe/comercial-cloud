package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "estoque")
public class EstoqueEntity extends TenantScopedEntity {

    @Column(name = "loja_id", nullable = false)
    public UUID lojaId;

    @Column(name = "produto_id", nullable = false)
    public UUID produtoId;

    @Column(name = "quantidade", nullable = false, precision = 15, scale = 3)
    public BigDecimal quantidade;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
