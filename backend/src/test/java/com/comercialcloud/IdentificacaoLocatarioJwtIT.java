package com.comercialcloud;

import com.comercialcloud.domain.shared.DemoConstants;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestProfile(IdentificacaoLocatarioJwtIT.ModoJwt.class)
class IdentificacaoLocatarioJwtIT {

    public static class ModoJwt implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("comercial-cloud.tenant.resolution", "jwt");
        }
    }

    @Test
    void cabecalhoSemTokenNaoDaAcesso() {
        given().header("X-Tenant-Id", DemoConstants.TENANT_DEMO.toString())
                .header("X-User-Id", DemoConstants.USER_ADMIN.toString())
                .when().get("/api/v1/produtos")
                .then().statusCode(401);
    }

    @Test
    void semIdentificacaoResponde401() {
        given().when().get("/api/v1/produtos").then().statusCode(401);
    }

    static Stream<String> rotasDeDados() {
        return RotasDaApi.leitura();
    }

    @ParameterizedTest
    @MethodSource("rotasDeDados")
    void nenhumaRotaDeDadosRespondeSemToken(String rota) {
        given().when().get(rota).then().statusCode(401);
        given().header("X-Tenant-Id", DemoConstants.TENANT_DEMO.toString())
                .header("X-User-Id", DemoConstants.USER_ADMIN.toString())
                .when().get(rota).then().statusCode(401);
    }

    @Test
    void rotaDeSessaoDaDemonstracaoNaoExisteForaDaDemonstracao() {
        given().contentType("application/json")
                .body(Map.of("email", "admin@demo.com", "senha", "qualquer"))
                .when().post("/api/v1/public/demo/sessao")
                .then().statusCode(404);
    }

    @Test
    void rotasPublicasContinuamAbertas() {
        given().when().get("/q/health/ready").then().statusCode(200);
    }
}
