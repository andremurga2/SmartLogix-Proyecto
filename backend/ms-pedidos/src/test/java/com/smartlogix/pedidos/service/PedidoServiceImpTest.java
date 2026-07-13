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

        pedidoEntity = new Pedido();
        pedidoEntity.setId(1L);
        pedidoEntity.setEstado("COMPLETADO");
    }

    // ── CREAR PEDIDO — flujo feliz ─────────────────────────────────────────────

    @Test
    void debeCrearPedidoCorrectamenteYPublicarEvento() {
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

        PedidoDTO resultado = pedidoService.crearPedido(pedidoDTO);

        assertNotNull(resultado);
        assertEquals("COMPLETADO", resultado.getEstado());
        verify(inventarioClient, times(1)).descontarStock("SKU-001", 2);
        verify(eventPublisher, times(1)).publicarPedidoCreado(any(PedidoCreadoEvent.class));
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    void debeCalcularTotalCorrecto() {
        PedidoItem itemEntity = new PedidoItem();
        itemEntity.setSkuProducto("SKU-001");
        itemEntity.setCantidad(2);

        when(pedidoFactory.toEntity(pedidoDTO)).thenReturn(pedidoEntity);
        when(pedidoFactory.toItemEntity(itemDTO)).thenReturn(itemEntity);
        when(inventarioClient.obtenerProducto("SKU-001")).thenReturn(productoResponse);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            assertEquals(new BigDecimal("1000.00"), p.getPrecioTotal());
            return p;
        });
        when(pedidoFactory.toDTO(any())).thenReturn(new PedidoDTO());

        pedidoService.crearPedido(pedidoDTO);
    }

    @Test
    void debeCrearPedidoConMultiplesItems() {
        PedidoItemDTO item2 = new PedidoItemDTO();
        item2.setSkuProducto("SKU-002");
        item2.setCantidad(1);

        pedidoDTO.setItems(List.of(itemDTO, item2));

        PedidoItem itemEntity1 = new PedidoItem();
        itemEntity1.setSkuProducto("SKU-001");
        itemEntity1.setCantidad(2);

        PedidoItem itemEntity2 = new PedidoItem();
        itemEntity2.setSkuProducto("SKU-002");
        itemEntity2.setCantidad(1);

        ProductoResponse producto2 = new ProductoResponse();
        producto2.setSku("SKU-002");
        producto2.setPrecio(new BigDecimal("200.00"));
        producto2.setStockActual(5);

        when(pedidoFactory.toEntity(pedidoDTO)).thenReturn(pedidoEntity);
        when(pedidoFactory.toItemEntity(itemDTO)).thenReturn(itemEntity1);
        when(pedidoFactory.toItemEntity(item2)).thenReturn(itemEntity2);
        when(inventarioClient.obtenerProducto("SKU-001")).thenReturn(productoResponse);
        when(inventarioClient.obtenerProducto("SKU-002")).thenReturn(producto2);
        when(pedidoRepository.save(any())).thenReturn(pedidoEntity);
        when(pedidoFactory.toDTO(any())).thenReturn(new PedidoDTO());

        pedidoService.crearPedido(pedidoDTO);

        // Total esperado: 2*500 + 1*200 = $1200
        verify(inventarioClient, times(1)).descontarStock("SKU-001", 2);
        verify(inventarioClient, times(1)).descontarStock("SKU-002", 1);
    }

    @Test
    void debeLanzarExcepcionCuandoStockEsInsuficiente() {
        productoResponse.setStockActual(1); // sólo hay 1, se piden 2

        when(pedidoFactory.toEntity(pedidoDTO)).thenReturn(pedidoEntity);
        when(inventarioClient.obtenerProducto("SKU-001")).thenReturn(productoResponse);

        assertThrows(RuntimeException.class, () -> pedidoService.crearPedido(pedidoDTO));

        verify(inventarioClient, never()).descontarStock(anyString(), anyInt());
        verify(pedidoRepository, never()).save(any());
    }

    // ── COMPENSACIÓN (SAGA) — falla el guardado del pedido ─────────────────────

    @Test
    void debeRevertirStockConSkuYCantidadCorrectosCuandoFallaElGuardadoDelPedido() {
        // Arrange: el stock se descuenta sin problema, pero persistir el pedido falla
        // (p. ej. constraint de BD, conexión caída con PostgreSQL).
        PedidoItem itemEntity = new PedidoItem();
        itemEntity.setSkuProducto("SKU-001");
        itemEntity.setCantidad(2);

        RuntimeException errorDeGuardado = new RuntimeException("Fallo de conexión con la base de datos");

        when(pedidoFactory.toEntity(pedidoDTO)).thenReturn(pedidoEntity);
        when(pedidoFactory.toItemEntity(itemDTO)).thenReturn(itemEntity);
        when(inventarioClient.obtenerProducto("SKU-001")).thenReturn(productoResponse);
        when(pedidoRepository.save(any(Pedido.class))).thenThrow(errorDeGuardado);

        // Act & Assert
        RuntimeException excepcionLanzada = assertThrows(RuntimeException.class,
                () -> pedidoService.crearPedido(pedidoDTO));
        assertSame(errorDeGuardado, excepcionLanzada);

        // El stock ya se había descontado antes de intentar guardar
        verify(inventarioClient, times(1)).descontarStock("SKU-001", 2);

        // La compensación debe revertir exactamente el SKU y la cantidad descontados
        ArgumentCaptor<String> skuCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> cantidadCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(inventarioClient, times(1)).revertirStock(skuCaptor.capture(), cantidadCaptor.capture());
        assertEquals("SKU-001", skuCaptor.getValue());
        assertEquals(2, cantidadCaptor.getValue());

        // No se debe publicar ningún evento de pedido creado si el guardado falló
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void debeRevertirStockDeTodosLosItemsConfirmadosCuandoFallaElGuardadoConMultiplesItems() {
        // Arrange: dos ítems logran descontar stock, pero el guardado final del pedido falla.
        PedidoItemDTO item2 = new PedidoItemDTO();
        item2.setSkuProducto("SKU-002");
        item2.setCantidad(3);
        pedidoDTO.setItems(List.of(itemDTO, item2));

        PedidoItem itemEntity1 = new PedidoItem();
        itemEntity1.setSkuProducto("SKU-001");
        itemEntity1.setCantidad(2);

        PedidoItem itemEntity2 = new PedidoItem();
        itemEntity2.setSkuProducto("SKU-002");
        itemEntity2.setCantidad(3);

        ProductoResponse producto2 = new ProductoResponse();
        producto2.setSku("SKU-002");
        producto2.setPrecio(new BigDecimal("100.00"));
        producto2.setStockActual(10);

        when(pedidoFactory.toEntity(pedidoDTO)).thenReturn(pedidoEntity);
        when(pedidoFactory.toItemEntity(itemDTO)).thenReturn(itemEntity1);
        when(pedidoFactory.toItemEntity(item2)).thenReturn(itemEntity2);
        when(inventarioClient.obtenerProducto("SKU-001")).thenReturn(productoResponse);
        when(inventarioClient.obtenerProducto("SKU-002")).thenReturn(producto2);
        when(pedidoRepository.save(any(Pedido.class)))
                .thenThrow(new RuntimeException("Violación de constraint en BD"));

        assertThrows(RuntimeException.class, () -> pedidoService.crearPedido(pedidoDTO));

        verify(inventarioClient, times(1)).revertirStock("SKU-001", 2);
        verify(inventarioClient, times(1)).revertirStock("SKU-002", 3);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void noDebeInterrumpirLaCompensacionSiLaReversionDeUnItemFalla() {
        // Arrange: revertirStock también falla (ms-inventario caído). El servicio debe
        // igual relanzar la excepción original de guardado, sin quedar la reversión
        // silenciosamente perdida (queda registrada en logs para reconciliación).
        PedidoItem itemEntity = new PedidoItem();
        itemEntity.setSkuProducto("SKU-001");
        itemEntity.setCantidad(2);

        RuntimeException errorDeGuardado = new RuntimeException("Timeout guardando pedido");

        when(pedidoFactory.toEntity(pedidoDTO)).thenReturn(pedidoEntity);
        when(pedidoFactory.toItemEntity(itemDTO)).thenReturn(itemEntity);
        when(inventarioClient.obtenerProducto("SKU-001")).thenReturn(productoResponse);
        when(pedidoRepository.save(any(Pedido.class))).thenThrow(errorDeGuardado);
        doThrow(new RuntimeException("ms-inventario no disponible"))
                .when(inventarioClient).revertirStock("SKU-001", 2);

        RuntimeException excepcionLanzada = assertThrows(RuntimeException.class,
                () -> pedidoService.crearPedido(pedidoDTO));

        assertSame(errorDeGuardado, excepcionLanzada);
        verify(inventarioClient, times(1)).revertirStock("SKU-001", 2);
    }

    // ── VALIDACIONES ──────────────────────────────────────────────────────────

    @Test
    void debeLanzarExcepcionCuandoItemsEsNull() {
        pedidoDTO.setItems(null);

        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.crearPedido(pedidoDTO));

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

        PedidoDTO resultado = pedidoService.crearPedidoFallback(pedidoDTO, inventarioCaido);

        assertEquals("FALLIDO", resultado.getEstado());
        assertNotNull(resultado.getMensaje());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void debeFallbackConItemsNulos() {
        pedidoDTO.setItems(null);
        RuntimeException t = new RuntimeException("error");

        when(pedidoFactory.toEntity(pedidoDTO)).thenReturn(pedidoEntity);
        when(pedidoRepository.save(any())).thenReturn(pedidoEntity);
        when(pedidoFactory.toDTO(any())).thenReturn(new PedidoDTO());

        assertDoesNotThrow(() -> pedidoService.crearPedidoFallback(pedidoDTO, t));
    }

    // ── EVENTO RABBITMQ ───────────────────────────────────────────────────────

    @Test
    void debePublicarEventoConDatosCorrectosDelPedido() {
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

        pedidoService.crearPedido(pedidoDTO);

        ArgumentCaptor<PedidoCreadoEvent> captor = ArgumentCaptor.forClass(PedidoCreadoEvent.class);
        verify(eventPublisher).publicarPedidoCreado(captor.capture());

        PedidoCreadoEvent eventoPublicado = captor.getValue();
        assertEquals(99L, eventoPublicado.getPedidoId());
        assertEquals("SKU-001", eventoPublicado.getSkuProducto());
        assertEquals(2, eventoPublicado.getCantidad());
        assertEquals("COMPLETADO", eventoPublicado.getEstado());
    }

    // ── LISTAR PEDIDOS ────────────────────────────────────────────────────────

    @Test
    void debeListarPedidosCorrectamente() {
        when(pedidoRepository.findAll()).thenReturn(List.of(pedidoEntity));
        when(pedidoFactory.toDTO(pedidoEntity)).thenReturn(pedidoDTO);

        List<PedidoDTO> lista = pedidoService.listarPedidos();

        assertEquals(1, lista.size());
        verify(pedidoRepository, times(1)).findAll();
    }
}