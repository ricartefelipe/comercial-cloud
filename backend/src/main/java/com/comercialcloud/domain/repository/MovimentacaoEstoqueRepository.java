
package com.comercialcloud.domain.repository;

import com.comercialcloud.domain.model.MovimentacaoEstoque;

import java.util.List;
import java.util.UUID;

public interface MovimentacaoEstoqueRepository {

    List<MovimentacaoEstoque> listar(UUID tenantId, UUID lojaId, UUID produtoId);

    MovimentacaoEstoque save(MovimentacaoEstoque movimentacao);
}
