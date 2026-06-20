import { apiClient, ApiClientError, buildQuery } from "@/lib/api/client";
import { getLojaId } from "@/lib/auth/tenant-context";
import type {
  ConfiguracaoFiscal,
  ConfiguracaoFiscalRequest,
  DocumentoFiscal,
} from "@/types";

export async function getConfiguracaoFiscal(lojaId = getLojaId()): Promise<ConfiguracaoFiscal | null> {
  try {
    return await apiClient<ConfiguracaoFiscal>(
      `/api/v1/fiscal/configuracao${buildQuery({ lojaId })}`,
      { includeUserId: false },
    );
  } catch (error) {
    if (error instanceof ApiClientError && error.status === 404) {
      return null;
    }
    throw error;
  }
}

export async function salvarConfiguracaoFiscal(
  data: ConfiguracaoFiscalRequest,
): Promise<ConfiguracaoFiscal> {
  return apiClient<ConfiguracaoFiscal>("/api/v1/fiscal/configuracao", {
    method: "POST",
    body: data,
  });
}

export async function listDocumentosFiscais(lojaId = getLojaId()): Promise<DocumentoFiscal[]> {
  return apiClient<DocumentoFiscal[]>(
    `/api/v1/fiscal/documentos${buildQuery({ lojaId })}`,
    { includeUserId: false },
  );
}

export async function getDocumentoFiscalPorVenda(vendaId: string): Promise<DocumentoFiscal | null> {
  try {
    return await apiClient<DocumentoFiscal>(`/api/v1/fiscal/vendas/${vendaId}`, {
      includeUserId: false,
    });
  } catch (error) {
    if (error instanceof ApiClientError && error.status === 404) {
      return null;
    }
    throw error;
  }
}

export async function emitirDocumentoFiscal(
  vendaId: string,
  modelo: "NFCE" | "NFE" = "NFCE",
): Promise<DocumentoFiscal> {
  return apiClient<DocumentoFiscal>(
    `/api/v1/fiscal/vendas/${vendaId}/emitir${buildQuery({ modelo })}`,
    { method: "POST", includeUserId: false },
  );
}

export async function cancelarDocumentoFiscal(
  documentoId: string,
  justificativa: string,
): Promise<DocumentoFiscal> {
  return apiClient<DocumentoFiscal>(`/api/v1/fiscal/documentos/${documentoId}/cancelar`, {
    method: "POST",
    body: { justificativa },
    includeUserId: false,
  });
}
