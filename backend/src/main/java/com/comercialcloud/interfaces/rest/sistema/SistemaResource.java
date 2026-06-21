package com.comercialcloud.interfaces.rest.sistema;

import com.comercialcloud.application.pdv.PixPdvService;
import com.comercialcloud.infrastructure.billing.AsaasClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

@Path("/api/v1/sistema")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Sistema")
public class SistemaResource {

    @Inject PixPdvService pixPdvService;

    @Inject AsaasClient asaasClient;

    @ConfigProperty(name = "comercial-cloud.fiscal.provider", defaultValue = "sandbox")
    String fiscalProvider;

    @ConfigProperty(name = "comercial-cloud.billing.gateway", defaultValue = "sandbox")
    String billingGateway;

    @ConfigProperty(name = "comercial-cloud.fiscal.focusnfe.enabled", defaultValue = "false")
    boolean focusEnabled;

    @GET
    @Path("/capacidades")
    public Map<String, Object> capacidades() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fiscalProvider", fiscalProvider);
        map.put("fiscalFocusHabilitado", focusEnabled);
        map.put("billingGateway", billingGateway);
        map.put("asaasConfigurado", asaasClient.isConfigured());
        map.put("pixPdvDisponivel", pixPdvService.pixDisponivel());
        return map;
    }
}
