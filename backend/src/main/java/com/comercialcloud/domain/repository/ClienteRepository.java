
package com.comercialcloud.domain.repository;

import com.comercialcloud.domain.model.Cliente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository {

    Optional<Cliente> find(UUID tenantId, UUID id);

    Cliente save(Cliente cliente);

    List<Cliente> list(UUID tenantId, int page, int size);

    void delete(UUID tenantId, UUID id);
}
