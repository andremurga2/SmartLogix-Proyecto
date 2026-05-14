package com.smartlogix.inventario.repository;

import com.smartlogix.inventario.model.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Patrón Repository.
 * Abstrae el acceso a datos y aísla la lógica de persistencia de la lógica de negocio.
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Optional<Producto> findBySku(String sku);
}
