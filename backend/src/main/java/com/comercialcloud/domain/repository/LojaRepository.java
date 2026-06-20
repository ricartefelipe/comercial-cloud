
package com.comercialcloud.domain.repository;

import com.comercialcloud.domain.model.Loja;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LojaRepository {

    Optional<Loja> find(UUID tenantId, UUID id);

    List<Loja> listByTenant(UUID tenantId);

    long countAtivas(UUID tenantId);

    Loja save(Loja loja);
}
