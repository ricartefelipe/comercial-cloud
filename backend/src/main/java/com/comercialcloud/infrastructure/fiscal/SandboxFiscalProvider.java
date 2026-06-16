package com.comercialcloud.infrastructure.fiscal;

import com.comercialcloud.application.fiscal.CancelamentoRequest;
import com.comercialcloud.application.fiscal.CancelamentoResultado;
import com.comercialcloud.application.fiscal.EmissaoFiscalRequest;
import com.comercialcloud.application.fiscal.EmissaoResultado;
import com.comercialcloud.application.fiscal.FiscalProvider;
import com.comercialcloud.domain.model.ModeloDocumentoFiscal;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Provedor fiscal embutido para ambiente de desenvolvimento/sandbox. Não se
 * comunica com a SEFAZ: gera uma chave de acesso de 44 dígitos com dígito
 * verificador mod-11, protocolo e DANFE fictícios, aplicando regras de validação
 * equivalentes às reais para tornar o fluxo demonstrável de ponta a ponta.
 */
@ApplicationScoped
public class SandboxFiscalProvider implements FiscalProvider {

    private static final DateTimeFormatter AAMM = DateTimeFormatter.ofPattern("yyMM");

    @Override
    public String nome() {
        return "sandbox";
    }

    @Override
    public EmissaoResultado emitir(EmissaoFiscalRequest request) {
        if (request.valorTotal() == null || request.valorTotal().compareTo(BigDecimal.ZERO) <= 0) {
            return EmissaoResultado.rejeitado("Valor total da nota deve ser maior que zero", nome());
        }
        if (request.itens() == null || request.itens().isEmpty()) {
            return EmissaoResultado.rejeitado("Nota sem itens", nome());
        }
        if (request.modelo() == ModeloDocumentoFiscal.NFE
                && (request.destinatarioDocumento() == null || request.destinatarioDocumento().isBlank())) {
            return EmissaoResultado.rejeitado("Destinatário (CPF/CNPJ) é obrigatório para NF-e", nome());
        }

        String chave = gerarChaveAcesso(request);
        String protocolo = gerarProtocolo();
        String urlDanfe =
                "https://sandbox.comercialcloud.local/danfe/" + chave + ".pdf";
        return EmissaoResultado.autorizado(chave, protocolo, urlDanfe, nome());
    }

    @Override
    public CancelamentoResultado cancelar(CancelamentoRequest request) {
        if (request.chaveAcesso() == null || request.chaveAcesso().length() != 44) {
            return CancelamentoResultado.falha("Chave de acesso inválida para cancelamento");
        }
        if (request.justificativa() == null || request.justificativa().trim().length() < 15) {
            return CancelamentoResultado.falha("Justificativa deve ter ao menos 15 caracteres");
        }
        return CancelamentoResultado.ok(gerarProtocolo());
    }

    private String gerarChaveAcesso(EmissaoFiscalRequest request) {
        String uf = codigoUf(request.uf());
        String aamm = LocalDate.now().format(AAMM);
        String cnpj = soDigitos(request.emitenteCnpj(), 14);
        String modelo = request.modelo() == ModeloDocumentoFiscal.NFE ? "55" : "65";
        String serie = pad(request.serie(), 3);
        String numero = pad(request.numero(), 9);
        String tipoEmissao = "1";
        String codigoNumerico = pad(ThreadLocalRandom.current().nextInt(0, 99_999_999), 8);

        String base = uf + aamm + cnpj + modelo + serie + numero + tipoEmissao + codigoNumerico;
        return base + calcularDvMod11(base);
    }

    private static String gerarProtocolo() {
        long agora = System.currentTimeMillis();
        int aleatorio = ThreadLocalRandom.current().nextInt(100, 999);
        return "1" + agora + aleatorio;
    }

    private static String codigoUf(String uf) {
        if (uf == null) {
            return "35";
        }
        return switch (uf.toUpperCase()) {
            case "SP" -> "35";
            case "RJ" -> "33";
            case "MG" -> "31";
            case "RS" -> "43";
            case "PR" -> "41";
            case "SC" -> "42";
            case "BA" -> "29";
            case "PE" -> "26";
            case "CE" -> "23";
            case "DF" -> "53";
            case "GO" -> "52";
            case "ES" -> "32";
            default -> "35";
        };
    }

    private static String soDigitos(String valor, int tamanho) {
        String limpo = valor == null ? "" : valor.replaceAll("\\D", "");
        if (limpo.length() >= tamanho) {
            return limpo.substring(0, tamanho);
        }
        return "0".repeat(tamanho - limpo.length()) + limpo;
    }

    private static String pad(long valor, int tamanho) {
        String s = Long.toString(Math.abs(valor));
        if (s.length() >= tamanho) {
            return s.substring(s.length() - tamanho);
        }
        return "0".repeat(tamanho - s.length()) + s;
    }

    private static String calcularDvMod11(String base) {
        int peso = 2;
        int soma = 0;
        for (int i = base.length() - 1; i >= 0; i--) {
            soma += (base.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        int dv = resto == 0 || resto == 1 ? 0 : 11 - resto;
        return Integer.toString(dv);
    }
}
