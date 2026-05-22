package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auditoria")
public class AuditoriaEntity extends TenantScopedEntity {

    @Column(name = "entidade", nullable = false, length = 100)
    public String entidade;

    @Column(name = "entidade_id", nullable = false)
    public UUID entidadeId;

    @Column(name = "acao", nullable = false, length = 50)
    public String acao;

    @Column(name = "usuario_id")
    public UUID usuarioId;

    @Column(name = "detalhes")
    public String detalhes;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
