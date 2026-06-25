package com.smartlogix.inventario.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class PedidoCreadoListenerTest {

    @InjectMocks
    private PedidoCreadoListener listener;

    @Test
    void debeProcessarEventoCompletadoSinExcepcion() {
        PedidoCreadoEvent event = new PedidoCreadoEvent(
                1L, "SKU-001", 2, "PAYPAL-XYZ", "COMPLETADO");

        assertDoesNotThrow(() -> listener.onPedidoCreado(event));
    }

    @Test
    void debeProcessarEventoFallidoSinExcepcion() {
        PedidoCreadoEvent event = new PedidoCreadoEvent(
                2L, "SKU-002", 1, "PAYPAL-ABC", "FALLIDO");

        assertDoesNotThrow(() -> listener.onPedidoCreado(event));
    }

    @Test
    void debeProcessarEventoConDatosNulos() {
        PedidoCreadoEvent event = new PedidoCreadoEvent(
                null, null, null, null, null);

        assertDoesNotThrow(() -> listener.onPedidoCreado(event));
    }
}