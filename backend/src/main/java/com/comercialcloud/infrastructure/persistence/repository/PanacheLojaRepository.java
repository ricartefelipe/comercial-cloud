package com.comercialcloud.infrastructure.persistence.repository;

import com.comercialcloud.domain.model.Loja;
import com.comercialcloud.domain.repository.LojaRepository;
import com.comercialcloud.infrastructure.persistence.entity.LojaEntity;
import com.comercialcloud.infrastructure.persistence.mapper.EntityMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheLojaRepository implements LojaRepository {

    @Inject EntityMapper mapper;

    @Override
    public Optional<Loja> find(UUID tenantId, UUID lojaId) {
        return LojaEntity.<LojaEntity>find("tenantId = ?1 and id = ?2", tenantId, lojaId).firstResultOptional()
                .map(mapper::toDomain);
    }
}
