# ── Etapa 1: Build con Maven ─────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copiar el parent POM y los POMs de todos los módulos
COPY pom.xml ./
COPY ms-auth/pom.xml ms-auth/
COPY bff/pom.xml bff/
COPY ms-inventario/pom.xml ms-inventario/
COPY ms-pedidos/pom.xml ms-pedidos/

# Descargar dependencias sin compilar (optimiza cache de Docker)
RUN mvn dependency:go-offline -pl ms-auth --no-transfer-progress

# Copiar el código fuente de ms-auth
COPY ms-auth/src ms-auth/src/

# Compilar solo el módulo ms-auth
RUN mvn package -pl ms-auth -am -DskipTests --no-transfer-progress

# ── Etapa 2: Imagen de ejecución ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/ms-auth/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]