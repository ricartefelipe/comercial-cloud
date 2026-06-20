import { apiClient, buildQuery } from "@/lib/api/client";
import { getLojaId } from "@/lib/auth/tenant-context";
import type { DashboardResumo } from "@/types";

export async function getDashboardResumo(lojaId = getLojaId()): Promise<DashboardResumo> {
  return apiClient<DashboardResumo>(
    `/api/v1/dashboard/resumo${buildQuery({ lojaId })}`,
  );
}
