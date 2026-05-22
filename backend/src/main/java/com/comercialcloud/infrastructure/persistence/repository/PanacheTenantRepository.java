package com.comercialcloud.infrastructure.persistence.repository;

import com.comercialcloud.domain.model.Tenant;
import com.comercialcloud.domain.repository.TenantRepository;
import com.comercialcloud.infrastructure.persistence.entity.TenantEntity;
import com.comercialcloud.infrastructure.persistence.mapper.EntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheTenantRepository implements TenantRepository {

    @Inject
    EntityMapper mapper;

    @Override
    public Optional<Tenant> find(UUID id) {
        return TenantEntity.<TenantEntity>find("id = ?1", id).firstResultOptional().map(mapper::toDomain);
    }
}
