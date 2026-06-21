package com.comercialcloud.interfaces.rest.caixa;

import com.comercialcloud.application.caixa.CaixaMovimentoService;
import com.comercialcloud.application.caixa.CaixaService;
import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.domain.model.Caixa;
import com.comercialcloud.domain.model.MovimentoCaixa;
import com.comercialcloud.domain.model.TipoMovimentoCaixa;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.inject.Inject;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/caixas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Caixa")
public class CaixaResource {

    @Inject CaixaService caixaService;

    @Inject CaixaMovimentoService caixaMovimentoService;

    @Inject TenantContext tenantContext;

    @POST
    @Path("/abrir")
    public Response abrir(AbrirBody request) {
        UUID operadorId =
                tenantContext.getUserId()
                        .orElseThrow(
                                () -> new BusinessException(
                                        "USUARIO_OBRIGATORIO", "Header X-User-Id é obrigatório"));
        var caixa = caixaService.abrir(request.lojaId(), operadorId, request.valorAbertura());
        return Response.status(Response.Status.CREATED).entity(toMap(caixa)).build();
    }

    @POST
    @Path("/{id}/fechar")
    public Map<String, Object> fechar(@PathParam("id") UUID id, FecharBody request) {
        return toMap(caixaService.fechar(id, request.valorFechamentoInformado()));
    }

    @GET
    @Path("/aberto")
    public Response aberto(
            @QueryParam("lojaId") UUID lojaId,
            @QueryParam("operadorId") UUID operadorId) {
        return caixaService.buscarAberto(lojaId, operadorId)
                .map(c -> Response.ok(toMap(c)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/{id}/resumo")
    public Map<String, Object> resumo(@PathParam("id") UUID id) {
        var resumo = caixaService.resumo(id);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("caixaId", resumo.caixaId());
        map.put("totalVendas", resumo.totalVendas());
        map.put("totalDinheiro", resumo.totalDinheiro());
        map.put("totalPix", resumo.totalPix());
        map.put("totalCartao", resumo.totalCartao());
        map.put("totalOutros", resumo.totalOutros());
        map.put("quantidadeVendas", resumo.quantidadeVendas());
        map.put("valorAbertura", resumo.valorAbertura());
        map.put("valorCalculado", resumo.valorCalculado());
        map.put("totalSangrias", resumo.totalSangrias());
        map.put("totalSuprimentos", resumo.totalSuprimentos());
        map.put("valorGavetaEsperada", resumo.valorGavetaEsperada());
        return map;
    }

    @POST
    @Path("/{id}/movimentos")
    public Response registrarMovimento(@PathParam("id") UUID id, MovimentoBody request) {
        TipoMovimentoCaixa tipo = TipoMovimentoCaixa.valueOf(request.tipo().toUpperCase());
        var movimento = caixaMovimentoService.registrar(id, tipo, request.valor(), request.motivo());
        return Response.status(Response.Status.CREATED).entity(toMovimentoMap(movimento)).build();
    }

    @GET
    @Path("/{id}/movimentos")
    public List<Map<String, Object>> listarMovimentos(@PathParam("id") UUID id) {
        return caixaMovimentoService.listar(id).stream().map(CaixaResource::toMovimentoMap).toList();
    }

    static Map<String, Object> toMovimentoMap(MovimentoCaixa movimento) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", movimento.id());
        map.put("caixaId", movimento.caixaId());
        map.put("lojaId", movimento.lojaId());
        map.put("usuarioId", movimento.usuarioId());
        map.put("tipo", movimento.tipo().name());
        map.put("valor", movimento.valor());
        map.put("motivo", movimento.motivo());
        map.put("createdAt", movimento.createdAt());
        return map;
    }

    @GET
    @Path("/historico")
    public List<Map<String, Object>> historico(
            @QueryParam("lojaId") UUID lojaId, @QueryParam("limit") @DefaultValue("20") int limit) {
        return caixaService.listarHistorico(lojaId, limit).stream().map(CaixaResource::toMap).toList();
    }

    static Map<String, Object> toMap(Caixa caixa) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", caixa.id());
        map.put("tenantId", caixa.tenantId());
        map.put("lojaId", caixa.lojaId());
        map.put("operadorId", caixa.usuarioId());
        map.put("status", caixa.status().name());
        map.put("valorAbertura", caixa.valorAbertura());
        map.put("valorFechamentoInformado", caixa.valorFechamento());
        map.put("openedAt", caixa.abertoEm());
        map.put("closedAt", caixa.fechadoEm());
        if (caixa.valorFechamento() != null && caixa.totalVendas() != null) {
            map.put("valorCalculado", caixa.valorAbertura().add(caixa.totalVendas()));
            map.put(
                    "diferenca",
                    caixa.valorFechamento().subtract(caixa.valorAbertura().add(caixa.totalVendas())));
        }
        return map;
    }

    public record AbrirBody(
            @NotNull UUID lojaId,
            @NotNull @DecimalMin("0.0") BigDecimal valorAbertura) {}

    public record FecharBody(
            @NotNull @DecimalMin("0.0") BigDecimal valorFechamentoInformado) {}

    public record MovimentoBody(
            @NotNull String tipo,
            @NotNull @DecimalMin("0.01") BigDecimal valor,
            @NotNull String motivo) {}
}
