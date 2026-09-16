#!/usr/bin/env bash
# =============================================================================
#  Respaldo de la base de datos de Team Portal (conserva los últimos 7).
#
#      sudo bash deploy/ec2/respaldar-bd.sh
#
#  Restaurar un respaldo (reemplaza los datos actuales):
#      gunzip -c /opt/teamportal/backups/ARCHIVO.sql.gz \
#        | sudo docker exec -i -u postgres teamportal psql -v ON_ERROR_STOP=1 -d teamportal
#
#  Respaldo automático todos los días a las 3 a. m. (sudo crontab -e):
#      0 3 * * * /bin/bash /home/ubuntu/TABLERO-DE-NOTAS/deploy/ec2/respaldar-bd.sh >> /var/log/teamportal-backup.log 2>&1
# =============================================================================
set -Eeuo pipefail

CONTAINER="${CONTAINER:-teamportal}"
BACKUP_DIR="${BACKUP_DIR:-/opt/teamportal/backups}"
KEEP="${KEEP:-7}"
DB_NAME="${DB_NAME:-teamportal}"

[ "$(id -u)" -eq 0 ] || { echo "Ejecuta con sudo: sudo bash $0"; exit 1; }

mkdir -p "${BACKUP_DIR}"
chmod 700 "${BACKUP_DIR}"
FILE="${BACKUP_DIR}/teamportal-$(date +%Y%m%d-%H%M%S).sql.gz"

docker exec -u postgres "${CONTAINER}" pg_dump --clean --if-exists --no-owner -d "${DB_NAME}" | gzip > "${FILE}"
echo "Respaldo creado: ${FILE} ($(du -h "${FILE}" | cut -f1))"

# Borra los más viejos, deja solo los últimos ${KEEP}
ls -1t "${BACKUP_DIR}"/teamportal-*.sql.gz 2>/dev/null | tail -n +"$((KEEP + 1))" | xargs -r rm -f
