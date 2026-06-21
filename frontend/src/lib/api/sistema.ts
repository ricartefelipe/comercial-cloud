import { apiClient } from "@/lib/api/client";

export interface CapacidadesSistema {
  fiscalProvider: string;
  fiscalFocusHabilitado: boolean;
  billingGateway: string;
  asaasConfigurado: boolean;
  pixPdvDisponivel: boolean;
}

export async function getCapacidadesSistema(): Promise<CapacidadesSistema> {
  return apiClient<CapacidadesSistema>("/api/v1/sistema/capacidades", {
    includeUserId: false,
  });
}
