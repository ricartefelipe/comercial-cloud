package com.comercialcloud.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Venda(
        UUID id,
        UUID tenantId,
        UUID lojaId,
        UUID clienteId,
        UUID usuarioId,
        UUID caixaId,
        StatusVenda status,
        BigDecimal subtotal,
        BigDecimal desconto,
        BigDecimal total,
        Instant createdAt,
        Instant finalizedAt,
        Instant canceledAt,
        List<ItemVenda> itens,
        List<PagamentoVenda> pagamentos) {

    public Venda withoutItemsAndPayments() {
        return new Venda(
                id,
                tenantId,
                lojaId,
                clienteId,
                usuarioId,
                caixaId,
                status,
                subtotal,
                desconto,
                total,
                createdAt,
                finalizedAt,
                canceledAt,
                List.of(),
                List.of());
    }

    public Venda withItemsAndPayments(List<ItemVenda> novosItens, List<PagamentoVenda> novosPagamentos) {
        return new Venda(
                id,
                tenantId,
                lojaId,
                clienteId,
                usuarioId,
                caixaId,
                status,
                subtotal,
                desconto,
                total,
                createdAt,
                finalizedAt,
                canceledAt,
                List.copyOf(novosItens),
                List.copyOf(novosPagamentos));
    }

    public Venda withValoresMercadorias(BigDecimal novoSubtotal, BigDecimal novoDesconto, BigDecimal novoTotal) {
        return new Venda(
                id,
                tenantId,
                lojaId,
                clienteId,
                usuarioId,
                caixaId,
                status,
                novoSubtotal,
                novoDesconto,
                novoTotal,
                createdAt,
                finalizedAt,
                canceledAt,
                itens,
                pagamentos);
    }

    public Venda withStatusEHorarios(
            StatusVenda novoStatus, Instant novoFinalizedAt, Instant novoCanceledAt) {
        return new Venda(
                id,
                tenantId,
                lojaId,
                clienteId,
                usuarioId,
                caixaId,
                novoStatus,
                subtotal,
                desconto,
                total,
                createdAt,
                novoFinalizedAt,
                novoCanceledAt,
                itens,
                pagamentos);
    }
}
