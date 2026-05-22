import { apiClient, buildQuery } from "@/lib/api/client";
import { LOJA_ID } from "@/lib/constants";
import type { AjusteEstoqueRequest, Estoque, MovimentacaoEstoque } from "@/types";

export async function listEstoques(lojaId = LOJA_ID): Promise<Estoque[]> {
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
  lojaId = LOJA_ID,
): Promise<MovimentacaoEstoque[]> {
  return apiClient<MovimentacaoEstoque[]>(
    `/api/v1/estoques/movimentacoes${buildQuery({ produtoId, lojaId })}`,
    { includeUserId: false },
  );
}
