package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "item_venda")
public class ItemVendaEntity extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id", nullable = false)
    public VendaEntity venda;

    @Column(name = "produto_id", nullable = false)
    public UUID produtoId;

    @Column(name = "quantidade", nullable = false, precision = 15, scale = 3)
    public BigDecimal quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 15, scale = 2)
    public BigDecimal precoUnitario;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    public BigDecimal subtotal;
}
