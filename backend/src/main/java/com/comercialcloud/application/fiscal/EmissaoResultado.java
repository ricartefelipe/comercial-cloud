package com.comercialcloud.application.fiscal;

import com.comercialcloud.domain.model.StatusDocumentoFiscal;

public record EmissaoResultado(
        StatusDocumentoFiscal status,
        String chaveAcesso,
        String protocolo,
        String motivoRejeicao,
        String urlDanfe,
        String providerRef) {

    public static EmissaoResultado autorizado(
            String chaveAcesso, String protocolo, String urlDanfe, String providerRef) {
        return new EmissaoResultado(
                StatusDocumentoFiscal.AUTORIZADO, chaveAcesso, protocolo, null, urlDanfe, providerRef);
    }

    public static EmissaoResultado rejeitado(String motivo, String providerRef) {
        return new EmissaoResultado(
                StatusDocumentoFiscal.REJEITADO, null, null, motivo, null, providerRef);
    }

    public static EmissaoResultado erro(String motivo) {
        return new EmissaoResultado(StatusDocumentoFiscal.ERRO, null, null, motivo, null, null);
    }
}
