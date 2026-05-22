package com.comercialcloud.application.configuracao;

import com.comercialcloud.infrastructure.audit.AuditService;
import com.comercialcloud.infrastructure.persistence.entity.ConfiguracaoEntity;
import com.comercialcloud.infrastructure.security.TenantContext;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ConfiguracaoService {

    @Inject
    TenantContext tenantContext;

    @Inject
    AuditService auditService;

    public List<ConfiguracaoEntity> listarPorLoja(UUID lojaId) {
        UUID tenantId = tenantContext.getTenantId();
        if (lojaId == null) {
            return ConfiguracaoEntity.find(
                            "tenantId = ?1 and lojaId is null", Sort.by("chave"), tenantId)
                    .list();
        }
        return ConfiguracaoEntity.find("tenantId = ?1 and lojaId = ?2", Sort.by("chave"), tenantId, lojaId)
                .list();
    }

    @Transactional
    public ConfiguracaoEntity upsert(UUID lojaId, String chave, String valor) {
        UUID tenantId = tenantContext.getTenantId();
        ConfiguracaoEntity existente =
                lojaId == null
                        ? ConfiguracaoEntity.<ConfiguracaoEntity>find(
                                        "tenantId = ?1 and lojaId is null and chave = ?2", tenantId, chave)
                                .firstResult()
                        : ConfiguracaoEntity.<ConfiguracaoEntity>find(
                                        "tenantId = ?1 and lojaId = ?2 and chave = ?3", tenantId, lojaId, chave)
                                .firstResult();

        if (existente == null) {
            ConfiguracaoEntity nova = new ConfiguracaoEntity();
            nova.id = UUID.randomUUID();
            nova.tenantId = tenantId;
            nova.lojaId = lojaId;
            nova.chave = chave;
            nova.valor = valor;
            nova.updatedAt = Instant.now();
            nova.persist();
            auditService.registrar("Configuracao", nova.id, "CRIAR", chave);
            return nova;
        }

        existente.valor = valor;
        existente.updatedAt = Instant.now();
        existente.persist();
        auditService.registrar("Configuracao", existente.id, "ATUALIZAR", chave);
        return existente;
    }
}
