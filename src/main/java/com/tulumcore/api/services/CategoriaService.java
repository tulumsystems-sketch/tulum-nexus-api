package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Categoria;
import com.tulumcore.api.repositories.CategoriaRepository;
import com.tulumcore.api.services.AuditoryLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private AuditoryLogService auditoryLogService;

    public List<Categoria> getAllCategorias() {
        String tenant = TenantContext.getCurrentTenant();
        return categoriaRepository.findAllByTenantId(tenant);
    }

    public Optional<Categoria> getCategoriaById(Long id) {
        String tenant = TenantContext.getCurrentTenant();
        return categoriaRepository.findByIdAndTenantId(id, tenant);
    }

    public Categoria createOrUpdateCategoria(Categoria categoria) {
        boolean isNew = categoria.getId() == null;
        Categoria saved = categoriaRepository.save(categoria);
        if (isNew) {
            auditoryLogService.registrar("CREATE", "CATEGORIA", saved.getId(),
                    "Se creó la categoría: " + saved.getNombre(), null, null);
        } else {
            auditoryLogService.registrar("UPDATE", "CATEGORIA", saved.getId(),
                    "Se actualizó la categoría: " + saved.getNombre(), null, null);
        }
        return saved;
    }

    public void deleteCategoria(Long id) {
        getCategoriaById(id).ifPresent(c -> {
            categoriaRepository.deleteById(id);
            auditoryLogService.registrar("DELETE", "CATEGORIA", id,
                    "Se eliminó la categoría: " + c.getNombre(), null, null);
        });
    }

    public List<Categoria> getLatestCategorias(int limit) {
        String tenant = TenantContext.getCurrentTenant();
        return categoriaRepository.findAllByTenantId(tenant)
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(limit)
                .toList();
    }
}