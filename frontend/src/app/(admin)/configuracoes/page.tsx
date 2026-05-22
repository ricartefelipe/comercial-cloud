"use client";

import { Button } from "@/components/ui/Button";
import { Card, CardHeader } from "@/components/ui/Card";
import { ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { listConfiguracoes, upsertConfiguracoes } from "@/lib/api/configuracoes";
import { LOJA_ID, TENANT_ID } from "@/lib/constants";
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
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ["configuracoes", LOJA_ID],
    queryFn: () => listConfiguracoes(LOJA_ID),
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

  const saveMutation = useMutation({
    mutationFn: (items: ConfiguracaoRequest[]) => upsertConfiguracoes(items),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["configuracoes", LOJA_ID] });
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
      .map(([chave, valor]) => ({ lojaId: LOJA_ID, chave, valor }));
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
          Tenant: {TENANT_ID.slice(0, 8)}... | Loja: {LOJA_ID.slice(0, 8)}...
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
      )}
    </div>
  );
}
