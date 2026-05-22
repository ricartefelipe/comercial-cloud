import { apiClient, buildQuery } from "@/lib/api/client";
import type { Cliente, ClienteRequest, Page } from "@/types";

export async function listClientes(page = 0, size = 20): Promise<Page<Cliente>> {
  return apiClient<Page<Cliente>>(
    `/api/v1/clientes${buildQuery({ page, size })}`,
    { includeUserId: false },
  );
}

export async function getCliente(id: string): Promise<Cliente> {
  return apiClient<Cliente>(`/api/v1/clientes/${id}`, { includeUserId: false });
}

export async function createCliente(data: ClienteRequest): Promise<Cliente> {
  return apiClient<Cliente>("/api/v1/clientes", {
    method: "POST",
    body: data,
    includeUserId: false,
  });
}

export async function updateCliente(id: string, data: ClienteRequest): Promise<Cliente> {
  return apiClient<Cliente>(`/api/v1/clientes/${id}`, {
    method: "PUT",
    body: data,
    includeUserId: false,
  });
}
