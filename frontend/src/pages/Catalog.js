import React, { useState, useEffect } from 'react';
import ProductCard from '../components/ProductCard';
import OrderModal from '../components/OrderModal';
import { api } from '../services/api';

const Catalog = () => {
  const [productos, setProductos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [successMsg, setSuccessMsg] = useState('');

  const fetchCatalogo = async () => {
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
  };

  useEffect(() => {
    fetchCatalogo();
  }, []);

  const handlePurchaseSuccess = (response) => {
    setSelectedProduct(null);
    setSuccessMsg(response.mensaje || '¡Pedido completado con éxito!');
    
    // Actualizar catálogo para reflejar el stock descontado
    fetchCatalogo();
    
    // Ocultar mensaje después de 5 segundos
    setTimeout(() => {
      setSuccessMsg('');
    }, 5000);
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

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
          <h2>Productos Disponibles</h2>
          <button className="btn-secondary" onClick={fetchCatalogo}>
            <i className="ri-refresh-line"></i> Actualizar
          </button>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem' }}>
            <i className="ri-loader-4-line spin" style={{ fontSize: '3rem', color: 'var(--primary)' }}></i>
            <p style={{ marginTop: '1rem', color: 'var(--text-muted)' }}>Cargando catálogo desde el BFF...</p>
          </div>
        ) : productos.length === 0 ? (
          <div className="glass-panel" style={{ textAlign: 'center', padding: '3rem' }}>
            <i className="ri-inbox-line" style={{ fontSize: '3rem', color: 'var(--text-muted)' }}></i>
            <h3 style={{ margin: '1rem 0' }}>Inventario Vacío</h3>
            <p className="product-desc">No hay productos disponibles en este momento.</p>
          </div>
        ) : (
          <div className="grid grid-cols-4">
            {productos.map(producto => (
              <ProductCard 
                key={producto.sku} 
                producto={producto} 
                onBuyClick={setSelectedProduct} 
              />
            ))}
          </div>
        )}
      </main>

      {selectedProduct && (
        <OrderModal 
          producto={selectedProduct} 
          onClose={() => setSelectedProduct(null)}
          onPurchaseSuccess={handlePurchaseSuccess}
        />
      )}
    </div>
  );
};

export default Catalog;
