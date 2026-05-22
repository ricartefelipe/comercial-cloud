package com.comercialcloud.interfaces.rest.configuracao;

import com.comercialcloud.application.configuracao.ConfiguracaoService;
import com.comercialcloud.infrastructure.persistence.entity.ConfiguracaoEntity;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/configuracoes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Configuracoes")
public class ConfiguracaoResource {

    @Inject
    ConfiguracaoService configuracaoService;

    @GET
    public List<Map<String, Object>> listar(@QueryParam("lojaId") UUID lojaId) {
        return configuracaoService.listarPorLoja(lojaId).stream().map(ConfiguracaoResource::toMap).toList();
    }

    @PUT
    public Map<String, Object> upsert(ConfiguracaoBody request) {
        return toMap(configuracaoService.upsert(request.lojaId(), request.chave(), request.valor()));
    }

    static Map<String, Object> toMap(ConfiguracaoEntity entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.id);
        map.put("tenantId", entity.tenantId);
        map.put("lojaId", entity.lojaId);
        map.put("chave", entity.chave);
        map.put("valor", entity.valor);
        map.put("updatedAt", entity.updatedAt);
        return map;
    }

    public record ConfiguracaoBody(UUID lojaId, @NotBlank String chave, @NotBlank String valor) {}
}
