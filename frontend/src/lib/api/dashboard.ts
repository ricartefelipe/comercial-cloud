import { apiClient, buildQuery } from "@/lib/api/client";
import { LOJA_ID } from "@/lib/constants";
import type { DashboardResumo } from "@/types";

export async function getDashboardResumo(lojaId = LOJA_ID): Promise<DashboardResumo> {
  return apiClient<DashboardResumo>(
    `/api/v1/dashboard/resumo${buildQuery({ lojaId })}`,
  );
}
