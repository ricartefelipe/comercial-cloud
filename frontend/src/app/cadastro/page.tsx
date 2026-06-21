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
import { useState, type Dispatch, type SetStateAction } from "react";

const initialFormData = {
  nomeFantasia: "",
  razaoSocial: "",
  cnpj: "",
  adminNome: "",
  adminEmail: "",
  adminSenha: "",
};

function updateField(
  setter: Dispatch<SetStateAction<typeof initialFormData>>,
  field: keyof typeof initialFormData,
  value: string,
) {
  setter((prev) => ({ ...prev, [field]: value }));
}

export default function CadastroPage() {
  const router = useRouter();
  const [wizardStep, setWizardStep] = useState(0);
  const [step, setStep] = useState<"form" | "success">("form");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [planoCodigo, setPlanoCodigo] = useState("ESSENCIAL");
  const [formData, setFormData] = useState(initialFormData);
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

  const handleSubmit = async () => {
    setLoading(true);
    setError("");

    try {
      const result = await criarConta({
        nomeFantasia: formData.nomeFantasia,
        razaoSocial: formData.razaoSocial,
        cnpj: formData.cnpj,
        adminNome: formData.adminNome,
        adminEmail: formData.adminEmail,
        adminSenha: formData.adminSenha || "",
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
        nomeFantasia: formData.nomeFantasia,
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

  const wizardLabels = ["Empresa", "Administrador", "Plano"];

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

        <div className="mb-6 flex justify-center gap-2">
          {wizardLabels.map((label, index) => (
            <div
              key={label}
              className={`rounded-full px-3 py-1 text-xs font-medium ${
                index === wizardStep
                  ? "bg-brand-600 text-white"
                  : index < wizardStep
                    ? "bg-brand-100 text-brand-700"
                    : "bg-slate-100 text-slate-500"
              }`}
            >
              {index + 1}. {label}
            </div>
          ))}
        </div>

        <div className="space-y-4">
          {wizardStep === 0 && (
            <>
              <div className="grid gap-4 sm:grid-cols-2">
                <Input
                  label="Nome fantasia"
                  required
                  placeholder="Minha Loja"
                  value={formData.nomeFantasia}
                  onChange={(e) => updateField(setFormData, "nomeFantasia", e.target.value)}
                />
                <Input
                  label="Razão social"
                  required
                  placeholder="Minha Loja LTDA"
                  value={formData.razaoSocial}
                  onChange={(e) => updateField(setFormData, "razaoSocial", e.target.value)}
                />
              </div>
              <Input
                label="CNPJ"
                required
                placeholder="00.000.000/0001-00"
                value={formData.cnpj}
                onChange={(e) => updateField(setFormData, "cnpj", e.target.value)}
              />
            </>
          )}

          {wizardStep === 1 && (
            <>
              <div className="grid gap-4 sm:grid-cols-2">
                <Input
                  label="Seu nome"
                  required
                  placeholder="João Silva"
                  value={formData.adminNome}
                  onChange={(e) => updateField(setFormData, "adminNome", e.target.value)}
                />
                <Input
                  label="E-mail do administrador"
                  type="email"
                  required
                  placeholder="admin@minhaloja.com"
                  value={formData.adminEmail}
                  onChange={(e) => updateField(setFormData, "adminEmail", e.target.value)}
                />
              </div>
              <Input
                label="Senha de acesso"
                type="password"
                minLength={8}
                placeholder="Mínimo 8 caracteres (Keycloak em produção)"
                value={formData.adminSenha}
                onChange={(e) => updateField(setFormData, "adminSenha", e.target.value)}
              />
            </>
          )}

          {wizardStep === 2 && (
            <>
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
              <p className="text-xs text-slate-500">
                Ao criar conta, você concorda com os{" "}
                <Link href="/termos" className="text-brand-600 hover:text-brand-700">
                  termos de uso
                </Link>{" "}
                e a{" "}
                <Link href="/privacidade" className="text-brand-600 hover:text-brand-700">
                  política de privacidade
                </Link>
                .
              </p>
            </>
          )}

          {error && <ErrorMessage message={error} />}

          <div className="flex gap-3 pt-2">
            {wizardStep > 0 && (
              <Button type="button" variant="outline" onClick={() => setWizardStep((s) => s - 1)}>
                Voltar
              </Button>
            )}
            {wizardStep < 2 ? (
              <Button
                type="button"
                className="flex-1"
                onClick={() => setWizardStep((s) => s + 1)}
                disabled={
                  (wizardStep === 0 &&
                    (!formData.nomeFantasia || !formData.razaoSocial || !formData.cnpj)) ||
                  (wizardStep === 1 && (!formData.adminNome || !formData.adminEmail))
                }
              >
                Continuar
              </Button>
            ) : (
              <Button type="button" className="flex-1" size="lg" loading={loading} onClick={handleSubmit}>
                Criar conta e começar trial
              </Button>
            )}
          </div>
        </div>

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
