package com.comercialcloud.interfaces.rest.venda;

import com.comercialcloud.application.venda.VendaService;
import com.comercialcloud.domain.model.ItemVenda;
import com.comercialcloud.domain.model.PagamentoVenda;
import com.comercialcloud.domain.model.Venda;
import com.comercialcloud.domain.repository.ClienteRepository;
import com.comercialcloud.domain.repository.ProdutoRepository;
import jakarta.inject.Inject;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/vendas")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Vendas")
public class VendaResource {

    @Inject VendaService vendaService;
    @Inject ClienteRepository clienteRepository;
    @Inject ProdutoRepository produtoRepository;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response criar(CriarVendaBody request) {
        Venda venda = vendaService.criar(request.lojaId(), request.clienteId());
        return Response.status(Response.Status.CREATED).entity(toMapResposta(venda)).build();
    }

    @GET
    @Path("/{id}")
    public Map<String, Object> buscar(@PathParam("id") UUID id) {
        return toMapResposta(vendaService.buscarPorId(id));
    }

    @GET
    public List<Map<String, Object>> listar(
            @QueryParam("lojaId") UUID lojaId,
            @QueryParam("dataInicio") String dataInicio,
            @QueryParam("dataFim") String dataFim) {
        return vendaService.listar(lojaId, parseInstant(dataInicio), parseInstant(dataFim)).stream()
                .map(this::toMapResposta)
                .toList();
    }

    @POST
    @Path("/{id}/itens")
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, Object> adicionarItem(@PathParam("id") UUID id, AdicionarItemBody request) {
        return toMapResposta(vendaService.adicionarItem(
                id, request.produtoId(), request.quantidade(), request.desconto()));
    }

    @DELETE
    @Path("/{id}/itens/{itemId}")
    public Map<String, Object> removerItem(@PathParam("id") UUID id, @PathParam("itemId") UUID itemId) {
        return toMapResposta(vendaService.removerItem(id, itemId));
    }

    @POST
    @Path("/{id}/pagamentos")
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, Object> adicionarPagamento(@PathParam("id") UUID id, PagamentoBody request) {
        return toMapResposta(vendaService.adicionarPagamento(id, request.formaPagamento(), request.valor()));
    }

    @POST
    @Path("/{id}/finalizar")
    public Map<String, Object> finalizar(@PathParam("id") UUID id) {
        return toMapResposta(vendaService.finalizar(id));
    }

    @POST
    @Path("/{id}/cancelar")
    public Map<String, Object> cancelar(@PathParam("id") UUID id) {
        return toMapResposta(vendaService.cancelar(id));
    }

    public Map<String, Object> toMapResposta(Venda v) {
        UUID tenantId = v.tenantId();
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", v.id());
        root.put("tenantId", tenantId);
        root.put("lojaId", v.lojaId());
        root.put("clienteId", v.clienteId());
        root.put("operadorId", v.usuarioId());
        root.put("caixaId", v.caixaId());
        root.put("status", v.status().name());
        root.put("subtotal", v.subtotal());
        root.put("descontoTotal", v.desconto());
        root.put("total", v.total());
        root.put("createdAt", v.createdAt());
        root.put("finalizadaAt", v.finalizedAt());
        root.put("canceladaAt", v.canceledAt());

        String clienteNome = null;
        if (v.clienteId() != null) {
            clienteNome = clienteRepository.find(tenantId, v.clienteId()).map(c -> c.nome()).orElse(null);
        }
        root.put("clienteNome", clienteNome);

        List<Map<String, Object>> itens = v.itens().stream().map(i -> itemMap(tenantId, i)).toList();
        List<Map<String, Object>> pagamentos =
                v.pagamentos().stream().map(VendaResource::pagamentoMap).toList();
        root.put("itens", itens);
        root.put("pagamentos", pagamentos);
        root.put(
                "formaPagamentoPrincipal",
                pagamentos.isEmpty() ? null : pagamentos.getFirst().get("formaPagamento"));
        return root;
    }

    private Map<String, Object> itemMap(UUID tenantId, ItemVenda item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.id());
        map.put("produtoId", item.produtoId());
        map.put("quantidade", item.quantidade());
        map.put("precoUnitario", item.precoUnitario());
        map.put("subtotal", item.subtotal());
        produtoRepository.find(tenantId, item.produtoId()).ifPresent(produto -> {
            map.put("produtoNome", produto.nome());
            map.put("produtoSku", produto.sku());
        });
        return map;
    }

    static Map<String, Object> pagamentoMap(PagamentoVenda pagamento) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", pagamento.id());
        map.put("formaPagamento", pagamento.formaPagamento().name());
        map.put("valor", pagamento.valor());
        return map;
    }

    static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public record CriarVendaBody(@NotNull UUID lojaId, UUID clienteId) {}

    public record AdicionarItemBody(
            @NotNull UUID produtoId,
            @NotNull @DecimalMin("0.0") BigDecimal quantidade,
            BigDecimal desconto) {}

    public record PagamentoBody(
            @NotBlank String formaPagamento, @NotNull @DecimalMin("0.0") BigDecimal valor) {}
}
