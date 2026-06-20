import Link from "next/link";
import type { ReactNode } from "react";

type LegalShellProps = {
  title: string;
  updated: string;
  children: ReactNode;
};

export function LegalShell({ title, updated, children }: LegalShellProps) {
  return (
    <div className="min-h-screen bg-slate-100 px-4 py-10">
      <article className="mx-auto max-w-2xl rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
        <p className="text-sm">
          <Link href="/cadastro" className="text-brand-600 hover:text-brand-700">
            ← Voltar ao cadastro
          </Link>
        </p>
        <h1 className="mt-4 text-2xl font-bold text-slate-900">{title}</h1>
        <p className="mt-1 text-sm text-slate-500">{updated}</p>
        <div className="prose prose-slate mt-6 max-w-none text-sm leading-relaxed text-slate-700">
          {children}
        </div>
      </article>
    </div>
  );
}
