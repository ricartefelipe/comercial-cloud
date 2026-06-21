"use client";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import {
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
} from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Select } from "@/components/ui/Select";
import { convidarUsuario, listConvitesPendentes } from "@/lib/api/convites";
import { createUsuario, listUsuarios } from "@/lib/api/usuarios";
import type { UsuarioRequest, UsuarioRole } from "@/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Mail, Plus, Shield } from "lucide-react";
import { FormEvent, useState } from "react";

const ROLE_OPTIONS: { value: UsuarioRole; label: string }[] = [
  { value: "ADMIN", label: "Administrador" },
  { value: "GERENTE", label: "Gerente" },
  { value: "CAIXA", label: "Caixa" },
];

export default function UsuariosPage() {
  const [modalOpen, setModalOpen] = useState(false);
  const [conviteModalOpen, setConviteModalOpen] = useState(false);
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<UsuarioRole>("CAIXA");
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ["usuarios"],
    queryFn: listUsuarios,
  });

  const { data: convites } = useQuery({
    queryKey: ["convites-pendentes"],
    queryFn: listConvitesPendentes,
  });

  const createMutation = useMutation({
    mutationFn: createUsuario,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["usuarios"] });
      setModalOpen(false);
      resetForm();
    },
  });

  const conviteMutation = useMutation({
    mutationFn: convidarUsuario,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["convites-pendentes"] });
      setConviteModalOpen(false);
      resetForm();
    },
  });

  const resetForm = () => {
    setNome("");
    setEmail("");
    setRole("CAIXA");
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const payload: UsuarioRequest = { nome, email, role };
    await createMutation.mutateAsync(payload);
  };

  const handleConvite = async (e: FormEvent) => {
    e.preventDefault();
    const payload: UsuarioRequest = { nome, email, role };
    await conviteMutation.mutateAsync(payload);
  };

  return (
    <div>
      <PageHeader
        title="Usuários"
        description="Gerenciamento de usuários, convites e permissões"
        action={
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" onClick={() => setConviteModalOpen(true)}>
              <Mail className="h-4 w-4" />
              Convidar por e-mail
            </Button>
            <Button onClick={() => setModalOpen(true)}>
              <Plus className="h-4 w-4" />
              Novo usuário
            </Button>
          </div>
        }
      />

      {isLoading && <LoadingSpinner />}
      {error && <ErrorMessage message={(error as Error).message} />}

      {convites && convites.length > 0 && (
        <Card className="mb-6">
          <h2 className="mb-3 font-semibold">Convites pendentes</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-slate-500">
                  <th className="py-2 font-medium">Nome</th>
                  <th className="py-2 font-medium">E-mail</th>
                  <th className="py-2 font-medium">Perfil</th>
                  <th className="py-2 font-medium">Expira em</th>
                </tr>
              </thead>
              <tbody>
                {convites.map((c) => (
                  <tr key={c.id} className="border-b border-slate-100">
                    <td className="py-2">{c.nome}</td>
                    <td className="py-2 text-slate-500">{c.email}</td>
                    <td className="py-2">{c.role}</td>
                    <td className="py-2 text-slate-500">
                      {new Date(c.expiraEm).toLocaleDateString("pt-BR")}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {!isLoading && !error && (
        <>
          {data?.length ? (
            <Card padding="none">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 bg-slate-50 text-left text-slate-500">
                      <th className="px-6 py-3 font-medium">Nome</th>
                      <th className="px-6 py-3 font-medium">E-mail</th>
                      <th className="px-6 py-3 font-medium">Perfil</th>
                      <th className="px-6 py-3 font-medium">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.map((user) => (
                      <tr key={user.id} className="border-b border-slate-100">
                        <td className="px-6 py-4 font-medium">{user.nome}</td>
                        <td className="px-6 py-4 text-slate-500">{user.email}</td>
                        <td className="px-6 py-4">
                          <Badge variant="info">
                            <Shield className="mr-1 inline h-3 w-3" />
                            {user.role}
                          </Badge>
                        </td>
                        <td className="px-6 py-4">
                          <Badge variant={user.ativo ? "success" : "danger"}>
                            {user.ativo ? "Ativo" : "Inativo"}
                          </Badge>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          ) : (
            <EmptyState title="Nenhum usuário cadastrado" />
          )}
        </>
      )}

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title="Novo usuário"
        footer={
          <>
            <Button variant="ghost" onClick={() => setModalOpen(false)}>
              Cancelar
            </Button>
            <Button
              onClick={handleSubmit}
              loading={createMutation.isPending}
              disabled={!nome.trim() || !email.trim()}
            >
              Criar
            </Button>
          </>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input label="Nome" value={nome} onChange={(e) => setNome(e.target.value)} required />
          <Input
            label="E-mail"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <Select
            label="Perfil"
            options={ROLE_OPTIONS}
            value={role}
            onChange={(e) => setRole(e.target.value as UsuarioRole)}
          />
          {createMutation.error && (
            <ErrorMessage message={(createMutation.error as Error).message} />
          )}
        </form>
      </Modal>

      <Modal
        open={conviteModalOpen}
        onClose={() => setConviteModalOpen(false)}
        title="Convidar por e-mail"
        footer={
          <>
            <Button variant="ghost" onClick={() => setConviteModalOpen(false)}>
              Cancelar
            </Button>
            <Button
              onClick={handleConvite}
              loading={conviteMutation.isPending}
              disabled={!nome.trim() || !email.trim()}
            >
              Enviar convite
            </Button>
          </>
        }
      >
        <form onSubmit={handleConvite} className="space-y-4">
          <p className="text-sm text-slate-600">
            A pessoa receberá um link para definir a senha e ativar a conta.
          </p>
          <Input label="Nome" value={nome} onChange={(e) => setNome(e.target.value)} required />
          <Input
            label="E-mail"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <Select
            label="Perfil"
            options={ROLE_OPTIONS}
            value={role}
            onChange={(e) => setRole(e.target.value as UsuarioRole)}
          />
          {conviteMutation.error && (
            <ErrorMessage message={(conviteMutation.error as Error).message} />
          )}
        </form>
      </Modal>
    </div>
  );
}
