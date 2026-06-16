package com.comercialcloud.interfaces.rest.billing;

import com.comercialcloud.application.billing.BillingService;
import com.comercialcloud.infrastructure.persistence.entity.AssinaturaTenantEntity;
import com.comercialcloud.infrastructure.persistence.entity.FaturaAssinaturaEntity;
import com.comercialcloud.infrastructure.persistence.entity.PlanoAssinaturaEntity;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/billing")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Billing")
public class BillingResource {

    @Inject
    BillingService billingService;

    @Inject
    TenantContext tenantContext;

    @GET
    @Path("/planos")
    public List<Map<String, Object>> planos() {
        return billingService.listarPlanos().stream().map(BillingResource::planoToMap).toList();
    }

    @POST
    @Path("/assinar")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response assinar(AssinarBody body) {
        boolean comTrial = body.comTrial() == null || body.comTrial();
        AssinaturaTenantEntity assinatura =
                billingService.assinar(tenantContext.getTenantId(), body.planoCodigo(), comTrial);
        return Response.status(Response.Status.CREATED).entity(assinaturaToMap(assinatura)).build();
    }

    @GET
    @Path("/assinatura")
    public Response assinatura() {
        return billingService
                .assinaturaAtual()
                .<Response>map(a -> Response.ok(assinaturaToMap(a)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("/assinatura/trocar-plano")
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, Object> trocarPlano(AssinarBody body) {
        return assinaturaToMap(billingService.trocarPlano(body.planoCodigo()));
    }

    @POST
    @Path("/assinatura/cancelar")
    public Map<String, Object> cancelar() {
        return assinaturaToMap(billingService.cancelar());
    }

    @POST
    @Path("/assinatura/reativar")
    public Map<String, Object> reativar() {
        return assinaturaToMap(billingService.reativar());
    }

    @GET
    @Path("/faturas")
    public List<Map<String, Object>> faturas() {
        return billingService.listarFaturas().stream().map(BillingResource::faturaToMap).toList();
    }

    @POST
    @Path("/faturas/{id}/pagar")
    public Map<String, Object> pagarFatura(@PathParam("id") UUID id) {
        return faturaToMap(billingService.marcarFaturaPaga(id));
    }

    @POST
    @Path("/faturar")
    public Map<String, Object> executarFaturamento() {
        List<UUID> ids = billingService.idsAssinaturasParaFaturar();
        for (UUID id : ids) {
            billingService.gerarFaturaAssinatura(id);
        }
        return Map.of("assinaturasProcessadas", ids.size());
    }

    @POST
    @Path("/cobrar")
    public Map<String, Object> executarCobranca() {
        List<UUID> ids = billingService.idsFaturasParaCobrar();
        for (UUID id : ids) {
            billingService.cobrarFatura(id);
        }
        return Map.of("faturasProcessadas", ids.size());
    }

    private static Map<String, Object> planoToMap(PlanoAssinaturaEntity p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.id);
        map.put("codigo", p.codigo);
        map.put("nome", p.nome);
        map.put("precoMensal", p.precoMensal);
        map.put("ativo", p.ativo);
        return map;
    }

    private static Map<String, Object> assinaturaToMap(AssinaturaTenantEntity a) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", a.id);
        map.put("tenantId", a.tenantId);
        map.put("planoId", a.planoId);
        map.put("status", a.status);
        map.put("inicio", a.inicio);
        map.put("proximaCobranca", a.proximaCobranca);
        map.put("trialFim", a.trialFim);
        map.put("tentativasFalha", a.tentativasFalha);
        return map;
    }

    private static Map<String, Object> faturaToMap(FaturaAssinaturaEntity f) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", f.id);
        map.put("assinaturaId", f.assinaturaId);
        map.put("competencia", f.competencia);
        map.put("valor", f.valor);
        map.put("vencimento", f.vencimento);
        map.put("status", f.status);
        map.put("tentativas", f.tentativas);
        map.put("transacaoRef", f.transacaoRef);
        map.put("motivoRecusa", f.motivoRecusa);
        map.put("pagaEm", f.pagaEm);
        return map;
    }

    public record AssinarBody(@NotBlank String planoCodigo, Boolean comTrial) {}
}
