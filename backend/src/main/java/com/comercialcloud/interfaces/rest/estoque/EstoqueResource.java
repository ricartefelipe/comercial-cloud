package com.comercialcloud.interfaces.rest.estoque;

import com.comercialcloud.application.estoque.EstoqueService;
import com.comercialcloud.domain.model.Estoque;
import com.comercialcloud.domain.model.MovimentacaoEstoque;
import com.comercialcloud.domain.repository.ProdutoRepository;
import jakarta.inject.Inject;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/estoques")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Estoque")
public class EstoqueResource {

    @Inject
    EstoqueService estoqueService;

    @Inject
    ProdutoRepository produtoRepository;

    @GET
    public List<Map<String, Object>> listar(@QueryParam("lojaId") UUID lojaId) {
        return estoqueService.listarPorLoja(lojaId).stream().map(e -> toMap(e)).toList();
    }

    @POST
    @Path("/entrada")
    public void entrada(MovimentoBody request) {
        estoqueService.entrada(request.lojaId(), request.produtoId(), request.quantidade(), request.motivo());
    }

    @POST
    @Path("/saida")
    public void saida(MovimentoBody request) {
        estoqueService.saida(request.lojaId(), request.produtoId(), request.quantidade(), request.motivo());
    }

    @POST
    @Path("/inventario")
    public void inventario(InventarioBody request) {
        estoqueService.inventario(
                request.lojaId(), request.produtoId(), request.quantidadeContada(), request.motivo());
    }

    @POST
    @Path("/ajustes")
    public void ajustar(AjusteBody request) {
        estoqueService.ajustar(request.lojaId(), request.produtoId(), request.quantidade(), request.motivo());
    }

    @PATCH
    @Path("/minimo")
    public Map<String, Object> definirMinimo(MinimoBody request) {
        Estoque estoque =
                estoqueService.definirQuantidadeMinima(
                        request.lojaId(), request.produtoId(), request.quantidadeMinima());
        return toMap(estoque);
    }

    @GET
    @Path("/movimentacoes")
    public List<Map<String, Object>> movimentacoes(
            @QueryParam("lojaId") UUID lojaId,
            @QueryParam("produtoId") UUID produtoId) {
        return estoqueService.listarMovimentacoes(lojaId, produtoId).stream()
                .map(m -> toMovimentacaoMap(m))
                .toList();
    }

    private Map<String, Object> toMap(Estoque estoque) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", estoque.id());
        map.put("tenantId", estoque.tenantId());
        map.put("lojaId", estoque.lojaId());
        map.put("produtoId", estoque.produtoId());
        map.put("quantidadeAtual", estoque.quantidade());
        map.put("quantidadeMinima", estoque.quantidadeMinima());

        produtoRepository.find(estoque.tenantId(), estoque.produtoId()).ifPresent(produto -> {
            map.put("produtoNome", produto.nome());
            map.put("produtoSku", produto.sku());
        });

        return map;
    }

    static Map<String, Object> toMovimentacaoMap(MovimentacaoEstoque mov) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", mov.id());
        map.put("produtoId", mov.produtoId());
        map.put("lojaId", mov.lojaId());
        map.put("tipo", mov.tipo().name());
        map.put("origem", mov.motivo());
        map.put("quantidade", mov.quantidade());
        map.put("quantidadeAnterior", mov.saldoAnterior());
        map.put("quantidadePosterior", mov.saldoPosterior());
        map.put("referenciaId", mov.referenciaId());
        map.put("createdAt", mov.createdAt());
        return map;
    }

    public record AjusteBody(
            @NotNull UUID lojaId,
            @NotNull UUID produtoId,
            @NotNull @DecimalMin("0.0") BigDecimal quantidade,
            @NotBlank String motivo) {}

    public record MovimentoBody(
            @NotNull UUID lojaId,
            @NotNull UUID produtoId,
            @NotNull @DecimalMin("0.01") BigDecimal quantidade,
            @NotBlank String motivo) {}

    public record InventarioBody(
            @NotNull UUID lojaId,
            @NotNull UUID produtoId,
            @NotNull @DecimalMin("0.0") BigDecimal quantidadeContada,
            @NotBlank String motivo) {}

    public record MinimoBody(
            @NotNull UUID lojaId,
            @NotNull UUID produtoId,
            @NotNull @DecimalMin("0.0") BigDecimal quantidadeMinima) {}
}
