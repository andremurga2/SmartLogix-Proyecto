# ── Etapa 1: Build con Maven ─────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml ./
COPY ms-auth/pom.xml ms-auth/
COPY bff/pom.xml bff/
COPY ms-inventario/pom.xml ms-inventario/
COPY ms-pedidos/pom.xml ms-pedidos/

RUN mvn dependency:go-offline -pl ms-inventario --no-transfer-progress

COPY ms-inventario/src ms-inventario/src/

RUN mvn package -pl ms-inventario -am -DskipTests --no-transfer-progress

# ── Etapa 2: Imagen de ejecución ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/ms-inventario/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
