package com.comercialcloud.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "convite_usuario")
public class ConviteUsuarioEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "tenant_id", nullable = false)
    public UUID tenantId;

    @Column(name = "email", nullable = false)
    public String email;

    @Column(name = "nome", nullable = false)
    public String nome;

    @Column(name = "role", nullable = false, length = 30)
    public String role;

    @Column(name = "token", nullable = false, length = 64)
    public String token;

    @Column(name = "expira_em", nullable = false)
    public Instant expiraEm;

    @Column(name = "aceito_em")
    public Instant aceitoEm;

    @Column(name = "criado_por")
    public UUID criadoPor;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
