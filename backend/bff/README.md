# Backend For Frontend (BFF) / API Gateway

Este componente actúa como la única puerta de entrada (API Gateway) para la aplicación React. Recibe las peticiones del cliente y las rutea o compone con los microservicios subyacentes (`ms-inventario` y `ms-pedidos`).

## Propósito
- Simplificar las llamadas desde el frontend.
- Componer respuestas que requieren datos de múltiples microservicios.
- Ocultar la topología interna del backend.

## Instalación y Ejecución

```bash
mvn spring-boot:run
```
El BFF se levantará en el puerto 8080 por defecto.

## Pruebas
```bash
mvn clean test
```
