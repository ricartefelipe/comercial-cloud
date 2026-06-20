package com.comercialcloud.infrastructure.fiscal;

import com.comercialcloud.application.fiscal.EmissaoFiscalRequest;
import com.comercialcloud.domain.model.AmbienteFiscal;
import com.comercialcloud.domain.model.ModeloDocumentoFiscal;
import com.comercialcloud.domain.model.RegimeTributario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FocusNfeFiscalProviderTest {

    @Test
    void rejeitaNfeModelo() {
        FocusNfeFiscalProvider provider = new FocusNfeFiscalProvider();
        var request =
                new EmissaoFiscalRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ModeloDocumentoFiscal.NFE,
                        AmbienteFiscal.HOMOLOGACAO,
                        "12345678000199",
                        "101942171617",
                        "Loja Demo LTDA",
                        RegimeTributario.SIMPLES_NACIONAL,
                        "SP",
                        "1",
                        "token",
                        1,
                        1L,
                        BigDecimal.TEN,
                        "12345678901",
                        "Cliente",
                        List.of(
                                new EmissaoFiscalRequest.EmissaoItem(
                                        "Produto",
                                        BigDecimal.ONE,
                                        BigDecimal.TEN,
                                        BigDecimal.TEN,
                                        "21069090",
                                        "5102",
                                        "UN")));

        var resultado = provider.emitir(request);

        assertTrue(resultado.motivoRejeicao().contains("NFC-e"));
        assertEquals("focusnfe", resultado.providerRef());
    }
}
