"use client";

import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ErrorMessage, LoadingSpinner, PageHeader } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { downloadRelatorioCsv, getRelatorioResumo } from "@/lib/api/relatorios";
import { getLojaId } from "@/lib/auth/tenant-context";
import { formatCurrency } from "@/lib/format";
import { useQuery } from "@tanstack/react-query";
import { Download, FileSpreadsheet } from "lucide-react";
import { useState } from "react";

function defaultInicio(): string {
  const d = new Date();
  d.setDate(d.getDate() - 30);
  return d.toISOString().slice(0, 10);
}

function defaultFim(): string {
  return new Date().toISOString().slice(0, 10);
}

export default function RelatoriosPage() {
  const [dataInicio, setDataInicio] = useState(defaultInicio);
  const [dataFim, setDataFim] = useState(defaultFim);
  const [downloading, setDownloading] = useState<"vendas" | "estoque" | null>(null);
  const [downloadError, setDownloadError] = useState("");

  const lojaId = getLojaId();

  const { data: resumo, isLoading, error } = useQuery({
    queryKey: ["relatorio-resumo", lojaId, dataInicio, dataFim],
    queryFn: () => getRelatorioResumo({ lojaId, dataInicio, dataFim }),
  });

  const handleDownload = async (tipo: "vendas" | "estoque") => {
    setDownloadError("");
    setDownloading(tipo);
    try {
      await downloadRelatorioCsv(tipo, { lojaId, dataInicio, dataFim });
    } catch (e) {
      setDownloadError((e as Error).message);
    } finally {
      setDownloading(null);
    }
  };

  return (
    <div>
      <PageHeader
        title="Relatórios"
        description="Exporte vendas e estoque da loja ativa em CSV"
      />

      <Card className="mb-6">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Input
            label="Data início"
            type="date"
            value={dataInicio}
            onChange={(e) => setDataInicio(e.target.value)}
          />
          <Input
            label="Data fim"
            type="date"
            value={dataFim}
            onChange={(e) => setDataFim(e.target.value)}
          />
        </div>
      </Card>

      {isLoading && <LoadingSpinner />}
      {error && <ErrorMessage message={(error as Error).message} />}
      {downloadError && <ErrorMessage message={downloadError} />}

      {resumo && !isLoading && (
        <div className="mb-6 grid gap-4 sm:grid-cols-3">
          <Card>
            <p className="text-sm text-slate-500">Vendas finalizadas</p>
            <p className="text-2xl font-semibold">{resumo.vendasFinalizadas}</p>
          </Card>
          <Card>
            <p className="text-sm text-slate-500">Faturamento</p>
            <p className="text-2xl font-semibold">{formatCurrency(Number(resumo.faturamento))}</p>
          </Card>
          <Card>
            <p className="text-sm text-slate-500">SKUs com estoque</p>
            <p className="text-2xl font-semibold">{resumo.skusComEstoque}</p>
          </Card>
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <div className="flex items-start gap-3">
            <FileSpreadsheet className="mt-1 h-8 w-8 text-brand-600" />
            <div className="flex-1">
              <h3 className="font-semibold">Vendas por período</h3>
              <p className="mt-1 text-sm text-slate-500">
                CSV com vendas finalizadas no intervalo selecionado.
              </p>
              <Button
                className="mt-4"
                onClick={() => handleDownload("vendas")}
                loading={downloading === "vendas"}
              >
                <Download className="h-4 w-4" />
                Baixar vendas.csv
              </Button>
            </div>
          </div>
        </Card>

        <Card>
          <div className="flex items-start gap-3">
            <FileSpreadsheet className="mt-1 h-8 w-8 text-brand-600" />
            <div className="flex-1">
              <h3 className="font-semibold">Estoque atual</h3>
              <p className="mt-1 text-sm text-slate-500">
                Posição de estoque da loja ativa (SKU, nome e quantidade).
              </p>
              <Button
                className="mt-4"
                onClick={() => handleDownload("estoque")}
                loading={downloading === "estoque"}
              >
                <Download className="h-4 w-4" />
                Baixar estoque.csv
              </Button>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
