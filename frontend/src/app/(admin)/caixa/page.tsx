"use client";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader } from "@/components/ui/Card";
import { ErrorMessage, LoadingSpinner, PageHeader, StatCard } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { formatCurrency, formatDateTime } from "@/lib/format";
import {
  abrirCaixa,
  fecharCaixa,
  getCaixaAberto,
  getCaixaResumo,
  listHistoricoCaixas,
  listMovimentosCaixa,
  registrarMovimentoCaixa,
} from "@/lib/api/caixa";
import { getLojaId } from "@/lib/auth/tenant-context";
import { STATUS_CAIXA_LABELS, TIPO_MOVIMENTO_CAIXA_LABELS, type TipoMovimentoCaixa } from "@/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowDownCircle, ArrowUpCircle, Lock, LockOpen, Wallet } from "lucide-react";
import { useMemo, useState } from "react";

export default function CaixaPage() {
  const [openModal, setOpenModal] = useState(false);
  const [closeModal, setCloseModal] = useState(false);
  const [movimentoModal, setMovimentoModal] = useState<TipoMovimentoCaixa | null>(null);
  const [movimentoValor, setMovimentoValor] = useState("");
  const [movimentoMotivo, setMovimentoMotivo] = useState("");
  const [valorAbertura, setValorAbertura] = useState("100");
  const [valorFechamento, setValorFechamento] = useState("");
  const queryClient = useQueryClient();

  const { data: caixa, isLoading, error } = useQuery({
    queryKey: ["caixa-aberto"],
    queryFn: () => getCaixaAberto(),
  });

  const { data: resumo } = useQuery({
    queryKey: ["caixa-resumo", caixa?.id],
    queryFn: () => getCaixaResumo(caixa!.id),
    enabled: !!caixa?.id,
  });

  const { data: historico } = useQuery({
    queryKey: ["caixa-historico"],
    queryFn: () => listHistoricoCaixas(),
  });

  const { data: movimentos } = useQuery({
    queryKey: ["caixa-movimentos", caixa?.id],
    queryFn: () => listMovimentosCaixa(caixa!.id),
    enabled: !!caixa?.id,
  });

  const abrirMutation = useMutation({
    mutationFn: abrirCaixa,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["caixa-aberto"] });
      setOpenModal(false);
    },
  });

  const fecharMutation = useMutation({
    mutationFn: (valor: number) => fecharCaixa(caixa!.id, { valorFechamentoInformado: valor }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["caixa-aberto"] });
      queryClient.invalidateQueries({ queryKey: ["caixa-resumo"] });
      queryClient.invalidateQueries({ queryKey: ["caixa-historico"] });
      setCloseModal(false);
      setValorFechamento("");
    },
  });

  const movimentoMutation = useMutation({
    mutationFn: () =>
      registrarMovimentoCaixa(caixa!.id, {
        tipo: movimentoModal!,
        valor: Number(movimentoValor),
        motivo: movimentoMotivo,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["caixa-resumo"] });
      queryClient.invalidateQueries({ queryKey: ["caixa-movimentos"] });
      setMovimentoModal(null);
      setMovimentoValor("");
      setMovimentoMotivo("");
    },
  });

  const valorCalculadoFechamento = resumo?.valorGavetaEsperada ?? resumo?.valorCalculado ?? 0;
  const diferencaPreview = useMemo(() => {
    const informado = Number(valorFechamento);
    if (!valorFechamento || Number.isNaN(informado)) return null;
    return Math.round((informado - valorCalculadoFechamento) * 100) / 100;
  }, [valorFechamento, valorCalculadoFechamento]);

  if (isLoading) return <LoadingSpinner />;

  return (
    <div>
      <PageHeader
        title="Caixa"
        description="Abertura, fechamento e conciliação"
        action={
          caixa ? (
            <Button variant="danger" onClick={() => setCloseModal(true)}>
              <Lock className="h-4 w-4" />
              Fechar caixa
            </Button>
          ) : (
            <Button onClick={() => setOpenModal(true)}>
              <LockOpen className="h-4 w-4" />
              Abrir caixa
            </Button>
          )
        }
      />

      {error && !caixa && (
        <ErrorMessage message={(error as Error).message} />
      )}

      {!caixa ? (
        <Card className="text-center py-12">
          <Wallet className="mx-auto h-12 w-12 text-slate-300" />
          <p className="mt-4 text-lg font-medium text-slate-900">Caixa fechado</p>
          <p className="mt-1 text-sm text-slate-500">
            Abra o caixa para iniciar as operações do dia.
          </p>
          <Button className="mt-6" onClick={() => setOpenModal(true)}>
            Abrir caixa
          </Button>
        </Card>
      ) : (
        <>
          <div className="mb-6 flex items-center gap-3">
            <Badge variant="success" className="text-sm px-3 py-1">
              {STATUS_CAIXA_LABELS[caixa.status]}
            </Badge>
            <span className="text-sm text-slate-500">
              Aberto em {formatDateTime(caixa.openedAt)}
            </span>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard title="Valor abertura" value={formatCurrency(caixa.valorAbertura)} />
            <StatCard title="Total vendas" value={formatCurrency(resumo?.totalVendas ?? 0)} />
            <StatCard title="Gaveta esperada" value={formatCurrency(resumo?.valorGavetaEsperada ?? 0)} />
            <StatCard
              title="Valor calculado (geral)"
              value={formatCurrency(resumo?.valorCalculado ?? caixa.valorCalculado ?? 0)}
            />
          </div>

          <div className="mt-4 flex flex-wrap gap-2">
            <Button variant="outline" size="sm" onClick={() => setMovimentoModal("SUPRIMENTO")}>
              <ArrowUpCircle className="h-4 w-4" />
              Suprimento
            </Button>
            <Button variant="outline" size="sm" onClick={() => setMovimentoModal("SANGRIA")}>
              <ArrowDownCircle className="h-4 w-4" />
              Sangria
            </Button>
          </div>

          {resumo && (
            <Card className="mt-6">
              <CardHeader title="Resumo por forma de pagamento" />
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Dinheiro</p>
                  <p className="text-lg font-semibold">{formatCurrency(resumo.totalDinheiro)}</p>
                </div>
                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">PIX</p>
                  <p className="text-lg font-semibold">{formatCurrency(resumo.totalPix)}</p>
                </div>
                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Cartão</p>
                  <p className="text-lg font-semibold">{formatCurrency(resumo.totalCartao)}</p>
                </div>
                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Outros</p>
                  <p className="text-lg font-semibold">{formatCurrency(resumo.totalOutros)}</p>
                </div>
                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Sangrias</p>
                  <p className="text-lg font-semibold">{formatCurrency(resumo.totalSangrias ?? 0)}</p>
                </div>
                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Suprimentos</p>
                  <p className="text-lg font-semibold">{formatCurrency(resumo.totalSuprimentos ?? 0)}</p>
                </div>
              </div>
            </Card>
          )}

          {movimentos && movimentos.length > 0 && (
            <Card className="mt-6" padding="none">
              <CardHeader title="Movimentos de caixa" />
              <div className="px-6 pb-6 overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 text-left text-slate-500">
                      <th className="py-2 font-medium">Tipo</th>
                      <th className="py-2 font-medium">Valor</th>
                      <th className="py-2 font-medium">Motivo</th>
                      <th className="py-2 font-medium">Data</th>
                    </tr>
                  </thead>
                  <tbody>
                    {movimentos.map((mov) => (
                      <tr key={mov.id} className="border-b border-slate-100">
                        <td className="py-2">{TIPO_MOVIMENTO_CAIXA_LABELS[mov.tipo]}</td>
                        <td className="py-2">{formatCurrency(mov.valor)}</td>
                        <td className="py-2">{mov.motivo ?? "—"}</td>
                        <td className="py-2 text-slate-500">{formatDateTime(mov.createdAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )}
        </>
      )}

      {historico && historico.length > 0 && (
        <Card className="mt-6" padding="none">
          <CardHeader title="Histórico de fechamentos" />
          <div className="px-6 pb-6 overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-left text-slate-500">
                  <th className="px-6 py-3 font-medium">Fechado em</th>
                  <th className="px-6 py-3 font-medium">Abertura</th>
                  <th className="px-6 py-3 font-medium">Calculado</th>
                  <th className="px-6 py-3 font-medium">Informado</th>
                  <th className="px-6 py-3 font-medium">Diferença</th>
                </tr>
              </thead>
              <tbody>
                {historico.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100">
                    <td className="px-6 py-3">
                      {item.closedAt ? formatDateTime(item.closedAt) : "—"}
                    </td>
                    <td className="px-6 py-3">{formatCurrency(item.valorAbertura)}</td>
                    <td className="px-6 py-3">
                      {formatCurrency(item.valorCalculado ?? item.valorAbertura)}
                    </td>
                    <td className="px-6 py-3">
                      {formatCurrency(item.valorFechamentoInformado ?? 0)}
                    </td>
                    <td className="px-6 py-3">
                      {item.diferenca !== undefined ? (
                        <span
                          className={
                            item.diferenca === 0
                              ? "text-emerald-600"
                              : item.diferenca > 0
                                ? "text-amber-600"
                                : "text-red-600"
                          }
                        >
                          {formatCurrency(item.diferenca)}
                        </span>
                      ) : (
                        "—"
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <Modal
        open={openModal}
        onClose={() => setOpenModal(false)}
        title="Abrir caixa"
        footer={
          <>
            <Button variant="ghost" onClick={() => setOpenModal(false)}>
              Cancelar
            </Button>
            <Button
              loading={abrirMutation.isPending}
              onClick={() =>
                abrirMutation.mutate({
                  lojaId: getLojaId(),
                  valorAbertura: Number(valorAbertura),
                })
              }
            >
              Confirmar abertura
            </Button>
          </>
        }
      >
        <Input
          label="Valor de abertura (R$)"
          type="number"
          step="0.01"
          value={valorAbertura}
          onChange={(e) => setValorAbertura(e.target.value)}
        />
        {abrirMutation.error && (
          <div className="mt-4">
            <ErrorMessage message={(abrirMutation.error as Error).message} />
          </div>
        )}
      </Modal>

      <Modal
        open={closeModal}
        onClose={() => setCloseModal(false)}
        title="Fechar caixa"
        footer={
          <>
            <Button variant="ghost" onClick={() => setCloseModal(false)}>
              Cancelar
            </Button>
            <Button
              variant="danger"
              loading={fecharMutation.isPending}
              onClick={() => fecharMutation.mutate(Number(valorFechamento))}
              disabled={!valorFechamento}
            >
              Confirmar fechamento
            </Button>
          </>
        }
      >
        <Input
          label="Valor informado no fechamento (R$)"
          type="number"
          step="0.01"
          value={valorFechamento}
          onChange={(e) => setValorFechamento(e.target.value)}
        />
        {resumo && (
          <p className="mt-2 text-sm text-slate-500">
            Gaveta esperada (dinheiro): {formatCurrency(resumo.valorGavetaEsperada ?? resumo.valorCalculado)}
          </p>
        )}
        {diferencaPreview !== null && (
          <p
            className={`mt-2 text-sm font-medium ${
              diferencaPreview === 0
                ? "text-emerald-600"
                : diferencaPreview > 0
                  ? "text-amber-600"
                  : "text-red-600"
            }`}
          >
            Diferença: {formatCurrency(diferencaPreview)}
            {diferencaPreview > 0 ? " (sobra)" : diferencaPreview < 0 ? " (falta)" : ""}
          </p>
        )}
        {fecharMutation.error && (
          <div className="mt-4">
            <ErrorMessage message={(fecharMutation.error as Error).message} />
          </div>
        )}
      </Modal>

      <Modal
        open={!!movimentoModal}
        onClose={() => setMovimentoModal(null)}
        title={movimentoModal ? TIPO_MOVIMENTO_CAIXA_LABELS[movimentoModal] : "Movimento"}
        footer={
          <>
            <Button variant="ghost" onClick={() => setMovimentoModal(null)}>
              Cancelar
            </Button>
            <Button
              loading={movimentoMutation.isPending}
              onClick={() => movimentoMutation.mutate()}
              disabled={!movimentoValor || !movimentoMotivo}
            >
              Confirmar
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <Input
            label="Valor (R$)"
            type="number"
            step="0.01"
            min="0.01"
            value={movimentoValor}
            onChange={(e) => setMovimentoValor(e.target.value)}
          />
          <Input
            label="Motivo"
            value={movimentoMotivo}
            onChange={(e) => setMovimentoMotivo(e.target.value)}
          />
          {movimentoMutation.error && (
            <ErrorMessage message={(movimentoMutation.error as Error).message} />
          )}
        </div>
      </Modal>
    </div>
  );
}
