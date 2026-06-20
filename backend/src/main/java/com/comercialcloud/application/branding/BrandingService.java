package com.comercialcloud.application.branding;

import com.comercialcloud.application.configuracao.ConfiguracaoService;
import com.comercialcloud.infrastructure.persistence.entity.ConfiguracaoEntity;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class BrandingService {

    static final String KEY_NOME = "branding.nome_exibicao";
    static final String KEY_COR = "branding.cor_primaria";
    static final String KEY_LOGO = "branding.logo_url";

    private static final Set<String> CHAVES_PERMITIDAS = Set.of(KEY_NOME, KEY_COR, KEY_LOGO);

    @Inject TenantContext tenantContext;
    @Inject ConfiguracaoService configuracaoService;

    public Map<String, String> obter() {
        Map<String, String> branding = new LinkedHashMap<>();
        branding.put("nomeExibicao", valor(KEY_NOME, "ComercialCloud"));
        branding.put("corPrimaria", valor(KEY_COR, "#4f7cff"));
        branding.put("logoUrl", valor(KEY_LOGO, ""));
        return branding;
    }

    @Transactional
    public Map<String, String> salvar(Map<String, String> valores) {
        if (valores.containsKey("nomeExibicao")) {
            upsert(KEY_NOME, valores.get("nomeExibicao"));
        }
        if (valores.containsKey("corPrimaria")) {
            upsert(KEY_COR, valores.get("corPrimaria"));
        }
        if (valores.containsKey("logoUrl")) {
            upsert(KEY_LOGO, valores.get("logoUrl"));
        }
        return obter();
    }

    private void upsert(String chave, String valor) {
        if (!CHAVES_PERMITIDAS.contains(chave)) {
            return;
        }
        configuracaoService.upsert(null, chave, valor != null ? valor : "");
    }

    private String valor(String chave, String padrao) {
        return configuracaoService.listarPorLoja(null).stream()
                .filter(c -> chave.equals(c.chave))
                .map(c -> c.valor)
                .findFirst()
                .orElse(padrao);
    }
}
