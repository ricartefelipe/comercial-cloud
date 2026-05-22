"use client";

import { Card } from "@/components/ui/Card";
import { ErrorMessage, PageHeader } from "@/components/ui/Common";
import { ProdutoForm } from "@/features/produtos/ProdutoForm";
import { createProduto } from "@/lib/api/produtos";
import type { ProdutoRequest } from "@/types";
import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";

export default function NovoProdutoPage() {
  const router = useRouter();

  const mutation = useMutation({
    mutationFn: createProduto,
    onSuccess: (produto) => router.push(`/produtos/${produto.id}`),
  });

  const handleSubmit = async (data: ProdutoRequest) => {
    await mutation.mutateAsync(data);
  };

  return (
    <div>
      <PageHeader title="Novo produto" description="Cadastre um novo item no catálogo" />

      <Card>
        <ProdutoForm onSubmit={handleSubmit} loading={mutation.isPending} />
      </Card>

      {mutation.error && (
        <div className="mt-4">
          <ErrorMessage message={(mutation.error as Error).message} />
        </div>
      )}
    </div>
  );
}
