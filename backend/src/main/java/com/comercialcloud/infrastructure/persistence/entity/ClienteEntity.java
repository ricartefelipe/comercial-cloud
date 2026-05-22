package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "cliente")
public class ClienteEntity extends TenantScopedEntity {

    @Column(name = "nome", nullable = false)
    public String nome;

    @Column(name = "cpf_cnpj", length = 20)
    public String cpfCnpj;

    @Column(name = "email")
    public String email;

    @Column(name = "telefone", length = 30)
    public String telefone;

    @Column(name = "ativo", nullable = false)
    public boolean ativo;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
