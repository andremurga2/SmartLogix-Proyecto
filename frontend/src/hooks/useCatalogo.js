import { useState, useEffect, useCallback } from 'react';
import { api } from '../services/api';

const PRODUCTOS_POR_PAGINA = 8;

export function useCatalogo() {
  const [productos, setProductos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [busqueda, setBusqueda] = useState('');
  const [soloDisponibles, setSoloDisponibles] = useState(false);
  const [paginaActual, setPaginaActual] = useState(1);

  const fetchCatalogo = useCallback(async () => {
    try {
      setLoading(true);
      const data = await api.getCatalogo();
      setProductos(data);
      setError(null);
    } catch (err) {
      setError('No se pudo cargar el catálogo. Verifique si el BFF está ejecutándose.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCatalogo();
  }, [fetchCatalogo]);

  useEffect(() => {
    setPaginaActual(1);
  }, [busqueda, soloDisponibles]);

  const productosFiltrados = productos.filter(p => {
    const termino = busqueda.toLowerCase();
    const coincide = p.nombre.toLowerCase().includes(termino) || p.sku.toLowerCase().includes(termino);
    const disponible = soloDisponibles ? p.disponible && p.stockActual > 0 : true;
    return coincide && disponible;
  });

  const totalPaginas = Math.ceil(productosFiltrados.length / PRODUCTOS_POR_PAGINA);
  const inicio = (paginaActual - 1) * PRODUCTOS_POR_PAGINA;
  const productosPagina = productosFiltrados.slice(inicio, inicio + PRODUCTOS_POR_PAGINA);

  const cambiarPagina = (pagina) => {
    setPaginaActual(pagina);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return {
    productos, productosFiltrados, productosPagina,
    loading, error, fetchCatalogo,
    busqueda, setBusqueda,
    soloDisponibles, setSoloDisponibles,
    paginaActual, totalPaginas, cambiarPagina,
  };
}