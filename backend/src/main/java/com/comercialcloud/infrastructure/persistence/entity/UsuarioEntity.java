package com.comercialcloud.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class UsuarioEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "tenant_id", nullable = false)
    public UUID tenantId;

    @Column(name = "nome", nullable = false)
    public String nome;

    @Column(name = "email", nullable = false)
    public String email;

    @Column(name = "role", nullable = false, length = 50)
    public String role;

    @Column(name = "ativo", nullable = false)
    public boolean ativo;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
