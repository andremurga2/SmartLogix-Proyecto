import React, { useState } from 'react';
import './OrderModal.css';
import PayPalCheckout from './PayPalCheckout';
import { useCart } from '../context/CartContext';

/**
 * CartModal — Flujo de compra con carrito multi-producto + PayPal.
 *
 * Pasos:
 *   1. El usuario revisa/ajusta cantidades de todos los productos del carrito.
 *   2. PayPalCheckout crea UNA orden en el BFF con el desglose completo.
 *   3. Al aprobarse y capturarse el pago, se registra UN pedido con N items.
 */
const CartModal = ({ onClose, onPurchaseSuccess }) => {
  const { items, actualizarCantidad, quitarItem, vaciarCarrito, total } = useCart();
  const [error, setError] = useState('');
  const [step, setStep] = useState('resumen'); // 'resumen' | 'pago'

  const handleContinuarPago = () => {
    if (items.length === 0) {
      setError('Tu carrito está vacío.');
      return;
    }
    setError('');
    setStep('pago');
  };

  const handlePayPalSuccess = (pagoResponse) => {
    vaciarCarrito();
    onPurchaseSuccess({
      ...pagoResponse.pedido,
      mensaje: pagoResponse.mensaje || '¡Pago con PayPal completado exitosamente!',
      paypalOrderId: pagoResponse.paypalOrderId,
    });
  };

  const handlePayPalError = (msg) => {
    setError(msg);
    setStep('resumen');
  };

  const itemsParaApi = items.map(i => ({ skuProducto: i.sku, cantidad: i.cantidad }));

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content glass-panel" onClick={e => e.stopPropagation()}>

        <div className="modal-header">
          <h2>{step === 'resumen' ? 'Tu Carrito' : 'Pagar con PayPal'}</h2>
          <button className="btn-close" onClick={onClose}>
            <i className="ri-close-line"></i>
          </button>
        </div>

        <div className="modal-body">
          {/* ── PASO 1: Resumen del carrito ── */}
          {step === 'resumen' && (
            <>
              {items.length === 0 ? (
                <p style={{ textAlign: 'center', padding: '2rem 0', color: 'var(--text-muted)' }}>
                  Tu carrito está vacío.
                </p>
              ) : (
                items.map(item => (
                  <div key={item.sku} className="product-summary" style={{ marginBottom: '0.75rem' }}>
                    <div className="summary-row">
                      <span>{item.nombre}</span>
                      <strong>${(item.precio * item.cantidad).toLocaleString()}</strong>
                    </div>
                    <div className="summary-row" style={{ alignItems: 'center' }}>
                      <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                        ${item.precio.toLocaleString()} c/u — SKU: {item.sku}
                      </span>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <input
                          type="number"
                          className="input-glass"
                          value={item.cantidad}
                          onChange={e => actualizarCantidad(item.sku, parseInt(e.target.value, 10) || 1)}
                          min="1"
                          max={item.stockActual}
                          style={{ width: '60px' }}
                        />
                        <button className="btn-secondary" onClick={() => quitarItem(item.sku)} style={{ padding: '4px 10px' }}>
                          <i className="ri-delete-bin-line"></i>
                        </button>
                      </div>
                    </div>
                  </div>
                ))
              )}

              {items.length > 0 && (
                <div className="total-price">
                  <span>Total a pagar:</span>
                  <h3>${total.toLocaleString()} USD</h3>
                </div>
              )}

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
              {items.map(item => (
                <div key={item.sku} className="summary-row">
                  <span>{item.cantidad}x {item.nombre}</span>
                  <strong>${(item.precio * item.cantidad).toLocaleString()}</strong>
                </div>
              ))}
              <div className="summary-row" style={{ marginTop: '0.5rem', borderTop: '1px solid rgba(255,255,255,0.1)', paddingTop: '0.5rem' }}>
                <span>Total:</span>
                <strong style={{ color: 'var(--primary)', fontSize: '1.2rem' }}>
                  ${total.toLocaleString()} USD
                </strong>
              </div>

              <p style={{ textAlign: 'center', color: 'var(--text-muted)', margin: '1rem 0 0.5rem' }}>
                <i className="ri-shield-check-line"></i> Pago seguro procesado por PayPal
              </p>

              <PayPalCheckout
                items={itemsParaApi}
                moneda="USD"
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

        <div className="modal-footer">
          {step === 'resumen' ? (
            <>
              <button className="btn-secondary" onClick={onClose}>Seguir Comprando</button>
              <button className="btn-primary" onClick={handleContinuarPago} disabled={items.length === 0}>
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

export default CartModal;