package com.comercialcloud;

import com.comercialcloud.application.billing.BillingService;
import com.comercialcloud.application.fiscal.FiscalService;
import com.comercialcloud.infrastructure.persistence.entity.DocumentoFiscalEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Faturar, cobrar e processar documentos fiscais pela API afetam só o locatário de quem chama. Percorrer todos os
 * locatários é papel exclusivo dos agendadores internos ({@code BillingScheduler} e {@code FiscalEmissaoScheduler}),
 * que usam os métodos sem locatário do serviço.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProcessamentoEntreLocatariosIT {

    static final UUID TENANT_A = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    static final UUID ADMIN_A = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
    static final UUID TENANT_B = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    static final UUID ADMIN_B = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001");
    static final UUID DOCUMENTO_A = UUID.fromString("dddddddd-f000-0000-0000-000000000001");
    static final UUID DOCUMENTO_B = UUID.fromString("eeeeeeee-f000-0000-0000-000000000001");

    @Inject BillingService billingService;
    @Inject FiscalService fiscalService;

    private static RequestSpecification como(UUID tenant, UUID usuario) {
        return given().header("X-Tenant-Id", tenant.toString()).header("X-User-Id", usuario.toString());
    }

    @Test
    @Order(1)
    void osDoisLocatariosAssinam() {
        for (UUID[] t : new UUID[][] {{TENANT_A, ADMIN_A}, {TENANT_B, ADMIN_B}}) {
            como(t[0], t[1])
                    .contentType(ContentType.JSON)
                    .body(Map.of("planoCodigo", "PRO", "comTrial", false))
                    .when().post("/api/v1/billing/assinar")
                    .then().statusCode(201).body("status", equalTo("ATIVA"));
        }
    }

    @Test
    @Order(2)
    void faturarPeloAdminDeAFaturaSoA() {
        como(TENANT_A, ADMIN_A).when().post("/api/v1/billing/faturar")
                .then().statusCode(200).body("assinaturasProcessadas", equalTo(1));

        como(TENANT_A, ADMIN_A).when().get("/api/v1/billing/faturas").then().statusCode(200).body("$", hasSize(1));
        como(TENANT_B, ADMIN_B).when().get("/api/v1/billing/faturas").then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    @Order(3)
    void cobrarPeloAdminDeACobraSoA() {
        como(TENANT_A, ADMIN_A).when().post("/api/v1/billing/cobrar")
                .then().statusCode(200).body("faturasProcessadas", equalTo(1));

        como(TENANT_A, ADMIN_A).when().get("/api/v1/billing/faturas")
                .then().statusCode(200).body("[0].status", equalTo("PAGA"));
        como(TENANT_B, ADMIN_B).when().get("/api/v1/billing/faturas").then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    @Order(4)
    void repetirNaoGeraNemCobraDeNovo() {
        como(TENANT_A, ADMIN_A).when().post("/api/v1/billing/faturar")
                .then().statusCode(200).body("assinaturasProcessadas", equalTo(0));
        como(TENANT_A, ADMIN_A).when().post("/api/v1/billing/cobrar")
                .then().statusCode(200).body("faturasProcessadas", equalTo(0));
        como(TENANT_A, ADMIN_A).when().get("/api/v1/billing/faturas").then().statusCode(200).body("$", hasSize(1));
    }

    @Test
    @Order(5)
    void oAgendadorFaturaTodosOsLocatarios() {
        // O BillingScheduler percorre exatamente estes métodos.
        var assinaturas = billingService.idsAssinaturasParaFaturar();
        assertTrue(assinaturas.size() >= 1, "a assinatura de B precisa estar entre as do agendador");
        assinaturas.forEach(billingService::gerarFaturaAssinatura);

        como(TENANT_B, ADMIN_B).when().get("/api/v1/billing/faturas")
                .then().statusCode(200).body("$", hasSize(1)).body("[0].status", equalTo("ABERTA"));
        // A já foi faturada nesta competência: o agendador não duplica.
        como(TENANT_A, ADMIN_A).when().get("/api/v1/billing/faturas").then().statusCode(200).body("$", hasSize(1));
    }

    @Test
    @Order(6)
    void comGatewayDeSimulacaoOLocatarioPagaAPropriaFatura() {
        String faturaB = como(TENANT_B, ADMIN_B).when().get("/api/v1/billing/faturas")
                .then().statusCode(200).extract().path("[0].id");
        como(TENANT_B, ADMIN_B).when().post("/api/v1/billing/faturas/" + faturaB + "/pagar")
                .then().statusCode(200).body("status", equalTo("PAGA"));
    }

    @Test
    @Order(7)
    void processarFiscalPeloAdminDeAProcessaSoA() {
        como(TENANT_A, ADMIN_A).when().post("/api/v1/fiscal/processar")
                .then().statusCode(200).body("pendentes", equalTo(1));

        DocumentoFiscalEntity b = QuarkusTransaction.requiringNew().call(() -> DocumentoFiscalEntity.findById(DOCUMENTO_B));
        assertEquals("PENDENTE", b.status);
        assertEquals(0, b.tentativas);

        DocumentoFiscalEntity a = QuarkusTransaction.requiringNew().call(() -> DocumentoFiscalEntity.findById(DOCUMENTO_A));
        assertTrue(!"PENDENTE".equals(a.status) || a.tentativas > 0, "o documento de A precisa ter sido processado");
    }

    @Test
    @Order(8)
    void oAgendadorFiscalVeOsDocumentosDeTodos() {
        assertTrue(fiscalService.idsPendentes().contains(DOCUMENTO_B));
    }
}
