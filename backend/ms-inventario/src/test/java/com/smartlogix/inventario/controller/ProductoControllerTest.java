package com.smartlogix.inventario.controller;

import com.smartlogix.inventario.model.dto.ProductoDTO;
import com.smartlogix.inventario.service.ProductoService;
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
class ProductoControllerTest {

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private ProductoController productoController;

    private ProductoDTO productoMock;

    @BeforeEach
    void setUp() {
        productoMock = new ProductoDTO();
        productoMock.setSku("SKU-001");
        productoMock.setNombre("Laptop Test");
        productoMock.setPrecio(new BigDecimal("999.99"));
        productoMock.setStockActual(10);
        productoMock.setDisponible(true);
    }

    @Test
    void listarTodosDebeRetornar200ConLista() {
        when(productoService.obtenerTodos()).thenReturn(List.of(productoMock));

        ResponseEntity<List<ProductoDTO>> response = productoController.listarTodos();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("SKU-001", response.getBody().get(0).getSku());
    }

    @Test
    void obtenerPorSkuDebeRetornar200() {
        when(productoService.obtenerPorSku("SKU-001")).thenReturn(productoMock);

        ResponseEntity<ProductoDTO> response = productoController.obtenerPorSku("SKU-001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SKU-001", response.getBody().getSku());
    }

    @Test
    void crearProductoDebeRetornar201() {
        when(productoService.crearProducto(productoMock)).thenReturn(productoMock);

        ResponseEntity<ProductoDTO> response = productoController.crearProducto(productoMock);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("SKU-001", response.getBody().getSku());
    }

    @Test
    void actualizarProductoDebeRetornar200() {
        when(productoService.actualizarProducto("SKU-001", productoMock)).thenReturn(productoMock);

        ResponseEntity<ProductoDTO> response = productoController.actualizarProducto("SKU-001", productoMock);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void eliminarProductoDebeRetornar204() {
        doNothing().when(productoService).eliminarProducto("SKU-001");

        ResponseEntity<Void> response = productoController.eliminarProducto("SKU-001");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(productoService, times(1)).eliminarProducto("SKU-001");
    }

    @Test
    void descontarStockDebeRetornar204() {
        doNothing().when(productoService).actualizarStock("SKU-001", 3);

        ResponseEntity<Void> response = productoController.descontarStock("SKU-001", 3);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(productoService, times(1)).actualizarStock("SKU-001", 3);
    }
}