package com.comercialcloud.interfaces.rest.publico;

import com.comercialcloud.application.tenant.TenantOnboardingService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

@Path("/api/v1/public/onboarding")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Onboarding")
public class OnboardingResource {

    @Inject
    TenantOnboardingService tenantOnboardingService;

    @POST
    public Response onboard(OnboardingBody request) {
        var result = tenantOnboardingService.onboard(
                request.nomeFantasia(),
                request.razaoSocial(),
                request.cnpj(),
                request.adminNome(),
                request.adminEmail());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenantId", result.tenantId());
        body.put("lojaId", result.lojaId());
        body.put("adminUserId", result.adminUserId());
        return Response.status(Response.Status.CREATED).entity(body).build();
    }

    public record OnboardingBody(
            @NotBlank String nomeFantasia,
            @NotBlank String razaoSocial,
            @NotBlank String cnpj,
            @NotBlank String adminNome,
            @NotBlank String adminEmail) {}
}
