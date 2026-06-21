package com.comercialcloud.interfaces.rest.publico;

import com.comercialcloud.application.usuario.ConviteUsuarioService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

@Path("/api/v1/public/convites")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Publico")
public class PublicConviteResource {

    @Inject ConviteUsuarioService conviteUsuarioService;

    @GET
    @Path("/{token}")
    public Map<String, Object> consultar(@PathParam("token") String token) {
        return conviteUsuarioService.consultarPublico(token);
    }

    @POST
    @Path("/{token}/aceitar")
    public Map<String, Object> aceitar(@PathParam("token") String token, AceitarBody body) {
        return conviteUsuarioService.aceitar(token, body.senha(), body.nome());
    }

    public record AceitarBody(@NotBlank String senha, String nome) {}
}
