package com.smartlogix.inventario.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener Event-Driven: consume el evento "pedido.creado" desde RabbitMQ.
 *
 * El descuento de stock ya ocurrió de forma síncrona en ms-pedidos (via Feign).
 * Este listener sirve para auditoría, métricas, alertas de stock bajo, etc.,
 * sin bloquear la respuesta al usuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PedidoCreadoListener {

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void onPedidoCreado(PedidoCreadoEvent event) {
        log.info("[EVENT-DRIVEN] Evento recibido: pedido.creado | " +
                        "pedidoId={}, sku={}, cantidad={}, paypalOrderId={}, estado={}",
                event.getPedidoId(),
                event.getSkuProducto(),
                event.getCantidad(),
                event.getPaypalOrderId(),
                event.getEstado());

        // Aquí puedes agregar: alertas de stock bajo, notificaciones, analytics, etc.
        // Por ejemplo:
        if ("COMPLETADO".equals(event.getEstado())) {
            log.info("[AUDIT] Venta confirmada por PayPal — SKU: {}, Qty: {}",
                    event.getSkuProducto(), event.getCantidad());
        }
    }
}
