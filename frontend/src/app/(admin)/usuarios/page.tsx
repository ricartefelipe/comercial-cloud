"use client";

import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { PageHeader } from "@/components/ui/Common";
import { Settings, Shield, Users } from "lucide-react";

const perfis = [
  { nome: "Administrador", perfil: "ADMIN", status: "Ativo" },
  { nome: "Gerente Loja", perfil: "GERENTE", status: "Ativo" },
  { nome: "Operador Caixa", perfil: "CAIXA", status: "Ativo" },
];

export default function UsuariosPage() {
  return (
    <div>
      <PageHeader
        title="Usuários"
        description="Gerenciamento de usuários e permissões"
      />

      <Card className="mb-6 border-dashed">
        <div className="flex items-center gap-4 text-slate-500">
          <Users className="h-8 w-8" />
          <div>
            <p className="font-medium text-slate-700">Módulo em desenvolvimento</p>
            <p className="text-sm">
              A gestão completa de usuários será integrada com autenticação Keycloak.
            </p>
          </div>
        </div>
      </Card>

      <Card padding="none">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50 text-left text-slate-500">
                <th className="px-6 py-3 font-medium">Nome</th>
                <th className="px-6 py-3 font-medium">Perfil</th>
                <th className="px-6 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {perfis.map((user) => (
                <tr key={user.nome} className="border-b border-slate-100">
                  <td className="px-6 py-4 font-medium">{user.nome}</td>
                  <td className="px-6 py-4">
                    <Badge variant="info">
                      <Shield className="mr-1 inline h-3 w-3" />
                      {user.perfil}
                    </Badge>
                  </td>
                  <td className="px-6 py-4">
                    <Badge variant="success">{user.status}</Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
