"use client";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { EmptyState, ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Select } from "@/components/ui/Select";
import { ajustarEstoque, listEstoques, listMovimentacoes } from "@/lib/api/estoque";
import { listProdutos } from "@/lib/api/produtos";
import { LOJA_ID } from "@/lib/constants";
import { formatDateTime } from "@/lib/format";
import { TIPO_MOVIMENTACAO_LABELS } from "@/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, History, Plus } from "lucide-react";
import { useState } from "react";

export default function EstoquePage() {
  const [adjustOpen, setAdjustOpen] = useState(false);
  const [movementsOpen, setMovementsOpen] = useState(false);
  const [selectedProdutoId, setSelectedProdutoId] = useState("");
  const [quantidade, setQuantidade] = useState("");
  const [motivo, setMotivo] = useState("");
  const [viewProdutoId, setViewProdutoId] = useState("");
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

  const adjustMutation = useMutation({
    mutationFn: ajustarEstoque,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["estoques"] });
      setAdjustOpen(false);
      setSelectedProdutoId("");
      setQuantidade("");
      setMotivo("");
    },
  });

  const lowStock = estoques?.filter(
    (e) => e.quantidadeAtual <= e.quantidadeMinima,
  );

  const produtoOptions = (produtos?.content ?? []).map((p) => ({
    value: p.id,
    label: `${p.sku} - ${p.nome}`,
  }));

  return (
    <div>
      <PageHeader
        title="Estoque"
        description="Controle de estoque por loja"
        action={
          <Button onClick={() => setAdjustOpen(true)}>
            <Plus className="h-4 w-4" />
            Ajustar estoque
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
                          <td className="px-6 py-4 text-slate-500">{item.quantidadeMinima}</td>
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
        open={adjustOpen}
        onClose={() => setAdjustOpen(false)}
        title="Ajustar estoque"
        footer={
          <>
            <Button variant="ghost" onClick={() => setAdjustOpen(false)}>
              Cancelar
            </Button>
            <Button
              loading={adjustMutation.isPending}
              onClick={() =>
                adjustMutation.mutate({
                  lojaId: LOJA_ID,
                  produtoId: selectedProdutoId,
                  quantidade: Number(quantidade),
                  motivo,
                })
              }
              disabled={!selectedProdutoId || !quantidade || !motivo}
            >
              Confirmar ajuste
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <Select
            label="Produto"
            options={[{ value: "", label: "Selecione..." }, ...produtoOptions]}
            value={selectedProdutoId}
            onChange={(e) => setSelectedProdutoId(e.target.value)}
          />
          <Input
            label="Quantidade (positivo = entrada, negativo = saída)"
            type="number"
            value={quantidade}
            onChange={(e) => setQuantidade(e.target.value)}
          />
          <Input
            label="Motivo"
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
          />
          {adjustMutation.error && (
            <ErrorMessage message={(adjustMutation.error as Error).message} />
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
    </div>
  );
}
