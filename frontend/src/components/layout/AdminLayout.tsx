"use client";

import { Menu } from "lucide-react";
import { useState } from "react";
import { Sidebar } from "./Sidebar";

export function AdminLayout({ children }: { children: React.ReactNode }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="flex min-h-screen bg-slate-50">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="flex flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-16 items-center gap-4 border-b border-slate-200 bg-white px-4 lg:px-6">
          <button
            onClick={() => setSidebarOpen(true)}
            className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 lg:hidden"
          >
            <Menu className="h-5 w-5" />
          </button>
          <div className="flex-1">
            <p className="text-sm text-slate-500">Retaguarda</p>
          </div>
          <div className="flex items-center gap-2">
            <div className="h-8 w-8 rounded-full bg-brand-100 flex items-center justify-center">
              <span className="text-xs font-semibold text-brand-700">AD</span>
            </div>
          </div>
        </header>

        <main className="flex-1 p-4 lg:p-6">{children}</main>
      </div>
    </div>
  );
}
