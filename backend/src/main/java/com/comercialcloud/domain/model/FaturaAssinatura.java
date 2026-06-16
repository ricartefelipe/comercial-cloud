package com.comercialcloud.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FaturaAssinatura(
        UUID id,
        UUID tenantId,
        UUID assinaturaId,
        String competencia,
        BigDecimal valor,
        LocalDate vencimento,
        StatusFatura status,
        int tentativas,
        Instant createdAt,
        Instant pagaEm) {}
