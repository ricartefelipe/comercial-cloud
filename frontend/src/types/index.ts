export type StatusVenda = "ABERTA" | "FINALIZADA" | "CANCELADA";

export type FormaPagamento =
  | "DINHEIRO"
  | "PIX"
  | "CARTAO_DEBITO"
  | "CARTAO_CREDITO"
  | "VALE"
  | "OUTROS";

export type StatusCaixa = "ABERTO" | "FECHADO";

export type TipoMovimentacaoEstoque =
  | "ENTRADA"
  | "SAIDA"
  | "AJUSTE"
  | "VENDA"
  | "CANCELAMENTO_VENDA";

export type StatusContaReceber = "ABERTO" | "PAGO" | "CANCELADO";

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ApiError {
  timestamp?: string;
  status: number;
  error?: string;
  message: string;
  path?: string;
  correlationId?: string;
  details?: string[];
  code?: string;
}

export type UsuarioRole = "ADMIN" | "GERENTE" | "CAIXA";

export interface Usuario {
  id: string;
  tenantId?: string;
  nome: string;
  email: string;
  role: UsuarioRole;
  ativo: boolean;
  createdAt?: string;
}

export interface UsuarioRequest {
  nome: string;
  email: string;
  role: UsuarioRole;
}

export interface Configuracao {
  id?: string;
  tenantId?: string;
  lojaId?: string;
  chave: string;
  valor: string;
  updatedAt?: string;
}

export interface ConfiguracaoRequest {
  lojaId?: string;
  chave: string;
  valor: string;
}

export interface Produto {
  id: string;
  tenantId?: string;
  sku: string;
  codigoBarras?: string;
  nome: string;
  descricao?: string;
  categoria?: string;
  unidadeMedida: string;
  precoVenda: number;
  precoCusto?: number;
  ativo: boolean;
  controlaEstoque: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProdutoRequest {
  sku: string;
  codigoBarras?: string;
  nome: string;
  descricao?: string;
  categoria?: string;
  unidadeMedida: string;
  precoVenda: number;
  precoCusto?: number;
  controlaEstoque: boolean;
}

export interface Estoque {
  id: string;
  tenantId?: string;
  lojaId: string;
  produtoId: string;
  produtoNome?: string;
  produtoSku?: string;
  quantidadeAtual: number;
  quantidadeMinima: number;
}

export interface AjusteEstoqueRequest {
  lojaId: string;
  produtoId: string;
  quantidade: number;
  motivo: string;
}

export interface MovimentacaoEstoque {
  id: string;
  produtoId: string;
  lojaId: string;
  tipo: TipoMovimentacaoEstoque;
  origem?: string;
  quantidade: number;
  quantidadeAnterior?: number;
  quantidadePosterior?: number;
  referenciaId?: string;
  createdBy?: string;
  createdAt: string;
}

export interface Cliente {
  id: string;
  tenantId?: string;
  nome: string;
  cpfCnpj?: string;
  email?: string;
  telefone?: string;
  ativo: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ClienteRequest {
  nome: string;
  cpfCnpj?: string;
  email?: string;
  telefone?: string;
}

export interface ItemVenda {
  id: string;
  produtoId: string;
  produtoNome?: string;
  produtoSku?: string;
  quantidade: number;
  precoUnitario: number;
  desconto: number;
  total: number;
}

export interface PagamentoVenda {
  id: string;
  formaPagamento: FormaPagamento;
  valor: number;
  status?: string;
}

export interface Venda {
  id: string;
  tenantId?: string;
  lojaId: string;
  numero?: number;
  clienteId?: string;
  clienteNome?: string;
  operadorId?: string;
  status: StatusVenda;
  subtotal: number;
  descontoTotal: number;
  total: number;
  formaPagamentoPrincipal?: FormaPagamento;
  itens?: ItemVenda[];
  pagamentos?: PagamentoVenda[];
  createdAt?: string;
  updatedAt?: string;
  finalizadaAt?: string;
}

export interface VendaRequest {
  lojaId: string;
  clienteId?: string;
}

export interface ItemVendaRequest {
  produtoId: string;
  quantidade: number;
  desconto: number;
}

export interface PagamentoVendaRequest {
  formaPagamento: FormaPagamento;
  valor: number;
}

export interface Caixa {
  id: string;
  tenantId?: string;
  lojaId: string;
  operadorId: string;
  status: StatusCaixa;
  valorAbertura: number;
  valorFechamentoInformado?: number;
  valorCalculado?: number;
  diferenca?: number;
  openedAt: string;
  closedAt?: string;
}

export interface AbrirCaixaRequest {
  lojaId: string;
  valorAbertura: number;
}

export interface FecharCaixaRequest {
  valorFechamentoInformado: number;
}

export interface CaixaResumo {
  caixaId: string;
  totalVendas: number;
  totalDinheiro: number;
  totalPix: number;
  totalCartao: number;
  totalOutros: number;
  quantidadeVendas: number;
  valorAbertura: number;
  valorCalculado: number;
}

export interface ContaReceber {
  id: string;
  tenantId?: string;
  lojaId: string;
  vendaId?: string;
  descricao: string;
  valor: number;
  status: StatusContaReceber;
  vencimento: string;
  pagamento?: string;
}

export interface DashboardResumo {
  vendasDoDia: number;
  faturamentoDoDia: number;
  ticketMedio: number;
  produtosEstoqueBaixo: Estoque[];
  ultimasVendas: Venda[];
  vendasPorFormaPagamento?: Record<FormaPagamento, number>;
}

export const FORMA_PAGAMENTO_LABELS: Record<FormaPagamento, string> = {
  DINHEIRO: "Dinheiro",
  PIX: "PIX",
  CARTAO_DEBITO: "Cartão Débito",
  CARTAO_CREDITO: "Cartão Crédito",
  VALE: "Vale",
  OUTROS: "Outros",
};

export const STATUS_VENDA_LABELS: Record<StatusVenda, string> = {
  ABERTA: "Aberta",
  FINALIZADA: "Finalizada",
  CANCELADA: "Cancelada",
};

export const STATUS_CAIXA_LABELS: Record<StatusCaixa, string> = {
  ABERTO: "Aberto",
  FECHADO: "Fechado",
};

export const TIPO_MOVIMENTACAO_LABELS: Record<TipoMovimentacaoEstoque, string> = {
  ENTRADA: "Entrada",
  SAIDA: "Saída",
  AJUSTE: "Ajuste",
  VENDA: "Venda",
  CANCELAMENTO_VENDA: "Cancelamento",
};

export const STATUS_CONTA_LABELS: Record<StatusContaReceber, string> = {
  ABERTO: "Em aberto",
  PAGO: "Pago",
  CANCELADO: "Cancelado",
};
