# BFF — Backend for Frontend / API Gateway

Componente central de **SmartLogix** que actúa como única puerta de entrada para la aplicación React. Recibe todas las peticiones del cliente, valida la autenticación JWT y las delega a los microservicios internos (`ms-auth`, `ms-inventario`, `ms-pedidos`). Además integra el flujo de pago con **PayPal Sandbox**.

---

## Tabla de contenidos

1. [Tecnologías](#tecnologías)
2. [Arquitectura y rol en el sistema](#arquitectura-y-rol-en-el-sistema)
3. [Variables de entorno](#variables-de-entorno)
4. [Instalación y ejecución local](#instalación-y-ejecución-local)
5. [Ejecución con Docker](#ejecución-con-docker)
6. [Endpoints disponibles](#endpoints-disponibles)
7. [Flujo de pago PayPal](#flujo-de-pago-paypal)
8. [Documentación OpenAPI / Swagger](#documentación-openapi--swagger)
9. [Ejecución de pruebas unitarias](#ejecución-de-pruebas-unitarias)
10. [Cobertura de pruebas (JaCoCo)](#cobertura-de-pruebas-jacoco)

---

## Tecnologías

| Herramienta | Versión |
|---|---|
| Java | 17 (Eclipse Temurin) |
| Spring Boot | 3.x |
| Spring Cloud OpenFeign | — |
| springdoc-openapi (Swagger UI) | — |
| PayPal REST SDK | — |
| Lombok | — |
| Maven | 3.9.6 |
| JUnit 5 + Mockito | — |
| JaCoCo | — |

---

## Arquitectura y rol en el sistema

```
React Frontend (puerto 3000)
        │
        ▼
    BFF / API Gateway  (puerto 9090)   ← este microservicio
        │
        ├─── ms-auth        (puerto 8083)  validación JWT y gestión de usuarios
        ├─── ms-inventario  (puerto 8081)  catálogo y stock de productos
        ├─── ms-pedidos     (puerto 8082)  registro y consulta de pedidos
        └─── PayPal API     (sandbox)      creación y captura de órdenes de pago
```

El BFF implementa el patrón **Backend for Frontend**: el frontend nunca llama directamente a los microservicios internos. Toda la lógica de orquestación, validación de tokens y composición de respuestas vive aquí.

---

## Variables de entorno

Crea un archivo `.env` en la raíz del módulo (o configura las variables en Docker Compose):

```env
# URLs de microservicios internos
AUTH_SERVICE_URL=http://localhost:8083
INVENTARIO_SERVICE_URL=http://localhost:8081
PEDIDOS_SERVICE_URL=http://localhost:8082

# Credenciales PayPal Sandbox
PAYPAL_CLIENT_ID=tu_client_id_sandbox
PAYPAL_CLIENT_SECRET=tu_client_secret_sandbox
PAYPAL_MODE=sandbox
```

---

## Instalación y ejecución local

### Prerrequisitos

- Java 17+
- Maven 3.9+
- Los microservicios `ms-auth`, `ms-inventario` y `ms-pedidos` deben estar corriendo

### Pasos

```bash
# 1. Clonar el repositorio y entrar al módulo
cd backend/bff

# 2. Compilar el proyecto
mvn clean package -DskipTests

# 3. Ejecutar
mvn spring-boot:run
```

El BFF quedará disponible en `http://localhost:9090`.

---

## Ejecución con Docker

Desde la raíz del proyecto, levantar todo el stack con Docker Compose:

```bash
docker compose up --build
```

El BFF se expone en el puerto `9090` según la configuración del `docker-compose.yml` raíz.

---

## Endpoints disponibles

### Autenticación (públicos)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/auth/login` | Inicia sesión y retorna JWT |
| `GET` | `/api/auth/validate` | Valida un token JWT |
| `POST` | `/api/auth/registro` | Registro público de nuevo usuario (rol USER) |

**Ejemplo — login:**
```json
// POST /api/auth/login
// Body:
{ "username": "admin", "password": "admin123" }

// Respuesta 200:
{ "success": true, "token": "eyJhbGci...", "username": "admin", "role": "ADMIN" }
```

---

### Tienda (público / requiere JWT)

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| `GET` | `/api/store/catalogo` | No | Lista todos los productos disponibles |
| `POST` | `/api/store/comprar` | JWT | Registra un pedido con los ítems del carrito |

**Ejemplo — comprar:**
```json
// POST /api/store/comprar
// Header: Authorization: Bearer <token>
// Body:
{
  "items": [
    { "skuProducto": "SKU-1001", "cantidad": 2 },
    { "skuProducto": "SKU-1003", "cantidad": 1 }
  ]
}
```

---

### Pagos PayPal

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/pagos/crear-orden` | Paso 1 — crea la orden en PayPal y retorna la URL de aprobación |
| `POST` | `/api/pagos/capturar-orden` | Paso 2 — captura el pago aprobado y registra el pedido |
| `POST` | `/api/pagos/webhook` | Recibe notificaciones de eventos de PayPal |

**Ejemplo — crear orden:**
```json
// POST /api/pagos/crear-orden
// Body:
{
  "moneda": "USD",
  "items": [
    { "skuProducto": "SKU-1001", "cantidad": 1, "precioUnitario": 999.00 }
  ]
}

// Respuesta 200:
{ "orderId": "8GB67...", "approveUrl": "https://www.sandbox.paypal.com/...", "status": "CREATED" }
```

---

### Administración (requiere JWT con rol ADMIN)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/admin/usuarios` | Lista todos los usuarios |
| `POST` | `/api/admin/usuarios` | Crea un nuevo usuario |
| `PUT` | `/api/admin/usuarios/{id}` | Actualiza un usuario existente |
| `DELETE` | `/api/admin/usuarios/{id}` | Elimina un usuario |
| `POST` | `/api/admin/productos` | Crea un nuevo producto en inventario |
| `PUT` | `/api/admin/productos/{sku}` | Actualiza un producto por SKU |
| `DELETE` | `/api/admin/productos/{sku}` | Elimina un producto por SKU |
| `GET` | `/api/admin/pedidos` | Lista todos los pedidos registrados |

> Todos los endpoints `/admin/**` requieren el header `Authorization: Bearer <token>` con un token de rol `ADMIN`. Retornan `403 Forbidden` si el rol no coincide.

---

## Flujo de pago PayPal

```
Frontend                BFF                     PayPal Sandbox
   │                     │                            │
   │─ POST /crear-orden ─▶│                            │
   │                     │─── createOrder() ──────────▶│
   │                     │◀── orderId + approveUrl ────│
   │◀── approveUrl ───────│                            │
   │                     │                            │
   │  (usuario aprueba en PayPal)                      │
   │                     │                            │
   │─ POST /capturar-orden▶│                            │
   │                     │─── captureOrder() ─────────▶│
   │                     │◀── COMPLETED ───────────────│
   │                     │─── realizarCompra() ───▶ ms-pedidos
   │◀── PagoResponse ─────│
```

---

## Documentación OpenAPI / Swagger

Con el BFF corriendo, la documentación interactiva está disponible en:

- **Swagger UI:** `http://localhost:9090/swagger-ui.html`
- **JSON OpenAPI:** `http://localhost:9090/v3/api-docs`

Para exportar la especificación como archivo estático:

```bash
curl http://localhost:9090/v3/api-docs -o SmartLogix-BFF-openapi.json
```

---

## Ejecución de pruebas unitarias

```bash
mvn clean test
```

Las suites de prueba cubren:

| Suite | Tests | Descripción |
|---|---|---|
| `BffControllerTest` | 24 | Endpoints de catálogo, compra, auth y admin |
| `PayPalControllerTest` | 6 | Flujo crear orden, capturar orden y webhook |
| `BffServiceTest` | 5 | Lógica de orquestación y delegación |
| **Total** | **35** | **0 fallos, 0 errores** |

---

## Cobertura de pruebas (JaCoCo)

| Métrica | Resultado |
|---|---|
| Instrucciones | **82%** |
| Ramas | **75%** |
| Métodos | **35 cubiertos** |

Para generar el reporte HTML:

```bash
mvn clean test jacoco:report
```

El reporte queda en `target/site/jacoco/index.html`.