package com.comercialcloud.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Estoque(
        UUID id, UUID tenantId, UUID lojaId, UUID produtoId, BigDecimal quantidade, Instant updatedAt) {}
