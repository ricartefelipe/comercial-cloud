package com.comercialcloud.application.fiscal;

import com.comercialcloud.domain.model.AmbienteFiscal;
import com.comercialcloud.domain.model.ModeloDocumentoFiscal;
import com.comercialcloud.domain.model.RegimeTributario;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record EmissaoFiscalRequest(
        UUID tenantId,
        UUID lojaId,
        UUID vendaId,
        ModeloDocumentoFiscal modelo,
        AmbienteFiscal ambiente,
        String emitenteCnpj,
        String emitenteInscricaoEstadual,
        String emitenteRazaoSocial,
        RegimeTributario regimeTributario,
        String uf,
        String cscId,
        String csc,
        int serie,
        long numero,
        BigDecimal valorTotal,
        String destinatarioDocumento,
        String destinatarioNome,
        List<EmissaoItem> itens) {

    public record EmissaoItem(
            String descricao,
            BigDecimal quantidade,
            BigDecimal valorUnitario,
            BigDecimal valorTotal,
            String ncm,
            String cfop,
            String unidade) {}
}
