# Microservicio de Inventario

Este microservicio es responsable de gestionar el catálogo de productos y mantener el control del stock disponible.
Implementa el patrón **Repository**, **Factory Method** y **Dependency Injection**.

## Instalación

1. Asegúrate de tener Java 17 y Maven instalados.
2. La base de datos recomendada es PostgreSQL, provisionada mediante Docker Compose.

## Ejecución Local

Para ejecutar el microservicio localmente (sin Docker):
```bash
mvn spring-boot:run
```
*Nota: Asegúrate de tener una instancia de PostgreSQL corriendo en el puerto 5432 con la base de datos `db_inventario` creada o ajusta el `application.yml` a H2 para desarrollo local.*

## Pruebas Unitarias

El proyecto utiliza JUnit 5 y Mockito para las pruebas unitarias. Para ejecutar las pruebas y generar el reporte de cobertura con Jacoco:

```bash
mvn clean test
```

## Patrones de Diseño Utilizados
- **Repository**: Para encapsular la lógica de acceso a datos utilizando Spring Data JPA.
- **Factory Method**: Para la creación instanciada de DTOs y Entidades.
- **Dependency Injection**: Utilizado en todo el servicio mediante Spring (`@Autowired`, inyección por constructor).
