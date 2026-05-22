package com.comercialcloud.interfaces.rest.financeiro;

import com.comercialcloud.application.financeiro.FinanceiroService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/financeiro")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Financeiro")
public class FinanceiroResource {

    @Inject
    FinanceiroService financeiroService;

    @GET
    @Path("/contas-receber")
    public List<Map<String, Object>> listarContasReceber(@QueryParam("lojaId") UUID lojaId) {
        return financeiroService.listarContasReceber(lojaId).stream()
                .map(view -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", view.conta().id);
                    map.put("tenantId", view.conta().tenantId);
                    map.put("lojaId", view.lojaId());
                    map.put("vendaId", view.conta().vendaId);
                    map.put("descricao", view.descricao());
                    map.put("valor", view.conta().valor);
                    map.put("status", view.conta().status);
                    map.put("vencimento", view.conta().vencimento);
                    return map;
                })
                .toList();
    }
}
