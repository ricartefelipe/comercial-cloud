"use client";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { EmptyState, ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { formatCurrency } from "@/lib/format";
import {
  ativarProduto,
  importarProdutosCsv,
  inativarProduto,
  listProdutos,
} from "@/lib/api/produtos";
import { listEstoques } from "@/lib/api/estoque";
import type { Produto } from "@/types";
import { ProductImage } from "@/components/produtos/ProductImage";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Search, Upload } from "lucide-react";
import Link from "next/link";
import { useRef, useState } from "react";

export default function ProdutosPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [toggleTarget, setToggleTarget] = useState<Produto | null>(null);
  const [importOpen, setImportOpen] = useState(false);
  const [importResult, setImportResult] = useState<string>("");
  const fileInputRef = useRef<HTMLInputElement>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ["produtos", page],
    queryFn: () => listProdutos(page, 20),
  });

  const { data: estoques } = useQuery({
    queryKey: ["estoques-produtos"],
    queryFn: () => listEstoques(),
  });

  const estoquePorProduto = new Map(
    (estoques ?? []).map((e) => [e.produtoId, e]),
  );

  const toggleMutation = useMutation({
    mutationFn: (produto: Produto) =>
      produto.ativo ? inativarProduto(produto.id) : ativarProduto(produto.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["produtos"] });
      setToggleTarget(null);
    },
  });

  const importMutation = useMutation({
    mutationFn: (csv: string) => importarProdutosCsv(csv),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["produtos"] });
      setImportResult(`${result.criados} criados, ${result.ignorados} ignorados`);
    },
  });

  const handleImportFile = async (file: File | null) => {
    if (!file) return;
    const text = await file.text();
    importMutation.mutate(text);
  };

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
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => setImportOpen(true)}>
              <Upload className="h-4 w-4" />
              Importar CSV
            </Button>
            <Link href="/produtos/novo">
              <Button>
                <Plus className="h-4 w-4" />
                Novo produto
              </Button>
            </Link>
          </div>
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
                      <th className="px-6 py-3 font-medium">Produto</th>
                      <th className="px-6 py-3 font-medium">SKU</th>
                      <th className="px-6 py-3 font-medium">Estoque</th>
                      <th className="px-6 py-3 font-medium">Preço</th>
                      <th className="px-6 py-3 font-medium">Status</th>
                      <th className="px-6 py-3 font-medium">Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtered.map((produto) => {
                      const estoque = estoquePorProduto.get(produto.id);
                      const qty = estoque?.quantidadeAtual;
                      const isLow =
                        estoque !== undefined &&
                        qty !== undefined &&
                        qty <= estoque.quantidadeMinima;
                      return (
                      <tr key={produto.id} className="border-b border-slate-100 hover:bg-slate-50">
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            <ProductImage src={produto.imagemUrl} alt={produto.nome} size="sm" />
                            <span className="font-medium">{produto.nome}</span>
                          </div>
                        </td>
                        <td className="px-6 py-4 font-mono text-xs">{produto.sku}</td>
                        <td className="px-6 py-4">
                          {produto.controlaEstoque ? (
                            qty !== undefined ? (
                              <span
                                className={
                                  qty <= 0
                                    ? "font-medium text-red-600"
                                    : isLow
                                      ? "font-medium text-amber-600"
                                      : "text-slate-700"
                                }
                              >
                                {qty} un.{isLow && qty > 0 ? " (baixo)" : ""}
                              </span>
                            ) : (
                              <span className="text-slate-400">—</span>
                            )
                          ) : (
                            <span className="text-slate-400">N/A</span>
                          )}
                        </td>
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
                    );
                    })}
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

      <Modal
        open={importOpen}
        onClose={() => {
          setImportOpen(false);
          setImportResult("");
        }}
        title="Importar produtos (CSV)"
        footer={
          <Button variant="ghost" onClick={() => setImportOpen(false)}>
            Fechar
          </Button>
        }
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            Cabeçalho esperado: sku;nome;preco (opcional: codigo_barras, categoria, preco_custo)
          </p>
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv,text/csv"
            className="block w-full text-sm"
            onChange={(e) => handleImportFile(e.target.files?.[0] ?? null)}
          />
          {importMutation.isPending && <p className="text-sm text-slate-500">Importando...</p>}
          {importResult && <p className="text-sm text-emerald-700">{importResult}</p>}
          {importMutation.error && (
            <ErrorMessage message={(importMutation.error as Error).message} />
          )}
        </div>
      </Modal>
    </div>
  );
}
