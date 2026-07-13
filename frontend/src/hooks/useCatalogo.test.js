// frontend/src/hooks/useCatalogo.test.js
import { renderHook, act, waitFor } from '@testing-library/react';
import { useCatalogo } from './useCatalogo';
import { api } from '../services/api';

jest.mock('../services/api', () => ({
  api: {
    getCatalogo: jest.fn(),
  },
}));

const productosMock = [
  { sku: 'SKU-001', nombre: 'Laptop', precio: 999.99, stockActual: 5, disponible: true },
  { sku: 'SKU-002', nombre: 'Mouse', precio: 20, stockActual: 0, disponible: false },
  { sku: 'SKU-003', nombre: 'Teclado Mecánico', precio: 80, stockActual: 3, disponible: true },
];

beforeEach(() => {
  jest.clearAllMocks();
  // jsdom no implementa scrollTo; cambiarPagina lo invoca, así que lo simulamos.
  window.scrollTo = jest.fn();
});

describe('useCatalogo — carga inicial', () => {
  test('inicia en estado de carga y expone loading = true', async () => {
    api.getCatalogo.mockResolvedValueOnce(productosMock);
    const { result } = renderHook(() => useCatalogo());

    expect(result.current.loading).toBe(true);

    await waitFor(() => expect(result.current.loading).toBe(false));
  });

  test('carga el catálogo y lo expone en productos/productosPagina', async () => {
    api.getCatalogo.mockResolvedValueOnce(productosMock);
    const { result } = renderHook(() => useCatalogo());

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(api.getCatalogo).toHaveBeenCalledTimes(1);
    expect(result.current.productos).toHaveLength(3);
    expect(result.current.productosPagina).toHaveLength(3);
    expect(result.current.error).toBeNull();
  });

  test('expone un mensaje de error si falla la carga del catálogo', async () => {
    api.getCatalogo.mockRejectedValueOnce(new Error('network error'));
    const { result } = renderHook(() => useCatalogo());

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.error).toBe(
      'No se pudo cargar el catálogo. Verifique si el BFF está ejecutándose.'
    );
    expect(result.current.productos).toEqual([]);
  });
});

describe('useCatalogo — búsqueda y filtro de disponibilidad', () => {
  test('filtra productos por nombre', async () => {
    api.getCatalogo.mockResolvedValueOnce(productosMock);
    const { result } = renderHook(() => useCatalogo());
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => {
      result.current.setBusqueda('mouse');
    });

    expect(result.current.productosFiltrados).toHaveLength(1);
    expect(result.current.productosFiltrados[0].sku).toBe('SKU-002');
  });

  test('filtra productos por SKU', async () => {
    api.getCatalogo.mockResolvedValueOnce(productosMock);
    const { result } = renderHook(() => useCatalogo());
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => {
      result.current.setBusqueda('SKU-003');
    });

    expect(result.current.productosFiltrados).toHaveLength(1);
    expect(result.current.productosFiltrados[0].nombre).toBe('Teclado Mecánico');
  });

  test('el filtro "solo disponibles" excluye productos sin stock', async () => {
    api.getCatalogo.mockResolvedValueOnce(productosMock);
    const { result } = renderHook(() => useCatalogo());
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => {
      result.current.setSoloDisponibles(true);
    });

    expect(result.current.productosFiltrados).toHaveLength(2);
    expect(result.current.productosFiltrados.map(p => p.sku)).toEqual(['SKU-001', 'SKU-003']);
  });

  test('reinicia a la página 1 cuando cambia la búsqueda', async () => {
    api.getCatalogo.mockResolvedValueOnce(productosMock);
    const { result } = renderHook(() => useCatalogo());
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => {
      result.current.cambiarPagina(2);
    });
    expect(result.current.paginaActual).toBe(2);

    act(() => {
      result.current.setBusqueda('laptop');
    });

    expect(result.current.paginaActual).toBe(1);
  });
});

describe('useCatalogo — paginación', () => {
  test('pagina los productos según PRODUCTOS_POR_PAGINA (8 por página)', async () => {
    const muchosProductos = Array.from({ length: 10 }, (_, i) => ({
      sku: `SKU-${i}`,
      nombre: `Producto ${i}`,
      precio: 10,
      stockActual: 1,
      disponible: true,
    }));
    api.getCatalogo.mockResolvedValueOnce(muchosProductos);

    const { result } = renderHook(() => useCatalogo());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.totalPaginas).toBe(2);
    expect(result.current.productosPagina).toHaveLength(8);

    act(() => {
      result.current.cambiarPagina(2);
    });

    expect(result.current.productosPagina).toHaveLength(2);
  });
});

describe('useCatalogo — fetchCatalogo', () => {
  test('permite recargar el catálogo manualmente', async () => {
    api.getCatalogo.mockResolvedValueOnce(productosMock);
    const { result } = renderHook(() => useCatalogo());
    await waitFor(() => expect(result.current.loading).toBe(false));

    api.getCatalogo.mockResolvedValueOnce([...productosMock, {
      sku: 'SKU-004', nombre: 'Monitor', precio: 300, stockActual: 2, disponible: true,
    }]);

    await act(async () => {
      await result.current.fetchCatalogo();
    });

    expect(api.getCatalogo).toHaveBeenCalledTimes(2);
    expect(result.current.productos).toHaveLength(4);
  });
});