import { apiClient } from "@/lib/api/client";
import type { Usuario, UsuarioRequest } from "@/types";

export async function listUsuarios(): Promise<Usuario[]> {
  return apiClient<Usuario[]>("/api/v1/usuarios", { includeUserId: false });
}

export async function getUsuario(id: string): Promise<Usuario> {
  return apiClient<Usuario>(`/api/v1/usuarios/${id}`, { includeUserId: false });
}

export async function createUsuario(data: UsuarioRequest): Promise<Usuario> {
  return apiClient<Usuario>("/api/v1/usuarios", {
    method: "POST",
    body: data,
    includeUserId: false,
  });
}

export async function updateUsuario(id: string, data: UsuarioRequest): Promise<Usuario> {
  return apiClient<Usuario>(`/api/v1/usuarios/${id}`, {
    method: "PUT",
    body: data,
    includeUserId: false,
  });
}

export async function inativarUsuario(id: string): Promise<void> {
  return apiClient<void>(`/api/v1/usuarios/${id}`, {
    method: "DELETE",
    includeUserId: false,
  });
}
