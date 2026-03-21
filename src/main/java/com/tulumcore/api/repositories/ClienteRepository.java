package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Segunda línea de defensa: siempre filtramos por tenant explícitamente
    List<Cliente> findAllByTenantId(String tenantId);

    Optional<Cliente> findByIdAndTenantId(Long id, String tenantId);
}