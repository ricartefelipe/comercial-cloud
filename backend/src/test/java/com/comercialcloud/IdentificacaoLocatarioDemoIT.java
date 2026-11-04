package com.comercialcloud;

import com.comercialcloud.domain.shared.DemoConstants;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * Modo de demonstração: a API não responde a ninguém sem a sessão que o servidor emite à conta de demonstração.
 * Antes, todas as rotas respondiam ao visitante anônimo como o administrador do locatário fictício.
 */
@QuarkusTest
@TestProfile(IdentificacaoLocatarioDemoIT.ModoDemo.class)
class IdentificacaoLocatarioDemoIT {

    static final String SEGREDO = "segredo-de-teste-da-demonstracao-com-mais-de-32";
    static final String SENHA = "senha-publica-de-teste";
    private static final String HS256 = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    public static class ModoDemo implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "comercial-cloud.tenant.resolution", "demo",
                    "comercial-cloud.demo.session-secret", SEGREDO,
                    "comercial-cloud.demo.senha", SENHA);
        }
    }

    static Stream<String> rotasDeDados() {
        return RotasDaApi.leitura();
    }

    @ParameterizedTest
    @MethodSource("rotasDeDados")
    void semSessaoResponde401(String rota) {
        given().when().get(rota).then().statusCode(401);
    }

    @ParameterizedTest
    @MethodSource("rotasDeDados")
    void cabecalhosDeLocatarioEUsuarioNaoSubstituemASessao(String rota) {
        given().header("X-Tenant-Id", DemoConstants.TENANT_DEMO.toString())
                .header("X-User-Id", DemoConstants.USER_ADMIN.toString())
                .when().get(rota)
                .then().statusCode(401);
    }

    @Test
    void escritaSemSessaoResponde401() {
        given().contentType("application/json").body("{}").when().post("/api/v1/lojas").then().statusCode(401);
        given().contentType("application/json").body("{}").when().put("/api/v1/configuracoes").then().statusCode(401);
        given().contentType("application/json").body("{}").when().post("/api/v1/usuarios").then().statusCode(401);
    }

    @Test
    void senhaErradaNaoAbreSessao() {
        given().contentType("application/json")
                .body(Map.of("email", "admin@demo.com", "senha", "errada"))
                .when().post("/api/v1/public/demo/sessao")
                .then().statusCode(401);
    }

    @Test
    void emailForaDoLocatarioDeDemonstracaoNaoAbreSessao() {
        given().contentType("application/json")
                .body(Map.of("email", "outro@exemplo.com", "senha", SENHA))
                .when().post("/api/v1/public/demo/sessao")
                .then().statusCode(401);
    }

    @Test
    void contaDeDemonstracaoVeOsDadosDoLocatarioFicticio() {
        String token = entrar("admin@demo.com");
        comSessao(token).when().get("/api/v1/produtos")
                .then().statusCode(200)
                .body(containsString(DemoConstants.PRODUTO_1.toString()));
        comSessao(token).when().get("/api/v1/usuarios").then().statusCode(200);
        comSessao(token).when().get("/api/v1/auditoria").then().statusCode(200);
        comSessao(token).when().get("/api/v1/configuracoes").then().statusCode(200);
    }

    @Test
    void emailComMaiusculasEEspacosEntra() {
        given().contentType("application/json")
                .body(Map.of("email", "  Admin@Demo.com ", "senha", SENHA))
                .when().post("/api/v1/public/demo/sessao")
                .then().statusCode(200);
    }

    @Test
    void cabecalhoDeOutroLocatarioEIgnoradoComSessao() {
        comSessao(entrar("admin@demo.com"))
                .header("X-Tenant-Id", DemoConstants.TENANT_OTHER.toString())
                .when().get("/api/v1/produtos")
                .then().statusCode(200)
                .body(containsString(DemoConstants.PRODUTO_1.toString()));
    }

    @Test
    void perfilVemDaSessaoENaoDoCabecalho() {
        comSessao(entrar("caixa@demo.com"))
                .header("X-User-Id", DemoConstants.USER_ADMIN.toString())
                .contentType("application/json").body("{}")
                .when().post("/api/v1/lojas")
                .then().statusCode(403);
        comSessao(entrar("admin@demo.com"))
                .header("X-User-Id", DemoConstants.USER_CAIXA.toString())
                .contentType("application/json").body("{}")
                .when().post("/api/v1/lojas")
                .then().statusCode(not(403));
    }

    @Test
    void assinaturaAlteradaResponde401() {
        String token = entrar("admin@demo.com");
        String[] partes = token.split("\\.");
        String outraAssinatura = assinatura(partes[0] + "." + partes[1], "segredo-diferente-com-mais-de-32-caracteres");
        comSessao(partes[0] + "." + partes[1] + "." + outraAssinatura)
                .when().get("/api/v1/produtos").then().statusCode(401);
    }

    @Test
    void corpoAlteradoResponde401() {
        String[] partes = entrar("caixa@demo.com").split("\\.");
        String corpoDeAdmin = b64(claims(DemoConstants.TENANT_DEMO.toString(), Instant.now().plusSeconds(600)));
        comSessao(partes[0] + "." + corpoDeAdmin + "." + partes[2])
                .when().get("/api/v1/produtos").then().statusCode(401);
    }

    @Test
    void algoritmoNoneResponde401() {
        String corpo = b64(claims(DemoConstants.TENANT_DEMO.toString(), Instant.now().plusSeconds(600)));
        String token = b64("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + corpo + ".";
        comSessao(token).when().get("/api/v1/produtos").then().statusCode(401);
    }

    @Test
    void sessaoVencidaResponde401() {
        String token = token(claims(DemoConstants.TENANT_DEMO.toString(), Instant.now().minusSeconds(5)), SEGREDO);
        comSessao(token).when().get("/api/v1/produtos").then().statusCode(401);
    }

    @Test
    void sessaoDeOutroLocatarioResponde401() {
        String token = token(claims(DemoConstants.TENANT_OTHER.toString(), Instant.now().plusSeconds(600)), SEGREDO);
        comSessao(token).when().get("/api/v1/produtos").then().statusCode(401);
    }

    @Test
    void sessaoAssinadaPeloSegredoDoServidorEAceita() {
        String token = token(claims(DemoConstants.TENANT_DEMO.toString(), Instant.now().plusSeconds(600)), SEGREDO);
        comSessao(token).when().get("/api/v1/produtos").then().statusCode(200);
    }

    @Test
    void rotasPublicasContinuamAbertas() {
        given().when().get("/q/health/ready").then().statusCode(200);
        given().when().get("/api/v1/public/planos").then().statusCode(200);
    }

    /** Como o painel da demonstração envia a sessão. */
    static io.restassured.specification.RequestSpecification comSessao(String token) {
        return given().header("X-Sessao-Demo", token);
    }

    @Test
    void sessaoEmAuthorizationNaoServe() {
        given().header("Authorization", "Bearer " + entrar("admin@demo.com"))
                .when().get("/api/v1/produtos").then().statusCode(401);
    }

    static String entrar(String email) {
        return given().contentType("application/json")
                .body(Map.of("email", email, "senha", SENHA))
                .when().post("/api/v1/public/demo/sessao")
                .then().statusCode(200)
                .extract().path("access_token");
    }

    private static String claims(String tenantId, Instant exp) {
        return "{\"iss\":\"comercialcloud-demo\",\"sub\":\"" + DemoConstants.USER_ADMIN + "\",\"app_user_id\":\""
                + DemoConstants.USER_ADMIN + "\",\"tenant_id\":\"" + tenantId + "\",\"exp\":" + exp.getEpochSecond()
                + "}";
    }

    private static String token(String corpo, String segredo) {
        String conteudo = b64(HS256) + "." + b64(corpo);
        return conteudo + "." + assinatura(conteudo, segredo);
    }

    private static String assinatura(String conteudo, String segredo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String b64(String valor) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(valor.getBytes(StandardCharsets.UTF_8));
    }
}
