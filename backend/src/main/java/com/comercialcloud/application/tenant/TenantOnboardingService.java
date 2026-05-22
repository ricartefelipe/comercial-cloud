package com.comercialcloud.application.tenant;

import com.comercialcloud.domain.exception.ConflictException;
import com.comercialcloud.domain.model.Loja;
import com.comercialcloud.domain.model.Tenant;
import com.comercialcloud.domain.model.Usuario;
import com.comercialcloud.domain.repository.UsuarioRepository;
import com.comercialcloud.infrastructure.persistence.entity.LojaEntity;
import com.comercialcloud.infrastructure.persistence.entity.TenantEntity;
import com.comercialcloud.infrastructure.persistence.mapper.EntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class TenantOnboardingService {

    @Inject
    EntityMapper mapper;

    @Inject
    UsuarioRepository usuarioRepository;

    public record OnboardingResult(UUID tenantId, UUID lojaId, UUID adminUserId) {}

    @Transactional
    public OnboardingResult onboard(
            String nomeFantasia, String razaoSocial, String cnpj, String adminNome, String adminEmail) {
        Instant now = Instant.now();
        UUID tenantId = UUID.randomUUID();
        UUID lojaId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        Tenant tenant = new Tenant(tenantId, razaoSocial, true, now);
        TenantEntity tenantEntity = mapper.toTenantEntity(tenant);
        tenantEntity.persist();

        Loja loja = new Loja(lojaId, tenantId, nomeFantasia, true, now);
        LojaEntity lojaEntity = mapper.toEntity(loja);
        lojaEntity.persist();

        usuarioRepository
                .findByEmail(tenantId, adminEmail)
                .ifPresent(u -> {
                    throw new ConflictException("EMAIL_DUPLICADO", "E-mail já cadastrado");
                });

        Usuario admin = new Usuario(adminUserId, tenantId, adminNome, adminEmail, "ADMIN", true, now);
        usuarioRepository.save(admin);

        return new OnboardingResult(tenantId, lojaId, adminUserId);
    }
}
