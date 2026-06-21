import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { api } from '../services/api';
import '../styles/Login.css';

const Register = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleRegister = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');

        if (password !== confirmPassword) {
            setError('Las contraseñas no coinciden.');
            return;
        }
        if (password.length < 6) {
            setError('La contraseña debe tener al menos 6 caracteres.');
            return;
        }

        setLoading(true);
        try {
            await api.registrar({ username, password });
            setSuccess('Cuenta creada correctamente. Redirigiendo al login...');
            setTimeout(() => navigate('/login'), 1800);
        } catch (err) {
            setError(err.message || 'No se pudo crear la cuenta.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <h1>SmartLogix</h1>
                <h2>Crear Cuenta</h2>

                {error && <div className="error-message">{error}</div>}
                {success && (
                    <div className="error-message" style={{ background: '#efe', color: '#2a7', borderLeftColor: '#2a7' }}>
                        {success}
                    </div>
                )}

                <form onSubmit={handleRegister}>
                    <div className="form-group">
                        <label htmlFor="username">Usuario</label>
                        <input
                            type="text"
                            id="username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            placeholder="Elige un nombre de usuario"
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
                            placeholder="Mínimo 6 caracteres"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="confirmPassword">Confirmar contraseña</label>
                        <input
                            type="password"
                            id="confirmPassword"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            placeholder="Repite la contraseña"
                            required
                        />
                    </div>

                    <button type="submit" className="btn-login" disabled={loading}>
                        {loading ? 'Creando cuenta...' : 'Crear Cuenta'}
                    </button>
                </form>

                <div className="demo-section">
                    <p>¿Ya tienes cuenta?</p>
                    <Link to="/login" className="btn-demo user" style={{ display: 'block', textAlign: 'center', textDecoration: 'none' }}>
                        Iniciar Sesión
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default Register;