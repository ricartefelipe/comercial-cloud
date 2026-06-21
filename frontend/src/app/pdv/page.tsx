"use client";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { ErrorMessage, LoadingSpinner } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Select } from "@/components/ui/Select";
import { OfflineIndicator, OnlineBadge } from "@/components/pdv/OfflineIndicator";
import { PdvReceipt } from "@/components/pdv/PdvReceipt";
import { ProductImage } from "@/components/produtos/ProductImage";
import { useBranding } from "@/components/branding/BrandingProvider";
import { abrirCaixa, getCaixaAberto } from "@/lib/api/caixa";
import { listClientes, consumidorRapido } from "@/lib/api/clientes";
import { getDashboardResumo } from "@/lib/api/dashboard";
import { listEstoques } from "@/lib/api/estoque";
import { buscarProdutos, getProdutoByCodigoBarras, listProdutos } from "@/lib/api/produtos";
import {
  addItemVenda,
  addPagamentoVenda,
  aplicarDescontoVenda,
  createVenda,
  finalizarVenda,
  removeItemVenda,
  vincularClienteVenda,
} from "@/lib/api/vendas";
import { looksLikeBarcode, useBarcodeScanner } from "@/hooks/useBarcodeScanner";
import { getLojaId, getOperatorLabel } from "@/lib/auth/tenant-context";
import { formatCurrency, itemVendaLineTotal, toNumber } from "@/lib/format";
import type { FormaPagamento, ItemVenda, Produto, Venda } from "@/types";
import { FORMA_PAGAMENTO_LABELS } from "@/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ArrowLeft,
  Banknote,
  CreditCard,
  Keyboard,
  LockOpen,
  Maximize2,
  Minimize2,
  Minus,
  Package,
  Plus,
  Search,
  ShoppingCart,
  Smartphone,
  Ticket,
  Trash2,
  Wallet,
  X,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

const FORMAS_PAGAMENTO: FormaPagamento[] = [
  "DINHEIRO",
  "PIX",
  "CARTAO_DEBITO",
  "CARTAO_CREDITO",
  "VALE",
  "OUTROS",
];

const FORMA_PAGAMENTO_ICONS: Record<FormaPagamento, LucideIcon> = {
  DINHEIRO: Banknote,
  PIX: Smartphone,
  CARTAO_DEBITO: CreditCard,
  CARTAO_CREDITO: CreditCard,
  VALE: Ticket,
  OUTROS: Wallet,
};

function roundMoney(value: number): number {
  return Math.round(value * 100) / 100;
}

function ProductTile({
  produto,
  disabled,
  estoqueQty,
  estoqueMinimo,
  onAdd,
}: {
  produto: Produto;
  disabled: boolean;
  estoqueQty?: number;
  estoqueMinimo?: number;
  onAdd: (produto: Produto) => void;
}) {
  const semEstoque =
    produto.controlaEstoque && estoqueQty !== undefined && estoqueQty <= 0;
  const estoqueBaixo =
    produto.controlaEstoque &&
    estoqueQty !== undefined &&
    estoqueQty > 0 &&
    estoqueQty <= (estoqueMinimo ?? 10);

  return (
    <button
      type="button"
      onClick={() => onAdd(produto)}
      disabled={disabled || semEstoque}
      className="group rounded-xl border border-slate-200 bg-white p-4 text-left transition-all hover:border-brand-400 hover:shadow-md active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
    >
      <div className="mb-3 flex items-start gap-3">
        <ProductImage src={produto.imagemUrl} alt={produto.nome} size="md" />
        <div className="min-w-0 flex-1">
          <div className="flex items-start justify-between gap-2">
            <p className="font-semibold text-slate-900 group-hover:text-brand-700">
              {produto.nome}
            </p>
            {produto.categoria && (
              <span className="shrink-0 rounded-md bg-slate-100 px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide text-slate-500">
                {produto.categoria}
              </span>
            )}
          </div>
          <p className="text-xs text-slate-500">{produto.sku}</p>
        </div>
      </div>
      {semEstoque && (
        <p className="mb-1 text-xs font-medium text-red-600">Sem estoque</p>
      )}
      {!semEstoque && estoqueBaixo && (
        <p className="mb-1 text-xs font-medium text-amber-600">Estoque: {estoqueQty} un.</p>
      )}
      {!semEstoque && produto.controlaEstoque && estoqueQty !== undefined && !estoqueBaixo && (
        <p className="mb-1 text-xs text-slate-500">{estoqueQty} em estoque</p>
      )}
      <p className="mt-1 text-lg font-bold text-brand-600">
        {formatCurrency(produto.precoVenda)}
      </p>
    </button>
  );
}

export default function PdvPage() {
  const queryClient = useQueryClient();
  const branding = useBranding();
  const searchRef = useRef<HTMLInputElement>(null);
  const [termo, setTermo] = useState("");
  const [searchResults, setSearchResults] = useState<Produto[]>([]);
  const [searching, setSearching] = useState(false);
  const [venda, setVenda] = useState<Venda | null>(null);
  const [clienteId, setClienteId] = useState("");
  const [step, setStep] = useState<"cart" | "payment" | "receipt">("cart");
  const [pagamentos, setPagamentos] = useState<{ forma: FormaPagamento; valor: string }[]>([
    { forma: "DINHEIRO", valor: "" },
  ]);
  const [formaPrincipal, setFormaPrincipal] = useState<FormaPagamento>("DINHEIRO");
  const [pagamentoMisto, setPagamentoMisto] = useState(false);
  const [valorRecebido, setValorRecebido] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [openCaixaModal, setOpenCaixaModal] = useState(false);
  const [valorAbertura, setValorAbertura] = useState("100");
  const [cpfNota, setCpfNota] = useState("");
  const [descontoVenda, setDescontoVenda] = useState("");
  const [kioskMode, setKioskMode] = useState(false);

  const { data: clientes } = useQuery({
    queryKey: ["clientes-pdv"],
    queryFn: () => listClientes(0, 100),
  });

  const { data: caixaAberto, isLoading: caixaLoading } = useQuery({
    queryKey: ["caixa-aberto-pdv"],
    queryFn: () => getCaixaAberto(),
  });

  const { data: catalogo, isLoading: catalogoLoading } = useQuery({
    queryKey: ["produtos-pdv"],
    queryFn: () => listProdutos(0, 48),
  });

  const { data: estoques } = useQuery({
    queryKey: ["estoques-pdv"],
    queryFn: () => listEstoques(),
  });

  const { data: dashboardPdv } = useQuery({
    queryKey: ["dashboard-pdv"],
    queryFn: () => getDashboardResumo(),
    staleTime: 60_000,
  });

  const estoquePorProduto = useMemo(() => {
    const map = new Map<string, { qty: number; minimo: number }>();
    for (const e of estoques ?? []) {
      map.set(e.produtoId, {
        qty: e.quantidadeAtual,
        minimo: e.quantidadeMinima,
      });
    }
    return map;
  }, [estoques]);

  const produtosRapidos = useMemo(() => {
    const ativos = (catalogo?.content ?? []).filter((p) => p.ativo);
    const topIds = dashboardPdv?.topProdutos?.map((p) => p.produtoId) ?? [];
    const byId = new Map(ativos.map((p) => [p.id, p]));
    const ordenados: Produto[] = [];
    for (const id of topIds) {
      const p = byId.get(id);
      if (p) ordenados.push(p);
    }
    for (const p of ativos) {
      if (!topIds.includes(p.id)) ordenados.push(p);
    }
    return ordenados.slice(0, 24);
  }, [catalogo?.content, dashboardPdv?.topProdutos]);
  const caixaPronto = !caixaLoading && !!caixaAberto;
  const operadorLabel = getOperatorLabel();

  const abrirCaixaMutation = useMutation({
    mutationFn: () =>
      abrirCaixa({
        lojaId: getLojaId(),
        valorAbertura: Number(valorAbertura) || 0,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["caixa-aberto-pdv"] });
      queryClient.invalidateQueries({ queryKey: ["caixa-aberto"] });
      setOpenCaixaModal(false);
      setError("");
    },
    onError: (err) => setError((err as Error).message),
  });

  const clienteOptions = [
    { value: "", label: "Consumidor (sem cliente)" },
    ...(clientes?.content ?? []).map((c) => ({
      value: c.id,
      label: c.nome,
    })),
  ];

  const ensureVenda = useCallback(async () => {
    if (venda) return venda;
    const nova = await createVenda({
      lojaId: getLojaId(),
      clienteId: clienteId || undefined,
    });
    setVenda(nova);
    return nova;
  }, [venda, clienteId]);

  const handleSearch = async () => {
    if (!termo.trim()) return;
    setSearching(true);
    setError("");
    try {
      if (looksLikeBarcode(termo.trim())) {
        try {
          const produto = await getProdutoByCodigoBarras(termo.trim());
          if (produto.ativo) {
            await handleAddProduct(produto);
            return;
          }
        } catch {
          // fallback para busca textual
        }
      }
      const results = await buscarProdutos(termo.trim());
      const ativos = results.filter((p) => p.ativo);
      if (ativos.length === 1) {
        await handleAddProduct(ativos[0]);
        return;
      }
      setSearchResults(ativos);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSearching(false);
    }
  };

  const resolverProdutoPorCodigo = useCallback(
    async (codigo: string) => {
      setError("");
      try {
        const produto = await getProdutoByCodigoBarras(codigo);
        if (!produto.ativo) {
          setError("Produto inativo.");
          return;
        }
        await handleAddProduct(produto);
      } catch (err) {
        setError((err as Error).message);
      }
    },
    // handleAddProduct is stable enough for scanner callback
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [caixaPronto, venda, clienteId],
  );

  useBarcodeScanner(
    (codigo) => {
      void resolverProdutoPorCodigo(codigo);
    },
    step === "cart" && caixaPronto,
  );

  const handleAddProduct = async (produto: Produto) => {
    if (!caixaPronto) {
      setError("Abra o caixa antes de adicionar produtos.");
      setOpenCaixaModal(true);
      return;
    }
    const estoque = estoquePorProduto.get(produto.id);
    if (produto.controlaEstoque && estoque !== undefined && estoque.qty <= 0) {
      setError(`Produto "${produto.nome}" sem estoque disponível.`);
      return;
    }
    setLoading(true);
    setError("");
    try {
      const currentVenda = await ensureVenda();
      const updated = await addItemVenda(currentVenda.id, {
        produtoId: produto.id,
        quantidade: 1,
        desconto: 0,
      });
      setVenda(updated);
      setSearchResults([]);
      setTermo("");
      searchRef.current?.focus();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateQty = async (item: ItemVenda, delta: number) => {
    if (!venda) return;
    const newQty = item.quantidade + delta;
    if (newQty <= 0) {
      await handleRemoveItem(item.id);
      return;
    }
    setLoading(true);
    try {
      await removeItemVenda(venda.id, item.id);
      const updated = await addItemVenda(venda.id, {
        produtoId: item.produtoId,
        quantidade: newQty,
        desconto: item.desconto ?? 0,
      });
      setVenda(updated);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveItem = async (itemId: string) => {
    if (!venda) return;
    setLoading(true);
    try {
      const updated = await removeItemVenda(venda.id, itemId);
      setVenda(updated);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const totalVenda = roundMoney(toNumber(venda?.total));
  const pagamentosEfetivos = pagamentoMisto
    ? pagamentos
    : [{ forma: formaPrincipal, valor: String(totalVenda) }];
  const totalPago = roundMoney(
    pagamentosEfetivos.reduce((sum, p) => sum + toNumber(p.valor), 0),
  );
  const valorDinheiro = roundMoney(
    pagamentosEfetivos
      .filter((p) => p.forma === "DINHEIRO")
      .reduce((sum, p) => sum + toNumber(p.valor), 0),
  );
  const diferencaPagamento = roundMoney(totalVenda - totalPago);
  const faltaPagar = Math.max(0, diferencaPagamento);
  const troco =
    valorDinheiro > 0 && toNumber(valorRecebido) > 0
      ? Math.max(0, roundMoney(toNumber(valorRecebido) - valorDinheiro))
      : 0;
  const pagamentoValido = Math.abs(diferencaPagamento) <= 0.01 && totalPago > 0;

  const iniciarPagamento = useCallback(async () => {
    if (!caixaPronto) {
      setOpenCaixaModal(true);
      return;
    }
    if (!venda) return;
    setLoading(true);
    setError("");
    try {
      let current = venda;
      const desconto = toNumber(descontoVenda);
      if (desconto > 0) {
        current = await aplicarDescontoVenda(current.id, desconto);
        setVenda(current);
      }
      if (cpfNota.trim()) {
        const cliente = await consumidorRapido(cpfNota.trim());
        current = await vincularClienteVenda(current.id, cliente.id);
        setVenda(current);
      } else if (clienteId) {
        current = await vincularClienteVenda(current.id, clienteId);
        setVenda(current);
      }
      setFormaPrincipal("DINHEIRO");
      setPagamentoMisto(false);
      setPagamentos([{ forma: "DINHEIRO", valor: String(toNumber(current.total)) }]);
      setValorRecebido("");
      setStep("payment");
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }, [caixaPronto, venda, descontoVenda, cpfNota, clienteId]);

  const selecionarForma = (forma: FormaPagamento) => {
    setFormaPrincipal(forma);
    setError("");
    if (!pagamentoMisto) {
      setValorRecebido("");
    }
  };

  const ativarPagamentoMisto = () => {
    setPagamentoMisto(true);
    setPagamentos([{ forma: formaPrincipal, valor: String(totalVenda) }]);
    setValorRecebido("");
    setError("");
  };

  const desativarPagamentoMisto = () => {
    setPagamentoMisto(false);
    setPagamentos([{ forma: formaPrincipal, valor: String(totalVenda) }]);
    setValorRecebido("");
    setError("");
  };

  const adicionarLinhaPagamento = () => {
    const restante = roundMoney(totalVenda - totalPago);
    setPagamentos([
      ...pagamentos,
      {
        forma: pagamentos.some((p) => p.forma === "PIX") ? "CARTAO_DEBITO" : "PIX",
        valor: restante > 0 ? String(restante) : "",
      },
    ]);
  };

  const atualizarLinhaPagamento = (
    idx: number,
    patch: Partial<{ forma: FormaPagamento; valor: string }>,
  ) => {
    setPagamentos(
      pagamentos.map((pag, i) => (i === idx ? { ...pag, ...patch } : pag)),
    );
    setError("");
  };

  const handleFinalize = async () => {
    if (!venda) return;
    if (!pagamentoValido) {
      setError("A soma dos pagamentos deve ser igual ao total da venda.");
      return;
    }
    setLoading(true);
    setError("");
    try {
      for (const pag of pagamentosEfetivos) {
        const valor = roundMoney(toNumber(pag.valor));
        if (valor > 0) {
          await addPagamentoVenda(venda.id, {
            formaPagamento: pag.forma,
            valor,
          });
        }
      }
      const finalizada = await finalizarVenda(venda.id);
      setVenda(finalizada);
      setStep("receipt");
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const handleNovaVenda = () => {
    setVenda(null);
    setStep("cart");
    setFormaPrincipal("DINHEIRO");
    setPagamentoMisto(false);
    setPagamentos([{ forma: "DINHEIRO", valor: "" }]);
    setValorRecebido("");
    setCpfNota("");
    setDescontoVenda("");
    setClienteId("");
    setError("");
    searchRef.current?.focus();
  };

  const toggleKiosk = async () => {
    if (!kioskMode) {
      setKioskMode(true);
      try {
        await document.documentElement.requestFullscreen();
      } catch {
        // fullscreen opcional
      }
      return;
    }
    setKioskMode(false);
    if (document.fullscreenElement) {
      await document.exitFullscreen();
    }
  };

  useEffect(() => {
    searchRef.current?.focus();
  }, []);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "F2") {
        e.preventDefault();
        searchRef.current?.focus();
      }
      if (e.key === "F4" && venda && venda.itens?.length && caixaPronto) {
        e.preventDefault();
        iniciarPagamento();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [venda, caixaPronto, iniciarPagamento]);

  if (step === "receipt" && venda) {
    return (
      <PdvReceipt
        venda={venda}
        brandName={branding.nomeExibicao}
        onNovaVenda={handleNovaVenda}
      />
    );
  }

  return (
    <div className="flex min-h-screen flex-col bg-slate-100">
      <header className="flex items-center justify-between bg-slate-900 px-4 py-3 text-white lg:px-6">
        <div className="flex items-center gap-4">
          {!kioskMode && (
            <Link href="/dashboard">
              <Button variant="ghost" size="sm" className="text-white hover:bg-slate-800">
                <ArrowLeft className="h-4 w-4" />
                Retaguarda
              </Button>
            </Link>
          )}
          <div className="flex items-center gap-2">
            <ShoppingCart className="h-5 w-5 text-brand-400" />
            <h1 className="text-lg font-bold">{kioskMode ? branding.nomeExibicao : "PDV"}</h1>
          </div>
          <OfflineIndicator />
        </div>
        <div className="flex items-center gap-4">
          <OnlineBadge />
          <Button
            variant="ghost"
            size="sm"
            className="text-white hover:bg-slate-800 print:hidden"
            onClick={() => void toggleKiosk()}
          >
            {kioskMode ? <Minimize2 className="h-4 w-4" /> : <Maximize2 className="h-4 w-4" />}
          </Button>
          <div className="hidden text-right sm:block">
            <p className="text-xs text-slate-400">Operador</p>
            <p className="max-w-[160px] truncate text-sm font-medium">{operadorLabel}</p>
          </div>
          <div className="text-right">
            {!caixaLoading && (
              <Badge variant={caixaAberto ? "success" : "warning"} className="mb-1">
                {caixaAberto ? "Caixa aberto" : "Caixa fechado"}
              </Badge>
            )}
            <p className="text-xs text-slate-400">Total da venda</p>
            <p className="text-2xl font-bold text-brand-400">
              {formatCurrency(totalVenda)}
            </p>
          </div>
        </div>
      </header>

      {!caixaLoading && !caixaAberto && (
        <div className="flex flex-wrap items-center justify-center gap-3 border-b border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900 lg:px-6">
          <span>Abra o caixa para registrar vendas.</span>
          <Button size="sm" onClick={() => setOpenCaixaModal(true)}>
            <LockOpen className="h-4 w-4" />
            Abrir caixa
          </Button>
          <Link href="/caixa" className="text-xs font-medium underline">
            Gerenciar na retaguarda
          </Link>
        </div>
      )}

      <div className="hidden border-b border-slate-200 bg-white px-4 py-2 text-xs text-slate-500 lg:flex lg:px-6">
        <Keyboard className="mr-2 h-4 w-4 shrink-0" />
        <span>
          <kbd className="rounded bg-slate-100 px-1.5 py-0.5 font-mono">F2</kbd> buscar produto
          {" · "}
          <kbd className="rounded bg-slate-100 px-1.5 py-0.5 font-mono">F4</kbd> ir para pagamento
          {" · "}
          <kbd className="rounded bg-slate-100 px-1.5 py-0.5 font-mono">Enter</kbd> na busca
        </span>
      </div>

      <div className="flex flex-1 flex-col lg:flex-row">
        {/* Left: Search & Products */}
        <div className="flex flex-1 flex-col p-4 lg:p-6">
          {step === "cart" && (
            <>
              <div className="mb-4 flex gap-2">
                <div className="relative flex-1">
                  <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                  <input
                    ref={searchRef}
                    type="text"
                    placeholder="Buscar produto (F2)..."
                    className="w-full rounded-xl border border-slate-300 bg-white py-3 pl-11 pr-4 text-lg focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20"
                    value={termo}
                    onChange={(e) => setTermo(e.target.value)}
                    onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                  />
                </div>
                <Button size="lg" onClick={handleSearch} loading={searching}>
                  Buscar
                </Button>
              </div>

              <div className="mb-4 grid gap-4 sm:grid-cols-2">
                <Select
                  label="Cliente (opcional)"
                  options={clienteOptions}
                  value={clienteId}
                  onChange={(e) => setClienteId(e.target.value)}
                />
                <Input
                  label="CPF/CNPJ na nota (opcional)"
                  placeholder="000.000.000-00"
                  value={cpfNota}
                  onChange={(e) => setCpfNota(e.target.value)}
                />
              </div>

              <div className="mb-4 max-w-xs">
                <Input
                  label="Desconto na venda (R$)"
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="0,00"
                  value={descontoVenda}
                  onChange={(e) => setDescontoVenda(e.target.value)}
                />
              </div>

              {searchResults.length > 0 ? (
                <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
                  {searchResults.map((produto) => {
                    const estoque = estoquePorProduto.get(produto.id);
                    return (
                    <ProductTile
                      key={produto.id}
                      produto={produto}
                      disabled={loading || !caixaPronto}
                      estoqueQty={estoque?.qty}
                      estoqueMinimo={estoque?.minimo}
                      onAdd={handleAddProduct}
                    />
                    );
                  })}
                </div>
              ) : (
                <div className="flex flex-1 flex-col">
                  <div className="mb-3 flex items-center gap-2 text-sm font-medium text-slate-700">
                    <Package className="h-4 w-4 text-brand-500" />
                    Produtos rápidos
                  </div>

                  {catalogoLoading ? (
                    <LoadingSpinner />
                  ) : produtosRapidos.length > 0 ? (
                    <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
                      {produtosRapidos.map((produto) => {
                        const estoque = estoquePorProduto.get(produto.id);
                        return (
                        <ProductTile
                          key={produto.id}
                          produto={produto}
                          disabled={loading || !caixaPronto}
                          estoqueQty={estoque?.qty}
                          estoqueMinimo={estoque?.minimo}
                          onAdd={handleAddProduct}
                        />
                        );
                      })}
                    </div>
                  ) : (
                    <div className="flex flex-1 flex-col items-center justify-center rounded-xl border border-dashed border-slate-300 bg-white px-6 py-12 text-center">
                      <Search className="mb-3 h-10 w-10 text-slate-300" />
                      <p className="font-medium text-slate-700">Nenhum produto cadastrado</p>
                      <p className="mt-1 max-w-sm text-sm text-slate-500">
                        Use a busca acima ou cadastre produtos na retaguarda.
                      </p>
                    </div>
                  )}

                  {termo && !searching && searchResults.length === 0 && (
                    <p className="mt-4 text-center text-slate-500">
                      Nenhum produto encontrado para &quot;{termo}&quot;.
                    </p>
                  )}
                </div>
              )}
            </>
          )}

          {step === "payment" && (
            <div className="mx-auto w-full max-w-2xl space-y-5">
              <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
                <p className="text-sm font-medium text-slate-500">Total a receber</p>
                <p className="mt-1 text-4xl font-bold text-brand-600">
                  {formatCurrency(totalVenda)}
                </p>
              </div>

              {!pagamentoMisto ? (
                <>
                  <div>
                    <p className="mb-3 text-sm font-medium text-slate-700">
                      Forma de pagamento
                    </p>
                    <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
                      {FORMAS_PAGAMENTO.map((forma) => {
                        const Icon = FORMA_PAGAMENTO_ICONS[forma];
                        const selected = formaPrincipal === forma;
                        return (
                          <button
                            key={forma}
                            type="button"
                            onClick={() => selecionarForma(forma)}
                            className={`flex flex-col items-center gap-2 rounded-xl border-2 px-3 py-4 text-sm font-medium transition-all ${
                              selected
                                ? "border-brand-500 bg-brand-50 text-brand-700 shadow-sm"
                                : "border-slate-200 bg-white text-slate-700 hover:border-brand-300 hover:bg-slate-50"
                            }`}
                          >
                            <Icon className="h-6 w-6" />
                            {FORMA_PAGAMENTO_LABELS[forma]}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  {formaPrincipal === "DINHEIRO" && (
                    <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 space-y-3">
                      <Input
                        label="Valor entregue pelo cliente"
                        type="number"
                        step="0.01"
                        min="0"
                        placeholder={String(totalVenda)}
                        value={valorRecebido}
                        onChange={(e) => setValorRecebido(e.target.value)}
                      />
                      <div className="flex flex-wrap gap-2">
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() => setValorRecebido(String(totalVenda))}
                        >
                          Valor exato
                        </Button>
                        {[20, 50, 100, 200].map((nota) => (
                          <Button
                            key={nota}
                            type="button"
                            variant="outline"
                            size="sm"
                            disabled={nota < totalVenda}
                            onClick={() => setValorRecebido(String(nota))}
                          >
                            R$ {nota}
                          </Button>
                        ))}
                      </div>
                      {toNumber(valorRecebido) > 0 && (
                        <div
                          className={`rounded-lg px-4 py-3 text-sm ${
                            troco > 0
                              ? "bg-emerald-50 text-emerald-800"
                              : "bg-slate-100 text-slate-600"
                          }`}
                        >
                          {troco > 0 ? (
                            <>
                              Troco:{" "}
                              <span className="text-xl font-bold">{formatCurrency(troco)}</span>
                            </>
                          ) : (
                            "Sem troco — valor exato ou falta completar."
                          )}
                        </div>
                      )}
                    </div>
                  )}

                  <button
                    type="button"
                    onClick={ativarPagamentoMisto}
                    className="text-sm font-medium text-brand-600 hover:text-brand-700"
                  >
                    Dividir em mais de uma forma de pagamento
                  </button>
                </>
              ) : (
                <>
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium text-slate-700">Pagamento misto</p>
                    <button
                      type="button"
                      onClick={desativarPagamentoMisto}
                      className="text-sm text-slate-500 hover:text-slate-700"
                    >
                      Voltar ao pagamento simples
                    </button>
                  </div>

                  <div className="space-y-2">
                    {pagamentos.map((pag, idx) => (
                      <div
                        key={idx}
                        className="flex gap-2 rounded-xl border border-slate-200 bg-white p-3"
                      >
                        <Select
                          options={FORMAS_PAGAMENTO.map((f) => ({
                            value: f,
                            label: FORMA_PAGAMENTO_LABELS[f],
                          }))}
                          value={pag.forma}
                          onChange={(e) =>
                            atualizarLinhaPagamento(idx, {
                              forma: e.target.value as FormaPagamento,
                            })
                          }
                          className="flex-1"
                        />
                        <Input
                          type="number"
                          step="0.01"
                          min="0"
                          placeholder="0,00"
                          value={pag.valor}
                          onChange={(e) =>
                            atualizarLinhaPagamento(idx, { valor: e.target.value })
                          }
                          className="w-36"
                        />
                        {pagamentos.length > 1 && (
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() =>
                              setPagamentos(pagamentos.filter((_, i) => i !== idx))
                            }
                          >
                            <X className="h-4 w-4" />
                          </Button>
                        )}
                      </div>
                    ))}
                  </div>

                  <Button type="button" variant="outline" size="sm" onClick={adicionarLinhaPagamento}>
                    <Plus className="h-4 w-4" />
                    Adicionar forma de pagamento
                  </Button>

                  {valorDinheiro > 0 && (
                    <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 space-y-3">
                      <Input
                        label="Valor entregue em dinheiro"
                        type="number"
                        step="0.01"
                        min="0"
                        value={valorRecebido}
                        onChange={(e) => setValorRecebido(e.target.value)}
                      />
                      {toNumber(valorRecebido) > 0 && troco > 0 && (
                        <p className="text-sm text-emerald-700">
                          Troco: <span className="text-xl font-bold">{formatCurrency(troco)}</span>
                        </p>
                      )}
                    </div>
                  )}
                </>
              )}

              <div
                className={`rounded-xl px-4 py-3 text-sm ${
                  pagamentoValido
                    ? "bg-emerald-50 text-emerald-800"
                    : faltaPagar > 0
                      ? "bg-amber-50 text-amber-800"
                      : "bg-red-50 text-red-800"
                }`}
              >
                <div className="flex justify-between">
                  <span>Informado</span>
                  <span className="font-semibold">{formatCurrency(totalPago)}</span>
                </div>
                {!pagamentoValido && faltaPagar > 0 && (
                  <div className="mt-1 flex justify-between">
                    <span>Falta</span>
                    <span className="font-semibold">{formatCurrency(faltaPagar)}</span>
                  </div>
                )}
                {!pagamentoValido && diferencaPagamento < -0.01 && (
                  <div className="mt-1 flex justify-between">
                    <span>Excedente</span>
                    <span className="font-semibold">
                      {formatCurrency(Math.abs(diferencaPagamento))}
                    </span>
                  </div>
                )}
                {pagamentoValido && (
                  <p className="mt-1 font-medium">Pagamento completo — pronto para finalizar.</p>
                )}
              </div>

              {error && <ErrorMessage message={error} />}

              <div className="flex gap-3 pt-2">
                <Button variant="outline" onClick={() => setStep("cart")}>
                  Voltar
                </Button>
                <Button
                  className="flex-1"
                  size="lg"
                  loading={loading}
                  disabled={!pagamentoValido}
                  onClick={handleFinalize}
                >
                  Finalizar venda
                </Button>
              </div>
            </div>
          )}

          {error && step !== "payment" && (
            <div className="mt-4">
              <ErrorMessage message={error} />
            </div>
          )}
        </div>

        {/* Right: Cart */}
        <aside className="w-full border-t border-slate-200 bg-white lg:w-96 lg:border-l lg:border-t-0">
          <div className="flex h-full flex-col">
            <div className="border-b border-slate-200 px-4 py-3">
              <h2 className="font-semibold text-slate-900">
                Carrinho ({venda?.itens?.length ?? 0})
              </h2>
            </div>

            <div className="flex-1 overflow-y-auto p-4 scrollbar-thin">
              {venda?.itens?.length ? (
                <div className="space-y-3">
                  {venda.itens.map((item) => (
                    <div
                      key={item.id}
                      className="rounded-lg border border-slate-200 p-3"
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <p className="text-sm font-medium">
                            {item.produtoNome ?? item.produtoSku}
                          </p>
                          <p className="text-xs text-slate-500">
                            {formatCurrency(item.precoUnitario)} un.
                          </p>
                        </div>
                        <button
                          onClick={() => handleRemoveItem(item.id)}
                          className="text-slate-400 hover:text-red-500"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                      <div className="mt-2 flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => handleUpdateQty(item, -1)}
                            className="rounded-lg border border-slate-200 p-1 hover:bg-slate-50"
                          >
                            <Minus className="h-4 w-4" />
                          </button>
                          <span className="w-8 text-center font-medium">
                            {item.quantidade}
                          </span>
                          <button
                            onClick={() => handleUpdateQty(item, 1)}
                            className="rounded-lg border border-slate-200 p-1 hover:bg-slate-50"
                          >
                            <Plus className="h-4 w-4" />
                          </button>
                        </div>
                        <span className="font-semibold text-brand-600">
                          {formatCurrency(itemVendaLineTotal(item))}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="py-8 text-center text-sm text-slate-400">
                  Carrinho vazio. Busque produtos para adicionar.
                </p>
              )}
            </div>

            <div className="border-t border-slate-200 p-4">
              <div className="mb-3 space-y-1 text-sm">
                <div className="flex justify-between text-slate-500">
                  <span>Subtotal</span>
                  <span>{formatCurrency(venda?.subtotal ?? 0)}</span>
                </div>
                {(venda?.descontoTotal ?? 0) > 0 && (
                  <div className="flex justify-between text-red-500">
                    <span>Desconto</span>
                    <span>-{formatCurrency(venda?.descontoTotal ?? 0)}</span>
                  </div>
                )}
              </div>
              <div className="mb-4 flex justify-between text-xl font-bold">
                <span>Total</span>
                <span className="text-brand-600">{formatCurrency(totalVenda)}</span>
              </div>
              {step === "cart" && (
                <Button
                  className="w-full"
                  size="lg"
                  loading={loading}
                  disabled={!venda?.itens?.length || !caixaPronto}
                  onClick={() => void iniciarPagamento()}
                >
                  Ir para pagamento (F4)
                </Button>
              )}
            </div>
          </div>
        </aside>
      </div>

      <Modal
        open={openCaixaModal}
        onClose={() => setOpenCaixaModal(false)}
        title="Abrir caixa"
        footer={
          <>
            <Button variant="outline" onClick={() => setOpenCaixaModal(false)}>
              Cancelar
            </Button>
            <Button
              loading={abrirCaixaMutation.isPending}
              onClick={() => abrirCaixaMutation.mutate()}
            >
              <LockOpen className="h-4 w-4" />
              Confirmar abertura
            </Button>
          </>
        }
      >
        <p className="mb-4 text-sm text-slate-600">
          Informe o valor inicial em dinheiro no caixa para começar a vender.
        </p>
        <Input
          label="Valor de abertura (R$)"
          type="number"
          step="0.01"
          min="0"
          value={valorAbertura}
          onChange={(e) => setValorAbertura(e.target.value)}
        />
      </Modal>
    </div>
  );
}
