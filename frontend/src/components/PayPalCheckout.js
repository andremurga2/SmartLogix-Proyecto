import React, { useEffect, useRef } from 'react';

/**
 * PayPalCheckout
 * Renderiza el botón oficial de PayPal JS SDK v2 (Smart Buttons).
 * Props:
 *  - items      : [{ skuProducto, cantidad }]
 *  - moneda     : string  e.g. "USD"
 *  - onSuccess  : (pagoResponse) => void
 *  - onError    : (msg) => void
 */
const PayPalCheckout = ({ items, moneda = 'USD', onSuccess, onError }) => {
  const paypalRef = useRef(null);
  const rendered = useRef(false);
  const API_BASE = process.env.REACT_APP_BFF_URL || 'http://localhost:9090/api';

  useEffect(() => {
    if (rendered.current) return;
    rendered.current = true;

    if (!window.paypal) {
      onError('El SDK de PayPal no está cargado. Verifica el Client ID en index.html.');
      return;
    }

    window.paypal
      .Buttons({
        style: {
          layout: 'vertical',
          color: 'gold',
          shape: 'rect',
          label: 'paypal',
          height: 45,
        },

        // ── PASO 1: crear orden en el backend con el carrito completo ──
        createOrder: async () => {
          try {
            const res = await fetch(`${API_BASE}/pagos/crear-orden`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ items, moneda }),
            });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            if (!data.orderId) throw new Error('No se recibió orderId del backend');
            return data.orderId;
          } catch (err) {
            onError('Error al crear la orden PayPal: ' + err.message);
            throw err;
          }
        },

        // ── PASO 2: el usuario aprueba → capturar pago ──
        onApprove: async (data) => {
          try {
            const res = await fetch(`${API_BASE}/pagos/capturar-orden`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                orderId: data.orderID,
                items,
              }),
            });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const pagoData = await res.json();

            if (pagoData.exitoso) {
              onSuccess(pagoData);
            } else {
              onError(pagoData.mensaje || 'El pago no fue completado.');
            }
          } catch (err) {
            onError('Error al capturar el pago: ' + err.message);
          }
        },

        onCancel: () => {
          onError('El pago fue cancelado por el usuario.');
        },

        onError: (err) => {
          console.error('PayPal SDK error:', err);
          onError('Error inesperado en el botón de PayPal.');
        },
      })
      .render(paypalRef.current);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return <div ref={paypalRef} style={{ marginTop: '1rem' }} />;
};

export default PayPalCheckout;