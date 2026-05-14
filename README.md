# 📦 SmartLogix — E-Commerce con Microservicios

> **Duoc UC · Desarrollo FullStack III · Mayo 2026**  
> Arquitectura híbrida: Spring Boot 3 + React 18 + PostgreSQL 15 + RabbitMQ + Docker

---

## 📋 Tabla de Contenidos

1. [Arquitectura del Sistema](#arquitectura)
2. [Patrones Implementados](#patrones)
3. [Estructura del Proyecto](#estructura)
4. [Requisitos Previos](#requisitos)
5. [Configuración de Variables de Entorno](#variables)
6. [Cómo Correr el Proyecto](#correr)
7. [Usuarios de Prueba](#usuarios)
8. [URLs y Puertos](#urls)
9. [Comandos Útiles](#comandos)
10. [Solución de Problemas](#problemas)

---

## 🏗️ Arquitectura del Sistema <a name="arquitectura"></a>

```
┌──────────────────────────────────────────────────────────────────────┐
│                     CLIENTE (Navegador)                              │
│               http://localhost:3000  (React SPA)                     │
│    Carrito gestionado en estado React (sin backend, latencia cero)   │
└─────────────────────────┬────────────────────────────────────────────┘
                          │ HTTP REST + JWT (Bearer Token)
                          ▼
┌──────────────────────────────────────────────────────────────────────┐
│           BFF / API Gateway  :9090  (Spring Boot)                    │
│  • Enruta requests a los microservicios internos                     │
│  • Valida JWT consultando ms-auth antes de rutas protegidas          │
│  • Integra PayPal (crear orden / capturar / webhook)                 │
└───────────┬─────────────────┬──────────────────┬────────────────────┘
            │ Feign           │ Feign            │ Feign
            ▼                 ▼                  ▼
┌───────────────────┐  ┌──────────────────┐  ┌──────────────────────┐
│  ms-auth  :8083   │  │ ms-inventario    │  │  ms-pedidos  :8082   │
│  Spring Boot      │  │ :8081            │  │  Spring Boot +       │
│  • Valida usuario │  │  Spring Boot     │  │  Resilience4j        │
│  • Emite JWT      │  │  • CRUD productos│  │  • Crear pedidos     │
│  • Verifica token │  │  • Stock en      │  │  • Circuit Breaker   │
│  • Usuarios en    │  │    tiempo real   │  │  • Publica eventos   │
│    memoria        │  │  • DataSeeder    │  │    RabbitMQ          │
└───────────────────┘  │  • Consume       │  └──────────┬───────────┘
                       │    eventos MQ    │             │ JPA
                       └──────┬───────────┘             ▼
                              │ JPA          ┌─────────────────────────┐
                              ▼              │  PostgreSQL :5433        │
                   ┌──────────────────────┐  │  db_pedidos              │
                   │  PostgreSQL :5432    │  └─────────────────────────┘
                   │  db_inventario       │
                   └──────────────────────┘

                   ┌──────────────────────────────────────────────────┐
                   │         RabbitMQ  :5672 / UI :15672              │
                   │         Exchange: smartlogix.exchange             │
                   │         Queue: pedido.creado.queue                │
                   │   ms-pedidos ──[pedido.creado]──► ms-inventario  │
                   └──────────────────────────────────────────────────┘
```

### Flujo de Pago PayPal

```
Frontend → BFF /pagos/crear-orden → PayPal API → devuelve approveUrl
Frontend → redirige usuario a approveUrl (aprobación en PayPal)
PayPal  → redirige a /pago/exito?token=...
Frontend → BFF /pagos/capturar-orden → PayPal API (captura)
BFF     → ms-pedidos (registra pedido COMPLETADO)
ms-pedidos → RabbitMQ publica evento pedido.creado
ms-inventario → consume evento (auditoría / alertas stock)
```

---

## 🧩 Patrones Implementados <a name="patrones"></a>

| Patrón | Dónde se aplica |
|--------|----------------|
| **Microservicios** | `ms-auth`, `ms-inventario`, `ms-pedidos` — desacoplados y desplegables independientemente |
| **BFF / API Gateway** | `bff` — punto de entrada único, enrutamiento, validación JWT |
| **Event-Driven (RabbitMQ)** | `ms-pedidos` publica `pedido.creado`; `ms-inventario` lo consume de forma asíncrona |
| **Circuit Breaker** | `ms-pedidos → PedidoServiceImpl` con `@CircuitBreaker` (Resilience4j); fallback ante fallo de inventario |
| **JWT (Autenticación)** | `ms-auth` emite tokens HS256; BFF los valida sin consultar BD en cada petición |
| **Repository Pattern** | `ProductoRepository`, `PedidoRepository` (Spring Data JPA) |
| **Factory Method** | `ProductoFactory`, `PedidoFactory` — mapeo Entidad ↔ DTO |
| **Database-per-Service** | Cada microservicio tiene su propia BD PostgreSQL aislada |
| **Serverless (FaaS)** | PayPal webhook `/api/pagos/webhook` — se activa solo al confirmar un pago |
| **Carrito en Frontend** | Estado React (`useState`) — sin backend, latencia cero |

---

## 📁 Estructura del Proyecto <a name="estructura"></a>

```
SmartLogix-PayPal/
│
├── 📄 README.md                     ← Este archivo
├── 📄 docker-compose.yml            ← Orquestación de todos los servicios
├── 📄 .env.paypal.example           ← Plantilla de variables de entorno PayPal
├── 🖥️  start.bat / start.sh          ← Iniciar todo (Windows / Mac-Linux)
├── 🛑  stop.bat  / stop.sh           ← Detener todo
├── 📋  logs.bat  / logs.sh           ← Ver logs en tiempo real
│
├── backend/
│   ├── pom.xml                      ← POM raíz (módulos: bff, ms-auth, ms-inventario, ms-pedidos)
│   │
│   ├── ms-auth/                     ← 🆕 Microservicio JWT (puerto 8083)
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/main/java/com/smartlogix/auth/
│   │       ├── MsAuthApplication.java
│   │       ├── controller/AuthController.java
│   │       ├── service/AuthService.java
│   │       ├── util/JwtUtil.java
│   │       └── model/{LoginRequest, LoginResponse, ValidateResponse}.java
│   │
│   ├── bff/                         ← API Gateway + PayPal (puerto 9090)
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/main/java/com/smartlogix/bff/
│   │       ├── BffApplication.java
│   │       ├── client/{AuthClient, InventarioClient, PedidosClient}.java
│   │       ├── config/{CorsConfig, PayPalConfig}.java
│   │       ├── controller/{BffController, PayPalController}.java
│   │       └── service/{AuthService, BffService, PayPalService}.java
│   │
│   ├── ms-inventario/               ← Inventario + consumidor RabbitMQ (puerto 8081)
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/main/java/com/smartlogix/inventario/
│   │       ├── event/{RabbitMQConfig, PedidoCreadoEvent, PedidoCreadoListener}.java
│   │       ├── config/DataSeeder.java
│   │       ├── controller/ProductoController.java
│   │       ├── factory/ProductoFactory.java
│   │       ├── model/{dto/ProductoDTO, entity/Producto}.java
│   │       ├── repository/ProductoRepository.java
│   │       └── service/impl/ProductoServiceImpl.java
│   │
│   └── ms-pedidos/                  ← Pedidos + Circuit Breaker + publicador RabbitMQ (puerto 8082)
│       ├── Dockerfile
│       ├── pom.xml
│       └── src/main/java/com/smartlogix/pedidos/
│           ├── event/{RabbitMQConfig, PedidoCreadoEvent, PedidoEventPublisher}.java
│           ├── client/{InventarioClient, ProductoResponse}.java
│           ├── controller/PedidoController.java
│           ├── factory/PedidoFactory.java
│           ├── model/{dto/PedidoDTO, entity/Pedido}.java
│           ├── repository/PedidoRepository.java
│           └── service/impl/PedidoServiceImpl.java
│
└── frontend/                        ← React 18 SPA (puerto 3000)
    ├── Dockerfile
    ├── package.json
    ├── nginx.conf
    └── src/
        ├── App.js
        ├── services/api.js           ← Envía JWT en cada request
        ├── components/{PayPalCheckout, OrderModal, ProductCard, ProtectedRoute}.js
        └── pages/{Login, Catalog, Admin, PagoExito, PagoCancelado}.js
```

---

## ✅ Requisitos Previos <a name="requisitos"></a>

| Herramienta | Versión mínima | Verificar |
|-------------|---------------|-----------|
| Docker Desktop | 4.x | `docker --version` |
| Docker Compose | 2.x (incluido en Docker Desktop) | `docker compose version` |
| Git | cualquiera | `git --version` |

> **No necesitas** Java, Maven ni Node.js instalados localmente — todo compila dentro de Docker.

---

## ⚙️ Configuración de Variables de Entorno <a name="variables"></a>

### PayPal (obligatorio para pagos)

Copia el archivo de ejemplo y edita con tus credenciales sandbox:

```bash
cp .env.paypal.example .env
```

Contenido de `.env`:

```env
# Credenciales PayPal Sandbox — obtener en https://developer.paypal.com
PAYPAL_CLIENT_ID=TU_CLIENT_ID_SANDBOX
PAYPAL_CLIENT_SECRET=TU_CLIENT_SECRET_SANDBOX
PAYPAL_MODE=sandbox

# URLs de retorno (cambiar si usas ngrok para webhooks)
PAYPAL_RETURN_URL=http://localhost:3000/pago/exito
PAYPAL_CANCEL_URL=http://localhost:3000/pago/cancelado

# JWT Secret (opcional — el valor por defecto funciona en desarrollo)
JWT_SECRET=SmartLogix2026-SuperSecretKey-MustBe256BitsOrMoreForHS256
JWT_EXPIRATION_MS=86400000
```

> 💡 Sin credenciales PayPal el sistema funciona igual, pero el botón de pago fallará.  
> Consulta `PAYPAL_SETUP.md` para la guía completa de configuración.

---

## 🚀 Cómo Correr el Proyecto <a name="correr"></a>

### Opción A — Script automático (recomendado)

**Windows:**
```bat
start.bat
```

**Mac / Linux:**
```bash
chmod +x start.sh
./start.sh
```

### Opción B — Docker Compose manual

```bash
# 1. Construir todas las imágenes (primera vez o tras cambios en código)
docker compose build

# 2. Levantar todos los servicios en segundo plano
docker compose up -d

# 3. Verificar que todos están corriendo
docker compose ps
```

### Orden de arranque (automático vía healthchecks)

```
RabbitMQ ──► db-inventario ──► ms-inventario ──► ms-pedidos
                                                         │
db-pedidos ──────────────────────────────────────────────┘
ms-auth (independiente)
bff ──► (depende de ms-auth, ms-inventario, ms-pedidos)
frontend ──► bff
```

### ⏱️ Primera vez

La primera ejecución tarda **3-5 minutos** porque Docker descarga las imágenes base y Maven compila los 4 módulos Java. Las ejecuciones siguientes son mucho más rápidas gracias a la caché de capas.

Cuando veas esto en los logs, el sistema está listo:

```
smartlogix-bff         | Started BffApplication in X.XXX seconds
smartlogix-ms-auth     | Started MsAuthApplication in X.XXX seconds
smartlogix-frontend    | Starting the development server...
```

---

## 👤 Usuarios de Prueba <a name="usuarios"></a>

| Usuario | Contraseña | Rol | Acceso |
|---------|-----------|-----|--------|
| `admin` | `admin123` | ADMIN | Catálogo + Panel Admin |
| `user1` | `password123` | USER | Catálogo |
| `user2` | `password456` | USER | Catálogo |

Al hacer login, el frontend recibe un **JWT** válido por 24 horas y lo almacena en `localStorage`. Todas las peticiones protegidas lo envían automáticamente en el header `Authorization: Bearer <token>`.

---

## 🌐 URLs y Puertos <a name="urls"></a>

| Servicio | URL | Descripción |
|---------|-----|-------------|
| **Frontend** | http://localhost:3000 | App React — Login / Catálogo / Admin |
| **BFF / API Gateway** | http://localhost:9090 | API principal del frontend |
| **ms-auth** | http://localhost:8083 | Autenticación y emisión de JWT |
| **ms-inventario** | http://localhost:8081 | API interna de productos |
| **ms-pedidos** | http://localhost:8082 | API interna de pedidos |
| **RabbitMQ UI** | http://localhost:15672 | Panel de mensajería (guest / guest) |
| **PostgreSQL inventario** | localhost:5432 | BD `db_inventario` |
| **PostgreSQL pedidos** | localhost:5433 | BD `db_pedidos` |

### Endpoints principales del BFF (`:9090`)

```
POST /api/auth/login              → Login → devuelve JWT
GET  /api/auth/validate           → Verifica JWT (header Authorization)
GET  /api/store/catalogo          → Lista productos del inventario
POST /api/store/comprar           → Crea pedido (requiere JWT)
POST /api/pagos/crear-orden       → Inicia orden PayPal
POST /api/pagos/capturar-orden    → Captura pago y registra pedido
POST /api/pagos/webhook           → Webhook PayPal (requiere HTTPS)
```

### Endpoints de ms-auth (`:8083`)

```
POST /api/auth/login              → Devuelve JWT si credenciales válidas
GET  /api/auth/validate           → Valida JWT (usado por el BFF)
```

---

## 🛠️ Comandos Útiles <a name="comandos"></a>

```bash
# Ver logs de todos los servicios en tiempo real
docker compose logs -f

# Ver logs de un servicio específico
docker compose logs -f ms-auth
docker compose logs -f ms-pedidos
docker compose logs -f rabbitmq

# Reiniciar un servicio sin detener los demás
docker compose restart ms-auth
docker compose restart bff

# Detener todos los servicios (preserva datos en volúmenes)
docker compose down

# Detener Y eliminar volúmenes (limpia BDs y RabbitMQ — fresh start)
docker compose down -v

# Reconstruir solo un servicio tras cambios en código
docker compose build ms-auth
docker compose up -d ms-auth

# Acceder a la BD de inventario
docker exec -it smartlogix-db-inventario psql -U admin -d db_inventario

# Acceder a la BD de pedidos
docker exec -it smartlogix-db-pedidos psql -U admin -d db_pedidos

# Ver colas de RabbitMQ por consola
docker exec -it smartlogix-rabbitmq rabbitmqctl list_queues
```

---

## 🔍 Solución de Problemas <a name="problemas"></a>

### Puerto ya en uso

```bash
# Ver qué proceso usa el puerto (ej: 5432)
# Windows:
netstat -ano | findstr :5432
# Mac/Linux:
lsort -i :5432
```

Edita `docker-compose.yml` y cambia el puerto del host (parte izquierda):
```yaml
ports:
  - "5434:5432"   # cambia 5432 por otro disponible
```

### ms-auth no arranca / JWT error

Verifica que `JWT_SECRET` tenga al menos 32 caracteres. El valor por defecto ya cumple esto.

```bash
docker compose logs ms-auth
```

### ms-pedidos no puede conectar con RabbitMQ

RabbitMQ tarda en estar listo. El `restart: on-failure` hace que ms-pedidos reintente automáticamente. Espera 30-60 segundos y verifica:

```bash
docker compose logs ms-pedidos | grep -i rabbit
docker compose logs rabbitmq | tail -20
```

### Error `Stock insuficiente` en compra

El `DataSeeder` de ms-inventario carga productos con stock limitado. Puedes reiniciarlo:

```bash
docker compose restart ms-inventario
```

O conéctate a la BD y actualiza stock:

```bash
docker exec -it smartlogix-db-inventario psql -U admin -d db_inventario -c \
  "UPDATE productos SET stock_actual = 100;"
```

### El Circuit Breaker se activa (estado OPEN)

Si ms-inventario falla varias veces, Resilience4j abre el circuito por 10 segundos. El pedido queda en estado `FALLIDO`. Para resetear:

```bash
docker compose restart ms-pedidos
```

### Pago PayPal no funciona

1. Verifica que `.env` tenga credenciales sandbox válidas
2. Para webhooks locales necesitas una URL pública HTTPS — usa ngrok:
   ```bash
   ngrok http 9090
   # Actualiza PAYPAL_RETURN_URL y PAYPAL_CANCEL_URL en .env con la URL de ngrok
   ```
   Ver `PAYPAL_SETUP.md` para guía completa.

---

## 🏛️ Decisiones de Diseño

### ¿Por qué RabbitMQ si el stock ya se descuenta con Feign?

El descuento de stock ocurre de forma **síncrona** vía Feign (garantiza consistencia antes de confirmar el pago). RabbitMQ complementa esto publicando el evento `pedido.creado` de forma **asíncrona** para que ms-inventario pueda reaccionar sin bloquear la respuesta al usuario: auditoría, alertas de stock bajo, notificaciones, métricas, etc. Esta es la esencia de la arquitectura Event-Driven.

### ¿Por qué el carrito está en el frontend?

El carrito es estado de sesión local que no necesita persistirse hasta el momento del pago. Mantenerlo en React con `useState` elimina una round-trip de red en cada interacción, reduce la complejidad del backend y da latencia cero al usuario.

### ¿Por qué JWT en ms-auth y no en el BFF?

Separar la responsabilidad de autenticación en su propio microservicio permite escalar ms-auth independientemente, reemplazarlo (por OAuth2, Keycloak, etc.) sin tocar el BFF, y que cualquier microservicio futuro pueda validar tokens consultando el mismo servicio.

