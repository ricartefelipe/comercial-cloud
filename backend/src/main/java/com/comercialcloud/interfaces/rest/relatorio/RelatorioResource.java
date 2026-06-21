package com.comercialcloud.interfaces.rest.relatorio;

import com.comercialcloud.application.relatorio.RelatorioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/relatorios")
@Tag(name = "Relatorios")
public class RelatorioResource {

    @Inject RelatorioService relatorioService;

    @GET
    @Path("/resumo")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> resumo(
            @QueryParam("lojaId") UUID lojaId,
            @QueryParam("dataInicio") String dataInicio,
            @QueryParam("dataFim") String dataFim) {
        return relatorioService.resumo(lojaId, parseInicio(dataInicio), parseFim(dataFim));
    }

    @GET
    @Path("/vendas")
    public Response vendas(
            @QueryParam("lojaId") UUID lojaId,
            @QueryParam("dataInicio") String dataInicio,
            @QueryParam("dataFim") String dataFim,
            @QueryParam("formato") @DefaultValue("json") String formato) {
        Instant inicio = parseInicio(dataInicio);
        Instant fim = parseFim(dataFim);
        if ("csv".equalsIgnoreCase(formato)) {
            byte[] csv = relatorioService.vendasCsv(lojaId, inicio, fim);
            return Response.ok(csv, "text/csv; charset=utf-8")
                    .header("Content-Disposition", "attachment; filename=\"vendas.csv\"")
                    .build();
        }
        List<Map<String, Object>> json = relatorioService.vendasJson(lojaId, inicio, fim);
        return Response.ok(json).build();
    }

    @GET
    @Path("/estoque")
    public Response estoque(
            @QueryParam("lojaId") UUID lojaId, @QueryParam("formato") @DefaultValue("json") String formato) {
        if ("csv".equalsIgnoreCase(formato)) {
            byte[] csv = relatorioService.estoqueCsv(lojaId);
            return Response.ok(csv, "text/csv; charset=utf-8")
                    .header("Content-Disposition", "attachment; filename=\"estoque.csv\"")
                    .build();
        }
        List<Map<String, Object>> json = relatorioService.estoqueJson(lojaId);
        return Response.ok(json).build();
    }

    private static Instant parseInicio(String data) {
        if (data == null || data.isBlank()) {
            return LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        return LocalDate.parse(data).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private static Instant parseFim(String data) {
        if (data == null || data.isBlank()) {
            return LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        return LocalDate.parse(data).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
