package com.comercialcloud.interfaces.rest.fiscal;

import com.comercialcloud.application.fiscal.ConfiguracaoFiscalService;
import com.comercialcloud.application.fiscal.FiscalService;
import com.comercialcloud.domain.model.AmbienteFiscal;
import com.comercialcloud.domain.model.ModeloDocumentoFiscal;
import com.comercialcloud.domain.model.RegimeTributario;
import com.comercialcloud.infrastructure.persistence.entity.ConfiguracaoFiscalEntity;
import com.comercialcloud.infrastructure.persistence.entity.DocumentoFiscalEntity;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/fiscal")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Fiscal")
public class FiscalResource {

    @Inject
    FiscalService fiscalService;

    @Inject
    ConfiguracaoFiscalService configuracaoFiscalService;

    @POST
    @Path("/configuracao")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response salvarConfiguracao(ConfiguracaoBody body) {
        ConfiguracaoFiscalEntity config =
                configuracaoFiscalService.salvar(
                        new ConfiguracaoFiscalService.UpsertConfiguracao(
                                body.lojaId(),
                                body.cnpj(),
                                body.inscricaoEstadual(),
                                body.razaoSocial(),
                                body.regimeTributario(),
                                body.ambiente(),
                                body.uf(),
                                body.cscId(),
                                body.csc(),
                                body.serieNfce(),
                                body.habilitado()));
        return Response.ok(configToMap(config)).build();
    }

    @GET
    @Path("/configuracao")
    public Response obterConfiguracao(@QueryParam("lojaId") UUID lojaId) {
        return configuracaoFiscalService
                .buscarPorLojaAtual(lojaId)
                .<Response>map(c -> Response.ok(configToMap(c)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("/vendas/{vendaId}/emitir")
    public Map<String, Object> emitir(
            @PathParam("vendaId") UUID vendaId, @QueryParam("modelo") String modelo) {
        ModeloDocumentoFiscal m =
                modelo == null || modelo.isBlank()
                        ? ModeloDocumentoFiscal.NFCE
                        : ModeloDocumentoFiscal.valueOf(modelo.toUpperCase());
        return documentoToMap(fiscalService.emitirAgora(vendaId, m));
    }

    @GET
    @Path("/vendas/{vendaId}")
    public Response porVenda(@PathParam("vendaId") UUID vendaId) {
        return fiscalService
                .buscarPorVenda(vendaId)
                .<Response>map(d -> Response.ok(documentoToMap(d)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/documentos")
    public List<Map<String, Object>> listar(@QueryParam("lojaId") UUID lojaId) {
        return fiscalService.listar(lojaId).stream().map(FiscalResource::documentoToMap).toList();
    }

    @GET
    @Path("/documentos/{id}")
    public Map<String, Object> porId(@PathParam("id") UUID id) {
        return documentoToMap(fiscalService.buscarPorId(id));
    }

    @POST
    @Path("/documentos/{id}/cancelar")
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, Object> cancelar(@PathParam("id") UUID id, CancelamentoBody body) {
        return documentoToMap(fiscalService.cancelar(id, body.justificativa()));
    }

    @POST
    @Path("/processar")
    public Map<String, Object> processarPendentes() {
        List<UUID> ids = fiscalService.idsPendentes();
        int processados = 0;
        for (UUID id : ids) {
            fiscalService.processarDocumento(id);
            processados++;
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("pendentes", ids.size());
        resp.put("processados", processados);
        return resp;
    }

    private static Map<String, Object> documentoToMap(DocumentoFiscalEntity doc) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", doc.id);
        map.put("lojaId", doc.lojaId);
        map.put("vendaId", doc.vendaId);
        map.put("modelo", doc.modelo);
        map.put("status", doc.status);
        map.put("numero", doc.numero);
        map.put("serie", doc.serie);
        map.put("chaveAcesso", doc.chaveAcesso);
        map.put("protocolo", doc.protocolo);
        map.put("motivoRejeicao", doc.motivoRejeicao);
        map.put("urlDanfe", doc.urlDanfe);
        map.put("tentativas", doc.tentativas);
        map.put("createdAt", doc.createdAt);
        map.put("updatedAt", doc.updatedAt);
        return map;
    }

    private static Map<String, Object> configToMap(ConfiguracaoFiscalEntity c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.id);
        map.put("lojaId", c.lojaId);
        map.put("cnpj", c.cnpj);
        map.put("inscricaoEstadual", c.inscricaoEstadual);
        map.put("razaoSocial", c.razaoSocial);
        map.put("regimeTributario", c.regimeTributario);
        map.put("ambiente", c.ambiente);
        map.put("uf", c.uf);
        map.put("serieNfce", c.serieNfce);
        map.put("proximoNumeroNfce", c.proximoNumeroNfce);
        map.put("habilitado", c.habilitado);
        return map;
    }

    public record ConfiguracaoBody(
            @NotNull UUID lojaId,
            @NotBlank String cnpj,
            String inscricaoEstadual,
            @NotBlank String razaoSocial,
            @NotNull RegimeTributario regimeTributario,
            @NotNull AmbienteFiscal ambiente,
            @NotBlank String uf,
            String cscId,
            String csc,
            Integer serieNfce,
            boolean habilitado) {}

    public record CancelamentoBody(@NotBlank String justificativa) {}
}
