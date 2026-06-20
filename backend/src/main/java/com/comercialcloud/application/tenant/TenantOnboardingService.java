package com.comercialcloud.application.tenant;

import com.comercialcloud.application.billing.BillingService;
import com.comercialcloud.domain.exception.ConflictException;
import com.comercialcloud.domain.model.Loja;
import com.comercialcloud.domain.model.Tenant;
import com.comercialcloud.domain.model.Usuario;
import com.comercialcloud.domain.repository.UsuarioRepository;
import com.comercialcloud.infrastructure.persistence.entity.AssinaturaTenantEntity;
import com.comercialcloud.infrastructure.persistence.entity.EstoqueEntity;
import com.comercialcloud.infrastructure.persistence.entity.LojaEntity;
import com.comercialcloud.infrastructure.persistence.entity.ProdutoEntity;
import com.comercialcloud.infrastructure.persistence.entity.TenantEntity;
import com.comercialcloud.infrastructure.persistence.mapper.EntityMapper;
import com.comercialcloud.infrastructure.security.KeycloakProvisioningService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Onboarding self-service de um novo tenant: cria tenant + loja + usuário admin,
 * inicia a assinatura (trial ou paga) e, opcionalmente, semeia dados de exemplo
 * para que o lojista consiga usar o PDV imediatamente após o cadastro.
 */
@ApplicationScoped
public class TenantOnboardingService {

    @Inject
    EntityMapper mapper;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    BillingService billingService;

    @Inject
    KeycloakProvisioningService keycloakProvisioningService;

    @Inject
    @ConfigProperty(name = "comercial-cloud.onboarding.senha-padrao")
    Optional<String> senhaPadraoOnboarding;

    @Inject
    @ConfigProperty(name = "comercial-cloud.billing.plano-padrao", defaultValue = "ESSENCIAL")
    String planoPadrao;

    @Inject
    @ConfigProperty(name = "comercial-cloud.onboarding.seed-demo", defaultValue = "true")
    boolean seedDemoPadrao;

    public record OnboardRequest(
            String nomeFantasia,
            String razaoSocial,
            String cnpj,
            String adminNome,
            String adminEmail,
            String adminSenha,
            String planoCodigo,
            Boolean comTrial,
            Boolean seedDemo) {}

    public record OnboardingResult(
            UUID tenantId,
            UUID lojaId,
            UUID adminUserId,
            String planoCodigo,
            String assinaturaStatus,
            LocalDate trialFim,
            int produtosDemo,
            String loginEmail,
            String loginSenha,
            String keycloakStatus) {}

    @Transactional
    public OnboardingResult onboard(OnboardRequest req) {
        Instant now = Instant.now();
        UUID tenantId = UUID.randomUUID();
        UUID lojaId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        Tenant tenant = new Tenant(tenantId, req.razaoSocial(), true, now);
        TenantEntity tenantEntity = mapper.toTenantEntity(tenant);
        tenantEntity.persist();

        Loja loja = new Loja(lojaId, tenantId, req.nomeFantasia(), true, now);
        LojaEntity lojaEntity = mapper.toEntity(loja);
        lojaEntity.persist();

        usuarioRepository
                .findByEmail(tenantId, req.adminEmail())
                .ifPresent(u -> {
                    throw new ConflictException("EMAIL_DUPLICADO", "E-mail já cadastrado");
                });

        Usuario admin =
                new Usuario(adminUserId, tenantId, req.adminNome(), req.adminEmail(), "ADMIN", true, now);
        usuarioRepository.save(admin);

        String planoCodigo =
                req.planoCodigo() != null && !req.planoCodigo().isBlank()
                        ? req.planoCodigo().trim().toUpperCase()
                        : planoPadrao;
        boolean comTrial = req.comTrial() == null || req.comTrial();
        AssinaturaTenantEntity assinatura =
                billingService.assinar(tenantId, planoCodigo, comTrial);
        assinatura.billingEmail = req.adminEmail();
        assinatura.billingCnpj = req.cnpj();

        boolean seed = req.seedDemo() != null ? req.seedDemo() : seedDemoPadrao;
        int produtosDemo = seed ? seedDadosDemo(tenantId, lojaId, now) : 0;

        String senha =
                req.adminSenha() != null && !req.adminSenha().isBlank()
                        ? req.adminSenha()
                        : senhaPadraoOnboarding.filter(s -> !s.isBlank()).orElse(null);

        String loginEmail = req.adminEmail();
        String loginSenha = null;
        String keycloakStatus = "demo_headers";

        if (keycloakProvisioningService.isEnabled() && senha != null && !senha.isBlank()) {
            KeycloakProvisioningService.ProvisionResult kc =
                    keycloakProvisioningService.provisionar(
                            new KeycloakProvisioningService.ProvisionRequest(
                                    tenantId,
                                    lojaId,
                                    adminUserId,
                                    req.adminEmail(),
                                    req.adminNome(),
                                    senha,
                                    "ADMIN"));
            keycloakStatus = kc.mensagem();
            if (kc.criado()) {
                loginSenha = senha;
                keycloakStatus = "keycloak";
            }
        }

        return new OnboardingResult(
                tenantId,
                lojaId,
                adminUserId,
                planoCodigo,
                assinatura.status,
                assinatura.trialFim,
                produtosDemo,
                loginEmail,
                loginSenha,
                keycloakStatus);
    }

    private int seedDadosDemo(UUID tenantId, UUID lojaId, Instant now) {
        record DemoProduto(String sku, String barras, String nome, String preco, String estoque) {}
        DemoProduto[] demos = {
            new DemoProduto("DEMO-001", "7890000000017", "Café Torrado 500g", "18.90", "40"),
            new DemoProduto("DEMO-002", "7890000000024", "Açúcar Refinado 1kg", "5.49", "60"),
            new DemoProduto("DEMO-003", "7890000000031", "Refrigerante 2L", "8.99", "75"),
            new DemoProduto("DEMO-004", "7890000000048", "Pão de Forma", "9.90", "30"),
            new DemoProduto("DEMO-005", "7890000000055", "Leite Integral 1L", "4.79", "90"),
        };
        for (DemoProduto d : demos) {
            UUID produtoId = UUID.randomUUID();
            ProdutoEntity produto = new ProdutoEntity();
            produto.id = produtoId;
            produto.tenantId = tenantId;
            produto.sku = d.sku();
            produto.codigoBarras = d.barras();
            produto.nome = d.nome();
            produto.descricao = "Produto de demonstração";
            produto.preco = new BigDecimal(d.preco());
            produto.ativo = true;
            produto.createdAt = now;
            produto.updatedAt = now;
            produto.persist();

            EstoqueEntity estoque = new EstoqueEntity();
            estoque.id = UUID.randomUUID();
            estoque.tenantId = tenantId;
            estoque.lojaId = lojaId;
            estoque.produtoId = produtoId;
            estoque.quantidade = new BigDecimal(d.estoque());
            estoque.updatedAt = now;
            estoque.persist();
        }
        return demos.length;
    }
}
