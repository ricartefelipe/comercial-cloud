package com.comercialcloud.application.produto;

import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.domain.exception.ConflictException;
import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.domain.model.Produto;
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
public class ProdutoService {

    @Inject TenantContext tenantContext;
    @Inject AuditService auditService;
    @Inject ProdutoRepository produtoRepository;

    public record PageResult<T>(List<T> content, long totalElements, int totalPages, int size, int number) {}

    @Transactional
    public Produto criar(String sku, String codigoBarras, String nome, String descricao, BigDecimal preco) {
        UUID tenantId = tenantContext.getTenantId();
        validarPreco(preco);
        validarSkuUnico(tenantId, sku, null);
        validarCodigoBarrasUnico(tenantId, codigoBarras, null);
        Instant now = Instant.now();
        Produto produto =
                new Produto(
                        UUID.randomUUID(),
                        tenantId,
                        sku,
                        codigoBarras,
                        nome,
                        descricao,
                        preco,
                        true,
                        now,
                        now);
        Produto salvo = produtoRepository.save(produto);
        auditService.registrar("Produto", salvo.id(), "CRIAR", "SKU: " + sku);
        return salvo;
    }

    @Transactional
    public Produto atualizar(
            UUID id, String sku, String codigoBarras, String nome, String descricao, BigDecimal preco) {
        Produto atual = obter(tenantContext.getTenantId(), id);
        validarPreco(preco);
        validarSkuUnico(atual.tenantId(), sku, id);
        validarCodigoBarrasUnico(atual.tenantId(), codigoBarras, id);
        Instant now = Instant.now();
        Produto atualizado =
                new Produto(
                        atual.id(),
                        atual.tenantId(),
                        sku,
                        codigoBarras,
                        nome,
                        descricao,
                        preco,
                        atual.ativo(),
                        atual.createdAt(),
                        now);
        Produto salvo = produtoRepository.save(atualizado);
        auditService.registrar("Produto", salvo.id(), "ATUALIZAR", "SKU: " + sku);
        return salvo;
    }

    public Produto buscarPorId(UUID id) {
        return obter(tenantContext.getTenantId(), id);
    }

    public PageResult<Produto> listar(int page, int size) {
        UUID tenantId = tenantContext.getTenantId();
        int safeSize = Math.max(size, 1);
        long total = produtoRepository.count(tenantId);
        List<Produto> content = produtoRepository.page(tenantId, page, safeSize);
        int totalPages = (int) Math.ceil((double) total / safeSize);
        return new PageResult<>(content, total, totalPages, safeSize, page);
    }

    public List<Produto> buscar(String termo) {
        return produtoRepository.buscarPorTermo(tenantContext.getTenantId(), termo);
    }

    @Transactional
    public Produto ativar(UUID id) {
        return alterarStatus(id, true);
    }

    @Transactional
    public Produto inativar(UUID id) {
        return alterarStatus(id, false);
    }

    private Produto alterarStatus(UUID id, boolean ativo) {
        Produto produto = obter(tenantContext.getTenantId(), id);
        Instant now = Instant.now();
        Produto atualizado =
                new Produto(
                        produto.id(),
                        produto.tenantId(),
                        produto.sku(),
                        produto.codigoBarras(),
                        produto.nome(),
                        produto.descricao(),
                        produto.preco(),
                        ativo,
                        produto.createdAt(),
                        now);
        Produto salvo = produtoRepository.save(atualizado);
        auditService.registrar("Produto", salvo.id(), ativo ? "ATIVAR" : "INATIVAR", null);
        return salvo;
    }

    private Produto obter(UUID tenantId, UUID id) {
        return produtoRepository
                .find(tenantId, id)
                .orElseThrow(() -> new NotFoundException("PRODUTO_NOT_FOUND", "Produto não encontrado"));
    }

    private void validarPreco(BigDecimal preco) {
        if (preco == null || preco.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("PRECO_INVALIDO", "Preço inválido");
        }
    }

    private void validarSkuUnico(UUID tenantId, String sku, UUID ignoreId) {
        if (produtoRepository.existsSkuExclusive(tenantId, sku, ignoreId)) {
            throw new ConflictException("SKU_DUPLICADO", "SKU já cadastrado");
        }
    }

    private void validarCodigoBarrasUnico(UUID tenantId, String codigoBarras, UUID ignoreId) {
        if (codigoBarras == null || codigoBarras.isBlank()) {
            return;
        }
        if (produtoRepository.existsCodigoBarrasExclusive(tenantId, codigoBarras, ignoreId)) {
            throw new ConflictException("CODIGO_BARRAS_DUPLICADO", "Código de barras duplicado");
        }
    }
}
