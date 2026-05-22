package com.comercialcloud.infrastructure.security;

import jakarta.enterprise.context.RequestScoped;

import java.util.Optional;
import java.util.UUID;

@RequestScoped
public class TenantContext {

    private UUID tenantId;
    private UUID userId;
    private String role;

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

    public Optional<String> getRole() {
        return Optional.ofNullable(role);
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
