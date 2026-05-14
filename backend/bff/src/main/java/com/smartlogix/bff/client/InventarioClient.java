package com.smartlogix.bff.client;

import com.smartlogix.bff.model.ProductoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "ms-inventario", url = "${smartlogix.inventario.url}")
public interface InventarioClient {

    @GetMapping("/api/inventario/productos")
    List<ProductoDTO> listarTodos();

    @GetMapping("/api/inventario/productos/{sku}")
    ProductoDTO obtenerPorSku(@PathVariable("sku") String sku);

    @PostMapping("/api/inventario/productos")
    ProductoDTO crearProducto(@RequestBody ProductoDTO productoDTO);

    @PutMapping("/api/inventario/productos/{sku}")
    ProductoDTO actualizarProducto(@PathVariable("sku") String sku, @RequestBody ProductoDTO productoDTO);

    @DeleteMapping("/api/inventario/productos/{sku}")
    void eliminarProducto(@PathVariable("sku") String sku);
}
