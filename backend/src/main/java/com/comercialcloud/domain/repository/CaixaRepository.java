
package com.comercialcloud.domain.repository;

import com.comercialcloud.domain.model.Caixa;

import java.util.Optional;
import java.util.UUID;

public interface CaixaRepository {

    Optional<Caixa> findAberto(UUID tenantId, UUID lojaId, UUID usuarioId);

    Optional<Caixa> find(UUID tenantId, UUID id);

    Caixa save(Caixa caixa);
}
