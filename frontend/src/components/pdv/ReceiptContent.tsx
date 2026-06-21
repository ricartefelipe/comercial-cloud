"use client";

import { Badge } from "@/components/ui/Badge";
import { formatCurrency, itemVendaLineTotal } from "@/lib/format";
import type { DocumentoFiscal, Venda } from "@/types";
import { FORMA_PAGAMENTO_LABELS, STATUS_DOCUMENTO_FISCAL_LABELS } from "@/types";

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

interface ReceiptContentProps {
  venda: Venda;
  brandName: string;
  documento?: DocumentoFiscal | null;
}

export function ReceiptContent({ venda, brandName, documento }: ReceiptContentProps) {
  return (
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
          {documento.motivoRejeicao && (
            <p className="mt-2 text-xs text-red-600">{documento.motivoRejeicao}</p>
          )}
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
  );
}
