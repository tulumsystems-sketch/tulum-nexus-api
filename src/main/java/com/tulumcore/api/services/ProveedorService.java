package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Proveedor;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.ProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorService {

    @Autowired
    private ProveedorRepository repository;

    @Autowired
    private AuditoryLogService auditoryLogService;

    public List<Proveedor> getAll() {
        return repository.findAllByTenantId(TenantContext.getCurrentTenant());
    }

    public Optional<Proveedor> getById(Long id) {
        return repository.findByIdAndTenantId(id, TenantContext.getCurrentTenant());
    }

    @Transactional
    public Proveedor save(Proveedor proveedor) {
        String tenant = TenantContext.getCurrentTenant();
        proveedor.setTenantId(tenant);

        boolean isNew = proveedor.getId() == null;
        String detalleAnterior = null;
        if (!isNew) {
            Proveedor existente = repository.findByIdAndTenantId(proveedor.getId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + proveedor.getId()));
            detalleAnterior = detalleProveedor(existente);
        }

        Proveedor saved = repository.save(proveedor);
        auditoryLogService.registrar(isNew ? "CREATE" : "UPDATE", "PROVEEDOR", saved.getId(),
                (isNew ? "Proveedor creado: " : "Proveedor actualizado: ") + saved.getNombre(),
                detalleAnterior, detalleProveedor(saved));
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        String tenant = TenantContext.getCurrentTenant();
        Proveedor proveedor = repository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + id));
        String detalleAnterior = detalleProveedor(proveedor);
        repository.delete(proveedor);
        auditoryLogService.registrar("DELETE", "PROVEEDOR", id,
                "Proveedor eliminado: " + proveedor.getNombre(), detalleAnterior, null);
    }

    private String detalleProveedor(Proveedor proveedor) {
        return auditoryLogService.detalle(
                "nombre", proveedor.getNombre(),
                "contacto", proveedor.getContacto(),
                "telefono", proveedor.getTelefono(),
                "email", proveedor.getEmail(),
                "direccion", proveedor.getDireccion(),
                "cuit", proveedor.getCuit(),
                "observaciones", proveedor.getObservaciones()
        );
    }
}
