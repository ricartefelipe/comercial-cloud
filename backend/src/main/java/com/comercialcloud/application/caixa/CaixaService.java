package com.comercialcloud.application.caixa;

import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.domain.exception.ConflictException;
import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.domain.model.Caixa;
import com.comercialcloud.domain.model.FormaPagamento;
import com.comercialcloud.domain.model.StatusCaixa;
import com.comercialcloud.domain.model.Venda;
import com.comercialcloud.domain.repository.CaixaRepository;
import com.comercialcloud.domain.repository.LojaRepository;
import com.comercialcloud.domain.repository.UsuarioRepository;
import com.comercialcloud.domain.repository.VendaRepository;
import com.comercialcloud.infrastructure.audit.AuditService;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CaixaService {

    @Inject TenantContext tenantContext;
    @Inject AuditService auditService;
    @Inject CaixaRepository caixaRepository;
    @Inject LojaRepository lojaRepository;
    @Inject UsuarioRepository usuarioRepository;
    @Inject VendaRepository vendaRepository;

    public record ResumoCaixa(
            UUID caixaId,
            BigDecimal totalVendas,
            BigDecimal totalDinheiro,
            BigDecimal totalPix,
            BigDecimal totalCartao,
            BigDecimal totalOutros,
            long quantidadeVendas,
            BigDecimal valorAbertura,
            BigDecimal valorCalculado
    ) {}

    @Transactional
    public Caixa abrir(UUID lojaId, UUID operadorId, BigDecimal valorAbertura) {
        UUID tenantId = tenantContext.getTenantId();
        validarLoja(tenantId, lojaId);
        validarOperador(tenantId, operadorId);

        caixaRepository.findAberto(tenantId, lojaId, operadorId).ifPresent(c -> {
            throw new ConflictException("CAIXA_ABERTO", "Operador já possui caixa aberto nesta loja");
        });

        Instant agora = Instant.now();
        BigDecimal valor = valorAbertura != null ? valorAbertura : BigDecimal.ZERO;
        Caixa caixa =
                new Caixa(
                        UUID.randomUUID(),
                        tenantId,
                        lojaId,
                        operadorId,
                        StatusCaixa.ABERTO,
                        valor,
                        null,
                        BigDecimal.ZERO,
                        agora,
                        null);
        Caixa salvo = caixaRepository.save(caixa);
        auditService.registrar("Caixa", salvo.id(), "ABRIR", "Loja: " + lojaId);
        return salvo;
    }

    @Transactional
    public Caixa fechar(UUID id, BigDecimal valorFechamentoInformado) {
        UUID tenantId = tenantContext.getTenantId();
        Caixa caixa = obter(tenantId, id);
        if (caixa.status() != StatusCaixa.ABERTO) {
            throw new BusinessException("CAIXA_FECHADO", "Caixa não está aberto");
        }
        ResumoCaixa resumo = calcularResumo(caixa);
        Instant agora = Instant.now();
        Caixa fechado =
                new Caixa(
                        caixa.id(),
                        caixa.tenantId(),
                        caixa.lojaId(),
                        caixa.usuarioId(),
                        StatusCaixa.FECHADO,
                        caixa.valorAbertura(),
                        valorFechamentoInformado,
                        resumo.totalVendas(),
                        caixa.abertoEm(),
                        agora);
        Caixa salvo = caixaRepository.save(fechado);
        auditService.registrar("Caixa", salvo.id(), "FECHAR", "Total vendas: " + salvo.totalVendas());
        return salvo;
    }

    public Optional<Caixa> buscarAberto(UUID lojaId, UUID operadorId) {
        return caixaRepository.findAberto(tenantContext.getTenantId(), lojaId, operadorId);
    }

    public Caixa exigirCaixaAberto(UUID lojaId, UUID operadorId) {
        return buscarAberto(lojaId, operadorId).orElseThrow(
                () -> new BusinessException("CAIXA_FECHADO", "Operador deve abrir caixa antes de vender"));
    }

    public ResumoCaixa resumo(UUID id) {
        return calcularResumo(obter(tenantContext.getTenantId(), id));
    }

    private ResumoCaixa calcularResumo(Caixa caixa) {
        List<Venda> vendas = vendaRepository.listFinalizadasPorCaixaComDetalhes(caixa.tenantId(), caixa.id());
        BigDecimal totalVendas = BigDecimal.ZERO;
        BigDecimal din = BigDecimal.ZERO;
        BigDecimal pix = BigDecimal.ZERO;
        BigDecimal card = BigDecimal.ZERO;
        BigDecimal out = BigDecimal.ZERO;
        for (Venda v : vendas) {
            totalVendas = totalVendas.add(v.total());
            for (var p : v.pagamentos()) {
                FormaPagamento f = p.formaPagamento();
                switch (f) {
                    case DINHEIRO -> din = din.add(p.valor());
                    case PIX -> pix = pix.add(p.valor());
                    case CARTAO_DEBITO, CARTAO_CREDITO -> card = card.add(p.valor());
                    default -> out = out.add(p.valor());
                }
            }
        }
        return new ResumoCaixa(
                caixa.id(),
                totalVendas,
                din,
                pix,
                card,
                out,
                vendas.size(),
                caixa.valorAbertura(),
                caixa.valorAbertura().add(totalVendas));
    }

    private Caixa obter(UUID tenantId, UUID id) {
        return caixaRepository.find(tenantId, id)
                .orElseThrow(() -> new NotFoundException("CAIXA_NOT_FOUND", "Caixa não encontrado"));
    }

    private void validarLoja(UUID tenantId, UUID lojaId) {
        lojaRepository.find(tenantId, lojaId)
                .orElseThrow(() -> new NotFoundException("LOJA_NOT_FOUND", "Loja não encontrada"));
    }

    private void validarOperador(UUID tenantId, UUID operadorId) {
        usuarioRepository.find(tenantId, operadorId)
                .orElseThrow(() -> new NotFoundException("USUARIO_NOT_FOUND", "Usuário não encontrado"));
    }
}
