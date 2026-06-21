package com.comercialcloud.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "plano_assinatura")
public class PlanoAssinaturaEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "codigo", nullable = false, length = 30)
    public String codigo;

    @Column(name = "nome", nullable = false, length = 100)
    public String nome;

    @Column(name = "preco_mensal", nullable = false, precision = 15, scale = 2)
    public BigDecimal precoMensal;

    @Column(name = "ativo", nullable = false)
    public boolean ativo;

    @Column(name = "max_lojas", nullable = false)
    public int maxLojas = 1;
}
