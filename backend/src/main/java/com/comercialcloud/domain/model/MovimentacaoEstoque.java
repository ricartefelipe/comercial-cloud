package com.comercialcloud.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MovimentacaoEstoque(
        UUID id,
        UUID tenantId,
        UUID lojaId,
        UUID produtoId,
        TipoMovimentacaoEstoque tipo,
        BigDecimal quantidade,
        BigDecimal saldoAnterior,
        BigDecimal saldoPosterior,
        String motivo,
        UUID referenciaId,
        Instant createdAt) {}
