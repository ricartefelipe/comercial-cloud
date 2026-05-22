package com.comercialcloud.application.cliente;

import com.comercialcloud.domain.exception.NotFoundException;
import com.comercialcloud.domain.model.Cliente;
import com.comercialcloud.domain.repository.ClienteRepository;
import com.comercialcloud.infrastructure.audit.AuditService;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import com.comercialcloud.domain.shared.PageResult;

import java.util.UUID;

@ApplicationScoped
public class ClienteService {

    @Inject TenantContext tenantContext;
    @Inject AuditService auditService;
    @Inject ClienteRepository clienteRepository;

    @Transactional
    public Cliente criar(String nome, String cpfCnpj, String email, String telefone) {
        UUID tenantId = tenantContext.getTenantId();
        Instant now = Instant.now();
        Cliente cliente =
                new Cliente(UUID.randomUUID(), tenantId, nome, cpfCnpj, email, telefone, true, now);
        Cliente salvo = clienteRepository.save(cliente);
        auditService.registrar("Cliente", salvo.id(), "CRIAR", nome);
        return salvo;
    }

    @Transactional
    public Cliente atualizar(UUID id, String nome, String cpfCnpj, String email, String telefone) {
        Cliente existente = obter(id);
        Cliente atualizado =
                new Cliente(
                        existente.id(),
                        existente.tenantId(),
                        nome,
                        cpfCnpj,
                        email,
                        telefone,
                        existente.ativo(),
                        existente.createdAt());
        Cliente salvo = clienteRepository.save(atualizado);
        auditService.registrar("Cliente", salvo.id(), "ATUALIZAR", nome);
        return salvo;
    }

    public Cliente buscarPorId(UUID id) {
        return obter(id);
    }

    public PageResult<Cliente> listar(int page, int size) {
        return clienteRepository.list(tenantContext.getTenantId(), page, size);
    }

    @Transactional
    public void excluir(UUID id) {
        clienteRepository.delete(tenantContext.getTenantId(), id);
        auditService.registrar("Cliente", id, "EXCLUIR", null);
    }

    private Cliente obter(UUID id) {
        return clienteRepository
                .find(tenantContext.getTenantId(), id)
                .orElseThrow(() -> new NotFoundException("CLIENTE_NOT_FOUND", "Cliente não encontrado"));
    }
}
