package com.comercialcloud.infrastructure.security;

import com.comercialcloud.domain.exception.BusinessException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import java.util.Set;
import java.util.UUID;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class TenantRequestFilter implements ContainerRequestFilter {

    private static final Set<String> PUBLIC_PREFIXES = Set.of("health", "q/");

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String USER_HEADER = "X-User-Id";

    @Inject
    TenantContext tenantContext;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String path = ctx.getUriInfo().getPath();
        if (PUBLIC_PREFIXES.stream().anyMatch(path::startsWith)) {
            return;
        }
        String tenantHeader = ctx.getHeaderString(TENANT_HEADER);
        if (tenantHeader == null || tenantHeader.isBlank()) {
            throw new BusinessException("TENANT_REQUIRED", "Header X-Tenant-Id é obrigatório");
        }
        try {
            tenantContext.setTenantId(UUID.fromString(tenantHeader.trim()));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("TENANT_INVALID", "Header X-Tenant-Id deve ser um UUID válido");
        }
        String userHeader = ctx.getHeaderString(USER_HEADER);
        if (userHeader != null && !userHeader.isBlank()) {
            try {
                tenantContext.setUserId(UUID.fromString(userHeader.trim()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("USER_INVALID", "Header X-User-Id deve ser um UUID válido");
            }
        }
    }
}
