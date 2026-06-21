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
  | "INVENTARIO"
  | "AJUSTE"
  | "VENDA"
  | "CANCELAMENTO_VENDA";

export type TipoMovimentoCaixa = "SANGRIA" | "SUPRIMENTO";

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
  imagemUrl?: string;
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
  imagemUrl?: string;
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
  /** Valor da linha retornado pela API. */
  subtotal: number;
  /** Legado — preferir `subtotal`. */
  total?: number;
  desconto?: number;
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
  totalSangrias?: number;
  totalSuprimentos?: number;
  valorGavetaEsperada?: number;
}

export interface MovimentoCaixa {
  id: string;
  caixaId: string;
  lojaId: string;
  usuarioId: string;
  tipo: TipoMovimentoCaixa;
  valor: number;
  motivo?: string;
  createdAt: string;
}

export interface MovimentoCaixaRequest {
  tipo: TipoMovimentoCaixa;
  valor: number;
  motivo: string;
}

export interface AuditoriaRegistro {
  id: string;
  tenantId?: string;
  entidade: string;
  entidadeId: string;
  acao: string;
  usuarioId?: string;
  detalhes?: string;
  createdAt: string;
}

export interface RelatorioAbcItem {
  produtoId: string;
  sku?: string;
  nome?: string;
  categoria?: string;
  faturamento: number;
  quantidadeVendida: number;
  participacaoPercentual: number;
  participacaoAcumulada: number;
  classeAbc: "A" | "B" | "C";
}

export interface RelatorioMargemItem {
  produtoId: string;
  sku?: string;
  nome?: string;
  categoria?: string;
  precoCusto: number;
  precoVenda: number;
  quantidadeVendida: number;
  faturamento: number;
  custoTotal: number;
  lucroBruto: number;
  margemPercentual: number;
}

export interface ImportacaoProdutoResult {
  criados: number;
  ignorados: number;
  erros: string[];
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
  vendasPorFormaPagamento?: Record<string, number>;
  topProdutos?: TopProdutoResumo[];
}

export interface TopProdutoResumo {
  produtoId: string;
  produtoNome: string;
  produtoSku: string;
  quantidadeVendida: number;
  faturamento: number;
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
  INVENTARIO: "Inventário",
  AJUSTE: "Ajuste",
  VENDA: "Venda",
  CANCELAMENTO_VENDA: "Cancelamento",
};

export const TIPO_MOVIMENTO_CAIXA_LABELS: Record<TipoMovimentoCaixa, string> = {
  SANGRIA: "Sangria",
  SUPRIMENTO: "Suprimento",
};

export const STATUS_CONTA_LABELS: Record<StatusContaReceber, string> = {
  ABERTO: "Em aberto",
  PAGO: "Pago",
  CANCELADO: "Cancelado",
};

export type StatusAssinatura =
  | "TRIAL"
  | "ATIVA"
  | "SUSPENSA"
  | "CANCELADA"
  | "INADIMPLENTE";

export type StatusFatura = "PENDENTE" | "PAGA" | "VENCIDA" | "FALHA" | "CANCELADA";

export type StatusDocumentoFiscal =
  | "PENDENTE"
  | "AUTORIZADO"
  | "REJEITADO"
  | "CANCELADO"
  | "ERRO";

export type AmbienteFiscal = "HOMOLOGACAO" | "PRODUCAO";
export type RegimeTributario = "SIMPLES_NACIONAL" | "LUCRO_PRESUMIDO" | "LUCRO_REAL";

export interface PlanoPublico {
  codigo: string;
  nome: string;
  precoMensal: number;
}

export interface PlanoAssinatura extends PlanoPublico {
  id?: string;
  ativo?: boolean;
}

export interface OnboardingRequest {
  nomeFantasia: string;
  razaoSocial: string;
  cnpj: string;
  adminNome: string;
  adminEmail: string;
  adminSenha?: string;
  planoCodigo?: string;
  comTrial?: boolean;
  seedDemo?: boolean;
}

export interface OnboardingResult {
  tenantId: string;
  lojaId: string;
  adminUserId: string;
  planoCodigo: string;
  assinaturaStatus: StatusAssinatura;
  trialFim?: string;
  produtosDemo: number;
  proximoPasso?: string;
  loginEmail?: string;
  loginSenha?: string;
  modoAcesso?: string;
}

export interface AssinaturaTenant {
  id: string;
  tenantId: string;
  planoId: string;
  status: StatusAssinatura;
  inicio?: string;
  proximaCobranca?: string;
  trialFim?: string;
  tentativasFalha?: number;
}

export interface FaturaAssinatura {
  id: string;
  assinaturaId: string;
  competencia?: string;
  valor: number;
  vencimento?: string;
  status: StatusFatura;
  tentativas?: number;
  transacaoRef?: string;
  motivoRecusa?: string;
  pagaEm?: string;
  checkoutUrl?: string;
  gatewayPaymentId?: string;
}

export interface ConfiguracaoFiscal {
  id?: string;
  lojaId: string;
  cnpj: string;
  inscricaoEstadual?: string;
  razaoSocial: string;
  regimeTributario: RegimeTributario;
  ambiente: AmbienteFiscal;
  uf: string;
  serieNfce?: number;
  proximoNumeroNfce?: number;
  habilitado: boolean;
}

export interface ConfiguracaoFiscalRequest {
  lojaId: string;
  cnpj: string;
  inscricaoEstadual?: string;
  razaoSocial: string;
  regimeTributario: RegimeTributario;
  ambiente: AmbienteFiscal;
  uf: string;
  cscId?: string;
  csc?: string;
  serieNfce?: number;
  habilitado: boolean;
}

export interface DocumentoFiscal {
  id: string;
  lojaId: string;
  vendaId: string;
  modelo: string;
  status: StatusDocumentoFiscal;
  numero?: number;
  serie?: number;
  chaveAcesso?: string;
  protocolo?: string;
  motivoRejeicao?: string;
  urlDanfe?: string;
  tentativas?: number;
  createdAt?: string;
  updatedAt?: string;
}

export const STATUS_ASSINATURA_LABELS: Record<StatusAssinatura, string> = {
  TRIAL: "Trial",
  ATIVA: "Ativa",
  SUSPENSA: "Suspensa",
  CANCELADA: "Cancelada",
  INADIMPLENTE: "Inadimplente",
};

export const STATUS_FATURA_LABELS: Record<StatusFatura, string> = {
  PENDENTE: "Pendente",
  PAGA: "Paga",
  VENCIDA: "Vencida",
  FALHA: "Falha",
  CANCELADA: "Cancelada",
};

export const STATUS_DOCUMENTO_FISCAL_LABELS: Record<StatusDocumentoFiscal, string> = {
  PENDENTE: "Pendente",
  AUTORIZADO: "Autorizada",
  REJEITADO: "Rejeitada",
  CANCELADO: "Cancelada",
  ERRO: "Erro",
};

export interface Loja {
  id: string;
  tenantId: string;
  nome: string;
  ativa: boolean;
  createdAt?: string;
}

export interface LojaResumo {
  limite: number;
  ativas: number;
  podeCriar: boolean;
}

export interface BrandingConfig {
  nomeExibicao: string;
  corPrimaria: string;
  logoUrl?: string;
}
