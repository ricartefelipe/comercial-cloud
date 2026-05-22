package com.comercialcloud.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Tenant(UUID id, String nome, boolean ativo, Instant createdAt) {}
