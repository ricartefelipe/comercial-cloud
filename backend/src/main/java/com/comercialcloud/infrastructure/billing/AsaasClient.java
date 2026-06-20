package com.comercialcloud.infrastructure.billing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente HTTP mínimo para a API Asaas v3 (sandbox ou produção). Usado pelo
 * {@link AsaasGatewayPagamento} para criar clientes e cobranças PIX.
 */
@ApplicationScoped
public class AsaasClient {

    private static final DateTimeFormatter ASAAS_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Inject ObjectMapper objectMapper;

    @Inject
    @ConfigProperty(name = "comercial-cloud.billing.asaas.api-key")
    Optional<String> apiKey;

    @Inject
    @ConfigProperty(name = "comercial-cloud.billing.asaas.base-url", defaultValue = "https://api-sandbox.asaas.com/v3")
    String baseUrl;

    @Inject
    @ConfigProperty(name = "comercial-cloud.billing.asaas.enabled", defaultValue = "false")
    boolean enabled;

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public boolean isConfigured() {
        return enabled && apiKey.filter(k -> !k.isBlank()).isPresent();
    }

    public CustomerResponse createCustomer(String name, String email, String cpfCnpj, String externalReference)
            throws Exception {
        Map<String, Object> body =
                Map.of(
                        "name", name,
                        "email", email,
                        "cpfCnpj", sanitizeCnpj(cpfCnpj),
                        "externalReference", externalReference);
        return post("/customers", body, CustomerResponse.class);
    }

    public PaymentResponse createPixPayment(
            String customerId,
            BigDecimal value,
            LocalDate dueDate,
            String description,
            String externalReference)
            throws Exception {
        Map<String, Object> body =
                Map.of(
                        "customer", customerId,
                        "billingType", "PIX",
                        "value", value,
                        "dueDate", dueDate.format(ASAAS_DATE),
                        "description", description,
                        "externalReference", externalReference);
        return post("/payments", body, PaymentResponse.class);
    }

    public PaymentResponse getPayment(String paymentId) throws Exception {
        return get("/payments/" + paymentId, PaymentResponse.class);
    }

    private <T> T get(String path, Class<T> type) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(30))
                        .header("access_token", apiKey.orElse(""))
                        .header("Accept", "application/json")
                        .GET()
                        .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return readResponse(response, type);
    }

    private <T> T post(String path, Object body, Class<T> type) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(30))
                        .header("access_token", apiKey.orElse(""))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return readResponse(response, type);
    }

    private <T> T readResponse(HttpResponse<String> response, Class<T> type) throws Exception {
        if (response.statusCode() >= 400) {
            throw new IllegalStateException(
                    "Asaas HTTP " + response.statusCode() + ": " + response.body());
        }
        return objectMapper.readValue(response.body(), type);
    }

    private static String sanitizeCnpj(String cnpj) {
        return cnpj == null ? "" : cnpj.replaceAll("\\D", "");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerResponse {
        public String id;
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentResponse {
        public String id;
        public String status;
        public String invoiceUrl;
        public String bankSlipUrl;
        public String transactionReceiptUrl;
    }
}
