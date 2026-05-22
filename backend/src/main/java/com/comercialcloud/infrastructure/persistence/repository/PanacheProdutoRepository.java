package com.comercialcloud.infrastructure.persistence.repository;

import com.comercialcloud.domain.model.Produto;
import com.comercialcloud.domain.repository.ProdutoRepository;
import com.comercialcloud.infrastructure.persistence.entity.ProdutoEntity;
import com.comercialcloud.infrastructure.persistence.mapper.EntityMapper;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheProdutoRepository implements ProdutoRepository {

    @Inject
    EntityMapper mapper;

    @Override
    public Optional<Produto> find(UUID tenantId, UUID id) {
        return ProdutoEntity.<ProdutoEntity>find("tenantId = ?1 and id = ?2", tenantId, id)
                .firstResultOptional()
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsSkuExclusive(UUID tenantId, String sku, UUID excludingId) {
        if (excludingId == null) {
            return ProdutoEntity.<ProdutoEntity>find("tenantId = ?1 and sku = ?2", tenantId, sku).firstResult() != null;
        }
        return ProdutoEntity.<ProdutoEntity>find("tenantId = ?1 and sku = ?2 and id != ?3", tenantId, sku, excludingId)
                        .firstResult()
                != null;
    }

    @Override
    public boolean existsCodigoBarrasExclusive(UUID tenantId, String codigoBarras, UUID excludingId) {
        if (codigoBarras == null || codigoBarras.isBlank()) {
            return false;
        }
        if (excludingId == null) {
            return ProdutoEntity.<ProdutoEntity>find("tenantId = ?1 and codigoBarras = ?2", tenantId, codigoBarras).firstResult()
                    != null;
        }
        return ProdutoEntity.<ProdutoEntity>find(
                        "tenantId = ?1 and codigoBarras = ?2 and id != ?3",
                        tenantId,
                        codigoBarras,
                        excludingId)
                        .firstResult()
                != null;
    }

    @Override
    public Produto save(Produto produto) {
        ProdutoEntity managed =
                ProdutoEntity.<ProdutoEntity>find("tenantId = ?1 and id = ?2", produto.tenantId(), produto.id())
                        .firstResult();
        if (managed == null) {
            ProdutoEntity novo = mapper.toEntity(produto);
            novo.persist();
            return mapper.toDomain(novo);
        }
        ProdutoEntity payload = mapper.toEntity(produto);
        managed.sku = payload.sku;
        managed.codigoBarras = payload.codigoBarras;
        managed.nome = payload.nome;
        managed.descricao = payload.descricao;
        managed.preco = payload.preco;
        managed.ativo = payload.ativo;
        managed.updatedAt = payload.updatedAt;
        return mapper.toDomain(managed);
    }

    @Override
    public long count(UUID tenantId) {
        return ProdutoEntity.count("tenantId = ?1", tenantId);
    }

    @Override
    public List<Produto> page(UUID tenantId, int page, int size) {
        List<ProdutoEntity> rows = ProdutoEntity.<ProdutoEntity>find("tenantId = ?1", Sort.by("nome"), tenantId)
                .page(Page.of(page, Math.max(size, 1)))
                .list();
        return rows.stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Produto> buscarPorTermo(UUID tenantId, String termo) {
        if (termo == null || termo.isBlank()) {
            return ProdutoEntity.<ProdutoEntity>find("tenantId = ?1", Sort.by("nome"), tenantId).list().stream()
                    .map(mapper::toDomain)
                    .toList();
        }
        String like = "%" + termo.toLowerCase() + "%";
        String query =
                "tenantId = ?1 and (lower(nome) like ?2 or lower(sku) like ?2 or codigoBarras = ?3)";
        return ProdutoEntity.<ProdutoEntity>find(query, tenantId, like, termo).list().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
