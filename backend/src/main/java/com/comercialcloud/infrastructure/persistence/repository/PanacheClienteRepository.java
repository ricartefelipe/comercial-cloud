package com.comercialcloud.infrastructure.persistence.repository;

import com.comercialcloud.domain.model.Cliente;
import com.comercialcloud.domain.repository.ClienteRepository;
import com.comercialcloud.infrastructure.persistence.entity.ClienteEntity;
import com.comercialcloud.infrastructure.persistence.mapper.EntityMapper;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheClienteRepository implements ClienteRepository {

    @Inject
    EntityMapper mapper;

    @Override
    public Optional<Cliente> find(UUID tenantId, UUID id) {
        return ClienteEntity.<ClienteEntity>find("tenantId = ?1 and id = ?2", tenantId, id)
                .firstResultOptional()
                .map(mapper::toDomain);
    }

    @Override
    public Cliente save(Cliente cliente) {
        ClienteEntity managed =
                ClienteEntity.<ClienteEntity>find("tenantId = ?1 and id = ?2", cliente.tenantId(), cliente.id())
                        .firstResult();
        if (managed == null) {
            ClienteEntity novo = mapper.toEntity(cliente);
            novo.persist();
            return mapper.toDomain(novo);
        }
        ClienteEntity payload = mapper.toEntity(cliente);
        managed.nome = payload.nome;
        managed.cpfCnpj = payload.cpfCnpj;
        managed.email = payload.email;
        managed.telefone = payload.telefone;
        managed.ativo = payload.ativo;
        return mapper.toDomain(managed);
    }

    @Override
    public List<Cliente> list(UUID tenantId, int page, int size) {
        return ClienteEntity.<ClienteEntity>find("tenantId = ?1", Sort.by("nome"), tenantId)
                .page(Page.of(page, Math.max(size, 1)))
                .list()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(UUID tenantId, UUID id) {
        ClienteEntity.<ClienteEntity>find("tenantId = ?1 and id = ?2", tenantId, id)
                .firstResultOptional()
                .ifPresent(ClienteEntity::delete);
    }
}
