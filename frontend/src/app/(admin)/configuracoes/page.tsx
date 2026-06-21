"use client";

import { Button } from "@/components/ui/Button";
import { Card, CardHeader } from "@/components/ui/Card";
import { ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { listConfiguracoes, upsertConfiguracoes } from "@/lib/api/configuracoes";
import { saveBranding } from "@/lib/api/branding";
import { useBranding } from "@/components/branding/BrandingProvider";
import { getLojaId, getTenantId } from "@/lib/auth/tenant-context";
import type { ConfiguracaoRequest } from "@/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Save } from "lucide-react";
import { FormEvent, useEffect, useState } from "react";

const CONFIG_KEYS = {
  empresa: [
    { key: "empresa.razao_social", label: "Razão social" },
    { key: "empresa.cnpj", label: "CNPJ" },
    { key: "empresa.inscricao_estadual", label: "Inscrição estadual" },
  ],
  loja: [
    { key: "loja.nome", label: "Nome da loja" },
    { key: "loja.endereco", label: "Endereço" },
    { key: "loja.horario", label: "Horário de funcionamento" },
  ],
  pagamentos: [
    { key: "pagamentos.dinheiro", label: "Aceita dinheiro (true/false)" },
    { key: "pagamentos.pix", label: "Aceita PIX (true/false)" },
    { key: "pagamentos.cartao", label: "Aceita cartão (true/false)" },
  ],
} as const;

type ConfigSection = keyof typeof CONFIG_KEYS;

function buildInitialValues(): Record<string, string> {
  const values: Record<string, string> = {};
  for (const section of Object.keys(CONFIG_KEYS) as ConfigSection[]) {
    for (const field of CONFIG_KEYS[section]) {
      values[field.key] = "";
    }
  }
  return values;
}

export default function ConfiguracoesPage() {
  const [values, setValues] = useState<Record<string, string>>(buildInitialValues);
  const [saved, setSaved] = useState(false);
  const brandingAtual = useBranding();
  const [branding, setBranding] = useState({
    nomeExibicao: brandingAtual.nomeExibicao,
    corPrimaria: brandingAtual.corPrimaria,
    logoUrl: brandingAtual.logoUrl ?? "",
  });
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ["configuracoes", getLojaId()],
    queryFn: () => listConfiguracoes(getLojaId()),
  });

  useEffect(() => {
    if (!data?.length) return;
    setValues((prev) => {
      const next = { ...prev };
      for (const config of data) {
        next[config.chave] = config.valor;
      }
      return next;
    });
  }, [data]);

  useEffect(() => {
    setBranding({
      nomeExibicao: brandingAtual.nomeExibicao,
      corPrimaria: brandingAtual.corPrimaria,
      logoUrl: brandingAtual.logoUrl ?? "",
    });
  }, [brandingAtual]);

  const saveMutation = useMutation({
    mutationFn: (items: ConfiguracaoRequest[]) => upsertConfiguracoes(items),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["configuracoes", getLojaId()] });
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    },
  });

  const brandingMutation = useMutation({
    mutationFn: () => saveBranding(branding),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["branding"] });
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    },
  });

  const handleChange = (key: string, value: string) => {
    setValues((prev) => ({ ...prev, [key]: value }));
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    const items: ConfiguracaoRequest[] = Object.entries(values)
      .filter(([, valor]) => valor.trim() !== "")
      .map(([chave, valor]) => ({ lojaId: getLojaId(), chave, valor }));
    await saveMutation.mutateAsync(items);
  };

  const sections: { id: ConfigSection; title: string }[] = [
    { id: "empresa", title: "Empresa" },
    { id: "loja", title: "Loja" },
    { id: "pagamentos", title: "Pagamentos" },
  ];

  return (
    <div>
      <PageHeader
        title="Configurações"
        description="Preferências do sistema"
        action={
          <Button onClick={handleSave} loading={saveMutation.isPending}>
            <Save className="h-4 w-4" />
            {saved ? "Salvo!" : "Salvar alterações"}
          </Button>
        }
      />

      <Card className="mb-6">
        <p className="text-sm font-medium text-slate-900">Ambiente atual</p>
        <p className="text-xs text-slate-500">
          Tenant: {getTenantId().slice(0, 8)}... | Loja: {getLojaId().slice(0, 8)}...
        </p>
      </Card>

      {isLoading && <LoadingSpinner />}
      {error && <ErrorMessage message={(error as Error).message} />}
      {saveMutation.error && (
        <div className="mb-4">
          <ErrorMessage message={(saveMutation.error as Error).message} />
        </div>
      )}

      {!isLoading && (
        <>
          <Card className="mb-6">
            <CardHeader
              title="Marca (white-label)"
              description="Nome, cor e logo exibidos no PDV e retaguarda"
              action={
                <Button
                  type="button"
                  onClick={() => brandingMutation.mutate()}
                  loading={brandingMutation.isPending}
                >
                  Salvar marca
                </Button>
              }
            />
            <div className="grid gap-4 sm:grid-cols-2">
              <Input
                label="Nome exibido"
                value={branding.nomeExibicao}
                onChange={(e) => setBranding((b) => ({ ...b, nomeExibicao: e.target.value }))}
              />
              <Input
                label="Cor primária (hex)"
                value={branding.corPrimaria}
                onChange={(e) => setBranding((b) => ({ ...b, corPrimaria: e.target.value }))}
                placeholder="#4f7cff"
              />
              <Input
                label="URL do logo (opcional)"
                value={branding.logoUrl}
                onChange={(e) => setBranding((b) => ({ ...b, logoUrl: e.target.value }))}
                className="sm:col-span-2"
              />
            </div>
            {brandingMutation.error && (
              <div className="mt-3">
                <ErrorMessage message={(brandingMutation.error as Error).message} />
              </div>
            )}
          </Card>

          <form onSubmit={handleSave} className="space-y-6">
          {sections.map((section) => (
              <Card key={section.id}>
                <CardHeader title={section.title} />
                <div className="space-y-4">
                  {CONFIG_KEYS[section.id].map((field) => (
                    <Input
                      key={field.key}
                      label={field.label}
                      value={values[field.key] ?? ""}
                      onChange={(e) => handleChange(field.key, e.target.value)}
                    />
                  ))}
                </div>
              </Card>
          ))}
        </form>
        </>
      )}
    </div>
  );
}
