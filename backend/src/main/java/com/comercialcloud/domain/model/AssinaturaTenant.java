package com.comercialcloud.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AssinaturaTenant(
        UUID id,
        UUID tenantId,
        UUID planoId,
        StatusAssinatura status,
        LocalDate inicio,
        LocalDate proximaCobranca,
        LocalDate trialFim,
        int tentativasFalha,
        Instant createdAt,
        Instant updatedAt) {}
