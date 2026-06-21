package com.smartlogix.pedidos.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PedidoItem> items = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal precioTotal;

    @Column(nullable = false)
    private String estado; // PENDIENTE, COMPLETADO, CANCELADO, FALLIDO

    @Column(name = "paypal_order_id")
    private String paypalOrderId;

    /** Mantiene la relación bidireccional sincronizada. */
    public void addItem(PedidoItem item) {
        items.add(item);
        item.setPedido(this);
    }
}