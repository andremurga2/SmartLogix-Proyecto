# ── Etapa 1: Build con Maven ─────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copiar el parent POM primero (cache de dependencias)
COPY pom.xml ./
COPY ms-auth/pom.xml ms-auth/
COPY bff/pom.xml bff/
COPY ms-inventario/pom.xml ms-inventario/
COPY ms-pedidos/pom.xml ms-pedidos/
# Descargar dependencias sin compilar (optimiza cache de Docker)
RUN mvn dependency:go-offline -pl bff --no-transfer-progress

# Copiar el código fuente del BFF
COPY bff/src bff/src/

# Compilar solo el módulo BFF
RUN mvn package -pl bff -am -DskipTests --no-transfer-progress

# ── Etapa 2: Imagen de ejecución ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/bff/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]



