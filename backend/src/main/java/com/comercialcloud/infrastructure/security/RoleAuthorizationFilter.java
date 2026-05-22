package com.comercialcloud.infrastructure.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import java.util.Set;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class RoleAuthorizationFilter implements ContainerRequestFilter {

    private static final Set<String> PROTECTED_PREFIXES = Set.of("api/v1/usuarios", "api/v1/configuracoes");

    @Inject
    TenantContext tenantContext;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String path = ctx.getUriInfo().getPath();
        if (PROTECTED_PREFIXES.stream().noneMatch(path::startsWith)) {
            return;
        }

        String method = ctx.getMethod();
        if (!isWriteMethod(method)) {
            return;
        }

        String role = tenantContext.getRole().orElse(null);
        if ("CAIXA".equalsIgnoreCase(role)) {
            throw new ForbiddenException("Operação não permitida para perfil CAIXA");
        }

        if (path.startsWith("api/v1/usuarios") && "POST".equalsIgnoreCase(method) && !tenantContext.isAdmin()) {
            throw new ForbiddenException("Apenas ADMIN pode criar usuários");
        }
    }

    private static boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }
}
