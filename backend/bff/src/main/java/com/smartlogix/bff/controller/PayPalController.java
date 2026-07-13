package com.smartlogix.bff.controller;

import com.paypal.orders.Order;
import com.smartlogix.bff.model.*;
import com.smartlogix.bff.service.BffService;
import com.smartlogix.bff.service.PayPalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Tag(name = "Pagos (PayPal)", description = "Flujo de checkout en dos pasos con PayPal sandbox: crear orden y capturar pago")
public class PayPalController {

    private final PayPalService payPalService;
    private final BffService bffService;

    @Operation(
            summary = "Crear orden PayPal (paso 1)",
            description = "Crea la orden en PayPal con el desglose completo del carrito y devuelve la URL de " +
                    "aprobación a la que debe redirigirse al usuario."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden creada, URL de aprobación disponible"),
            @ApiResponse(responseCode = "400", description = "Carrito o moneda inválidos"),
            @ApiResponse(responseCode = "500", description = "Error de comunicación con la API de PayPal")
    })
    @PostMapping("/crear-orden")
    public ResponseEntity<CrearOrdenResponse> crearOrden(@RequestBody CrearOrdenRequest request) {
        try {
            Order order = payPalService.crearOrden(
                    request.getItems(),
                    request.getMoneda() != null ? request.getMoneda() : "USD"
            );

            String approveUrl = payPalService.obtenerApproveUrl(order);

            return ResponseEntity.ok(new CrearOrdenResponse(
                    order.id(),
                    approveUrl,
                    order.status()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new CrearOrdenResponse(null, null, "ERROR: " + e.getMessage()));
        } catch (IOException e) {
            log.error("Error al crear orden PayPal: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new CrearOrdenResponse(null, null, "ERROR: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "Capturar orden PayPal (paso 2)",
            description = "Captura el pago de una orden ya aprobada por el usuario en PayPal. Si el estado " +
                    "devuelto es COMPLETED, registra el pedido con todos sus ítems en ms-pedidos; si no, no " +
                    "se crea ningún pedido."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago capturado y pedido registrado"),
            @ApiResponse(responseCode = "400", description = "El pago no fue completado (estado distinto de COMPLETED)"),
            @ApiResponse(responseCode = "500", description = "Error al comunicarse con la API de PayPal")
    })
    @PostMapping("/capturar-orden")
    public ResponseEntity<PagoResponse> capturarOrden(@RequestBody CapturarOrdenRequest request) {
        try {
            // 1. Capturar el pago en PayPal
            Order capturedOrder = payPalService.capturarOrden(request.getOrderId());

            if (!"COMPLETED".equals(capturedOrder.status())) {
                return ResponseEntity.badRequest().body(new PagoResponse(
                        false,
                        "El pago no fue completado. Estado: " + capturedOrder.status(),
                        request.getOrderId(),
                        capturedOrder.status(),
                        null
                ));
            }

            // 2. Pago exitoso → registrar pedido con todos los ítems en ms-pedidos
            List<PedidoItemDTO> itemsDTO = request.getItems().stream()
                    .map(item -> PedidoItemDTO.builder()
                            .skuProducto(item.getSkuProducto())
                            .cantidad(item.getCantidad())
                            .build())
                    .collect(Collectors.toList());

            PedidoDTO pedidoDTO = PedidoDTO.builder()
                    .items(itemsDTO)
                    .paypalOrderId(request.getOrderId())
                    .build();

            PedidoDTO pedidoCreado = bffService.realizarCompra(pedidoDTO);

            return ResponseEntity.ok(new PagoResponse(
                    true,
                    "Pago procesado y pedido registrado exitosamente.",
                    request.getOrderId(),
                    capturedOrder.status(),
                    pedidoCreado
            ));

        } catch (IOException e) {
            log.error("Error al capturar orden PayPal {}: {}", request.getOrderId(), e.getMessage());
            return ResponseEntity.internalServerError().body(new PagoResponse(
                    false,
                    "Error al capturar el pago: " + e.getMessage(),
                    request.getOrderId(),
                    "ERROR",
                    null
            ));
        }
    }

    @Operation(
            summary = "Webhook de PayPal",
            description = "Recibe notificaciones asíncronas de eventos de PayPal (ej. PAYMENT.CAPTURE.COMPLETED). " +
                    "Requiere una URL pública HTTPS accesible por PayPal — en desarrollo se usa un túnel como ngrok."
    )
    @ApiResponse(responseCode = "200", description = "Evento recibido y registrado en log")
    @PostMapping("/webhook")
    public ResponseEntity<Void> recibirWebhook(
            @Parameter(description = "Id de transmisión enviado por PayPal para trazabilidad del evento")
            @RequestHeader(value = "Paypal-Transmission-Id", required = false) String transmissionId,
            @RequestBody String payload) {

        log.info("Webhook PayPal recibido. TransmissionId: {}", transmissionId);
        log.debug("Payload: {}", payload);
        return ResponseEntity.ok().build();
    }
}