package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Categoria;
import com.tulumcore.api.repositories.CategoriaRepository;
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

    public List<Categoria> getAllCategorias() {
        String tenant = TenantContext.getCurrentTenant();
        return categoriaRepository.findAllByTenantId(tenant);
    }

    public Optional<Categoria> getCategoriaById(Long id) {
        String tenant = TenantContext.getCurrentTenant();
        return categoriaRepository.findByIdAndTenantId(id, tenant);
    }

    public Categoria createOrUpdateCategoria(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    public void deleteCategoria(Long id) {
        getCategoriaById(id).ifPresent(c -> categoriaRepository.deleteById(id));
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