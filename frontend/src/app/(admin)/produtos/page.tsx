"use client";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { EmptyState, ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { ConfirmModal } from "@/components/ui/Modal";
import { formatCurrency } from "@/lib/format";
import {
  ativarProduto,
  inativarProduto,
  listProdutos,
} from "@/lib/api/produtos";
import type { Produto } from "@/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Search } from "lucide-react";
import Link from "next/link";
import { useState } from "react";

export default function ProdutosPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [toggleTarget, setToggleTarget] = useState<Produto | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ["produtos", page],
    queryFn: () => listProdutos(page, 20),
  });

  const toggleMutation = useMutation({
    mutationFn: (produto: Produto) =>
      produto.ativo ? inativarProduto(produto.id) : ativarProduto(produto.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["produtos"] });
      setToggleTarget(null);
    },
  });

  const filtered = data?.content.filter(
    (p) =>
      !search ||
      p.nome.toLowerCase().includes(search.toLowerCase()) ||
      p.sku.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <div>
      <PageHeader
        title="Produtos"
        description="Gerencie o catálogo de produtos"
        action={
          <Link href="/produtos/novo">
            <Button>
              <Plus className="h-4 w-4" />
              Novo produto
            </Button>
          </Link>
        }
      />

      <Card className="mb-6">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <Input
            placeholder="Buscar por nome ou SKU..."
            className="pl-10"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </Card>

      {isLoading && <LoadingSpinner />}
      {error && <ErrorMessage message={(error as Error).message} />}

      {!isLoading && !error && (
        <>
          {filtered?.length ? (
            <Card padding="none">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 bg-slate-50 text-left text-slate-500">
                      <th className="px-6 py-3 font-medium">SKU</th>
                      <th className="px-6 py-3 font-medium">Nome</th>
                      <th className="px-6 py-3 font-medium">Categoria</th>
                      <th className="px-6 py-3 font-medium">Preço</th>
                      <th className="px-6 py-3 font-medium">Status</th>
                      <th className="px-6 py-3 font-medium">Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtered.map((produto) => (
                      <tr key={produto.id} className="border-b border-slate-100 hover:bg-slate-50">
                        <td className="px-6 py-4 font-mono text-xs">{produto.sku}</td>
                        <td className="px-6 py-4 font-medium">{produto.nome}</td>
                        <td className="px-6 py-4 text-slate-500">{produto.categoria ?? "-"}</td>
                        <td className="px-6 py-4">{formatCurrency(produto.precoVenda)}</td>
                        <td className="px-6 py-4">
                          <Badge variant={produto.ativo ? "success" : "default"}>
                            {produto.ativo ? "Ativo" : "Inativo"}
                          </Badge>
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex gap-2">
                            <Link href={`/produtos/${produto.id}`}>
                              <Button variant="ghost" size="sm">
                                Editar
                              </Button>
                            </Link>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => setToggleTarget(produto)}
                            >
                              {produto.ativo ? "Inativar" : "Ativar"}
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          ) : (
            <EmptyState
              title="Nenhum produto encontrado"
              description="Cadastre o primeiro produto do catálogo."
              action={
                <Link href="/produtos/novo">
                  <Button>Novo produto</Button>
                </Link>
              }
            />
          )}

          {data && data.totalPages > 1 && (
            <div className="mt-4 flex items-center justify-between">
              <p className="text-sm text-slate-500">
                Página {page + 1} de {data.totalPages} ({data.totalElements} produtos)
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                >
                  Anterior
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page >= data.totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Próxima
                </Button>
              </div>
            </div>
          )}
        </>
      )}

      <ConfirmModal
        open={!!toggleTarget}
        onClose={() => setToggleTarget(null)}
        onConfirm={() => toggleTarget && toggleMutation.mutate(toggleTarget)}
        title={toggleTarget?.ativo ? "Inativar produto" : "Ativar produto"}
        message={`Deseja ${toggleTarget?.ativo ? "inativar" : "ativar"} o produto "${toggleTarget?.nome}"?`}
        confirmLabel={toggleTarget?.ativo ? "Inativar" : "Ativar"}
        loading={toggleMutation.isPending}
        variant={toggleTarget?.ativo ? "danger" : "primary"}
      />
    </div>
  );
}
