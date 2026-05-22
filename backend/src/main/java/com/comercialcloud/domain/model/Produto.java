package com.comercialcloud.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Produto(
        UUID id,
        UUID tenantId,
        String sku,
        String codigoBarras,
        String nome,
        String descricao,
        BigDecimal preco,
        boolean ativo,
        Instant createdAt,
        Instant updatedAt) {}
