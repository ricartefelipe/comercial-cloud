package com.comercialcloud.interfaces.rest.pdv;

import com.comercialcloud.application.pdv.PixPdvService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/vendas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "PIX PDV")
public class PixPdvResource {

    @Inject PixPdvService pixPdvService;

    @POST
    @Path("/{vendaId}/pix-cobranca")
    public Map<String, Object> criarCobranca(@PathParam("vendaId") UUID vendaId, PixBody body) {
        return toMap(pixPdvService.criarCobranca(vendaId, body.valor()));
    }

    @GET
    @Path("/{vendaId}/pix-cobranca/{cobrancaId}")
    public Map<String, Object> consultar(
            @PathParam("vendaId") UUID vendaId, @PathParam("cobrancaId") UUID cobrancaId) {
        PixPdvService.PixCobrancaResult result = pixPdvService.consultarStatus(cobrancaId);
        if (!result.vendaId().equals(vendaId)) {
            throw new jakarta.ws.rs.NotFoundException();
        }
        return toMap(result);
    }

    static Map<String, Object> toMap(PixPdvService.PixCobrancaResult result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", result.id());
        map.put("vendaId", result.vendaId());
        map.put("valor", result.valor());
        map.put("status", result.status());
        map.put("checkoutUrl", result.checkoutUrl());
        map.put("pixPayload", result.pixPayload());
        map.put("pixEncodedImage", result.pixEncodedImage());
        map.put("gatewayPaymentId", result.gatewayPaymentId());
        return map;
    }

    public record PixBody(@NotNull @DecimalMin("0.01") BigDecimal valor) {}
}
