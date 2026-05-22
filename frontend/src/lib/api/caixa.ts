import { apiClient, buildQuery, ApiClientError } from "@/lib/api/client";
import { LOJA_ID, USER_ID } from "@/lib/constants";
import type {
  AbrirCaixaRequest,
  Caixa,
  CaixaResumo,
  FecharCaixaRequest,
} from "@/types";

export async function abrirCaixa(data: AbrirCaixaRequest): Promise<Caixa> {
  return apiClient<Caixa>("/api/v1/caixas/abrir", {
    method: "POST",
    body: data,
  });
}

export async function getCaixaAberto(
  lojaId = LOJA_ID,
  operadorId = USER_ID,
): Promise<Caixa | null> {
  try {
    return await apiClient<Caixa>(
      `/api/v1/caixas/aberto${buildQuery({ lojaId, operadorId })}`,
      { includeUserId: false },
    );
  } catch (error) {
    if (error instanceof ApiClientError && error.status === 404) {
      return null;
    }
    throw error;
  }
}

export async function fecharCaixa(
  caixaId: string,
  data: FecharCaixaRequest,
): Promise<Caixa> {
  return apiClient<Caixa>(`/api/v1/caixas/${caixaId}/fechar`, {
    method: "POST",
    body: data,
  });
}

export async function getCaixaResumo(caixaId: string): Promise<CaixaResumo> {
  return apiClient<CaixaResumo>(`/api/v1/caixas/${caixaId}/resumo`, {
    includeUserId: false,
  });
}
