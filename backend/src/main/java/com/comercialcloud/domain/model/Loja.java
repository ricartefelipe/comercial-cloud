package com.comercialcloud.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Loja(UUID id, UUID tenantId, String nome, boolean ativa, Instant createdAt) {}
