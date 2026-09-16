# syntax=docker/dockerfile:1
# =============================================================================
#  TABLERO-DE-NOTAS · Team Portal
#  Una sola imagen con:
#    - Frontend Angular   -> servido por nginx en el puerto 80
#    - Backend Spring Boot -> puerto interno 8080 (nginx redirige /api)
#    - PostgreSQL 16       -> instalado y configurado dentro de la imagen
#  supervisord arranca y vigila los tres procesos.
#
#  Construir (desde la raíz del repositorio):
#      docker build -t teamportal .
#
#  Ejecutar:
#      docker run -d --name teamportal -p 8080:80 \
#        -e DB_PASSWORD=CambiaEstaClave \
#        -e JWT_SECRET=UnaClaveLargaDeAlMenos32CaracteresParaFirmarJWT \
#        -v teamportal-data:/var/lib/postgresql/data \
#        teamportal
#
#  Abrir: http://localhost:8080
#  (Opcional) conectarse a PostgreSQL desde tu equipo: agrega -p 5432:5432
# =============================================================================


# -----------------------------------------------------------------------------
# Etapa 1: compilar el frontend (Angular 21)
# -----------------------------------------------------------------------------
FROM node:22-bookworm-slim AS frontend-build
WORKDIR /build
# Permite usar swap en máquinas con poca RAM (ej. t3.micro) en vez de fallar por memoria
ENV NODE_OPTIONS=--max-old-space-size=1536

COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --no-audit --no-fund

COPY frontend/ ./
RUN npm run build -- --configuration production


# -----------------------------------------------------------------------------
# Etapa 2: compilar el backend (Spring Boot 4 / Java 21)
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /build
ENV MAVEN_OPTS=-Xmx768m

COPY backend/pom.xml ./
COPY backend/src ./src
# Los tests se omiten porque necesitan una base de datos corriendo.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package -DskipTests \
 && cp target/teamportal-*.jar /build/app.jar


# -----------------------------------------------------------------------------
# Etapa 3: imagen final (Java 21 + PostgreSQL 16 + nginx + supervisor)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-noble

ENV PG_VERSION=16 \
    PGDATA=/var/lib/postgresql/data \
    LANG=C.UTF-8 \
    TZ=America/Bogota
ENV PATH=/usr/lib/postgresql/${PG_VERSION}/bin:${PATH}

# --- Instalación de PostgreSQL, nginx y supervisor ---------------------------
# Se evita que el paquete cree el clúster "main" por defecto: el clúster se crea
# en el primer arranque dentro de $PGDATA (así funciona con volúmenes).
RUN mkdir -p /etc/postgresql-common/createcluster.d \
 && echo "create_main_cluster = false" > /etc/postgresql-common/createcluster.d/00-no-main-cluster.conf \
 && apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        postgresql-${PG_VERSION} \
        nginx \
        supervisor \
        curl \
        tzdata \
 && rm -rf /var/lib/apt/lists/* \
 && rm -f /etc/nginx/sites-enabled/default \
 && useradd --system --home-dir /app --shell /usr/sbin/nologin teamportal \
 && mkdir -p /app /var/www/teamportal "$PGDATA" /run/postgresql \
 && chown -R postgres:postgres "$PGDATA" /run/postgresql

# --- nginx: sirve Angular y redirige /api al backend -------------------------
COPY <<'NGINX' /etc/nginx/conf.d/teamportal.conf
server {
    listen 80 default_server;
    server_name _;

    root  /var/www/teamportal;
    index index.html;

    access_log /dev/stdout;
    error_log  /dev/stderr warn;

    client_max_body_size 10m;

    gzip on;
    gzip_types text/plain text/css application/javascript application/json image/svg+xml;

    # API REST -> Spring Boot
    location /api/ {
        proxy_pass         http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }

    # Archivos estáticos (Angular genera nombres con hash)
    location ~* \.(?:js|css|ico|png|jpg|jpeg|gif|svg|woff2?|ttf)$ {
        expires 7d;
        access_log off;
        try_files $uri =404;
    }

    # Rutas de Angular (/login, /board, ...) -> index.html
    location / {
        add_header Cache-Control "no-cache";
        try_files $uri $uri/ /index.html;
    }
}
NGINX

# --- supervisord: arranca PostgreSQL, backend y nginx ------------------------
COPY <<'SUPERVISOR' /etc/supervisor/teamportal.conf
[supervisord]
nodaemon=true
user=root
logfile=/dev/null
logfile_maxbytes=0
pidfile=/run/supervisord.pid

[program:postgresql]
command=/usr/lib/postgresql/%(ENV_PG_VERSION)s/bin/postgres -D %(ENV_PGDATA)s
user=postgres
priority=10
autorestart=true
stopsignal=INT
stopwaitsecs=60
stdout_logfile=/dev/stdout
stdout_logfile_maxbytes=0
redirect_stderr=true

[program:backend]
command=/usr/local/bin/start-backend.sh
user=teamportal
directory=/app
priority=20
autorestart=true
startsecs=10
stopwaitsecs=30
stdout_logfile=/dev/stdout
stdout_logfile_maxbytes=0
redirect_stderr=true

[program:nginx]
command=/usr/sbin/nginx -g "daemon off;"
priority=30
autorestart=true
stdout_logfile=/dev/stdout
stdout_logfile_maxbytes=0
redirect_stderr=true
SUPERVISOR

# --- Script de arranque: inicializa y configura PostgreSQL -------------------
COPY --chmod=755 <<'ENTRYPOINT_SH' /usr/local/bin/docker-entrypoint.sh
#!/usr/bin/env bash
set -Eeuo pipefail

# Variables que usa application.yml (se pueden cambiar con -e en docker run)
export DB_HOST="${DB_HOST:-127.0.0.1}"
export DB_PORT="${DB_PORT:-5432}"
export DB_NAME="${DB_NAME:-teamportal}"
export DB_USER="${DB_USER:-teamportal}"
export DB_PASSWORD="${DB_PASSWORD:-teamportal}"
export JAVA_OPTS="${JAVA_OPTS:--XX:MaxRAMPercentage=50.0}"

if [ -z "${JWT_SECRET:-}" ]; then
  echo "ADVERTENCIA: JWT_SECRET no está definido; se usará la clave de desarrollo de application.yml."
fi
if [ "$DB_PASSWORD" = "teamportal" ]; then
  echo "ADVERTENCIA: DB_PASSWORD usa el valor por defecto; cámbialo con -e DB_PASSWORD=..."
fi

PG_BIN="/usr/lib/postgresql/${PG_VERSION}/bin"

mkdir -p "$PGDATA" /run/postgresql
chown -R postgres:postgres "$PGDATA" /run/postgresql
chmod 700 "$PGDATA"

if [ ! -s "$PGDATA/PG_VERSION" ]; then
  echo ">> Primer arranque: inicializando PostgreSQL en $PGDATA"

  runuser -u postgres -- "$PG_BIN/initdb" \
      --pgdata="$PGDATA" \
      --username=postgres \
      --encoding=UTF8 \
      --locale=C.UTF-8 \
      --auth-local=peer \
      --auth-host=scram-sha-256

  # Configuración del servidor
  cat >> "$PGDATA/postgresql.conf" <<'CONF'

# ---- Team Portal ----
listen_addresses = '*'
port = 5432
unix_socket_directories = '/run/postgresql'
max_connections = 100
shared_buffers = 128MB
password_encryption = scram-sha-256
CONF

  # Acceso por red con contraseña (el superusuario postgres no tiene contraseña,
  # así que solo puede entrar desde dentro del contenedor)
  cat >> "$PGDATA/pg_hba.conf" <<'HBA'

# ---- Team Portal ----
host    all    all    0.0.0.0/0    scram-sha-256
host    all    all    ::/0         scram-sha-256
HBA

  # Servidor temporal (solo socket local) para crear usuario y base de datos
  runuser -u postgres -- "$PG_BIN/pg_ctl" -D "$PGDATA" -o "-c listen_addresses=''" -w start

  runuser -u postgres -- "$PG_BIN/psql" -v ON_ERROR_STOP=1 --no-psqlrc -d postgres \
      -v db_user="$DB_USER" -v db_pass="$DB_PASSWORD" -v db_name="$DB_NAME" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN', :'db_user')
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'db_user')\gexec
SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'db_user', :'db_pass')\gexec
SELECT format('CREATE DATABASE %I OWNER %I ENCODING %L', :'db_name', :'db_user', 'UTF8')
 WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'db_name')\gexec
SQL

  runuser -u postgres -- "$PG_BIN/pg_ctl" -D "$PGDATA" -m fast -w stop
  echo ">> PostgreSQL configurado: base '$DB_NAME', usuario '$DB_USER'"
else
  echo ">> PostgreSQL ya inicializado en $PGDATA (se conserva la configuración existente)"
fi

exec /usr/bin/supervisord -c /etc/supervisor/teamportal.conf
ENTRYPOINT_SH

# --- Script del backend: espera a PostgreSQL y arranca Spring Boot -----------
COPY --chmod=755 <<'BACKEND_SH' /usr/local/bin/start-backend.sh
#!/usr/bin/env bash
set -euo pipefail

echo ">> Esperando a PostgreSQL en ${DB_HOST}:${DB_PORT} ..."
until "/usr/lib/postgresql/${PG_VERSION}/bin/pg_isready" -q -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME"; do
  sleep 1
done

echo ">> Iniciando backend Spring Boot"
# shellcheck disable=SC2086
exec java $JAVA_OPTS -jar /app/app.jar
BACKEND_SH

# Por si el Dockerfile se guardó con saltos de línea de Windows (CRLF)
RUN sed -i 's/\r$//' \
      /usr/local/bin/docker-entrypoint.sh \
      /usr/local/bin/start-backend.sh \
      /etc/nginx/conf.d/teamportal.conf \
      /etc/supervisor/teamportal.conf \
 && nginx -t

# --- Artefactos compilados ---------------------------------------------------
COPY --from=frontend-build /build/dist/frontend/browser/ /var/www/teamportal/
COPY --from=backend-build  /build/app.jar /app/app.jar

EXPOSE 80 5432
VOLUME ["/var/lib/postgresql/data"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=180s --retries=3 \
  CMD pg_isready -q -h 127.0.0.1 \
   && curl -fsS -o /dev/null http://127.0.0.1/ \
   && curl -sS  -o /dev/null http://127.0.0.1:8080/ \
   || exit 1

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
