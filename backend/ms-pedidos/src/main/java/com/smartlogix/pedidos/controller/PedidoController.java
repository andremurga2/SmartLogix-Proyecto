package com.smartlogix.pedidos.controller;

import com.smartlogix.pedidos.model.dto.PedidoDTO;
import com.smartlogix.pedidos.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Creación y consulta de pedidos, con compensación de stock ante fallos")
public class PedidoController {

    private final PedidoService pedidoService;

    @Operation(
            summary = "Crear pedido",
            description = "Valida y descuenta el stock de cada ítem contra ms-inventario (síncrono vía Feign), " +
                    "calcula el total y persiste el pedido. Si algún ítem falla por stock insuficiente, revierte " +
                    "automáticamente el stock ya descontado de los ítems anteriores (compensación tipo Saga) y " +
                    "publica el evento 'pedido.creado' en RabbitMQ para auditoría."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Stock insuficiente en alguno de los ítems solicitados")
    })
    @PostMapping
    public ResponseEntity<PedidoDTO> crearPedido(@RequestBody PedidoDTO pedidoDTO) {
        return ResponseEntity.ok(pedidoService.crearPedido(pedidoDTO));
    }

    @Operation(summary = "Listar pedidos", description = "Devuelve todos los pedidos registrados. Uso administrativo.")
    @ApiResponse(responseCode = "200", description = "Listado de pedidos")
    @GetMapping
    public ResponseEntity<List<PedidoDTO>> listarPedidos() {
        return ResponseEntity.ok(pedidoService.listarPedidos());
    }
}