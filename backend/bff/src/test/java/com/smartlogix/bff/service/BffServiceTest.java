package com.smartlogix.bff.service;

import com.smartlogix.bff.client.InventarioClient;
import com.smartlogix.bff.client.PedidosClient;
import com.smartlogix.bff.model.PedidoDTO;
import com.smartlogix.bff.model.ProductoDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BffServiceTest {

    @Mock
    private InventarioClient inventarioClient;

    @Mock
    private PedidosClient pedidosClient;

    @InjectMocks
    private BffService bffService;

    @Test
    void debeObtenerCatalogo() {
        // Arrange
        ProductoDTO mockProduct = new ProductoDTO();
        mockProduct.setSku("SKU-1");
        mockProduct.setNombre("Test Product");
        when(inventarioClient.listarTodos()).thenReturn(Collections.singletonList(mockProduct));

        // Act
        List<ProductoDTO> catalogo = bffService.obtenerCatalogo();

        // Assert
        assertNotNull(catalogo);
        assertEquals(1, catalogo.size());
        assertEquals("SKU-1", catalogo.get(0).getSku());
        verify(inventarioClient, times(1)).listarTodos();
    }

    @Test
    void debeRealizarCompra() {
        // Arrange
        PedidoDTO request = new PedidoDTO();
        request.setSkuProducto("SKU-1");
        request.setCantidad(2);

        PedidoDTO response = new PedidoDTO();
        response.setSkuProducto("SKU-1");
        response.setCantidad(2);
        response.setEstado("COMPLETADO");
        response.setPrecioTotal(new BigDecimal("2000"));

        when(pedidosClient.crearPedido(request)).thenReturn(response);

        // Act
        PedidoDTO resultado = bffService.realizarCompra(request);

        // Assert
        assertNotNull(resultado);
        assertEquals("COMPLETADO", resultado.getEstado());
        verify(pedidosClient, times(1)).crearPedido(request);
    }
}
