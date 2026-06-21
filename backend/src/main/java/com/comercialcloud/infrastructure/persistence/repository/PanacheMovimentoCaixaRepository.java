package com.comercialcloud.infrastructure.persistence.repository;

import com.comercialcloud.domain.model.MovimentoCaixa;
import com.comercialcloud.domain.repository.MovimentoCaixaRepository;
import com.comercialcloud.infrastructure.persistence.entity.MovimentoCaixaEntity;
import com.comercialcloud.infrastructure.persistence.mapper.EntityMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PanacheMovimentoCaixaRepository implements MovimentoCaixaRepository {

    @Inject EntityMapper mapper;

    @Inject EntityManager entityManager;

    @Override
    @Transactional
    public MovimentoCaixa save(MovimentoCaixa movimento) {
        MovimentoCaixaEntity entity = mapper.toEntity(movimento);
        MovimentoCaixaEntity merged = entityManager.merge(entity);
        entityManager.flush();
        return mapper.toDomain(merged);
    }

    @Override
    public List<MovimentoCaixa> listarPorCaixa(UUID tenantId, UUID caixaId) {
        List<MovimentoCaixaEntity> rows =
                MovimentoCaixaEntity.find("tenantId = ?1 and caixaId = ?2", tenantId, caixaId).list();
        List<MovimentoCaixaEntity> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparing((MovimentoCaixaEntity e) -> e.createdAt).reversed());
        return sorted.stream().map(mapper::toDomain).toList();
    }
}
