package com.comercialcloud.infrastructure.persistence.repository;

import com.comercialcloud.domain.model.Loja;
import com.comercialcloud.domain.repository.LojaRepository;
import com.comercialcloud.infrastructure.persistence.entity.LojaEntity;
import com.comercialcloud.infrastructure.persistence.mapper.EntityMapper;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheLojaRepository implements LojaRepository {

    @Inject EntityMapper mapper;

    @Override
    public Optional<Loja> find(UUID tenantId, UUID lojaId) {
        return LojaEntity.<LojaEntity>find("tenantId = ?1 and id = ?2", tenantId, lojaId)
                .firstResultOptional()
                .map(mapper::toDomain);
    }

    @Override
    public List<Loja> listByTenant(UUID tenantId) {
        return LojaEntity.<LojaEntity>find("tenantId = ?1", Sort.by("nome"), tenantId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countAtivas(UUID tenantId) {
        return LojaEntity.count("tenantId = ?1 and ativa = true", tenantId);
    }

    @Override
    public Loja save(Loja loja) {
        LojaEntity entity =
                LojaEntity.<LojaEntity>findByIdOptional(loja.id())
                        .orElseGet(LojaEntity::new);
        entity.id = loja.id();
        entity.tenantId = loja.tenantId();
        entity.nome = loja.nome();
        entity.ativa = loja.ativa();
        entity.createdAt = loja.createdAt();
        entity.persist();
        return mapper.toDomain(entity);
    }
}
