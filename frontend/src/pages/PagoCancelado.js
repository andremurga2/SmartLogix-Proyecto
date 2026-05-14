import React from 'react';
import { useNavigate } from 'react-router-dom';

const PagoCancelado = () => {
  const navigate = useNavigate();
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '80vh', gap: '1.5rem' }}>
      <i className="ri-close-circle-fill" style={{ fontSize: '5rem', color: '#ef4444' }}></i>
      <h2>Pago cancelado</h2>
      <p style={{ color: 'var(--text-muted)' }}>El pago fue cancelado. Puedes intentarlo de nuevo cuando quieras.</p>
      <button className="btn-primary" onClick={() => navigate('/catalog')}>
        <i className="ri-arrow-left-line"></i> Volver al Catálogo
      </button>
    </div>
  );
};

export default PagoCancelado;
