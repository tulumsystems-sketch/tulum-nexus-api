package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Segunda línea de defensa: siempre filtramos por tenant explícitamente
    List<Categoria> findAllByTenantId(String tenantId);

    Optional<Categoria> findByIdAndTenantId(Long id, String tenantId);
}