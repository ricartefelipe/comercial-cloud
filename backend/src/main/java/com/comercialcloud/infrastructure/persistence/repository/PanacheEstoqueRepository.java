package com.comercialcloud.infrastructure.persistence.repository;

import com.comercialcloud.domain.model.Estoque;
import com.comercialcloud.domain.repository.EstoqueRepository;
import com.comercialcloud.infrastructure.persistence.entity.EstoqueEntity;
import com.comercialcloud.infrastructure.persistence.mapper.EntityMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheEstoqueRepository implements EstoqueRepository {

    @Inject EntityMapper mapper;
    @Inject EntityManager entityManager;

    @Override
    public Optional<Estoque> findPorLojaProduto(UUID tenantId, UUID lojaId, UUID produtoId) {
        return EstoqueEntity.<EstoqueEntity>find(
                        "tenantId = ?1 and lojaId = ?2 and produtoId = ?3", tenantId, lojaId, produtoId)
                .firstResultOptional()
                .map(ee -> mapper.toDomain(ee));
    }

    @Override
    @Transactional
    public Estoque save(Estoque estoque) {
        EstoqueEntity entity = mapper.toEntity(estoque);
        EstoqueEntity merged = entityManager.merge(entity);
        entityManager.flush();
        return mapper.toDomain(merged);
    }

    @Override
    public List<Estoque> listPorLoja(UUID tenantId, UUID lojaId) {
        return EstoqueEntity.<EstoqueEntity>find("tenantId = ?1 and lojaId = ?2 order by produtoId", tenantId, lojaId)
                .list()
                .stream()
                .map(ee -> mapper.toDomain(ee))
                .toList();
    }

    @Override
    public List<Estoque> listPorQuantidadeMax(UUID tenantId, UUID lojaId, BigDecimal quantidadeMax) {
        return EstoqueEntity.<EstoqueEntity>find(
                        "tenantId = ?1 and lojaId = ?2 and quantidade <= ?3 order by quantidade",
                        tenantId,
                        lojaId,
                        quantidadeMax)
                .list()
                .stream()
                .map(ee -> mapper.toDomain(ee))
                .toList();
    }
}
