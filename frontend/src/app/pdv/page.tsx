"use client";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ErrorMessage } from "@/components/ui/Common";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { listClientes } from "@/lib/api/clientes";
import { buscarProdutos } from "@/lib/api/produtos";
import {
  addItemVenda,
  addPagamentoVenda,
  createVenda,
  finalizarVenda,
  removeItemVenda,
} from "@/lib/api/vendas";
import { LOJA_ID, USER_ID } from "@/lib/constants";
import { formatCurrency } from "@/lib/format";
import type { FormaPagamento, ItemVenda, Produto, Venda } from "@/types";
import { FORMA_PAGAMENTO_LABELS } from "@/types";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  ArrowLeft,
  Minus,
  Plus,
  Receipt,
  Search,
  ShoppingCart,
  Trash2,
  X,
} from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";

const FORMAS_PAGAMENTO: FormaPagamento[] = [
  "DINHEIRO",
  "PIX",
  "CARTAO_DEBITO",
  "CARTAO_CREDITO",
  "VALE",
  "OUTROS",
];

export default function PdvPage() {
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
  const [valorRecebido, setValorRecebido] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const { data: clientes } = useQuery({
    queryKey: ["clientes-pdv"],
    queryFn: () => listClientes(0, 100),
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
      lojaId: LOJA_ID,
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
      const results = await buscarProdutos(termo.trim());
      setSearchResults(results.filter((p) => p.ativo));
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSearching(false);
    }
  };

  const handleAddProduct = async (produto: Produto) => {
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
        desconto: item.desconto,
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

  const totalPago = pagamentos.reduce((sum, p) => sum + (Number(p.valor) || 0), 0);
  const totalVenda = venda?.total ?? 0;
  const troco = Number(valorRecebido) - totalVenda;
  const faltaPagar = Math.max(0, totalVenda - totalPago);

  const handleFinalize = async () => {
    if (!venda) return;
    setLoading(true);
    setError("");
    try {
      for (const pag of pagamentos) {
        const valor = Number(pag.valor);
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
    setPagamentos([{ forma: "DINHEIRO", valor: "" }]);
    setValorRecebido("");
    setClienteId("");
    setError("");
    searchRef.current?.focus();
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
      if (e.key === "F4" && venda && venda.itens?.length) {
        e.preventDefault();
        setStep("payment");
        setPagamentos([{ forma: "DINHEIRO", valor: String(venda.total) }]);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [venda]);

  if (step === "receipt" && venda) {
    return (
      <div className="flex min-h-screen flex-col bg-slate-900 text-white">
        <header className="flex items-center justify-between border-b border-slate-700 px-6 py-4">
          <div className="flex items-center gap-3">
            <Receipt className="h-6 w-6 text-brand-400" />
            <h1 className="text-xl font-bold">Comprovante</h1>
          </div>
          <Badge variant="success">Venda finalizada</Badge>
        </header>

        <main className="mx-auto w-full max-w-md flex-1 p-6">
          <div className="rounded-xl bg-white p-6 text-slate-900 shadow-xl">
            <div className="border-b border-dashed border-slate-300 pb-4 text-center">
              <p className="text-lg font-bold">ComercialCloud</p>
              <p className="text-sm text-slate-500">
                Venda #{venda.numero ?? venda.id.slice(0, 8)}
              </p>
            </div>

            <div className="space-y-2 border-b border-dashed border-slate-300 py-4">
              {venda.itens?.map((item) => (
                <div key={item.id} className="flex justify-between text-sm">
                  <span>
                    {item.quantidade}x {item.produtoNome ?? item.produtoSku}
                  </span>
                  <span>{formatCurrency(item.total)}</span>
                </div>
              ))}
            </div>

            <div className="space-y-1 py-4 text-sm">
              <div className="flex justify-between">
                <span>Subtotal</span>
                <span>{formatCurrency(venda.subtotal)}</span>
              </div>
              {venda.descontoTotal > 0 && (
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
                <p className="mb-2 font-medium">Pagamentos:</p>
                {venda.pagamentos.map((pag) => (
                  <div key={pag.id} className="flex justify-between">
                    <span>{FORMA_PAGAMENTO_LABELS[pag.formaPagamento]}</span>
                    <span>{formatCurrency(pag.valor)}</span>
                  </div>
                ))}
              </div>
            )}

            <p className="mt-6 text-center text-xs text-slate-400">
              Obrigado pela preferência!
            </p>
          </div>

          <div className="mt-6 flex gap-3">
            <Button className="flex-1" onClick={handleNovaVenda}>
              Nova venda
            </Button>
            <Link href="/dashboard" className="flex-1">
              <Button variant="outline" className="w-full bg-transparent text-white border-slate-600 hover:bg-slate-800">
                Retaguarda
              </Button>
            </Link>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col bg-slate-100">
      <header className="flex items-center justify-between bg-slate-900 px-4 py-3 text-white lg:px-6">
        <div className="flex items-center gap-4">
          <Link href="/dashboard">
            <Button variant="ghost" size="sm" className="text-white hover:bg-slate-800">
              <ArrowLeft className="h-4 w-4" />
              Retaguarda
            </Button>
          </Link>
          <div className="flex items-center gap-2">
            <ShoppingCart className="h-5 w-5 text-brand-400" />
            <h1 className="text-lg font-bold">PDV</h1>
          </div>
        </div>
        <div className="text-right">
          <p className="text-xs text-slate-400">Operador caixa</p>
          <p className="text-xs text-slate-500">{USER_ID.slice(0, 8)}...</p>
          <p className="text-xs text-slate-400">Total</p>
          <p className="text-2xl font-bold text-brand-400">
            {formatCurrency(totalVenda)}
          </p>
        </div>
      </header>

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

              <div className="mb-4">
                <Select
                  label="Cliente (opcional)"
                  options={clienteOptions}
                  value={clienteId}
                  onChange={(e) => setClienteId(e.target.value)}
                />
              </div>

              {searchResults.length > 0 && (
                <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
                  {searchResults.map((produto) => (
                    <button
                      key={produto.id}
                      onClick={() => handleAddProduct(produto)}
                      disabled={loading}
                      className="rounded-xl border border-slate-200 bg-white p-4 text-left transition-all hover:border-brand-400 hover:shadow-md active:scale-[0.98]"
                    >
                      <p className="font-semibold text-slate-900">{produto.nome}</p>
                      <p className="text-xs text-slate-500">{produto.sku}</p>
                      <p className="mt-2 text-lg font-bold text-brand-600">
                        {formatCurrency(produto.precoVenda)}
                      </p>
                    </button>
                  ))}
                </div>
              )}

              {termo && !searching && searchResults.length === 0 && (
                <p className="text-center text-slate-500">Nenhum produto encontrado.</p>
              )}
            </>
          )}

          {step === "payment" && (
            <div className="mx-auto w-full max-w-lg space-y-4">
              <h2 className="text-xl font-bold text-slate-900">Pagamento</h2>
              <p className="text-3xl font-bold text-brand-600">
                {formatCurrency(totalVenda)}
              </p>

              {pagamentos.map((pag, idx) => (
                <div key={idx} className="flex gap-2">
                  <Select
                    options={FORMAS_PAGAMENTO.map((f) => ({
                      value: f,
                      label: FORMA_PAGAMENTO_LABELS[f],
                    }))}
                    value={pag.forma}
                    onChange={(e) => {
                      const updated = [...pagamentos];
                      updated[idx] = { ...updated[idx], forma: e.target.value as FormaPagamento };
                      setPagamentos(updated);
                    }}
                    className="flex-1"
                  />
                  <Input
                    type="number"
                    step="0.01"
                    placeholder="Valor"
                    value={pag.valor}
                    onChange={(e) => {
                      const updated = [...pagamentos];
                      updated[idx] = { ...updated[idx], valor: e.target.value };
                      setPagamentos(updated);
                    }}
                    className="w-32"
                  />
                  {pagamentos.length > 1 && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setPagamentos(pagamentos.filter((_, i) => i !== idx))}
                    >
                      <X className="h-4 w-4" />
                    </Button>
                  )}
                </div>
              ))}

              <Button
                variant="outline"
                size="sm"
                onClick={() => setPagamentos([...pagamentos, { forma: "PIX", valor: "" }])}
              >
                <Plus className="h-4 w-4" />
                Adicionar forma de pagamento
              </Button>

              {pagamentos.some((p) => p.forma === "DINHEIRO") && (
                <Input
                  label="Valor recebido (dinheiro)"
                  type="number"
                  step="0.01"
                  value={valorRecebido}
                  onChange={(e) => setValorRecebido(e.target.value)}
                />
              )}

              {Number(valorRecebido) > 0 && troco >= 0 && (
                <div className="rounded-lg bg-emerald-50 px-4 py-3">
                  <p className="text-sm text-emerald-700">
                    Troco: <span className="text-xl font-bold">{formatCurrency(troco)}</span>
                  </p>
                </div>
              )}

              {faltaPagar > 0 && (
                <div className="rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-700">
                  Falta pagar: {formatCurrency(faltaPagar)}
                </div>
              )}

              <div className="flex gap-3 pt-4">
                <Button variant="outline" onClick={() => setStep("cart")}>
                  Voltar
                </Button>
                <Button
                  className="flex-1"
                  size="lg"
                  loading={loading}
                  disabled={faltaPagar > 0.01}
                  onClick={handleFinalize}
                >
                  Finalizar venda
                </Button>
              </div>
            </div>
          )}

          {error && (
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
                          {formatCurrency(item.total)}
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
                  disabled={!venda?.itens?.length}
                  onClick={() => {
                    setStep("payment");
                    setPagamentos([{ forma: "DINHEIRO", valor: String(totalVenda) }]);
                  }}
                >
                  Ir para pagamento (F4)
                </Button>
              )}
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}
