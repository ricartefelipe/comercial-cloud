package com.comercialcloud.infrastructure.persistence.repository;

import com.comercialcloud.domain.model.Usuario;
import com.comercialcloud.domain.repository.UsuarioRepository;
import com.comercialcloud.infrastructure.persistence.entity.UsuarioEntity;
import com.comercialcloud.infrastructure.persistence.mapper.EntityMapper;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
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

    @Override
    public List<Usuario> list(UUID tenantId) {
        return UsuarioEntity.<UsuarioEntity>find("tenantId = ?1", Sort.by("nome"), tenantId).list().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Usuario save(Usuario usuario) {
        UsuarioEntity managed =
                UsuarioEntity.<UsuarioEntity>find("tenantId = ?1 and id = ?2", usuario.tenantId(), usuario.id())
                        .firstResult();
        if (managed == null) {
            UsuarioEntity novo = mapper.toEntity(usuario);
            novo.persist();
            return mapper.toDomain(novo);
        }
        UsuarioEntity payload = mapper.toEntity(usuario);
        managed.nome = payload.nome;
        managed.email = payload.email;
        managed.role = payload.role;
        managed.ativo = payload.ativo;
        return mapper.toDomain(managed);
    }

    @Override
    public Optional<Usuario> findByEmail(UUID tenantId, String email) {
        return UsuarioEntity.<UsuarioEntity>find("tenantId = ?1 and email = ?2", tenantId, email)
                .firstResultOptional()
                .map(mapper::toDomain);
    }
}
