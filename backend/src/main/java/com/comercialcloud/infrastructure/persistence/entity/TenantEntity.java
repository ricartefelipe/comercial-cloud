package com.comercialcloud.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant")
public class TenantEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "nome", nullable = false)
    public String nome;

    @Column(name = "ativo", nullable = false)
    public boolean ativo;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
