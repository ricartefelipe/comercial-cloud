"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  BarChart3,
  Box,
  CreditCard,
  LayoutDashboard,
  Package,
  Settings,
  ShoppingCart,
  Store,
  Users,
  Wallet,
  Warehouse,
  X,
} from "lucide-react";
import { cn } from "@/lib/utils";

const menuItems = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/produtos", label: "Produtos", icon: Package },
  { href: "/estoque", label: "Estoque", icon: Warehouse },
  { href: "/clientes", label: "Clientes", icon: Users },
  { href: "/vendas", label: "Vendas", icon: ShoppingCart },
  { href: "/caixa", label: "Caixa", icon: Wallet },
  { href: "/financeiro", label: "Financeiro", icon: CreditCard },
  { href: "/usuarios", label: "Usuários", icon: Users },
  { href: "/configuracoes", label: "Configurações", icon: Settings },
];

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

export function Sidebar({ open, onClose }: SidebarProps) {
  const pathname = usePathname();

  return (
    <>
      {open && (
        <div
          className="fixed inset-0 z-40 bg-slate-900/50 lg:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-50 flex w-64 flex-col bg-slate-900 text-white transition-transform lg:static lg:translate-x-0",
          open ? "translate-x-0" : "-translate-x-full",
        )}
      >
        <div className="flex h-16 items-center justify-between border-b border-slate-700 px-4">
          <div className="flex items-center gap-2">
            <Store className="h-6 w-6 text-brand-400" />
            <span className="text-lg font-bold">ComercialCloud</span>
          </div>
          <button onClick={onClose} className="rounded-lg p-1 hover:bg-slate-800 lg:hidden">
            <X className="h-5 w-5" />
          </button>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {menuItems.map((item) => {
            const Icon = item.icon;
            const active = pathname === item.href || pathname.startsWith(`${item.href}/`);

            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={onClose}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                  active
                    ? "bg-brand-600 text-white"
                    : "text-slate-300 hover:bg-slate-800 hover:text-white",
                )}
              >
                <Icon className="h-5 w-5 shrink-0" />
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="border-t border-slate-700 p-3">
          <Link
            href="/pdv"
            onClick={onClose}
            className="flex items-center gap-3 rounded-lg bg-brand-600 px-3 py-2.5 text-sm font-medium text-white transition-colors hover:bg-brand-500"
          >
            <BarChart3 className="h-5 w-5" />
            Abrir PDV
          </Link>
        </div>
      </aside>
    </>
  );
}
