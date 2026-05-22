package com.comercialcloud.interfaces.rest.dashboard;

import com.comercialcloud.application.dashboard.DashboardService;
import com.comercialcloud.domain.model.Estoque;
import com.comercialcloud.domain.model.Produto;
import com.comercialcloud.interfaces.rest.venda.VendaResource;
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

@Path("/api/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Dashboard")
public class DashboardResource {

    @Inject
    DashboardService dashboardService;

    @Inject
    VendaResource vendaResource;

    @GET
    @Path("/resumo")
    public Map<String, Object> resumo(@QueryParam("lojaId") UUID lojaId) {
        var result = dashboardService.resumo(lojaId);

        List<Map<String, Object>> estoqueBaixo =
                result.produtosEstoqueBaixo().stream()
                        .map(e -> enrichEstoque(e, result.produtosPorId()))
                        .toList();

        List<Map<String, Object>> ultimas =
                result.ultimasVendas().stream().map(v -> vendaResource.toMapResposta(v)).toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("vendasDoDia", result.vendasDoDia());
        payload.put("faturamentoDoDia", result.faturamentoDoDia());
        payload.put("ticketMedio", result.ticketMedio());
        payload.put("estoquesBaixo", estoqueBaixo);
        payload.put("ultimasVendas", ultimas);
        payload.put("vendasPorFormaPagamento", result.vendasPorFormaPagamento());
        return payload;
    }

    static Map<String, Object> enrichEstoque(Estoque estoque, Map<UUID, Produto> produtosPorId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", estoque.id());
        map.put("tenantId", estoque.tenantId());
        map.put("lojaId", estoque.lojaId());
        map.put("produtoId", estoque.produtoId());
        map.put("quantidadeAtual", estoque.quantidade());
        Produto produto = produtosPorId.get(estoque.produtoId());
        if (produto != null) {
            map.put("produtoNome", produto.nome());
            map.put("produtoSku", produto.sku());
        }
        return map;
    }
}
