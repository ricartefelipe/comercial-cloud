package com.comercialcloud.interfaces.rest.publico;

import com.comercialcloud.application.billing.BillingService;
import com.comercialcloud.infrastructure.billing.AsaasClient;
import com.comercialcloud.infrastructure.billing.AsaasGatewayPagamento;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Optional;

/**
 * Webhook público do Asaas para confirmação de pagamentos PIX. Configure a URL
 * {@code /api/v1/public/webhooks/asaas} no painel Asaas.
 */
@Path("/api/v1/public/webhooks/asaas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Billing")
public class AsaasWebhookResource {

    @Inject BillingService billingService;
    @Inject AsaasClient asaasClient;

    @Inject
    @ConfigProperty(name = "comercial-cloud.billing.asaas.webhook-token")
    Optional<String> webhookToken;

    @POST
    public Response receber(JsonNode payload) {
        if (webhookToken.filter(t -> !t.isBlank()).isPresent()) {
            String token = payload.path("token").asText("");
            if (!webhookToken.get().equals(token)) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        }

        String event = payload.path("event").asText("");
        JsonNode payment = payload.path("payment");
        if (payment.isMissingNode()) {
            return Response.ok().build();
        }

        String paymentId = payment.path("id").asText(null);
        String status = payment.path("status").asText(null);
        if (paymentId == null) {
            return Response.ok().build();
        }

        if (AsaasGatewayPagamento.isPaid(status)
                || "PAYMENT_RECEIVED".equalsIgnoreCase(event)
                || "PAYMENT_CONFIRMED".equalsIgnoreCase(event)) {
            billingService.confirmarPagamentoGateway(paymentId, paymentId);
        }

        return Response.ok().build();
    }
}
