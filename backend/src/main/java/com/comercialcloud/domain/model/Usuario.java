package com.comercialcloud.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Usuario(
        UUID id, UUID tenantId, String nome, String email, String role, boolean ativo, Instant createdAt) {}
