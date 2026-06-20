"use client";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader } from "@/components/ui/Card";
import { ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { Select } from "@/components/ui/Select";
import {
  cancelarAssinatura,
  getAssinatura,
  listFaturas,
  listPlanos,
  pagarFatura,
  reativarAssinatura,
  trocarPlano,
} from "@/lib/api/billing";
import { formatCurrency, formatDate } from "@/lib/format";
import {
  STATUS_ASSINATURA_LABELS,
  STATUS_FATURA_LABELS,
  type StatusAssinatura,
  type StatusFatura,
} from "@/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, CreditCard } from "lucide-react";
import { useSearchParams } from "next/navigation";
import { useState } from "react";

function assinaturaVariant(status: StatusAssinatura) {
  switch (status) {
    case "ATIVA":
    case "TRIAL":
      return "success" as const;
    case "SUSPENSA":
    case "INADIMPLENTE":
      return "danger" as const;
    default:
      return "warning" as const;
  }
}

function faturaVariant(status: StatusFatura) {
  switch (status) {
    case "PAGA":
      return "success" as const;
    case "FALHA":
    case "VENCIDA":
      return "danger" as const;
    default:
      return "warning" as const;
  }
}

export default function AssinaturaContent() {
  const searchParams = useSearchParams();
  const suspensa = searchParams.get("suspensa") === "1";
  const queryClient = useQueryClient();
  const [novoPlano, setNovoPlano] = useState("");

  const { data: assinatura, isLoading, error } = useQuery({
    queryKey: ["assinatura"],
    queryFn: getAssinatura,
  });

  const { data: planos } = useQuery({
    queryKey: ["planos-billing"],
    queryFn: listPlanos,
  });

  const { data: faturas, isLoading: faturasLoading } = useQuery({
    queryKey: ["faturas"],
    queryFn: listFaturas,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["assinatura"] });
    queryClient.invalidateQueries({ queryKey: ["faturas"] });
  };

  const trocarMutation = useMutation({
    mutationFn: (codigo: string) => trocarPlano(codigo),
    onSuccess: invalidate,
  });

  const cancelarMutation = useMutation({
    mutationFn: cancelarAssinatura,
    onSuccess: invalidate,
  });

  const reativarMutation = useMutation({
    mutationFn: reativarAssinatura,
    onSuccess: invalidate,
  });

  const pagarMutation = useMutation({
    mutationFn: (id: string) => pagarFatura(id),
    onSuccess: invalidate,
  });

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={(error as Error).message} />;

  const planoOptions = (planos ?? []).map((p) => ({
    value: p.codigo,
    label: `${p.nome} — ${formatCurrency(Number(p.precoMensal))}/mês`,
  }));

  return (
    <div>
      <PageHeader
        title="Assinatura"
        description="Gerencie seu plano, faturas e pagamentos"
      />

      {suspensa && (
        <div className="mb-6 flex items-start gap-3 rounded-lg border border-amber-200 bg-amber-50 p-4 text-amber-900">
          <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0" />
          <div>
            <p className="font-medium">Conta suspensa por inadimplência</p>
            <p className="text-sm">
              Regularize o pagamento abaixo para voltar a usar o PDV e a retaguarda.
            </p>
          </div>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader title="Plano atual" />
          {assinatura ? (
            <div className="space-y-4">
              <div className="flex items-center gap-2">
                <Badge variant={assinaturaVariant(assinatura.status)} className="text-sm px-3 py-1">
                  {STATUS_ASSINATURA_LABELS[assinatura.status]}
                </Badge>
                {assinatura.trialFim && assinatura.status === "TRIAL" && (
                  <span className="text-sm text-slate-500">
                    Trial até {formatDate(assinatura.trialFim)}
                  </span>
                )}
              </div>

              {assinatura.proximaCobranca && (
                <p className="text-sm text-slate-600">
                  Próxima cobrança: {formatDate(assinatura.proximaCobranca)}
                </p>
              )}

              <div className="flex flex-col gap-2 sm:flex-row sm:items-end">
                <div className="flex-1">
                  <Select
                    label="Trocar plano"
                    value={novoPlano || planoOptions[0]?.value || ""}
                    onChange={(e) => setNovoPlano(e.target.value)}
                    options={planoOptions}
                  />
                </div>
                <Button
                  onClick={() => trocarMutation.mutate(novoPlano || planoOptions[0]?.value || "ESSENCIAL")}
                  loading={trocarMutation.isPending}
                >
                  Trocar
                </Button>
              </div>

              <div className="flex gap-2 border-t border-slate-200 pt-4">
                {assinatura.status === "CANCELADA" ? (
                  <Button
                    variant="outline"
                    onClick={() => reativarMutation.mutate()}
                    loading={reativarMutation.isPending}
                  >
                    Reativar assinatura
                  </Button>
                ) : (
                  <Button
                    variant="danger"
                    onClick={() => cancelarMutation.mutate()}
                    loading={cancelarMutation.isPending}
                  >
                    Cancelar assinatura
                  </Button>
                )}
              </div>
            </div>
          ) : (
            <p className="text-sm text-slate-500">Nenhuma assinatura encontrada.</p>
          )}
        </Card>

        <Card>
          <CardHeader title="Como funciona o pagamento" />
          <div className="space-y-3 text-sm text-slate-600">
            <p className="flex items-start gap-2">
              <CreditCard className="mt-0.5 h-4 w-4 shrink-0 text-brand-600" />
              No ambiente atual, a cobrança usa gateway sandbox (simulação).
            </p>
            <p>
              Clique em <strong>Pagar fatura</strong> para simular aprovação e reativar
              contas suspensas.
            </p>
            <p className="text-xs text-slate-400">
              Em produção, substitua o adapter por Asaas, Iugu, Stripe ou Pagar.me
              (ver README-COMERCIAL.md).
            </p>
          </div>
        </Card>
      </div>

      <Card className="mt-6">
        <CardHeader title="Faturas" />
        {faturasLoading ? (
          <LoadingSpinner />
        ) : faturas?.length ? (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-slate-500">
                  <th className="pb-2 font-medium">Competência</th>
                  <th className="pb-2 font-medium">Valor</th>
                  <th className="pb-2 font-medium">Vencimento</th>
                  <th className="pb-2 font-medium">Status</th>
                  <th className="pb-2 font-medium">Ação</th>
                </tr>
              </thead>
              <tbody>
                {faturas.map((fatura) => (
                  <tr key={fatura.id} className="border-b border-slate-100">
                    <td className="py-3">{fatura.competencia ?? "—"}</td>
                    <td className="py-3 font-medium">{formatCurrency(fatura.valor)}</td>
                    <td className="py-3">
                      {fatura.vencimento ? formatDate(fatura.vencimento) : "—"}
                    </td>
                    <td className="py-3">
                      <Badge variant={faturaVariant(fatura.status)}>
                        {STATUS_FATURA_LABELS[fatura.status]}
                      </Badge>
                    </td>
                    <td className="py-3">
                      {fatura.status !== "PAGA" && fatura.status !== "CANCELADA" && (
                        <div className="flex flex-col gap-1">
                          <Button
                            size="sm"
                            onClick={() => pagarMutation.mutate(fatura.id)}
                            loading={pagarMutation.isPending}
                          >
                            Pagar fatura
                          </Button>
                          {fatura.checkoutUrl && (
                            <a
                              href={fatura.checkoutUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-xs text-brand-600 hover:underline"
                            >
                              Abrir checkout PIX (Asaas)
                            </a>
                          )}
                        </div>
                      )}
                      {fatura.motivoRecusa && (
                        <p className="mt-1 text-xs text-red-600">{fatura.motivoRecusa}</p>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-sm text-slate-500">Nenhuma fatura gerada ainda.</p>
        )}
      </Card>
    </div>
  );
}
