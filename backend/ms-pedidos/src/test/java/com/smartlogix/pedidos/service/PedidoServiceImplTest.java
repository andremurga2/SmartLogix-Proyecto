package com.smartlogix.pedidos.service;

import com.smartlogix.pedidos.client.InventarioClient;
import com.smartlogix.pedidos.client.ProductoResponse;
import com.smartlogix.pedidos.factory.PedidoFactory;
import com.smartlogix.pedidos.model.dto.PedidoDTO;
import com.smartlogix.pedidos.model.entity.Pedido;
import com.smartlogix.pedidos.repository.PedidoRepository;
import com.smartlogix.pedidos.service.impl.PedidoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private PedidoFactory pedidoFactory;

    @Mock
    private InventarioClient inventarioClient;

    @InjectMocks
    private PedidoServiceImpl pedidoService;

    private PedidoDTO pedidoDTORequest;
    private ProductoResponse productoResponseMock;
    private Pedido pedidoEntityMock;
    private PedidoDTO pedidoDTOResponse;

    @BeforeEach
    void setUp() {
        pedidoDTORequest = PedidoDTO.builder()
                .skuProducto("SKU-123")
                .cantidad(2)
                .build();

        productoResponseMock = new ProductoResponse();
        productoResponseMock.setSku("SKU-123");
        productoResponseMock.setPrecio(new BigDecimal("1000"));
        productoResponseMock.setStockActual(10);
        productoResponseMock.setDisponible(true);

        pedidoEntityMock = Pedido.builder()
                .id(1L)
                .skuProducto("SKU-123")
                .cantidad(2)
                .precioTotal(new BigDecimal("2000"))
                .estado("COMPLETADO")
                .build();

        pedidoDTOResponse = PedidoDTO.builder()
                .id(1L)
                .skuProducto("SKU-123")
                .cantidad(2)
                .precioTotal(new BigDecimal("2000"))
                .estado("COMPLETADO")
                .build();
    }

    @Test
    void debeCrearPedidoExitosamente() {
        // Arrange
        when(inventarioClient.obtenerProducto("SKU-123")).thenReturn(productoResponseMock);
        doNothing().when(inventarioClient).descontarStock("SKU-123", 2);
        
        when(pedidoFactory.toEntity(pedidoDTORequest)).thenReturn(pedidoEntityMock);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoEntityMock);
        when(pedidoFactory.toDTO(pedidoEntityMock)).thenReturn(pedidoDTOResponse);

        // Act
        PedidoDTO resultado = pedidoService.crearPedido(pedidoDTORequest);

        // Assert
        assertNotNull(resultado);
        assertEquals("COMPLETADO", resultado.getEstado());
        assertEquals("Pedido procesado exitosamente.", resultado.getMensaje());
        verify(inventarioClient, times(1)).descontarStock("SKU-123", 2);
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    void debeLanzarExcepcionPorStockInsuficiente() {
        // Arrange
        productoResponseMock.setStockActual(1); // Menos de la cantidad pedida (2)
        when(inventarioClient.obtenerProducto("SKU-123")).thenReturn(productoResponseMock);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            pedidoService.crearPedido(pedidoDTORequest);
        });

        assertEquals("Stock insuficiente en el inventario.", exception.getMessage());
        verify(inventarioClient, never()).descontarStock(anyString(), anyInt());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void debeEjecutarFallbackCuandoInventarioFalla() {
        // Arrange
        Throwable feignException = new RuntimeException("Connection refused");
        
        Pedido entityFallido = Pedido.builder()
                .skuProducto("SKU-123")
                .cantidad(2)
                .estado("FALLIDO")
                .precioTotal(BigDecimal.ZERO)
                .build();
                
        PedidoDTO responseFallido = PedidoDTO.builder()
                .skuProducto("SKU-123")
                .cantidad(2)
                .estado("FALLIDO")
                .build();

        when(pedidoFactory.toEntity(pedidoDTORequest)).thenReturn(entityFallido);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(entityFallido);
        when(pedidoFactory.toDTO(entityFallido)).thenReturn(responseFallido);

        // Act
        PedidoDTO resultado = pedidoService.crearPedidoFallback(pedidoDTORequest, feignException);

        // Assert
        assertNotNull(resultado);
        assertEquals("FALLIDO", resultado.getEstado());
        assertTrue(resultado.getMensaje().contains("El servicio de inventario no está disponible"));
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }
}
