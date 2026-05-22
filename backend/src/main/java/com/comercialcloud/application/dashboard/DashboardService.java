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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class DashboardService {

    private static final BigDecimal ESTOQUE_BAIXO_LIMITE = new BigDecimal("10");

    @Inject TenantContext tenantContext;

    @Inject VendaRepository vendaRepository;

    @Inject EstoqueRepository estoqueRepository;

    @Inject ProdutoRepository produtoRepository;

    public record ResumoDashboard(
            long vendasDoDia,
            BigDecimal faturamentoDoDia,
            BigDecimal ticketMedio,
            List<Estoque> produtosEstoqueBaixo,
            Map<UUID, Produto> produtosPorId,
            Map<String, BigDecimal> vendasPorFormaPagamento,
            List<Venda> ultimasVendas
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

        List<Estoque> estoqueBaixo =
                estoqueRepository.listPorQuantidadeMax(tenantId, lojaId, ESTOQUE_BAIXO_LIMITE);

        Map<UUID, Produto> produtosPorId = new HashMap<>();
        for (Estoque estoque : estoqueBaixo) {
            produtoRepository
                    .find(tenantId, estoque.produtoId())
                    .ifPresent(produto -> produtosPorId.put(produto.id(), produto));
        }

        List<Venda> ultimas = vendaRepository.listUltimasCompleto(tenantId, lojaId, 10);

        return new ResumoDashboard(qtd, faturamento, ticket, estoqueBaixo, produtosPorId, porForma, ultimas);
    }
}
