"use client";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { getDocumentoFiscalPorVenda } from "@/lib/api/fiscal";
import { formatCurrency, itemVendaLineTotal } from "@/lib/format";
import type { DocumentoFiscal, Venda } from "@/types";
import { FORMA_PAGAMENTO_LABELS, STATUS_DOCUMENTO_FISCAL_LABELS } from "@/types";
import { Printer, Receipt } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";

function fiscalBadgeVariant(status: string | undefined) {
  switch (status) {
    case "AUTORIZADO":
      return "success" as const;
    case "PENDENTE":
      return "warning" as const;
    case "REJEITADO":
    case "ERRO":
      return "danger" as const;
    default:
      return "default" as const;
  }
}

interface PdvReceiptProps {
  venda: Venda;
  brandName: string;
  onNovaVenda: () => void;
}

export function PdvReceipt({ venda, brandName, onNovaVenda }: PdvReceiptProps) {
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
        <div
          id="pdv-receipt"
          className="rounded-xl bg-white p-6 text-slate-900 shadow-xl print:rounded-none print:shadow-none print:p-4"
        >
          <div className="border-b border-dashed border-slate-300 pb-4 text-center">
            <p className="text-lg font-bold">{brandName}</p>
            <p className="text-sm text-slate-500">
              Venda #{venda.numero ?? venda.id.slice(0, 8)}
            </p>
            {venda.clienteNome && (
              <p className="mt-1 text-xs text-slate-500">Cliente: {venda.clienteNome}</p>
            )}
          </div>

          <div className="space-y-2 border-b border-dashed border-slate-300 py-4">
            {venda.itens?.map((item) => (
              <div key={item.id} className="flex justify-between text-sm">
                <span>
                  {item.quantidade}x {item.produtoNome ?? item.produtoSku}
                </span>
                <span>{formatCurrency(itemVendaLineTotal(item))}</span>
              </div>
            ))}
          </div>

          <div className="space-y-1 py-4 text-sm">
            <div className="flex justify-between">
              <span>Subtotal</span>
              <span>{formatCurrency(venda.subtotal)}</span>
            </div>
            {(venda.descontoTotal ?? 0) > 0 && (
              <div className="flex justify-between text-red-600">
                <span>Desconto</span>
                <span>-{formatCurrency(venda.descontoTotal)}</span>
              </div>
            )}
            <div className="flex justify-between text-lg font-bold">
              <span>TOTAL</span>
              <span>{formatCurrency(venda.total)}</span>
            </div>
          </div>

          {venda.pagamentos && venda.pagamentos.length > 0 && (
            <div className="border-t border-dashed border-slate-300 pt-4 text-sm">
              <p className="mb-2 font-medium">Pagamentos</p>
              {venda.pagamentos.map((pag) => (
                <div key={pag.id} className="flex justify-between">
                  <span>{FORMA_PAGAMENTO_LABELS[pag.formaPagamento]}</span>
                  <span>{formatCurrency(pag.valor)}</span>
                </div>
              ))}
            </div>
          )}

          {documento && (
            <div className="mt-4 border-t border-dashed border-slate-300 pt-4 text-sm">
              <div className="flex items-center justify-between">
                <span className="font-medium">NFC-e</span>
                <Badge variant={fiscalBadgeVariant(documento.status)}>
                  {STATUS_DOCUMENTO_FISCAL_LABELS[documento.status] ?? documento.status}
                </Badge>
              </div>
              {documento.chaveAcesso && (
                <p className="mt-2 break-all text-xs text-slate-500">{documento.chaveAcesso}</p>
              )}
              {documento.urlDanfe && (
                <a
                  href={documento.urlDanfe}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="mt-2 inline-block text-brand-600 underline print:hidden"
                >
                  Abrir DANFE
                </a>
              )}
            </div>
          )}

          <p className="mt-6 text-center text-xs text-slate-400">Obrigado pela preferência!</p>
        </div>

        <div className="mt-6 flex gap-3 print:hidden">
          <Button className="flex-1" variant="outline" onClick={handlePrint}>
            <Printer className="h-4 w-4" />
            Imprimir
          </Button>
          <Button className="flex-1" onClick={onNovaVenda}>
            Nova venda
          </Button>
          <Link href="/dashboard" className="flex-1">
            <Button
              variant="outline"
              className="w-full bg-transparent text-white border-slate-600 hover:bg-slate-800"
            >
              Retaguarda
            </Button>
          </Link>
        </div>
      </main>
    </div>
  );
}
