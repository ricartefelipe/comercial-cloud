"use client";

import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { EmptyState, ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { formatCurrency, formatDate } from "@/lib/format";
import { listContasReceber } from "@/lib/api/financeiro";
import { STATUS_CONTA_LABELS } from "@/types";
import { useQuery } from "@tanstack/react-query";

function statusVariant(status: string) {
  switch (status) {
    case "PAGO":
      return "success" as const;
    case "CANCELADO":
      return "danger" as const;
    default:
      return "warning" as const;
  }
}

export default function FinanceiroPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["contas-receber"],
    queryFn: () => listContasReceber(),
  });

  const totalAberto = data
    ?.filter((c) => c.status === "ABERTO")
    .reduce((sum, c) => sum + c.valor, 0) ?? 0;

  return (
    <div>
      <PageHeader
        title="Financeiro"
        description="Contas a receber"
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <Card>
          <p className="text-sm text-slate-500">Total em aberto</p>
          <p className="mt-1 text-2xl font-bold text-amber-600">
            {formatCurrency(totalAberto)}
          </p>
        </Card>
        <Card>
          <p className="text-sm text-slate-500">Total de contas</p>
          <p className="mt-1 text-2xl font-bold text-slate-900">{data?.length ?? 0}</p>
        </Card>
        <Card>
          <p className="text-sm text-slate-500">Pagas</p>
          <p className="mt-1 text-2xl font-bold text-emerald-600">
            {data?.filter((c) => c.status === "PAGO").length ?? 0}
          </p>
        </Card>
      </div>

      {isLoading && <LoadingSpinner />}
      {error && <ErrorMessage message={(error as Error).message} />}

      {!isLoading && !error && (
        <>
          {data?.length ? (
            <Card padding="none">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 bg-slate-50 text-left text-slate-500">
                      <th className="px-6 py-3 font-medium">Descrição</th>
                      <th className="px-6 py-3 font-medium">Valor</th>
                      <th className="px-6 py-3 font-medium">Vencimento</th>
                      <th className="px-6 py-3 font-medium">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.map((conta) => (
                      <tr key={conta.id} className="border-b border-slate-100 hover:bg-slate-50">
                        <td className="px-6 py-4 font-medium">{conta.descricao}</td>
                        <td className="px-6 py-4">{formatCurrency(conta.valor)}</td>
                        <td className="px-6 py-4 text-slate-500">
                          {formatDate(conta.vencimento)}
                        </td>
                        <td className="px-6 py-4">
                          <Badge variant={statusVariant(conta.status)}>
                            {STATUS_CONTA_LABELS[conta.status]}
                          </Badge>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          ) : (
            <EmptyState title="Nenhuma conta a receber" />
          )}
        </>
      )}
    </div>
  );
}
