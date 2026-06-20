package com.comercialcloud.interfaces.rest.branding;

import com.comercialcloud.application.branding.BrandingService;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/api/v1/branding")
@Produces(MediaType.APPLICATION_JSON)
public class BrandingResource {

    @Inject BrandingService brandingService;
    @Inject TenantContext tenantContext;

    @GET
    public Map<String, String> obter() {
        return brandingService.obter();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, String> salvar(Map<String, String> body) {
        if (!tenantContext.isAdmin()) {
            throw new ForbiddenException("Apenas ADMIN pode alterar branding");
        }
        return brandingService.salvar(body);
    }
}
