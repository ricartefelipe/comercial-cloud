package com.comercialcloud.application.venda;

import com.comercialcloud.application.caixa.CaixaService;
import com.comercialcloud.application.estoque.EstoqueService;
import com.comercialcloud.application.financeiro.FinanceiroService;
import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.domain.model.Caixa;
import com.comercialcloud.domain.model.FormaPagamento;
import com.comercialcloud.domain.model.ItemVenda;
import com.comercialcloud.domain.model.PagamentoVenda;
import com.comercialcloud.domain.model.Produto;
import com.comercialcloud.domain.model.StatusVenda;
import com.comercialcloud.domain.model.Venda;
import com.comercialcloud.domain.repository.ClienteRepository;
import com.comercialcloud.domain.repository.LojaRepository;
import com.comercialcloud.domain.repository.ProdutoRepository;
import com.comercialcloud.domain.repository.VendaRepository;
import com.comercialcloud.infrastructure.audit.AuditService;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class VendaService {

    @Inject CaixaService caixaService;
    @Inject EstoqueService estoqueService;
    @Inject FinanceiroService financeiroService;
    @Inject TenantContext tenantContext;
    @Inject AuditService auditService;
    @Inject VendaRepository vendaRepository;
    @Inject ProdutoRepository produtoRepository;
    @Inject ClienteRepository clienteRepository;
    @Inject LojaRepository lojaRepository;

    @Transactional
    public Venda criar(UUID lojaId, UUID clienteId) {
        UUID tenantId = tenantContext.getTenantId();
        UUID usuarioId =
                tenantContext.getUserId()
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "USUARIO_OBRIGATORIO",
                                                "Header X-User-Id é obrigatório para criar vendas"));

        validarLoja(tenantId, lojaId);
        if (clienteId != null) {
            clienteRepository.find(tenantId, clienteId).orElseThrow(
                    () -> new NotFoundException("CLIENTE_NOT_FOUND", "Cliente não encontrado"));
        }

        Caixa caixa = caixaService.exigirCaixaAberto(lojaId, usuarioId);

        Instant agora = Instant.now();
        Venda nova =
                new Venda(
                        UUID.randomUUID(),
                        tenantId,
                        lojaId,
                        clienteId,
                        usuarioId,
                        caixa.id(),
                        StatusVenda.ABERTA,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        agora,
                        null,
                        null,
                        List.of(),
                        List.of());

        Venda salva = vendaRepository.save(nova);
        auditService.registrar("Venda", salva.id(), "CRIAR", null);
        return salva;
    }

    @Transactional
    public Venda adicionarItem(UUID vendaId, UUID produtoId, BigDecimal quantidade, BigDecimal descontoItem) {
        UUID tenantId = tenantContext.getTenantId();
        Venda venda = assertVendaAberta(tenantId, vendaId);

        Produto produto =
                produtoRepository.find(tenantId, produtoId).orElseThrow(
                        () -> new NotFoundException("PRODUTO_NOT_FOUND", "Produto não encontrado"));

        if (!produto.ativo()) {
            throw new BusinessException("PRODUTO_INATIVO", "Produto inativo não pode ser vendido");
        }

        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("QUANTIDADE_INVALIDA", "Quantidade deve ser maior que zero");
        }

        BigDecimal desconto = descontoItem != null ? descontoItem : BigDecimal.ZERO;
        if (desconto.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("DESCONTO_INVALIDO", "Desconto do item não pode ser negativo");
        }

        BigDecimal precoUnitario = produto.preco();
        BigDecimal bruto = precoUnitario.multiply(quantidade).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = bruto.subtract(desconto).setScale(2, RoundingMode.HALF_UP);
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    "DESCONTO_INVALIDO", "Desconto do item não pode exceder o subtotal");
        }

        List<ItemVenda> itens = new ArrayList<>(venda.itens());
        ItemVenda item =
                new ItemVenda(UUID.randomUUID(), produtoId, quantidade, precoUnitario, subtotal);
        itens.add(item);

        Venda comItem = venda.withItemsAndPayments(itens, venda.pagamentos());
        Venda recalculada = recalcularTotais(comItem);
        auditService.registrar("Venda", vendaId, "ADICIONAR_ITEM", "Produto: " + produtoId);
        return vendaRepository.save(recalculada);
    }

    @Transactional
    public Venda removerItem(UUID vendaId, UUID itemId) {
        UUID tenantId = tenantContext.getTenantId();
        Venda venda = assertVendaAberta(tenantId, vendaId);

        List<ItemVenda> itens = new ArrayList<>(venda.itens());
        boolean removido = itens.removeIf(i -> itemId.equals(i.id()));
        if (!removido) {
            throw new NotFoundException("ITEM_NOT_FOUND", "Item não encontrado na venda");
        }

        Venda semItem = recalcularTotais(venda.withItemsAndPayments(itens, venda.pagamentos()));
        auditService.registrar("Venda", vendaId, "REMOVER_ITEM", "Item: " + itemId);
        return vendaRepository.save(semItem);
    }

    @Transactional
    public Venda adicionarPagamento(UUID vendaId, String formaPagamentoStr, BigDecimal valor) {
        UUID tenantId = tenantContext.getTenantId();
        Venda venda = assertVendaAberta(tenantId, vendaId);

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("PAGAMENTO_INVALIDO", "Valor do pagamento deve ser maior que zero");
        }

        FormaPagamento forma = validarFormaPagamentoEnum(formaPagamentoStr);

        List<PagamentoVenda> pagamentos = new ArrayList<>(venda.pagamentos());
        pagamentos.add(new PagamentoVenda(UUID.randomUUID(), forma, valor));

        Venda comPagamentos = recalcularTotais(venda.withItemsAndPayments(venda.itens(), pagamentos));
        auditService.registrar("Venda", vendaId, "ADICIONAR_PAGAMENTO", forma.name() + ": " + valor);
        return vendaRepository.save(comPagamentos);
    }

    @Transactional
    public Venda finalizar(UUID vendaId) {
        UUID tenantId = tenantContext.getTenantId();
        Venda venda = assertVendaAberta(tenantId, vendaId);

        if (venda.itens().isEmpty()) {
            throw new BusinessException("VENDA_SEM_ITENS", "Venda deve ter ao menos 1 item para finalizar");
        }

        BigDecimal totalPagamentos =
                venda.pagamentos().stream().map(PagamentoVenda::valor).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPagamentos.compareTo(venda.total()) != 0) {
            throw new BusinessException(
                    "PAGAMENTO_INCOMPLETO", "Soma dos pagamentos deve ser igual ao total da venda");
        }

        for (ItemVenda item : venda.itens()) {
            estoqueService.baixarEstoque(venda.lojaId(), item.produtoId(), item.quantidade(), vendaId);
        }

        Instant agora = Instant.now();
        Venda finalizada =
                recalcularTotais(venda).withStatusEHorarios(StatusVenda.FINALIZADA, agora, venda.canceledAt());

        for (PagamentoVenda pagamento : finalizada.pagamentos()) {
            if (FormaPagamento.VALE == pagamento.formaPagamento()) {
                financeiroService.criarContaReceberVenda(
                        finalizada.id(), finalizada.clienteId(), pagamento.valor(), LocalDate.now().plusDays(30));
            }
        }

        Venda salva = vendaRepository.save(finalizada);
        auditService.registrar("Venda", vendaId, "FINALIZAR", "Total: " + salva.total());
        return salva;
    }

    @Transactional
    public Venda cancelar(UUID vendaId) {
        UUID tenantId = tenantContext.getTenantId();
        Venda venda = obterOuFalhar(tenantId, vendaId);

        if (venda.status() == StatusVenda.CANCELADA) {
            throw new BusinessException("VENDA_JA_CANCELADA", "Venda já está cancelada");
        }

        if (venda.status() == StatusVenda.FINALIZADA) {
            for (ItemVenda item : venda.itens()) {
                estoqueService.estornarEstoque(venda.lojaId(), item.produtoId(), item.quantidade(), vendaId);
            }
            financeiroService.cancelarPorVenda(venda.id());
        }

        Instant agora = Instant.now();
        Venda cancelada =
                recalcularTotais(venda).withStatusEHorarios(StatusVenda.CANCELADA, venda.finalizedAt(), agora);

        Venda salva = vendaRepository.save(cancelada);
        auditService.registrar("Venda", vendaId, "CANCELAR", null);
        return salva;
    }

    public Venda buscarPorId(UUID id) {
        return obterOuFalhar(tenantContext.getTenantId(), id);
    }

    public List<Venda> listar(UUID lojaId, Instant dataInicio, Instant dataFim) {
        return vendaRepository.listar(tenantContext.getTenantId(), lojaId, dataInicio, dataFim);
    }

    private Venda obterOuFalhar(UUID tenantId, UUID vendaId) {
        return vendaRepository
                .findComItensPagamentos(tenantId, vendaId)
                .orElseThrow(() -> new NotFoundException("VENDA_NOT_FOUND", "Venda não encontrada"));
    }

    private Venda assertVendaAberta(UUID tenantId, UUID vendaId) {
        Venda venda = obterOuFalhar(tenantId, vendaId);
        if (venda.status() != StatusVenda.ABERTA) {
            throw new BusinessException("VENDA_NAO_EDITAVEL", "Apenas vendas abertas podem ser alteradas");
        }
        return venda;
    }

    private Venda recalcularTotais(Venda venda) {
        BigDecimal subtotal =
                venda.itens().stream().map(ItemVenda::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal desconto = venda.desconto() != null ? venda.desconto() : BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(desconto).setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("TOTAL_NEGATIVO", "Desconto não pode tornar o total negativo");
        }

        return venda.withValoresMercadorias(subtotal, desconto, total);
    }

    private void validarLoja(UUID tenantId, UUID lojaId) {
        lojaRepository.find(tenantId, lojaId).orElseThrow(
                () -> new NotFoundException("LOJA_NOT_FOUND", "Loja não encontrada"));
    }

    private static FormaPagamento validarFormaPagamentoEnum(String formaPagamento) {
        try {
            return FormaPagamento.valueOf(formaPagamento);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(
                    "PAGAMENTO_INVALIDO", "Forma de pagamento inválida: " + formaPagamento);
        }
    }
}
