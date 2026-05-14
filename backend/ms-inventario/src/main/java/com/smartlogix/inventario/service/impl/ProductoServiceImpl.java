package com.smartlogix.inventario.service.impl;

import com.smartlogix.inventario.factory.ProductoFactory;
import com.smartlogix.inventario.model.dto.ProductoDTO;
import com.smartlogix.inventario.model.entity.Producto;
import com.smartlogix.inventario.repository.ProductoRepository;
import com.smartlogix.inventario.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoFactory productoFactory;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> obtenerTodos() {
        return productoRepository.findAll()
                .stream()
                .map(productoFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoDTO obtenerPorSku(String sku) {
        Producto producto = productoRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con SKU: " + sku));
        return productoFactory.toDTO(producto);
    }

    @Override
    @Transactional
    public ProductoDTO crearProducto(ProductoDTO productoDTO) {
        Producto entity = productoFactory.toEntity(productoDTO);
        Producto guardado = productoRepository.save(entity);
        return productoFactory.toDTO(guardado);
    }

    @Override
    @Transactional
    public ProductoDTO actualizarProducto(String sku, ProductoDTO productoDTO) {
        Producto producto = productoRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con SKU: " + sku));
        producto.setNombre(productoDTO.getNombre());
        producto.setDescripcion(productoDTO.getDescripcion());
        producto.setPrecio(productoDTO.getPrecio());
        producto.setStockActual(productoDTO.getStockActual());
        return productoFactory.toDTO(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public void eliminarProducto(String sku) {
        Producto producto = productoRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con SKU: " + sku));
        productoRepository.delete(producto);
    }

    @Override
    @Transactional
    public void actualizarStock(String sku, Integer cantidadComprada) {
        Producto producto = productoRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con SKU: " + sku));
        if (producto.getStockActual() < cantidadComprada) {
            throw new RuntimeException("Stock insuficiente para el SKU: " + sku);
        }
        producto.setStockActual(producto.getStockActual() - cantidadComprada);
        productoRepository.save(producto);
    }
}
