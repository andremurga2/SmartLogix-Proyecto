package com.smartlogix.pedidos.integration;

import com.smartlogix.pedidos.client.InventarioClient;
import com.smartlogix.pedidos.client.ProductoResponse;
import com.smartlogix.pedidos.event.PedidoCreadoEvent;
import com.smartlogix.pedidos.event.RabbitMQConfig;
import com.smartlogix.pedidos.model.dto.PedidoDTO;
import com.smartlogix.pedidos.model.dto.PedidoItemDTO;
import com.smartlogix.pedidos.model.entity.Pedido;
import com.smartlogix.pedidos.repository.PedidoRepository;
import com.smartlogix.pedidos.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test de integración "real": levanta contenedores efímeros de PostgreSQL y
 * RabbitMQ con Testcontainers (las mismas imágenes que usa docker-compose.yml
 * en producción/dev) y ejercita PedidoServiceImpl contra ellos, en vez de
 * depender exclusivamente de mocks de repositorio/broker como en
 * PedidoServiceImpTest.
 *
 * Lo único que se mockea es InventarioClient (el Feign hacia ms-inventario),
 * porque ese es un microservicio externo — no el objetivo de este test — y
 * levantarlo también correspondería a un test end-to-end más amplio, no a
 * este test de integración enfocado en persistencia + mensajería.
 *
 * Requiere un daemon Docker disponible en la máquina/CI donde se ejecute
 * `mvn test` (Testcontainers lo detecta automáticamente).
 */
@Testcontainers
@SpringBootTest
class PedidoIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withDatabaseName("db_pedidos_test")
                    .withUsername("admin")
                    .withPassword("admin_password");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"));

    // El Feign client hacia ms-inventario no forma parte de lo que este test
    // valida (Postgres + RabbitMQ reales); se mockea para controlar el
    // escenario de negocio sin depender de otro servicio HTTP.
    @MockBean
    private InventarioClient inventarioClient;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void debeCrearPedidoPersistirEnPostgresYPublicarEventoEnRabbitMQ() {
        // Arrange
        String sku = "SKU-INTEG-001";
        String paypalOrderId = "PAYPAL-INTEG-" + System.currentTimeMillis();

        ProductoResponse producto = new ProductoResponse();
        producto.setSku(sku);
        producto.setPrecio(new BigDecimal("250.00"));
        producto.setStockActual(20);
        when(inventarioClient.obtenerProducto(sku)).thenReturn(producto);

        PedidoItemDTO itemDTO = PedidoItemDTO.builder()
                .skuProducto(sku)
                .cantidad(3)
                .build();
        PedidoDTO pedidoDTO = PedidoDTO.builder()
                .items(List.of(itemDTO))
                .paypalOrderId(paypalOrderId)
                .build();

        // Act
        PedidoDTO resultado = pedidoService.crearPedido(pedidoDTO);

        // Assert — respuesta del servicio
        assertNotNull(resultado.getId());
        assertEquals("COMPLETADO", resultado.getEstado());
        assertEquals(0, new BigDecimal("750.00").compareTo(resultado.getPrecioTotal()));

        // Assert — se descontó stock en ms-inventario (mock)
        verify(inventarioClient, times(1)).descontarStock(sku, 3);
        verify(inventarioClient, never()).revertirStock(anyString(), anyInt());

        // Assert — el pedido quedó realmente persistido en PostgreSQL (no un mock)
        Optional<Pedido> enBaseDeDatos = pedidoRepository.findById(resultado.getId());
        assertTrue(enBaseDeDatos.isPresent(), "El pedido debe existir en la base de datos real");
        assertEquals("COMPLETADO", enBaseDeDatos.get().getEstado());
        assertEquals(paypalOrderId, enBaseDeDatos.get().getPaypalOrderId());
        assertEquals(1, enBaseDeDatos.get().getItems().size());
        assertEquals(sku, enBaseDeDatos.get().getItems().get(0).getSkuProducto());

        // Assert — el evento realmente llegó a la cola de RabbitMQ (no un mock del publisher)
        Object mensaje = rabbitTemplate.receiveAndConvert(RabbitMQConfig.QUEUE, 5000);
        assertNotNull(mensaje, "Debe haber un mensaje real en la cola " + RabbitMQConfig.QUEUE);
        assertInstanceOf(PedidoCreadoEvent.class, mensaje);

        PedidoCreadoEvent evento = (PedidoCreadoEvent) mensaje;
        assertEquals(resultado.getId(), evento.getPedidoId());
        assertEquals(sku, evento.getSkuProducto());
        assertEquals(3, evento.getCantidad());
        assertEquals("COMPLETADO", evento.getEstado());
    }

    @Test
    void debeRevertirStockYNoPublicarEventoCuandoFallaUnItemDelPedido() {
        // Arrange: el primer ítem descuenta stock correctamente; el segundo
        // no tiene stock suficiente y hace fallar todo el pedido.
        String skuOk = "SKU-INTEG-OK";
        String skuInsuficiente = "SKU-INTEG-FAIL";
        String paypalOrderId = "PAYPAL-INTEG-FAIL-" + System.currentTimeMillis();

        ProductoResponse productoOk = new ProductoResponse();
        productoOk.setSku(skuOk);
        productoOk.setPrecio(new BigDecimal("100.00"));
        productoOk.setStockActual(10);
        when(inventarioClient.obtenerProducto(skuOk)).thenReturn(productoOk);

        ProductoResponse productoSinStock = new ProductoResponse();
        productoSinStock.setSku(skuInsuficiente);
        productoSinStock.setPrecio(new BigDecimal("50.00"));
        productoSinStock.setStockActual(1); // se pedirán 5
        when(inventarioClient.obtenerProducto(skuInsuficiente)).thenReturn(productoSinStock);

        PedidoItemDTO itemOk = PedidoItemDTO.builder().skuProducto(skuOk).cantidad(2).build();
        PedidoItemDTO itemFalla = PedidoItemDTO.builder().skuProducto(skuInsuficiente).cantidad(5).build();
        PedidoDTO pedidoDTO = PedidoDTO.builder()
                .items(List.of(itemOk, itemFalla))
                .paypalOrderId(paypalOrderId)
                .build();

        // Act
        // Nota: a diferencia de PedidoServiceImpTest (que llama al método
        // directamente sin el proxy de Spring), aquí el bean real pasa por
        // el aspecto de Resilience4j. Con fallbackMethod configurado, ese
        // aspecto SIEMPRE captura la excepción y delega a crearPedidoFallback
        // en vez de dejarla propagar — por eso no se espera que
        // crearPedido() lance aquí. Lo que sí queda abierto (por el orden,
        // no fijado explícitamente, entre los aspectos @Transactional y
        // @CircuitBreaker) es si el guardado que hace ese fallback termina
        // persistido o se revierte junto con la transacción. Por eso este
        // test no asume ninguna de las dos y sólo valida lo que es cierto
        // en ambos casos: se revirtió el stock ya descontado y nunca se
        // publicó un evento de pedido creado.
        pedidoService.crearPedido(pedidoDTO);

        // Assert — se revirtió exactamente el stock del ítem que sí llegó a descontarse
        verify(inventarioClient, times(1)).descontarStock(skuOk, 2);
        verify(inventarioClient, never()).descontarStock(eq(skuInsuficiente), anyInt());
        verify(inventarioClient, times(1)).revertirStock(skuOk, 2);
        verify(inventarioClient, never()).revertirStock(eq(skuInsuficiente), anyInt());

        // Assert — jamás se persistió un pedido COMPLETADO con este paypalOrderId
        boolean hayPedidoCompletado = pedidoRepository.findAll().stream()
                .anyMatch(p -> paypalOrderId.equals(p.getPaypalOrderId()) && "COMPLETADO".equals(p.getEstado()));
        assertFalse(hayPedidoCompletado, "No debe quedar un pedido COMPLETADO cuando un ítem falla");

        // Assert — no se publicó ningún evento pedido.creado en la cola real
        Object mensaje = rabbitTemplate.receiveAndConvert(RabbitMQConfig.QUEUE, 2000);
        assertNull(mensaje, "No debe haber evento en RabbitMQ cuando el pedido no se completa");
    }
}