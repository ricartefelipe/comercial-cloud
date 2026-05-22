package com.comercialcloud.application.estoque;

import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.domain.model.Estoque;
import com.comercialcloud.domain.model.MovimentacaoEstoque;
import com.comercialcloud.domain.model.TipoMovimentacaoEstoque;
import com.comercialcloud.domain.repository.EstoqueRepository;
import com.comercialcloud.domain.repository.LojaRepository;
import com.comercialcloud.domain.repository.MovimentacaoEstoqueRepository;
import com.comercialcloud.domain.repository.ProdutoRepository;
import com.comercialcloud.infrastructure.audit.AuditService;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EstoqueService {

    @Inject TenantContext tenantContext;

    @Inject AuditService auditService;

    @Inject LojaRepository lojaRepository;

    @Inject ProdutoRepository produtoRepository;

    @Inject EstoqueRepository estoqueRepository;

    @Inject MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    public List<Estoque> listarPorLoja(UUID lojaId) {
        UUID tenantId = tenantContext.getTenantId();
        validarLoja(tenantId, lojaId);
        return estoqueRepository.listPorLoja(tenantId, lojaId);
    }

    public List<MovimentacaoEstoque> listarMovimentacoes(UUID lojaId, UUID produtoId) {
        UUID tenantId = tenantContext.getTenantId();
        validarLoja(tenantId, lojaId);
        return movimentacaoEstoqueRepository.listar(tenantId, lojaId, produtoId);
    }

    @Transactional
    public void ajustar(UUID lojaId, UUID produtoId, BigDecimal novaQuantidade, String motivo) {
        UUID tenantId = tenantContext.getTenantId();
        validarLoja(tenantId, lojaId);
        validarProduto(tenantId, produtoId);

        if (novaQuantidade == null || novaQuantidade.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("ESTOQUE_NEGATIVO", "Estoque não pode ser negativo");
        }

        Estoque estoque = obterOuCriar(tenantId, lojaId, produtoId);
        BigDecimal saldoAnterior = estoque.quantidade();
        BigDecimal diferenca = novaQuantidade.subtract(saldoAnterior);

        Estoque atualizado =
                new Estoque(
                        estoque.id(),
                        estoque.tenantId(),
                        estoque.lojaId(),
                        estoque.produtoId(),
                        novaQuantidade,
                        Instant.now());
        estoqueRepository.save(atualizado);

        registrarMovimentacao(
                tenantId,
                lojaId,
                produtoId,
                TipoMovimentacaoEstoque.AJUSTE,
                diferenca.abs(),
                saldoAnterior,
                novaQuantidade,
                motivo,
                null);
        auditService.registrar("Estoque", estoque.id(), "AJUSTE", motivo);
    }

    @Transactional
    public void baixarEstoque(UUID lojaId, UUID produtoId, BigDecimal quantidade, UUID referenciaId) {
        UUID tenantId = tenantContext.getTenantId();
        validarLoja(tenantId, lojaId);
        validarProduto(tenantId, produtoId);

        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("QUANTIDADE_INVALIDA", "Quantidade da baixa deve ser maior que zero");
        }

        Estoque estoque = obterOuCriar(tenantId, lojaId, produtoId);
        BigDecimal saldoAnterior = estoque.quantidade();
        BigDecimal saldoPosterior = saldoAnterior.subtract(quantidade);

        if (saldoPosterior.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("ESTOQUE_INSUFICIENTE", "Estoque insuficiente para a operação");
        }

        Estoque atualizado =
                new Estoque(
                        estoque.id(),
                        estoque.tenantId(),
                        estoque.lojaId(),
                        estoque.produtoId(),
                        saldoPosterior,
                        Instant.now());
        estoqueRepository.save(atualizado);

        registrarMovimentacao(
                tenantId,
                lojaId,
                produtoId,
                TipoMovimentacaoEstoque.VENDA,
                quantidade,
                saldoAnterior,
                saldoPosterior,
                "Baixa por venda",
                referenciaId);
    }

    @Transactional
    public void estornarEstoque(UUID lojaId, UUID produtoId, BigDecimal quantidade, UUID referenciaId) {
        UUID tenantId = tenantContext.getTenantId();
        validarLoja(tenantId, lojaId);
        validarProduto(tenantId, produtoId);

        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("QUANTIDADE_INVALIDA", "Quantidade do estorno deve ser maior que zero");
        }

        Estoque estoque = obterOuCriar(tenantId, lojaId, produtoId);
        BigDecimal saldoAnterior = estoque.quantidade();
        BigDecimal saldoPosterior = saldoAnterior.add(quantidade);

        Estoque atualizado =
                new Estoque(
                        estoque.id(),
                        estoque.tenantId(),
                        estoque.lojaId(),
                        estoque.produtoId(),
                        saldoPosterior,
                        Instant.now());
        estoqueRepository.save(atualizado);

        registrarMovimentacao(
                tenantId,
                lojaId,
                produtoId,
                TipoMovimentacaoEstoque.CANCELAMENTO_VENDA,
                quantidade,
                saldoAnterior,
                saldoPosterior,
                "Estorno por cancelamento",
                referenciaId);
    }

    private Estoque obterOuCriar(UUID tenantId, UUID lojaId, UUID produtoId) {
        return estoqueRepository.findPorLojaProduto(tenantId, lojaId, produtoId).orElseGet(
                () ->
                        estoqueRepository.save(new Estoque(
                                UUID.randomUUID(),
                                tenantId,
                                lojaId,
                                produtoId,
                                BigDecimal.ZERO,
                                Instant.now())));
    }

    private void registrarMovimentacao(
            UUID tenantId,
            UUID lojaId,
            UUID produtoId,
            TipoMovimentacaoEstoque tipo,
            BigDecimal quantidade,
            BigDecimal saldoAnterior,
            BigDecimal saldoPosterior,
            String motivo,
            UUID referenciaId) {
        MovimentacaoEstoque mov =
                new MovimentacaoEstoque(
                        UUID.randomUUID(),
                        tenantId,
                        lojaId,
                        produtoId,
                        tipo,
                        quantidade,
                        saldoAnterior,
                        saldoPosterior,
                        motivo,
                        referenciaId,
                        Instant.now());
        movimentacaoEstoqueRepository.save(mov);
    }

    private void validarLoja(UUID tenantId, UUID lojaId) {
        lojaRepository.find(tenantId, lojaId)
                .orElseThrow(() -> new NotFoundException("LOJA_NOT_FOUND", "Loja não encontrada"));
    }

    private void validarProduto(UUID tenantId, UUID produtoId) {
        produtoRepository.find(tenantId, produtoId)
                .orElseThrow(() -> new NotFoundException("PRODUTO_NOT_FOUND", "Produto não encontrado"));
    }
}
