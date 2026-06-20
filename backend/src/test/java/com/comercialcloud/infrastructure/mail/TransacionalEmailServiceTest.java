package com.comercialcloud.infrastructure.mail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TransacionalEmailServiceTest {

    @Test
    void metodosPublicosAceitamParametrosNulosSemExcecao() {
        TransacionalEmailService service = new TransacionalEmailService();
        assertDoesNotThrow(
                () -> {
                    service.enviarBoasVindas(null, "Nome", "ESSENCIAL", null);
                    service.enviarNovaFatura(null, "2026-06", null, null, null);
                    service.enviarSuspensao(null, "2026-06");
                    service.enviarPagamentoConfirmado(null, "2026-06");
                });
    }
}
