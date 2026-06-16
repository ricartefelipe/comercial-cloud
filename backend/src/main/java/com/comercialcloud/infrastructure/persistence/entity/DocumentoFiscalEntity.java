package com.comercialcloud.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documento_fiscal")
public class DocumentoFiscalEntity extends TenantScopedEntity {

    @Column(name = "loja_id", nullable = false)
    public UUID lojaId;

    @Column(name = "venda_id", nullable = false)
    public UUID vendaId;

    @Column(name = "modelo", nullable = false, length = 10)
    public String modelo;

    @Column(name = "status", nullable = false, length = 20)
    public String status;

    @Column(name = "numero")
    public Integer numero;

    @Column(name = "serie")
    public Integer serie;

    @Column(name = "chave_acesso", length = 44)
    public String chaveAcesso;

    @Column(name = "protocolo", length = 60)
    public String protocolo;

    @Column(name = "motivo_rejeicao", length = 500)
    public String motivoRejeicao;

    @Column(name = "url_danfe", length = 500)
    public String urlDanfe;

    @Column(name = "provider_ref", length = 100)
    public String providerRef;

    @Column(name = "tentativas", nullable = false)
    public int tentativas;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
