package com.comercialcloud.application.fiscal;

import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.domain.model.AmbienteFiscal;
import com.comercialcloud.domain.model.ModeloDocumentoFiscal;
import com.comercialcloud.domain.model.RegimeTributario;
import com.comercialcloud.domain.model.StatusDocumentoFiscal;
import com.comercialcloud.infrastructure.audit.AuditService;
import com.comercialcloud.infrastructure.persistence.entity.ClienteEntity;
import com.comercialcloud.infrastructure.persistence.entity.ConfiguracaoFiscalEntity;
import com.comercialcloud.infrastructure.persistence.entity.DocumentoFiscalEntity;
import com.comercialcloud.infrastructure.persistence.entity.ItemVendaEntity;
import com.comercialcloud.infrastructure.persistence.entity.ProdutoEntity;
import com.comercialcloud.infrastructure.persistence.entity.VendaEntity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FiscalService {

    private static final Logger LOG = Logger.getLogger(FiscalService.class);

    private static final String NCM_PADRAO = "00000000";
    private static final String CFOP_PADRAO = "5102";
    private static final String UNIDADE_PADRAO = "UN";

    @Inject TenantContext tenantContext;
    @Inject AuditService auditService;
    @Inject ConfiguracaoFiscalService configuracaoFiscalService;

    @Inject
    @ConfigProperty(name = "comercial-cloud.fiscal.enabled", defaultValue = "true")
    boolean fiscalEnabled;

    @Inject
    @ConfigProperty(name = "comercial-cloud.fiscal.provider", defaultValue = "sandbox")
    String providerNome;

    @Inject
    @ConfigProperty(name = "comercial-cloud.fiscal.max-tentativas", defaultValue = "5")
    int maxTentativas;

    @Inject
    Instance<FiscalProvider> providers;

    // ---------------------------------------------------------------------
    // Registro (padrão outbox) — chamado dentro da transação da venda
    // ---------------------------------------------------------------------

    /**
     * Registra a intenção de emitir um documento fiscal para a venda. Idempotente
     * por (tenant, venda, modelo). Não emite de fato: deixa PENDENTE para o worker.
     */
    public void registrarEmissaoPendente(
            UUID tenantId, UUID lojaId, UUID vendaId, ModeloDocumentoFiscal modelo) {
        if (!fiscalEnabled) {
            return;
        }
        boolean habilitada =
                configuracaoFiscalService
                        .buscarPorLoja(tenantId, lojaId)
                        .map(c -> c.habilitado)
                        .orElse(false);
        if (!habilitada) {
            return;
        }
        boolean jaExiste =
                DocumentoFiscalEntity.count(
                                "tenantId = ?1 and vendaId = ?2 and modelo = ?3",
                                tenantId,
                                vendaId,
                                modelo.name())
                        > 0;
        if (jaExiste) {
            return;
        }

        Instant agora = Instant.now();
        DocumentoFiscalEntity doc = new DocumentoFiscalEntity();
        doc.id = UUID.randomUUID();
        doc.tenantId = tenantId;
        doc.lojaId = lojaId;
        doc.vendaId = vendaId;
        doc.modelo = modelo.name();
        doc.status = StatusDocumentoFiscal.PENDENTE.name();
        doc.tentativas = 0;
        doc.createdAt = agora;
        doc.updatedAt = agora;
        doc.persist();
    }

    // ---------------------------------------------------------------------
    // Processamento assíncrono (worker)
    // ---------------------------------------------------------------------

    /**
     * IDs de documentos pendentes elegíveis a processamento. O loop transacional
     * por documento fica a cargo do chamador (scheduler/resource), garantindo que
     * cada {@link #processarDocumento(UUID)} rode em sua própria transação via proxy
     * CDI e que falhas isoladas não contaminem o lote.
     */
    @Transactional
    public List<UUID> idsPendentes() {
        if (!fiscalEnabled) {
            return List.of();
        }
        return DocumentoFiscalEntity.<DocumentoFiscalEntity>find(
                        "status = ?1 and tentativas < ?2",
                        StatusDocumentoFiscal.PENDENTE.name(),
                        maxTentativas)
                .stream()
                .map(d -> d.id)
                .toList();
    }

    @Transactional
    @ActivateRequestContext
    public DocumentoFiscalEntity processarDocumento(UUID documentoId) {
        DocumentoFiscalEntity doc =
                DocumentoFiscalEntity.<DocumentoFiscalEntity>findById(documentoId);
        if (doc == null || !StatusDocumentoFiscal.PENDENTE.name().equals(doc.status)) {
            return doc;
        }

        doc.tentativas += 1;
        doc.updatedAt = Instant.now();

        Optional<ConfiguracaoFiscalEntity> configOpt =
                configuracaoFiscalService.buscarPorLoja(doc.tenantId, doc.lojaId);
        if (configOpt.isEmpty() || !configOpt.get().habilitado) {
            aplicarResultado(
                    doc,
                    EmissaoResultado.erro("Configuração fiscal da loja ausente ou desabilitada"));
            return doc;
        }

        VendaEntity venda =
                VendaEntity.<VendaEntity>find("tenantId = ?1 and id = ?2", doc.tenantId, doc.vendaId)
                        .firstResultOptional()
                        .orElse(null);
        if (venda == null) {
            aplicarResultado(doc, EmissaoResultado.erro("Venda não encontrada para emissão"));
            return doc;
        }

        long numero =
                configuracaoFiscalService.proximoNumero(doc.tenantId, doc.lojaId);
        ConfiguracaoFiscalEntity config = configOpt.get();
        EmissaoFiscalRequest request =
                montarRequest(doc, config, venda, ModeloDocumentoFiscal.valueOf(doc.modelo), numero);

        EmissaoResultado resultado;
        try {
            resultado = provider().emitir(request);
        } catch (RuntimeException e) {
            LOG.errorf(e, "Erro no provedor fiscal para documento %s", doc.id);
            resultado = EmissaoResultado.erro("Erro no provedor: " + e.getMessage());
        }
        doc.serie = config.serieNfce;
        doc.numero = (int) numero;
        aplicarResultado(doc, resultado);
        auditService.registrar("DocumentoFiscal", doc.id, "EMITIR", doc.status);
        return doc;
    }

    // ---------------------------------------------------------------------
    // Operações tenant-scoped (REST/demo)
    // ---------------------------------------------------------------------

    /** Garante o registro pendente e processa imediatamente (uso interativo/demo). */
    @Transactional
    public DocumentoFiscalEntity emitirAgora(UUID vendaId, ModeloDocumentoFiscal modelo) {
        UUID tenantId = tenantContext.getTenantId();
        VendaEntity venda =
                VendaEntity.<VendaEntity>find("tenantId = ?1 and id = ?2", tenantId, vendaId)
                        .firstResultOptional()
                        .orElseThrow(() -> new NotFoundException("VENDA_NOT_FOUND", "Venda não encontrada"));

        registrarEmissaoPendente(tenantId, venda.lojaId, vendaId, modelo);
        DocumentoFiscalEntity doc =
                DocumentoFiscalEntity.<DocumentoFiscalEntity>find(
                                "tenantId = ?1 and vendaId = ?2 and modelo = ?3",
                                tenantId,
                                vendaId,
                                modelo.name())
                        .firstResultOptional()
                        .orElseThrow(() ->
                                new BusinessException(
                                        "EMISSAO_INDISPONIVEL",
                                        "Emissão fiscal não habilitada para a loja desta venda"));

        if (StatusDocumentoFiscal.PENDENTE.name().equals(doc.status)) {
            return processarDocumento(doc.id);
        }
        return doc;
    }

    @Transactional
    public DocumentoFiscalEntity cancelar(UUID documentoId, String justificativa) {
        UUID tenantId = tenantContext.getTenantId();
        DocumentoFiscalEntity doc =
                DocumentoFiscalEntity.<DocumentoFiscalEntity>find(
                                "tenantId = ?1 and id = ?2", tenantId, documentoId)
                        .firstResultOptional()
                        .orElseThrow(() ->
                                new NotFoundException("DOC_FISCAL_NOT_FOUND", "Documento fiscal não encontrado"));

        if (!StatusDocumentoFiscal.AUTORIZADO.name().equals(doc.status)) {
            throw new BusinessException(
                    "DOC_NAO_CANCELAVEL", "Apenas documentos autorizados podem ser cancelados");
        }

        ConfiguracaoFiscalEntity config =
                configuracaoFiscalService
                        .buscarPorLoja(tenantId, doc.lojaId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "CONFIG_FISCAL_NOT_FOUND", "Configuração fiscal não encontrada"));

        CancelamentoResultado resultado =
                provider()
                        .cancelar(
                                new CancelamentoRequest(
                                        doc.chaveAcesso,
                                        doc.protocolo,
                                        justificativa,
                                        AmbienteFiscal.valueOf(config.ambiente)));
        if (!resultado.sucesso()) {
            throw new BusinessException("CANCELAMENTO_REJEITADO", resultado.motivo());
        }

        doc.status = StatusDocumentoFiscal.CANCELADO.name();
        doc.protocolo = resultado.protocolo();
        doc.updatedAt = Instant.now();
        auditService.registrar("DocumentoFiscal", doc.id, "CANCELAR", justificativa);
        return doc;
    }

    public Optional<DocumentoFiscalEntity> buscarPorVenda(UUID vendaId) {
        return DocumentoFiscalEntity.<DocumentoFiscalEntity>find(
                        "tenantId = ?1 and vendaId = ?2",
                        Sort.by("createdAt").descending(),
                        tenantContext.getTenantId(),
                        vendaId)
                .firstResultOptional();
    }

    public DocumentoFiscalEntity buscarPorId(UUID id) {
        return DocumentoFiscalEntity.<DocumentoFiscalEntity>find(
                        "tenantId = ?1 and id = ?2", tenantContext.getTenantId(), id)
                .firstResultOptional()
                .orElseThrow(() ->
                        new NotFoundException("DOC_FISCAL_NOT_FOUND", "Documento fiscal não encontrado"));
    }

    public List<DocumentoFiscalEntity> listar(UUID lojaId) {
        if (lojaId != null) {
            return DocumentoFiscalEntity.<DocumentoFiscalEntity>find(
                            "tenantId = ?1 and lojaId = ?2",
                            Sort.by("createdAt").descending(),
                            tenantContext.getTenantId(),
                            lojaId)
                    .list();
        }
        return DocumentoFiscalEntity.<DocumentoFiscalEntity>find(
                        "tenantId = ?1", Sort.by("createdAt").descending(), tenantContext.getTenantId())
                .list();
    }

    // ---------------------------------------------------------------------
    // Internos
    // ---------------------------------------------------------------------

    private void aplicarResultado(DocumentoFiscalEntity doc, EmissaoResultado resultado) {
        doc.status = resultado.status().name();
        doc.chaveAcesso = resultado.chaveAcesso();
        doc.protocolo = resultado.protocolo();
        doc.motivoRejeicao = resultado.motivoRejeicao();
        doc.urlDanfe = resultado.urlDanfe();
        doc.providerRef = resultado.providerRef();
        doc.updatedAt = Instant.now();
    }

    private EmissaoFiscalRequest montarRequest(
            DocumentoFiscalEntity doc,
            ConfiguracaoFiscalEntity config,
            VendaEntity venda,
            ModeloDocumentoFiscal modelo,
            long numero) {
        List<EmissaoFiscalRequest.EmissaoItem> itens = new ArrayList<>();
        for (ItemVendaEntity item : venda.itens) {
            String descricao =
                    ProdutoEntity.<ProdutoEntity>find(
                                    "tenantId = ?1 and id = ?2", doc.tenantId, item.produtoId)
                            .firstResultOptional()
                            .map(p -> p.nome)
                            .orElse("Produto " + item.produtoId);
            itens.add(
                    new EmissaoFiscalRequest.EmissaoItem(
                            descricao,
                            item.quantidade,
                            item.precoUnitario,
                            item.subtotal,
                            NCM_PADRAO,
                            CFOP_PADRAO,
                            UNIDADE_PADRAO));
        }

        String destinatarioDoc = null;
        String destinatarioNome = null;
        if (venda.clienteId != null) {
            ClienteEntity cliente =
                    ClienteEntity.<ClienteEntity>find(
                                    "tenantId = ?1 and id = ?2", doc.tenantId, venda.clienteId)
                            .firstResultOptional()
                            .orElse(null);
            if (cliente != null) {
                destinatarioDoc = cliente.cpfCnpj;
                destinatarioNome = cliente.nome;
            }
        }

        return new EmissaoFiscalRequest(
                doc.tenantId,
                doc.lojaId,
                doc.vendaId,
                modelo,
                AmbienteFiscal.valueOf(config.ambiente),
                config.cnpj,
                config.inscricaoEstadual,
                config.razaoSocial,
                RegimeTributario.valueOf(config.regimeTributario),
                config.uf,
                config.cscId,
                config.csc,
                config.serieNfce,
                numero,
                venda.total,
                destinatarioDoc,
                destinatarioNome,
                itens);
    }

    private FiscalProvider provider() {
        for (FiscalProvider p : providers) {
            if (p.nome().equalsIgnoreCase(providerNome)) {
                return p;
            }
        }
        throw new BusinessException(
                "PROVEDOR_FISCAL_INDISPONIVEL",
                "Nenhum provedor fiscal configurado para '" + providerNome + "'");
    }
}
