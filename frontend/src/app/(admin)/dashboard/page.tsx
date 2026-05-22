"use client";

import { Badge } from "@/components/ui/Badge";
import { Card, CardHeader } from "@/components/ui/Card";
import { ErrorMessage, LoadingSpinner, PageHeader, StatCard } from "@/components/ui/Common";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { getDashboardResumo } from "@/lib/api/dashboard";
import { STATUS_VENDA_LABELS } from "@/types";
import { useQuery } from "@tanstack/react-query";
import { DollarSign, Package, ShoppingCart, TrendingUp } from "lucide-react";
import Link from "next/link";

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

export default function DashboardPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["dashboard"],
    queryFn: () => getDashboardResumo(),
  });

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={(error as Error).message} />;

  return (
    <div>
      <PageHeader
        title="Dashboard"
        description="Visão geral do dia"
      />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Vendas do dia"
          value={String(data?.vendasDoDia ?? 0)}
          icon={<ShoppingCart className="h-5 w-5" />}
        />
        <StatCard
          title="Faturamento"
          value={formatCurrency(data?.faturamentoDoDia ?? 0)}
          icon={<DollarSign className="h-5 w-5" />}
        />
        <StatCard
          title="Ticket médio"
          value={formatCurrency(data?.ticketMedio ?? 0)}
          icon={<TrendingUp className="h-5 w-5" />}
        />
        <StatCard
          title="Estoque baixo"
          value={String(data?.produtosEstoqueBaixo?.length ?? 0)}
          icon={<Package className="h-5 w-5" />}
        />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader title="Últimas vendas" />
          {data?.ultimasVendas?.length ? (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-left text-slate-500">
                    <th className="pb-3 font-medium">Nº</th>
                    <th className="pb-3 font-medium">Total</th>
                    <th className="pb-3 font-medium">Status</th>
                    <th className="pb-3 font-medium">Data</th>
                  </tr>
                </thead>
                <tbody>
                  {data.ultimasVendas.map((venda) => (
                    <tr key={venda.id} className="border-b border-slate-100">
                      <td className="py-3">
                        <Link
                          href={`/vendas/${venda.id}`}
                          className="font-medium text-brand-600 hover:underline"
                        >
                          #{venda.numero ?? venda.id.slice(0, 8)}
                        </Link>
                      </td>
                      <td className="py-3">{formatCurrency(venda.total)}</td>
                      <td className="py-3">
                        <Badge variant={statusVariant(venda.status)}>
                          {STATUS_VENDA_LABELS[venda.status]}
                        </Badge>
                      </td>
                      <td className="py-3 text-slate-500">
                        {venda.createdAt ? formatDateTime(venda.createdAt) : "-"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-sm text-slate-500">Nenhuma venda recente.</p>
          )}
        </Card>

        <Card>
          <CardHeader title="Produtos com estoque baixo" />
          {data?.produtosEstoqueBaixo?.length ? (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-left text-slate-500">
                    <th className="pb-3 font-medium">Produto</th>
                    <th className="pb-3 font-medium">Atual</th>
                    <th className="pb-3 font-medium">Mínimo</th>
                  </tr>
                </thead>
                <tbody>
                  {data.produtosEstoqueBaixo.map((item) => (
                    <tr key={item.id} className="border-b border-slate-100">
                      <td className="py-3 font-medium">
                        {item.produtoNome ?? item.produtoId.slice(0, 8)}
                      </td>
                      <td className="py-3">
                        <Badge variant="danger">{item.quantidadeAtual}</Badge>
                      </td>
                      <td className="py-3 text-slate-500">{item.quantidadeMinima}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-sm text-slate-500">Nenhum produto com estoque baixo.</p>
          )}
        </Card>
      </div>
    </div>
  );
}
