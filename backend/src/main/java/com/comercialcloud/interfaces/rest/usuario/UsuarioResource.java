package com.comercialcloud.interfaces.rest.usuario;

import com.comercialcloud.application.usuario.UsuarioService;
import com.comercialcloud.domain.model.Usuario;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

@Path("/api/v1/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Usuarios")
public class UsuarioResource {

    @Inject
    UsuarioService usuarioService;

    @GET
    public List<Map<String, Object>> listar() {
        return usuarioService.listar().stream().map(UsuarioResource::toMap).toList();
    }

    @POST
    public Response criar(UsuarioBody request) {
        Usuario usuario = usuarioService.criar(request.nome(), request.email(), request.role());
        return Response.status(Response.Status.CREATED).entity(toMap(usuario)).build();
    }

    @PUT
    @Path("/{id}")
    public Map<String, Object> atualizar(@PathParam("id") UUID id, UsuarioBody request) {
        return toMap(usuarioService.atualizar(id, request.nome(), request.email(), request.role()));
    }

    @DELETE
    @Path("/{id}")
    public Response inativar(@PathParam("id") UUID id) {
        usuarioService.inativar(id);
        return Response.noContent().build();
    }

    static Map<String, Object> toMap(Usuario usuario) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", usuario.id());
        map.put("tenantId", usuario.tenantId());
        map.put("nome", usuario.nome());
        map.put("email", usuario.email());
        map.put("role", usuario.role());
        map.put("ativo", usuario.ativo());
        map.put("createdAt", usuario.createdAt());
        return map;
    }

    public record UsuarioBody(@NotBlank String nome, @NotBlank String email, @NotBlank String role) {}
}
