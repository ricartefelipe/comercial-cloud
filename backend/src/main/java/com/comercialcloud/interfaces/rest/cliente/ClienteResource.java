package com.comercialcloud.interfaces.rest.cliente;

import com.comercialcloud.application.cliente.ClienteService;
import com.comercialcloud.domain.model.Cliente;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
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

@Path("/api/clientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Clientes")
public class ClienteResource {

    @Inject
    ClienteService clienteService;

    @POST
    public Response criar(ClienteBody request) {
        var cliente = clienteService.criar(
                request.nome(), request.cpfCnpj(), request.email(), request.telefone());
        return Response.status(Response.Status.CREATED).entity(toMap(cliente)).build();
    }

    @GET
    @Path("/{id}")
    public Map<String, Object> buscar(@PathParam("id") UUID id) {
        return toMap(clienteService.buscarPorId(id));
    }

    @GET
    public List<Map<String, Object>> listar(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return clienteService.listar(page, size).stream().map(c -> toMap(c)).toList();
    }

    @PUT
    @Path("/{id}")
    public Map<String, Object> atualizar(@PathParam("id") UUID id, ClienteBody request) {
        return toMap(clienteService.atualizar(
                id, request.nome(), request.cpfCnpj(), request.email(), request.telefone()));
    }

    @DELETE
    @Path("/{id}")
    public Response excluir(@PathParam("id") UUID id) {
        clienteService.excluir(id);
        return Response.noContent().build();
    }

    static Map<String, Object> toMap(Cliente cliente) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", cliente.id());
        map.put("tenantId", cliente.tenantId());
        map.put("nome", cliente.nome());
        map.put("cpfCnpj", cliente.cpfCnpj());
        map.put("email", cliente.email());
        map.put("telefone", cliente.telefone());
        map.put("ativo", cliente.ativo());
        map.put("createdAt", cliente.createdAt());
        return map;
    }

    public record ClienteBody(
            @NotBlank String nome,
            String cpfCnpj,
            String email,
            String telefone) {}
}
