import { API_BASE_URL, TENANT_ID, USER_ID } from "@/lib/constants";
import { getToken } from "@/lib/auth/session";
import type { ApiError } from "@/types";

export class ApiClientError extends Error {
  status: number;
  code?: string;
  details?: string[];

  constructor(message: string, status: number, code?: string, details?: string[]) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

interface RequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  includeUserId?: boolean;
}

function generateUuid(): string {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

function getCorrelationId(): string {
  if (typeof window === "undefined") return generateUuid();
  let id = sessionStorage.getItem("cc_correlation_id");
  if (!id) {
    id = generateUuid();
    sessionStorage.setItem("cc_correlation_id", id);
  }
  return id;
}

function parseApiError(data: unknown, status: number): ApiClientError {
  if (data && typeof data === "object") {
    const err = data as ApiError & { code?: string };
    const message = err.message || err.error || `Erro ${status}`;
    const code = err.code;
    const details = err.details;
    return new ApiClientError(message, err.status ?? status, code, details);
  }
  return new ApiClientError(`Erro ${status}`, status);
}

export async function apiClient<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { body, includeUserId = true, headers, ...rest } = options;

  const requestHeaders: Record<string, string> = {
    "X-Tenant-Id": TENANT_ID,
    "X-Correlation-Id": getCorrelationId(),
    ...(includeUserId ? { "X-User-Id": USER_ID } : {}),
    ...(headers as Record<string, string>),
  };

  const token = getToken();
  if (token) {
    requestHeaders.Authorization = `Bearer ${token}`;
  }

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
    let code: string | undefined;
    let details: string[] | undefined;

    try {
      const errorData = await response.json();
      const parsed = parseApiError(errorData, response.status);
      errorMessage = parsed.message;
      code = parsed.code;
      details = parsed.details;
    } catch {
      try {
        errorMessage = (await response.text()) || errorMessage;
      } catch {
        // keep default message
      }
    }

    throw new ApiClientError(errorMessage, response.status, code, details);
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
