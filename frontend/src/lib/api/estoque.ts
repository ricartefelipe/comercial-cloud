import { apiClient, buildQuery } from "@/lib/api/client";
import { getLojaId } from "@/lib/auth/tenant-context";
import type { AjusteEstoqueRequest, Estoque, MovimentacaoEstoque } from "@/types";

export interface MovimentoEstoqueRequest {
  lojaId: string;
  produtoId: string;
  quantidade: number;
  motivo: string;
}

export interface InventarioEstoqueRequest {
  lojaId: string;
  produtoId: string;
  quantidadeContada: number;
  motivo: string;
}

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

export async function entradaEstoque(data: MovimentoEstoqueRequest): Promise<void> {
  return apiClient<void>("/api/v1/estoques/entrada", {
    method: "POST",
    body: data,
  });
}

export async function saidaEstoque(data: MovimentoEstoqueRequest): Promise<void> {
  return apiClient<void>("/api/v1/estoques/saida", {
    method: "POST",
    body: data,
  });
}

export async function inventarioEstoque(data: InventarioEstoqueRequest): Promise<void> {
  return apiClient<void>("/api/v1/estoques/inventario", {
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
