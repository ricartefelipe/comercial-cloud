"use client";

import { Card } from "@/components/ui/Card";
import { ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { ProdutoForm } from "@/features/produtos/ProdutoForm";
import { getProduto, updateProduto } from "@/lib/api/produtos";
import type { ProdutoRequest } from "@/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams } from "next/navigation";

export default function EditarProdutoPage() {
  const params = useParams();
  const id = params.id as string;
  const queryClient = useQueryClient();

  const { data: produto, isLoading, error } = useQuery({
    queryKey: ["produto", id],
    queryFn: () => getProduto(id),
  });

  const mutation = useMutation({
    mutationFn: (data: ProdutoRequest) => updateProduto(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["produto", id] });
      queryClient.invalidateQueries({ queryKey: ["produtos"] });
    },
  });

  const handleSubmit = async (data: ProdutoRequest) => {
    await mutation.mutateAsync(data);
  };

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={(error as Error).message} />;
  if (!produto) return <ErrorMessage message="Produto não encontrado" />;

  return (
    <div>
      <PageHeader
        title="Editar produto"
        description={produto.nome}
      />

      <Card>
        <ProdutoForm
          produto={produto}
          onSubmit={handleSubmit}
          loading={mutation.isPending}
        />
      </Card>

      {mutation.error && (
        <div className="mt-4">
          <ErrorMessage message={(mutation.error as Error).message} />
        </div>
      )}
    </div>
  );
}
