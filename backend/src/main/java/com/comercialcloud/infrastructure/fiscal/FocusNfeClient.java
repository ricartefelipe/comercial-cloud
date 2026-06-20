package com.comercialcloud.infrastructure.fiscal;

import com.comercialcloud.domain.model.AmbienteFiscal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente HTTP para a API REST Focus NFe (NFC-e). Documentação:
 * https://doc.focusnfe.com.br/
 */
@ApplicationScoped
public class FocusNfeClient {

    @Inject ObjectMapper objectMapper;

    @Inject
    @ConfigProperty(name = "comercial-cloud.fiscal.focusnfe.token")
    Optional<String> token;

    @Inject
    @ConfigProperty(name = "comercial-cloud.fiscal.focusnfe.enabled", defaultValue = "false")
    boolean enabled;

    @Inject
    @ConfigProperty(name = "comercial-cloud.fiscal.focusnfe.base-url-homologacao", defaultValue = "https://homologacao.focusnfe.com.br")
    String baseUrlHomologacao;

    @Inject
    @ConfigProperty(name = "comercial-cloud.fiscal.focusnfe.base-url-producao", defaultValue = "https://api.focusnfe.com.br")
    String baseUrlProducao;

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    public boolean isConfigured() {
        return enabled && token.filter(t -> !t.isBlank()).isPresent();
    }

    public NfceResponse emitirNfce(String referencia, Map<String, Object> payload, AmbienteFiscal ambiente)
            throws Exception {
        return request("POST", "/v2/nfce?ref=" + encode(referencia), payload, ambiente, NfceResponse.class);
    }

    public NfceResponse consultarNfce(String referencia, AmbienteFiscal ambiente) throws Exception {
        return request("GET", "/v2/nfce/" + encode(referencia), null, ambiente, NfceResponse.class);
    }

    public NfceResponse cancelarNfce(String referencia, String justificativa, AmbienteFiscal ambiente)
            throws Exception {
        String path = "/v2/nfce/" + encode(referencia) + "?justificativa=" + encode(justificativa);
        return request("DELETE", path, null, ambiente, NfceResponse.class);
    }

    String urlDanfeCompleta(NfceResponse response, AmbienteFiscal ambiente) {
        if (response == null || response.caminhoDanfe() == null || response.caminhoDanfe().isBlank()) {
            return null;
        }
        String caminho = response.caminhoDanfe();
        if (caminho.startsWith("http")) {
            return caminho;
        }
        return baseUrl(ambiente) + caminho;
    }

    private <T> T request(
            String method,
            String path,
            Object body,
            AmbienteFiscal ambiente,
            Class<T> type)
            throws Exception {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl(ambiente) + path))
                        .timeout(Duration.ofSeconds(60))
                        .header("Authorization", basicAuth())
                        .header("Accept", "application/json");

        if ("POST".equals(method)) {
            String json = objectMapper.writeValueAsString(body);
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));
        } else if ("DELETE".equals(method)) {
            builder.DELETE();
        } else {
            builder.GET();
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() >= 400 && response.statusCode() != 422) {
            throw new IllegalStateException("Focus NFe HTTP " + response.statusCode() + ": " + response.body());
        }
        return objectMapper.readValue(response.body(), type);
    }

    private String baseUrl(AmbienteFiscal ambiente) {
        return ambiente == AmbienteFiscal.PRODUCAO ? baseUrlProducao : baseUrlHomologacao;
    }

    private String basicAuth() {
        String raw = token.orElse("") + ":";
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NfceResponse {
        public String status;
        public String chave_nfe;
        public String numero;
        public String serie;
        public String mensagem_sefaz;
        public String mensagem;
        public String caminho_danfe;
        public String caminhoDanfe;

        public String caminhoDanfe() {
            return caminho_danfe != null ? caminho_danfe : caminhoDanfe;
        }
    }
}
