package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "configuracao_fiscal")
public class ConfiguracaoFiscalEntity extends TenantScopedEntity {

    @Column(name = "loja_id", nullable = false)
    public UUID lojaId;

    @Column(name = "cnpj", nullable = false, length = 14)
    public String cnpj;

    @Column(name = "inscricao_estadual", length = 20)
    public String inscricaoEstadual;

    @Column(name = "razao_social", nullable = false, length = 255)
    public String razaoSocial;

    @Column(name = "regime_tributario", nullable = false, length = 30)
    public String regimeTributario;

    @Column(name = "ambiente", nullable = false, length = 20)
    public String ambiente;

    @Column(name = "uf", nullable = false, length = 2)
    public String uf;

    @Column(name = "csc_id", length = 10)
    public String cscId;

    @Column(name = "csc", length = 100)
    public String csc;

    @Column(name = "serie_nfce", nullable = false)
    public int serieNfce;

    @Column(name = "proximo_numero_nfce", nullable = false)
    public long proximoNumeroNfce;

    @Column(name = "habilitado", nullable = false)
    public boolean habilitado;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
