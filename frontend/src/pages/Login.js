import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { api } from '../services/api';
import '../styles/Login.css';

const Login = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const data = await api.login({ username, password });

            if (data.success) {
                localStorage.setItem('user', JSON.stringify({
                    username: data.username,
                    role: data.role,
                }));
                localStorage.setItem('isAuthenticated', 'true');
                if (data.token) {
                    localStorage.setItem('token', data.token);
                }

                if (data.role === 'ADMIN') {
                    navigate('/admin');
                } else {
                    navigate('/catalog');
                }
            } else {
                setError(data.message);
            }
        } catch (err) {
            setError('Error al conectar con el servidor');
            console.error('Login error:', err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <h1>SmartLogix</h1>
                <h2>Iniciar Sesión</h2>

                {error && <div className="error-message">{error}</div>}

                <form onSubmit={handleLogin}>
                    <div className="form-group">
                        <label htmlFor="username">Usuario</label>
                        <input
                            type="text"
                            id="username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            placeholder="Ingresa tu usuario"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="password">Contraseña</label>
                        <input
                            type="password"
                            id="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Ingresa tu contraseña"
                            required
                        />
                    </div>

                    <button type="submit" className="btn-login" disabled={loading}>
                        {loading ? 'Cargando...' : 'Iniciar Sesión'}
                    </button>
                </form>

                <p style={{ textAlign: 'center', margin: '15px 0', fontSize: '0.9em', color: '#666' }}>
                    ¿No tienes cuenta? <Link to="/register" style={{ color: '#667eea', fontWeight: 600, textDecoration: 'none' }}>Regístrate aquí</Link>
                </p>
            </div>
        </div>
    );
};

export default Login;