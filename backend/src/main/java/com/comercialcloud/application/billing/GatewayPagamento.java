package com.comercialcloud.application.billing;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Porta de saída para cobrança de faturas de assinatura do SaaS. O adapter
 * sandbox embutido permite simular aprovação/recusa sem integração real; um
 * adapter de provedor (Asaas, Iugu, Stripe, Pagar.me) pode ser plugado depois
 * implementando esta interface e selecionado via
 * {@code comercial-cloud.billing.gateway}.
 */
public interface GatewayPagamento {

    String nome();

    ResultadoCobranca cobrar(CobrancaRequest request);

    record CobrancaRequest(UUID tenantId, UUID faturaId, BigDecimal valor, String descricao) {}

    record ResultadoCobranca(boolean aprovado, String transacaoRef, String motivoRecusa) {

        public static ResultadoCobranca aprovado(String transacaoRef) {
            return new ResultadoCobranca(true, transacaoRef, null);
        }

        public static ResultadoCobranca recusado(String motivo) {
            return new ResultadoCobranca(false, null, motivo);
        }
    }
}
