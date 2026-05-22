package com.comercialcloud.infrastructure.security;

import jakarta.enterprise.context.RequestScoped;

import java.util.Optional;
import java.util.UUID;

@RequestScoped
public class TenantContext {

    private UUID tenantId;
    private UUID userId;

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Optional<UUID> getUserId() {
        return Optional.ofNullable(userId);
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
