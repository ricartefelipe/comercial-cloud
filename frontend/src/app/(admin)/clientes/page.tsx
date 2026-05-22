"use client";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { EmptyState, ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { Modal } from "@/components/ui/Modal";
import { ClienteForm } from "@/features/clientes/ClienteForm";
import { createCliente, listClientes, updateCliente } from "@/lib/api/clientes";
import type { Cliente, ClienteRequest } from "@/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useState } from "react";

export default function ClientesPage() {
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Cliente | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ["clientes"],
    queryFn: () => listClientes(),
  });

  const createMutation = useMutation({
    mutationFn: createCliente,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["clientes"] });
      setModalOpen(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: ClienteRequest }) =>
      updateCliente(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["clientes"] });
      setEditing(null);
    },
  });

  const handleCreate = async (formData: ClienteRequest) => {
    await createMutation.mutateAsync(formData);
  };

  const handleUpdate = async (formData: ClienteRequest) => {
    if (editing) {
      await updateMutation.mutateAsync({ id: editing.id, data: formData });
    }
  };

  const openCreate = () => {
    setEditing(null);
    setModalOpen(true);
  };

  const openEdit = (cliente: Cliente) => {
    setEditing(cliente);
    setModalOpen(true);
  };

  return (
    <div>
      <PageHeader
        title="Clientes"
        description="Cadastro de clientes"
        action={
          <Button onClick={openCreate}>
            <Plus className="h-4 w-4" />
            Novo cliente
          </Button>
        }
      />

      {isLoading && <LoadingSpinner />}
      {error && <ErrorMessage message={(error as Error).message} />}

      {!isLoading && !error && (
        <>
          {data?.content?.length ? (
            <Card padding="none">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 bg-slate-50 text-left text-slate-500">
                      <th className="px-6 py-3 font-medium">Nome</th>
                      <th className="px-6 py-3 font-medium">CPF/CNPJ</th>
                      <th className="px-6 py-3 font-medium">E-mail</th>
                      <th className="px-6 py-3 font-medium">Telefone</th>
                      <th className="px-6 py-3 font-medium">Status</th>
                      <th className="px-6 py-3 font-medium">Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.content.map((cliente) => (
                      <tr key={cliente.id} className="border-b border-slate-100 hover:bg-slate-50">
                        <td className="px-6 py-4 font-medium">{cliente.nome}</td>
                        <td className="px-6 py-4 text-slate-500">{cliente.cpfCnpj ?? "-"}</td>
                        <td className="px-6 py-4 text-slate-500">{cliente.email ?? "-"}</td>
                        <td className="px-6 py-4 text-slate-500">{cliente.telefone ?? "-"}</td>
                        <td className="px-6 py-4">
                          <Badge variant={cliente.ativo ? "success" : "default"}>
                            {cliente.ativo ? "Ativo" : "Inativo"}
                          </Badge>
                        </td>
                        <td className="px-6 py-4">
                          <Button variant="ghost" size="sm" onClick={() => openEdit(cliente)}>
                            Editar
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          ) : (
            <EmptyState
              title="Nenhum cliente cadastrado"
              action={<Button onClick={openCreate}>Novo cliente</Button>}
            />
          )}
        </>
      )}

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title="Novo cliente"
      >
        <ClienteForm
          onSubmit={handleCreate}
          loading={createMutation.isPending}
          onCancel={() => setModalOpen(false)}
        />
        {createMutation.error && (
          <div className="mt-4">
            <ErrorMessage message={(createMutation.error as Error).message} />
          </div>
        )}
      </Modal>

      <Modal
        open={!!editing}
        onClose={() => setEditing(null)}
        title="Editar cliente"
      >
        {editing && (
          <ClienteForm
            cliente={editing}
            onSubmit={handleUpdate}
            loading={updateMutation.isPending}
            onCancel={() => setEditing(null)}
          />
        )}
        {updateMutation.error && (
          <div className="mt-4">
            <ErrorMessage message={(updateMutation.error as Error).message} />
          </div>
        )}
      </Modal>
    </div>
  );
}
