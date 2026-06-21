package com.comercialcloud.application.usuario;

import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.domain.exception.ConflictException;
import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.domain.model.Loja;
import com.comercialcloud.domain.model.Usuario;
import com.comercialcloud.domain.repository.LojaRepository;
import com.comercialcloud.domain.repository.UsuarioRepository;
import com.comercialcloud.infrastructure.audit.AuditService;
import com.comercialcloud.infrastructure.mail.TransacionalEmailService;
import com.comercialcloud.infrastructure.persistence.entity.ConviteUsuarioEntity;
import com.comercialcloud.infrastructure.persistence.entity.TenantEntity;
import com.comercialcloud.infrastructure.security.KeycloakProvisioningService;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ConviteUsuarioService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject TenantContext tenantContext;
    @Inject UsuarioRepository usuarioRepository;
    @Inject LojaRepository lojaRepository;
    @Inject AuditService auditService;
    @Inject TransacionalEmailService emailService;
    @Inject KeycloakProvisioningService keycloakProvisioningService;

    @Inject
    @ConfigProperty(name = "comercial-cloud.convite.validade-dias", defaultValue = "7")
    int validadeDias;

    public List<Map<String, Object>> listarPendentes() {
        UUID tenantId = tenantContext.getTenantId();
        return ConviteUsuarioEntity.<ConviteUsuarioEntity>find(
                        "tenantId = ?1 and aceitoEm is null and expiraEm > ?2 order by createdAt desc",
                        tenantId,
                        Instant.now())
                .stream()
                .map(this::toMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> convidar(String nome, String email, String role) {
        UUID tenantId = tenantContext.getTenantId();
        String emailNorm = email.trim().toLowerCase();
        usuarioRepository
                .findByEmail(tenantId, emailNorm)
                .ifPresent(u -> {
                    throw new ConflictException("EMAIL_DUPLICADO", "E-mail já possui usuário ativo");
                });

        ConviteUsuarioEntity pendente =
                ConviteUsuarioEntity.<ConviteUsuarioEntity>find(
                                "tenantId = ?1 and lower(email) = ?2 and aceitoEm is null and expiraEm > ?3",
                                tenantId,
                                emailNorm,
                                Instant.now())
                        .firstResult();
        if (pendente != null) {
            throw new ConflictException("CONVITE_PENDENTE", "Já existe convite pendente para este e-mail");
        }

        Instant now = Instant.now();
        ConviteUsuarioEntity convite = new ConviteUsuarioEntity();
        convite.id = UUID.randomUUID();
        convite.tenantId = tenantId;
        convite.email = emailNorm;
        convite.nome = nome.trim();
        convite.role = role.trim().toUpperCase();
        convite.token = gerarToken();
        convite.expiraEm = now.plus(validadeDias, ChronoUnit.DAYS);
        convite.criadoPor = tenantContext.getUserId().orElse(null);
        convite.createdAt = now;
        convite.persist();

        emailService.enviarConviteUsuario(convite.email, convite.nome, convite.token);
        auditService.registrar("ConviteUsuario", convite.id, "CRIAR", convite.email);
        return toMap(convite);
    }

    public Map<String, Object> consultarPublico(String token) {
        ConviteUsuarioEntity convite = buscarPorToken(token);
        validarPendente(convite);
        TenantEntity tenant = TenantEntity.findById(convite.tenantId);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("email", convite.email);
        map.put("nome", convite.nome);
        map.put("role", convite.role);
        map.put("tenantNome", tenant != null ? tenant.nome : "Loja");
        map.put("expiraEm", convite.expiraEm);
        return map;
    }

    @Transactional
    public Map<String, Object> aceitar(String token, String senha, String nomeOpcional) {
        ConviteUsuarioEntity convite = buscarPorToken(token);
        validarPendente(convite);
        if (senha == null || senha.length() < 8) {
            throw new BusinessException("SENHA_INVALIDA", "Senha deve ter ao menos 8 caracteres");
        }

        usuarioRepository
                .findByEmail(convite.tenantId, convite.email)
                .ifPresent(u -> {
                    throw new ConflictException("EMAIL_DUPLICADO", "E-mail já cadastrado");
                });

        Instant now = Instant.now();
        String nome = nomeOpcional != null && !nomeOpcional.isBlank() ? nomeOpcional.trim() : convite.nome;
        Usuario usuario =
                new Usuario(UUID.randomUUID(), convite.tenantId, nome, convite.email, convite.role, true, now);
        usuarioRepository.save(usuario);

        convite.aceitoEm = now;
        convite.nome = nome;

        Loja loja =
                lojaRepository.listByTenant(convite.tenantId).stream()
                        .filter(Loja::ativa)
                        .findFirst()
                        .orElse(null);

        if (keycloakProvisioningService.isEnabled() && loja != null) {
            keycloakProvisioningService.provisionar(
                    new KeycloakProvisioningService.ProvisionRequest(
                            convite.tenantId,
                            loja.id(),
                            usuario.id(),
                            convite.email,
                            nome,
                            senha,
                            convite.role));
        }

        auditService.registrar("ConviteUsuario", convite.id, "ACEITAR", convite.email);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("usuarioId", usuario.id());
        result.put("email", usuario.email());
        result.put("loginUrl", emailService.appLoginUrl());
        return result;
    }

    private ConviteUsuarioEntity buscarPorToken(String token) {
        return ConviteUsuarioEntity.<ConviteUsuarioEntity>find("token = ?1", token)
                .firstResultOptional()
                .orElseThrow(() -> new NotFoundException("CONVITE_NOT_FOUND", "Convite inválido ou expirado"));
    }

    private void validarPendente(ConviteUsuarioEntity convite) {
        if (convite.aceitoEm != null) {
            throw new BusinessException("CONVITE_JA_ACEITO", "Este convite já foi utilizado");
        }
        if (convite.expiraEm.isBefore(Instant.now())) {
            throw new BusinessException("CONVITE_EXPIRADO", "Convite expirado");
        }
    }

    private static String gerarToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private Map<String, Object> toMap(ConviteUsuarioEntity c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.id);
        map.put("email", c.email);
        map.put("nome", c.nome);
        map.put("role", c.role);
        map.put("token", c.token);
        map.put("expiraEm", c.expiraEm);
        map.put("aceitoEm", c.aceitoEm);
        map.put("createdAt", c.createdAt);
        return map;
    }
}
