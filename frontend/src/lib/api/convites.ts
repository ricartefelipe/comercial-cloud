import { apiClient } from "@/lib/api/client";
import { API_BASE_URL } from "@/lib/constants";
import { ApiClientError } from "@/lib/api/client";
import type { UsuarioRequest, UsuarioRole } from "@/types";

export interface ConviteUsuario {
  id: string;
  email: string;
  nome: string;
  role: UsuarioRole;
  expiraEm: string;
  aceitoEm?: string | null;
  createdAt: string;
}

export interface ConvitePublico {
  email: string;
  nome: string;
  role: string;
  tenantNome: string;
  expiraEm: string;
}

export async function listConvitesPendentes(): Promise<ConviteUsuario[]> {
  return apiClient<ConviteUsuario[]>("/api/v1/usuarios/convites", { includeUserId: true });
}

export async function convidarUsuario(payload: UsuarioRequest): Promise<ConviteUsuario> {
  return apiClient<ConviteUsuario>("/api/v1/usuarios/convites", {
    method: "POST",
    body: payload,
  });
}

export async function consultarConvitePublico(token: string): Promise<ConvitePublico> {
  const response = await fetch(`${API_BASE_URL}/api/v1/public/convites/${token}`);
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new ApiClientError(
      (data as { message?: string }).message ?? `Erro ${response.status}`,
      response.status,
    );
  }
  return response.json();
}

export async function aceitarConvite(
  token: string,
  payload: { senha: string; nome?: string },
): Promise<{ usuarioId: string; email: string; loginUrl: string }> {
  const response = await fetch(`${API_BASE_URL}/api/v1/public/convites/${token}/aceitar`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new ApiClientError(
      (data as { message?: string }).message ?? `Erro ${response.status}`,
      response.status,
    );
  }
  return response.json();
}
