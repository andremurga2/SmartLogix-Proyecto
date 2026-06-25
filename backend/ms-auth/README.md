# ms-auth — Microservicio de Autenticación

Microservicio REST de **SmartLogix** encargado de autenticar usuarios y emitir tokens JWT. Persiste los datos de usuarios en PostgreSQL mediante JPA, expone endpoints de login y validación de token consumidos por el BFF, y ofrece una API de gestión de usuarios para el rol ADMIN.

---

## Tabla de contenidos

1. [Tecnologías](#tecnologías)
2. [Estructura del proyecto](#estructura-del-proyecto)
3. [Variables de entorno](#variables-de-entorno)
4. [Instalación y ejecución local](#instalación-y-ejecución-local)
5. [Ejecución con Docker](#ejecución-con-docker)
6. [Endpoints disponibles](#endpoints-disponibles)
7. [Ejecución de pruebas unitarias](#ejecución-de-pruebas-unitarias)
8. [Cobertura de pruebas (JaCoCo)](#cobertura-de-pruebas-jacoco)

---

## Tecnologías

| Herramienta | Versión |
|---|---|
| Java | 17 (Eclipse Temurin) |
| Spring Boot | 3.x |
| Spring Data JPA | — |
| Spring Security | BCryptPasswordEncoder |
| JJWT (jsonwebtoken) | 0.11.5 |
| PostgreSQL | 15+ |
| Lombok | — |
| Maven | 3.9.6 |
| JUnit 5 + Mockito | — |
| JaCoCo | — |

---

## Estructura del proyecto

```
ms-auth/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/smartlogix/auth/
    │   │   ├── MsAuthApplication.java
    │   │   ├── config/
    │   │   │   ├── DataSeeder.java          # Carga datos iniciales (usuarios de prueba)
    │   │   │   ├── PasswordEncoderConfig.java
    │   │   │   └── SecurityConfig.java
    │   │   ├── controller/
    │   │   │   └── AuthController.java      # Endpoints REST
    │   │   ├── model/
    │   │   │   ├── entity/Usuario.java      # Entidad JPA
    │   │   │   ├── LoginRequest.java
    │   │   │   ├── LoginResponse.java
    │   │   │   ├── UsuarioDTO.java
    │   │   │   └── ValidateResponse.java
    │   │   ├── repository/
    │   │   │   └── UsuarioRepository.java
    │   │   ├── service/
    │   │   │   └── AuthService.java
    │   │   └── util/
    │   │       └── JwtUtil.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/smartlogix/auth/
            ├── controller/AuthControllerTest.java
            ├── service/AuthServiceTest.java
            └── util/JwtUtilTest.java
```

---

## Variables de entorno

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5434/db_auth` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos | `admin` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos | `admin_password` |
| `JWT_SECRET` | Clave secreta HS256 (mínimo 256 bits) | `SmartLogix2026-SuperSecretKey-MustBe256BitsOrMoreForHS256` |
| `JWT_EXPIRATION_MS` | Tiempo de expiración del token (ms) | `86400000` (24 horas) |

---

## Instalación y ejecución local

### Requisitos previos

- **Java 17** instalado y en el `PATH`
- **Maven 3.9+** instalado
- **PostgreSQL** corriendo en el puerto `5434` con la base de datos `db_auth` creada

### Pasos

```bash
# 1. Clonar el repositorio raíz
git clone <URL-del-repositorio>
cd SmartLogix-PayPal/backend

# 2. Compilar el módulo ms-auth (desde la raíz del backend)
mvn package -pl ms-auth -am -DskipTests

# 3. Ejecutar el microservicio
java -jar ms-auth/target/ms-auth-*.jar
```

El servicio quedará disponible en `http://localhost:8083`.

> **Nota:** al arrancar, el `DataSeeder` inserta automáticamente usuarios de prueba si la base de datos está vacía. Hibernate gestiona el esquema con `ddl-auto: update`, por lo que no es necesario ejecutar scripts SQL manualmente.

---

## Ejecución con Docker

```bash
# Desde la carpeta raíz del backend (donde viven todos los pom.xml de módulo)
docker build -f ms-auth/Dockerfile -t smartlogix-ms-auth .

docker run -d \
  --name ms-auth \
  -p 8083:8083 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://<host-db>:5434/db_auth \
  -e SPRING_DATASOURCE_USERNAME=admin \
  -e SPRING_DATASOURCE_PASSWORD=admin_password \
  -e JWT_SECRET=SmartLogix2026-SuperSecretKey-MustBe256BitsOrMoreForHS256 \
  smartlogix-ms-auth
```

> El `Dockerfile` usa una construcción en dos etapas: la primera compila el JAR con Maven 3.9 + JDK 17; la segunda copia únicamente el JAR en una imagen mínima `eclipse-temurin:17-jre-alpine`.

---

## Endpoints disponibles

Base URL: `http://localhost:8083/api/auth`

### Autenticación

| Método | Ruta | Descripción | Body |
|---|---|---|---|
| `POST` | `/login` | Autentica un usuario y devuelve un JWT | `{ "username": "...", "password": "..." }` |
| `GET` | `/validate` | Valida el JWT enviado en el header `Authorization: Bearer <token>` | — |

#### Ejemplo — Login exitoso

**Request**
```json
POST /api/auth/login
{
  "username": "admin",
  "password": "admin123"
}
```

**Response `200 OK`**
```json
{
  "success": true,
  "message": "Login exitoso",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "role": "ADMIN"
}
```

**Response `401 Unauthorized`** (credenciales inválidas)
```json
{
  "success": false,
  "message": "Usuario o contraseña inválidos"
}
```

#### Ejemplo — Validar token

**Request**
```
GET /api/auth/validate
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Response `200 OK`**
```json
{
  "valid": true,
  "username": "admin",
  "role": "ADMIN",
  "message": "Token válido"
}
```

---

### Gestión de usuarios

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/usuarios` | Lista todos los usuarios (sin contraseña) |
| `POST` | `/usuarios` | Crea un usuario (acceso admin) |
| `POST` | `/registro` | Registro público — crea siempre con rol `USER` |
| `PUT` | `/usuarios/{id}` | Actualiza rol, estado o contraseña |
| `DELETE` | `/usuarios/{id}` | Elimina un usuario |

---

## Ejecución de pruebas unitarias

```bash
# Desde la raíz del backend
mvn test -pl ms-auth
```

Las pruebas cubren:

| Clase de test | Componente probado |
|---|---|
| `AuthServiceTest` | Lógica de login, validación, CRUD de usuarios |
| `AuthControllerTest` | Capa REST (mockeando AuthService) |
| `JwtUtilTest` | Generación y validación de tokens JWT |

---

## Cobertura de pruebas (JaCoCo)

Para generar el reporte HTML de cobertura:

```bash
mvn verify -pl ms-auth
```

El reporte queda en:
```
ms-auth/target/site/jacoco/index.html
```

Resultados obtenidos:

| Métrica | Cobertura |
|---|---|
| Instrucciones | **97%** |
| Ramas | **88%** |
