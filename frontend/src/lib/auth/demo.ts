import { API_BASE_URL } from "@/lib/constants";

interface SessaoDemonstracao {
  access_token?: string;
}

/** Troca a conta de demonstração por uma sessão do servidor. A API não responde a nenhuma rota de dados sem ela. */
export async function entrarNaDemonstracao(email: string, senha: string): Promise<string> {
  const response = await fetch(`${API_BASE_URL}/api/v1/public/demo/sessao`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, senha }),
  });
  if (response.status === 401) {
    throw new Error("E-mail ou senha inválidos.");
  }
  if (!response.ok) {
    throw new Error("A demonstração está indisponível no momento. Tente novamente em instantes.");
  }
  const data = (await response.json()) as SessaoDemonstracao;
  if (!data.access_token) {
    throw new Error("A demonstração está indisponível no momento. Tente novamente em instantes.");
  }
  return data.access_token;
}
