package com.tulumcore.api.repositories;

import com.tulumcore.api.entities.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
