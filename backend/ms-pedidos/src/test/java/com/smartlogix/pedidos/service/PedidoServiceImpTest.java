package com.smartlogix.pedidos.service;

import com.smartlogix.pedidos.client.InventarioClient;
import com.smartlogix.pedidos.client.ProductoResponse;
import com.smartlogix.pedidos.event.PedidoCreadoEvent;
import com.smartlogix.pedidos.event.PedidoEventPublisher;
import com.smartlogix.pedidos.factory.PedidoFactory;
import com.smartlogix.pedidos.model.dto.PedidoDTO;
import com.smartlogix.pedidos.model.dto.PedidoItemDTO;
import com.smartlogix.pedidos.model.entity.Pedido;
import com.smartlogix.pedidos.model.entity.PedidoItem;
import com.smartlogix.pedidos.repository.PedidoRepository;
import com.smartlogix.pedidos.service.impl.PedidoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private PedidoFactory pedidoFactory;
    @Mock private InventarioClient inventarioClient;
    @Mock private PedidoEventPublisher eventPublisher;

    @InjectMocks
    private PedidoServiceImpl pedidoService;

    private PedidoItemDTO itemDTO;
    private PedidoDTO pedidoDTO;
    private ProductoResponse productoResponse;
    private Pedido pedidoEntity;

    @BeforeEach
    void setUp() {
        itemDTO = new PedidoItemDTO();
        itemDTO.setSkuProducto("SKU-001");
        itemDTO.setCantidad(2);

        pedidoDTO = new PedidoDTO();
        pedidoDTO.setItems(List.of(itemDTO));
        pedidoDTO.setPaypalOrderId("PAYPAL-XYZ");

        productoResponse = new ProductoResponse();
        productoResponse.setSku("SKU-001");
        productoResponse.setPrecio(new BigDecimal("500.00"));
        productoResponse.setStockActual(10);

        // Pedido entity que simula lo que devuelve la factory
        pedidoEntity = new Pedido();
        pedidoEntity.setId(1L);
        pedidoEntity.setEstado("COMPLETADO");
    }

    // ── CREAR PEDIDO — flujo feliz ─────────────────────────────────────────────

    @Test
    void debeCrearPedidoCorrectamenteYPublicarEvento() {
        // Arrange
        PedidoItem itemEntity = new PedidoItem();
        itemEntity.setSkuProducto("SKU-001");
        itemEntity.setCantidad(2);

        when(pedidoFactory.toEntity(pedidoDTO)).thenReturn(pedidoEntity);
        when(pedidoFactory.toItemEntity(itemDTO)).thenReturn(itemEntity);
        when(inventarioClient.obtenerProducto("SKU-001")).thenReturn(productoResponse);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoEntity);

        PedidoDTO responseDTO = new PedidoDTO();
        responseDTO.setEstado("COMPLETADO");
        responseDTO.setPrecioTotal(new BigDecimal("1000.00"));
        when(pedidoFactory.toDTO(pedidoEntity)).thenReturn(responseDTO);

        // Act
        PedidoDTO resultado = pedidoService.crearPedido(pedidoDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals("COMPLETADO", resultado.getEstado());

        // Verifica que el stock fue descontado
        verify(inventarioClient, times(1)).descontarStock("SKU-001", 2);

        // Verifica que el evento fue publicado a RabbitMQ
        verify(eventPublisher, times(1)).publicarPedidoCreado(any(PedidoCreadoEvent.class));

        // Verifica que el pedido fue guardado
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    void debeCalcularTotalCorrecto() {
        // Arrange: 2 unidades a $500 = $1000
        PedidoItem itemEntity = new PedidoItem();
        itemEntity.setSkuProducto("SKU-001");
        itemEntity.setCantidad(2);

        when(pedidoFactory.toEntity(pedidoDTO)).thenReturn(pedidoEntity);
        when(pedidoFactory.toItemEntity(itemDTO)).thenReturn(itemEntity);
        when(inventarioClient.obtenerProducto("SKU-001")).thenReturn(productoResponse);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            // Capturamos el total calculado
            assertEquals(new BigDecimal("1000.00"), p.getPrecioTotal());
            return p;
        });
        when(pedidoFactory.toDTO(any())).thenReturn(new PedidoDTO());

        // Act
        pedidoService.crearPedido(pedidoDTO);
    }

    // ── VALIDACIONES ──────────────────────────────────────────────────────────

    @Test
    void debeLanzarExcepcionCuandoItemsEsNull() {
        pedidoDTO.setItems(null);

        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.crearPedido(pedidoDTO));

        // El inventario nunca debería ser consultado
        verifyNoInteractions(inventarioClient);
    }

    @Test
    void debeLanzarExcepcionCuandoItemsEsListaVacia() {
        pedidoDTO.setItems(Collections.emptyList());

        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.crearPedido(pedidoDTO));

        verifyNoInteractions(inventarioClient);
    }

    // ── CIRCUIT BREAKER / FALLBACK ────────────────────────────────────────────

    @Test
    void debeFallbackGuardarPedidoComoFallidoCuandoInventarioCae() {
        // Arrange: simulamos que ms-inventario está caído
        RuntimeException inventarioCaido = new RuntimeException("Connection refused");

        PedidoItem itemEntity = new PedidoItem();
        itemEntity.setSkuProducto("SKU-001");
        itemEntity.setCantidad(2);

        when(pedidoFactory.toEntity(pedidoDTO)).thenReturn(pedidoEntity);
        when(pedidoFactory.toItemEntity(itemDTO)).thenReturn(itemEntity);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoEntity);

        PedidoDTO fallbackDTO = new PedidoDTO();
        fallbackDTO.setEstado("FALLIDO");
        fallbackDTO.setMensaje("El servicio de inventario no está disponible. Pedido registrado como FALLIDO.");
        when(pedidoFactory.toDTO(any())).thenReturn(fallbackDTO);

        // Act: llamamos directamente al método fallback
        PedidoDTO resultado = pedidoService.crearPedidoFallback(pedidoDTO, inventarioCaido);

        // Assert
        assertEquals("FALLIDO", resultado.getEstado());
        assertNotNull(resultado.getMensaje());

        // El evento NO debe publicarse en fallback
        verifyNoInteractions(eventPublisher);
    }

    // ── EVENTO RABBITMQ ───────────────────────────────────────────────────────

    @Test
    void debePublicarEventoConDatosCorrectosDelPedido() {
        // Arrange
        PedidoItem itemEntity = new PedidoItem();
        itemEntity.setSkuProducto("SKU-001");
        itemEntity.setCantidad(2);

        pedidoEntity.setId(99L);
        pedidoEntity.setPaypalOrderId("PAYPAL-XYZ");
        pedidoEntity.setEstado("COMPLETADO");

        when(pedidoFactory.toEntity(pedidoDTO)).thenReturn(pedidoEntity);
        when(pedidoFactory.toItemEntity(itemDTO)).thenReturn(itemEntity);
        when(inventarioClient.obtenerProducto("SKU-001")).thenReturn(productoResponse);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoEntity);
        when(pedidoFactory.toDTO(any())).thenReturn(new PedidoDTO());

        // Act
        pedidoService.crearPedido(pedidoDTO);

        // Assert: capturar el evento publicado y verificar sus datos
        ArgumentCaptor<PedidoCreadoEvent> captor = ArgumentCaptor.forClass(PedidoCreadoEvent.class);
        verify(eventPublisher).publicarPedidoCreado(captor.capture());

        PedidoCreadoEvent eventoPublicado = captor.getValue();
        assertEquals(99L, eventoPublicado.getPedidoId());
        assertEquals("SKU-001", eventoPublicado.getSkuProducto());
        assertEquals(2, eventoPublicado.getCantidad());
        assertEquals("COMPLETADO", eventoPublicado.getEstado());
    }
}