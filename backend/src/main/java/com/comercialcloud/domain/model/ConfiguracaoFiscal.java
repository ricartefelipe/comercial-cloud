package com.comercialcloud.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Configuração fiscal por loja. Guarda os dados do emitente e o controle de
 * numeração necessários para emissão de NFC-e/NF-e. Dados sensíveis de
 * certificado/CSC são mantidos fora do domínio (apenas referência).
 */
public record ConfiguracaoFiscal(
        UUID id,
        UUID tenantId,
        UUID lojaId,
        String cnpj,
        String inscricaoEstadual,
        String razaoSocial,
        RegimeTributario regimeTributario,
        AmbienteFiscal ambiente,
        String uf,
        String cscId,
        String csc,
        int serieNfce,
        long proximoNumeroNfce,
        boolean habilitado,
        Instant createdAt,
        Instant updatedAt) {

    public ConfiguracaoFiscal comNumeracao(int novaSerie, long novoProximoNumero, Instant updated) {
        return new ConfiguracaoFiscal(
                id, tenantId, lojaId, cnpj, inscricaoEstadual, razaoSocial, regimeTributario,
                ambiente, uf, cscId, csc, novaSerie, novoProximoNumero, habilitado, createdAt, updated);
    }
}
