# SmartLogix — Arquitectura de Microservicios

## Diagrama de flujo general

```
[ Usuario / Navegador React (puerto 3000) ]
          |
          | HTTP/HTTPS (fetch + JWT en header)
          ▼
[ BFF / API Gateway (puerto 9090) ]
   ├── Valida JWT con ms-auth
   ├── Llama ms-inventario para catálogo
   ├── Llama ms-pedidos para compra
   └── Maneja integración PayPal
          |
    ┌─────┴──────────────────┐
    ▼                        ▼
[ ms-auth (8083) ]   [ ms-inventario (8081) ]
[ db-auth (5434) ]   [ db-inventario (5432) ]
                             |
                    (publica evento async)
                             ▼
                    [ RabbitMQ — smartlogix.exchange ]
                    [ Queue: pedido.creado.queue ]
                             |
                             ▼
                    [ ms-pedidos (8082) ]  ←── consume evento
                    [ db-pedidos (5433) ]
```

## Microservicios y responsabilidades

| Servicio | Puerto | Responsabilidad | BD |
|---|---|---|---|
| frontend | 3000 | UI React (catálogo, carrito, admin) | — |
| bff | 9090 | API Gateway + integración PayPal | — |
| ms-auth | 8083 | Login, JWT, registro de usuarios | PostgreSQL db-auth |
| ms-inventario | 8081 | Gestión de productos y stock | PostgreSQL db-inventario |
| ms-pedidos | 8082 | Creación y consulta de pedidos | PostgreSQL db-pedidos |
| rabbitmq | 5672 | Event bus asíncrono entre servicios | — |

## Patrones aplicados

| Patrón | Dónde | Por qué |
|---|---|---|
| BFF (Backend for Frontend) | `/backend/bff` | Agrega datos de ms-inventario y ms-pedidos para la UI. Evita que el frontend llame a múltiples servicios. |
| Repository Pattern | Todos los microservicios | Abstrae el acceso a la BD. Los servicios no conocen detalles del ORM. |
| Factory Pattern | `PedidoFactory`, `ProductoFactory` | Centraliza la construcción y mapeo de entidades/DTOs. |
| Publish/Subscribe (Event-Driven) | ms-pedidos → RabbitMQ → ms-inventario | Desacopla la confirmación de stock del flujo de compra. Si ms-inventario cae, el pedido se completa igual. |
| Circuit Breaker | `PedidoServiceImpl` con Resilience4j | Si ms-inventario no responde, activa el fallback y guarda el pedido como FALLIDO en lugar de propagar el error. |

## Flujo de compra completo (paso a paso)

1. Usuario selecciona productos en React → hace click en "Comprar con PayPal"
2. Frontend llama `POST /api/store/comprar` al BFF con el carrito
3. BFF delega a `ms-pedidos`
4. `ms-pedidos` consulta precios a `ms-inventario` (HTTP síncrono con Circuit Breaker)
5. `ms-pedidos` descuenta stock en `ms-inventario`
6. `ms-pedidos` guarda el pedido en `db-pedidos` y publica evento `PedidoCreadoEvent` en RabbitMQ
7. `ms-inventario` consume el evento para confirmación asíncrona
8. BFF responde al frontend con estado "COMPLETADO"
```
