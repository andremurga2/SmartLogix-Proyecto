import { api } from './api';

// Mockeamos fetch globalmente para no hacer llamadas reales
global.fetch = jest.fn();

beforeEach(() => {
  fetch.mockClear();
  localStorage.clear();
});

describe('api.login', () => {
  test('llama al endpoint correcto con las credenciales', async () => {
    // Arrange
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ success: true, token: 'jwt-abc' }),
    });

    // Act
    const result = await api.login({ username: 'admin', password: '1234' });

    // Assert
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/auth/login'),
      expect.objectContaining({ method: 'POST' })
    );
    expect(result.token).toBe('jwt-abc');
  });

  test('lanza error cuando el servidor responde 401', async () => {
    // Arrange
    fetch.mockResolvedValueOnce({
      ok: false,
      status: 401,
      json: async () => ({ message: 'Credenciales inválidas' }),
    });

    // Act & Assert
    await expect(api.login({ username: 'x', password: 'x' }))
      .rejects
      .toThrow('Credenciales inválidas');
  });
});

describe('api — header de autorización', () => {
  test('incluye el token JWT cuando está guardado en localStorage', async () => {
    // Arrange
    localStorage.setItem('token', 'mi-jwt-token');
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ([]),
    });

    // Act
    await api.getCatalogo();

    // Assert
    const [, config] = fetch.mock.calls[0];
    expect(config.headers['Authorization']).toBe('Bearer mi-jwt-token');
  });

  test('NO incluye Authorization cuando no hay token guardado', async () => {
    // localStorage.clear() ya fue llamado en beforeEach
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ([]),
    });

    await api.getCatalogo();

    const [, config] = fetch.mock.calls[0];
    expect(config.headers['Authorization']).toBeUndefined();
  });
});

describe('api.realizarCompra', () => {
  test('envía el pedido como POST con body JSON', async () => {
    // Arrange
    const pedido = { items: [{ skuProducto: 'SKU-1', cantidad: 1 }] };
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ estado: 'COMPLETADO' }),
    });

    // Act
    const result = await api.realizarCompra(pedido);

    // Assert
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/store/comprar'),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(pedido),
      })
    );
    expect(result.estado).toBe('COMPLETADO');
  });
});