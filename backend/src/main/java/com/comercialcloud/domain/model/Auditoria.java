package com.comercialcloud.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Auditoria(
        UUID id,
        UUID tenantId,
        String entidade,
        UUID entidadeId,
        String acao,
        UUID usuarioId,
        String detalhes,
        Instant createdAt) {}
