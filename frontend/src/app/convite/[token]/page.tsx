"use client";

import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ErrorMessage, LoadingSpinner } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { aceitarConvite, consultarConvitePublico } from "@/lib/api/convites";
import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, Store } from "lucide-react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";

export default function ConvitePage() {
  const params = useParams<{ token: string }>();
  const token = params.token;
  const router = useRouter();
  const [nome, setNome] = useState("");
  const [senha, setSenha] = useState("");
  const [confirmacao, setConfirmacao] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);
  const [loginUrl, setLoginUrl] = useState("/login");

  const { data, isLoading, error: loadError } = useQuery({
    queryKey: ["convite-publico", token],
    queryFn: () => consultarConvitePublico(token),
    enabled: Boolean(token),
  });

  useEffect(() => {
    if (data?.nome && !nome) {
      setNome(data.nome);
    }
  }, [data?.nome, nome]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    if (senha.length < 8) {
      setError("A senha deve ter ao menos 8 caracteres.");
      return;
    }
    if (senha !== confirmacao) {
      setError("As senhas não coincidem.");
      return;
    }
    setLoading(true);
    try {
      const result = await aceitarConvite(token, { senha, nome: nome.trim() });
      setLoginUrl(result.loginUrl);
      setDone(true);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50">
        <LoadingSpinner />
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4">
        <Card className="max-w-md w-full">
          <ErrorMessage message={(loadError as Error).message} />
          <Link href="/login" className="mt-4 inline-block text-sm text-brand-600 hover:underline">
            Ir para login
          </Link>
        </Card>
      </div>
    );
  }

  if (done) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4">
        <Card className="max-w-md w-full text-center">
          <CheckCircle2 className="mx-auto h-12 w-12 text-green-600" />
          <h1 className="mt-4 text-xl font-semibold">Conta ativada</h1>
          <p className="mt-2 text-slate-600">Você já pode acessar o sistema com seu e-mail e senha.</p>
          <Button className="mt-6" onClick={() => router.push(loginUrl)}>
            Fazer login
          </Button>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <Card className="w-full max-w-md">
        <div className="mb-6 flex items-center gap-2">
          <Store className="h-6 w-6 text-brand-600" />
          <span className="text-lg font-bold">{data?.tenantNome ?? "ComercialCloud"}</span>
        </div>
        <h1 className="text-xl font-semibold">Aceitar convite</h1>
        <p className="mt-2 text-sm text-slate-600">
          {data?.email} — perfil {data?.role}
        </p>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <Input label="Seu nome" value={nome} onChange={(e) => setNome(e.target.value)} required />
          <Input
            label="Senha"
            type="password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            required
            minLength={8}
          />
          <Input
            label="Confirmar senha"
            type="password"
            value={confirmacao}
            onChange={(e) => setConfirmacao(e.target.value)}
            required
            minLength={8}
          />
          {error && <ErrorMessage message={error} />}
          <Button type="submit" className="w-full" loading={loading}>
            Ativar conta
          </Button>
        </form>
      </Card>
    </div>
  );
}
