import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Catalog from './pages/Catalog';
import Login from './pages/Login';
import Register from './pages/Register';
import Admin from './pages/Admin';
import PagoExito from './pages/PagoExito';
import PagoCancelado from './pages/PagoCancelado';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
  return (
    <Router>
      <div className="App">
        <Routes>
          {/* Pública: Login */}
          <Route path="/login" element={<Login />} />

          {/* Pública: Registro */}
          <Route path="/register" element={<Register />} />

          {/* Retorno PayPal */}
          <Route path="/pago/exito" element={<PagoExito />} />
          <Route path="/pago/cancelado" element={<PagoCancelado />} />

          {/* Protegida: Catálogo */}
          <Route
            path="/catalog"
            element={
              <ProtectedRoute>
                <CatalogLayout />
              </ProtectedRoute>
            }
          />

          {/* Protegida: Admin */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute requiredRole="ADMIN">
                <Admin />
              </ProtectedRoute>
            }
          />

          {/* Raíz → Login */}
          <Route path="/" element={<Navigate to="/login" replace />} />
        </Routes>
      </div>
    </Router>
  );
}

function CatalogLayout() {
  return (
    <>
      <nav style={{
        background: 'rgba(13, 17, 23, 0.8)',
        padding: '1rem 2rem',
        borderBottom: '1px solid rgba(255,255,255,0.08)',
        position: 'sticky',
        top: 0,
        zIndex: 100,
        backdropFilter: 'blur(10px)',
      }}>
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          maxWidth: '1200px', margin: '0 auto',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <i className="ri-box-3-fill" style={{ fontSize: '2rem', color: 'var(--primary)' }}></i>
            <h2 style={{ margin: 0, fontSize: '1.5rem', letterSpacing: '1px' }}>
              Smart<span style={{ color: 'var(--primary)' }}>Logix</span>
            </h2>
          </div>
          <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
            {JSON.parse(localStorage.getItem('user') || '{}').role === 'ADMIN' && (
              <a href="/admin" style={{ color: 'var(--primary)', textDecoration: 'none', fontWeight: 600 }}>
                Panel Admin
              </a>
            )}
            <button className="btn-secondary" onClick={() => {
              localStorage.removeItem('user');
              localStorage.removeItem('isAuthenticated');
              window.location.href = '/login';
            }}>
              Cerrar Sesión
            </button>
          </div>
        </div>
      </nav>
      <Catalog />
    </>
  );
}

export default App;
