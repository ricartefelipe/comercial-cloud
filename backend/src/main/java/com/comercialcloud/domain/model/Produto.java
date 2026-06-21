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
        String imagemUrl,
        String categoria,
        BigDecimal preco,
        BigDecimal precoCusto,
        boolean controlaEstoque,
        boolean ativo,
        Instant createdAt,
        Instant updatedAt) {}