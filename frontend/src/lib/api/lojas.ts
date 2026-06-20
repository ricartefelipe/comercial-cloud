import { apiClient } from "@/lib/api/client";
import type { Loja, LojaResumo } from "@/types";

export async function listLojas(): Promise<Loja[]> {
  return apiClient<Loja[]>("/api/v1/lojas", { includeUserId: false });
}

export async function getLojasResumo(): Promise<LojaResumo> {
  return apiClient<LojaResumo>("/api/v1/lojas/resumo", { includeUserId: false });
}

export async function criarLoja(nome: string): Promise<Loja> {
  return apiClient<Loja>("/api/v1/lojas", {
    method: "POST",
    body: { nome },
    includeUserId: false,
  });
}

export async function atualizarLoja(
  id: string,
  data: { nome?: string; ativa?: boolean },
): Promise<Loja> {
  return apiClient<Loja>(`/api/v1/lojas/${id}`, {
    method: "PUT",
    body: data,
    includeUserId: false,
  });
}
