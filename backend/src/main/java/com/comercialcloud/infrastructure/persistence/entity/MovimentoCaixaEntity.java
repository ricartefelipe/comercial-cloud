package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movimento_caixa")
public class MovimentoCaixaEntity extends TenantScopedEntity {

    @Column(name = "loja_id", nullable = false)
    public UUID lojaId;

    @Column(name = "caixa_id", nullable = false)
    public UUID caixaId;

    @Column(name = "usuario_id", nullable = false)
    public UUID usuarioId;

    @Column(name = "tipo", nullable = false, length = 20)
    public String tipo;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    public BigDecimal valor;

    @Column(name = "motivo", length = 500)
    public String motivo;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
