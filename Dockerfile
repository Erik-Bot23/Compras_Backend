# ============================================================================
#  Dockerfile de Ventas-Backend (build multi-etapa)
# ----------------------------------------------------------------------------
#  Por qué multi-etapa: Railway construye el contenedor desde este Dockerfile
#  en un entorno limpio donde NO existe target/*.jar. La primera etapa compila
#  con Maven dentro de la imagen y la segunda solo copia el .jar resultante,
#  dejando la imagen final ligera (sin Maven ni dependencias de build).
#
#  VARIABLES DE ENTORNO: no se exponen aquí. Los secretos se inyectan en tiempo
#  de ejecución con las Variables de Railway (DB*, JWT_SECRET, etc.).
# ============================================================================

# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copia primero solo el pom y baja las dependencias en capa aparte
# (aprovecha el cache de Docker: no re-descarga todo si solo cambia el código).
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Copia el código fuente y compila el jar final (sin ejecutar tests, que exigen
# base de datos local y no están disponibles durante el build).
COPY src ./src
RUN mvn clean package -DskipTests

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Ejecutar como usuario NO-root (seguridad): Railway ya permite hacer bind a
# $PORT >1024, así que no necesitamos privilegios. El directorio de uploads se
# crea y se asigna a ese usuario; en Railway se monta un Volumen persistente en
# /app/uploads (mismo UID 1001) para que las imágenes sobrevivan al redeploy.
RUN addgroup --system appgroup \
    && adduser --system --ingroup appgroup --uid 1001 appuser \
    && mkdir -p /app/uploads \
    && chown -R appuser:appgroup /app
USER appuser

# Copia únicamente el jar generado en la etapa build.
COPY --from=build /build/target/*.jar app.jar

# Railway usa su propio HEALTHCHECK opcional; /ping está marcado público
# (configura el healthcheck de Railway como HTTP GET https://<host>/ping).
EXPOSE 8081

# El puerto real lo define $PORT en Railway (application.yaml: server.port=${PORT:8081}).
ENTRYPOINT ["java", "-jar", "app.jar"]