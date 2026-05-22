package com.comercialcloud.application.financeiro;

import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.domain.model.StatusContaReceber;
import com.comercialcloud.infrastructure.audit.AuditService;
import com.comercialcloud.infrastructure.persistence.entity.ContaReceberEntity;
import com.comercialcloud.infrastructure.persistence.entity.VendaEntity;
import com.comercialcloud.infrastructure.security.TenantContext;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FinanceiroService {

    @Inject
    TenantContext tenantContext;

    @Inject
    AuditService auditService;

    public record ContaReceberView(ContaReceberEntity conta, UUID lojaId, String descricao) {}

    public List<ContaReceberView> listarContasReceber(UUID lojaId) {
        UUID tenantId = tenantContext.getTenantId();
        List<ContaReceberEntity> contas =
                ContaReceberEntity.find("tenantId = ?1", Sort.by("vencimento").descending(), tenantId).list();
        return contas.stream()
                .map(conta ->
                        new ContaReceberView(
                                conta,
                                resolverLojaId(tenantId, conta),
                                "Conta a receber - Venda " + conta.vendaId))
                .filter(view -> lojaId == null || lojaId.equals(view.lojaId()))
                .toList();
    }

    @Transactional
    public ContaReceberEntity criarContaReceberVenda(
            UUID vendaId, UUID clienteId, BigDecimal valor, LocalDate vencimento) {
        ContaReceberEntity conta = new ContaReceberEntity();
        conta.id = UUID.randomUUID();
        conta.tenantId = tenantContext.getTenantId();
        conta.vendaId = vendaId;
        conta.clienteId = clienteId;
        conta.valor = valor;
        conta.vencimento = vencimento;
        conta.status = StatusContaReceber.ABERTO.name();
        conta.createdAt = Instant.now();
        conta.persist();
        auditService.registrar("ContaReceber", conta.id, "CRIAR", "Venda: " + vendaId);
        return conta;
    }

    @Transactional
    public void cancelarPorVenda(UUID vendaId) {
        UUID tenantId = tenantContext.getTenantId();
        List<ContaReceberEntity> contas =
                ContaReceberEntity.list(
                        "tenantId = ?1 and vendaId = ?2 and status = ?3",
                        tenantId,
                        vendaId,
                        StatusContaReceber.ABERTO.name());
        for (ContaReceberEntity conta : contas) {
            conta.status = StatusContaReceber.CANCELADO.name();
            conta.persist();
            auditService.registrar(
                    "ContaReceber", conta.id, "CANCELAR", "Venda cancelada: " + vendaId);
        }
    }

    @Transactional
    public ContaReceberEntity marcarPago(UUID contaId) {
        ContaReceberEntity conta =
                ContaReceberEntity.<ContaReceberEntity>find(
                                "tenantId = ?1 and id = ?2", tenantContext.getTenantId(), contaId)
                        .firstResultOptional()
                        .orElseThrow(() ->
                                new NotFoundException("CONTA_NOT_FOUND", "Conta a receber não encontrada"));

        conta.status = StatusContaReceber.PAGO.name();
        conta.persist();
        auditService.registrar("ContaReceber", conta.id, "PAGAR", null);
        return conta;
    }

    private UUID resolverLojaId(UUID tenantId, ContaReceberEntity conta) {
        if (conta.vendaId == null) {
            return null;
        }
        return VendaEntity.<VendaEntity>find("tenantId = ?1 and id = ?2", tenantId, conta.vendaId)
                .firstResultOptional()
                .map(v -> v.lojaId)
                .orElse(null);
    }
}
