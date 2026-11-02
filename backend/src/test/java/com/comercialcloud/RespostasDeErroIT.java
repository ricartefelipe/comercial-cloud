package com.comercialcloud;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class RespostasDeErroIT {

    private static final String TENANT_DEMO = "11111111-1111-1111-1111-111111111111";

    @Test
    void rotaInexistenteResponde404EmJson() {
        given().header("X-Tenant-Id", TENANT_DEMO)
                .when().get("/api/v1/rota-que-nao-existe")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("code", equalTo("NOT_FOUND"))
                .body("message", equalTo("Recurso não encontrado"));
    }

    @Test
    void metodoNaoPermitidoResponde405EmJson() {
        given().header("X-Tenant-Id", TENANT_DEMO)
                .when().delete("/api/v1/produtos")
                .then()
                .statusCode(405)
                .contentType(ContentType.JSON)
                .body("code", equalTo("METHOD_NOT_ALLOWED"));
    }

    @Test
    void jsonInvalidoNaoRespondeErroInterno() {
        given().header("X-Tenant-Id", TENANT_DEMO)
                .contentType(ContentType.JSON)
                .body("{invalido")
                .when().post("/api/v1/produtos")
                .then()
                .statusCode(400);
    }

    @Test
    void recursoDeNegocioInexistenteMantemCodigoProprio() {
        given().header("X-Tenant-Id", TENANT_DEMO)
                .when().get("/api/v1/produtos/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("code", notNullValue());
    }
}
