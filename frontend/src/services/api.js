// SmartLogix — Capa de servicios HTTP
const API_BASE_URL = process.env.REACT_APP_BFF_URL || 'http://localhost:9090/api';

function authHeader() {
  const token = localStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export const api = {
  async request(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const config = {
      headers: {
        'Content-Type': 'application/json',
        ...authHeader(),
        ...options.headers,
      },
      ...options,
    };
    try {
      const response = await fetch(url, config);
      if (response.status === 204) return null;
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
      }
      return await response.json();
    } catch (error) {
      console.error(`Error en ${endpoint}:`, error);
      throw error;
    }
  },

  // ── Auth ──────────────────────────────────────────────────────────────────
  async login(credentials) {
    return this.request('/auth/login', { method: 'POST', body: JSON.stringify(credentials) });
  },

  // ── Store / Catálogo ──────────────────────────────────────────────────────
  async getCatalogo() {
    return this.request('/store/catalogo');
  },

  async realizarCompra(pedido) {
    return this.request('/store/comprar', { method: 'POST', body: JSON.stringify(pedido) });
  },

  // ── Admin: Pedidos ────────────────────────────────────────────────────────
  async getPedidos() {
    return this.request('/admin/pedidos');
  },

  // ── Admin: Productos ──────────────────────────────────────────────────────
  async crearProducto(producto) {
    return this.request('/admin/productos', { method: 'POST', body: JSON.stringify(producto) });
  },

  async actualizarProducto(sku, producto) {
    return this.request(`/admin/productos/${sku}`, { method: 'PUT', body: JSON.stringify(producto) });
  },

  async eliminarProducto(sku) {
    return this.request(`/admin/productos/${sku}`, { method: 'DELETE' });
  },

  // ── Admin: Usuarios ───────────────────────────────────────────────────────
  async getUsuarios() {
    return this.request('/admin/usuarios');
  },

  async crearUsuario(usuario) {
    return this.request('/admin/usuarios', { method: 'POST', body: JSON.stringify(usuario) });
  },

  async actualizarUsuario(id, usuario) {
    return this.request(`/admin/usuarios/${id}`, { method: 'PUT', body: JSON.stringify(usuario) });
  },

  async eliminarUsuario(id) {
    return this.request(`/admin/usuarios/${id}`, { method: 'DELETE' });
  },
};

export default api;
