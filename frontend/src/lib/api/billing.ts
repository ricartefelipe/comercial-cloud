import { apiClient, ApiClientError } from "@/lib/api/client";
import type {
  AssinaturaTenant,
  FaturaAssinatura,
  PlanoAssinatura,
} from "@/types";

export async function listPlanos(): Promise<PlanoAssinatura[]> {
  return apiClient<PlanoAssinatura[]>("/api/v1/billing/planos");
}

export async function getAssinatura(): Promise<AssinaturaTenant | null> {
  try {
    return await apiClient<AssinaturaTenant>("/api/v1/billing/assinatura");
  } catch (error) {
    if (error instanceof ApiClientError && error.status === 404) {
      return null;
    }
    throw error;
  }
}

export async function assinarPlano(planoCodigo: string, comTrial = true): Promise<AssinaturaTenant> {
  return apiClient<AssinaturaTenant>("/api/v1/billing/assinar", {
    method: "POST",
    body: { planoCodigo, comTrial },
  });
}

export async function trocarPlano(planoCodigo: string): Promise<AssinaturaTenant> {
  return apiClient<AssinaturaTenant>("/api/v1/billing/assinatura/trocar-plano", {
    method: "POST",
    body: { planoCodigo },
  });
}

export async function cancelarAssinatura(): Promise<AssinaturaTenant> {
  return apiClient<AssinaturaTenant>("/api/v1/billing/assinatura/cancelar", {
    method: "POST",
  });
}

export async function reativarAssinatura(): Promise<AssinaturaTenant> {
  return apiClient<AssinaturaTenant>("/api/v1/billing/assinatura/reativar", {
    method: "POST",
  });
}

export async function listFaturas(): Promise<FaturaAssinatura[]> {
  return apiClient<FaturaAssinatura[]>("/api/v1/billing/faturas");
}

export async function pagarFatura(faturaId: string): Promise<FaturaAssinatura> {
  return apiClient<FaturaAssinatura>(`/api/v1/billing/faturas/${faturaId}/pagar`, {
    method: "POST",
  });
}
