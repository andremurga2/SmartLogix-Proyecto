package com.smartlogix.pedidos.service;

import java.util.List;

import com.smartlogix.pedidos.model.dto.PedidoDTO;

public interface PedidoService {
    PedidoDTO crearPedido(PedidoDTO pedidoDTO);
    List<PedidoDTO> listarPedidos();
}
