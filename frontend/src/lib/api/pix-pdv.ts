import { apiClient } from "@/lib/api/client";

export interface PixCobrancaPdv {
  id: string;
  vendaId: string;
  valor: number;
  status: "PENDENTE" | "PAGO" | string;
  checkoutUrl?: string;
  pixPayload?: string;
  pixEncodedImage?: string;
  gatewayPaymentId?: string;
}

export async function criarPixCobranca(
  vendaId: string,
  valor: number,
): Promise<PixCobrancaPdv> {
  return apiClient<PixCobrancaPdv>(`/api/v1/vendas/${vendaId}/pix-cobranca`, {
    method: "POST",
    body: { valor },
  });
}

export async function consultarPixCobranca(
  vendaId: string,
  cobrancaId: string,
): Promise<PixCobrancaPdv> {
  return apiClient<PixCobrancaPdv>(
    `/api/v1/vendas/${vendaId}/pix-cobranca/${cobrancaId}`,
    { includeUserId: false },
  );
}
