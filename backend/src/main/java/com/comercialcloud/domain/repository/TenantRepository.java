package com.comercialcloud.domain.repository;

import com.comercialcloud.domain.model.Tenant;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository {

    Optional<Tenant> find(UUID id);
}
