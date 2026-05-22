"use client";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { EmptyState, ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { listVendas } from "@/lib/api/vendas";
import { FORMA_PAGAMENTO_LABELS, STATUS_VENDA_LABELS } from "@/types";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
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

export default function VendasPage() {
  const [dataInicio, setDataInicio] = useState("");
  const [dataFim, setDataFim] = useState("");

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["vendas", dataInicio, dataFim],
    queryFn: () => listVendas({ dataInicio: dataInicio || undefined, dataFim: dataFim || undefined }),
  });

  return (
    <div>
      <PageHeader title="Vendas" description="Histórico de vendas" />

      <Card className="mb-6">
        <div className="flex flex-wrap items-end gap-4">
          <Input
            label="Data início"
            type="date"
            value={dataInicio}
            onChange={(e) => setDataInicio(e.target.value)}
          />
          <Input
            label="Data fim"
            type="date"
            value={dataFim}
            onChange={(e) => setDataFim(e.target.value)}
          />
          <Button onClick={() => refetch()}>Filtrar</Button>
        </div>
      </Card>

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
                      <th className="px-6 py-3 font-medium">Nº</th>
                      <th className="px-6 py-3 font-medium">Cliente</th>
                      <th className="px-6 py-3 font-medium">Total</th>
                      <th className="px-6 py-3 font-medium">Pagamento</th>
                      <th className="px-6 py-3 font-medium">Status</th>
                      <th className="px-6 py-3 font-medium">Data</th>
                      <th className="px-6 py-3 font-medium">Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.map((venda) => (
                      <tr key={venda.id} className="border-b border-slate-100 hover:bg-slate-50">
                        <td className="px-6 py-4 font-medium">
                          #{venda.numero ?? venda.id.slice(0, 8)}
                        </td>
                        <td className="px-6 py-4">{venda.clienteNome ?? "Consumidor"}</td>
                        <td className="px-6 py-4">{formatCurrency(venda.total)}</td>
                        <td className="px-6 py-4 text-slate-500">
                          {venda.formaPagamentoPrincipal
                            ? FORMA_PAGAMENTO_LABELS[venda.formaPagamentoPrincipal]
                            : "-"}
                        </td>
                        <td className="px-6 py-4">
                          <Badge variant={statusVariant(venda.status)}>
                            {STATUS_VENDA_LABELS[venda.status]}
                          </Badge>
                        </td>
                        <td className="px-6 py-4 text-slate-500">
                          {venda.createdAt ? formatDateTime(venda.createdAt) : "-"}
                        </td>
                        <td className="px-6 py-4">
                          <Link href={`/vendas/${venda.id}`}>
                            <Button variant="ghost" size="sm">
                              Detalhes
                            </Button>
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          ) : (
            <EmptyState title="Nenhuma venda encontrada" />
          )}
        </>
      )}
    </div>
  );
}
