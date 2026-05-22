package com.comercialcloud.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemVenda(UUID id, UUID produtoId, BigDecimal quantidade, BigDecimal precoUnitario, BigDecimal subtotal)
        {}
