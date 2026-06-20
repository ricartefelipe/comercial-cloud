"use client";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader } from "@/components/ui/Card";
import { ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import {
  getConfiguracaoFiscal,
  listDocumentosFiscais,
  salvarConfiguracaoFiscal,
} from "@/lib/api/fiscal";
import { getLojaId } from "@/lib/auth/tenant-context";
import { formatDateTime } from "@/lib/format";
import {
  STATUS_DOCUMENTO_FISCAL_LABELS,
  type AmbienteFiscal,
  type RegimeTributario,
  type StatusDocumentoFiscal,
} from "@/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ExternalLink, FileText } from "lucide-react";
import { FormEvent, useEffect, useState } from "react";

function docVariant(status: StatusDocumentoFiscal) {
  switch (status) {
    case "AUTORIZADA":
      return "success" as const;
    case "REJEITADA":
      return "danger" as const;
    default:
      return "warning" as const;
  }
}

export default function FiscalPage() {
  const lojaId = getLojaId();
  const queryClient = useQueryClient();
  const [saved, setSaved] = useState(false);
  const [form, setForm] = useState({
    cnpj: "",
    inscricaoEstadual: "",
    razaoSocial: "",
    regimeTributario: "SIMPLES_NACIONAL" as RegimeTributario,
    ambiente: "HOMOLOGACAO" as AmbienteFiscal,
    uf: "SP",
    cscId: "",
    csc: "",
    serieNfce: "1",
    habilitado: true,
  });

  const { data: config, isLoading, error } = useQuery({
    queryKey: ["fiscal-config", lojaId],
    queryFn: () => getConfiguracaoFiscal(lojaId),
  });

  const { data: documentos, isLoading: docsLoading } = useQuery({
    queryKey: ["fiscal-documentos", lojaId],
    queryFn: () => listDocumentosFiscais(lojaId),
  });

  useEffect(() => {
    if (!config) return;
    setForm({
      cnpj: config.cnpj,
      inscricaoEstadual: config.inscricaoEstadual ?? "",
      razaoSocial: config.razaoSocial,
      regimeTributario: config.regimeTributario,
      ambiente: config.ambiente,
      uf: config.uf,
      cscId: "",
      csc: "",
      serieNfce: String(config.serieNfce ?? 1),
      habilitado: config.habilitado,
    });
  }, [config]);

  const saveMutation = useMutation({
    mutationFn: () =>
      salvarConfiguracaoFiscal({
        lojaId,
        cnpj: form.cnpj,
        inscricaoEstadual: form.inscricaoEstadual || undefined,
        razaoSocial: form.razaoSocial,
        regimeTributario: form.regimeTributario,
        ambiente: form.ambiente,
        uf: form.uf,
        cscId: form.cscId || undefined,
        csc: form.csc || undefined,
        serieNfce: Number(form.serieNfce) || 1,
        habilitado: form.habilitado,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["fiscal-config", lojaId] });
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    saveMutation.mutate();
  };

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={(error as Error).message} />;

  return (
    <div>
      <PageHeader
        title="Fiscal"
        description="Configuração NFC-e e documentos emitidos"
      />

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader title="Configuração NFC-e" />
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="CNPJ"
              value={form.cnpj}
              onChange={(e) => setForm((f) => ({ ...f, cnpj: e.target.value }))}
              required
            />
            <Input
              label="Razão social"
              value={form.razaoSocial}
              onChange={(e) => setForm((f) => ({ ...f, razaoSocial: e.target.value }))}
              required
            />
            <Input
              label="Inscrição estadual"
              value={form.inscricaoEstadual}
              onChange={(e) => setForm((f) => ({ ...f, inscricaoEstadual: e.target.value }))}
            />
            <div className="grid gap-4 sm:grid-cols-2">
              <Select
                label="Regime tributário"
                value={form.regimeTributario}
                onChange={(e) =>
                  setForm((f) => ({
                    ...f,
                    regimeTributario: e.target.value as RegimeTributario,
                  }))
                }
                options={[
                  { value: "SIMPLES_NACIONAL", label: "Simples Nacional" },
                  { value: "LUCRO_PRESUMIDO", label: "Lucro Presumido" },
                  { value: "LUCRO_REAL", label: "Lucro Real" },
                ]}
              />
              <Select
                label="Ambiente SEFAZ"
                value={form.ambiente}
                onChange={(e) =>
                  setForm((f) => ({ ...f, ambiente: e.target.value as AmbienteFiscal }))
                }
                options={[
                  { value: "HOMOLOGACAO", label: "Homologação" },
                  { value: "PRODUCAO", label: "Produção" },
                ]}
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Input
                label="UF"
                value={form.uf}
                onChange={(e) => setForm((f) => ({ ...f, uf: e.target.value.toUpperCase() }))}
                maxLength={2}
                required
              />
              <Input
                label="Série NFC-e"
                type="number"
                value={form.serieNfce}
                onChange={(e) => setForm((f) => ({ ...f, serieNfce: e.target.value }))}
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Input
                label="CSC ID"
                value={form.cscId}
                onChange={(e) => setForm((f) => ({ ...f, cscId: e.target.value }))}
              />
              <Input
                label="CSC"
                type="password"
                value={form.csc}
                onChange={(e) => setForm((f) => ({ ...f, csc: e.target.value }))}
              />
            </div>
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input
                type="checkbox"
                checked={form.habilitado}
                onChange={(e) => setForm((f) => ({ ...f, habilitado: e.target.checked }))}
                className="rounded border-slate-300"
              />
              Emissão fiscal habilitada
            </label>

            {saveMutation.error && (
              <ErrorMessage message={(saveMutation.error as Error).message} />
            )}
            {saved && (
              <p className="text-sm font-medium text-emerald-600">Configuração salva.</p>
            )}

            <Button type="submit" loading={saveMutation.isPending}>
              Salvar configuração
            </Button>
          </form>
          <p className="mt-4 text-xs text-slate-400">
            Modo sandbox ativo: notas são simuladas. Substitua o provider fiscal em produção.
          </p>
        </Card>

        <Card>
          <CardHeader title="Documentos emitidos" />
          {docsLoading ? (
            <LoadingSpinner />
          ) : documentos?.length ? (
            <div className="max-h-96 space-y-3 overflow-y-auto">
              {documentos.map((doc) => (
                <div
                  key={doc.id}
                  className="rounded-lg border border-slate-200 p-3 text-sm"
                >
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <FileText className="h-4 w-4 text-slate-400" />
                      <span className="font-medium">
                        {doc.modelo} #{doc.numero ?? "—"}
                      </span>
                    </div>
                    <Badge variant={docVariant(doc.status)}>
                      {STATUS_DOCUMENTO_FISCAL_LABELS[doc.status]}
                    </Badge>
                  </div>
                  {doc.chaveAcesso && (
                    <p className="mt-1 break-all text-xs text-slate-500">{doc.chaveAcesso}</p>
                  )}
                  {doc.urlDanfe && (
                    <a
                      href={doc.urlDanfe}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="mt-2 inline-flex items-center gap-1 text-xs text-brand-600 hover:underline"
                    >
                      Ver DANFE <ExternalLink className="h-3 w-3" />
                    </a>
                  )}
                  {doc.createdAt && (
                    <p className="mt-1 text-xs text-slate-400">
                      {formatDateTime(doc.createdAt)}
                    </p>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-slate-500">
              Nenhum documento emitido. Finalize vendas no PDV para gerar NFC-e automaticamente.
            </p>
          )}
        </Card>
      </div>
    </div>
  );
}
