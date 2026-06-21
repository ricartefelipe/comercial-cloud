package com.comercialcloud.application.loja;

import com.comercialcloud.application.billing.BillingService;
import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.domain.model.Estoque;
import com.comercialcloud.domain.model.Loja;
import com.comercialcloud.domain.model.Produto;
import com.comercialcloud.domain.repository.EstoqueRepository;
import com.comercialcloud.domain.repository.LojaRepository;
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
public class LojaService {

    @Inject TenantContext tenantContext;
    @Inject LojaRepository lojaRepository;
    @Inject BillingService billingService;
    @Inject AuditService auditService;
    @Inject EstoqueRepository estoqueRepository;
    @Inject ProdutoRepository produtoRepository;

    public List<Loja> listar() {
        return lojaRepository.listByTenant(tenantContext.getTenantId());
    }

    public LojaResumo resumo() {
        UUID tenantId = tenantContext.getTenantId();
        int limite = billingService.limiteLojas(tenantId);
        long ativas = lojaRepository.countAtivas(tenantId);
        return new LojaResumo(limite, ativas, ativas < limite);
    }

    @Transactional
    public Loja criar(String nome) {
        UUID tenantId = tenantContext.getTenantId();
        validarLimite(tenantId);
        Instant now = Instant.now();
        Loja loja = new Loja(UUID.randomUUID(), tenantId, nome.trim(), true, now);
        Loja salva = lojaRepository.save(loja);
        seedEstoqueInicial(tenantId, salva.id());
        auditService.registrar("Loja", salva.id(), "CRIAR", salva.nome());
        return salva;
    }

    @Transactional
    public Loja atualizar(UUID id, String nome, Boolean ativa) {
        UUID tenantId = tenantContext.getTenantId();
        Loja existente =
                lojaRepository
                        .find(tenantId, id)
                        .orElseThrow(() -> new NotFoundException("LOJA_NOT_FOUND", "Loja não encontrada"));
        boolean novaAtiva = ativa != null ? ativa : existente.ativa();
        if (!existente.ativa() && novaAtiva) {
            validarLimite(tenantId);
        }
        if (existente.ativa() && !novaAtiva && lojaRepository.countAtivas(tenantId) <= 1) {
            throw new BusinessException("LOJA_UNICA", "O tenant precisa manter ao menos uma loja ativa");
        }
        Loja atualizada =
                new Loja(
                        existente.id(),
                        existente.tenantId(),
                        nome != null && !nome.isBlank() ? nome.trim() : existente.nome(),
                        novaAtiva,
                        existente.createdAt());
        Loja salva = lojaRepository.save(atualizada);
        auditService.registrar("Loja", salva.id(), "ATUALIZAR", salva.nome());
        return salva;
    }

    private void validarLimite(UUID tenantId) {
        int limite = billingService.limiteLojas(tenantId);
        long ativas = lojaRepository.countAtivas(tenantId);
        if (ativas >= limite) {
            throw new BusinessException(
                    "LIMITE_LOJAS",
                    "Limite de lojas do plano atingido (" + limite + "). Faça upgrade para Premium.");
        }
    }

    private void seedEstoqueInicial(UUID tenantId, UUID novaLojaId) {
        Instant now = Instant.now();
        var outras =
                lojaRepository.listByTenant(tenantId).stream()
                        .filter(l -> l.ativa() && !l.id().equals(novaLojaId))
                        .toList();
        if (!outras.isEmpty()) {
            UUID lojaOrigem = outras.getFirst().id();
            for (Estoque item : estoqueRepository.listPorLoja(tenantId, lojaOrigem)) {
                if (estoqueRepository.findPorLojaProduto(tenantId, novaLojaId, item.produtoId()).isEmpty()) {
                    estoqueRepository.save(
                            new Estoque(
                                    UUID.randomUUID(),
                                    tenantId,
                                    novaLojaId,
                                    item.produtoId(),
                                    BigDecimal.ZERO,
                                    now));
                }
            }
            return;
        }
        int page = 0;
        while (true) {
            List<Produto> batch = produtoRepository.page(tenantId, page, 200);
            if (batch.isEmpty()) {
                break;
            }
            for (Produto produto : batch) {
                if (estoqueRepository.findPorLojaProduto(tenantId, novaLojaId, produto.id()).isEmpty()) {
                    estoqueRepository.save(
                            new Estoque(
                                    UUID.randomUUID(),
                                    tenantId,
                                    novaLojaId,
                                    produto.id(),
                                    BigDecimal.ZERO,
                                    now));
                }
            }
            page++;
        }
    }

    public record LojaResumo(int limite, long ativas, boolean podeCriar) {}
}
