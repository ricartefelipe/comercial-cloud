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

    public record DadosProduto(
            String sku,
            String codigoBarras,
            String nome,
            String descricao,
            String imagemUrl,
            String categoria,
            BigDecimal preco,
            BigDecimal precoCusto,
            boolean controlaEstoque) {

        DadosProduto withImagemUrl(String url) {
            return new DadosProduto(
                    sku, codigoBarras, nome, descricao, url, categoria, preco, precoCusto, controlaEstoque);
        }
    }

    @Transactional
    public Produto criar(DadosProduto dados) {
        UUID tenantId = tenantContext.getTenantId();
        validarPreco(dados.preco());
        validarSkuUnico(tenantId, dados.sku(), null);
        validarCodigoBarrasUnico(tenantId, dados.codigoBarras(), null);
        Instant now = Instant.now();
        Produto produto = novoProduto(UUID.randomUUID(), tenantId, dados, true, now, now);
        Produto salvo = produtoRepository.save(produto);
        auditService.registrar("Produto", salvo.id(), "CRIAR", "SKU: " + dados.sku());
        return salvo;
    }

    @Transactional
    public Produto atualizar(UUID id, DadosProduto dados) {
        Produto atual = obter(tenantContext.getTenantId(), id);
        validarPreco(dados.preco());
        validarSkuUnico(atual.tenantId(), dados.sku(), id);
        validarCodigoBarrasUnico(atual.tenantId(), dados.codigoBarras(), id);
        Produto atualizado =
                novoProduto(
                        atual.id(),
                        atual.tenantId(),
                        dados,
                        atual.ativo(),
                        atual.createdAt(),
                        Instant.now());
        Produto salvo = produtoRepository.save(atualizado);
        auditService.registrar("Produto", salvo.id(), "ATUALIZAR", "SKU: " + dados.sku());
        return salvo;
    }

    @Transactional
    public Produto atualizarImagemUrl(UUID id, String imagemUrl) {
        Produto atual = obter(tenantContext.getTenantId(), id);
        DadosProduto dados = dadosFrom(atual).withImagemUrl(normalizarImagemUrl(imagemUrl));
        return produtoRepository.save(
                novoProduto(
                        atual.id(),
                        atual.tenantId(),
                        dados,
                        atual.ativo(),
                        atual.createdAt(),
                        Instant.now()));
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

    public Produto buscarPorCodigoBarras(String codigoBarras) {
        return produtoRepository
                .findByCodigoBarras(tenantContext.getTenantId(), codigoBarras)
                .orElseThrow(
                        () -> new NotFoundException("PRODUTO_NOT_FOUND", "Produto não encontrado para o código de barras"));
    }

    public List<String> listarCategorias() {
        return produtoRepository.listCategorias(tenantContext.getTenantId());
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
        Produto atualizado =
                novoProduto(
                        produto.id(),
                        produto.tenantId(),
                        dadosFrom(produto),
                        ativo,
                        produto.createdAt(),
                        Instant.now());
        Produto salvo = produtoRepository.save(atualizado);
        auditService.registrar("Produto", salvo.id(), ativo ? "ATIVAR" : "INATIVAR", null);
        return salvo;
    }

    static DadosProduto dadosFrom(Produto produto) {
        return new DadosProduto(
                produto.sku(),
                produto.codigoBarras(),
                produto.nome(),
                produto.descricao(),
                produto.imagemUrl(),
                produto.categoria(),
                produto.preco(),
                produto.precoCusto(),
                produto.controlaEstoque());
    }

    private static Produto novoProduto(
            UUID id,
            UUID tenantId,
            DadosProduto dados,
            boolean ativo,
            Instant createdAt,
            Instant updatedAt) {
        return new Produto(
                id,
                tenantId,
                dados.sku(),
                dados.codigoBarras(),
                dados.nome(),
                dados.descricao(),
                normalizarImagemUrl(dados.imagemUrl()),
                dados.categoria(),
                dados.preco(),
                normalizarPrecoCusto(dados.precoCusto()),
                dados.controlaEstoque(),
                ativo,
                createdAt,
                updatedAt);
    }

    private Produto obter(UUID tenantId, UUID id) {
        return produtoRepository
                .find(tenantId, id)
                .orElseThrow(() -> new NotFoundException("PRODUTO_NOT_FOUND", "Produto não encontrado"));
    }

    private static String normalizarImagemUrl(String imagemUrl) {
        if (imagemUrl == null || imagemUrl.isBlank()) {
            return null;
        }
        return imagemUrl.trim();
    }

    private static BigDecimal normalizarPrecoCusto(BigDecimal precoCusto) {
        return precoCusto != null ? precoCusto : BigDecimal.ZERO;
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
