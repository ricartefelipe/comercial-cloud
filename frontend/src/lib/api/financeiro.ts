import { apiClient, buildQuery } from "@/lib/api/client";
import { LOJA_ID } from "@/lib/constants";
import type { ContaReceber } from "@/types";

export async function listContasReceber(lojaId = LOJA_ID): Promise<ContaReceber[]> {
  return apiClient<ContaReceber[]>(
    `/api/v1/financeiro/contas-receber${buildQuery({ lojaId })}`,
    { includeUserId: false },
  );
}

export async function marcarContaPago(id: string): Promise<ContaReceber> {
  return apiClient<ContaReceber>(`/api/v1/financeiro/contas-receber/${id}/pagar`, {
    method: "PATCH",
    includeUserId: false,
  });
}
