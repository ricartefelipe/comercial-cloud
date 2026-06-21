package com.comercialcloud;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ConviteRelatorioIT {

    private static final String TENANT = "11111111-1111-1111-1111-111111111111";
    private static final String USER = "33333333-3333-3333-3333-333333333333";
    private static final String LOJA = "22222222-2222-2222-2222-222222222222";

    @Test
    void conviteCriarConsultarEAceitar() {
        String email = "convite-" + UUID.randomUUID() + "@test.com";

        String token =
                given()
                        .header("X-Tenant-Id", TENANT)
                        .header("X-User-Id", USER)
                        .contentType(ContentType.JSON)
                        .body(Map.of("nome", "Convidado Teste", "email", email, "role", "CAIXA"))
                        .when()
                        .post("/api/v1/usuarios/convites")
                        .then()
                        .statusCode(201)
                        .body("email", equalTo(email))
                        .body("token", notNullValue())
                        .extract()
                        .path("token");

        given()
                .when()
                .get("/api/v1/public/convites/" + token)
                .then()
                .statusCode(200)
                .body("email", equalTo(email))
                .body("tenantNome", notNullValue());

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("senha", "senha-segura-123", "nome", "Convidado Aceito"))
                .when()
                .post("/api/v1/public/convites/" + token + "/aceitar")
                .then()
                .statusCode(200)
                .body("email", equalTo(email))
                .body("usuarioId", notNullValue());

        given()
                .when()
                .get("/api/v1/public/convites/" + token)
                .then()
                .statusCode(400);
    }

    @Test
    void relatorioResumoEVendasCsv() {
        given()
                .header("X-Tenant-Id", TENANT)
                .header("X-User-Id", USER)
                .queryParam("lojaId", LOJA)
                .when()
                .get("/api/v1/relatorios/resumo")
                .then()
                .statusCode(200)
                .body("vendasFinalizadas", notNullValue())
                .body("faturamento", notNullValue());

        given()
                .header("X-Tenant-Id", TENANT)
                .header("X-User-Id", USER)
                .queryParam("lojaId", LOJA)
                .queryParam("formato", "csv")
                .when()
                .get("/api/v1/relatorios/estoque")
                .then()
                .statusCode(200)
                .contentType(containsString("text/csv"))
                .body(containsString("produto_id"));
    }
}
