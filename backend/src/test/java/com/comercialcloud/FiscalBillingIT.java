package com.comercialcloud;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FiscalBillingIT {

    static final UUID TENANT_DEMO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID LOJA_DEMO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID USER_CAIXA = UUID.fromString("44444444-4444-4444-4444-444444444444");
    static final UUID PRODUTO_1 = UUID.fromString("55555555-5555-5555-5555-555555555551");
    static final UUID TENANT_BILLING = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    static final UUID TENANT_SUSPENSO = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    // ------------------------------------------------------------------
    // Billing
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void planosDisponiveis() {
        given()
                .header("X-Tenant-Id", TENANT_BILLING)
                .when().get("/api/v1/billing/planos")
                .then()
                .statusCode(200)
                .body("$", hasSize(3));
    }

    @Test
    @Order(2)
    void onboardingCriaAssinaturaTrial() {
        String tenantId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "nomeFantasia", "Loja Teste Onboarding",
                        "razaoSocial", "Teste Onboarding LTDA",
                        "cnpj", "12345678000199",
                        "adminNome", "Admin Teste",
                        "adminEmail", "admin@onboardingteste.com"))
                .when().post("/api/v1/public/onboarding")
                .then()
                .statusCode(201)
                .extract().path("tenantId");

        given()
                .header("X-Tenant-Id", tenantId)
                .when().get("/api/v1/billing/assinatura")
                .then()
                .statusCode(200)
                .body("status", equalTo("TRIAL"));
    }

    @Test
    @Order(7)
    void planosPublicosSemTenant() {
        given()
                .when().get("/api/v1/public/planos")
                .then()
                .statusCode(200)
                .body("$", hasSize(3))
                .body("[0].codigo", notNullValue());
    }

    @Test
    @Order(8)
    void onboardingComPlanoESeedDemo() {
        String tenantId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "nomeFantasia", "Mercadinho Self Service",
                        "razaoSocial", "Self Service Varejo LTDA",
                        "cnpj", "98765432000188",
                        "adminNome", "Dono Loja",
                        "adminEmail", "dono@selfservice.com",
                        "planoCodigo", "PRO",
                        "comTrial", false,
                        "seedDemo", true))
                .when().post("/api/v1/public/onboarding")
                .then()
                .statusCode(201)
                .body("planoCodigo", equalTo("PRO"))
                .body("assinaturaStatus", equalTo("ATIVA"))
                .body("produtosDemo", equalTo(5))
                .extract().path("tenantId");

        given()
                .header("X-Tenant-Id", tenantId)
                .when().get("/api/v1/produtos")
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(5))
                .body("content", hasSize(5));
    }

    @Test
    @Order(3)
    void assinarSemTrialAtiva() {
        given()
                .header("X-Tenant-Id", TENANT_BILLING)
                .contentType(ContentType.JSON)
                .body(Map.of("planoCodigo", "PRO", "comTrial", false))
                .when().post("/api/v1/billing/assinar")
                .then()
                .statusCode(201)
                .body("status", equalTo("ATIVA"));
    }

    @Test
    @Order(4)
    void faturarECobrarMarcaFaturaPaga() {
        given()
                .header("X-Tenant-Id", TENANT_BILLING)
                .when().post("/api/v1/billing/faturar")
                .then()
                .statusCode(200)
                .body("assinaturasProcessadas", greaterThanOrEqualTo(1));

        given()
                .header("X-Tenant-Id", TENANT_BILLING)
                .when().get("/api/v1/billing/faturas")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].status", equalTo("ABERTA"))
                .body("[0].valor", equalTo(199.00f));

        given()
                .header("X-Tenant-Id", TENANT_BILLING)
                .when().post("/api/v1/billing/cobrar")
                .then()
                .statusCode(200)
                .body("faturasProcessadas", greaterThanOrEqualTo(1));

        given()
                .header("X-Tenant-Id", TENANT_BILLING)
                .when().get("/api/v1/billing/faturas")
                .then()
                .statusCode(200)
                .body("[0].status", equalTo("PAGA"));

        given()
                .header("X-Tenant-Id", TENANT_BILLING)
                .when().get("/api/v1/billing/assinatura")
                .then()
                .statusCode(200)
                .body("status", equalTo("ATIVA"));
    }

    @Test
    @Order(5)
    void tenantSuspensoBloqueadoExcetoBilling() {
        given()
                .header("X-Tenant-Id", TENANT_SUSPENSO)
                .when().get("/api/v1/produtos")
                .then()
                .statusCode(402)
                .body("code", equalTo("TENANT_SUSPENSO"));

        // billing continua acessível para regularização (404 = sem assinatura, não 402)
        given()
                .header("X-Tenant-Id", TENANT_SUSPENSO)
                .when().get("/api/v1/billing/assinatura")
                .then()
                .statusCode(404);
    }

    // ------------------------------------------------------------------
    // Fiscal
    // ------------------------------------------------------------------

    @Test
    @Order(6)
    void configurarEmitirNfce() {
        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "lojaId", LOJA_DEMO,
                        "cnpj", "12345678000199",
                        "razaoSocial", "Loja Demo LTDA",
                        "regimeTributario", "SIMPLES_NACIONAL",
                        "ambiente", "HOMOLOGACAO",
                        "uf", "SP",
                        "habilitado", true))
                .when().post("/api/v1/fiscal/configuracao")
                .then()
                .statusCode(200)
                .body("habilitado", equalTo(true));

        String vendaId = venderUmItem();

        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .when().get("/api/v1/fiscal/vendas/" + vendaId)
                .then()
                .statusCode(200)
                .body("status", equalTo("PENDENTE"))
                .body("modelo", equalTo("NFCE"));

        String chave = given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .when().post("/api/v1/fiscal/vendas/" + vendaId + "/emitir")
                .then()
                .statusCode(200)
                .body("status", equalTo("AUTORIZADO"))
                .body("chaveAcesso", notNullValue())
                .body("protocolo", notNullValue())
                .extract().path("chaveAcesso");

        assertEquals(44, chave.length());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String venderUmItem() {
        garantirCaixaAberto();

        String vendaId = given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .header("X-User-Id", USER_CAIXA)
                .contentType(ContentType.JSON)
                .body(Map.of("lojaId", LOJA_DEMO))
                .when().post("/api/v1/vendas")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .header("X-User-Id", USER_CAIXA)
                .contentType(ContentType.JSON)
                .body(Map.of("produtoId", PRODUTO_1, "quantidade", 1))
                .when().post("/api/v1/vendas/" + vendaId + "/itens")
                .then()
                .statusCode(200);

        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .header("X-User-Id", USER_CAIXA)
                .contentType(ContentType.JSON)
                .body(Map.of("formaPagamento", "DINHEIRO", "valor", 10.00))
                .when().post("/api/v1/vendas/" + vendaId + "/pagamentos")
                .then()
                .statusCode(200);

        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .header("X-User-Id", USER_CAIXA)
                .when().post("/api/v1/vendas/" + vendaId + "/finalizar")
                .then()
                .statusCode(200)
                .body("status", equalTo("FINALIZADA"));

        return vendaId;
    }

    private void garantirCaixaAberto() {
        int status = given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .queryParam("lojaId", LOJA_DEMO)
                .queryParam("operadorId", USER_CAIXA)
                .when().get("/api/v1/caixas/aberto")
                .then()
                .extract().statusCode();

        if (status == 404) {
            given()
                    .header("X-Tenant-Id", TENANT_DEMO)
                    .header("X-User-Id", USER_CAIXA)
                    .contentType(ContentType.JSON)
                    .body(Map.of("lojaId", LOJA_DEMO, "valorAbertura", 100.00))
                    .when().post("/api/v1/caixas/abrir")
                    .then()
                    .statusCode(201);
        }
    }
}
