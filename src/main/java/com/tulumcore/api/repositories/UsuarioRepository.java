package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Query nativa para que Hibernate NO aplique el filtro de tenant
    // Necesario para el login, donde el tenant aún no está en contexto
    @Query(value = "SELECT * FROM usuarios WHERE email = :email LIMIT 1", nativeQuery = true)
    Optional<Usuario> findByEmail(@Param("email") String email);

    // Estos sí filtran por tenant — para gestión interna
    List<Usuario> findAllByTenantId(String tenantId);

    Optional<Usuario> findByIdAndTenantId(Long id, String tenantId);
}