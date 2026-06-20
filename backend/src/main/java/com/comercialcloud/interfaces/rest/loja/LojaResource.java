package com.comercialcloud.interfaces.rest.loja;

import com.comercialcloud.application.loja.LojaService;
import com.comercialcloud.domain.model.Loja;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
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

@Path("/api/v1/lojas")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Lojas")
public class LojaResource {

    @Inject LojaService lojaService;

    @GET
    public List<Map<String, Object>> listar() {
        return lojaService.listar().stream().map(LojaResource::toMap).toList();
    }

    @GET
    @Path("/resumo")
    public Map<String, Object> resumo() {
        LojaService.LojaResumo r = lojaService.resumo();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("limite", r.limite());
        map.put("ativas", r.ativas());
        map.put("podeCriar", r.podeCriar());
        return map;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response criar(CriarLojaBody body) {
        Loja loja = lojaService.criar(body.nome());
        return Response.status(Response.Status.CREATED).entity(toMap(loja)).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, Object> atualizar(@PathParam("id") UUID id, AtualizarLojaBody body) {
        return toMap(lojaService.atualizar(id, body.nome(), body.ativa()));
    }

    private static Map<String, Object> toMap(Loja loja) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", loja.id());
        map.put("tenantId", loja.tenantId());
        map.put("nome", loja.nome());
        map.put("ativa", loja.ativa());
        map.put("createdAt", loja.createdAt());
        return map;
    }

    public record CriarLojaBody(@NotBlank String nome) {}

    public record AtualizarLojaBody(String nome, Boolean ativa) {}
}
