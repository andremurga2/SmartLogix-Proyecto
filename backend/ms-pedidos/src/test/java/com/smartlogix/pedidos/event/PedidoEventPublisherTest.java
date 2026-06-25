package com.smartlogix.pedidos.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PedidoEventPublisher eventPublisher;

    @Test
    void debePublicarEventoConDatosCorrectos() {
        PedidoCreadoEvent event = new PedidoCreadoEvent(
                1L, "SKU-001", 2, "PAYPAL-XYZ", "COMPLETADO");

        eventPublisher.publicarPedidoCreado(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                event
        );
    }

    @Test
    void debePublicarElEventoExactoQueRecibe() {
        PedidoCreadoEvent event = new PedidoCreadoEvent(
                99L, "SKU-999", 5, "PAYPAL-ABC", "FALLIDO");

        eventPublisher.publicarPedidoCreado(event);

        ArgumentCaptor<PedidoCreadoEvent> captor = ArgumentCaptor.forClass(PedidoCreadoEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.ROUTING_KEY),
                captor.capture()
        );

        PedidoCreadoEvent capturado = captor.getValue();
        assertEquals(99L, capturado.getPedidoId());
        assertEquals("SKU-999", capturado.getSkuProducto());
        assertEquals(5, capturado.getCantidad());
        assertEquals("FALLIDO", capturado.getEstado());
    }
}