package com.comercialcloud.domain.repository;

import com.comercialcloud.domain.model.MovimentoCaixa;

import java.util.List;
import java.util.UUID;

public interface MovimentoCaixaRepository {

    MovimentoCaixa save(MovimentoCaixa movimento);

    List<MovimentoCaixa> listarPorCaixa(UUID tenantId, UUID caixaId);
}
