import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';

const CartContext = createContext(null);
const STORAGE_KEY = 'smartlogix_cart';

const cargarCarritoInicial = () => {
  try {
    const guardado = localStorage.getItem(STORAGE_KEY);
    return guardado ? JSON.parse(guardado) : [];
  } catch {
    return [];
  }
};

export const CartProvider = ({ children }) => {
  const [items, setItems] = useState(cargarCarritoInicial); // [{ sku, nombre, precio, cantidad, stockActual }]

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    } catch {
      // localStorage lleno o deshabilitado: el carrito sigue funcionando
      // en memoria para esta sesión, solo no persiste al refrescar.
    }
  }, [items]);

  const agregarItem = useCallback((producto, cantidad = 1) => {
    setItems(prev => {
      const existente = prev.find(i => i.sku === producto.sku);
      if (existente) {
        const nuevaCantidad = Math.min(existente.cantidad + cantidad, producto.stockActual);
        return prev.map(i => i.sku === producto.sku ? { ...i, cantidad: nuevaCantidad } : i);
      }
      return [...prev, {
        sku: producto.sku,
        nombre: producto.nombre,
        precio: producto.precio,
        stockActual: producto.stockActual,
        cantidad: Math.min(cantidad, producto.stockActual),
      }];
    });
  }, []);

  const actualizarCantidad = useCallback((sku, cantidad) => {
    setItems(prev => prev.map(i =>
      i.sku === sku ? { ...i, cantidad: Math.max(1, Math.min(cantidad, i.stockActual)) } : i
    ));
  }, []);

  const quitarItem = useCallback((sku) => {
    setItems(prev => prev.filter(i => i.sku !== sku));
  }, []);

  const vaciarCarrito = useCallback(() => setItems([]), []);

  const total = items.reduce((acc, i) => acc + i.precio * i.cantidad, 0);
  const cantidadTotal = items.reduce((acc, i) => acc + i.cantidad, 0);

  return (
    <CartContext.Provider value={{
      items, agregarItem, actualizarCantidad, quitarItem, vaciarCarrito, total, cantidadTotal,
    }}>
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart debe usarse dentro de un CartProvider');
  return ctx;
};