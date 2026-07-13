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
    // Estos endpoints ahora reenvían el JWT hacia ms-auth, que a partir del
    // punto 3 valida el token él mismo en vez de confiar ciegamente en el BFF.
    public UsuarioDTO registrarUsuario(UsuarioDTO dto) {
        return authClient.registrarUsuario(dto);
    }

    public List<UsuarioDTO> listarUsuarios(String authHeader) {
        return authClient.listarUsuarios(authHeader);
    }

    public UsuarioDTO crearUsuario(String authHeader, UsuarioDTO dto) {
        return authClient.crearUsuario(authHeader, dto);
    }

    public UsuarioDTO actualizarUsuario(String authHeader, Long id, UsuarioDTO dto) {
        return authClient.actualizarUsuario(authHeader, id, dto);
    }

    public void eliminarUsuario(String authHeader, Long id) {
        authClient.eliminarUsuario(authHeader, id);
    }
}