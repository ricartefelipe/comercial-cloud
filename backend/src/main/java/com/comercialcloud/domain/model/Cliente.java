package com.comercialcloud.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Cliente(
        UUID id,
        UUID tenantId,
        String nome,
        String cpfCnpj,
        String email,
        String telefone,
        boolean ativo,
        Instant createdAt) {}
