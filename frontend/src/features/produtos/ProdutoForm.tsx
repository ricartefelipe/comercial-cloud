"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import type { Produto, ProdutoRequest } from "@/types";

const produtoSchema = z.object({
  sku: z.string().min(1, "SKU obrigatório"),
  codigoBarras: z.string().optional(),
  nome: z.string().min(1, "Nome obrigatório"),
  descricao: z.string().optional(),
  categoria: z.string().optional(),
  unidadeMedida: z.string().min(1, "Unidade obrigatória"),
  precoVenda: z.coerce.number().min(0, "Preço inválido"),
  precoCusto: z.coerce.number().min(0).optional(),
  controlaEstoque: z.boolean(),
});

type ProdutoFormData = z.infer<typeof produtoSchema>;

interface ProdutoFormProps {
  produto?: Produto;
  onSubmit: (data: ProdutoRequest) => Promise<void>;
  loading?: boolean;
}

export function ProdutoForm({ produto, onSubmit, loading }: ProdutoFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ProdutoFormData>({
    resolver: zodResolver(produtoSchema),
    defaultValues: {
      sku: produto?.sku ?? "",
      codigoBarras: produto?.codigoBarras ?? "",
      nome: produto?.nome ?? "",
      descricao: produto?.descricao ?? "",
      categoria: produto?.categoria ?? "",
      unidadeMedida: produto?.unidadeMedida ?? "UN",
      precoVenda: produto?.precoVenda ?? 0,
      precoCusto: produto?.precoCusto ?? 0,
      controlaEstoque: produto?.controlaEstoque ?? true,
    },
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <Input label="SKU" error={errors.sku?.message} {...register("sku")} />
        <Input
          label="Código de barras"
          error={errors.codigoBarras?.message}
          {...register("codigoBarras")}
        />
        <Input
          label="Nome"
          error={errors.nome?.message}
          className="sm:col-span-2"
          {...register("nome")}
        />
        <Input
          label="Descrição"
          error={errors.descricao?.message}
          className="sm:col-span-2"
          {...register("descricao")}
        />
        <Input label="Categoria" error={errors.categoria?.message} {...register("categoria")} />
        <Input
          label="Unidade de medida"
          error={errors.unidadeMedida?.message}
          {...register("unidadeMedida")}
        />
        <Input
          label="Preço de venda"
          type="number"
          step="0.01"
          error={errors.precoVenda?.message}
          {...register("precoVenda")}
        />
        <Input
          label="Preço de custo"
          type="number"
          step="0.01"
          error={errors.precoCusto?.message}
          {...register("precoCusto")}
        />
      </div>

      <label className="flex items-center gap-2 text-sm text-slate-700">
        <input type="checkbox" className="rounded border-slate-300" {...register("controlaEstoque")} />
        Controla estoque
      </label>

      <div className="flex justify-end gap-3 pt-4">
        <Button type="submit" loading={loading}>
          Salvar produto
        </Button>
      </div>
    </form>
  );
}
