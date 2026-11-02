package com.comercialcloud;

import com.comercialcloud.domain.shared.DemoConstants;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@TestProfile(IdentificacaoLocatarioDemoIT.ModoDemo.class)
class IdentificacaoLocatarioDemoIT {

    public static class ModoDemo implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("comercial-cloud.tenant.resolution", "demo");
        }
    }

    @Test
    void semCabecalhoUsaOLocatarioDeDemonstracao() {
        given().when().get("/api/v1/produtos")
                .then().statusCode(200)
                .body(containsString(DemoConstants.PRODUTO_1.toString()));
    }

    @Test
    void cabecalhoDeOutroLocatarioEIgnorado() {
        given().header("X-Tenant-Id", DemoConstants.TENANT_OTHER.toString())
                .when().get("/api/v1/produtos")
                .then().statusCode(200)
                .body(containsString(DemoConstants.PRODUTO_1.toString()));
    }

    @Test
    void cabecalhoInvalidoEIgnorado() {
        given().header("X-Tenant-Id", "nao-e-uuid")
                .when().get("/api/v1/produtos")
                .then().statusCode(200);
    }

    @Test
    void usuarioInformadoPeloClienteNaoAlteraOPerfil() {
        given().header("X-User-Id", DemoConstants.USER_CAIXA.toString())
                .contentType("application/json").body("{}")
                .when().post("/api/v1/lojas")
                .then().statusCode(org.hamcrest.Matchers.not(403));
    }
}
