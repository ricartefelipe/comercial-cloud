package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movimentacao_estoque")
public class MovimentacaoEstoqueEntity extends TenantScopedEntity {

    @Column(name = "loja_id", nullable = false)
    public UUID lojaId;

    @Column(name = "produto_id", nullable = false)
    public UUID produtoId;

    @Column(name = "tipo", nullable = false, length = 50)
    public String tipo;

    @Column(name = "quantidade", nullable = false, precision = 15, scale = 3)
    public BigDecimal quantidade;

    @Column(name = "saldo_anterior", nullable = false, precision = 15, scale = 3)
    public BigDecimal saldoAnterior;

    @Column(name = "saldo_posterior", nullable = false, precision = 15, scale = 3)
    public BigDecimal saldoPosterior;

    @Column(name = "motivo")
    public String motivo;

    @Column(name = "referencia_id")
    public UUID referenciaId;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
