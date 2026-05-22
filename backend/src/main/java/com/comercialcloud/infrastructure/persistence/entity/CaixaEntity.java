package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "caixa")
public class CaixaEntity extends TenantScopedEntity {

    @Column(name = "loja_id", nullable = false)
    public UUID lojaId;

    @Column(name = "usuario_id", nullable = false)
    public UUID usuarioId;

    @Column(name = "status", nullable = false, length = 20)
    public String status;

    @Column(name = "valor_abertura", nullable = false, precision = 15, scale = 2)
    public BigDecimal valorAbertura;

    @Column(name = "valor_fechamento", precision = 15, scale = 2)
    public BigDecimal valorFechamento;

    @Column(name = "total_vendas", nullable = false, precision = 15, scale = 2)
    public BigDecimal totalVendas;

    @Column(name = "aberto_em", nullable = false)
    public Instant abertoEm;

    @Column(name = "fechado_em")
    public Instant fechadoEm;
}
