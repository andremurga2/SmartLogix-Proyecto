package com.smartlogix.bff.service;

import com.smartlogix.bff.client.AuthClient;
import com.smartlogix.bff.client.InventarioClient;
import com.smartlogix.bff.client.PedidosClient;
import com.smartlogix.bff.model.PedidoDTO;
import com.smartlogix.bff.model.ProductoDTO;
import com.smartlogix.bff.model.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BffService {

    private final InventarioClient inventarioClient;
    private final PedidosClient pedidosClient;
    private final AuthClient authClient;

    public List<ProductoDTO> obtenerCatalogo() {
        return inventarioClient.listarTodos();
    }

    public PedidoDTO realizarCompra(PedidoDTO pedidoDTO) {
        return pedidosClient.crearPedido(pedidoDTO);
    }

    // ── Admin: Productos ──────────────────────────────────────────────────────
    public ProductoDTO crearProducto(ProductoDTO productoDTO) {
        return inventarioClient.crearProducto(productoDTO);
    }

    public ProductoDTO actualizarProducto(String sku, ProductoDTO productoDTO) {
        return inventarioClient.actualizarProducto(sku, productoDTO);
    }

    public void eliminarProducto(String sku) {
        inventarioClient.eliminarProducto(sku);
    }

    // ── Admin: Pedidos ────────────────────────────────────────────────────────
    public List<PedidoDTO> listarPedidos() {
        return pedidosClient.listarPedidos();
    }

    // ── Admin: Usuarios ───────────────────────────────────────────────────────
    public List<UsuarioDTO> listarUsuarios() {
        return authClient.listarUsuarios();
    }

    public UsuarioDTO crearUsuario(UsuarioDTO dto) {
        return authClient.crearUsuario(dto);
    }

    public UsuarioDTO actualizarUsuario(Long id, UsuarioDTO dto) {
        return authClient.actualizarUsuario(id, dto);
    }

    public void eliminarUsuario(Long id) {
        authClient.eliminarUsuario(id);
    }
}