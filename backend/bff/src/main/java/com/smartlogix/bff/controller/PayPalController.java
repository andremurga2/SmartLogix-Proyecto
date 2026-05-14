package com.smartlogix.bff.controller;

import com.paypal.orders.Order;
import com.smartlogix.bff.model.*;
import com.smartlogix.bff.service.BffService;
import com.smartlogix.bff.service.PayPalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class PayPalController {

    private final PayPalService payPalService;
    private final BffService bffService;

    /**
     * PASO 1 — Crea la orden en PayPal y devuelve la URL de aprobación del usuario.
     * El frontend redirige (o abre popup) con approveUrl.
     */
    @PostMapping("/crear-orden")
    public ResponseEntity<CrearOrdenResponse> crearOrden(@RequestBody CrearOrdenRequest request) {
        try {
            Order order = payPalService.crearOrden(
                    request.getMonto(),
                    request.getMoneda() != null ? request.getMoneda() : "USD",
                    request.getDescripcion() != null ? request.getDescripcion() : "Compra SmartLogix"
            );

            String approveUrl = payPalService.obtenerApproveUrl(order);

            return ResponseEntity.ok(new CrearOrdenResponse(
                    order.id(),
                    approveUrl,
                    order.status()
            ));

        } catch (IOException e) {
            log.error("Error al crear orden PayPal: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new CrearOrdenResponse(null, null, "ERROR: " + e.getMessage()));
        }
    }

    /**
     * PASO 2 — Captura el pago de una orden ya aprobada por el usuario en PayPal
     * y, si el pago fue exitoso (COMPLETED), registra el pedido en ms-pedidos.
     */
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

            // 2. Pago exitoso → registrar pedido en ms-pedidos
            PedidoDTO pedidoDTO = new PedidoDTO();
            pedidoDTO.setSkuProducto(request.getSkuProducto());
            pedidoDTO.setCantidad(request.getCantidad());
            pedidoDTO.setPaypalOrderId(request.getOrderId());

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

    /**
     * WEBHOOK — PayPal notifica eventos (PAYMENT.CAPTURE.COMPLETED, etc.)
     * Requiere URL pública HTTPS → usar ngrok en desarrollo.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> recibirWebhook(
            @RequestHeader(value = "Paypal-Transmission-Id", required = false) String transmissionId,
            @RequestBody String payload) {

        log.info("Webhook PayPal recibido. TransmissionId: {}", transmissionId);
        log.debug("Payload: {}", payload);
        // Aquí puedes parsear el evento y actualizar el estado del pedido si lo necesitas
        return ResponseEntity.ok().build();
    }
}
