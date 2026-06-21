import { ApiClientError, buildQuery } from "@/lib/api/client";
import { getLojaId, getTenantId, getUserId } from "@/lib/auth/tenant-context";
import { getToken } from "@/lib/auth/session";
import { API_BASE_URL } from "@/lib/constants";

async function authHeaders(): Promise<Record<string, string>> {
  const headers: Record<string, string> = {
    "X-Tenant-Id": getTenantId(),
    "X-User-Id": getUserId(),
  };
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

export async function downloadRelatorioCsv(
  tipo: "vendas" | "estoque",
  params: { lojaId?: string; dataInicio?: string; dataFim?: string },
): Promise<void> {
  const query = buildQuery({
    lojaId: params.lojaId,
    dataInicio: params.dataInicio,
    dataFim: params.dataFim,
    formato: "csv",
  });
  const path = `/api/v1/relatorios/${tipo}${query}`;
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: await authHeaders(),
  });
  if (!response.ok) {
    throw new ApiClientError(`Erro ${response.status}`, response.status);
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `${tipo}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

export interface RelatorioResumo {
  vendasFinalizadas: number;
  faturamento: number;
  skusComEstoque: number;
}

export async function getRelatorioResumo(params: {
  lojaId?: string;
  dataInicio?: string;
  dataFim?: string;
}): Promise<RelatorioResumo> {
  const query = buildQuery({
    lojaId: params.lojaId ?? getLojaId(),
    dataInicio: params.dataInicio,
    dataFim: params.dataFim,
  });
  const headers = await authHeaders();
  const response = await fetch(`${API_BASE_URL}/api/v1/relatorios/resumo${query}`, {
    headers,
  });
  if (!response.ok) {
    throw new ApiClientError(`Erro ${response.status}`, response.status);
  }
  return response.json();
}
