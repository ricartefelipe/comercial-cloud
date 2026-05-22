
package com.comercialcloud.domain.repository;

import com.comercialcloud.domain.model.Estoque;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstoqueRepository {

    Optional<Estoque> findPorLojaProduto(UUID tenantId, UUID lojaId, UUID produtoId);

    List<Estoque> listPorLoja(UUID tenantId, UUID lojaId);

    List<Estoque> listPorQuantidadeMax(UUID tenantId, UUID lojaId, BigDecimal maxQuantidade);

    Estoque save(Estoque estoque);
}
