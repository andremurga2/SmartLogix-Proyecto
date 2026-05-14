import React, { useState } from 'react';
import './OrderModal.css';
import PayPalCheckout from './PayPalCheckout';

/**
 * OrderModal — Flujo de compra con PayPal integrado.
 *
 * Pasos:
 *   1. El usuario selecciona cantidad → ve el resumen y el botón PayPal.
 *   2. PayPalCheckout crea la orden en el BFF y renderiza el botón oficial.
 *   3. Al aprobarse y capturarse el pago, se llama onPurchaseSuccess.
 */
const OrderModal = ({ producto, onClose, onPurchaseSuccess }) => {
  const [cantidad, setCantidad] = useState(1);
  const [error, setError] = useState('');
  const [step, setStep] = useState('resumen'); // 'resumen' | 'pago'

  const montoTotal = (producto.precio * cantidad).toFixed(2);

  const handleContinuarPago = () => {
    if (cantidad <= 0 || cantidad > producto.stockActual) {
      setError(`La cantidad debe estar entre 1 y ${producto.stockActual}`);
      return;
    }
    setError('');
    setStep('pago');
  };

  const handlePayPalSuccess = (pagoResponse) => {
    onPurchaseSuccess({
      ...pagoResponse.pedido,
      mensaje: pagoResponse.mensaje || '¡Pago con PayPal completado exitosamente!',
      paypalOrderId: pagoResponse.paypalOrderId,
    });
  };

  const handlePayPalError = (msg) => {
    setError(msg);
    setStep('resumen'); // volver al resumen si hay error
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content glass-panel" onClick={e => e.stopPropagation()}>

        {/* Header */}
        <div className="modal-header">
          <h2>{step === 'resumen' ? 'Confirmar Pedido' : 'Pagar con PayPal'}</h2>
          <button className="btn-close" onClick={onClose}>
            <i className="ri-close-line"></i>
          </button>
        </div>

        <div className="modal-body">
          {/* ── PASO 1: Resumen del pedido ── */}
          {step === 'resumen' && (
            <>
              <div className="product-summary">
                <div className="summary-row">
                  <span>Producto:</span>
                  <strong>{producto.nombre}</strong>
                </div>
                <div className="summary-row">
                  <span>SKU:</span>
                  <strong>{producto.sku}</strong>
                </div>
                <div className="summary-row">
                  <span>Precio Unitario:</span>
                  <strong>${producto.precio.toLocaleString()}</strong>
                </div>
              </div>

              <div className="input-group">
                <label>Cantidad a comprar (Max: {producto.stockActual})</label>
                <input
                  type="number"
                  className="input-glass"
                  value={cantidad}
                  onChange={e => { setCantidad(e.target.value); setError(''); }}
                  min="1"
                  max={producto.stockActual}
                />
              </div>

              <div className="total-price">
                <span>Total a pagar:</span>
                <h3>${(producto.precio * cantidad).toLocaleString()} USD</h3>
              </div>

              {error && (
                <div className="alert alert-error">
                  <i className="ri-error-warning-line"></i> {error}
                </div>
              )}
            </>
          )}

          {/* ── PASO 2: Botón PayPal ── */}
          {step === 'pago' && (
            <>
              <div className="product-summary">
                <div className="summary-row">
                  <span>Producto:</span>
                  <strong>{producto.nombre}</strong>
                </div>
                <div className="summary-row">
                  <span>Cantidad:</span>
                  <strong>{cantidad}</strong>
                </div>
                <div className="summary-row">
                  <span>Total:</span>
                  <strong style={{ color: 'var(--primary)', fontSize: '1.2rem' }}>
                    ${montoTotal} USD
                  </strong>
                </div>
              </div>

              <p style={{ textAlign: 'center', color: 'var(--text-muted)', margin: '1rem 0 0.5rem' }}>
                <i className="ri-shield-check-line"></i> Pago seguro procesado por PayPal
              </p>

              <PayPalCheckout
                monto={montoTotal}
                moneda="USD"
                descripcion={`${cantidad}x ${producto.nombre} (${producto.sku})`}
                skuProducto={producto.sku}
                cantidad={parseInt(cantidad, 10)}
                onSuccess={handlePayPalSuccess}
                onError={handlePayPalError}
              />

              {error && (
                <div className="alert alert-error" style={{ marginTop: '1rem' }}>
                  <i className="ri-error-warning-line"></i> {error}
                </div>
              )}
            </>
          )}
        </div>

        {/* Footer */}
        <div className="modal-footer">
          {step === 'resumen' ? (
            <>
              <button className="btn-secondary" onClick={onClose}>Cancelar</button>
              <button className="btn-primary" onClick={handleContinuarPago}>
                <i className="ri-paypal-line"></i> Continuar al Pago
              </button>
            </>
          ) : (
            <button className="btn-secondary" onClick={() => { setStep('resumen'); setError(''); }}>
              <i className="ri-arrow-left-line"></i> Volver
            </button>
          )}
        </div>

      </div>
    </div>
  );
};

export default OrderModal;
