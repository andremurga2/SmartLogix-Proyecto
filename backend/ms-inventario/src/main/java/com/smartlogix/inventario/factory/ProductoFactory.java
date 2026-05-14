package com.smartlogix.inventario.factory;

import com.smartlogix.inventario.model.dto.ProductoDTO;
import com.smartlogix.inventario.model.entity.Producto;
import org.springframework.stereotype.Component;

/**
 * Patrón Factory Method.
 * Encapsula la lógica de creación y mapeo entre Entidades (BD) y DTOs (Transferencia).
 */
@Component
public class ProductoFactory {

    public ProductoDTO toDTO(Producto producto) {
        if (producto == null) return null;
        
        return ProductoDTO.builder()
                .id(producto.getId())
                .sku(producto.getSku())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .precio(producto.getPrecio())
                .stockActual(producto.getStockActual())
                .disponible(producto.getStockActual() != null && producto.getStockActual() > 0)
                .build();
    }

    public Producto toEntity(ProductoDTO dto) {
        if (dto == null) return null;

        return Producto.builder()
                .id(dto.getId())
                .sku(dto.getSku())
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .precio(dto.getPrecio())
                .stockActual(dto.getStockActual() != null ? dto.getStockActual() : 0)
                .build();
    }
}
