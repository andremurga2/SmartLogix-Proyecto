package com.smartlogix.bff.service;

import com.smartlogix.bff.client.AuthClient;
import com.smartlogix.bff.client.InventarioClient;
import com.smartlogix.bff.client.PedidosClient;
import com.smartlogix.bff.model.PedidoDTO;
import com.smartlogix.bff.model.ProductoDTO;
import com.smartlogix.bff.model.UsuarioDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BffServiceTest {

    @Mock private InventarioClient inventarioClient;
    @Mock private PedidosClient pedidosClient;
    @Mock private AuthClient authClient;

    @InjectMocks
    private BffService bffService;

    @Test
    void obtenerCatalogoDebeRetornarListaDeProductos() {
        ProductoDTO p = new ProductoDTO();
        p.setSku("SKU-001");
        when(inventarioClient.listarTodos()).thenReturn(List.of(p));

        List<ProductoDTO> result = bffService.obtenerCatalogo();

        assertEquals(1, result.size());
        assertEquals("SKU-001", result.get(0).getSku());
    }

    @Test
    void realizarCompraDebeDelegarAPedidosClient() {
        PedidoDTO pedido = new PedidoDTO();
        pedido.setEstado("COMPLETADO");
        when(pedidosClient.crearPedido(pedido)).thenReturn(pedido);

        PedidoDTO result = bffService.realizarCompra(pedido);

        assertEquals("COMPLETADO", result.getEstado());
        verify(pedidosClient, times(1)).crearPedido(pedido);
    }

    @Test
    void crearProductoDebeDelegarAInventarioClient() {
        ProductoDTO p = new ProductoDTO();
        p.setSku("SKU-002");
        when(inventarioClient.crearProducto(p)).thenReturn(p);

        ProductoDTO result = bffService.crearProducto(p);

        assertEquals("SKU-002", result.getSku());
    }

    @Test
    void actualizarProductoDebeDelegarAInventarioClient() {
        ProductoDTO p = new ProductoDTO();
        p.setSku("SKU-001");
        when(inventarioClient.actualizarProducto("SKU-001", p)).thenReturn(p);

        ProductoDTO result = bffService.actualizarProducto("SKU-001", p);

        assertEquals("SKU-001", result.getSku());
    }

    @Test
    void eliminarProductoDebeLlamarAInventarioClient() {
        doNothing().when(inventarioClient).eliminarProducto("SKU-001");

        bffService.eliminarProducto("SKU-001");

        verify(inventarioClient, times(1)).eliminarProducto("SKU-001");
    }

    @Test
    void listarPedidosDebeDelegarAPedidosClient() {
        PedidoDTO pedido = new PedidoDTO();
        when(pedidosClient.listarPedidos()).thenReturn(List.of(pedido));

        List<PedidoDTO> result = bffService.listarPedidos();

        assertEquals(1, result.size());
    }

    @Test
    void registrarUsuarioDebeDelegarAAuthClient() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setUsername("nuevo");
        when(authClient.registrarUsuario(dto)).thenReturn(dto);

        UsuarioDTO result = bffService.registrarUsuario(dto);

        assertEquals("nuevo", result.getUsername());
    }

    @Test
    void listarUsuariosDebeDelegarAAuthClient() {
        UsuarioDTO dto = new UsuarioDTO();
        when(authClient.listarUsuarios()).thenReturn(List.of(dto));

        List<UsuarioDTO> result = bffService.listarUsuarios();

        assertEquals(1, result.size());
    }

    @Test
    void eliminarUsuarioDebeLlamarAAuthClient() {
        doNothing().when(authClient).eliminarUsuario(1L);

        bffService.eliminarUsuario(1L);

        verify(authClient, times(1)).eliminarUsuario(1L);
    }
}