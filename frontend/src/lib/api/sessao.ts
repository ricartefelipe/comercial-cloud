import { apiClient } from "@/lib/api/client";
import type { UsuarioRole } from "@/types";

export interface SessaoContexto {
  tenantId: string;
  userId?: string;
  nome?: string;
  email?: string;
  role?: UsuarioRole;
}

export async function getSessaoContexto(): Promise<SessaoContexto> {
  return apiClient<SessaoContexto>("/api/v1/sessao", { includeUserId: true });
}
