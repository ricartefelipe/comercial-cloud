"use client";

import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { EmptyState, ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { listAuditoria } from "@/lib/api/auditoria";
import { formatDateTime } from "@/lib/format";
import { useQuery } from "@tanstack/react-query";

export default function AuditoriaPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["auditoria"],
    queryFn: () => listAuditoria(200),
  });

  return (
    <div>
      <PageHeader
        title="Auditoria"
        description="Registro de ações relevantes no tenant"
      />

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
                      <th className="px-6 py-3 font-medium">Data</th>
                      <th className="px-6 py-3 font-medium">Entidade</th>
                      <th className="px-6 py-3 font-medium">Ação</th>
                      <th className="px-6 py-3 font-medium">Detalhes</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.map((item) => (
                      <tr key={item.id} className="border-b border-slate-100 hover:bg-slate-50">
                        <td className="px-6 py-3 text-slate-500">
                          {formatDateTime(item.createdAt)}
                        </td>
                        <td className="px-6 py-3">
                          <Badge>{item.entidade}</Badge>
                        </td>
                        <td className="px-6 py-3 font-medium">{item.acao}</td>
                        <td className="px-6 py-3 text-slate-600">
                          {item.detalhes ?? "—"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          ) : (
            <EmptyState title="Nenhum registro de auditoria" />
          )}
        </>
      )}
    </div>
  );
}
