package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailAndTenantId(String email, String tenantId);

    List<Usuario> findAllByTenantId(String tenantId);

    Optional<Usuario> findByIdAndTenantId(Long id, String tenantId);
}
