package com.comercialcloud.infrastructure.persistence.mapper;

import com.comercialcloud.domain.model.Caixa;
import com.comercialcloud.domain.model.Cliente;
import com.comercialcloud.domain.model.Estoque;
import com.comercialcloud.domain.model.ItemVenda;
import com.comercialcloud.domain.model.Loja;
import com.comercialcloud.domain.model.MovimentacaoEstoque;
import com.comercialcloud.domain.model.PagamentoVenda;
import com.comercialcloud.domain.model.Produto;
import com.comercialcloud.domain.model.StatusCaixa;
import com.comercialcloud.domain.model.StatusVenda;
import com.comercialcloud.domain.model.TipoMovimentacaoEstoque;
import com.comercialcloud.domain.model.FormaPagamento;
import com.comercialcloud.domain.model.Tenant;
import com.comercialcloud.domain.model.Usuario;
import com.comercialcloud.domain.model.Venda;
import com.comercialcloud.infrastructure.persistence.entity.CaixaEntity;
import com.comercialcloud.infrastructure.persistence.entity.ClienteEntity;
import com.comercialcloud.infrastructure.persistence.entity.EstoqueEntity;
import com.comercialcloud.infrastructure.persistence.entity.ItemVendaEntity;
import com.comercialcloud.infrastructure.persistence.entity.LojaEntity;
import com.comercialcloud.infrastructure.persistence.entity.MovimentacaoEstoqueEntity;
import com.comercialcloud.infrastructure.persistence.entity.PagamentoVendaEntity;
import com.comercialcloud.infrastructure.persistence.entity.ProdutoEntity;
import com.comercialcloud.infrastructure.persistence.entity.TenantEntity;
import com.comercialcloud.infrastructure.persistence.entity.UsuarioEntity;
import com.comercialcloud.infrastructure.persistence.entity.VendaEntity;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class EntityMapper {

    public Tenant toDomain(TenantEntity entity) {
        return new Tenant(entity.id, entity.nome, entity.ativo, entity.createdAt);
    }

    public TenantEntity toTenantEntity(Tenant tenant) {
        TenantEntity entity = new TenantEntity();
        entity.id = tenant.id();
        entity.nome = tenant.nome();
        entity.ativo = tenant.ativo();
        entity.createdAt = tenant.createdAt();
        return entity;
    }

    public Loja toDomain(LojaEntity entity) {
        return new Loja(entity.id, entity.tenantId, entity.nome, entity.ativa, entity.createdAt);
    }

    public LojaEntity toEntity(Loja loja) {
        LojaEntity entity = new LojaEntity();
        entity.id = loja.id();
        entity.tenantId = loja.tenantId();
        entity.nome = loja.nome();
        entity.ativa = loja.ativa();
        entity.createdAt = loja.createdAt();
        return entity;
    }

    public Usuario toDomain(UsuarioEntity entity) {
        return new Usuario(entity.id, entity.tenantId, entity.nome, entity.email, entity.role, entity.ativo, entity.createdAt);
    }

    public UsuarioEntity toEntity(Usuario usuario) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.id = usuario.id();
        entity.tenantId = usuario.tenantId();
        entity.nome = usuario.nome();
        entity.email = usuario.email();
        entity.role = usuario.role();
        entity.ativo = usuario.ativo();
        entity.createdAt = usuario.createdAt();
        return entity;
    }

    public Produto toDomain(ProdutoEntity entity) {
        return new Produto(
                entity.id,
                entity.tenantId,
                entity.sku,
                entity.codigoBarras,
                entity.nome,
                entity.descricao,
                entity.preco,
                entity.ativo,
                entity.createdAt,
                entity.updatedAt);
    }

    public ProdutoEntity toEntity(Produto produto) {
        ProdutoEntity entity = new ProdutoEntity();
        entity.id = produto.id();
        entity.tenantId = produto.tenantId();
        entity.sku = produto.sku();
        entity.codigoBarras = produto.codigoBarras();
        entity.nome = produto.nome();
        entity.descricao = produto.descricao();
        entity.preco = produto.preco();
        entity.ativo = produto.ativo();
        entity.createdAt = produto.createdAt();
        entity.updatedAt = produto.updatedAt();
        return entity;
    }

    public Cliente toDomain(ClienteEntity entity) {
        return new Cliente(
                entity.id,
                entity.tenantId,
                entity.nome,
                entity.cpfCnpj,
                entity.email,
                entity.telefone,
                entity.ativo,
                entity.createdAt);
    }

    public ClienteEntity toEntity(Cliente cliente) {
        ClienteEntity entity = new ClienteEntity();
        entity.id = cliente.id();
        entity.tenantId = cliente.tenantId();
        entity.nome = cliente.nome();
        entity.cpfCnpj = cliente.cpfCnpj();
        entity.email = cliente.email();
        entity.telefone = cliente.telefone();
        entity.ativo = cliente.ativo();
        entity.createdAt = cliente.createdAt();
        return entity;
    }

    public Estoque toDomain(EstoqueEntity entity) {
        return new Estoque(entity.id, entity.tenantId, entity.lojaId, entity.produtoId, entity.quantidade, entity.updatedAt);
    }

    public EstoqueEntity toEntity(Estoque estoque) {
        EstoqueEntity entity = new EstoqueEntity();
        entity.id = estoque.id();
        entity.tenantId = estoque.tenantId();
        entity.lojaId = estoque.lojaId();
        entity.produtoId = estoque.produtoId();
        entity.quantidade = estoque.quantidade();
        entity.updatedAt = estoque.updatedAt();
        return entity;
    }

    public MovimentacaoEstoque toDomain(MovimentacaoEstoqueEntity entity) {
        return new MovimentacaoEstoque(
                entity.id,
                entity.tenantId,
                entity.lojaId,
                entity.produtoId,
                TipoMovimentacaoEstoque.valueOf(entity.tipo),
                entity.quantidade,
                entity.saldoAnterior,
                entity.saldoPosterior,
                entity.motivo,
                entity.referenciaId,
                entity.createdAt);
    }

    public MovimentacaoEstoqueEntity toEntity(MovimentacaoEstoque movimentacao) {
        MovimentacaoEstoqueEntity entity = new MovimentacaoEstoqueEntity();
        entity.id = movimentacao.id();
        entity.tenantId = movimentacao.tenantId();
        entity.lojaId = movimentacao.lojaId();
        entity.produtoId = movimentacao.produtoId();
        entity.tipo = movimentacao.tipo().name();
        entity.quantidade = movimentacao.quantidade();
        entity.saldoAnterior = movimentacao.saldoAnterior();
        entity.saldoPosterior = movimentacao.saldoPosterior();
        entity.motivo = movimentacao.motivo();
        entity.referenciaId = movimentacao.referenciaId();
        entity.createdAt = movimentacao.createdAt();
        return entity;
    }

    public Caixa toDomain(CaixaEntity entity) {
        return new Caixa(
                entity.id,
                entity.tenantId,
                entity.lojaId,
                entity.usuarioId,
                StatusCaixa.valueOf(entity.status),
                entity.valorAbertura,
                entity.valorFechamento,
                entity.totalVendas,
                entity.abertoEm,
                entity.fechadoEm);
    }

    public CaixaEntity toEntity(Caixa caixa) {
        CaixaEntity entity = new CaixaEntity();
        entity.id = caixa.id();
        entity.tenantId = caixa.tenantId();
        entity.lojaId = caixa.lojaId();
        entity.usuarioId = caixa.usuarioId();
        entity.status = caixa.status().name();
        entity.valorAbertura = caixa.valorAbertura();
        entity.valorFechamento = caixa.valorFechamento();
        entity.totalVendas = caixa.totalVendas();
        entity.abertoEm = caixa.abertoEm();
        entity.fechadoEm = caixa.fechadoEm();
        return entity;
    }

    public Venda toDomain(VendaEntity entity) {
        List<ItemVenda> itens = new ArrayList<>();
        if (entity.itens != null) {
            for (ItemVendaEntity item : entity.itens) {
                itens.add(
                        new ItemVenda(item.id, item.produtoId, item.quantidade, item.precoUnitario, item.subtotal));
            }
        }
        List<PagamentoVenda> pagamentos = new ArrayList<>();
        if (entity.pagamentos != null) {
            for (PagamentoVendaEntity p : entity.pagamentos) {
                pagamentos.add(
                        new PagamentoVenda(
                                p.id, FormaPagamento.valueOf(p.formaPagamento), p.valor));
            }
        }
        return new Venda(
                entity.id,
                entity.tenantId,
                entity.lojaId,
                entity.clienteId,
                entity.usuarioId,
                entity.caixaId,
                StatusVenda.valueOf(entity.status),
                entity.subtotal,
                entity.desconto,
                entity.total,
                entity.createdAt,
                entity.finalizedAt,
                entity.canceledAt,
                List.copyOf(itens),
                List.copyOf(pagamentos));
    }

    /**
     * Venda nova para persistência (coleções vazias serão preenchidas em {@link #sincronizarVendaGerenciada}).
     */
    public VendaEntity toEntityNovo(Venda venda) {
        VendaEntity entity = new VendaEntity();
        entity.itens = new ArrayList<>();
        entity.pagamentos = new ArrayList<>();
        sincronizarVendaGerenciada(entity, venda);
        return entity;
    }

    public void sincronizarVendaGerenciada(VendaEntity entity, Venda venda) {
        aplicarCabecalhoVenda(entity, venda);
        entity.itens.clear();
        for (ItemVenda item : venda.itens()) {
            ItemVendaEntity row = new ItemVendaEntity();
            row.id = item.id();
            row.tenantId = venda.tenantId();
            row.venda = entity;
            row.produtoId = item.produtoId();
            row.quantidade = item.quantidade();
            row.precoUnitario = item.precoUnitario();
            row.subtotal = item.subtotal();
            entity.itens.add(row);
        }
        entity.pagamentos.clear();
        for (PagamentoVenda p : venda.pagamentos()) {
            PagamentoVendaEntity row = new PagamentoVendaEntity();
            row.id = p.id();
            row.tenantId = venda.tenantId();
            row.venda = entity;
            row.formaPagamento = p.formaPagamento().name();
            row.valor = p.valor();
            entity.pagamentos.add(row);
        }
    }

    private static void aplicarCabecalhoVenda(VendaEntity entity, Venda venda) {
        entity.id = venda.id();
        entity.tenantId = venda.tenantId();
        entity.lojaId = venda.lojaId();
        entity.clienteId = venda.clienteId();
        entity.usuarioId = venda.usuarioId();
        entity.caixaId = venda.caixaId();
        entity.status = venda.status().name();
        entity.subtotal = venda.subtotal();
        entity.desconto = venda.desconto();
        entity.total = venda.total();
        entity.createdAt = venda.createdAt();
        entity.finalizedAt = venda.finalizedAt();
        entity.canceledAt = venda.canceledAt();
    }
}
