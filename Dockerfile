# ---------- Etapa 1: Build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos primero solo el pom.xml para aprovechar la cache de capas de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiamos el resto del código fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Etapa 2: Runtime ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Usuario no root por seguridad
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copiamos el jar generado en la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Carpeta de uploads (comprobantes) referenciada en application.properties
VOLUME ["/app/uploads"]

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
