package com.comercialcloud.application.fiscal;

public record CancelamentoResultado(boolean sucesso, String protocolo, String motivo) {

    public static CancelamentoResultado ok(String protocolo) {
        return new CancelamentoResultado(true, protocolo, null);
    }

    public static CancelamentoResultado falha(String motivo) {
        return new CancelamentoResultado(false, null, motivo);
    }
}
