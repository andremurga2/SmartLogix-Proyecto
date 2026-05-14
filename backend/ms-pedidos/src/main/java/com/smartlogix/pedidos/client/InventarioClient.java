package com.smartlogix.pedidos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ms-inventario", url = "${smartlogix.inventario.url}")
public interface InventarioClient {

    @GetMapping("/api/inventario/productos/{sku}")
    ProductoResponse obtenerProducto(@PathVariable("sku") String sku);

    @PutMapping("/api/inventario/productos/{sku}/descontar-stock")
    void descontarStock(@PathVariable("sku") String sku, @RequestParam("cantidad") Integer cantidad);
}
