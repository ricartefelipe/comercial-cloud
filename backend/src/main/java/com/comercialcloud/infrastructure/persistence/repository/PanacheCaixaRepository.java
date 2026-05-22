package com.comercialcloud.infrastructure.persistence.repository;

import com.comercialcloud.domain.model.Caixa;
import com.comercialcloud.domain.model.StatusCaixa;
import com.comercialcloud.domain.repository.CaixaRepository;
import com.comercialcloud.infrastructure.persistence.entity.CaixaEntity;
import com.comercialcloud.infrastructure.persistence.mapper.EntityMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheCaixaRepository implements CaixaRepository {

    @Inject EntityMapper mapper;

    @Inject EntityManager entityManager;

    @Override
    public Optional<Caixa> find(UUID tenantId, UUID id) {
        return CaixaEntity.<CaixaEntity>find("tenantId = ?1 and id = ?2", tenantId, id).firstResultOptional()
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Caixa> findAberto(UUID tenantId, UUID lojaId, UUID usuarioId) {
        return CaixaEntity.<CaixaEntity>find(
                        "tenantId = ?1 and lojaId = ?2 and usuarioId = ?3 and status = ?4",
                        tenantId,
                        lojaId,
                        usuarioId,
                        StatusCaixa.ABERTO.name())
                .firstResultOptional()
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Caixa save(Caixa caixa) {
        CaixaEntity entity = mapper.toEntity(caixa);
        CaixaEntity merged = entityManager.merge(entity);
        entityManager.flush();
        return mapper.toDomain(merged);
    }
}
