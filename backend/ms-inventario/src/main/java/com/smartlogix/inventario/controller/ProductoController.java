package com.smartlogix.inventario.controller;

import com.smartlogix.inventario.model.dto.ProductoDTO;
import com.smartlogix.inventario.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<List<ProductoDTO>> listarTodos() {
        return ResponseEntity.ok(productoService.obtenerTodos());
    }

    @GetMapping("/{sku}")
    public ResponseEntity<ProductoDTO> obtenerPorSku(@PathVariable String sku) {
        return ResponseEntity.ok(productoService.obtenerPorSku(sku));
    }

    @PostMapping
    public ResponseEntity<ProductoDTO> crearProducto(@RequestBody ProductoDTO productoDTO) {
        ProductoDTO creado = productoService.crearProducto(productoDTO);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PutMapping("/{sku}")
    public ResponseEntity<ProductoDTO> actualizarProducto(
            @PathVariable String sku,
            @RequestBody ProductoDTO productoDTO) {
        return ResponseEntity.ok(productoService.actualizarProducto(sku, productoDTO));
    }

    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable String sku) {
        productoService.eliminarProducto(sku);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{sku}/descontar-stock")
    public ResponseEntity<Void> descontarStock(@PathVariable String sku, @RequestParam Integer cantidad) {
        productoService.actualizarStock(sku, cantidad);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{sku}/revertir-stock")
    public ResponseEntity<Void> revertirStock(@PathVariable String sku, @RequestParam Integer cantidad) {
        productoService.revertirStock(sku, cantidad);
        return ResponseEntity.noContent().build();
    }
}
