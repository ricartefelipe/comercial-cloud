import { apiClient, buildQuery } from "@/lib/api/client";
import type { Page, Produto, ProdutoRequest } from "@/types";

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

export async function inativarProduto(id: string): Promise<Produto> {
  return apiClient<Produto>(`/api/v1/produtos/${id}/inativar`, {
    method: "PATCH",
    includeUserId: false,
  });
}
