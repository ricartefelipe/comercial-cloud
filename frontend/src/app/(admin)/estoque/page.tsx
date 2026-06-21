"use client";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { EmptyState, ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Select } from "@/components/ui/Select";
import {
  ajustarEstoque,
  definirEstoqueMinimo,
  entradaEstoque,
  inventarioEstoque,
  listEstoques,
  listMovimentacoes,
  saidaEstoque,
} from "@/lib/api/estoque";
import { listProdutos } from "@/lib/api/produtos";
import { getLojaId } from "@/lib/auth/tenant-context";
import { formatDateTime } from "@/lib/format";
import { TIPO_MOVIMENTACAO_LABELS } from "@/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, History, Plus } from "lucide-react";
import { useState } from "react";

type OperacaoEstoque = "entrada" | "saida" | "inventario" | "ajuste";

const OPERACAO_LABELS: Record<OperacaoEstoque, string> = {
  entrada: "Entrada de mercadoria",
  saida: "Saída manual",
  inventario: "Inventário (contagem)",
  ajuste: "Ajuste para saldo absoluto",
};

export default function EstoquePage() {
  const [operacaoOpen, setOperacaoOpen] = useState(false);
  const [operacao, setOperacao] = useState<OperacaoEstoque>("entrada");
  const [movementsOpen, setMovementsOpen] = useState(false);
  const [selectedProdutoId, setSelectedProdutoId] = useState("");
  const [quantidade, setQuantidade] = useState("");
  const [motivo, setMotivo] = useState("");
  const [viewProdutoId, setViewProdutoId] = useState("");
  const [minimoTarget, setMinimoTarget] = useState<{ produtoId: string; nome: string; minimo: number } | null>(null);
  const [novoMinimo, setNovoMinimo] = useState("");
  const queryClient = useQueryClient();

  const { data: estoques, isLoading, error } = useQuery({
    queryKey: ["estoques"],
    queryFn: () => listEstoques(),
  });

  const { data: produtos } = useQuery({
    queryKey: ["produtos-all"],
    queryFn: () => listProdutos(0, 100),
  });

  const { data: movimentacoes, isLoading: loadingMov } = useQuery({
    queryKey: ["movimentacoes", viewProdutoId],
    queryFn: () => listMovimentacoes(viewProdutoId),
    enabled: !!viewProdutoId && movementsOpen,
  });

  const operacaoMutation = useMutation({
    mutationFn: async () => {
      const lojaId = getLojaId();
      const payload = {
        lojaId,
        produtoId: selectedProdutoId,
        motivo,
      };
      const qty = Number(quantidade);
      switch (operacao) {
        case "entrada":
          return entradaEstoque({ ...payload, quantidade: qty });
        case "saida":
          return saidaEstoque({ ...payload, quantidade: qty });
        case "inventario":
          return inventarioEstoque({ ...payload, quantidadeContada: qty });
        case "ajuste":
          return ajustarEstoque({ ...payload, quantidade: qty });
        default:
          throw new Error("Operação inválida");
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["estoques"] });
      setOperacaoOpen(false);
      setSelectedProdutoId("");
      setQuantidade("");
      setMotivo("");
    },
  });

  const minimoMutation = useMutation({
    mutationFn: () =>
      definirEstoqueMinimo(getLojaId(), minimoTarget!.produtoId, Number(novoMinimo)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["estoques"] });
      setMinimoTarget(null);
      setNovoMinimo("");
    },
  });

  const lowStock = estoques?.filter(
    (e) => e.quantidadeAtual <= e.quantidadeMinima,
  );

  const produtoOptions = (produtos?.content ?? []).map((p) => ({
    value: p.id,
    label: `${p.sku} - ${p.nome}`,
  }));

  const quantidadeLabel =
    operacao === "ajuste" || operacao === "inventario"
      ? "Quantidade contada (saldo final)"
      : "Quantidade";

  return (
    <div>
      <PageHeader
        title="Estoque"
        description="Entrada, saída, inventário e ajustes por loja"
        action={
          <Button onClick={() => setOperacaoOpen(true)}>
            <Plus className="h-4 w-4" />
            Movimentar estoque
          </Button>
        }
      />

      {lowStock && lowStock.length > 0 && (
        <div className="mb-6 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3">
          <div className="flex items-center gap-2 text-amber-800">
            <AlertTriangle className="h-5 w-5" />
            <span className="text-sm font-medium">
              {lowStock.length} produto(s) com estoque baixo
            </span>
          </div>
        </div>
      )}

      {isLoading && <LoadingSpinner />}
      {error && <ErrorMessage message={(error as Error).message} />}

      {!isLoading && !error && (
        <>
          {estoques?.length ? (
            <Card padding="none">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 bg-slate-50 text-left text-slate-500">
                      <th className="px-6 py-3 font-medium">Produto</th>
                      <th className="px-6 py-3 font-medium">SKU</th>
                      <th className="px-6 py-3 font-medium">Quantidade</th>
                      <th className="px-6 py-3 font-medium">Mínimo</th>
                      <th className="px-6 py-3 font-medium">Status</th>
                      <th className="px-6 py-3 font-medium">Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {estoques.map((item) => {
                      const isLow = item.quantidadeAtual <= item.quantidadeMinima;
                      return (
                        <tr key={item.id} className="border-b border-slate-100 hover:bg-slate-50">
                          <td className="px-6 py-4 font-medium">
                            {item.produtoNome ?? item.produtoId.slice(0, 8)}
                          </td>
                          <td className="px-6 py-4 font-mono text-xs">
                            {item.produtoSku ?? "-"}
                          </td>
                          <td className="px-6 py-4">{item.quantidadeAtual}</td>
                          <td className="px-6 py-4 text-slate-500">
                            <button
                              type="button"
                              className="hover:text-brand-600 hover:underline"
                              onClick={() => {
                                setMinimoTarget({
                                  produtoId: item.produtoId,
                                  nome: item.produtoNome ?? item.produtoId.slice(0, 8),
                                  minimo: item.quantidadeMinima,
                                });
                                setNovoMinimo(String(item.quantidadeMinima));
                              }}
                            >
                              {item.quantidadeMinima}
                            </button>
                          </td>
                          <td className="px-6 py-4">
                            <Badge variant={isLow ? "danger" : "success"}>
                              {isLow ? "Baixo" : "OK"}
                            </Badge>
                          </td>
                          <td className="px-6 py-4">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => {
                                setViewProdutoId(item.produtoId);
                                setMovementsOpen(true);
                              }}
                            >
                              <History className="h-4 w-4" />
                              Movimentações
                            </Button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </Card>
          ) : (
            <EmptyState title="Nenhum registro de estoque" />
          )}
        </>
      )}

      <Modal
        open={operacaoOpen}
        onClose={() => setOperacaoOpen(false)}
        title="Movimentar estoque"
        footer={
          <>
            <Button variant="ghost" onClick={() => setOperacaoOpen(false)}>
              Cancelar
            </Button>
            <Button
              loading={operacaoMutation.isPending}
              onClick={() => operacaoMutation.mutate()}
              disabled={!selectedProdutoId || !quantidade || !motivo}
            >
              Confirmar
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <Select
            label="Tipo de operação"
            options={Object.entries(OPERACAO_LABELS).map(([value, label]) => ({ value, label }))}
            value={operacao}
            onChange={(e) => setOperacao(e.target.value as OperacaoEstoque)}
          />
          <Select
            label="Produto"
            options={[{ value: "", label: "Selecione..." }, ...produtoOptions]}
            value={selectedProdutoId}
            onChange={(e) => setSelectedProdutoId(e.target.value)}
          />
          <Input
            label={quantidadeLabel}
            type="number"
            min="0"
            step="1"
            value={quantidade}
            onChange={(e) => setQuantidade(e.target.value)}
          />
          <Input
            label="Motivo"
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
          />
          {operacaoMutation.error && (
            <ErrorMessage message={(operacaoMutation.error as Error).message} />
          )}
        </div>
      </Modal>

      <Modal
        open={movementsOpen}
        onClose={() => setMovementsOpen(false)}
        title="Movimentações de estoque"
        size="lg"
      >
        {loadingMov && <LoadingSpinner />}
        {movimentacoes?.length ? (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-slate-500">
                  <th className="pb-2 font-medium">Tipo</th>
                  <th className="pb-2 font-medium">Qtd</th>
                  <th className="pb-2 font-medium">Anterior</th>
                  <th className="pb-2 font-medium">Posterior</th>
                  <th className="pb-2 font-medium">Data</th>
                </tr>
              </thead>
              <tbody>
                {movimentacoes.map((mov) => (
                  <tr key={mov.id} className="border-b border-slate-100">
                    <td className="py-2">
                      <Badge>{TIPO_MOVIMENTACAO_LABELS[mov.tipo]}</Badge>
                    </td>
                    <td className="py-2">{mov.quantidade}</td>
                    <td className="py-2">{mov.quantidadeAnterior ?? "-"}</td>
                    <td className="py-2">{mov.quantidadePosterior ?? "-"}</td>
                    <td className="py-2 text-slate-500">
                      {formatDateTime(mov.createdAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          !loadingMov && <p className="text-sm text-slate-500">Nenhuma movimentação.</p>
        )}
      </Modal>

      <Modal
        open={!!minimoTarget}
        onClose={() => setMinimoTarget(null)}
        title="Estoque mínimo"
        footer={
          <>
            <Button variant="ghost" onClick={() => setMinimoTarget(null)}>
              Cancelar
            </Button>
            <Button
              loading={minimoMutation.isPending}
              onClick={() => minimoMutation.mutate()}
              disabled={!novoMinimo}
            >
              Salvar
            </Button>
          </>
        }
      >
        {minimoTarget && (
          <div className="space-y-4">
            <p className="text-sm text-slate-600">Produto: {minimoTarget.nome}</p>
            <Input
              label="Quantidade mínima"
              type="number"
              min="0"
              step="1"
              value={novoMinimo}
              onChange={(e) => setNovoMinimo(e.target.value)}
            />
            {minimoMutation.error && (
              <ErrorMessage message={(minimoMutation.error as Error).message} />
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
