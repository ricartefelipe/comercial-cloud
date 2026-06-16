package com.comercialcloud.application.fiscal;

/**
 * Porta de saída para emissão de documentos fiscais eletrônicos.
 *
 * <p>O contrato é agnóstico de fornecedor: o adapter sandbox embutido permite
 * operar de ponta a ponta sem credenciais reais e um adapter real (PlugNotas,
 * Focus NFe, eNotas, etc.) pode ser plugado depois implementando esta interface
 * e selecionando-o via configuração {@code comercial-cloud.fiscal.provider}.
 */
public interface FiscalProvider {

    String nome();

    EmissaoResultado emitir(EmissaoFiscalRequest request);

    CancelamentoResultado cancelar(CancelamentoRequest request);
}
