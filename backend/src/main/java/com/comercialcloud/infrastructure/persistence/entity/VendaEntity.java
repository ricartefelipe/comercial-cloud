package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "venda")
public class VendaEntity extends TenantScopedEntity {

    @Column(name = "loja_id", nullable = false)
    public UUID lojaId;

    @Column(name = "cliente_id")
    public UUID clienteId;

    @Column(name = "usuario_id", nullable = false)
    public UUID usuarioId;

    @Column(name = "caixa_id")
    public UUID caixaId;

    @Column(name = "status", nullable = false, length = 20)
    public String status;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    public BigDecimal subtotal;

    @Column(name = "desconto", nullable = false, precision = 15, scale = 2)
    public BigDecimal desconto;

    @Column(name = "total", nullable = false, precision = 15, scale = 2)
    public BigDecimal total;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "finalized_at")
    public Instant finalizedAt;

    @Column(name = "canceled_at")
    public Instant canceledAt;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    public List<ItemVendaEntity> itens = new ArrayList<>();

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    public List<PagamentoVendaEntity> pagamentos = new ArrayList<>();
}
