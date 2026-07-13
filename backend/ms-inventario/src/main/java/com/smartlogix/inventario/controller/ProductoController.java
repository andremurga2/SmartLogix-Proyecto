package com.smartlogix.inventario.controller;

import com.smartlogix.inventario.model.dto.ProductoDTO;
import com.smartlogix.inventario.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/productos")
@RequiredArgsConstructor
@Tag(name = "Inventario", description = "Catálogo de productos y control de stock")
public class ProductoController {

    private final ProductoService productoService;

    @Operation(summary = "Listar catálogo", description = "Devuelve todos los productos disponibles en inventario.")
    @ApiResponse(responseCode = "200", description = "Listado de productos")
    @GetMapping
    public ResponseEntity<List<ProductoDTO>> listarTodos() {
        return ResponseEntity.ok(productoService.obtenerTodos());
    }

    @Operation(summary = "Obtener producto por SKU", description = "Devuelve el detalle de un producto puntual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un producto con ese SKU")
    })
    @GetMapping("/{sku}")
    public ResponseEntity<ProductoDTO> obtenerPorSku(@PathVariable String sku) {
        return ResponseEntity.ok(productoService.obtenerPorSku(sku));
    }

    @Operation(summary = "Crear producto", description = "Da de alta un nuevo producto en el catálogo. Uso administrativo.")
    @ApiResponse(responseCode = "201", description = "Producto creado")
    @PostMapping
    public ResponseEntity<ProductoDTO> crearProducto(@RequestBody ProductoDTO productoDTO) {
        ProductoDTO creado = productoService.crearProducto(productoDTO);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar producto", description = "Modifica los datos de un producto existente, identificado por SKU.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado"),
            @ApiResponse(responseCode = "404", description = "No existe un producto con ese SKU")
    })
    @PutMapping("/{sku}")
    public ResponseEntity<ProductoDTO> actualizarProducto(
            @PathVariable String sku,
            @RequestBody ProductoDTO productoDTO) {
        return ResponseEntity.ok(productoService.actualizarProducto(sku, productoDTO));
    }

    @Operation(summary = "Eliminar producto", description = "Elimina un producto del catálogo por SKU.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto eliminado"),
            @ApiResponse(responseCode = "404", description = "No existe un producto con ese SKU")
    })
    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable String sku) {
        productoService.eliminarProducto(sku);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Descontar stock",
            description = "Reduce el stock disponible de un SKU. Llamado de forma síncrona por ms-pedidos " +
                    "al confirmar cada ítem de un pedido nuevo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Stock descontado"),
            @ApiResponse(responseCode = "404", description = "SKU inexistente o stock insuficiente")
    })
    @PutMapping("/{sku}/descontar-stock")
    public ResponseEntity<Void> descontarStock(@PathVariable String sku, @RequestParam Integer cantidad) {
        productoService.actualizarStock(sku, cantidad);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Revertir stock (compensación Saga)",
            description = "Devuelve stock previamente descontado. Lo invoca ms-pedidos cuando falla la creación " +
                    "de un pedido después de haber descontado stock de ítems anteriores, como parte de la " +
                    "compensación transaccional entre servicios."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Stock revertido"),
            @ApiResponse(responseCode = "404", description = "SKU inexistente")
    })
    @PutMapping("/{sku}/revertir-stock")
    public ResponseEntity<Void> revertirStock(@PathVariable String sku, @RequestParam Integer cantidad) {
        productoService.revertirStock(sku, cantidad);
        return ResponseEntity.noContent().build();
    }
}