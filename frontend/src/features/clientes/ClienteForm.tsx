"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import type { Cliente, ClienteRequest } from "@/types";

const clienteSchema = z.object({
  nome: z.string().min(1, "Nome obrigatório"),
  cpfCnpj: z.string().optional(),
  email: z.string().email("E-mail inválido").optional().or(z.literal("")),
  telefone: z.string().optional(),
});

type ClienteFormData = z.infer<typeof clienteSchema>;

interface ClienteFormProps {
  cliente?: Cliente;
  onSubmit: (data: ClienteRequest) => Promise<void>;
  loading?: boolean;
  onCancel?: () => void;
}

export function ClienteForm({ cliente, onSubmit, loading, onCancel }: ClienteFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ClienteFormData>({
    resolver: zodResolver(clienteSchema),
    defaultValues: {
      nome: cliente?.nome ?? "",
      cpfCnpj: cliente?.cpfCnpj ?? "",
      email: cliente?.email ?? "",
      telefone: cliente?.telefone ?? "",
    },
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <Input label="Nome" error={errors.nome?.message} {...register("nome")} />
      <Input label="CPF/CNPJ" error={errors.cpfCnpj?.message} {...register("cpfCnpj")} />
      <Input label="E-mail" type="email" error={errors.email?.message} {...register("email")} />
      <Input label="Telefone" error={errors.telefone?.message} {...register("telefone")} />

      <div className="flex justify-end gap-3 pt-2">
        {onCancel && (
          <Button type="button" variant="ghost" onClick={onCancel}>
            Cancelar
          </Button>
        )}
        <Button type="submit" loading={loading}>
          Salvar cliente
        </Button>
      </div>
    </form>
  );
}
