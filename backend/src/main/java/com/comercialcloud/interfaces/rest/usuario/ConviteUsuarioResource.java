package com.comercialcloud.interfaces.rest.usuario;

import com.comercialcloud.application.usuario.ConviteUsuarioService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Path("/api/v1/usuarios/convites")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Usuarios")
public class ConviteUsuarioResource {

    @Inject ConviteUsuarioService conviteUsuarioService;

    @GET
    public List<Map<String, Object>> listarPendentes() {
        return conviteUsuarioService.listarPendentes();
    }

    @POST
    public Response convidar(ConviteBody request) {
        Map<String, Object> convite =
                conviteUsuarioService.convidar(request.nome(), request.email(), request.role());
        return Response.status(Response.Status.CREATED).entity(convite).build();
    }

    public record ConviteBody(@NotBlank String nome, @NotBlank String email, @NotBlank String role) {}
}
