package com.comercialcloud.application.fiscal;

import com.comercialcloud.domain.model.AmbienteFiscal;

public record CancelamentoRequest(
        String chaveAcesso, String protocolo, String justificativa, AmbienteFiscal ambiente) {}
