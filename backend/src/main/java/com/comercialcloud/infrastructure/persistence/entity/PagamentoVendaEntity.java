package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "pagamento_venda")
public class PagamentoVendaEntity extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id", nullable = false)
    public VendaEntity venda;

    @Column(name = "forma_pagamento", nullable = false, length = 50)
    public String formaPagamento;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    public BigDecimal valor;
}
