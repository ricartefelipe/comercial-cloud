"use client";

import { Button } from "@/components/ui/Button";
import { ErrorMessage, LoadingSpinner } from "@/components/ui/Common";
import { Modal } from "@/components/ui/Modal";
import { consultarPixCobranca, criarPixCobranca, type PixCobrancaPdv } from "@/lib/api/pix-pdv";
import { formatCurrency } from "@/lib/format";
import { Copy, QrCode } from "lucide-react";
import { useEffect, useState } from "react";

interface PixPaymentModalProps {
  open: boolean;
  onClose: () => void;
  vendaId: string;
  valor: number;
  onPago: () => void;
}

export function PixPaymentModal({
  open,
  onClose,
  vendaId,
  valor,
  onPago,
}: PixPaymentModalProps) {
  const [cobranca, setCobranca] = useState<PixCobrancaPdv | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!open) {
      setCobranca(null);
      setError("");
      return;
    }

    let cancelled = false;
    setLoading(true);
    void criarPixCobranca(vendaId, valor)
      .then((result) => {
        if (!cancelled) setCobranca(result);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [open, vendaId, valor]);

  useEffect(() => {
    if (!open || !cobranca || cobranca.status === "PAGO") return;

    const timer = window.setInterval(async () => {
      try {
        const atualizado = await consultarPixCobranca(vendaId, cobranca.id);
        setCobranca(atualizado);
        if (atualizado.status === "PAGO") {
          onPago();
        }
      } catch {
        // continua polling
      }
    }, 3000);

    return () => window.clearInterval(timer);
  }, [open, cobranca, vendaId, onPago]);

  const copyPayload = async () => {
    if (!cobranca?.pixPayload) return;
    await navigator.clipboard.writeText(cobranca.pixPayload);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 2000);
  };

  return (
    <Modal open={open} onClose={onClose} title="Pagamento PIX" size="md">
      <div className="space-y-4">
        <p className="text-center text-2xl font-bold text-brand-600">
          {formatCurrency(valor)}
        </p>

        {loading && <LoadingSpinner />}
        {error && <ErrorMessage message={error} />}

        {cobranca && !loading && (
          <>
            {cobranca.pixEncodedImage ? (
              <div className="flex justify-center">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={`data:image/png;base64,${cobranca.pixEncodedImage}`}
                  alt="QR Code PIX"
                  className="h-48 w-48 rounded-lg border border-slate-200"
                />
              </div>
            ) : (
              <div className="flex justify-center text-slate-400">
                <QrCode className="h-24 w-24" />
              </div>
            )}

            {cobranca.pixPayload && (
              <Button variant="outline" className="w-full" onClick={() => void copyPayload()}>
                <Copy className="h-4 w-4" />
                {copied ? "Copiado!" : "Copiar código PIX"}
              </Button>
            )}

            {cobranca.checkoutUrl && (
              <a
                href={cobranca.checkoutUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="block text-center text-sm text-brand-600 hover:underline"
              >
                Abrir checkout Asaas
              </a>
            )}

            <p className="text-center text-sm text-slate-500">
              {cobranca.status === "PAGO"
                ? "Pagamento confirmado!"
                : "Aguardando confirmação do pagamento..."}
            </p>
          </>
        )}
      </div>
    </Modal>
  );
}
