package com.comercialcloud.application.billing;

import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.domain.exception.ConflictException;
import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.domain.model.StatusAssinatura;
import com.comercialcloud.domain.model.StatusFatura;
import com.comercialcloud.infrastructure.audit.AuditService;
import com.comercialcloud.infrastructure.persistence.entity.AssinaturaTenantEntity;
import com.comercialcloud.infrastructure.persistence.entity.FaturaAssinaturaEntity;
import com.comercialcloud.infrastructure.persistence.entity.PlanoAssinaturaEntity;
import com.comercialcloud.infrastructure.persistence.entity.TenantEntity;
import com.comercialcloud.infrastructure.mail.TransacionalEmailService;
import com.comercialcloud.infrastructure.security.TenantContext;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class BillingService {

    private static final Logger LOG = Logger.getLogger(BillingService.class);
    private static final DateTimeFormatter COMPETENCIA = DateTimeFormatter.ofPattern("yyyy-MM");

    @Inject TenantContext tenantContext;
    @Inject AuditService auditService;
    @Inject Instance<GatewayPagamento> gateways;
    @Inject TransacionalEmailService emailService;

    @Inject
    @ConfigProperty(name = "comercial-cloud.billing.enabled", defaultValue = "true")
    boolean billingEnabled;

    @Inject
    @ConfigProperty(name = "comercial-cloud.billing.gateway", defaultValue = "sandbox")
    String gatewayNome;

    @Inject
    @ConfigProperty(name = "comercial-cloud.billing.trial-dias", defaultValue = "14")
    int trialDias;

    @Inject
    @ConfigProperty(name = "comercial-cloud.billing.max-tentativas", defaultValue = "3")
    int maxTentativas;

    @Inject
    @ConfigProperty(name = "comercial-cloud.billing.vencimento-dias", defaultValue = "5")
    int vencimentoDias;

    // ---------------------------------------------------------------------
    // Planos
    // ---------------------------------------------------------------------

    public List<PlanoAssinaturaEntity> listarPlanos() {
        return PlanoAssinaturaEntity.<PlanoAssinaturaEntity>find("ativo = true", Sort.by("precoMensal"))
                .list();
    }

    private PlanoAssinaturaEntity planoPorCodigo(String codigo) {
        return PlanoAssinaturaEntity.<PlanoAssinaturaEntity>find("codigo = ?1", codigo)
                .firstResultOptional()
                .orElseThrow(() -> new NotFoundException("PLANO_NOT_FOUND", "Plano não encontrado: " + codigo));
    }

    // ---------------------------------------------------------------------
    // Assinatura (tenant-scoped e onboarding)
    // ---------------------------------------------------------------------

    public Optional<AssinaturaTenantEntity> assinaturaDoTenant(UUID tenantId) {
        return AssinaturaTenantEntity.<AssinaturaTenantEntity>find("tenantId = ?1", tenantId)
                .firstResultOptional();
    }

    public Optional<AssinaturaTenantEntity> assinaturaAtual() {
        return assinaturaDoTenant(tenantContext.getTenantId());
    }

    @Transactional
    public AssinaturaTenantEntity assinar(UUID tenantId, String planoCodigo, boolean comTrial) {
        PlanoAssinaturaEntity plano = planoPorCodigo(planoCodigo);

        Optional<AssinaturaTenantEntity> existente = assinaturaDoTenant(tenantId);
        if (existente.isPresent()
                && !StatusAssinatura.CANCELADA.name().equals(existente.get().status)) {
            throw new ConflictException(
                    "ASSINATURA_EXISTENTE", "Tenant já possui assinatura ativa");
        }

        Instant agora = Instant.now();
        LocalDate hoje = LocalDate.now();
        AssinaturaTenantEntity assinatura = existente.orElseGet(AssinaturaTenantEntity::new);
        if (assinatura.id == null) {
            assinatura.id = UUID.randomUUID();
            assinatura.tenantId = tenantId;
            assinatura.createdAt = agora;
        }
        assinatura.planoId = plano.id;
        assinatura.inicio = hoje;
        assinatura.tentativasFalha = 0;
        if (comTrial && trialDias > 0) {
            assinatura.status = StatusAssinatura.TRIAL.name();
            assinatura.trialFim = hoje.plusDays(trialDias);
            assinatura.proximaCobranca = assinatura.trialFim;
        } else {
            assinatura.status = StatusAssinatura.ATIVA.name();
            assinatura.trialFim = null;
            assinatura.proximaCobranca = hoje;
        }
        assinatura.updatedAt = agora;
        assinatura.persist();
        auditService.registrar(
                "AssinaturaTenant", assinatura.id, "ASSINAR", plano.codigo + "/" + assinatura.status);
        return assinatura;
    }

    @Transactional
    public AssinaturaTenantEntity trocarPlano(String planoCodigo) {
        UUID tenantId = tenantContext.getTenantId();
        PlanoAssinaturaEntity plano = planoPorCodigo(planoCodigo);
        AssinaturaTenantEntity assinatura =
                assinaturaDoTenant(tenantId)
                        .orElseThrow(() ->
                                new NotFoundException("ASSINATURA_NOT_FOUND", "Assinatura não encontrada"));
        assinatura.planoId = plano.id;
        assinatura.updatedAt = Instant.now();
        auditService.registrar("AssinaturaTenant", assinatura.id, "TROCAR_PLANO", plano.codigo);
        return assinatura;
    }

    @Transactional
    public AssinaturaTenantEntity cancelar() {
        UUID tenantId = tenantContext.getTenantId();
        AssinaturaTenantEntity assinatura =
                assinaturaDoTenant(tenantId)
                        .orElseThrow(() ->
                                new NotFoundException("ASSINATURA_NOT_FOUND", "Assinatura não encontrada"));
        assinatura.status = StatusAssinatura.CANCELADA.name();
        assinatura.updatedAt = Instant.now();
        auditService.registrar("AssinaturaTenant", assinatura.id, "CANCELAR", null);
        return assinatura;
    }

    @Transactional
    public AssinaturaTenantEntity reativar() {
        UUID tenantId = tenantContext.getTenantId();
        AssinaturaTenantEntity assinatura =
                assinaturaDoTenant(tenantId)
                        .orElseThrow(() ->
                                new NotFoundException("ASSINATURA_NOT_FOUND", "Assinatura não encontrada"));
        assinatura.status = StatusAssinatura.ATIVA.name();
        assinatura.tentativasFalha = 0;
        assinatura.updatedAt = Instant.now();
        ativarTenant(tenantId);
        auditService.registrar("AssinaturaTenant", assinatura.id, "REATIVAR", null);
        return assinatura;
    }

    // ---------------------------------------------------------------------
    // Faturas (tenant-scoped)
    // ---------------------------------------------------------------------

    public List<FaturaAssinaturaEntity> listarFaturas() {
        return FaturaAssinaturaEntity.<FaturaAssinaturaEntity>find(
                        "tenantId = ?1", Sort.by("vencimento").descending(), tenantContext.getTenantId())
                .list();
    }

    @Transactional
    public FaturaAssinaturaEntity marcarFaturaPaga(UUID faturaId) {
        UUID tenantId = tenantContext.getTenantId();
        FaturaAssinaturaEntity fatura =
                FaturaAssinaturaEntity.<FaturaAssinaturaEntity>find(
                                "tenantId = ?1 and id = ?2", tenantId, faturaId)
                        .firstResultOptional()
                        .orElseThrow(() ->
                                new NotFoundException("FATURA_NOT_FOUND", "Fatura não encontrada"));
        aplicarPagamentoAprovado(fatura, "MANUAL-" + UUID.randomUUID());
        auditService.registrar("FaturaAssinatura", fatura.id, "PAGAR_MANUAL", fatura.competencia);
        return fatura;
    }

    // ---------------------------------------------------------------------
    // Geração de faturas (scheduler) — proxy-safe
    // ---------------------------------------------------------------------

    @Transactional
    public List<UUID> idsAssinaturasParaFaturar() {
        if (!billingEnabled) {
            return List.of();
        }
        LocalDate hoje = LocalDate.now();
        return AssinaturaTenantEntity.<AssinaturaTenantEntity>find(
                        "status in ?1 and proximaCobranca <= ?2",
                        List.of(
                                StatusAssinatura.TRIAL.name(),
                                StatusAssinatura.ATIVA.name(),
                                StatusAssinatura.INADIMPLENTE.name()),
                        hoje)
                .stream()
                .map(a -> a.id)
                .toList();
    }

    @Transactional
    @ActivateRequestContext
    public void gerarFaturaAssinatura(UUID assinaturaId) {
        AssinaturaTenantEntity assinatura =
                AssinaturaTenantEntity.<AssinaturaTenantEntity>findById(assinaturaId);
        if (assinatura == null) {
            return;
        }
        LocalDate hoje = LocalDate.now();
        if (assinatura.proximaCobranca.isAfter(hoje)) {
            return;
        }

        String competencia = YearMonth.from(assinatura.proximaCobranca).format(COMPETENCIA);
        boolean jaExiste =
                FaturaAssinaturaEntity.count(
                                "tenantId = ?1 and assinaturaId = ?2 and competencia = ?3",
                                assinatura.tenantId,
                                assinatura.id,
                                competencia)
                        > 0;
        if (!jaExiste) {
            PlanoAssinaturaEntity plano = PlanoAssinaturaEntity.findById(assinatura.planoId);
            FaturaAssinaturaEntity fatura = new FaturaAssinaturaEntity();
            fatura.id = UUID.randomUUID();
            fatura.tenantId = assinatura.tenantId;
            fatura.assinaturaId = assinatura.id;
            fatura.competencia = competencia;
            fatura.valor = plano.precoMensal;
            fatura.vencimento = hoje.plusDays(vencimentoDias);
            fatura.status = StatusFatura.ABERTA.name();
            fatura.tentativas = 0;
            fatura.createdAt = Instant.now();
            fatura.persist();
            emailService.enviarNovaFatura(
                    emailCobranca(assinatura),
                    competencia,
                    fatura.valor,
                    fatura.vencimento,
                    null);
        }

        if (StatusAssinatura.TRIAL.name().equals(assinatura.status)) {
            assinatura.status = StatusAssinatura.ATIVA.name();
        }
        assinatura.proximaCobranca = assinatura.proximaCobranca.plusMonths(1);
        assinatura.updatedAt = Instant.now();
    }

    // ---------------------------------------------------------------------
    // Cobrança de faturas (scheduler) — proxy-safe
    // ---------------------------------------------------------------------

    @Transactional
    public List<UUID> idsFaturasParaCobrar() {
        if (!billingEnabled) {
            return List.of();
        }
        LocalDate hoje = LocalDate.now();
        return FaturaAssinaturaEntity.<FaturaAssinaturaEntity>find(
                        "status = ?1 and vencimento <= ?2 and tentativas < ?3",
                        StatusFatura.ABERTA.name(),
                        hoje,
                        maxTentativas)
                .stream()
                .map(f -> f.id)
                .toList();
    }

    @Transactional
    @ActivateRequestContext
    public void cobrarFatura(UUID faturaId) {
        FaturaAssinaturaEntity fatura = FaturaAssinaturaEntity.findById(faturaId);
        if (fatura == null || !StatusFatura.ABERTA.name().equals(fatura.status)) {
            return;
        }
        AssinaturaTenantEntity assinatura =
                AssinaturaTenantEntity.findById(fatura.assinaturaId);
        if (assinatura == null) {
            return;
        }

        GatewayPagamento.ResultadoCobranca resultado;
        try {
            resultado =
                    gateway()
                            .cobrar(
                                    new GatewayPagamento.CobrancaRequest(
                                            fatura.tenantId,
                                            fatura.id,
                                            fatura.valor,
                                            "Assinatura " + fatura.competencia));
        } catch (RuntimeException e) {
            LOG.errorf(e, "Erro no gateway de pagamento para fatura %s", fatura.id);
            resultado = GatewayPagamento.ResultadoCobranca.recusado("Erro no gateway: " + e.getMessage());
        }

        fatura.tentativas += 1;
        if (resultado.aprovado()) {
            aplicarPagamentoAprovado(fatura, resultado.transacaoRef());
            return;
        }

        if (resultado.pendente()) {
            fatura.gatewayPaymentId = resultado.transacaoRef();
            fatura.checkoutUrl = resultado.checkoutUrl();
            fatura.motivoRecusa = resultado.motivoRecusa();
            fatura.tentativas -= 1;
            emailService.enviarNovaFatura(
                    emailCobranca(assinatura),
                    fatura.competencia,
                    fatura.valor,
                    fatura.vencimento,
                    fatura.checkoutUrl);
            return;
        }

        fatura.motivoRecusa = resultado.motivoRecusa();
        assinatura.tentativasFalha += 1;
        assinatura.updatedAt = Instant.now();
        if (fatura.tentativas >= maxTentativas) {
            fatura.status = StatusFatura.FALHA.name();
            assinatura.status = StatusAssinatura.SUSPENSA.name();
            suspenderTenant(assinatura.tenantId);
            emailService.enviarSuspensao(emailCobranca(assinatura), fatura.competencia);
        } else {
            assinatura.status = StatusAssinatura.INADIMPLENTE.name();
        }
    }

    // ---------------------------------------------------------------------
    // Internos
    // ---------------------------------------------------------------------

    private void aplicarPagamentoAprovado(FaturaAssinaturaEntity fatura, String transacaoRef) {
        fatura.status = StatusFatura.PAGA.name();
        fatura.transacaoRef = transacaoRef;
        fatura.motivoRecusa = null;
        fatura.pagaEm = Instant.now();

        AssinaturaTenantEntity assinatura =
                AssinaturaTenantEntity.findById(fatura.assinaturaId);
        if (assinatura != null) {
            assinatura.status = StatusAssinatura.ATIVA.name();
            assinatura.tentativasFalha = 0;
            assinatura.updatedAt = Instant.now();
            ativarTenant(assinatura.tenantId);
            emailService.enviarPagamentoConfirmado(
                    emailCobranca(assinatura), fatura.competencia);
        }
    }

    private String emailCobranca(AssinaturaTenantEntity assinatura) {
        if (assinatura == null) {
            return null;
        }
        if (assinatura.billingEmail != null && !assinatura.billingEmail.isBlank()) {
            return assinatura.billingEmail;
        }
        return null;
    }

    private void suspenderTenant(UUID tenantId) {
        TenantEntity tenant = TenantEntity.findById(tenantId);
        if (tenant != null) {
            tenant.ativo = false;
        }
    }

    private void ativarTenant(UUID tenantId) {
        TenantEntity tenant = TenantEntity.findById(tenantId);
        if (tenant != null && !tenant.ativo) {
            tenant.ativo = true;
        }
    }

    @Transactional
    public void confirmarPagamentoGateway(String gatewayPaymentId, String transacaoRef) {
        FaturaAssinaturaEntity fatura =
                FaturaAssinaturaEntity.<FaturaAssinaturaEntity>find(
                                "gatewayPaymentId = ?1 or transacaoRef = ?1", gatewayPaymentId)
                        .firstResultOptional()
                        .orElse(null);
        if (fatura == null || StatusFatura.PAGA.name().equals(fatura.status)) {
            return;
        }
        aplicarPagamentoAprovado(fatura, transacaoRef != null ? transacaoRef : gatewayPaymentId);
        auditService.registrar("FaturaAssinatura", fatura.id, "PAGAR_WEBHOOK", fatura.competencia);
    }

    private GatewayPagamento gateway() {
        for (GatewayPagamento g : gateways) {
            if (g.nome().equalsIgnoreCase(gatewayNome)) {
                return g;
            }
        }
        throw new BusinessException(
                "GATEWAY_INDISPONIVEL", "Nenhum gateway de pagamento configurado para '" + gatewayNome + "'");
    }

    public int limiteLojas(UUID tenantId) {
        return assinaturaDoTenant(tenantId)
                .map(a -> PlanoAssinaturaEntity.findById(a.planoId))
                .filter(p -> p != null)
                .map(p -> ((PlanoAssinaturaEntity) p).maxLojas)
                .orElse(1);
    }
}
