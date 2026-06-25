package com.smartlogix.pedidos.controller;

import com.smartlogix.pedidos.model.dto.PedidoDTO;
import com.smartlogix.pedidos.model.dto.PedidoItemDTO;
import com.smartlogix.pedidos.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock
    private PedidoService pedidoService;

    @InjectMocks
    private PedidoController pedidoController;

    private PedidoDTO pedidoMock;

    @BeforeEach
    void setUp() {
        PedidoItemDTO item = new PedidoItemDTO();
        item.setSkuProducto("SKU-001");
        item.setCantidad(2);

        pedidoMock = new PedidoDTO();
        pedidoMock.setEstado("COMPLETADO");
        pedidoMock.setPrecioTotal(new BigDecimal("1000.00"));
        pedidoMock.setItems(List.of(item));
        pedidoMock.setPaypalOrderId("PAYPAL-XYZ");
    }

    @Test
    void crearPedidoDebeRetornar200() {
        when(pedidoService.crearPedido(pedidoMock)).thenReturn(pedidoMock);

        ResponseEntity<PedidoDTO> response = pedidoController.crearPedido(pedidoMock);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("COMPLETADO", response.getBody().getEstado());
        verify(pedidoService, times(1)).crearPedido(pedidoMock);
    }

    @Test
    void listarPedidosDebeRetornar200ConLista() {
        when(pedidoService.listarPedidos()).thenReturn(List.of(pedidoMock));

        ResponseEntity<List<PedidoDTO>> response = pedidoController.listarPedidos();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("COMPLETADO", response.getBody().get(0).getEstado());
    }

    @Test
    void listarPedidosDebeRetornar200ConListaVacia() {
        when(pedidoService.listarPedidos()).thenReturn(List.of());

        ResponseEntity<List<PedidoDTO>> response = pedidoController.listarPedidos();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }
}