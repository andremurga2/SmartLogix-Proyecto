import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import '../styles/Admin.css';

const EMPTY_PRODUCTO = { sku: '', nombre: '', descripcion: '', precio: '', stockActual: 0, imagenUrl: '' };
const EMPTY_USUARIO = { username: '', password: '', role: 'USER', activo: true };

const Admin = () => {
    const [user, setUser] = useState(null);
    const [productos, setProductos] = useState([]);
    const [pedidos, setPedidos] = useState([]);
    const [activeTab, setActiveTab] = useState('productos');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Modal producto
    const [showModal, setShowModal] = useState(false);
    const [editando, setEditando] = useState(false);
    const [form, setForm] = useState(EMPTY_PRODUCTO);
    const [saving, setSaving] = useState(false);

    const [usuarios, setUsuarios] = useState([]);
    const [showUserModal, setShowUserModal] = useState(false);
    const [editandoUser, setEditandoUser] = useState(false);
    const [userForm, setUserForm] = useState(EMPTY_USUARIO);
    const [savingUser, setSavingUser] = useState(false);

    const navigate = useNavigate();

    useEffect(() => {
        const userData = localStorage.getItem('user');
        const isAuth = localStorage.getItem('isAuthenticated');
        if (!isAuth || !userData) { navigate('/login'); return; }
        const parsedUser = JSON.parse(userData);
        if (parsedUser.role !== 'ADMIN') { navigate('/catalog'); return; }
        setUser(parsedUser);
        fetchAll();
    }, [navigate]);

    const fetchAll = async () => {
        setLoading(true);
        setError(null);
        try {
            const [prods, peds, users] = await Promise.all([
                api.getCatalogo(),
                api.getPedidos(),
                api.getUsuarios(),
            ]);
            setProductos(prods || []);
            setPedidos(peds || []);
            setUsuarios(users || []);
        } catch (err) {
            setError('Error cargando datos: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleLogout = () => {
        localStorage.clear();
        navigate('/login');
    };

    // ── Productos ─────────────────────────────────────────────────────────────
    const abrirCrear = () => { setForm(EMPTY_PRODUCTO); setEditando(false); setShowModal(true); };

    const abrirEditar = (p) => {
        setForm({ sku: p.sku, nombre: p.nombre, descripcion: p.descripcion, precio: p.precio, stockActual: p.stockActual, imagenUrl: p.imagenUrl || '' });
        setEditando(true);
        setShowModal(true);
    };

    const handleFormChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

    const handleGuardar = async () => {
        if (!form.sku || !form.nombre || !form.precio) { alert('SKU, nombre y precio son obligatorios.'); return; }
        setSaving(true);
        try {
            const payload = { ...form, precio: parseFloat(form.precio), stockActual: parseInt(form.stockActual) || 0 };
            if (editando) {
                await api.actualizarProducto(form.sku, payload);
                alert('Producto actualizado correctamente.');
            } else {
                await api.crearProducto(payload);
                alert('Producto creado correctamente.');
            }
            setShowModal(false);
            fetchAll();
        } catch (err) {
            alert('Error: ' + err.message);
        } finally {
            setSaving(false);
        }
    };

    const handleEliminar = async (sku) => {
        if (!window.confirm(`¿Eliminar el producto ${sku}?`)) return;
        try {
            await api.eliminarProducto(sku);
            setProductos(productos.filter(p => p.sku !== sku));
            alert('Producto eliminado.');
        } catch (err) {
            alert('Error al eliminar: ' + err.message);
        }
    };
    
// ── Usuarios ──────────────────────────────────────────────────────────────
    const abrirCrearUsuario = () => { setUserForm(EMPTY_USUARIO); setEditandoUser(false); setShowUserModal(true); };

    const abrirEditarUsuario = (u) => {
        setUserForm({ id: u.id, username: u.username, password: '', role: u.role, activo: u.activo });
        setEditandoUser(true);
        setShowUserModal(true);
    };

    const handleUserFormChange = (e) => {
        const { name, value, type, checked } = e.target;
        setUserForm({ ...userForm, [name]: type === 'checkbox' ? checked : value });
    };

    const handleGuardarUsuario = async () => {
        if (!userForm.username || (!editandoUser && !userForm.password)) {
            alert('Usuario y contraseña son obligatorios.');
            return;
        }
        setSavingUser(true);
        try {
            if (editandoUser) {
                await api.actualizarUsuario(userForm.id, userForm);
                alert('Usuario actualizado correctamente.');
            } else {
                await api.crearUsuario(userForm);
                alert('Usuario creado correctamente.');
            }
            setShowUserModal(false);
            fetchAll();
        } catch (err) {
            alert('Error: ' + err.message);
        } finally {
            setSavingUser(false);
        }
    };

    const handleEliminarUsuario = async (u) => {
        if (!window.confirm(`¿Eliminar el usuario ${u.username}?`)) return;
        try {
            await api.eliminarUsuario(u.id);
            setUsuarios(usuarios.filter(x => x.id !== u.id));
            alert('Usuario eliminado.');
        } catch (err) {
            alert('Error al eliminar: ' + err.message);
        }
    };

    if (loading) return <div className="loading">⏳ Cargando panel de administración...</div>;

    return (
        <div className="admin-container">
            {/* Navbar */}
            <nav className="admin-navbar">
                <div className="navbar-brand"><h1>🔧 Panel de Administrador</h1></div>
                <div className="navbar-user">
                    <span className="user-info">👤 {user?.username}</span>
                    <button className="btn-logout" onClick={handleLogout}>Cerrar Sesión</button>
                </div>
            </nav>

            {error && <div style={{ background: '#fee', color: '#c00', padding: '10px 20px' }}>⚠️ {error}</div>}

            <div className="admin-content">
                {/* Sidebar */}
                <aside className="admin-sidebar">
                    <ul className="sidebar-menu">
                        {[['productos', '📦 Gestionar Productos'], ['pedidos', '📋 Ver Pedidos'], ['usuarios', '👥 Usuarios']].map(([key, label]) => (
                            <li key={key}>
                                <button className={`menu-item ${activeTab === key ? 'active' : ''}`} onClick={() => setActiveTab(key)}>
                                    {label}
                                </button>
                            </li>
                        ))}
                    </ul>
                </aside>

                <main className="admin-main">

                    {/* ── Tab Productos ── */}
                    {activeTab === 'productos' && (
                        <section className="tab-content">
                            <div className="section-header">
                                <h2>Gestionar Productos ({productos.length})</h2>
                                <button className="btn-primary" onClick={abrirCrear}>+ Nuevo Producto</button>
                            </div>
                            <div className="table-container">
                                <table className="admin-table">
                                    <thead>
                                        <tr><th>Imagen</th><th>SKU</th><th>Nombre</th><th>Precio</th><th>Stock</th><th>Disponible</th><th>Acciones</th></tr>
                                    </thead>
                                    <tbody>
                                        {productos.length === 0 ? (
                                            <tr><td colSpan="7" className="empty-cell">No hay productos</td></tr>
                                        ) : productos.map(p => (
                                            <tr key={p.sku}>
                                                <td>
                                                    {p.imagenUrl
                                                        ? <img src={p.imagenUrl} alt={p.nombre} style={{ width: 48, height: 48, objectFit: 'cover', borderRadius: 6 }} />
                                                        : <span style={{ fontSize: 24 }}>📦</span>
                                                    }
                                                </td>
                                                <td><code>{p.sku}</code></td>
                                                <td>{p.nombre}</td>
                                                <td>${parseFloat(p.precio).toFixed(2)}</td>
                                                <td>
                                                    <span style={{ color: p.stockActual === 0 ? '#e53e3e' : p.stockActual < 10 ? '#d69e2e' : '#38a169', fontWeight: 'bold' }}>
                                                        {p.stockActual}
                                                    </span>
                                                </td>
                                                <td>{p.disponible ? '✅' : '❌'}</td>
                                                <td>
                                                    <button className="btn-edit" onClick={() => abrirEditar(p)}>✏️ Editar</button>
                                                    <button className="btn-delete" onClick={() => handleEliminar(p.sku)} style={{ marginLeft: 6 }}>🗑️ Eliminar</button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    )}

                    {/* ── Tab Pedidos ── */}
                    {activeTab === 'pedidos' && (
                        <section className="tab-content">
                            <div className="section-header">
                                <h2>Historial de Pedidos ({pedidos.length})</h2>
                                <button className="btn-primary" onClick={fetchAll}>🔄 Actualizar</button>
                            </div>
                            <div className="table-container">
                                <table className="admin-table">
                                    <thead>
                                        <tr><th>ID</th><th>SKU Producto</th><th>Cantidad</th><th>Total</th><th>Estado</th><th>PayPal Order ID</th></tr>
                                    </thead>
                                    <tbody>
                                        {pedidos.length === 0 ? (
                                            <tr><td colSpan="6" className="empty-cell">No hay pedidos registrados</td></tr>
                                        ) : pedidos.map(ped => (
                                            <tr key={ped.id}>
                                                <td>#{ped.id}</td>
                                                <td><code>{ped.skuProducto}</code></td>
                                                <td>{ped.cantidad}</td>
                                                <td>${parseFloat(ped.precioTotal || 0).toFixed(2)}</td>
                                                <td>
                                                    <span className="estado-badge" style={{
                                                        background: ped.estado === 'COMPLETADO' ? '#c6f6d5' : ped.estado === 'FALLIDO' ? '#fed7d7' : '#fefcbf',
                                                        color: ped.estado === 'COMPLETADO' ? '#276749' : ped.estado === 'FALLIDO' ? '#9b2335' : '#744210',
                                                        padding: '2px 10px', borderRadius: 12, fontWeight: 'bold', fontSize: 12
                                                    }}>
                                                        {ped.estado}
                                                    </span>
                                                </td>
                                                <td style={{ fontSize: 11, color: '#666' }}>{ped.paypalOrderId || '—'}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    )}

                    {/* ── Tab Usuarios ── */}
                    {activeTab === 'usuarios' && (
                        <section className="tab-content">
                            <div className="section-header">
                                <h2>Usuarios del Sistema ({usuarios.length})</h2>
                                <button className="btn-primary" onClick={abrirCrearUsuario}>+ Nuevo Usuario</button>
                            </div>
                            <div className="table-container">
                                <table className="admin-table">
                                    <thead>
                                        <tr><th>Usuario</th><th>Rol</th><th>Estado</th><th>Acciones</th></tr>
                                    </thead>
                                    <tbody>
                                        {usuarios.length === 0 ? (
                                            <tr><td colSpan="4" className="empty-cell">No hay usuarios</td></tr>
                                        ) : usuarios.map(u => (
                                            <tr key={u.id}>
                                                <td>👤 {u.username}</td>
                                                <td><span className={`role-badge ${u.role.toLowerCase()}`}>{u.role}</span></td>
                                                <td>
                                                    {u.activo
                                                        ? <span className="status-active">Activo</span>
                                                        : <span style={{ color: '#999' }}>Inactivo</span>}
                                                </td>
                                                <td>
                                                    <button className="btn-edit" onClick={() => abrirEditarUsuario(u)}>✏️ Editar</button>
                                                    <button className="btn-delete" onClick={() => handleEliminarUsuario(u)} style={{ marginLeft: 6 }}>🗑️ Eliminar</button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    )}
                </main>
            </div>

            {/* ── Modal Crear/Editar Producto ── */}
            {showModal && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
                    <div style={{ background: '#fff', borderRadius: 12, padding: 32, width: 480, boxShadow: '0 20px 60px rgba(0,0,0,0.3)' }}>
                        <h2 style={{ marginBottom: 20 }}>{editando ? '✏️ Editar Producto' : '➕ Nuevo Producto'}</h2>

                        {/* Preview de imagen */}
                        {form.imagenUrl && (
                            <div style={{ marginBottom: 16, textAlign: 'center' }}>
                                <img
                                    src={form.imagenUrl}
                                    alt="Preview"
                                    style={{ width: 100, height: 100, objectFit: 'cover', borderRadius: 8, border: '1px solid #ddd' }}
                                    onError={(e) => { e.target.style.display = 'none'; }}
                                />
                            </div>
                        )}

                        {[
                            { label: 'SKU *', name: 'sku', type: 'text', disabled: editando },
                            { label: 'Nombre *', name: 'nombre', type: 'text' },
                            { label: 'Descripción', name: 'descripcion', type: 'text' },
                            { label: 'Precio (USD) *', name: 'precio', type: 'number' },
                            { label: 'Stock', name: 'stockActual', type: 'number' },
                            { label: 'URL Imagen', name: 'imagenUrl', type: 'text' },
                        ].map(({ label, name, type, disabled }) => (
                            <div key={name} style={{ marginBottom: 14 }}>
                                <label style={{ display: 'block', marginBottom: 4, fontWeight: 600, fontSize: 13 }}>{label}</label>
                                <input
                                    type={type}
                                    name={name}
                                    value={form[name]}
                                    onChange={handleFormChange}
                                    disabled={disabled}
                                    placeholder={name === 'imagenUrl' ? 'https://ejemplo.com/imagen.jpg' : ''}
                                    style={{ width: '100%', padding: '8px 12px', border: '1px solid #ddd', borderRadius: 6, fontSize: 14, boxSizing: 'border-box', background: disabled ? '#f5f5f5' : '#fff' }}
                                />
                            </div>
                        ))}
                        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 20 }}>
                            <button onClick={() => setShowModal(false)} style={{ padding: '8px 20px', border: '1px solid #ddd', borderRadius: 6, cursor: 'pointer', background: '#f5f5f5' }}>
                                Cancelar
                            </button>
                            <button onClick={handleGuardar} disabled={saving} style={{ padding: '8px 20px', background: '#4f46e5', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer', fontWeight: 600 }}>
                                {saving ? 'Guardando...' : editando ? 'Actualizar' : 'Crear'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* ── Modal Crear/Editar Usuario ── */}
            {showUserModal && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
                    <div style={{ background: '#fff', borderRadius: 12, padding: 32, width: 480, boxShadow: '0 20px 60px rgba(0,0,0,0.3)' }}>
                        <h2 style={{ marginBottom: 20 }}>{editandoUser ? '✏️ Editar Usuario' : '➕ Nuevo Usuario'}</h2>

                        <div style={{ marginBottom: 14 }}>
                            <label style={{ display: 'block', marginBottom: 4, fontWeight: 600, fontSize: 13 }}>Usuario *</label>
                            <input
                                type="text"
                                name="username"
                                value={userForm.username}
                                onChange={handleUserFormChange}
                                disabled={editandoUser}
                                style={{ width: '100%', padding: '8px 12px', border: '1px solid #ddd', borderRadius: 6, fontSize: 14, boxSizing: 'border-box', background: editandoUser ? '#f5f5f5' : '#fff' }}
                            />
                        </div>

                        <div style={{ marginBottom: 14 }}>
                            <label style={{ display: 'block', marginBottom: 4, fontWeight: 600, fontSize: 13 }}>
                                {editandoUser ? 'Nueva contraseña (dejar vacío para no cambiar)' : 'Contraseña *'}
                            </label>
                            <input
                                type="password"
                                name="password"
                                value={userForm.password}
                                onChange={handleUserFormChange}
                                style={{ width: '100%', padding: '8px 12px', border: '1px solid #ddd', borderRadius: 6, fontSize: 14, boxSizing: 'border-box' }}
                            />
                        </div>

                        <div style={{ marginBottom: 14 }}>
                            <label style={{ display: 'block', marginBottom: 4, fontWeight: 600, fontSize: 13 }}>Rol</label>
                            <select
                                name="role"
                                value={userForm.role}
                                onChange={handleUserFormChange}
                                style={{ width: '100%', padding: '8px 12px', border: '1px solid #ddd', borderRadius: 6, fontSize: 14, boxSizing: 'border-box' }}
                            >
                                <option value="USER">USER</option>
                                <option value="ADMIN">ADMIN</option>
                            </select>
                        </div>

                        {editandoUser && (
                            <div style={{ marginBottom: 14, display: 'flex', alignItems: 'center', gap: 8 }}>
                                <input
                                    type="checkbox"
                                    id="activo"
                                    name="activo"
                                    checked={userForm.activo}
                                    onChange={handleUserFormChange}
                                />
                                <label htmlFor="activo" style={{ fontWeight: 600, fontSize: 13 }}>Usuario activo</label>
                            </div>
                        )}

                        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 20 }}>
                            <button onClick={() => setShowUserModal(false)} style={{ padding: '8px 20px', border: '1px solid #ddd', borderRadius: 6, cursor: 'pointer', background: '#f5f5f5' }}>
                                Cancelar
                            </button>
                            <button onClick={handleGuardarUsuario} disabled={savingUser} style={{ padding: '8px 20px', background: '#4f46e5', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer', fontWeight: 600 }}>
                                {savingUser ? 'Guardando...' : editandoUser ? 'Actualizar' : 'Crear'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Admin;