"use client";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { getDocumentoFiscalPorVenda } from "@/lib/api/fiscal";
import { ReceiptContent } from "@/components/pdv/ReceiptContent";
import type { Venda } from "@/types";
import { Printer, Receipt } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import type { DocumentoFiscal } from "@/types";

interface PdvReceiptProps {
  venda: Venda;
  brandName: string;
  onNovaVenda?: () => void;
  showActions?: boolean;
}

export function PdvReceipt({
  venda,
  brandName,
  onNovaVenda,
  showActions = true,
}: PdvReceiptProps) {
  const [documento, setDocumento] = useState<DocumentoFiscal | null>(null);

  useEffect(() => {
    let active = true;
    let attempts = 0;

    const load = async () => {
      try {
        const doc = await getDocumentoFiscalPorVenda(venda.id);
        if (!active) return;
        setDocumento(doc);
        if (doc?.status === "PENDENTE" && attempts < 12) {
          attempts += 1;
          setTimeout(load, 2500);
        }
      } catch {
        if (active && attempts < 3) {
          attempts += 1;
          setTimeout(load, 2000);
        }
      }
    };

    load();
    return () => {
      active = false;
    };
  }, [venda.id]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "F8" || (e.ctrlKey && e.key === "p")) {
        e.preventDefault();
        window.print();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="flex min-h-screen flex-col bg-slate-900 text-white print:bg-white print:text-black">
      <header className="flex items-center justify-between border-b border-slate-700 px-6 py-4 print:hidden">
        <div className="flex items-center gap-3">
          <Receipt className="h-6 w-6 text-brand-400" />
          <h1 className="text-xl font-bold">Comprovante</h1>
        </div>
        <Badge variant="success">Venda finalizada</Badge>
      </header>

      <main className="mx-auto w-full max-w-md flex-1 p-6 print:p-0 print:max-w-none">
        <ReceiptContent venda={venda} brandName={brandName} documento={documento} />

        {showActions && (
          <div className="mt-6 flex gap-3 print:hidden">
            <Button className="flex-1" variant="outline" onClick={handlePrint}>
              <Printer className="h-4 w-4" />
              Imprimir (F8)
            </Button>
            {onNovaVenda && (
              <Button className="flex-1" onClick={onNovaVenda}>
                Nova venda
              </Button>
            )}
            <Link href="/dashboard" className="flex-1">
              <Button
                variant="outline"
                className="w-full bg-transparent text-white border-slate-600 hover:bg-slate-800"
              >
                Retaguarda
              </Button>
            </Link>
          </div>
        )}
      </main>
    </div>
  );
}
