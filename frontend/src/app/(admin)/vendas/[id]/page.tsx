"use client";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader } from "@/components/ui/Card";
import { ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { ConfirmModal } from "@/components/ui/Modal";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { cancelarVenda, getVenda } from "@/lib/api/vendas";
import { FORMA_PAGAMENTO_LABELS, STATUS_VENDA_LABELS } from "@/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";

function statusVariant(status: string) {
  switch (status) {
    case "FINALIZADA":
      return "success" as const;
    case "CANCELADA":
      return "danger" as const;
    default:
      return "warning" as const;
  }
}

export default function VendaDetalhePage() {
  const params = useParams();
  const id = params.id as string;
  const [cancelOpen, setCancelOpen] = useState(false);
  const queryClient = useQueryClient();

  const { data: venda, isLoading, error } = useQuery({
    queryKey: ["venda", id],
    queryFn: () => getVenda(id),
  });

  const cancelMutation = useMutation({
    mutationFn: () => cancelarVenda(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["venda", id] });
      queryClient.invalidateQueries({ queryKey: ["vendas"] });
      setCancelOpen(false);
    },
  });

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={(error as Error).message} />;
  if (!venda) return <ErrorMessage message="Venda não encontrada" />;

  return (
    <div>
      <PageHeader
        title={`Venda #${venda.numero ?? venda.id.slice(0, 8)}`}
        description={venda.createdAt ? formatDateTime(venda.createdAt) : undefined}
        action={
          <div className="flex gap-2">
            <Link href="/vendas">
              <Button variant="outline">
                <ArrowLeft className="h-4 w-4" />
                Voltar
              </Button>
            </Link>
            {venda.status !== "CANCELADA" && venda.status !== "FINALIZADA" && (
              <Button variant="danger" onClick={() => setCancelOpen(true)}>
                Cancelar venda
              </Button>
            )}
          </div>
        }
      />

      <div className="mb-6 flex flex-wrap gap-4">
        <Badge variant={statusVariant(venda.status)} className="text-sm px-3 py-1">
          {STATUS_VENDA_LABELS[venda.status]}
        </Badge>
        <span className="text-sm text-slate-500">
          Cliente: {venda.clienteNome ?? "Consumidor"}
        </span>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader title="Itens" />
            {venda.itens?.length ? (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 text-left text-slate-500">
                      <th className="pb-2 font-medium">Produto</th>
                      <th className="pb-2 font-medium">Qtd</th>
                      <th className="pb-2 font-medium">Unit.</th>
                      <th className="pb-2 font-medium">Desc.</th>
                      <th className="pb-2 font-medium">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {venda.itens.map((item) => (
                      <tr key={item.id} className="border-b border-slate-100">
                        <td className="py-2 font-medium">
                          {item.produtoNome ?? item.produtoSku ?? item.produtoId.slice(0, 8)}
                        </td>
                        <td className="py-2">{item.quantidade}</td>
                        <td className="py-2">{formatCurrency(item.precoUnitario)}</td>
                        <td className="py-2">{formatCurrency(item.desconto)}</td>
                        <td className="py-2 font-medium">{formatCurrency(item.total)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="text-sm text-slate-500">Nenhum item.</p>
            )}
          </Card>

          <Card>
            <CardHeader title="Pagamentos" />
            {venda.pagamentos?.length ? (
              <div className="space-y-2">
                {venda.pagamentos.map((pag) => (
                  <div
                    key={pag.id}
                    className="flex items-center justify-between rounded-lg bg-slate-50 px-4 py-2"
                  >
                    <span>{FORMA_PAGAMENTO_LABELS[pag.formaPagamento]}</span>
                    <span className="font-medium">{formatCurrency(pag.valor)}</span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-slate-500">Nenhum pagamento registrado.</p>
            )}
          </Card>
        </div>

        <Card>
          <CardHeader title="Resumo" />
          <dl className="space-y-3 text-sm">
            <div className="flex justify-between">
              <dt className="text-slate-500">Subtotal</dt>
              <dd>{formatCurrency(venda.subtotal)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Desconto</dt>
              <dd>{formatCurrency(venda.descontoTotal)}</dd>
            </div>
            <div className="flex justify-between border-t border-slate-200 pt-3 text-base font-bold">
              <dt>Total</dt>
              <dd className="text-brand-600">{formatCurrency(venda.total)}</dd>
            </div>
          </dl>
        </Card>
      </div>

      <ConfirmModal
        open={cancelOpen}
        onClose={() => setCancelOpen(false)}
        onConfirm={() => cancelMutation.mutate()}
        title="Cancelar venda"
        message="Tem certeza que deseja cancelar esta venda? Esta ação não pode ser desfeita."
        confirmLabel="Cancelar venda"
        loading={cancelMutation.isPending}
      />
    </div>
  );
}
