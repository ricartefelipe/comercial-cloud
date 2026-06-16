package com.comercialcloud.application.fiscal;

import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.domain.model.AmbienteFiscal;
import com.comercialcloud.domain.model.RegimeTributario;
import com.comercialcloud.infrastructure.audit.AuditService;
import com.comercialcloud.infrastructure.persistence.entity.ConfiguracaoFiscalEntity;
import com.comercialcloud.infrastructure.persistence.entity.LojaEntity;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ConfiguracaoFiscalService {

    @Inject
    TenantContext tenantContext;

    @Inject
    AuditService auditService;

    public record UpsertConfiguracao(
            UUID lojaId,
            String cnpj,
            String inscricaoEstadual,
            String razaoSocial,
            RegimeTributario regimeTributario,
            AmbienteFiscal ambiente,
            String uf,
            String cscId,
            String csc,
            Integer serieNfce,
            boolean habilitado) {}

    public Optional<ConfiguracaoFiscalEntity> buscarPorLoja(UUID tenantId, UUID lojaId) {
        return ConfiguracaoFiscalEntity.<ConfiguracaoFiscalEntity>find(
                        "tenantId = ?1 and lojaId = ?2", tenantId, lojaId)
                .firstResultOptional();
    }

    public Optional<ConfiguracaoFiscalEntity> buscarPorLojaAtual(UUID lojaId) {
        return buscarPorLoja(tenantContext.getTenantId(), lojaId);
    }

    @Transactional
    public ConfiguracaoFiscalEntity salvar(UpsertConfiguracao cmd) {
        UUID tenantId = tenantContext.getTenantId();
        LojaEntity.<LojaEntity>find("tenantId = ?1 and id = ?2", tenantId, cmd.lojaId())
                .firstResultOptional()
                .orElseThrow(() -> new NotFoundException("LOJA_NOT_FOUND", "Loja não encontrada"));

        Instant agora = Instant.now();
        ConfiguracaoFiscalEntity config =
                buscarPorLoja(tenantId, cmd.lojaId()).orElseGet(ConfiguracaoFiscalEntity::new);

        boolean novo = config.id == null;
        if (novo) {
            config.id = UUID.randomUUID();
            config.tenantId = tenantId;
            config.lojaId = cmd.lojaId();
            config.serieNfce = cmd.serieNfce() != null ? cmd.serieNfce() : 1;
            config.proximoNumeroNfce = 1L;
            config.createdAt = agora;
        } else if (cmd.serieNfce() != null) {
            config.serieNfce = cmd.serieNfce();
        }

        config.cnpj = somenteDigitos(cmd.cnpj());
        config.inscricaoEstadual = cmd.inscricaoEstadual();
        config.razaoSocial = cmd.razaoSocial();
        config.regimeTributario = cmd.regimeTributario().name();
        config.ambiente = cmd.ambiente().name();
        config.uf = cmd.uf() != null ? cmd.uf().toUpperCase() : null;
        config.cscId = cmd.cscId();
        config.csc = cmd.csc();
        config.habilitado = cmd.habilitado();
        config.updatedAt = agora;
        config.persist();

        auditService.registrar(
                "ConfiguracaoFiscal", config.id, novo ? "CRIAR" : "ATUALIZAR", "Loja: " + cmd.lojaId());
        return config;
    }

    /**
     * Aloca e consome o próximo número de NFC-e da loja de forma transacional.
     */
    @Transactional
    public long proximoNumero(UUID tenantId, UUID lojaId) {
        ConfiguracaoFiscalEntity config =
                buscarPorLoja(tenantId, lojaId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "CONFIG_FISCAL_NOT_FOUND", "Configuração fiscal da loja não encontrada"));
        long numero = config.proximoNumeroNfce;
        config.proximoNumeroNfce = numero + 1;
        config.updatedAt = Instant.now();
        config.persist();
        return numero;
    }

    private static String somenteDigitos(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }
}
