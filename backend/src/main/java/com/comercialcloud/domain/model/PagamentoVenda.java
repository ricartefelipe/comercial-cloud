package com.comercialcloud.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record PagamentoVenda(UUID id, FormaPagamento formaPagamento, BigDecimal valor) {}
