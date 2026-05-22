package com.comercialcloud.infrastructure.persistence.repository;

import com.comercialcloud.domain.model.Usuario;
import com.comercialcloud.domain.repository.UsuarioRepository;
import com.comercialcloud.infrastructure.persistence.entity.UsuarioEntity;
import com.comercialcloud.infrastructure.persistence.mapper.EntityMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheUsuarioRepository implements UsuarioRepository {

    @Inject EntityMapper mapper;

    @Override
    public Optional<Usuario> find(UUID tenantId, UUID id) {
        return UsuarioEntity.<UsuarioEntity>find("tenantId = ?1 and id = ?2", tenantId, id).firstResultOptional()
                .map(mapper::toDomain);
    }
}
