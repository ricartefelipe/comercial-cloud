package com.comercialcloud;

import java.util.stream.Stream;

/** Uma rota de leitura de cada grupo de dados da API, para provar que nenhuma responde sem identificação. */
final class RotasDaApi {

    private static final String ID = "55555555-5555-5555-5555-555555555551";

    private RotasDaApi() {}

    static Stream<String> leitura() {
        return Stream.of(
                "/api/v1/auditoria",
                "/api/v1/billing/assinatura",
                "/api/v1/billing/faturas",
                "/api/v1/billing/planos",
                "/api/v1/branding",
                "/api/v1/caixas/aberto",
                "/api/v1/caixas/historico",
                "/api/v1/clientes",
                "/api/v1/clientes/" + ID,
                "/api/v1/configuracoes",
                "/api/v1/dashboard/resumo",
                "/api/v1/estoques",
                "/api/v1/estoques/movimentacoes",
                "/api/v1/financeiro/contas-receber",
                "/api/v1/fiscal/configuracao",
                "/api/v1/fiscal/documentos",
                "/api/v1/lojas",
                "/api/v1/lojas/resumo",
                "/api/v1/produtos",
                "/api/v1/produtos/" + ID,
                "/api/v1/produtos/busca?q=a",
                "/api/v1/produtos/categorias",
                "/api/v1/relatorios/resumo",
                "/api/v1/relatorios/vendas",
                "/api/v1/sessao",
                "/api/v1/sistema/capacidades",
                "/api/v1/usuarios",
                "/api/v1/usuarios/convites",
                "/api/v1/vendas",
                "/api/v1/vendas/" + ID);
    }
}
