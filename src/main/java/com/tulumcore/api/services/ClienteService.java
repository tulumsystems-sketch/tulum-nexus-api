package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Cliente;
import com.tulumcore.api.repositories.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    public List<Cliente> getAllClientes() {
        // Siempre filtramos por el tenant del contexto actual
        String tenant = TenantContext.getCurrentTenant();
        return clienteRepository.findAllByTenantId(tenant);
    }

    public Optional<Cliente> getClienteById(Long id) {
        // Doble validación: id + tenant. Imposible acceder a datos de otro tenant.
        String tenant = TenantContext.getCurrentTenant();
        return clienteRepository.findByIdAndTenantId(id, tenant);
    }

    public Cliente createOrUpdateCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public void deleteCliente(Long id) {
        // Validamos que el cliente pertenece al tenant antes de borrar
        getClienteById(id).ifPresent(c -> clienteRepository.deleteById(id));
    }
}