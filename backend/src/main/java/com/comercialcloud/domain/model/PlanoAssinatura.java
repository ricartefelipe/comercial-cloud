package com.comercialcloud.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanoAssinatura(
        UUID id, String codigo, String nome, BigDecimal precoMensal, boolean ativo) {}
