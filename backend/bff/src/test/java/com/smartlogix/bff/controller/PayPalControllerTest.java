package com.smartlogix.bff.controller;

import com.paypal.orders.LinkDescription;
import com.paypal.orders.Order;
import com.smartlogix.bff.model.*;
import com.smartlogix.bff.service.BffService;
import com.smartlogix.bff.service.PayPalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayPalControllerTest {

    @Mock private PayPalService payPalService;
    @Mock private BffService bffService;

    @InjectMocks
    private PayPalController payPalController;

    private Order orderMock;
    private CrearOrdenRequest crearOrdenRequest;
    private CapturarOrdenRequest capturarOrdenRequest;

    @BeforeEach
    void setUp() {
        orderMock = mock(Order.class);

        ItemCarritoDTO item = new ItemCarritoDTO();
        item.setSkuProducto("SKU-001");
        item.setCantidad(2);

        crearOrdenRequest = new CrearOrdenRequest();
        crearOrdenRequest.setItems(List.of(item));
        crearOrdenRequest.setMoneda("USD");

        capturarOrdenRequest = new CapturarOrdenRequest();
        capturarOrdenRequest.setOrderId("PAYPAL-XYZ");
        capturarOrdenRequest.setItems(List.of(item));
    }

    // ── CREAR ORDEN ───────────────────────────────────────────────────────────

    @Test
    void crearOrdenDebeRetornar200ConApproveUrl() throws IOException {
        when(orderMock.id()).thenReturn("ORDER-123");
        when(orderMock.status()).thenReturn("CREATED");
        when(payPalService.crearOrden(any(), anyString())).thenReturn(orderMock);
        when(payPalService.obtenerApproveUrl(orderMock)).thenReturn("https://paypal.com/approve");

        ResponseEntity<CrearOrdenResponse> response = payPalController.crearOrden(crearOrdenRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ORDER-123", response.getBody().getOrderId());
        assertEquals("https://paypal.com/approve", response.getBody().getApproveUrl());
    }

    @Test
    void crearOrdenDebeRetornar200CuandoMonedaEsNull() throws IOException {
        crearOrdenRequest.setMoneda(null); // debe usar "USD" por defecto

        when(orderMock.id()).thenReturn("ORDER-456");
        when(orderMock.status()).thenReturn("CREATED");
        when(payPalService.crearOrden(any(), eq("USD"))).thenReturn(orderMock);
        when(payPalService.obtenerApproveUrl(orderMock)).thenReturn("https://paypal.com/approve");

        ResponseEntity<CrearOrdenResponse> response = payPalController.crearOrden(crearOrdenRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void crearOrdenDebeRetornar400SiCarritoEsInvalido() throws IOException {
        when(payPalService.crearOrden(any(), anyString()))
                .thenThrow(new IllegalArgumentException("El carrito está vacío."));

        ResponseEntity<CrearOrdenResponse> response = payPalController.crearOrden(crearOrdenRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getStatus().contains("ERROR"));
    }

    @Test
    void crearOrdenDebeRetornar500SiPayPalFalla() throws IOException {
        when(payPalService.crearOrden(any(), anyString()))
                .thenThrow(new IOException("PayPal no disponible"));

        ResponseEntity<CrearOrdenResponse> response = payPalController.crearOrden(crearOrdenRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().getStatus().contains("ERROR"));
    }

    // ── CAPTURAR ORDEN ────────────────────────────────────────────────────────

    @Test
    void capturarOrdenDebeRetornar200CuandoPagoEsCompletado() throws IOException {
        when(orderMock.status()).thenReturn("COMPLETED");
        when(payPalService.capturarOrden("PAYPAL-XYZ")).thenReturn(orderMock);

        PedidoDTO pedidoCreado = new PedidoDTO();
        pedidoCreado.setEstado("COMPLETADO");
        when(bffService.realizarCompra(any(PedidoDTO.class))).thenReturn(pedidoCreado);

        ResponseEntity<PagoResponse> response = payPalController.capturarOrden(capturarOrdenRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isExitoso());
        assertEquals("COMPLETADO", response.getBody().getPedido().getEstado());
        verify(bffService, times(1)).realizarCompra(any(PedidoDTO.class));
    }

    @Test
    void capturarOrdenDebeRetornar400SiPagoNoFueCompletado() throws IOException {
        when(orderMock.status()).thenReturn("PENDING");
        when(payPalService.capturarOrden("PAYPAL-XYZ")).thenReturn(orderMock);

        ResponseEntity<PagoResponse> response = payPalController.capturarOrden(capturarOrdenRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isExitoso());
        verifyNoInteractions(bffService);
    }

    @Test
    void capturarOrdenDebeRetornar500SiPayPalFalla() throws IOException {
        when(payPalService.capturarOrden("PAYPAL-XYZ"))
                .thenThrow(new IOException("Timeout PayPal"));

        ResponseEntity<PagoResponse> response = payPalController.capturarOrden(capturarOrdenRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isExitoso());
    }

    // ── WEBHOOK ───────────────────────────────────────────────────────────────

    @Test
    void webhookDebeRetornar200Siempre() {
        ResponseEntity<Void> response = payPalController.recibirWebhook(
                "transmission-id-123", "{\"event_type\":\"PAYMENT.CAPTURE.COMPLETED\"}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void webhookDebeRetornar200SinTransmissionId() {
        ResponseEntity<Void> response = payPalController.recibirWebhook(null, "{}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}