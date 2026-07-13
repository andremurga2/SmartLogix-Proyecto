import React, { useState } from 'react';
import ProductCard from '../components/ProductCard';
import CartModal from '../components/CartModal';
import { useCart } from '../context/CartContext';
import { useCatalogo } from '../hooks/useCatalogo';

const Catalog = () => {
  const {
    productos, productosFiltrados, productosPagina,
    loading, error, fetchCatalogo,
    busqueda, setBusqueda,
    soloDisponibles, setSoloDisponibles,
    paginaActual, totalPaginas, cambiarPagina,
  } = useCatalogo();

  const [showCart, setShowCart] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');

  const { cantidadTotal } = useCart();

  const handlePurchaseSuccess = (response) => {
    setShowCart(false);
    setSuccessMsg(response.mensaje || '¡Pedido completado con éxito!');
    fetchCatalogo();
    setTimeout(() => setSuccessMsg(''), 5000);
  };

  return (
    <div>
      <header className="hero-banner">
        <div className="hero-content">
          <h1 className="hero-title">Catálogo SmartLogix</h1>
          <p className="hero-subtitle">
            Gestión inteligente de inventario y pedidos. Plataforma integrada
            para administrar tus recursos en tiempo real.
          </p>
        </div>
      </header>

      <main className="container">
        {successMsg && (
          <div className="alert alert-success">
            <i className="ri-checkbox-circle-fill"></i>
            {successMsg}
          </div>
        )}

        {error && (
          <div className="alert alert-error">
            <i className="ri-error-warning-fill"></i>
            {error}
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <h2>Productos Disponibles</h2>
          <button className="btn-secondary" onClick={fetchCatalogo}>
            <i className="ri-refresh-line"></i> Actualizar
          </button>
        </div>

        {/* ── Barra de búsqueda y filtros ── */}
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap' }}>
          <div style={{ position: 'relative', flex: 1, minWidth: 200 }}>
            <i className="ri-search-line" style={{
              position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)',
              color: 'var(--text-muted)', fontSize: '1.1rem'
            }}></i>
            <input
              type="text"
              placeholder="Buscar por nombre o SKU..."
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              style={{
                width: '100%', padding: '0.65rem 1rem 0.65rem 2.25rem',
                borderRadius: 8, border: '1px solid rgba(255,255,255,0.15)',
                background: 'rgba(255,255,255,0.07)', color: 'var(--text-bright)',
                fontSize: '0.95rem', boxSizing: 'border-box', outline: 'none',
              }}
            />
          </div>

          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', userSelect: 'none', color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            <input
              type="checkbox"
              checked={soloDisponibles}
              onChange={(e) => setSoloDisponibles(e.target.checked)}
              style={{ width: 16, height: 16, cursor: 'pointer' }}
            />
            Solo disponibles
          </label>

          {(busqueda || soloDisponibles) && (
            <button
              onClick={() => { setBusqueda(''); setSoloDisponibles(false); }}
              style={{
                background: 'none', border: '1px solid rgba(255,255,255,0.2)',
                color: 'var(--text-muted)', borderRadius: 8, padding: '0.65rem 1rem',
                cursor: 'pointer', fontSize: '0.85rem'
              }}
            >
              <i className="ri-close-line"></i> Limpiar
            </button>
          )}
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem' }}>
            <i className="ri-loader-4-line spin" style={{ fontSize: '3rem', color: 'var(--primary)' }}></i>
            <p style={{ marginTop: '1rem', color: 'var(--text-muted)' }}>Cargando catálogo desde el BFF...</p>
          </div>
        ) : productosFiltrados.length === 0 ? (
          <div className="glass-panel" style={{ textAlign: 'center', padding: '3rem' }}>
            <i className="ri-search-line" style={{ fontSize: '3rem', color: 'var(--text-muted)' }}></i>
            <h3 style={{ margin: '1rem 0' }}>
              {productos.length === 0 ? 'Inventario Vacío' : 'Sin resultados'}
            </h3>
            <p className="product-desc">
              {productos.length === 0
                ? 'No hay productos disponibles en este momento.'
                : `No se encontraron productos para "${busqueda}".`}
            </p>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-4">
              {productosPagina.map(producto => (
                <ProductCard key={producto.sku} producto={producto} />
              ))}
            </div>

            {/* ── Paginación ── */}
            {totalPaginas > 1 && (
              <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '0.5rem', marginTop: '2.5rem' }}>
                <button
                  onClick={() => cambiarPagina(paginaActual - 1)}
                  disabled={paginaActual === 1}
                  style={{
                    padding: '0.5rem 1rem', borderRadius: 8, border: '1px solid rgba(255,255,255,0.15)',
                    background: 'rgba(255,255,255,0.07)', color: paginaActual === 1 ? 'var(--text-muted)' : 'var(--text-bright)',
                    cursor: paginaActual === 1 ? 'not-allowed' : 'pointer', fontSize: '0.9rem'
                  }}
                >
                  <i className="ri-arrow-left-s-line"></i>
                </button>

                {Array.from({ length: totalPaginas }, (_, i) => i + 1).map(pagina => (
                  <button
                    key={pagina}
                    onClick={() => cambiarPagina(pagina)}
                    style={{
                      padding: '0.5rem 0.9rem', borderRadius: 8,
                      border: pagina === paginaActual ? 'none' : '1px solid rgba(255,255,255,0.15)',
                      background: pagina === paginaActual ? 'var(--primary)' : 'rgba(255,255,255,0.07)',
                      color: 'var(--text-bright)', cursor: 'pointer',
                      fontWeight: pagina === paginaActual ? 700 : 400, fontSize: '0.9rem'
                    }}
                  >
                    {pagina}
                  </button>
                ))}

                <button
                  onClick={() => cambiarPagina(paginaActual + 1)}
                  disabled={paginaActual === totalPaginas}
                  style={{
                    padding: '0.5rem 1rem', borderRadius: 8, border: '1px solid rgba(255,255,255,0.15)',
                    background: 'rgba(255,255,255,0.07)', color: paginaActual === totalPaginas ? 'var(--text-muted)' : 'var(--text-bright)',
                    cursor: paginaActual === totalPaginas ? 'not-allowed' : 'pointer', fontSize: '0.9rem'
                  }}
                >
                  <i className="ri-arrow-right-s-line"></i>
                </button>
              </div>
            )}
          </>
        )}
      </main>

      {/* Botón flotante del carrito */}
      <button
        className="btn-primary"
        onClick={() => setShowCart(true)}
        style={{
          position: 'fixed', bottom: '2rem', right: '2rem', zIndex: 500,
          borderRadius: '50px', padding: '1rem 1.5rem', display: 'flex',
          alignItems: 'center', gap: '0.5rem', boxShadow: '0 10px 30px rgba(0,0,0,0.3)',
        }}
      >
        <i className="ri-shopping-cart-2-line" style={{ fontSize: '1.3rem' }}></i>
        Carrito {cantidadTotal > 0 && `(${cantidadTotal})`}
      </button>

      {showCart && (
        <CartModal
          onClose={() => setShowCart(false)}
          onPurchaseSuccess={handlePurchaseSuccess}
        />
      )}
    </div>
  );
};

export default Catalog;