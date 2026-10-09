# ---------- Etapa 1: Build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos primero solo el pom.xml para aprovechar la cache de capas de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiamos el resto del código fuente y compilamos
# (las pruebas se ejecutan en el job "test" del pipeline, antes de construir la imagen)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Etapa 2: Runtime ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Usuario no root por seguridad. La carpeta uploads debe pertenecerle para poder guardar
# comprobantes, fotos de barberos y las imágenes del asesor IA.
RUN addgroup --system spring && adduser --system --ingroup spring spring \
    && mkdir -p /app/uploads && chown -R spring:spring /app
USER spring:spring

# Copiamos el jar generado en la etapa de build
COPY --from=build --chown=spring:spring /app/target/barberia-la-clasica.jar app.jar

VOLUME ["/app/uploads"]

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
