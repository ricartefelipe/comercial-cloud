package com.comercialcloud.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Caixa(
        UUID id,
        UUID tenantId,
        UUID lojaId,
        UUID usuarioId,
        StatusCaixa status,
        BigDecimal valorAbertura,
        BigDecimal valorFechamento,
        BigDecimal totalVendas,
        Instant abertoEm,
        Instant fechadoEm) {}
