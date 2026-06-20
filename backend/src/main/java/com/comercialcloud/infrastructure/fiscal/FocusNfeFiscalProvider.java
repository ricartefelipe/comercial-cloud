package com.comercialcloud.infrastructure.fiscal;

import com.comercialcloud.application.fiscal.CancelamentoRequest;
import com.comercialcloud.application.fiscal.CancelamentoResultado;
import com.comercialcloud.application.fiscal.EmissaoFiscalRequest;
import com.comercialcloud.application.fiscal.EmissaoResultado;
import com.comercialcloud.application.fiscal.FiscalProvider;
import com.comercialcloud.domain.model.ModeloDocumentoFiscal;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provedor fiscal Focus NFe para emissão real de NFC-e (homologação ou produção).
 * Selecione via {@code comercial-cloud.fiscal.provider=focusnfe}.
 */
@ApplicationScoped
public class FocusNfeFiscalProvider implements FiscalProvider {

    private static final Logger LOG = Logger.getLogger(FocusNfeFiscalProvider.class);
    private static final DateTimeFormatter EMISSAO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Inject FocusNfeClient focusNfeClient;

    @Override
    public String nome() {
        return "focusnfe";
    }

    @Override
    public EmissaoResultado emitir(EmissaoFiscalRequest request) {
        if (request.modelo() != ModeloDocumentoFiscal.NFCE) {
            return EmissaoResultado.rejeitado(
                    "Focus NFe: emissão via API implementada para NFC-e; NF-e requer payload distinto",
                    nome());
        }
        if (!focusNfeClient.isConfigured()) {
            return EmissaoResultado.erro(
                    "Focus NFe não configurado. Defina fiscal.focusnfe.enabled=true e fiscal.focusnfe.token");
        }
        if (request.itens() == null || request.itens().isEmpty()) {
            return EmissaoResultado.rejeitado("Nota sem itens", nome());
        }
        if (request.valorTotal() == null || request.valorTotal().compareTo(BigDecimal.ZERO) <= 0) {
            return EmissaoResultado.rejeitado("Valor total inválido", nome());
        }

        String referencia = request.vendaId().toString();
        try {
            FocusNfeClient.NfceResponse resposta =
                    focusNfeClient.emitirNfce(referencia, montarPayload(request), request.ambiente());
            if (resposta == null) {
                resposta = focusNfeClient.consultarNfce(referencia, request.ambiente());
            }
            return mapearEmissao(resposta, request.ambiente(), request.vendaId().toString());
        } catch (Exception e) {
            LOG.errorf(e, "Falha Focus NFe ao emitir NFC-e ref=%s", referencia);
            if (isDuplicidade(e)) {
                try {
                    FocusNfeClient.NfceResponse existente =
                            focusNfeClient.consultarNfce(referencia, request.ambiente());
                    return mapearEmissao(existente, request.ambiente(), request.vendaId().toString());
                } catch (Exception consulta) {
                    LOG.warnf(consulta, "Falha ao consultar NFC-e duplicada ref=%s", referencia);
                }
            }
            return EmissaoResultado.erro("Focus NFe: " + e.getMessage());
        }
    }

    @Override
    public CancelamentoResultado cancelar(CancelamentoRequest request) {
        if (!focusNfeClient.isConfigured()) {
            return CancelamentoResultado.falha("Focus NFe não configurado");
        }
        if (request.justificativa() == null || request.justificativa().trim().length() < 15) {
            return CancelamentoResultado.falha("Justificativa deve ter ao menos 15 caracteres");
        }
        String referencia =
                request.referenciaEmissao() != null && !request.referenciaEmissao().isBlank()
                        ? request.referenciaEmissao()
                        : extrairReferencia(request.chaveAcesso());
        if (referencia == null) {
            return CancelamentoResultado.falha("Referência Focus NFe ausente (use providerRef da emissão)");
        }
        try {
            FocusNfeClient.NfceResponse resposta =
                    focusNfeClient.cancelarNfce(referencia, request.justificativa(), request.ambiente());
            if (resposta != null && "cancelado".equalsIgnoreCase(resposta.status)) {
                return CancelamentoResultado.ok(resposta.numero);
            }
            return CancelamentoResultado.falha(
                    resposta != null ? coalesce(resposta.mensagem_sefaz, resposta.mensagem, "Cancelamento rejeitado")
                            : "Resposta vazia do Focus NFe");
        } catch (Exception e) {
            LOG.errorf(e, "Falha Focus NFe ao cancelar ref=%s", referencia);
            return CancelamentoResultado.falha("Focus NFe: " + e.getMessage());
        }
    }

    private EmissaoResultado mapearEmissao(
            FocusNfeClient.NfceResponse resposta,
            com.comercialcloud.domain.model.AmbienteFiscal ambiente,
            String referencia) {
        if (resposta == null) {
            return EmissaoResultado.erro("Resposta vazia do Focus NFe");
        }
        String status = resposta.status != null ? resposta.status.toLowerCase() : "";
        if ("autorizado".equals(status)) {
            String danfe = focusNfeClient.urlDanfeCompleta(resposta, ambiente);
            return EmissaoResultado.autorizado(
                    resposta.chave_nfe,
                    resposta.numero,
                    danfe,
                    referencia);
        }
        if ("processando_autorizacao".equals(status)) {
            return EmissaoResultado.rejeitado(
                    "Nota em processamento na SEFAZ; reprocessar documento pendente em instantes",
                    nome());
        }
        String motivo = coalesce(resposta.mensagem_sefaz, resposta.mensagem, "Emissão rejeitada pela SEFAZ");
        return EmissaoResultado.rejeitado(motivo, nome());
    }

    private Map<String, Object> montarPayload(EmissaoFiscalRequest request) {
        Map<String, Object> nfce = new HashMap<>();
        nfce.put("natureza_operacao", "VENDA AO CONSUMIDOR");
        nfce.put(
                "data_emissao",
                OffsetDateTime.now(ZoneOffset.of("-03:00")).format(EMISSAO));
        nfce.put("cnpj_emitente", soDigitos(request.emitenteCnpj()));
        nfce.put("inscricao_estadual_emitente", request.emitenteInscricaoEstadual());
        nfce.put("nome_emitente", request.emitenteRazaoSocial());
        nfce.put("nome_fantasia_emitente", request.emitenteRazaoSocial());
        nfce.put("uf_emitente", request.uf());

        if (request.cscId() != null && !request.cscId().isBlank()) {
            nfce.put("id_token_nfce_producao", request.cscId());
            nfce.put("csc_nfce_producao", request.csc());
        }

        if (request.destinatarioDocumento() != null && !request.destinatarioDocumento().isBlank()) {
            String doc = soDigitos(request.destinatarioDocumento());
            if (doc.length() == 11) {
                nfce.put("cpf_destinatario", doc);
            } else if (doc.length() == 14) {
                nfce.put("cnpj_destinatario", doc);
            }
            if (request.destinatarioNome() != null) {
                nfce.put("nome_destinatario", request.destinatarioNome());
            }
        }

        List<Map<String, Object>> items = new ArrayList<>();
        int numero = 1;
        for (EmissaoFiscalRequest.EmissaoItem item : request.itens()) {
            Map<String, Object> linha = new HashMap<>();
            linha.put("numero_item", Integer.toString(numero));
            linha.put("codigo_produto", Integer.toString(numero));
            numero++;
            linha.put("descricao", item.descricao());
            linha.put("codigo_ncm", item.ncm());
            linha.put("cfop", item.cfop());
            linha.put("unidade_comercial", item.unidade());
            linha.put("unidade_tributavel", item.unidade());
            linha.put("quantidade_comercial", formatDecimal(item.quantidade()));
            linha.put("quantidade_tributavel", formatDecimal(item.quantidade()));
            linha.put("valor_unitario_comercial", formatDecimal(item.valorUnitario()));
            linha.put("valor_unitario_tributavel", formatDecimal(item.valorUnitario()));
            linha.put("valor_bruto", formatDecimal(item.valorTotal()));
            linha.put("icms_origem", "0");
            linha.put("icms_situacao_tributaria", "102");
            items.add(linha);
        }
        nfce.put("items", items);

        Map<String, Object> pagamento = new HashMap<>();
        pagamento.put("forma_pagamento", "01");
        pagamento.put("valor_pagamento", formatDecimal(request.valorTotal()));
        nfce.put("formas_pagamento", List.of(pagamento));

        return nfce;
    }

    private static String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String soDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    private static String coalesce(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    private static boolean isDuplicidade(Exception e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("422") || msg.toLowerCase().contains("duplic"));
    }

    /** Cancelamento usa providerRef gravado na emissão (vendaId) quando chave não traz referência. */
    private static String extrairReferencia(String chaveOuRef) {
        if (chaveOuRef == null || chaveOuRef.isBlank()) {
            return null;
        }
        if (chaveOuRef.contains(":")) {
            return chaveOuRef.substring(chaveOuRef.indexOf(':') + 1);
        }
        if (chaveOuRef.length() == 36 && chaveOuRef.contains("-")) {
            return chaveOuRef;
        }
        return null;
    }
}
