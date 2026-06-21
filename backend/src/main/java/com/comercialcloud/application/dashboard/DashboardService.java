package com.comercialcloud.application.dashboard;

import com.comercialcloud.domain.model.Estoque;
import com.comercialcloud.domain.model.Produto;
import com.comercialcloud.domain.model.Venda;
import com.comercialcloud.domain.repository.EstoqueRepository;
import com.comercialcloud.domain.repository.ProdutoRepository;
import com.comercialcloud.domain.repository.VendaRepository;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class DashboardService {

    @Inject TenantContext tenantContext;

    @Inject VendaRepository vendaRepository;

    @Inject EstoqueRepository estoqueRepository;

    @Inject ProdutoRepository produtoRepository;

    @Inject EntityManager entityManager;

    public record TopProduto(
            UUID produtoId,
            String produtoNome,
            String produtoSku,
            BigDecimal quantidadeVendida,
            BigDecimal faturamento) {}

    public record ResumoDashboard(
            long vendasDoDia,
            BigDecimal faturamentoDoDia,
            BigDecimal ticketMedio,
            List<Estoque> produtosEstoqueBaixo,
            Map<UUID, Produto> produtosPorId,
            Map<String, BigDecimal> vendasPorFormaPagamento,
            List<Venda> ultimasVendas,
            List<TopProduto> topProdutos
    ) {}

    public ResumoDashboard resumo(UUID lojaId) {
        UUID tenantId = tenantContext.getTenantId();
        Instant inicio = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant fim = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Venda> vendasDia =
                vendaRepository.listFinalizadasNoPeriodo(tenantId, lojaId, inicio, fim);

        BigDecimal faturamento =
                vendasDia.stream().map(Venda::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        long qtd = vendasDia.size();
        BigDecimal ticket =
                qtd > 0 ? faturamento.divide(BigDecimal.valueOf(qtd), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        Map<String, BigDecimal> porForma = new HashMap<>();
        for (Venda venda : vendasDia) {
            for (var pagamento : venda.pagamentos()) {
                porForma.merge(pagamento.formaPagamento().name(), pagamento.valor(), BigDecimal::add);
            }
        }

        List<Estoque> estoqueBaixo = estoqueRepository.listEstoqueBaixo(tenantId, lojaId);

        Map<UUID, Produto> produtosPorId = new HashMap<>();
        for (Estoque estoque : estoqueBaixo) {
            produtoRepository
                    .find(tenantId, estoque.produtoId())
                    .ifPresent(produto -> produtosPorId.put(produto.id(), produto));
        }

        List<Venda> ultimas = vendaRepository.listUltimasCompleto(tenantId, lojaId, 10);

        List<TopProduto> topProdutos = listarTopProdutos(tenantId, lojaId, 30, 5);

        return new ResumoDashboard(qtd, faturamento, ticket, estoqueBaixo, produtosPorId, porForma, ultimas, topProdutos);
    }

    @SuppressWarnings("unchecked")
    private List<TopProduto> listarTopProdutos(UUID tenantId, UUID lojaId, int dias, int limite) {
        Instant inicio = LocalDate.now().minusDays(dias).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Object[]> rows =
                entityManager
                        .createQuery(
                                """
                                SELECT i.produtoId, SUM(i.quantidade), SUM(i.subtotal)
                                FROM ItemVendaEntity i
                                JOIN i.venda v
                                WHERE v.tenantId = :tenantId
                                  AND v.lojaId = :lojaId
                                  AND v.status = 'FINALIZADA'
                                  AND v.finalizedAt >= :inicio
                                GROUP BY i.produtoId
                                ORDER BY SUM(i.quantidade) DESC
                                """,
                                Object[].class)
                        .setParameter("tenantId", tenantId)
                        .setParameter("lojaId", lojaId)
                        .setParameter("inicio", inicio)
                        .setMaxResults(limite)
                        .getResultList();

        List<TopProduto> result = new ArrayList<>();
        for (Object[] row : rows) {
            UUID produtoId = (UUID) row[0];
            BigDecimal qtd = (BigDecimal) row[1];
            BigDecimal fat = (BigDecimal) row[2];
            String nome = produtoId.toString();
            String sku = "";
            var produtoOpt = produtoRepository.find(tenantId, produtoId);
            if (produtoOpt.isPresent()) {
                nome = produtoOpt.get().nome();
                sku = produtoOpt.get().sku();
            }
            result.add(new TopProduto(produtoId, nome, sku, qtd, fat));
        }
        return result;
    }
}
