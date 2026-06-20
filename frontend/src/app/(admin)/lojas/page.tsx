"use client";

import { Button } from "@/components/ui/Button";
import { Card, CardHeader } from "@/components/ui/Card";
import { ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import {
  atualizarLoja,
  criarLoja,
  getLojasResumo,
  listLojas,
} from "@/lib/api/lojas";
import { getLojaId, setLojaId } from "@/lib/auth/tenant-context";
import type { Loja } from "@/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Store } from "lucide-react";
import { FormEvent, useState } from "react";

export default function LojasPage() {
  const queryClient = useQueryClient();
  const [nomeNova, setNomeNova] = useState("");

  const { data: lojas = [], isLoading, error } = useQuery({
    queryKey: ["lojas"],
    queryFn: listLojas,
  });

  const { data: resumo } = useQuery({
    queryKey: ["lojas-resumo"],
    queryFn: getLojasResumo,
  });

  const criarMutation = useMutation({
    mutationFn: (nome: string) => criarLoja(nome),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["lojas"] });
      queryClient.invalidateQueries({ queryKey: ["lojas-resumo"] });
      setNomeNova("");
    },
  });

  const atualizarMutation = useMutation({
    mutationFn: ({ id, ativa }: { id: string; ativa: boolean }) =>
      atualizarLoja(id, { ativa }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["lojas"] });
      queryClient.invalidateQueries({ queryKey: ["lojas-resumo"] });
    },
  });

  const handleCriar = async (e: FormEvent) => {
    e.preventDefault();
    if (!nomeNova.trim()) return;
    await criarMutation.mutateAsync(nomeNova.trim());
  };

  const selecionar = (loja: Loja) => {
    setLojaId(loja.id);
    queryClient.clear();
    window.location.reload();
  };

  return (
    <div>
      <PageHeader
        title="Lojas"
        description="Gerencie filiais do tenant (limites conforme plano)"
      />

      {resumo && (
        <Card className="mb-6">
          <p className="text-sm text-slate-600">
            Plano permite <strong>{resumo.limite}</strong> loja(s) ativa(s). Em uso:{" "}
            <strong>{resumo.ativas}</strong>.
            {!resumo.podeCriar && " Faça upgrade para Premium para ampliar."}
          </p>
        </Card>
      )}

      {isLoading && <LoadingSpinner />}
      {error && <ErrorMessage message={(error as Error).message} />}

      <div className="mb-6 grid gap-4 md:grid-cols-2">
        {lojas.map((loja) => (
          <Card key={loja.id}>
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <Store className="h-5 w-5 text-brand-600" />
                <div>
                  <p className="font-medium text-slate-900">{loja.nome}</p>
                  <p className="text-xs text-slate-500">
                    {loja.ativa ? "Ativa" : "Inativa"}
                    {getLojaId() === loja.id ? " · selecionada" : ""}
                  </p>
                </div>
              </div>
              <div className="flex gap-2">
                {loja.ativa && getLojaId() !== loja.id && (
                  <Button size="sm" variant="outline" onClick={() => selecionar(loja)}>
                    Usar
                  </Button>
                )}
                {loja.ativa && lojas.filter((l) => l.ativa).length > 1 && (
                  <Button
                    size="sm"
                    variant="outline"
                    loading={atualizarMutation.isPending}
                    onClick={() => atualizarMutation.mutate({ id: loja.id, ativa: false })}
                  >
                    Desativar
                  </Button>
                )}
                {!loja.ativa && (
                  <Button
                    size="sm"
                    loading={atualizarMutation.isPending}
                    onClick={() => atualizarMutation.mutate({ id: loja.id, ativa: true })}
                  >
                    Reativar
                  </Button>
                )}
              </div>
            </div>
          </Card>
        ))}
      </div>

      {resumo?.podeCriar && (
        <Card>
          <CardHeader title="Nova loja" />
          <form onSubmit={handleCriar} className="flex flex-col gap-3 sm:flex-row">
            <Input
              label="Nome da filial"
              value={nomeNova}
              onChange={(e) => setNomeNova(e.target.value)}
              placeholder="Filial Centro"
              className="flex-1"
            />
            <Button type="submit" loading={criarMutation.isPending} className="self-end">
              <Plus className="h-4 w-4" />
              Adicionar
            </Button>
          </form>
          {criarMutation.error && (
            <div className="mt-3">
              <ErrorMessage message={(criarMutation.error as Error).message} />
            </div>
          )}
        </Card>
      )}
    </div>
  );
}
