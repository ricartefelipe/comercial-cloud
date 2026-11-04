"use client";

import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ErrorMessage } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { entrarNaDemonstracao } from "@/lib/auth/demo";
import { enableDemoMode, saveToken } from "@/lib/auth/session";
import { applyJwtWorkspaceClaims } from "@/lib/auth/tenant-context";
import {
  AUTH_MODE,
  DEMO_EMAIL,
  DEMO_SENHA,
  KEYCLOAK_CLIENT_ID,
  KEYCLOAK_REALM,
  KEYCLOAK_URL,
} from "@/lib/constants";
import { Store } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { FormEvent, useState } from "react";

interface TokenResponse {
  access_token?: string;
  error?: string;
  error_description?: string;
}

export default function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const redirectTo = searchParams.get("redirect") ?? "/dashboard";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const demonstracao = AUTH_MODE === "demo";

  const entrar = async (emailInformado: string, senhaInformada: string) => {
    setLoading(true);
    setError("");

    try {
      if (demonstracao) {
        const token = await entrarNaDemonstracao(emailInformado.trim(), senhaInformada);
        saveToken(token);
        applyJwtWorkspaceClaims(token);
        router.push(redirectTo);
        return;
      }

      const tokenUrl = `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token`;
      const body = new URLSearchParams({
        grant_type: "password",
        client_id: KEYCLOAK_CLIENT_ID,
        username: emailInformado,
        password: senhaInformada,
      });

      const response = await fetch(tokenUrl, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: body.toString(),
      });

      const data = (await response.json()) as TokenResponse;

      if (!response.ok || !data.access_token) {
        throw new Error(data.error_description ?? data.error ?? "Falha na autenticação");
      }

      saveToken(data.access_token);
      applyJwtWorkspaceClaims(data.access_token);
      router.push(redirectTo);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    void entrar(email, password);
  };

  /** Demonstração: preenche a conta pública e entra por ela, pelo mesmo caminho do formulário. */
  const handleContaDemonstracao = () => {
    setEmail(DEMO_EMAIL);
    setPassword(DEMO_SENHA);
    void entrar(DEMO_EMAIL, DEMO_SENHA);
  };

  /** Só no desenvolvimento local por cabeçalho, sem Keycloak. */
  const handleDemoLogin = () => {
    enableDemoMode();
    router.push(redirectTo);
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 px-4">
      <Card className="w-full max-w-md">
        <div className="mb-6 flex flex-col items-center text-center">
          <div className="mb-3 rounded-xl bg-brand-50 p-3">
            <Store className="h-8 w-8 text-brand-600" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900">ComercialCloud</h1>
          <p className="mt-1 text-sm text-slate-500">
            {demonstracao
              ? "Demonstração: loja fictícia, dados apagados todos os dias"
              : "Entre com sua conta da retaguarda (Keycloak)"}
          </p>
        </div>

        {demonstracao && (
          <div className="mb-4 rounded-lg border border-brand-100 bg-brand-50 p-3 text-sm text-slate-700">
            <p className="font-medium text-slate-900">Conta de demonstração</p>
            <p className="mt-1">
              E-mail: <span className="font-mono">{DEMO_EMAIL}</span>
            </p>
            {DEMO_SENHA && (
              <p>
                Senha: <span className="font-mono">{DEMO_SENHA}</span>
              </p>
            )}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="E-mail"
            type="email"
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <Input
            label="Senha"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          {error && <ErrorMessage message={error} />}

          <Button type="submit" className="w-full" size="lg" loading={loading}>
            Entrar
          </Button>
        </form>

        <div className="mt-4 border-t border-slate-200 pt-4 text-center space-y-2">
          {demonstracao && DEMO_SENHA && (
            <button
              type="button"
              onClick={handleContaDemonstracao}
              disabled={loading}
              className="text-sm font-medium text-brand-600 hover:text-brand-700 disabled:opacity-60"
            >
              Entrar com a conta de demonstração
            </button>
          )}
          {AUTH_MODE === "header" && (
            <>
              <button
                type="button"
                onClick={handleDemoLogin}
                className="text-sm font-medium text-brand-600 hover:text-brand-700"
              >
                Entrar em modo demo
              </button>
              <p className="text-xs text-slate-400">
                Desenvolvimento local: abre a loja fictícia, sem Keycloak
              </p>
            </>
          )}
          {!demonstracao && (
            <Link href="/cadastro" className="block text-sm font-medium text-brand-600 hover:text-brand-700">
              Criar conta grátis (14 dias)
            </Link>
          )}
        </div>

        <div className="mt-4 text-center">
          <Link href="/pdv" className="text-sm text-slate-500 hover:text-slate-700">
            Ir para o PDV
          </Link>
        </div>
      </Card>
    </div>
  );
}
