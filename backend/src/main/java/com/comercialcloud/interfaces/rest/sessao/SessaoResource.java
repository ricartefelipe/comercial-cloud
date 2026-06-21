package com.comercialcloud.interfaces.rest.sessao;

import com.comercialcloud.domain.repository.UsuarioRepository;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

@Path("/api/v1/sessao")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Sessao")
public class SessaoResource {

    @Inject TenantContext tenantContext;

    @Inject UsuarioRepository usuarioRepository;

    @GET
    public Map<String, Object> contexto() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("tenantId", tenantContext.getTenantId());
        tenantContext.getUserId().ifPresent(userId -> {
            map.put("userId", userId);
            usuarioRepository.find(tenantContext.getTenantId(), userId).ifPresent(usuario -> {
                map.put("nome", usuario.nome());
                map.put("email", usuario.email());
                map.put("role", usuario.role());
            });
        });
        tenantContext.getRole().ifPresent(role -> map.putIfAbsent("role", role));
        return map;
    }
}
