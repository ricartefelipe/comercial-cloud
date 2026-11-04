package com.comercialcloud;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Com gateway real, a fatura da assinatura só fica paga pela confirmação do gateway (webhook). O locatário não pode
 * marcar a própria fatura como paga: isso reativaria a conta sem pagamento.
 */
@QuarkusTest
@TestProfile(PagamentoManualComGatewayRealIT.GatewayReal.class)
class PagamentoManualComGatewayRealIT {

    public static class GatewayReal implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("comercial-cloud.billing.gateway", "asaas");
        }
    }

    static final UUID TENANT = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    static final UUID ADMIN = UUID.fromString("ffffffff-0000-0000-0000-000000000001");

    private static RequestSpecification comoAdmin() {
        return given().header("X-Tenant-Id", TENANT.toString()).header("X-User-Id", ADMIN.toString());
    }

    @Test
    void locatarioNaoMarcaAPropriaFaturaComoPaga() {
        comoAdmin().contentType(ContentType.JSON)
                .body(Map.of("planoCodigo", "PRO", "comTrial", false))
                .when().post("/api/v1/billing/assinar")
                .then().statusCode(201);
        comoAdmin().when().post("/api/v1/billing/faturar").then().statusCode(200);
        String fatura = comoAdmin().when().get("/api/v1/billing/faturas")
                .then().statusCode(200).extract().path("[0].id");

        comoAdmin().when().post("/api/v1/billing/faturas/" + fatura + "/pagar")
                .then().statusCode(409).body("code", equalTo("PAGAMENTO_MANUAL_INDISPONIVEL"));

        comoAdmin().when().get("/api/v1/billing/faturas")
                .then().statusCode(200).body("[0].status", equalTo("ABERTA"));
    }
}
