import { apiClient } from "@/lib/api/client";
import type { AuditoriaRegistro } from "@/types";

export async function listAuditoria(limit = 100): Promise<AuditoriaRegistro[]> {
  return apiClient<AuditoriaRegistro[]>(`/api/v1/auditoria?limit=${limit}`, {
    includeUserId: true,
  });
}
