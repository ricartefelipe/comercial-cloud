package com.comercialcloud.application.usuario;

import com.comercialcloud.domain.exception.ConflictException;
import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.domain.model.Usuario;
import com.comercialcloud.domain.repository.UsuarioRepository;
import com.comercialcloud.infrastructure.audit.AuditService;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UsuarioService {

    @Inject
    TenantContext tenantContext;

    @Inject
    AuditService auditService;

    @Inject
    UsuarioRepository usuarioRepository;

    public List<Usuario> listar() {
        return usuarioRepository.list(tenantContext.getTenantId());
    }

    @Transactional
    public Usuario criar(String nome, String email, String role) {
        UUID tenantId = tenantContext.getTenantId();
        usuarioRepository
                .findByEmail(tenantId, email)
                .ifPresent(u -> {
                    throw new ConflictException("EMAIL_DUPLICADO", "E-mail já cadastrado para este tenant");
                });
        Instant now = Instant.now();
        Usuario usuario = new Usuario(UUID.randomUUID(), tenantId, nome, email, role, true, now);
        Usuario salvo = usuarioRepository.save(usuario);
        auditService.registrar("Usuario", salvo.id(), "CRIAR", nome);
        return salvo;
    }

    @Transactional
    public Usuario atualizar(UUID id, String nome, String email, String role) {
        Usuario existente = obter(id);
        if (!existente.email().equalsIgnoreCase(email)) {
            usuarioRepository
                    .findByEmail(existente.tenantId(), email)
                    .filter(u -> !u.id().equals(id))
                    .ifPresent(u -> {
                        throw new ConflictException("EMAIL_DUPLICADO", "E-mail já cadastrado para este tenant");
                    });
        }
        Usuario atualizado =
                new Usuario(existente.id(), existente.tenantId(), nome, email, role, existente.ativo(), existente.createdAt());
        Usuario salvo = usuarioRepository.save(atualizado);
        auditService.registrar("Usuario", salvo.id(), "ATUALIZAR", nome);
        return salvo;
    }

    @Transactional
    public void inativar(UUID id) {
        Usuario existente = obter(id);
        Usuario inativo =
                new Usuario(
                        existente.id(),
                        existente.tenantId(),
                        existente.nome(),
                        existente.email(),
                        existente.role(),
                        false,
                        existente.createdAt());
        usuarioRepository.save(inativo);
        auditService.registrar("Usuario", id, "INATIVAR", null);
    }

    private Usuario obter(UUID id) {
        return usuarioRepository
                .find(tenantContext.getTenantId(), id)
                .orElseThrow(() -> new NotFoundException("USUARIO_NOT_FOUND", "Usuário não encontrado"));
    }
}
