
package com.comercialcloud.domain.repository;

import com.comercialcloud.domain.model.Venda;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendaRepository {

    Optional<Venda> findComItensPagamentos(UUID tenantId, UUID id);

    Venda save(Venda venda);

    List<Venda> listar(UUID tenantId, UUID lojaId, Instant dataInicio, Instant dataFim);

    List<Venda> listFinalizadasPorCaixaComDetalhes(UUID tenantId, UUID caixaId);

    List<Venda> listFinalizadasNoPeriodo(UUID tenantId, UUID lojaId, Instant inicio, Instant fim);

    List<Venda> listUltimasCompleto(UUID tenantId, UUID lojaId, int limit);
}
