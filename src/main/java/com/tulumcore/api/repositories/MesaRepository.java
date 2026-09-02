package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    List<Mesa> findAllByTenantIdOrderByNumeroAsc(String tenantId);

    Optional<Mesa> findByIdAndTenantId(Long id, String tenantId);

    boolean existsByTenantIdAndNumero(String tenantId, Integer numero);

    Optional<Mesa> findByTenantIdAndNumero(String tenantId, Integer numero);
}
