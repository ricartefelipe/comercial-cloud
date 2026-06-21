package com.comercialcloud.interfaces.rest.auditoria;

import com.comercialcloud.application.auditoria.AuditoriaConsultaService;
import com.comercialcloud.domain.model.Auditoria;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/api/v1/auditoria")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Auditoria")
public class AuditoriaResource {

    @Inject AuditoriaConsultaService auditoriaConsultaService;

    @GET
    public List<Map<String, Object>> listar(@QueryParam("limit") @DefaultValue("100") int limit) {
        return auditoriaConsultaService.listar(limit).stream().map(AuditoriaResource::toMap).toList();
    }

    static Map<String, Object> toMap(Auditoria auditoria) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", auditoria.id());
        map.put("tenantId", auditoria.tenantId());
        map.put("entidade", auditoria.entidade());
        map.put("entidadeId", auditoria.entidadeId());
        map.put("acao", auditoria.acao());
        map.put("usuarioId", auditoria.usuarioId());
        map.put("detalhes", auditoria.detalhes());
        map.put("createdAt", auditoria.createdAt());
        return map;
    }
}
