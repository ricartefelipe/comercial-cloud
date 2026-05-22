"use client";

import { Card } from "@/components/ui/Card";
import { PageHeader } from "@/components/ui/Common";
import { LOJA_ID, TENANT_ID } from "@/lib/constants";
import { Bell, Building2, CreditCard, Printer, Settings, Store } from "lucide-react";

const sections = [
  {
    icon: Building2,
    title: "Empresa",
    description: "Dados cadastrais e informações fiscais",
    items: ["Razão social", "CNPJ", "Inscrição estadual"],
  },
  {
    icon: Store,
    title: "Loja",
    description: "Configurações da loja principal",
    items: ["Nome da loja", "Endereço", "Horário de funcionamento"],
  },
  {
    icon: CreditCard,
    title: "Pagamentos",
    description: "Formas de pagamento aceitas",
    items: ["Dinheiro", "PIX", "Cartão débito/crédito"],
  },
  {
    icon: Printer,
    title: "Impressão",
    description: "Configurações de impressora e comprovantes",
    items: ["Impressora térmica", "Layout do cupom"],
  },
  {
    icon: Bell,
    title: "Notificações",
    description: "Alertas de estoque e vendas",
    items: ["Estoque baixo", "Fechamento de caixa"],
  },
];

export default function ConfiguracoesPage() {
  return (
    <div>
      <PageHeader
        title="Configurações"
        description="Preferências do sistema"
      />

      <Card className="mb-6">
        <div className="flex items-center gap-3">
          <Settings className="h-5 w-5 text-brand-600" />
          <div>
            <p className="text-sm font-medium text-slate-900">Ambiente atual</p>
            <p className="text-xs text-slate-500">
              Tenant: {TENANT_ID.slice(0, 8)}... | Loja: {LOJA_ID.slice(0, 8)}...
            </p>
          </div>
        </div>
      </Card>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {sections.map((section) => {
          const Icon = section.icon;
          return (
            <Card key={section.title} className="hover:border-brand-200 transition-colors cursor-pointer">
              <div className="flex items-start gap-3">
                <div className="rounded-lg bg-brand-50 p-2">
                  <Icon className="h-5 w-5 text-brand-600" />
                </div>
                <div>
                  <h3 className="font-semibold text-slate-900">{section.title}</h3>
                  <p className="mt-1 text-sm text-slate-500">{section.description}</p>
                  <ul className="mt-3 space-y-1">
                    {section.items.map((item) => (
                      <li key={item} className="text-xs text-slate-400">
                        • {item}
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
