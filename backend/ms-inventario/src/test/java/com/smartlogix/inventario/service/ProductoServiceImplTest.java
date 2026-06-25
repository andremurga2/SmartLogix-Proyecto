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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private ProductoFactory productoFactory;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private Producto productoMock;
    private ProductoDTO productoDTOMock;

    @BeforeEach
    void setUp() {
        productoMock = Producto.builder()
                .id(1L).sku("SKU-123").nombre("Laptop")
                .precio(new BigDecimal("1000")).stockActual(10).build();

        productoDTOMock = ProductoDTO.builder()
                .id(1L).sku("SKU-123").nombre("Laptop")
                .precio(new BigDecimal("1000")).stockActual(10).disponible(true).build();
    }

    // ── OBTENER TODOS ─────────────────────────────────────────────────────────

    @Test
    void debeRetornarListaDeProductos() {
        when(productoRepository.findAll()).thenReturn(List.of(productoMock));
        when(productoFactory.toDTO(productoMock)).thenReturn(productoDTOMock);

        List<ProductoDTO> resultado = productoService.obtenerTodos();

        assertEquals(1, resultado.size());
        assertEquals("SKU-123", resultado.get(0).getSku());
        verify(productoRepository, times(1)).findAll();
    }

    @Test
    void debeRetornarListaVaciaSiNoHayProductos() {
        when(productoRepository.findAll()).thenReturn(List.of());

        List<ProductoDTO> resultado = productoService.obtenerTodos();

        assertTrue(resultado.isEmpty());
    }

    // ── OBTENER POR SKU ───────────────────────────────────────────────────────

    @Test
    void debeObtenerProductoPorSkuExitosamente() {
        when(productoRepository.findBySku("SKU-123")).thenReturn(Optional.of(productoMock));
        when(productoFactory.toDTO(productoMock)).thenReturn(productoDTOMock);

        ProductoDTO resultado = productoService.obtenerPorSku("SKU-123");

        assertNotNull(resultado);
        assertEquals("SKU-123", resultado.getSku());
        verify(productoRepository, times(1)).findBySku("SKU-123");
    }

    @Test
    void debeLanzarExcepcionCuandoSkuNoExiste() {
        when(productoRepository.findBySku(anyString())).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class,
                () -> productoService.obtenerPorSku("SKU-999"));

        assertEquals("Producto no encontrado con SKU: SKU-999", ex.getMessage());
    }

    // ── CREAR PRODUCTO ────────────────────────────────────────────────────────

    @Test
    void debeCrearProductoCorrectamente() {
        when(productoFactory.toEntity(productoDTOMock)).thenReturn(productoMock);
        when(productoRepository.save(productoMock)).thenReturn(productoMock);
        when(productoFactory.toDTO(productoMock)).thenReturn(productoDTOMock);

        ProductoDTO resultado = productoService.crearProducto(productoDTOMock);

        assertNotNull(resultado);
        assertEquals("SKU-123", resultado.getSku());
        verify(productoRepository, times(1)).save(productoMock);
    }

    // ── ACTUALIZAR PRODUCTO ───────────────────────────────────────────────────

    @Test
    void debeActualizarProductoExistente() {
        ProductoDTO dtoActualizado = ProductoDTO.builder()
                .nombre("Laptop Pro").precio(new BigDecimal("1200"))
                .stockActual(5).build();

        when(productoRepository.findBySku("SKU-123")).thenReturn(Optional.of(productoMock));
        when(productoRepository.save(productoMock)).thenReturn(productoMock);
        when(productoFactory.toDTO(productoMock)).thenReturn(productoDTOMock);

        ProductoDTO resultado = productoService.actualizarProducto("SKU-123", dtoActualizado);

        assertNotNull(resultado);
        verify(productoRepository, times(1)).save(productoMock);
        assertEquals("Laptop Pro", productoMock.getNombre());
        assertEquals(new BigDecimal("1200"), productoMock.getPrecio());
    }

    @Test
    void debeLanzarExcepcionAlActualizarSkuInexistente() {
        when(productoRepository.findBySku("SKU-XXX")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> productoService.actualizarProducto("SKU-XXX", productoDTOMock));
    }

    // ── ELIMINAR PRODUCTO ─────────────────────────────────────────────────────

    @Test
    void debeEliminarProductoExistente() {
        when(productoRepository.findBySku("SKU-123")).thenReturn(Optional.of(productoMock));

        productoService.eliminarProducto("SKU-123");

        verify(productoRepository, times(1)).delete(productoMock);
    }

    @Test
    void debeLanzarExcepcionAlEliminarSkuInexistente() {
        when(productoRepository.findBySku("SKU-ZZZ")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> productoService.eliminarProducto("SKU-ZZZ"));
    }

    // ── ACTUALIZAR STOCK ──────────────────────────────────────────────────────

    @Test
    void debeActualizarStockCorrectamente() {
        when(productoRepository.findBySku("SKU-123")).thenReturn(Optional.of(productoMock));

        productoService.actualizarStock("SKU-123", 2);

        assertEquals(8, productoMock.getStockActual());
        verify(productoRepository, times(1)).save(productoMock);
    }

    @Test
    void debeLanzarExcepcionPorStockInsuficiente() {
        when(productoRepository.findBySku("SKU-123")).thenReturn(Optional.of(productoMock));

        Exception ex = assertThrows(RuntimeException.class,
                () -> productoService.actualizarStock("SKU-123", 15));

        assertEquals("Stock insuficiente para el SKU: SKU-123", ex.getMessage());
        verify(productoRepository, never()).save(any());
    }

    @Test
    void debeLanzarExcepcionAlActualizarStockDeSkuInexistente() {
        when(productoRepository.findBySku("SKU-999")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> productoService.actualizarStock("SKU-999", 1));
    }
}