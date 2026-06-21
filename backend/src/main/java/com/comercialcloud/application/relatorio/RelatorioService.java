package com.comercialcloud.application.relatorio;

import com.comercialcloud.domain.model.Estoque;
import com.comercialcloud.domain.model.Produto;
import com.comercialcloud.domain.model.StatusVenda;
import com.comercialcloud.domain.model.Venda;
import com.comercialcloud.domain.repository.EstoqueRepository;
import com.comercialcloud.domain.repository.ProdutoRepository;
import com.comercialcloud.domain.repository.VendaRepository;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class RelatorioService {

    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));

    @Inject TenantContext tenantContext;
    @Inject VendaRepository vendaRepository;
    @Inject EstoqueRepository estoqueRepository;
    @Inject ProdutoRepository produtoRepository;

    public byte[] vendasCsv(UUID lojaId, Instant inicio, Instant fim) {
        UUID tenantId = tenantContext.getTenantId();
        List<Venda> vendas = vendaRepository.listFinalizadasNoPeriodo(tenantId, lojaId, inicio, fim);
        StringBuilder sb = new StringBuilder();
        sb.append("id;data_finalizacao;status;subtotal;desconto;total;itens\n");
        for (Venda v : vendas) {
            sb.append(csv(v.id()))
                    .append(';')
                    .append(csv(formatInstant(v.finalizedAt())))
                    .append(';')
                    .append(csv(v.status() != null ? v.status().name() : ""))
                    .append(';')
                    .append(csv(v.subtotal()))
                    .append(';')
                    .append(csv(v.desconto()))
                    .append(';')
                    .append(csv(v.total()))
                    .append(';')
                    .append(v.itens() != null ? v.itens().size() : 0)
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public List<Map<String, Object>> vendasJson(UUID lojaId, Instant inicio, Instant fim) {
        UUID tenantId = tenantContext.getTenantId();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Venda v : vendaRepository.listFinalizadasNoPeriodo(tenantId, lojaId, inicio, fim)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", v.id());
            row.put("finalizedAt", v.finalizedAt());
            row.put("status", v.status() != null ? v.status().name() : null);
            row.put("subtotal", v.subtotal());
            row.put("desconto", v.desconto());
            row.put("total", v.total());
            row.put("itens", v.itens() != null ? v.itens().size() : 0);
            rows.add(row);
        }
        return rows;
    }

    public byte[] estoqueCsv(UUID lojaId) {
        UUID tenantId = tenantContext.getTenantId();
        List<Estoque> itens = estoqueRepository.listPorLoja(tenantId, lojaId);
        StringBuilder sb = new StringBuilder();
        sb.append("produto_id;sku;nome;quantidade;atualizado_em\n");
        for (Estoque e : itens) {
            Produto produto =
                    produtoRepository
                            .find(tenantId, e.produtoId())
                            .orElse(null);
            sb.append(csv(e.produtoId()))
                    .append(';')
                    .append(csv(produto != null ? produto.sku() : ""))
                    .append(';')
                    .append(csv(produto != null ? produto.nome() : ""))
                    .append(';')
                    .append(csv(e.quantidade()))
                    .append(';')
                    .append(csv(formatInstant(e.updatedAt())))
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public List<Map<String, Object>> estoqueJson(UUID lojaId) {
        UUID tenantId = tenantContext.getTenantId();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Estoque e : estoqueRepository.listPorLoja(tenantId, lojaId)) {
            Produto produto =
                    produtoRepository
                            .find(tenantId, e.produtoId())
                            .orElse(null);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("produtoId", e.produtoId());
            row.put("sku", produto != null ? produto.sku() : null);
            row.put("nome", produto != null ? produto.nome() : null);
            row.put("quantidade", e.quantidade());
            row.put("updatedAt", e.updatedAt());
            rows.add(row);
        }
        return rows;
    }

    public Map<String, Object> resumo(UUID lojaId, Instant inicio, Instant fim) {
        UUID tenantId = tenantContext.getTenantId();
        List<Venda> vendas = vendaRepository.listFinalizadasNoPeriodo(tenantId, lojaId, inicio, fim);
        BigDecimal faturamento = BigDecimal.ZERO;
        for (Venda v : vendas) {
            if (v.status() == StatusVenda.FINALIZADA && v.total() != null) {
                faturamento = faturamento.add(v.total());
            }
        }
        long estoqueItens = estoqueRepository.listPorLoja(tenantId, lojaId).size();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("vendasFinalizadas", vendas.size());
        map.put("faturamento", faturamento);
        map.put("skusComEstoque", estoqueItens);
        return map;
    }

    private static String formatInstant(Instant instant) {
        return instant != null ? DATA_HORA.format(instant) : "";
    }

    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
