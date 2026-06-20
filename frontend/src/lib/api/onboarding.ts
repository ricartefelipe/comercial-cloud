import { API_BASE_URL } from "@/lib/constants";
import type { OnboardingRequest, OnboardingResult, PlanoPublico } from "@/types";

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

export async function listPlanosPublicos(): Promise<PlanoPublico[]> {
  const response = await fetch(`${API_BASE_URL}/api/v1/public/planos`, {
    headers: { "X-Correlation-Id": generateUuid() },
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Erro ao carregar planos (${response.status})`);
  }

  return (await response.json()) as PlanoPublico[];
}

export async function criarConta(data: OnboardingRequest): Promise<OnboardingResult> {
  const response = await fetch(`${API_BASE_URL}/api/v1/public/onboarding`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Correlation-Id": generateUuid(),
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    let message = `Erro ao criar conta (${response.status})`;
    try {
      const body = (await response.json()) as { message?: string };
      if (body.message) message = body.message;
    } catch {
      // mantém mensagem padrão
    }
    throw new Error(message);
  }

  return (await response.json()) as OnboardingResult;
}
