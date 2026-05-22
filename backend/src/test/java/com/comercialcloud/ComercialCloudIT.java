package com.comercialcloud;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ComercialCloudIT {

    static final UUID TENANT_DEMO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID TENANT_OUTRO = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    static final UUID LOJA_DEMO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID USER_CAIXA = UUID.fromString("44444444-4444-4444-4444-444444444444");
    static final UUID PRODUTO_1 = UUID.fromString("55555555-5555-5555-5555-555555555551");
    static final UUID PRODUTO_INATIVO = UUID.fromString("55555555-5555-5555-5555-555555555553");

    static UUID caixaId;

    @Test
    @Order(1)
    @DisplayName("1. cadastro produto")
    void cadastroProduto() {
        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "sku", "TEST-SKU-001",
                        "codigoBarras", "9990001112223",
                        "nome", "Produto Teste",
                        "descricao", "Descricao teste",
                        "preco", 15.99
                ))
                .when().post("/api/v1/produtos")
                .then()
                .statusCode(201)
                .body("sku", equalTo("TEST-SKU-001"))
                .body("ativo", equalTo(true));
    }

    @Test
    @Order(2)
    @DisplayName("2. produto duplicado sku same tenant")
    void produtoDuplicadoSkuMesmoTenant() {
        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "sku", "TEST-SKU-001",
                        "nome", "Outro Produto",
                        "preco", 10.00
                ))
                .when().post("/api/v1/produtos")
                .then()
                .statusCode(409)
                .body("code", equalTo("SKU_DUPLICADO"));
    }

    @Test
    @Order(3)
    @DisplayName("3. same sku different tenants OK")
    void mesmoSkuTenantDiferenteOk() {
        given()
                .header("X-Tenant-Id", TENANT_OUTRO)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "sku", "TEST-SKU-001",
                        "nome", "Produto Tenant 2",
                        "preco", 10.00
                ))
                .when().post("/api/v1/produtos")
                .then()
                .statusCode(201)
                .body("sku", equalTo("TEST-SKU-001"));
    }

    @Test
    @Order(4)
    @DisplayName("4. finalizacao venda baixa estoque")
    void finalizacaoVendaBaixaEstoque() {
        abrirCaixaAtual();

        BigDecimal estoqueAntes = obterQuantidadeEstoque(PRODUTO_1);

        String vendaId = criarVenda();
        adicionarItem(vendaId, PRODUTO_1, 2);
        adicionarPagamento(vendaId, 20.00);

        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .header("X-User-Id", USER_CAIXA)
                .when().post("/api/v1/vendas/" + vendaId + "/finalizar")
                .then()
                .statusCode(200)
                .body("status", equalTo("FINALIZADA"));

        BigDecimal estoqueDepois = obterQuantidadeEstoque(PRODUTO_1);
        assertEquals(0, estoqueDepois.compareTo(estoqueAntes.subtract(new BigDecimal("2"))));

        fecharCaixaAberto();
    }

    @Test
    @Order(5)
    @DisplayName("5. bloqueio venda sem caixa")
    void bloqueioVendaSemCaixa() {
        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .header("X-User-Id", USER_CAIXA)
                .contentType(ContentType.JSON)
                .body(Map.of("lojaId", LOJA_DEMO))
                .when().post("/api/v1/vendas")
                .then()
                .statusCode(400)
                .body("code", equalTo("CAIXA_FECHADO"));
    }

    @Test
    @Order(6)
    @DisplayName("6. bloqueio produto inativo")
    void bloqueioProdutoInativo() {
        abrirCaixaSeNecessario();

        String vendaId = criarVenda();

        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .header("X-User-Id", USER_CAIXA)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "produtoId", PRODUTO_INATIVO,
                        "quantidade", 1
                ))
                .when().post("/api/v1/vendas/" + vendaId + "/itens")
                .then()
                .statusCode(400)
                .body("code", equalTo("PRODUTO_INATIVO"));
    }

    @Test
    @Order(7)
    @DisplayName("7. bloqueio estoque negativo")
    void bloqueioEstoqueNegativo() {
        abrirCaixaSeNecessario();

        String vendaId = criarVenda();
        adicionarItem(vendaId, PRODUTO_1, 99999);
        adicionarPagamento(vendaId, 999990.00);

        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .header("X-User-Id", USER_CAIXA)
                .when().post("/api/v1/vendas/" + vendaId + "/finalizar")
                .then()
                .statusCode(400)
                .body("code", equalTo("ESTOQUE_INSUFICIENTE"));
    }

    @Test
    @Order(8)
    @DisplayName("8. cancelamento estorna estoque")
    void cancelamentoEstornaEstoque() {
        abrirCaixaSeNecessario();

        BigDecimal estoqueAntes = obterQuantidadeEstoque(PRODUTO_1);

        String vendaId = criarVenda();
        adicionarItem(vendaId, PRODUTO_1, 3);
        adicionarPagamento(vendaId, 30.00);

        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .header("X-User-Id", USER_CAIXA)
                .when().post("/api/v1/vendas/" + vendaId + "/finalizar")
                .then()
                .statusCode(200);

        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .header("X-User-Id", USER_CAIXA)
                .when().post("/api/v1/vendas/" + vendaId + "/cancelar")
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELADA"));

        BigDecimal estoqueDepois = obterQuantidadeEstoque(PRODUTO_1);
        assertEquals(0, estoqueDepois.compareTo(estoqueAntes));
    }

    @Test
    @Order(9)
    @DisplayName("9. fechamento caixa calculando totais")
    void fechamentoCaixaTotais() {
        fecharCaixaAberto();
        UUID novoCaixaId = abrirCaixaAtual();

        String vendaId = criarVenda();
        adicionarItem(vendaId, PRODUTO_1, 1);
        adicionarPagamento(vendaId, 10.00);

        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .header("X-User-Id", USER_CAIXA)
                .when().post("/api/v1/vendas/" + vendaId + "/finalizar")
                .then()
                .statusCode(200);

        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .header("X-User-Id", USER_CAIXA)
                .contentType(ContentType.JSON)
                .body(Map.of("valorFechamentoInformado", 110.00))
                .when().post("/api/v1/caixas/" + novoCaixaId + "/fechar")
                .then()
                .statusCode(200)
                .body("status", equalTo("FECHADO"));

        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .when().get("/api/v1/caixas/" + novoCaixaId + "/resumo")
                .then()
                .statusCode(200)
                .body("totalVendas", equalTo(10.0f))
                .body("quantidadeVendas", equalTo(1));
    }

    private UUID abrirCaixaAtual() {
        caixaId = UUID.fromString(
                given()
                        .header("X-Tenant-Id", TENANT_DEMO)
                        .header("X-User-Id", USER_CAIXA)
                        .contentType(ContentType.JSON)
                        .body(Map.of(
                                "lojaId", LOJA_DEMO,
                                "valorAbertura", 100.00
                        ))
                        .when().post("/api/v1/caixas/abrir")
                        .then()
                        .statusCode(201)
                        .extract().path("id")
        );
        return caixaId;
    }

    private void abrirCaixaSeNecessario() {
        int status = given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .queryParam("lojaId", LOJA_DEMO)
                .queryParam("operadorId", USER_CAIXA)
                .when().get("/api/v1/caixas/aberto")
                .then()
                .extract().statusCode();

        if (status == 404) {
            abrirCaixaAtual();
        }
    }

    private String criarVenda() {
        return given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .header("X-User-Id", USER_CAIXA)
                .contentType(ContentType.JSON)
                .body(Map.of("lojaId", LOJA_DEMO))
                .when().post("/api/v1/vendas")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private void adicionarItem(String vendaId, UUID produtoId, int quantidade) {
        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .header("X-User-Id", USER_CAIXA)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "produtoId", produtoId,
                        "quantidade", quantidade
                ))
                .when().post("/api/v1/vendas/" + vendaId + "/itens")
                .then()
                .statusCode(200);
    }

    private void adicionarPagamento(String vendaId, double valor) {
        given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .header("X-User-Id", USER_CAIXA)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "formaPagamento", "DINHEIRO",
                        "valor", valor
                ))
                .when().post("/api/v1/vendas/" + vendaId + "/pagamentos")
                .then()
                .statusCode(200);
    }

    private BigDecimal obterQuantidadeEstoque(UUID produtoId) {
        List<Map<String, Object>> estoques = given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .queryParam("lojaId", LOJA_DEMO)
                .when().get("/api/v1/estoques")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("$");

        return estoques.stream()
                .filter(e -> produtoId.toString().equals(String.valueOf(e.get("produtoId"))))
                .map(e -> new BigDecimal(String.valueOf(e.get("quantidadeAtual"))))
                .findFirst()
                .orElseThrow();
    }

    private void fecharCaixaAberto() {
        var response = given()
                .header("X-Tenant-Id", TENANT_DEMO)
                .queryParam("lojaId", LOJA_DEMO)
                .queryParam("operadorId", USER_CAIXA)
                .when().get("/api/v1/caixas/aberto");

        if (response.statusCode() == 200) {
            String id = response.then().extract().path("id");
            given()
                    .header("X-Tenant-Id", TENANT_DEMO)
                    .header("X-User-Id", USER_CAIXA)
                    .contentType(ContentType.JSON)
                    .body(Map.of("valorFechamentoInformado", 100.00))
                    .when().post("/api/v1/caixas/" + id + "/fechar")
                    .then()
                    .statusCode(200);
        }
    }
}
