package com.comercialcloud.interfaces.rest.publico;

import com.comercialcloud.application.billing.BillingService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo público de planos para a página de cadastro self-service (não exige
 * tenant). Permite que o prospect veja preços antes de criar a conta.
 */
@Path("/api/v1/public/planos")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Onboarding")
public class PlanosPublicosResource {

    @Inject
    BillingService billingService;

    @GET
    public List<Map<String, Object>> listar() {
        return billingService.listarPlanos().stream()
                .map(p -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("codigo", p.codigo);
                    map.put("nome", p.nome);
                    map.put("precoMensal", p.precoMensal);
                    return map;
                })
                .toList();
    }
}
