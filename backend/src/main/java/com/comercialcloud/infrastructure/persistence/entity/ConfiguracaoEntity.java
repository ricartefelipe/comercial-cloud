package com.comercialcloud.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "configuracao")
public class ConfiguracaoEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "tenant_id", nullable = false)
    public UUID tenantId;

    @Column(name = "loja_id")
    public UUID lojaId;

    @Column(name = "chave", nullable = false, length = 255)
    public String chave;

    @Column(name = "valor", nullable = false, length = 2000)
    public String valor;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
