package com.comercialcloud;

import com.comercialcloud.domain.shared.DemoConstants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;

/**
 * O perfil CAIXA não administra o locatário: usuários, configurações, lojas, identidade visual, faturamento e
 * processamento fiscal ficam com o ADMIN. Vale em qualquer modo de identificação; aqui, o de cabeçalho dos testes.
 */
@QuarkusTest
class AutorizacaoPorPerfilIT {

    private static RequestSpecification como(java.util.UUID usuario) {
        return given().header("X-Tenant-Id", DemoConstants.TENANT_DEMO.toString())
                .header("X-User-Id", usuario.toString())
                .contentType("application/json")
                .body("{}");
    }

    @Test
    void caixaNaoAdministraOLocatario() {
        como(DemoConstants.USER_CAIXA).when().post("/api/v1/usuarios").then().statusCode(403);
        como(DemoConstants.USER_CAIXA).when().put("/api/v1/configuracoes").then().statusCode(403);
        como(DemoConstants.USER_CAIXA).when().post("/api/v1/lojas").then().statusCode(403);
        como(DemoConstants.USER_CAIXA).when().put("/api/v1/branding").then().statusCode(403);
        como(DemoConstants.USER_CAIXA).when().post("/api/v1/billing/faturar").then().statusCode(403);
        como(DemoConstants.USER_CAIXA).when().post("/api/v1/billing/cobrar").then().statusCode(403);
        como(DemoConstants.USER_CAIXA).when().post("/api/v1/fiscal/processar").then().statusCode(403);
    }

    @Test
    void adminAdministraOLocatario() {
        como(DemoConstants.USER_ADMIN).when().post("/api/v1/lojas").then().statusCode(not(403));
        como(DemoConstants.USER_ADMIN).when().post("/api/v1/billing/faturar").then().statusCode(not(403));
        como(DemoConstants.USER_ADMIN).when().post("/api/v1/fiscal/processar").then().statusCode(not(403));
    }

    @Test
    void caixaContinuaLendoEVendendo() {
        given().header("X-Tenant-Id", DemoConstants.TENANT_DEMO.toString())
                .header("X-User-Id", DemoConstants.USER_CAIXA.toString())
                .when().get("/api/v1/produtos").then().statusCode(200);
    }
}
