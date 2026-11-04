package com.comercialcloud.interfaces.rest.publico;

import com.comercialcloud.domain.model.Usuario;
import com.comercialcloud.domain.repository.UsuarioRepository;
import com.comercialcloud.domain.shared.DemoConstants;
import com.comercialcloud.infrastructure.security.SessaoDemonstracao;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Entrada da demonstração pública: a conta de demonstração (e-mail da carga inicial e a senha publicada) troca as
 * credenciais por uma sessão do servidor. Fora do modo de demonstração a rota não existe.
 */
@Path("/api/v1/public/demo/sessao")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Demonstração")
public class SessaoDemonstracaoResource {

    public record Credenciais(String email, String senha) {}

    @Inject
    SessaoDemonstracao sessao;

    @Inject
    UsuarioRepository usuarios;

    @POST
    public Map<String, Object> entrar(Credenciais credenciais) {
        if (!sessao.ativa()) {
            throw new NotFoundException();
        }
        if (credenciais == null || credenciais.email() == null || credenciais.email().isBlank()) {
            throw new NotAuthorizedException("Bearer");
        }
        boolean senhaConfere = sessao.senhaConfere(credenciais.senha());
        Optional<Usuario> usuario = usuarios
                .findByEmail(DemoConstants.TENANT_DEMO, credenciais.email().trim().toLowerCase(Locale.ROOT))
                .filter(Usuario::ativo);
        if (!senhaConfere || usuario.isEmpty()) {
            throw new NotAuthorizedException("Bearer");
        }
        return Map.of(
                "access_token", sessao.emitir(usuario.get()),
                "token_type", "Bearer",
                "expires_in", sessao.validade().toSeconds());
    }
}
