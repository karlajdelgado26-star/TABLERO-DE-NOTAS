#!/usr/bin/env bash
# =============================================================================
#  Despliega (o actualiza) Team Portal en el EC2.
#
#      sudo bash deploy/ec2/desplegar.sh                 # git pull + compilar + reiniciar
#      sudo bash deploy/ec2/desplegar.sh --sin-actualizar  # no hace git pull
#      sudo bash deploy/ec2/desplegar.sh --sin-compilar    # reinicia con la imagen ya construida
#
#  La primera vez crea /opt/teamportal/teamportal.env con contraseñas aleatorias.
#  NO borres ese archivo: la base de datos quedó creada con esa contraseña.
# =============================================================================
set -Eeuo pipefail

APP_DIR="${APP_DIR:-/opt/teamportal}"
ENV_FILE="${APP_DIR}/teamportal.env"
IMAGE="teamportal"
CONTAINER="teamportal"
VOLUME="teamportal-data"
HTTP_PORT="${HTTP_PORT:-80}"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-600}"

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

PULL=true
BUILD=true
for arg in "$@"; do
  case "${arg}" in
    --sin-actualizar) PULL=false ;;
    --sin-compilar) BUILD=false ;;
    -h|--help) sed -n '2,12p' "$0"; exit 0 ;;
    *) echo "Opción desconocida: ${arg}"; exit 1 ;;
  esac
done

step() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
warn() { printf '\033[1;33mADVERTENCIA:\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31mERROR:\033[0m %s\n' "$*"; exit 1; }

[ "$(id -u)" -eq 0 ] || fail "Ejecuta con sudo: sudo bash $0"
command -v docker >/dev/null 2>&1 || fail "Docker no está instalado. Ejecuta primero: sudo bash deploy/ec2/preparar-servidor.sh"

# -----------------------------------------------------------------------------
if [ "${PULL}" = true ]; then
  step "Actualizando el código (git pull)"
  REPO_OWNER="$(stat -c '%U' "${REPO_DIR}")"
  if sudo -u "${REPO_OWNER}" git -C "${REPO_DIR}" pull --ff-only; then
    sudo -u "${REPO_OWNER}" git -C "${REPO_DIR}" log -1 --format='Versión: %h %s (%cr)'
  else
    warn "No se pudo hacer git pull; se usa el código que ya está en ${REPO_DIR}."
  fi
fi

# -----------------------------------------------------------------------------
step "Configuración y secretos"
mkdir -p "${APP_DIR}"
chmod 700 "${APP_DIR}"
if [ ! -f "${ENV_FILE}" ]; then
  umask 077
  cat > "${ENV_FILE}" <<EOF
# Generado el $(date '+%Y-%m-%d %H:%M'). No lo borres ni cambies DB_PASSWORD:
# la base de datos ya quedó creada con esta contraseña.
DB_PASSWORD=$(openssl rand -hex 24)
JWT_SECRET=$(openssl rand -hex 48)
# Memoria para una instancia pequeña (t3.micro / t3.small)
JAVA_OPTS=-XX:MaxRAMPercentage=40.0 -XX:+UseSerialGC
EOF
  echo "Creado ${ENV_FILE} con contraseñas aleatorias."
else
  echo "Usando ${ENV_FILE} existente."
fi

# -----------------------------------------------------------------------------
if [ "${BUILD}" = true ]; then
  step "Construyendo la imagen (en un t3.micro puede tardar 10-20 minutos)"
  FREE_GB="$(df -BG --output=avail /var/lib/docker 2>/dev/null | tail -1 | tr -dc '0-9')"
  if [ -n "${FREE_GB}" ] && [ "${FREE_GB}" -lt 4 ]; then
    warn "Solo quedan ${FREE_GB} GB libres. Si la compilación falla por espacio, amplía el volumen EBS."
  fi

  if docker image inspect "${IMAGE}:latest" >/dev/null 2>&1; then
    docker tag "${IMAGE}:latest" "${IMAGE}:anterior"
  fi
  docker build -t "${IMAGE}:latest" "${REPO_DIR}"
else
  docker image inspect "${IMAGE}:latest" >/dev/null 2>&1 || fail "No existe la imagen ${IMAGE}:latest; ejecuta sin --sin-compilar."
fi

# -----------------------------------------------------------------------------
step "Reiniciando el contenedor"
docker rm -f "${CONTAINER}" >/dev/null 2>&1 || true
docker run -d \
  --name "${CONTAINER}" \
  --restart unless-stopped \
  -p "${HTTP_PORT}:80" \
  --env-file "${ENV_FILE}" \
  -v "${VOLUME}:/var/lib/postgresql/data" \
  --log-opt max-size=10m --log-opt max-file=3 \
  "${IMAGE}:latest" >/dev/null

# -----------------------------------------------------------------------------
step "Esperando a que la aplicación responda (máximo $((HEALTH_TIMEOUT_SECONDS / 60)) minutos)"
start="$(date +%s)"
status="starting"
while true; do
  status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${CONTAINER}" 2>/dev/null || echo "missing")"
  elapsed=$(( $(date +%s) - start ))
  if [ "${status}" = "healthy" ]; then
    break
  fi
  if [ "${status}" = "exited" ] || [ "${status}" = "missing" ] || [ "${elapsed}" -ge "${HEALTH_TIMEOUT_SECONDS}" ]; then
    echo
    docker logs --tail 80 "${CONTAINER}" 2>&1 || true
    fail "La aplicación no quedó lista (estado: ${status}). Revisa los logs de arriba o: sudo docker logs -f ${CONTAINER}"
  fi
  printf '\r   %3ss · estado: %-10s' "${elapsed}" "${status}"
  sleep 5
done
printf '\r   Lista en %ss.%-20s\n' "$(( $(date +%s) - start ))" ""

docker image prune -f >/dev/null 2>&1 || true

# -----------------------------------------------------------------------------
PUBLIC_IP=""
TOKEN="$(curl -fsS --max-time 2 -X PUT http://169.254.169.254/latest/api/token \
  -H 'X-aws-ec2-metadata-token-ttl-seconds: 60' 2>/dev/null || true)"
if [ -n "${TOKEN}" ]; then
  PUBLIC_IP="$(curl -fsS --max-time 2 -H "X-aws-ec2-metadata-token: ${TOKEN}" \
    http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || true)"
fi
[[ "${PUBLIC_IP}" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]] || PUBLIC_IP=""
URL="http://${PUBLIC_IP:-IP_PUBLICA_DEL_EC2}"
[ "${HTTP_PORT}" = "80" ] || URL="${URL}:${HTTP_PORT}"

step "Team Portal está en línea"
echo "   ${URL}"
echo
echo "   Si no abre desde tu navegador, permite HTTP (puerto ${HTTP_PORT}) en el grupo de seguridad del EC2."
echo "   Logs:       sudo docker logs -f ${CONTAINER}"
echo "   Respaldo:   sudo bash deploy/ec2/respaldar-bd.sh"
echo
warn "Los usuarios demo (admin@demo.com, lider@demo.com, usuario@demo.com) tienen contraseñas públicas."
warn "Entra como administrador y cámbiales la contraseña en Usuarios > Contraseña."
