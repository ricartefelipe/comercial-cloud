package com.comercialcloud.domain.repository;

import com.comercialcloud.domain.model.Produto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProdutoRepository {

    Optional<Produto> find(UUID tenantId, UUID id);

    boolean existsSkuExclusive(UUID tenantId, String sku, UUID excludingId);

    boolean existsCodigoBarrasExclusive(UUID tenantId, String codigoBarras, UUID excludingId);

    Produto save(Produto produto);

    long count(UUID tenantId);

    List<Produto> page(UUID tenantId, int page, int size);

    List<Produto> buscarPorTermo(UUID tenantId, String termo);
}
