package com.comercialcloud.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Estoque(
        UUID id,
        UUID tenantId,
        UUID lojaId,
        UUID produtoId,
        BigDecimal quantidade,
        BigDecimal quantidadeMinima,
        Instant updatedAt) {

    public static final BigDecimal QUANTIDADE_MINIMA_PADRAO = new BigDecimal("10");
}
