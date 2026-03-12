package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Spring Data JPA arma la query de SQL automáticamente con solo leer el nombre del método.
    Optional<Usuario> findByEmail(String email);
}