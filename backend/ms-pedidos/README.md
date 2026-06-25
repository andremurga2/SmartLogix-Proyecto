# Microservicio de Pedidos

Este microservicio es responsable de la gestión de órdenes de compra. Interactúa con el microservicio de Inventario para verificar stock antes de confirmar un pedido.

## Resiliencia (Circuit Breaker)
Se ha implementado **Resilience4j** para aplicar el patrón Circuit Breaker. Si el `ms-inventario` falla o no está disponible, el Circuit Breaker se abre para evitar llamadas en cascada y proporcionar un "fallback" controlado, asegurando la resiliencia del sistema.

## Instalación

1. Asegúrate de tener Java 17 y Maven instalados.
2. La base de datos es PostgreSQL (Puerto 5433 en docker-compose).

## Ejecución Local

Para ejecutar el microservicio localmente (sin Docker):
```bash
mvn spring-boot:run
```

## Pruebas Unitarias

Para ejecutar las pruebas y medir la cobertura de código (Jacoco):
```bash
mvn clean test
```

## Patrones de Diseño
- **Circuit Breaker**: Manejo de fallos en llamadas de red (Resilience4j).
- **Repository**: Persistencia de Pedidos.
- **Dependency Injection**: Inversión de control con Spring Boot.

## Decisión de diseño: Acoplamiento síncrono con ms-inventario

`ms-pedidos` llama a `ms-inventario` de forma **síncrona vía HTTP** (Feign Client)
para obtener el precio del producto y descontar el stock antes de confirmar el pedido.

**¿Por qué no es async puro?**
El descuento de stock debe ocurrir *antes* de confirmar al usuario que el pago fue aceptado.
Si usáramos mensajería async, el usuario recibiría confirmación y el stock podría no estar
disponible todavía, generando inconsistencias.

**Temporal coupling mitigado con Circuit Breaker:**
Se usa **Resilience4j** con `@CircuitBreaker(name = "inventarioCB")`.
Si ms-inventario no responde, el método `crearPedidoFallback` guarda el pedido
como `ESTADO = "FALLIDO"` en vez de lanzar un error 500 al usuario.

**Referencia en clase:** PDF ipc_v2 — Temporal Coupling y patrón de
"responde primero, procesa después".