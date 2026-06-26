package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Proveedor;
import com.tulumcore.api.repositories.ProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorService {

    @Autowired
    private ProveedorRepository repository;

    public List<Proveedor> getAll() {
        return repository.findAllByTenantId(TenantContext.getCurrentTenant());
    }

    public Optional<Proveedor> getById(Long id) {
        return repository.findByIdAndTenantId(id, TenantContext.getCurrentTenant());
    }

    public Proveedor save(Proveedor proveedor) {
        proveedor.setTenantId(TenantContext.getCurrentTenant());
        return repository.save(proveedor);
    }

    public void delete(Long id) {
        getById(id).ifPresent(p -> repository.deleteById(id));
    }
}
