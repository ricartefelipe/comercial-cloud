import { apiClient, buildQuery } from "@/lib/api/client";
import type { Page, Produto, ProdutoRequest, ImportacaoProdutoResult } from "@/types";

export async function listProdutos(page = 0, size = 20): Promise<Page<Produto>> {
  return apiClient<Page<Produto>>(
    `/api/v1/produtos${buildQuery({ page, size })}`,
    { includeUserId: false },
  );
}

export async function buscarProdutos(termo: string): Promise<Produto[]> {
  return apiClient<Produto[]>(
    `/api/v1/produtos/busca${buildQuery({ termo })}`,
    { includeUserId: false },
  );
}

export async function getProduto(id: string): Promise<Produto> {
  return apiClient<Produto>(`/api/v1/produtos/${id}`, { includeUserId: false });
}

export async function createProduto(data: ProdutoRequest): Promise<Produto> {
  return apiClient<Produto>("/api/v1/produtos", {
    method: "POST",
    body: data,
    includeUserId: false,
  });
}

export async function updateProduto(id: string, data: ProdutoRequest): Promise<Produto> {
  return apiClient<Produto>(`/api/v1/produtos/${id}`, {
    method: "PUT",
    body: data,
    includeUserId: false,
  });
}

export async function ativarProduto(id: string): Promise<Produto> {
  return apiClient<Produto>(`/api/v1/produtos/${id}/ativar`, {
    method: "PATCH",
    includeUserId: false,
  });
}

export async function getProdutoByCodigoBarras(codigo: string): Promise<Produto> {
  return apiClient<Produto>(`/api/v1/produtos/codigo-barras/${encodeURIComponent(codigo)}`, {
    includeUserId: false,
  });
}

export async function inativarProduto(id: string): Promise<Produto> {
  return apiClient<Produto>(`/api/v1/produtos/${id}/inativar`, {
    method: "PATCH",
    includeUserId: false,
  });
}

export async function listCategoriasProduto(): Promise<string[]> {
  return apiClient<string[]>("/api/v1/produtos/categorias", { includeUserId: false });
}

export async function uploadProdutoImagem(
  id: string,
  contentType: string,
  conteudoBase64: string,
): Promise<{ imagemUrl: string }> {
  return apiClient<{ imagemUrl: string }>(`/api/v1/produtos/${id}/imagem`, {
    method: "POST",
    body: { contentType, conteudoBase64 },
    includeUserId: false,
  });
}

export async function importarProdutosCsv(csv: string): Promise<ImportacaoProdutoResult> {
  return apiClient<ImportacaoProdutoResult>("/api/v1/produtos/importacao", {
    method: "POST",
    body: { csv },
    includeUserId: false,
  });
}
