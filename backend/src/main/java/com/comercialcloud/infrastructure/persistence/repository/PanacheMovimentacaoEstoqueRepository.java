package com.comercialcloud.infrastructure.persistence.repository;

import com.comercialcloud.domain.model.MovimentacaoEstoque;
import com.comercialcloud.domain.repository.MovimentacaoEstoqueRepository;
import com.comercialcloud.infrastructure.persistence.entity.MovimentacaoEstoqueEntity;
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
public class PanacheMovimentacaoEstoqueRepository implements MovimentacaoEstoqueRepository {

    @Inject EntityMapper mapper;
    @Inject EntityManager entityManager;

    @Override
    @Transactional
    public MovimentacaoEstoque save(MovimentacaoEstoque movimentacao) {
        MovimentacaoEstoqueEntity entity = mapper.toEntity(movimentacao);
        MovimentacaoEstoqueEntity merged = entityManager.merge(entity);
        entityManager.flush();
        return mapper.toDomain(merged);
    }

    @Override
    public List<MovimentacaoEstoque> listar(UUID tenantId, UUID lojaId, UUID produtoId) {
        List<MovimentacaoEstoqueEntity> rows;
        if (produtoId != null) {
            rows =
                    MovimentacaoEstoqueEntity.find(
                                    "tenantId = ?1 and lojaId = ?2 and produtoId = ?3",
                                    tenantId,
                                    lojaId,
                                    produtoId)
                            .list();
        } else {
            rows = MovimentacaoEstoqueEntity.find("tenantId = ?1 and lojaId = ?2", tenantId, lojaId).list();
        }
        List<MovimentacaoEstoqueEntity> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparing((MovimentacaoEstoqueEntity e) -> e.createdAt).reversed());
        return sorted.stream().map(me -> mapper.toDomain(me)).toList();
    }
}
