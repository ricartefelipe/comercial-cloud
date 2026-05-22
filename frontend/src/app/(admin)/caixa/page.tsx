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
} from "@/lib/api/caixa";
import { LOJA_ID } from "@/lib/constants";
import { STATUS_CAIXA_LABELS } from "@/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Lock, LockOpen, Wallet } from "lucide-react";
import { useState } from "react";

export default function CaixaPage() {
  const [openModal, setOpenModal] = useState(false);
  const [closeModal, setCloseModal] = useState(false);
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
      setCloseModal(false);
    },
  });

  if (isLoading) return <LoadingSpinner />;

  return (
    <div>
      <PageHeader
        title="Caixa"
        description="Abertura e fechamento de caixa"
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
            <StatCard
              title="Valor abertura"
              value={formatCurrency(caixa.valorAbertura)}
            />
            <StatCard
              title="Total vendas"
              value={formatCurrency(resumo?.totalVendas ?? 0)}
            />
            <StatCard
              title="Qtd. vendas"
              value={String(resumo?.quantidadeVendas ?? 0)}
            />
            <StatCard
              title="Valor calculado"
              value={formatCurrency(resumo?.valorCalculado ?? caixa.valorCalculado ?? 0)}
            />
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
              </div>
            </Card>
          )}
        </>
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
                  lojaId: LOJA_ID,
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
            Valor calculado: {formatCurrency(resumo.valorCalculado)}
          </p>
        )}
        {fecharMutation.error && (
          <div className="mt-4">
            <ErrorMessage message={(fecharMutation.error as Error).message} />
          </div>
        )}
      </Modal>
    </div>
  );
}
