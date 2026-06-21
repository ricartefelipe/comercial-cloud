"use client";

import { listLojas } from "@/lib/api/lojas";
import { getLojaId, setLojaId } from "@/lib/auth/tenant-context";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Store } from "lucide-react";
import { useRouter } from "next/navigation";

export function LojaSwitcher() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const lojaAtual = getLojaId();

  const { data: lojas = [] } = useQuery({
    queryKey: ["lojas"],
    queryFn: listLojas,
  });

  if (lojas.length <= 1) {
    const nome = lojas.find((l) => l.id === lojaAtual)?.nome ?? "Loja";
    return (
      <div className="flex items-center gap-2 text-sm text-slate-600">
        <Store className="h-4 w-4" />
        <span>{nome}</span>
      </div>
    );
  }

  return (
    <div className="flex items-center gap-2">
      <Store className="h-4 w-4 text-slate-500" />
      <select
        className="rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-sm text-slate-700"
        value={lojaAtual}
        onChange={(e) => {
          setLojaId(e.target.value);
          queryClient.clear();
          router.refresh();
        }}
      >
        {lojas
          .filter((l) => l.ativa)
          .map((loja) => (
            <option key={loja.id} value={loja.id}>
              {loja.nome}
            </option>
          ))}
      </select>
    </div>
  );
}
