# ============================================
# Dockerfile para LD Admin Tool Backend
# ============================================
# Aplicación Spring Boot con Java 17

FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Descargar dependencias (cache layer)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src src

# Compilar la aplicación
RUN mvn clean package -DskipTests

# ============================================
# Imagen de producción
# ============================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar el JAR compilado
COPY --from=build /app/target/*.jar app.jar

# Exponer puerto
EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Variables de entorno por defecto (se pueden sobrescribir en docker-compose)
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
