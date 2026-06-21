package com.comercialcloud.application.produto;

import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.domain.model.Produto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ProdutoImportService {

    @Inject ProdutoService produtoService;

    public Map<String, Object> importarCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new BusinessException("CSV_OBRIGATORIO", "Conteúdo CSV é obrigatório");
        }

        String[] lines = csv.replace("\r", "").split("\n");
        if (lines.length < 2) {
            throw new BusinessException("CSV_INVALIDO", "CSV deve conter cabeçalho e ao menos uma linha");
        }

        String[] headers = splitLine(lines[0]);
        Map<String, Integer> index = indexHeaders(headers);
        int criados = 0;
        int ignorados = 0;
        List<String> erros = new ArrayList<>();

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] cols = splitLine(line);
            try {
                String sku = value(cols, index, "sku");
                String nome = value(cols, index, "nome");
                BigDecimal preco = parseDecimal(value(cols, index, "preco", "preco_venda", "precovenda"));
                if (sku == null || nome == null || preco == null) {
                    throw new IllegalArgumentException("sku, nome e preco são obrigatórios");
                }
                ProdutoService.DadosProduto dados =
                        new ProdutoService.DadosProduto(
                                sku,
                                value(cols, index, "codigo_barras", "codigobarras", "codigo"),
                                nome,
                                value(cols, index, "descricao"),
                                value(cols, index, "imagem_url", "imagemurl"),
                                value(cols, index, "categoria"),
                                preco,
                                parseDecimalOrZero(value(cols, index, "preco_custo", "precocusto")),
                                parseBoolean(value(cols, index, "controla_estoque", "controlaestoque"), true));
                produtoService.criar(dados);
                criados++;
            } catch (Exception e) {
                ignorados++;
                erros.add("Linha " + (i + 1) + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("criados", criados);
        result.put("ignorados", ignorados);
        result.put("erros", erros);
        return result;
    }

    private static Map<String, Integer> indexHeaders(String[] headers) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            map.put(normalize(headers[i]), i);
        }
        return map;
    }

    private static String normalize(String header) {
        return header == null
                ? ""
                : header.trim().toLowerCase().replace(" ", "_").replace("-", "_");
    }

    private static String[] splitLine(String line) {
        if (line.contains(";")) {
            return line.split(";", -1);
        }
        return line.split(",", -1);
    }

    private static String value(String[] cols, Map<String, Integer> index, String... keys) {
        for (String key : keys) {
            Integer pos = index.get(normalize(key));
            if (pos != null && pos < cols.length) {
                String val = cols[pos].trim();
                if (!val.isEmpty()) {
                    return val;
                }
            }
        }
        return null;
    }

    private static BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return new BigDecimal(raw.replace(",", "."));
    }

    private static BigDecimal parseDecimalOrZero(String raw) {
        BigDecimal value = parseDecimal(raw);
        return value != null ? value : BigDecimal.ZERO;
    }

    private static boolean parseBoolean(String raw, boolean defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return "1".equals(raw) || "true".equalsIgnoreCase(raw) || "sim".equalsIgnoreCase(raw);
    }
}
