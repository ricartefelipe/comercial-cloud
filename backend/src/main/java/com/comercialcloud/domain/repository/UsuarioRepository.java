
package com.comercialcloud.domain.repository;

import com.comercialcloud.domain.model.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository {

    Optional<Usuario> find(UUID tenantId, UUID id);

    List<Usuario> list(UUID tenantId);

    Usuario save(Usuario usuario);

    Optional<Usuario> findByEmail(UUID tenantId, String email);
}
