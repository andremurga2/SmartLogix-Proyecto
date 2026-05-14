package com.smartlogix.inventario.service;

import com.smartlogix.inventario.model.dto.ProductoDTO;
import java.util.List;

public interface ProductoService {
    List<ProductoDTO> obtenerTodos();
    ProductoDTO obtenerPorSku(String sku);
    ProductoDTO crearProducto(ProductoDTO productoDTO);
    ProductoDTO actualizarProducto(String sku, ProductoDTO productoDTO);
    void eliminarProducto(String sku);
    void actualizarStock(String sku, Integer cantidadComprada);
}
