package com.smartlogix.pedidos.service;

import com.smartlogix.pedidos.model.dto.PedidoDTO;
import java.util.List;

public interface PedidoService {
    PedidoDTO crearPedido(PedidoDTO pedidoDTO);
    List<PedidoDTO> listarPedidos();
}
