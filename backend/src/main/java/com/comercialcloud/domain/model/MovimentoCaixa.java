package com.comercialcloud.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MovimentoCaixa(
        UUID id,
        UUID tenantId,
        UUID lojaId,
        UUID caixaId,
        UUID usuarioId,
        TipoMovimentoCaixa tipo,
        BigDecimal valor,
        String motivo,
        Instant createdAt) {}
