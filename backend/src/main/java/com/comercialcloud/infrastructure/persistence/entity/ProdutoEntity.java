package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "produto")
public class ProdutoEntity extends TenantScopedEntity {

    @Column(name = "sku", nullable = false, length = 100)
    public String sku;

    @Column(name = "codigo_barras", length = 100)
    public String codigoBarras;

    @Column(name = "nome", nullable = false)
    public String nome;

    @Column(name = "descricao")
    public String descricao;

    @Column(name = "imagem_url", length = 500)
    public String imagemUrl;

    @Column(name = "categoria", length = 100)
    public String categoria;

    @Column(name = "preco", nullable = false, precision = 15, scale = 2)
    public BigDecimal preco;

    @Column(name = "preco_custo", precision = 15, scale = 2)
    public BigDecimal precoCusto;

    @Column(name = "controla_estoque", nullable = false)
    public boolean controlaEstoque;

    @Column(name = "ativo", nullable = false)
    public boolean ativo;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
