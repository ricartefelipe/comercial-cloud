package com.comercialcloud.infrastructure.security;

import com.comercialcloud.domain.model.Usuario;
import com.comercialcloud.domain.shared.DemoConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sessão da demonstração pública ({@code comercial-cloud.tenant.resolution=demo}): sem Keycloak, a conta de
 * demonstração entra por {@code /api/v1/public/demo/sessao} e recebe um token assinado pelo servidor (HS256, segredo
 * só do servidor, prazo curto), enviado no cabeçalho {@code X-Sessao-Demo}. Sem ele a API responde 401, como em
 * produção: nenhuma rota de dados é anônima.
 */
@ApplicationScoped
public class SessaoDemonstracao {

    static final String EMISSOR = "comercialcloud-demo";
    private static final int TAMANHO_MINIMO_SEGREDO = 32;
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final String CABECALHO =
            B64.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    public record Sessao(UUID tenantId, UUID userId) {}

    @ConfigProperty(name = "comercial-cloud.tenant.resolution", defaultValue = "jwt")
    String resolution;

    @ConfigProperty(name = "comercial-cloud.demo.session-secret")
    Optional<String> segredo;

    @ConfigProperty(name = "comercial-cloud.demo.senha")
    Optional<String> senha;

    @ConfigProperty(name = "comercial-cloud.demo.session-ttl", defaultValue = "PT8H")
    Duration validade;

    @Inject
    ObjectMapper mapper;

    /** Sem segredo forte e senha definidos, o modo de demonstração não sobe: melhor falhar na partida que abrir a API. */
    void validarConfiguracao(@Observes StartupEvent evento) {
        if (!ativa()) {
            return;
        }
        if (segredo.map(String::trim).orElse("").length() < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException(
                    "Modo de demonstração exige COMERCIAL_CLOUD_DEMO_SESSION_SECRET com pelo menos "
                            + TAMANHO_MINIMO_SEGREDO + " caracteres");
        }
        if (senha.map(String::isBlank).orElse(true)) {
            throw new IllegalStateException("Modo de demonstração exige COMERCIAL_CLOUD_DEMO_SENHA");
        }
    }

    public boolean ativa() {
        return "demo".equalsIgnoreCase(resolution);
    }

    public Duration validade() {
        return validade;
    }

    /** Compara em tempo constante, sobre o resumo, para não revelar o tamanho da senha. */
    public boolean senhaConfere(String informada) {
        if (informada == null || senha.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(resumo(informada), resumo(senha.get()));
    }

    public String emitir(Usuario usuario) {
        Instant agora = Instant.now();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", EMISSOR);
        claims.put("sub", usuario.id().toString());
        claims.put("app_user_id", usuario.id().toString());
        claims.put("tenant_id", usuario.tenantId().toString());
        claims.put("name", usuario.nome());
        claims.put("email", usuario.email());
        claims.put("iat", agora.getEpochSecond());
        claims.put("exp", agora.plus(validade).getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());
        try {
            String corpo = B64.encodeToString(mapper.writeValueAsBytes(claims));
            String assinado = CABECALHO + "." + corpo;
            return assinado + "." + B64.encodeToString(assinar(assinado));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível emitir a sessão de demonstração", e);
        }
    }

    /**
     * Aceita só o cabeçalho exato que este servidor emite (HS256): {@code alg} trocado ou {@code none} cai fora antes
     * da assinatura. Exige emissor, prazo e o locatário de demonstração.
     */
    public Optional<Sessao> validar(String token) {
        if (!ativa() || token == null) {
            return Optional.empty();
        }
        String[] partes = token.trim().split("\\.", -1);
        if (partes.length != 3 || !CABECALHO.equals(partes[0])) {
            return Optional.empty();
        }
        try {
            byte[] esperada = assinar(partes[0] + "." + partes[1]);
            if (!MessageDigest.isEqual(esperada, B64D.decode(partes[2]))) {
                return Optional.empty();
            }
            JsonNode claims = mapper.readTree(B64D.decode(partes[1]));
            if (!EMISSOR.equals(claims.path("iss").asText())) {
                return Optional.empty();
            }
            if (!claims.path("exp").canConvertToLong()
                    || claims.path("exp").asLong() <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            UUID tenantId = UUID.fromString(claims.path("tenant_id").asText());
            UUID userId = UUID.fromString(claims.path("app_user_id").asText());
            if (!DemoConstants.TENANT_DEMO.equals(tenantId)) {
                return Optional.empty();
            }
            return Optional.of(new Sessao(tenantId, userId));
        } catch (IllegalArgumentException | java.io.IOException e) {
            return Optional.empty();
        }
    }

    private byte[] assinar(String conteudo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.orElseThrow().trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 indisponível", e);
        }
    }

    private static byte[] resumo(String valor) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
