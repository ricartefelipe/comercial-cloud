"use client";

import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ErrorMessage } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { criarConta, listPlanosPublicos } from "@/lib/api/onboarding";
import { setWorkspaceContext } from "@/lib/auth/tenant-context";
import { clearDemoMode, enableDemoMode } from "@/lib/auth/session";
import { formatCurrency } from "@/lib/format";
import type { PlanoPublico } from "@/types";
import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, Store } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";

export default function CadastroPage() {
  const router = useRouter();
  const [step, setStep] = useState<"form" | "success">("form");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [planoCodigo, setPlanoCodigo] = useState("ESSENCIAL");
  const [successInfo, setSuccessInfo] = useState<{
    nomeFantasia: string;
    trialFim?: string;
    produtosDemo: number;
    loginEmail?: string;
    loginSenha?: string;
    modoAcesso?: string;
  } | null>(null);

  const { data: planos, isLoading: planosLoading } = useQuery({
    queryKey: ["planos-publicos"],
    queryFn: listPlanosPublicos,
  });

  const planoOptions = (planos ?? []).map((p: PlanoPublico) => ({
    value: p.codigo,
    label: `${p.nome} — ${formatCurrency(Number(p.precoMensal))}/mês`,
  }));

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    const form = new FormData(e.currentTarget);

    try {
      const result = await criarConta({
        nomeFantasia: String(form.get("nomeFantasia")),
        razaoSocial: String(form.get("razaoSocial")),
        cnpj: String(form.get("cnpj")),
        adminNome: String(form.get("adminNome")),
        adminEmail: String(form.get("adminEmail")),
        adminSenha: String(form.get("adminSenha") || ""),
        planoCodigo,
        comTrial: true,
        seedDemo: true,
      });

      setWorkspaceContext({
        tenantId: result.tenantId,
        userId: result.adminUserId,
        lojaId: result.lojaId,
      });

      if (result.modoAcesso === "keycloak" && result.loginSenha) {
        clearDemoMode();
      } else {
        enableDemoMode();
      }

      setSuccessInfo({
        nomeFantasia: String(form.get("nomeFantasia")),
        trialFim: result.trialFim,
        produtosDemo: result.produtosDemo,
        loginEmail: result.loginEmail,
        loginSenha: result.loginSenha,
        modoAcesso: result.modoAcesso,
      });
      setStep("success");
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  if (step === "success" && successInfo) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-100 px-4">
        <Card className="w-full max-w-lg text-center">
          <div className="mb-4 flex justify-center">
            <CheckCircle2 className="h-12 w-12 text-emerald-500" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900">Conta criada!</h1>
          <p className="mt-2 text-slate-600">
            <strong>{successInfo.nomeFantasia}</strong> está pronta para usar.
          </p>
          <ul className="mt-4 space-y-2 text-left text-sm text-slate-600">
            <li>• Trial de 14 dias ativo{successInfo.trialFim ? ` até ${successInfo.trialFim}` : ""}</li>
            <li>• {successInfo.produtosDemo} produtos demo já cadastrados</li>
            <li>• Abra o caixa e comece a vender no PDV</li>
          </ul>
          <Button className="mt-6 w-full" size="lg" onClick={() => router.push("/dashboard")}>
            Ir para a retaguarda
          </Button>
          {successInfo.modoAcesso === "keycloak" && successInfo.loginEmail && (
            <p className="mt-4 text-sm text-slate-600">
              Login: <strong>{successInfo.loginEmail}</strong>
              {successInfo.loginSenha ? ` · senha definida no cadastro` : ""}
            </p>
          )}
          <p className="mt-4 text-xs text-slate-400">
            {successInfo.modoAcesso === "keycloak"
              ? "Faça login em /login com seu e-mail e senha."
              : "Modo demo ativo com seu tenant. Configure Keycloak para login com senha em produção."}
          </p>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 px-4 py-8">
      <Card className="w-full max-w-xl">
        <div className="mb-6 flex flex-col items-center text-center">
          <div className="mb-3 rounded-xl bg-brand-50 p-3">
            <Store className="h-8 w-8 text-brand-600" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900">Criar conta</h1>
          <p className="mt-1 text-sm text-slate-500">
            14 dias grátis · PDV + retaguarda · dados demo incluídos
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label="Nome fantasia"
              name="nomeFantasia"
              required
              placeholder="Minha Loja"
            />
            <Input
              label="Razão social"
              name="razaoSocial"
              required
              placeholder="Minha Loja LTDA"
            />
          </div>

          <Input
            label="CNPJ"
            name="cnpj"
            required
            placeholder="00.000.000/0001-00"
          />

          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label="Seu nome"
              name="adminNome"
              required
              placeholder="João Silva"
            />
            <Input
              label="E-mail do administrador"
              name="adminEmail"
              type="email"
              required
              placeholder="admin@minhaloja.com"
            />
          </div>

          <Input
            label="Senha de acesso"
            name="adminSenha"
            type="password"
            minLength={8}
            placeholder="Mínimo 8 caracteres (Keycloak em produção)"
          />

          <Select
            label="Plano"
            value={planoCodigo}
            onChange={(e) => setPlanoCodigo(e.target.value)}
            options={
              planoOptions.length
                ? planoOptions
                : [{ value: "ESSENCIAL", label: "Carregando planos..." }]
            }
            disabled={planosLoading}
          />

          {error && <ErrorMessage message={error} />}

          <Button type="submit" className="w-full" size="lg" loading={loading}>
            Criar conta e começar trial
          </Button>
        </form>

        <p className="mt-4 text-center text-sm text-slate-500">
          Já tem conta?{" "}
          <Link href="/login" className="font-medium text-brand-600 hover:text-brand-700">
            Entrar
          </Link>
        </p>
      </Card>
    </div>
  );
}
