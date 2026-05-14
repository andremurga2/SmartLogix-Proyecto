import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
                // Guardar datos en localStorage (incluye JWT)
                localStorage.setItem('user', JSON.stringify({
                    username: data.username,
                    role: data.role,
                }));
                localStorage.setItem('isAuthenticated', 'true');
                if (data.token) {
                    localStorage.setItem('token', data.token);
                }

                // Redirigir según el rol
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

    const handleDemoLogin = (role) => {
        const demoUser = role === 'admin' ? 'admin' : 'user1';
        const demoPassword = role === 'admin' ? 'admin123' : 'password123';
        setUsername(demoUser);
        setPassword(demoPassword);
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

                <div className="demo-section">
                    <p>Prueba con:</p>
                    <button
                        type="button"
                        className="btn-demo admin"
                        onClick={() => handleDemoLogin('admin')}
                    >
                        Admin (admin / admin123)
                    </button>
                    <button
                        type="button"
                        className="btn-demo user"
                        onClick={() => handleDemoLogin('user')}
                    >
                        Usuario (user1 / password123)
                    </button>
                </div>
            </div>
        </div>
    );
};

export default Login;
