package com.comercialcloud.application.auditoria;

import com.comercialcloud.domain.model.Auditoria;
import com.comercialcloud.infrastructure.persistence.entity.AuditoriaEntity;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AuditoriaConsultaService {

    @Inject TenantContext tenantContext;

    public List<Auditoria> listar(int limit) {
        UUID tenantId = tenantContext.getTenantId();
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        List<AuditoriaEntity> rows = AuditoriaEntity.find("tenantId = ?1", tenantId).list();
        List<AuditoriaEntity> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparing((AuditoriaEntity a) -> a.createdAt).reversed());
        return sorted.stream().limit(safeLimit).map(this::toDomain).toList();
    }

    private Auditoria toDomain(AuditoriaEntity entity) {
        return new Auditoria(
                entity.id,
                entity.tenantId,
                entity.entidade,
                entity.entidadeId,
                entity.acao,
                entity.usuarioId,
                entity.detalhes,
                entity.createdAt);
    }
}
