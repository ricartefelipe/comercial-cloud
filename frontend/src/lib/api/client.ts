import { API_BASE_URL, TENANT_ID, USER_ID } from "@/lib/constants";
import type { ApiError } from "@/types";

export class ApiClientError extends Error {
  status: number;
  details?: string[];

  constructor(message: string, status: number, details?: string[]) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
    this.details = details;
  }
}

interface RequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  includeUserId?: boolean;
}

export async function apiClient<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { body, includeUserId = true, headers, ...rest } = options;

  const requestHeaders: Record<string, string> = {
    "X-Tenant-Id": TENANT_ID,
    ...(includeUserId ? { "X-User-Id": USER_ID } : {}),
    ...(headers as Record<string, string>),
  };

  if (body !== undefined) {
    requestHeaders["Content-Type"] = "application/json";
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    headers: requestHeaders,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    let errorMessage = `Erro ${response.status}`;
    let details: string[] | undefined;

    try {
      const errorData = (await response.json()) as ApiError;
      errorMessage = errorData.message || errorMessage;
      details = errorData.details;
    } catch {
      errorMessage = (await response.text()) || errorMessage;
    }

    throw new ApiClientError(errorMessage, response.status, details);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get("content-type");
  if (contentType?.includes("application/json")) {
    return (await response.json()) as T;
  }

  return undefined as T;
}

export function buildQuery(params: Record<string, string | number | undefined>): string {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      searchParams.set(key, String(value));
    }
  });
  const query = searchParams.toString();
  return query ? `?${query}` : "";
}
