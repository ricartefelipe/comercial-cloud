package com.comercialcloud.interfaces.rest.produto;

import com.comercialcloud.application.produto.ProdutoImagemService;
import com.comercialcloud.application.produto.ProdutoImportService;
import com.comercialcloud.application.produto.ProdutoService;
import com.comercialcloud.domain.model.Produto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.inject.Inject;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
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

@Path("/api/v1/produtos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Produtos")
public class ProdutoResource {

    @Inject ProdutoService produtoService;

    @Inject ProdutoImagemService produtoImagemService;

    @Inject ProdutoImportService produtoImportService;

    @GET
    @Path("/categorias")
    public List<String> categorias() {
        return produtoService.listarCategorias();
    }

    @POST
    @Path("/importacao")
    public Map<String, Object> importar(ImportacaoBody request) {
        return produtoImportService.importarCsv(request.csv());
    }

    @POST
    @Path("/{id}/imagem")
    public Map<String, Object> uploadImagem(@PathParam("id") UUID id, ImagemBody request) {
        String url = produtoImagemService.salvarImagemBase64(id, request.contentType(), request.conteudoBase64());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("imagemUrl", url);
        return map;
    }

    @GET
    @Path("/midias/{tenantId}/{fileName}")
    @Produces({"image/jpeg", "image/png", "image/webp", "image/gif"})
    public Response midia(@PathParam("tenantId") UUID tenantId, @PathParam("fileName") String fileName) {
        byte[] bytes = produtoImagemService.lerImagem(tenantId, fileName);
        return Response.ok(bytes, produtoImagemService.contentTypeDeArquivo(fileName)).build();
    }

    @POST
    public Response criar(ProdutoBody request) {
        var produto = produtoService.criar(request.toDados());
        return Response.status(Response.Status.CREATED).entity(toMap(produto)).build();
    }

    @GET
    @Path("/{id}")
    public Map<String, Object> buscar(@PathParam("id") UUID id) {
        return toMap(produtoService.buscarPorId(id));
    }

    @GET
    public Map<String, Object> listar(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        var result = produtoService.listar(page, size);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", result.content().stream().map(ProdutoResource::toMap).toList());
        response.put("totalElements", result.totalElements());
        response.put("totalPages", result.totalPages());
        response.put("size", result.size());
        response.put("number", result.number());
        return response;
    }

    @GET
    @Path("/busca")
    public List<Map<String, Object>> buscar(@QueryParam("termo") String termo) {
        return produtoService.buscar(termo).stream().map(ProdutoResource::toMap).toList();
    }

    @GET
    @Path("/codigo-barras/{codigo}")
    public Map<String, Object> buscarPorCodigoBarras(@PathParam("codigo") String codigo) {
        return toMap(produtoService.buscarPorCodigoBarras(codigo));
    }

    @PUT
    @Path("/{id}")
    public Map<String, Object> atualizar(@PathParam("id") UUID id, ProdutoBody request) {
        return toMap(produtoService.atualizar(id, request.toDados()));
    }

    @PATCH
    @Path("/{id}/ativar")
    public Map<String, Object> ativar(@PathParam("id") UUID id) {
        return toMap(produtoService.ativar(id));
    }

    @PATCH
    @Path("/{id}/inativar")
    public Map<String, Object> inativar(@PathParam("id") UUID id) {
        return toMap(produtoService.inativar(id));
    }

    static Map<String, Object> toMap(Produto produto) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", produto.id());
        map.put("tenantId", produto.tenantId());
        map.put("sku", produto.sku());
        map.put("codigoBarras", produto.codigoBarras());
        map.put("nome", produto.nome());
        map.put("descricao", produto.descricao());
        map.put("imagemUrl", produto.imagemUrl());
        map.put("categoria", produto.categoria());
        map.put("unidadeMedida", "UN");
        map.put("precoVenda", produto.preco());
        map.put("preco", produto.preco());
        map.put("precoCusto", produto.precoCusto());
        map.put("ativo", produto.ativo());
        map.put("controlaEstoque", produto.controlaEstoque());
        map.put("createdAt", produto.createdAt());
        map.put("updatedAt", produto.updatedAt());
        return map;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProdutoBody(
            @NotBlank String sku,
            String codigoBarras,
            @NotBlank String nome,
            String descricao,
            String imagemUrl,
            String categoria,
            String unidadeMedida,
            @DecimalMin("0.0") BigDecimal precoVenda,
            @DecimalMin("0.0") BigDecimal preco,
            @DecimalMin("0.0") BigDecimal precoCusto,
            Boolean controlaEstoque) {

        BigDecimal resolverPreco() {
            if (precoVenda != null) {
                return precoVenda;
            }
            if (preco != null) {
                return preco;
            }
            throw new IllegalArgumentException("preco ou precoVenda é obrigatório");
        }

        ProdutoService.DadosProduto toDados() {
            return new ProdutoService.DadosProduto(
                    sku,
                    codigoBarras,
                    nome,
                    descricao,
                    imagemUrl,
                    categoria,
                    resolverPreco(),
                    precoCusto != null ? precoCusto : BigDecimal.ZERO,
                    controlaEstoque == null || controlaEstoque);
        }
    }

    public record ImagemBody(String contentType, String conteudoBase64) {}

    public record ImportacaoBody(@NotBlank String csv) {}
}
