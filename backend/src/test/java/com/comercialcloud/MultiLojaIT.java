package com.comercialcloud;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class MultiLojaIT {

    private static final String TENANT = "11111111-1111-1111-1111-111111111111";
    private static final String USER = "33333333-3333-3333-3333-333333333333";

    @Test
    void premiumPermiteCriarSegundaLoja() {
        String email = "multi-" + UUID.randomUUID() + "@test.com";
        var onboarding =
                given()
                        .contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "nomeFantasia", "Loja Multi",
                                        "razaoSocial", "Multi LTDA",
                                        "cnpj", "12345678000199",
                                        "adminNome", "Admin",
                                        "adminEmail", email,
                                        "planoCodigo", "PREMIUM",
                                        "comTrial", true))
                        .when()
                        .post("/api/v1/public/onboarding")
                        .then()
                        .statusCode(201)
                        .extract();
        String tenantId = onboarding.path("tenantId");
        String adminUserId = onboarding.path("adminUserId");

        given()
                .header("X-Tenant-Id", tenantId)
                .header("X-User-Id", adminUserId)
                .when()
                .get("/api/v1/lojas/resumo")
                .then()
                .statusCode(200)
                .body("limite", equalTo(10))
                .body("podeCriar", equalTo(true));

        given()
                .header("X-Tenant-Id", tenantId)
                .header("X-User-Id", adminUserId)
                .contentType(ContentType.JSON)
                .body(Map.of("nome", "Filial Centro"))
                .when()
                .post("/api/v1/lojas")
                .then()
                .statusCode(201)
                .body("nome", equalTo("Filial Centro"));
    }

    @Test
    void essencialBloqueiaSegundaLoja() {
        given()
                .header("X-Tenant-Id", TENANT)
                .header("X-User-Id", USER)
                .when()
                .get("/api/v1/lojas/resumo")
                .then()
                .statusCode(200)
                .body("limite", equalTo(1))
                .body("ativas", greaterThanOrEqualTo(1));

        given()
                .header("X-Tenant-Id", TENANT)
                .header("X-User-Id", USER)
                .contentType(ContentType.JSON)
                .body(Map.of("nome", "Filial Proibida"))
                .when()
                .post("/api/v1/lojas")
                .then()
                .statusCode(400)
                .body("code", equalTo("LIMITE_LOJAS"));
    }

    @Test
    void adminSalvaBranding() {
        given()
                .header("X-Tenant-Id", TENANT)
                .header("X-User-Id", USER)
                .contentType(ContentType.JSON)
                .body(Map.of("nomeExibicao", "Minha Marca PDV", "corPrimaria", "#059669"))
                .when()
                .put("/api/v1/branding")
                .then()
                .statusCode(200)
                .body("nomeExibicao", equalTo("Minha Marca PDV"))
                .body("corPrimaria", equalTo("#059669"));
    }
}
