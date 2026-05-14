# 💳 Integración PayPal — SmartLogix

> Guía completa para configurar, levantar y probar la pasarela de pagos PayPal
> integrada al ecosistema de microservicios SmartLogix.

---

## 🏗️ Qué se integró

| Capa | Archivo / Servicio | Cambio |
|---|---|---|
| **BFF** | `PayPalConfig.java` | Bean `PayPalHttpClient` configurado con Client ID y Secret |
| **BFF** | `PayPalService.java` | Lógica para crear y capturar órdenes vía PayPal Orders v2 API |
| **BFF** | `PayPalController.java` | Endpoints: `POST /api/pagos/crear-orden`, `POST /api/pagos/capturar-orden`, `POST /api/pagos/webhook` |
| **BFF** | `pom.xml` | Dependencia `com.paypal.sdk:checkout-sdk:1.0.5` |
| **BFF** | `application.yml` | Variables `paypal.client-id`, `paypal.client-secret`, `paypal.mode`, `paypal.return-url`, `paypal.cancel-url` |
| **ms-pedidos** | `Pedido.java` | Nuevo campo `paypal_order_id` en la tabla `pedidos` |
| **ms-pedidos** | `PedidoDTO.java` | Nuevo campo `paypalOrderId` en el DTO |
| **ms-pedidos** | `PedidoServiceImpl.java` | Persiste el `paypalOrderId` junto al pedido |
| **Frontend** | `PayPalCheckout.js` | Componente con botones oficiales PayPal JS SDK v2 |
| **Frontend** | `OrderModal.js` | Flujo 2 pasos: resumen → pago PayPal |
| **Frontend** | `index.html` | `<script>` del PayPal JS SDK |
| **Frontend** | `App.js` | Rutas `/pago/exito` y `/pago/cancelado` |
| **Raíz** | `docker-compose.yml` | Variables de entorno PayPal en el servicio `bff` |

---

## 📋 Pre-requisitos

| Herramienta | Versión mínima | Notas |
|---|---|---|
| Docker | 24+ | `docker --version` |
| Docker Compose | V2 (plugin) | `docker compose version` |
| ngrok | Cualquiera | Solo para webhook en desarrollo |
| Cuenta PayPal Developer | — | [developer.paypal.com](https://developer.paypal.com) |

---

## 🔑 Paso 1 — Obtener credenciales PayPal Sandbox

1. Ir a [developer.paypal.com](https://developer.paypal.com) → **Dashboard**.
2. En **My Apps & Credentials** → **Sandbox** → **Create App**.
3. Nombrar la app (ej: `SmartLogix-Dev`) y crear.
4. Copiar **Client ID** y **Secret Key**.

> 💡 Para probar pagos, PayPal provee cuentas de sandbox en **Sandbox > Accounts**.
> Usa el *Personal account* (comprador) para aprobar el pago en el popup.

---

## 🔧 Paso 2 — Configurar credenciales

### Opción A — archivo `.env` (recomendado para docker-compose)

Crea un archivo `.env` en la **raíz del proyecto** (junto a `docker-compose.yml`):

```bash
# Copiar el ejemplo y editar
cp .env.paypal.example .env
```

Editar `.env`:
```env
PAYPAL_CLIENT_ID=AaBbCc...tuClientIdSandbox
PAYPAL_CLIENT_SECRET=EeFfGg...tuClientSecretSandbox
PAYPAL_MODE=sandbox
PAYPAL_RETURN_URL=http://localhost:3000/pago/exito
PAYPAL_CANCEL_URL=http://localhost:3000/pago/cancelado
```

### Opción B — editar directamente `application.yml` del BFF

Abrir `backend/bff/src/main/resources/application.yml` y reemplazar:
```yaml
paypal:
  client-id: TU_CLIENT_ID_AQUI
  client-secret: TU_CLIENT_SECRET_AQUI
```

### Frontend — registrar Client ID en `index.html`

Abrir `frontend/public/index.html` y reemplazar en la línea del script:
```html
src="https://www.paypal.com/sdk/js?client-id=TU_CLIENT_ID_SANDBOX&currency=USD&intent=capture"
```

> ⚠️ El `client-id` del SDK JS **debe coincidir** con el `PAYPAL_CLIENT_ID` del backend.

---

## 🚀 Paso 3 — Levantar el proyecto

```bash
# Desde la raíz del proyecto
docker compose up --build
```

Esperar hasta que todos los servicios estén `healthy`. Verificar con:
```bash
docker compose ps
```

| Servicio | URL | Estado esperado |
|---|---|---|
| Frontend | http://localhost:3000 | `running` |
| BFF | http://localhost:9090/api | `running` |
| MS Inventario | http://localhost:8081 | `running` |
| MS Pedidos | http://localhost:8082 | `running` |
| DB Inventario | puerto 5432 | `healthy` |
| DB Pedidos | puerto 5433 | `healthy` |

---

## 🌐 Paso 4 — Configurar webhook con ngrok (opcional, solo para webhook)

> Los pasos 1-3 ya son suficientes para probar el flujo completo de pago.
> El webhook es opcional y útil para recibir notificaciones asíncronas de PayPal.

```bash
# Terminal separada
ngrok http 9090
```

Copiar la URL HTTPS generada (ej: `https://abc123.ngrok.io`) y registrarla en PayPal:

1. **developer.paypal.com** → **My Apps** → seleccionar tu app → **Webhooks**.
2. **Add Webhook** → URL: `https://abc123.ngrok.io/api/pagos/webhook`
3. Eventos a suscribir:
   - `PAYMENT.CAPTURE.COMPLETED`
   - `PAYMENT.CAPTURE.DENIED`
   - `CHECKOUT.ORDER.APPROVED`

> ⚠️ La URL de ngrok cambia en cada reinicio (plan gratuito). Repetir este paso cada vez.

---

## 🔄 Flujo de Pago — Paso a Paso

```
Usuario → Catálogo → Selecciona producto
    ↓
OrderModal (Paso 1): selecciona cantidad → ve el total
    ↓
"Continuar al Pago" → OrderModal (Paso 2): botón PayPal
    ↓
PayPalCheckout llama a: POST /api/pagos/crear-orden
    ↓ (BFF crea orden en PayPal API)
PayPal devuelve orderId + approveUrl
    ↓
El usuario aprueba el pago en el popup PayPal (cuenta sandbox)
    ↓
PayPalCheckout llama a: POST /api/pagos/capturar-orden
    ↓ (BFF captura el pago en PayPal → si COMPLETED:)
BFF llama a ms-pedidos: POST /api/pedidos (con paypalOrderId)
    ↓ (ms-pedidos valida stock con ms-inventario, descuenta, guarda pedido)
Frontend muestra mensaje de éxito ✅
```

---

## 🧪 Probar el Flujo Completo

1. Abrir http://localhost:3000 y hacer login.
2. Ir al catálogo y hacer clic en **Comprar** en cualquier producto.
3. Seleccionar cantidad → **Continuar al Pago**.
4. El botón naranja de PayPal aparece. Hacer clic.
5. En el popup de PayPal, iniciar sesión con la **cuenta sandbox Personal** (comprador).
6. Aprobar el pago.
7. Verificar el mensaje de éxito en la app.

### Verificar en la base de datos

```bash
# Conectar a db-pedidos
docker exec -it smartlogix-db-pedidos psql -U admin -d db_pedidos

# Ver pedidos con paypal_order_id
SELECT id, sku_producto, cantidad, precio_total, estado, paypal_order_id
FROM pedidos
ORDER BY id DESC
LIMIT 5;

\q
```

### Verificar en PayPal Sandbox

- Ir a [sandbox.paypal.com](https://sandbox.paypal.com) → iniciar sesión con la cuenta **Business** (vendedor).
- En **Activity** aparecerán las transacciones completadas.

---

## 🔌 Endpoints PayPal del BFF

### `POST /api/pagos/crear-orden`

**Request:**
```json
{
  "skuProducto": "SKU-001",
  "cantidad": 2,
  "monto": "59.98",
  "moneda": "USD",
  "descripcion": "2x Laptop SmartLogix Pro (SKU-001)"
}
```

**Response:**
```json
{
  "orderId": "4PL78394HT123456X",
  "approveUrl": "https://www.sandbox.paypal.com/checkoutnow?token=4PL783...",
  "status": "CREATED"
}
```

---

### `POST /api/pagos/capturar-orden`

**Request:**
```json
{
  "orderId": "4PL78394HT123456X",
  "skuProducto": "SKU-001",
  "cantidad": 2
}
```

**Response (éxito):**
```json
{
  "exitoso": true,
  "mensaje": "Pago procesado y pedido registrado exitosamente.",
  "paypalOrderId": "4PL78394HT123456X",
  "estadoPago": "COMPLETED",
  "pedido": {
    "id": 7,
    "skuProducto": "SKU-001",
    "cantidad": 2,
    "precioTotal": 59.98,
    "estado": "COMPLETADO",
    "paypalOrderId": "4PL78394HT123456X"
  }
}
```

---

### `POST /api/pagos/webhook`

Recibe notificaciones asíncronas de PayPal (requiere URL pública HTTPS).
Retorna `200 OK` siempre.

---

## 🛠️ Solución de Problemas

| Problema | Causa probable | Solución |
|---|---|---|
| Botón PayPal no aparece | Client ID incorrecto en `index.html` | Verificar que `client-id` en el script del SDK coincida con el del `.env` |
| Error 401 al crear orden | Client ID / Secret inválidos | Regenerar credenciales en developer.paypal.com |
| `PayPal SDK no está cargado` | El script no cargó (bloqueado, sin internet) | Revisar consola del navegador para errores de red |
| Webhook no recibe eventos | URL localhost no válida para PayPal | Usar ngrok y registrar la URL HTTPS en el panel PayPal |
| `Stock insuficiente` | El inventario no tiene unidades | Usar el Panel Admin para aumentar el stock del producto |
| Pedido queda en `FALLIDO` | ms-inventario no responde (Circuit Breaker) | Verificar `docker compose ps` y que todos los servicios estén `running` |

---

## 🔒 Seguridad — Checklist antes de Producción

- [ ] Cambiar `PAYPAL_MODE` de `sandbox` a `live`
- [ ] Reemplazar credenciales sandbox por credenciales **live** de PayPal
- [ ] Actualizar `PAYPAL_RETURN_URL` y `PAYPAL_CANCEL_URL` con el dominio real HTTPS
- [ ] Actualizar el `client-id` en `index.html` por el live
- [ ] Verificar firma del webhook (`Paypal-Transmission-Sig`) en `PayPalController`
- [ ] Nunca commitear `.env` al repositorio (está en `.gitignore`)
- [ ] Activar HTTPS en el servidor de producción (TLS/SSL)

---

## 📁 Archivos nuevos / modificados

```
SmartLogix-PayPal/
├── .env.paypal.example                          ← NUEVO: plantilla de variables PayPal
├── docker-compose.yml                           ← MODIFICADO: vars PayPal en servicio bff
├── PAYPAL_SETUP.md                              ← NUEVO: esta guía
├── backend/
│   ├── bff/
│   │   ├── pom.xml                              ← MODIFICADO: dependencia PayPal SDK
│   │   └── src/main/
│   │       ├── java/com/smartlogix/bff/
│   │       │   ├── config/PayPalConfig.java     ← NUEVO
│   │       │   ├── service/PayPalService.java   ← NUEVO
│   │       │   ├── controller/PayPalController.java ← NUEVO
│   │       │   └── model/
│   │       │       ├── CrearOrdenRequest.java   ← NUEVO
│   │       │       ├── CrearOrdenResponse.java  ← NUEVO
│   │       │       ├── CapturarOrdenRequest.java ← NUEVO
│   │       │       ├── PagoResponse.java        ← NUEVO
│   │       │       └── PedidoDTO.java           ← MODIFICADO: +paypalOrderId
│   │       └── resources/application.yml       ← MODIFICADO: bloque paypal
│   └── ms-pedidos/
│       └── src/main/java/com/smartlogix/pedidos/
│           ├── model/entity/Pedido.java         ← MODIFICADO: +paypal_order_id
│           ├── model/dto/PedidoDTO.java         ← MODIFICADO: +paypalOrderId
│           ├── factory/PedidoFactory.java       ← MODIFICADO: mapea paypalOrderId
│           └── service/impl/PedidoServiceImpl.java ← MODIFICADO: persiste paypalOrderId
└── frontend/
    ├── public/index.html                        ← MODIFICADO: script PayPal JS SDK
    └── src/
        ├── App.js                               ← MODIFICADO: rutas /pago/exito y /cancelado
        ├── components/
        │   ├── PayPalCheckout.js                ← NUEVO
        │   └── OrderModal.js                    ← MODIFICADO: flujo 2 pasos con PayPal
        └── pages/
            ├── PagoExito.js                     ← NUEVO
            └── PagoCancelado.js                 ← NUEVO
```
