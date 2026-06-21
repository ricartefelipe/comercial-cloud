package com.comercialcloud.application.caixa;

import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.domain.model.Caixa;
import com.comercialcloud.domain.model.MovimentoCaixa;
import com.comercialcloud.domain.model.StatusCaixa;
import com.comercialcloud.domain.model.TipoMovimentoCaixa;
import com.comercialcloud.domain.repository.CaixaRepository;
import com.comercialcloud.domain.repository.MovimentoCaixaRepository;
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
public class CaixaMovimentoService {

    @Inject TenantContext tenantContext;

    @Inject AuditService auditService;

    @Inject CaixaRepository caixaRepository;

    @Inject MovimentoCaixaRepository movimentoCaixaRepository;

    public List<MovimentoCaixa> listar(UUID caixaId) {
        UUID tenantId = tenantContext.getTenantId();
        obterCaixaAberto(tenantId, caixaId);
        return movimentoCaixaRepository.listarPorCaixa(tenantId, caixaId);
    }

    @Transactional
    public MovimentoCaixa registrar(UUID caixaId, TipoMovimentoCaixa tipo, BigDecimal valor, String motivo) {
        UUID tenantId = tenantContext.getTenantId();
        UUID usuarioId =
                tenantContext
                        .getUserId()
                        .orElseThrow(
                                () -> new BusinessException("USUARIO_OBRIGATORIO", "Header X-User-Id é obrigatório"));

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("VALOR_INVALIDO", "Valor deve ser maior que zero");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new BusinessException("MOTIVO_OBRIGATORIO", "Motivo é obrigatório");
        }

        Caixa caixa = obterCaixaAberto(tenantId, caixaId);
        MovimentoCaixa movimento =
                new MovimentoCaixa(
                        UUID.randomUUID(),
                        tenantId,
                        caixa.lojaId(),
                        caixa.id(),
                        usuarioId,
                        tipo,
                        valor,
                        motivo.trim(),
                        Instant.now());
        MovimentoCaixa salvo = movimentoCaixaRepository.save(movimento);
        auditService.registrar("Caixa", caixa.id(), tipo.name(), valor.toPlainString() + " — " + motivo.trim());
        return salvo;
    }

    public BigDecimal totalSangrias(UUID tenantId, UUID caixaId) {
        return somarPorTipo(tenantId, caixaId, TipoMovimentoCaixa.SANGRIA);
    }

    public BigDecimal totalSuprimentos(UUID tenantId, UUID caixaId) {
        return somarPorTipo(tenantId, caixaId, TipoMovimentoCaixa.SUPRIMENTO);
    }

    private BigDecimal somarPorTipo(UUID tenantId, UUID caixaId, TipoMovimentoCaixa tipo) {
        return movimentoCaixaRepository.listarPorCaixa(tenantId, caixaId).stream()
                .filter(m -> m.tipo() == tipo)
                .map(MovimentoCaixa::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Caixa obterCaixaAberto(UUID tenantId, UUID caixaId) {
        Caixa caixa =
                caixaRepository
                        .find(tenantId, caixaId)
                        .orElseThrow(() -> new NotFoundException("CAIXA_NOT_FOUND", "Caixa não encontrado"));
        if (caixa.status() != StatusCaixa.ABERTO) {
            throw new BusinessException("CAIXA_FECHADO", "Caixa não está aberto");
        }
        return caixa;
    }
}
