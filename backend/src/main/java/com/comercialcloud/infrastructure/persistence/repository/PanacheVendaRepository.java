package com.comercialcloud.infrastructure.persistence.repository;

import com.comercialcloud.domain.model.StatusVenda;
import com.comercialcloud.domain.model.Venda;
import com.comercialcloud.domain.repository.VendaRepository;
import com.comercialcloud.infrastructure.persistence.entity.VendaEntity;
import com.comercialcloud.infrastructure.persistence.mapper.EntityMapper;

import io.quarkus.panache.common.Page;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheVendaRepository implements VendaRepository {

    @Inject EntityMapper mapper;
    @Inject EntityManager entityManager;

    @Override
    @Transactional
    public Venda save(Venda venda) {
        Optional<VendaEntity> existente =
                VendaEntity.<VendaEntity>find("tenantId = ?1 and id = ?2", venda.tenantId(), venda.id()).firstResultOptional();

        if (existente.isEmpty()) {
            VendaEntity novo = mapper.toEntityNovo(venda);
            entityManager.persist(novo);
            entityManager.flush();
            return mapper.toDomain(novo);
        }

        VendaEntity gerenciada = existente.get();
        mapper.sincronizarVendaGerenciada(gerenciada, venda);
        entityManager.merge(gerenciada);
        entityManager.flush();
        return mapper.toDomain(gerenciada);
    }

    @Override
    public Optional<Venda> findComItensPagamentos(UUID tenantId, UUID vendaId) {
        return VendaEntity.<VendaEntity>find("tenantId = ?1 and id = ?2", tenantId, vendaId).firstResultOptional()
                .map(ve -> mapper.toDomain(ve));
    }

    @Override
    public List<Venda> listar(UUID tenantId, UUID lojaId, Instant dataInicio, Instant dataFim) {
        return VendaEntity.<VendaEntity>find(
                        "tenantId = ?1 and lojaId = ?2"
                                + " and (?3 is null or createdAt >= ?3)"
                                + " and (?4 is null or createdAt < ?4)"
                                + " order by createdAt desc",
                        tenantId,
                        lojaId,
                        dataInicio,
                        dataFim)
                .list()
                .stream()
                .map(ve -> mapper.toDomain(ve))
                .toList();
    }

    @Override
    public List<Venda> listFinalizadasPorCaixaComDetalhes(UUID tenantId, UUID caixaId) {
        return VendaEntity.<VendaEntity>find(
                        "tenantId = ?1 and caixaId = ?2 and status = ?3 order by finalizedAt",
                        tenantId,
                        caixaId,
                        StatusVenda.FINALIZADA.name())
                .list()
                .stream()
                .map(ve -> mapper.toDomain(ve))
                .toList();
    }

    @Override
    public List<Venda> listFinalizadasNoPeriodo(UUID tenantId, UUID lojaId, Instant inicio, Instant fim) {
        return VendaEntity.<VendaEntity>find(
                        "tenantId = ?1 and lojaId = ?2 and status = ?3 and finalizedAt >= ?4 and finalizedAt < ?5",
                        tenantId,
                        lojaId,
                        StatusVenda.FINALIZADA.name(),
                        inicio,
                        fim)
                .list()
                .stream()
                .map(ve -> mapper.toDomain(ve))
                .toList();
    }

    @Override
    public List<Venda> listUltimasCompleto(UUID tenantId, UUID lojaId, int limite) {
        List<VendaEntity> page =
                VendaEntity.<VendaEntity>find(
                                "tenantId = ?1 and lojaId = ?2 and status = ?3 order by finalizedAt desc",
                                tenantId,
                                lojaId,
                                StatusVenda.FINALIZADA.name())
                        .page(Page.of(0, Math.max(limite, 1)))
                        .list();
        return page.stream().map(ve -> mapper.toDomain(ve)).toList();
    }
}
