import React from 'react';
import './ProductCard.css';

const ProductCard = ({ producto, onBuyClick }) => {
  const isOutOfStock = !producto.disponible || producto.stockActual <= 0;

  // Creamos un color de gradiente único basado en el SKU para darle un toque dinámico a cada card
  const getGradient = (sku) => {
    const hash = sku.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    const hues = [210, 260, 320, 150, 40, 0];
    const hue = hues[hash % hues.length];
    return `linear-gradient(135deg, hsl(${hue}, 80%, 60%) 0%, hsl(${hue + 40}, 80%, 40%) 100%)`;
  };

  return (
    <div className="product-card glass-panel">
      <div 
        className="product-image-placeholder"
        style={{ background: getGradient(producto.sku) }}
      >
        <i className="ri-box-3-line product-icon"></i>
        <div className="product-badge">
          {isOutOfStock ? (
            <span className="badge badge-danger">Agotado</span>
          ) : (
            <span className="badge badge-success">En Stock: {producto.stockActual}</span>
          )}
        </div>
      </div>
      
      <div className="product-info">
        <div className="product-header">
          <span className="product-sku">{producto.sku}</span>
          <h3 className="product-name">{producto.nombre}</h3>
        </div>
        
        <p className="product-desc">{producto.descripcion || 'Sin descripción disponible.'}</p>
        
        <div className="product-footer">
          <span className="product-price">${producto.precio.toLocaleString()}</span>
          <button 
            className="btn-primary" 
            onClick={() => onBuyClick(producto)}
            disabled={isOutOfStock}
          >
            <i className="ri-shopping-cart-line"></i>
            Comprar
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProductCard;
