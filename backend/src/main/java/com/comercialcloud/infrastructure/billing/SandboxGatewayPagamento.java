package com.comercialcloud.infrastructure.billing;

import com.comercialcloud.application.billing.GatewayPagamento;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.UUID;

/**
 * Gateway de pagamento embutido para desenvolvimento/sandbox. O comportamento é
 * configurável por {@code comercial-cloud.billing.sandbox-comportamento}:
 * <ul>
 *   <li>{@code APROVAR_SEMPRE} (padrão): toda cobrança é aprovada;</li>
 *   <li>{@code RECUSAR_SEMPRE}: toda cobrança é recusada (permite demonstrar o
 *       fluxo de inadimplência/suspensão).</li>
 * </ul>
 */
@ApplicationScoped
public class SandboxGatewayPagamento implements GatewayPagamento {

    @Inject
    @ConfigProperty(
            name = "comercial-cloud.billing.sandbox-comportamento",
            defaultValue = "APROVAR_SEMPRE")
    String comportamento;

    @Override
    public String nome() {
        return "sandbox";
    }

    @Override
    public ResultadoCobranca cobrar(CobrancaRequest request) {
        if ("RECUSAR_SEMPRE".equalsIgnoreCase(comportamento)) {
            return ResultadoCobranca.recusado("Pagamento recusado (sandbox)");
        }
        return ResultadoCobranca.aprovado("SANDBOX-" + UUID.randomUUID());
    }
}
