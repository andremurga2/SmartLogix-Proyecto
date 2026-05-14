import React from 'react';
import { useNavigate } from 'react-router-dom';

const PagoExito = () => {
  const navigate = useNavigate();
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '80vh', gap: '1.5rem' }}>
      <i className="ri-checkbox-circle-fill" style={{ fontSize: '5rem', color: '#22c55e' }}></i>
      <h2>¡Pago completado exitosamente!</h2>
      <p style={{ color: 'var(--text-muted)' }}>Tu pedido ha sido registrado en SmartLogix.</p>
      <button className="btn-primary" onClick={() => navigate('/catalog')}>
        <i className="ri-arrow-left-line"></i> Volver al Catálogo
      </button>
    </div>
  );
};

export default PagoExito;
