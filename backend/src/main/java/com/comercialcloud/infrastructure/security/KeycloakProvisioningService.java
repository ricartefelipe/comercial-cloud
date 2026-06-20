package com.comercialcloud.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Provisiona usuário admin no Keycloak após onboarding self-service. Ativo quando
 * {@code comercial-cloud.keycloak.provisioning.enabled=true}.
 */
@ApplicationScoped
public class KeycloakProvisioningService {

    private static final Logger LOG = Logger.getLogger(KeycloakProvisioningService.class);

    @Inject ObjectMapper objectMapper;

    @Inject
    @ConfigProperty(name = "comercial-cloud.keycloak.provisioning.enabled", defaultValue = "false")
    boolean enabled;

    @Inject
    @ConfigProperty(name = "comercial-cloud.keycloak.server-url", defaultValue = "http://localhost:8180")
    String serverUrl;

    @Inject
    @ConfigProperty(name = "comercial-cloud.keycloak.realm", defaultValue = "comercialcloud")
    String realm;

    @Inject
    @ConfigProperty(name = "comercial-cloud.keycloak.admin-client-id", defaultValue = "comercial-cloud-api")
    String adminClientId;

    @Inject
    @ConfigProperty(name = "comercial-cloud.keycloak.admin-client-secret", defaultValue = "comercial-cloud-api-secret")
    String adminClientSecret;

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public record ProvisionRequest(
            UUID tenantId,
            UUID lojaId,
            UUID appUserId,
            String email,
            String nome,
            String senha,
            String role) {}

    public record ProvisionResult(boolean criado, String loginEmail, String mensagem) {}

    public boolean isEnabled() {
        return enabled;
    }

    public ProvisionResult provisionar(ProvisionRequest req) {
        if (!enabled) {
            return new ProvisionResult(false, req.email(), "Keycloak provisioning desabilitado");
        }
        try {
            String token = adminAccessToken();
            criarUsuario(token, req);
            return new ProvisionResult(true, req.email(), "Usuário criado no Keycloak. Faça login com e-mail e senha.");
        } catch (Exception e) {
            LOG.errorf(e, "Falha ao provisionar usuário Keycloak para %s", req.email());
            return new ProvisionResult(false, req.email(), "Keycloak: " + e.getMessage());
        }
    }

    private String adminAccessToken() throws Exception {
        String body =
                "grant_type=client_credentials"
                        + "&client_id="
                        + encode(adminClientId)
                        + "&client_secret="
                        + encode(adminClientSecret);
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(serverUrl + "/realms/" + realm + "/protocol/openid-connect/token"))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Token admin Keycloak HTTP " + response.statusCode());
        }
        JsonNode json = objectMapper.readTree(response.body());
        return json.path("access_token").asText();
    }

    private void criarUsuario(String adminToken, ProvisionRequest req) throws Exception {
        Map<String, Object> payload =
                Map.of(
                        "username", req.email(),
                        "email", req.email(),
                        "firstName", firstName(req.nome()),
                        "lastName", lastName(req.nome()),
                        "enabled", true,
                        "emailVerified", true,
                        "attributes",
                                Map.of(
                                        "tenant_id", List.of(req.tenantId().toString()),
                                        "app_user_id", List.of(req.appUserId().toString()),
                                        "loja_id", List.of(req.lojaId().toString())),
                        "credentials",
                                List.of(
                                        Map.of(
                                                "type", "password",
                                                "value", req.senha(),
                                                "temporary", false)));

        String json = objectMapper.writeValueAsString(payload);
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(serverUrl + "/admin/realms/" + realm + "/users"))
                        .timeout(Duration.ofSeconds(20))
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 409) {
            throw new IllegalStateException("E-mail já existe no Keycloak");
        }
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Criar usuário Keycloak HTTP " + response.statusCode() + ": " + response.body());
        }

        atribuirRole(adminToken, req.email(), req.role());
    }

    private void atribuirRole(String adminToken, String email, String role) throws Exception {
        HttpRequest find =
                HttpRequest.newBuilder()
                        .uri(URI.create(serverUrl + "/admin/realms/" + realm + "/users?email=" + encode(email)))
                        .timeout(Duration.ofSeconds(15))
                        .header("Authorization", "Bearer " + adminToken)
                        .GET()
                        .build();
        HttpResponse<String> findResponse = httpClient.send(find, HttpResponse.BodyHandlers.ofString());
        JsonNode users = objectMapper.readTree(findResponse.body());
        if (!users.isArray() || users.isEmpty()) {
            return;
        }
        String userId = users.get(0).path("id").asText();
        String roleJson = objectMapper.writeValueAsString(List.of(Map.of("name", role)));
        HttpRequest assign =
                HttpRequest.newBuilder()
                        .uri(URI.create(serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm"))
                        .timeout(Duration.ofSeconds(15))
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(roleJson))
                        .build();
        httpClient.send(assign, HttpResponse.BodyHandlers.ofString());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String firstName(String nome) {
        int space = nome == null ? -1 : nome.indexOf(' ');
        return space > 0 ? nome.substring(0, space) : Optional.ofNullable(nome).orElse("Admin");
    }

    private static String lastName(String nome) {
        int space = nome == null ? -1 : nome.indexOf(' ');
        return space > 0 ? nome.substring(space + 1) : "ComercialCloud";
    }
}
