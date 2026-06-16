package com.comercialcloud.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Documento fiscal eletrônico associado a uma venda. Segue um ciclo de vida
 * PENDENTE -> AUTORIZADO/REJEITADO/ERRO -> CANCELADO, processado de forma
 * assíncrona (padrão outbox) para não acoplar a finalização da venda à
 * latência/indisponibilidade do provedor fiscal.
 */
public record DocumentoFiscal(
        UUID id,
        UUID tenantId,
        UUID lojaId,
        UUID vendaId,
        ModeloDocumentoFiscal modelo,
        StatusDocumentoFiscal status,
        Integer numero,
        Integer serie,
        String chaveAcesso,
        String protocolo,
        String motivoRejeicao,
        String urlDanfe,
        String providerRef,
        int tentativas,
        Instant createdAt,
        Instant updatedAt) {}
