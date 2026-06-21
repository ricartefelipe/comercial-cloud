package com.comercialcloud.infrastructure.security;

import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.domain.exception.PaymentRequiredException;
import com.comercialcloud.domain.repository.UsuarioRepository;
import com.comercialcloud.infrastructure.persistence.entity.TenantEntity;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;
import java.util.UUID;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class TenantRequestFilter implements ContainerRequestFilter {

    private static final Set<String> PUBLIC_PREFIXES = Set.of("health", "q/", "api/v1/public/", "api/v1/produtos/midias/");

    /**
     * Prefixos isentos da verificação de tenant suspenso, para que um tenant
     * inadimplente ainda consiga regularizar o pagamento (ver/pagar faturas).
     */
    private static final Set<String> BILLING_PREFIXES = Set.of("api/v1/billing");

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String USER_HEADER = "X-User-Id";

    @Inject
    TenantContext tenantContext;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    jakarta.enterprise.inject.Instance<JsonWebToken> jwt;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String path = ctx.getUriInfo().getPath();
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }
        final String normalizedPath = path == null ? "" : path;
        if (PUBLIC_PREFIXES.stream().anyMatch(normalizedPath::startsWith)) {
            return;
        }

        UUID tenantId = resolveTenantId(ctx);
        tenantContext.setTenantId(tenantId);

        boolean isentoSuspensao = BILLING_PREFIXES.stream().anyMatch(normalizedPath::startsWith);
        if (!isentoSuspensao) {
            TenantEntity tenant = TenantEntity.findById(tenantId);
            if (tenant != null && !tenant.ativo) {
                throw new PaymentRequiredException(
                        "TENANT_SUSPENSO",
                        "Assinatura suspensa por inadimplência. Regularize o pagamento em /api/v1/billing.");
            }
        }

        UUID userId = resolveUserId(ctx);
        if (userId != null) {
            tenantContext.setUserId(userId);
            usuarioRepository
                    .find(tenantId, userId)
                    .ifPresent(usuario -> tenantContext.setRole(usuario.role()));
        }
    }

    private UUID resolveTenantId(ContainerRequestContext ctx) {
        if (jwt.isResolvable()) {
            JsonWebToken token = jwt.get();
            Object claim = token.getClaim("tenant_id");
            if (claim != null && !claim.toString().isBlank()) {
                try {
                    return UUID.fromString(claim.toString().trim());
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("TENANT_INVALID", "Claim tenant_id deve ser um UUID válido");
                }
            }
        }

        String tenantHeader = ctx.getHeaderString(TENANT_HEADER);
        if (tenantHeader == null || tenantHeader.isBlank()) {
            throw new BusinessException("TENANT_REQUIRED", "Header X-Tenant-Id é obrigatório");
        }
        try {
            return UUID.fromString(tenantHeader.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("TENANT_INVALID", "Header X-Tenant-Id deve ser um UUID válido");
        }
    }

    private UUID resolveUserId(ContainerRequestContext ctx) {
        if (jwt.isResolvable()) {
            JsonWebToken token = jwt.get();
            Object claim = token.getClaim("app_user_id");
            if (claim != null && !claim.toString().isBlank()) {
                try {
                    return UUID.fromString(claim.toString().trim());
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("USER_INVALID", "Claim app_user_id deve ser um UUID válido");
                }
            }
        }

        String userHeader = ctx.getHeaderString(USER_HEADER);
        if (userHeader == null || userHeader.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(userHeader.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("USER_INVALID", "Header X-User-Id deve ser um UUID válido");
        }
    }
}
