import { apiClient, buildQuery } from "@/lib/api/client";
import { getLojaId } from "@/lib/auth/tenant-context";
import type { AjusteEstoqueRequest, Estoque, MovimentacaoEstoque } from "@/types";

export async function listEstoques(lojaId = getLojaId()): Promise<Estoque[]> {
  return apiClient<Estoque[]>(
    `/api/v1/estoques${buildQuery({ lojaId })}`,
    { includeUserId: false },
  );
}

export async function ajustarEstoque(data: AjusteEstoqueRequest): Promise<void> {
  return apiClient<void>("/api/v1/estoques/ajustes", {
    method: "POST",
    body: data,
  });
}

export async function listMovimentacoes(
  produtoId: string,
  lojaId = getLojaId(),
): Promise<MovimentacaoEstoque[]> {
  return apiClient<MovimentacaoEstoque[]>(
    `/api/v1/estoques/movimentacoes${buildQuery({ produtoId, lojaId })}`,
    { includeUserId: false },
  );
}

export async function definirEstoqueMinimo(
  lojaId: string,
  produtoId: string,
  quantidadeMinima: number,
): Promise<Estoque> {
  return apiClient<Estoque>("/api/v1/estoques/minimo", {
    method: "PATCH",
    body: { lojaId, produtoId, quantidadeMinima },
  });
}
