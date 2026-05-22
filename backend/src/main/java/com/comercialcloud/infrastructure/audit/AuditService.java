package com.comercialcloud.infrastructure.audit;
import com.comercialcloud.infrastructure.persistence.entity.AuditoriaEntity;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant; import java.util.UUID;
@ApplicationScoped
public class AuditService {
    @Inject TenantContext tenantContext;
    @Transactional
    public void registrar(String entidade, UUID entidadeId, String acao, String detalhes) {
        UUID tenantId = tenantContext.getTenantId(); if (tenantId == null) return;
        AuditoriaEntity a = new AuditoriaEntity(); a.id = UUID.randomUUID(); a.tenantId = tenantId;
        a.entidade = entidade; a.entidadeId = entidadeId; a.acao = acao; a.usuarioId = tenantContext.getUserId().orElse(null);
        a.detalhes = detalhes; a.createdAt = Instant.now(); a.persist();
    }
}
