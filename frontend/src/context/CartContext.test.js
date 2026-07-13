// frontend/src/context/CartContext.test.js
import React from 'react';
import { renderHook, act } from '@testing-library/react';
import { CartProvider, useCart } from './CartContext';

const STORAGE_KEY = 'smartlogix_cart';

const wrapper = ({ children }) => <CartProvider>{children}</CartProvider>;

const productoMock = {
  sku: 'SKU-001',
  nombre: 'Laptop Test',
  precio: 999.99,
  stockActual: 5,
};

beforeEach(() => {
  localStorage.clear();
});

describe('CartContext — estado inicial', () => {
  test('inicia con el carrito vacío cuando no hay nada en localStorage', () => {
    const { result } = renderHook(() => useCart(), { wrapper });

    expect(result.current.items).toEqual([]);
    expect(result.current.total).toBe(0);
    expect(result.current.cantidadTotal).toBe(0);
  });

  test('carga el carrito inicial desde localStorage si existe', () => {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify([{ sku: 'SKU-999', nombre: 'Previo', precio: 10, stockActual: 3, cantidad: 2 }])
    );

    const { result } = renderHook(() => useCart(), { wrapper });

    expect(result.current.items).toHaveLength(1);
    expect(result.current.items[0].sku).toBe('SKU-999');
    expect(result.current.cantidadTotal).toBe(2);
  });

  test('useCart lanza error si se usa fuera de un CartProvider', () => {
    // Silenciamos el console.error que React imprime por el throw dentro del hook
    const spy = jest.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => renderHook(() => useCart())).toThrow(
      'useCart debe usarse dentro de un CartProvider'
    );

    spy.mockRestore();
  });
});

describe('CartContext — agregarItem', () => {
  test('agrega un producto nuevo al carrito', () => {
    const { result } = renderHook(() => useCart(), { wrapper });

    act(() => {
      result.current.agregarItem(productoMock, 1);
    });

    expect(result.current.items).toHaveLength(1);
    expect(result.current.items[0]).toMatchObject({ sku: 'SKU-001', cantidad: 1 });
  });

  test('incrementa la cantidad si el producto ya existe en el carrito', () => {
    const { result } = renderHook(() => useCart(), { wrapper });

    act(() => {
      result.current.agregarItem(productoMock, 1);
    });
    act(() => {
      result.current.agregarItem(productoMock, 2);
    });

    expect(result.current.items).toHaveLength(1);
    expect(result.current.items[0].cantidad).toBe(3);
  });

  test('no permite agregar más unidades que el stock disponible', () => {
    const { result } = renderHook(() => useCart(), { wrapper });

    act(() => {
      result.current.agregarItem(productoMock, 3);
    });
    act(() => {
      result.current.agregarItem(productoMock, 10); // pediría 13, pero el stock es 5
    });

    expect(result.current.items[0].cantidad).toBe(5);
  });

  test('respeta el tope de stock incluso en la primera adición', () => {
    const { result } = renderHook(() => useCart(), { wrapper });

    act(() => {
      result.current.agregarItem(productoMock, 999);
    });

    expect(result.current.items[0].cantidad).toBe(5);
  });
});

describe('CartContext — actualizarCantidad', () => {
  test('actualiza la cantidad de un ítem existente', () => {
    const { result } = renderHook(() => useCart(), { wrapper });

    act(() => {
      result.current.agregarItem(productoMock, 1);
    });
    act(() => {
      result.current.actualizarCantidad('SKU-001', 4);
    });

    expect(result.current.items[0].cantidad).toBe(4);
  });

  test('no permite bajar de 1 unidad', () => {
    const { result } = renderHook(() => useCart(), { wrapper });

    act(() => {
      result.current.agregarItem(productoMock, 1);
    });
    act(() => {
      result.current.actualizarCantidad('SKU-001', 0);
    });

    expect(result.current.items[0].cantidad).toBe(1);
  });

  test('no permite superar el stock disponible', () => {
    const { result } = renderHook(() => useCart(), { wrapper });

    act(() => {
      result.current.agregarItem(productoMock, 1);
    });
    act(() => {
      result.current.actualizarCantidad('SKU-001', 999);
    });

    expect(result.current.items[0].cantidad).toBe(5);
  });
});

describe('CartContext — quitarItem y vaciarCarrito', () => {
  test('quita un producto específico del carrito', () => {
    const { result } = renderHook(() => useCart(), { wrapper });
    const otroProducto = { sku: 'SKU-002', nombre: 'Mouse', precio: 20, stockActual: 10 };

    act(() => {
      result.current.agregarItem(productoMock, 1);
    });
    act(() => {
      result.current.agregarItem(otroProducto, 1);
    });
    act(() => {
      result.current.quitarItem('SKU-001');
    });

    expect(result.current.items).toHaveLength(1);
    expect(result.current.items[0].sku).toBe('SKU-002');
  });

  test('vacía completamente el carrito', () => {
    const { result } = renderHook(() => useCart(), { wrapper });

    act(() => {
      result.current.agregarItem(productoMock, 2);
    });
    act(() => {
      result.current.vaciarCarrito();
    });

    expect(result.current.items).toEqual([]);
    expect(result.current.total).toBe(0);
  });
});

describe('CartContext — total y cantidadTotal', () => {
  test('calcula el total y la cantidad total con múltiples productos', () => {
    const { result } = renderHook(() => useCart(), { wrapper });
    const otroProducto = { sku: 'SKU-002', nombre: 'Mouse', precio: 20, stockActual: 10 };

    act(() => {
      result.current.agregarItem(productoMock, 2); // 2 * 999.99
    });
    act(() => {
      result.current.agregarItem(otroProducto, 3); // 3 * 20
    });

    expect(result.current.cantidadTotal).toBe(5);
    expect(result.current.total).toBeCloseTo(2 * 999.99 + 3 * 20, 2);
  });
});

describe('CartContext — persistencia en localStorage', () => {
  test('persiste el carrito en localStorage al agregar un ítem', () => {
    const { result } = renderHook(() => useCart(), { wrapper });

    act(() => {
      result.current.agregarItem(productoMock, 1);
    });

    const guardado = JSON.parse(localStorage.getItem(STORAGE_KEY));
    expect(guardado).toHaveLength(1);
    expect(guardado[0].sku).toBe('SKU-001');
  });

  test('actualiza localStorage al vaciar el carrito', () => {
    const { result } = renderHook(() => useCart(), { wrapper });

    act(() => {
      result.current.agregarItem(productoMock, 1);
    });
    act(() => {
      result.current.vaciarCarrito();
    });

    const guardado = JSON.parse(localStorage.getItem(STORAGE_KEY));
    expect(guardado).toEqual([]);
  });
});