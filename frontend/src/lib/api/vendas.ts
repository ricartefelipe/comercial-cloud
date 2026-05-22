import { apiClient, buildQuery } from "@/lib/api/client";
import { LOJA_ID } from "@/lib/constants";
import type {
  ItemVendaRequest,
  PagamentoVendaRequest,
  Venda,
  VendaRequest,
} from "@/types";

export async function listVendas(params?: {
  lojaId?: string;
  dataInicio?: string;
  dataFim?: string;
}): Promise<Venda[]> {
  return apiClient<Venda[]>(
    `/api/v1/vendas${buildQuery({
      lojaId: params?.lojaId ?? LOJA_ID,
      dataInicio: params?.dataInicio,
      dataFim: params?.dataFim,
    })}`,
  );
}

export async function getVenda(id: string): Promise<Venda> {
  return apiClient<Venda>(`/api/v1/vendas/${id}`);
}

export async function createVenda(data: VendaRequest): Promise<Venda> {
  return apiClient<Venda>("/api/v1/vendas", {
    method: "POST",
    body: data,
  });
}

export async function addItemVenda(
  vendaId: string,
  data: ItemVendaRequest,
): Promise<Venda> {
  return apiClient<Venda>(`/api/v1/vendas/${vendaId}/itens`, {
    method: "POST",
    body: data,
  });
}

export async function removeItemVenda(vendaId: string, itemId: string): Promise<Venda> {
  return apiClient<Venda>(`/api/v1/vendas/${vendaId}/itens/${itemId}`, {
    method: "DELETE",
  });
}

export async function addPagamentoVenda(
  vendaId: string,
  data: PagamentoVendaRequest,
): Promise<Venda> {
  return apiClient<Venda>(`/api/v1/vendas/${vendaId}/pagamentos`, {
    method: "POST",
    body: data,
  });
}

export async function finalizarVenda(vendaId: string): Promise<Venda> {
  return apiClient<Venda>(`/api/v1/vendas/${vendaId}/finalizar`, {
    method: "POST",
  });
}

export async function cancelarVenda(vendaId: string, motivo?: string): Promise<Venda> {
  return apiClient<Venda>(`/api/v1/vendas/${vendaId}/cancelar`, {
    method: "POST",
    body: motivo ? { motivo } : undefined,
  });
}
