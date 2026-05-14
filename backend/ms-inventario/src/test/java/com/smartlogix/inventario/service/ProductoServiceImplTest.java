package com.smartlogix.inventario.service;

import com.smartlogix.inventario.factory.ProductoFactory;
import com.smartlogix.inventario.model.dto.ProductoDTO;
import com.smartlogix.inventario.model.entity.Producto;
import com.smartlogix.inventario.repository.ProductoRepository;
import com.smartlogix.inventario.service.impl.ProductoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProductoFactory productoFactory;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private Producto productoMock;
    private ProductoDTO productoDTOMock;

    @BeforeEach
    void setUp() {
        productoMock = Producto.builder()
                .id(1L)
                .sku("SKU-123")
                .nombre("Laptop")
                .precio(new BigDecimal("1000"))
                .stockActual(10)
                .build();

        productoDTOMock = ProductoDTO.builder()
                .id(1L)
                .sku("SKU-123")
                .nombre("Laptop")
                .precio(new BigDecimal("1000"))
                .stockActual(10)
                .disponible(true)
                .build();
    }

    @Test
    void debeObtenerProductoPorSkuExitosamente() {
        // Arrange (Preparación)
        when(productoRepository.findBySku("SKU-123")).thenReturn(Optional.of(productoMock));
        when(productoFactory.toDTO(productoMock)).thenReturn(productoDTOMock);

        // Act (Ejecución)
        ProductoDTO resultado = productoService.obtenerPorSku("SKU-123");

        // Assert (Verificación)
        assertNotNull(resultado);
        assertEquals("SKU-123", resultado.getSku());
        verify(productoRepository, times(1)).findBySku("SKU-123");
    }

    @Test
    void debeLanzarExcepcionCuandoSkuNoExiste() {
        // Arrange
        when(productoRepository.findBySku(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            productoService.obtenerPorSku("SKU-999");
        });

        assertEquals("Producto no encontrado con SKU: SKU-999", exception.getMessage());
    }

    @Test
    void debeActualizarStockCorrectamente() {
        // Arrange
        when(productoRepository.findBySku("SKU-123")).thenReturn(Optional.of(productoMock));

        // Act
        productoService.actualizarStock("SKU-123", 2);

        // Assert
        assertEquals(8, productoMock.getStockActual());
        verify(productoRepository, times(1)).save(productoMock);
    }

    @Test
    void debeLanzarExcepcionPorStockInsuficiente() {
        // Arrange
        when(productoRepository.findBySku("SKU-123")).thenReturn(Optional.of(productoMock));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            productoService.actualizarStock("SKU-123", 15); // Intentar comprar 15, stock es 10
        });

        assertEquals("Stock insuficiente para el SKU: SKU-123", exception.getMessage());
        verify(productoRepository, never()).save(any());
    }
}
