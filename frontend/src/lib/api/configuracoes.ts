import { apiClient, buildQuery } from "@/lib/api/client";
import { LOJA_ID } from "@/lib/constants";
import type { Configuracao, ConfiguracaoRequest } from "@/types";

export async function listConfiguracoes(lojaId = LOJA_ID): Promise<Configuracao[]> {
  return apiClient<Configuracao[]>(
    `/api/v1/configuracoes${buildQuery({ lojaId })}`,
    { includeUserId: false },
  );
}

export async function upsertConfiguracao(data: ConfiguracaoRequest): Promise<Configuracao> {
  return apiClient<Configuracao>("/api/v1/configuracoes", {
    method: "PUT",
    body: data,
    includeUserId: false,
  });
}

export async function upsertConfiguracoes(
  items: ConfiguracaoRequest[],
): Promise<Configuracao[]> {
  return Promise.all(items.map((item) => upsertConfiguracao(item)));
}
