#!/usr/bin/env bash
# =============================================================================
#  Prepara un EC2 con Ubuntu para compilar y ejecutar Team Portal con Docker.
#  Se ejecuta UNA sola vez (repetirlo no hace daño):
#
#      sudo bash deploy/ec2/preparar-servidor.sh
#
#  Hace:
#    1. Amplía la partición raíz si agrandaste el volumen EBS en la consola.
#    2. Crea un archivo de swap (2 GB) para que la compilación no se quede sin memoria.
#    3. Instala Docker, BuildKit (docker-buildx) y git.
# =============================================================================
set -Eeuo pipefail

SWAP_GB="${SWAP_GB:-2}"
MIN_DISK_GB=15

step() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
warn() { printf '\033[1;33mADVERTENCIA:\033[0m %s\n' "$*"; }

if [ "$(id -u)" -ne 0 ]; then
  echo "Ejecuta este script con sudo: sudo bash $0"
  exit 1
fi

TARGET_USER="${SUDO_USER:-ubuntu}"
export DEBIAN_FRONTEND=noninteractive

# -----------------------------------------------------------------------------
step "1/4 Disco: ampliar la partición raíz si el volumen EBS es más grande"
if ! command -v growpart >/dev/null 2>&1; then
  apt-get update -y
  apt-get install -y cloud-guest-utils
fi

ROOT_PART="$(lsblk -rno NAME,MOUNTPOINT | awk '$2 == "/" { print $1; exit }')"
if [ -n "${ROOT_PART}" ] && [ -e "/sys/class/block/${ROOT_PART}/partition" ]; then
  ROOT_DISK="$(lsblk -rno PKNAME "/dev/${ROOT_PART}")"
  ROOT_PARTNUM="$(cat "/sys/class/block/${ROOT_PART}/partition")"
  if growpart "/dev/${ROOT_DISK}" "${ROOT_PARTNUM}"; then
    case "$(findmnt -no FSTYPE /)" in
      ext2|ext3|ext4) resize2fs "/dev/${ROOT_PART}" ;;
      xfs) xfs_growfs / ;;
      *) warn "Sistema de archivos no reconocido; amplíalo manualmente." ;;
    esac
    echo "Partición raíz ampliada."
  else
    echo "La partición ya ocupa todo el volumen."
  fi
else
  warn "No se encontró la partición raíz; se omite la ampliación automática."
fi

DISK_GB="$(df -BG --output=size / | tail -1 | tr -dc '0-9')"
if [ "${DISK_GB}" -lt "${MIN_DISK_GB}" ]; then
  warn "El disco raíz tiene ${DISK_GB} GB. Compilar la imagen necesita unos ${MIN_DISK_GB} GB."
  warn "En la consola de AWS: EC2 > Volúmenes > (volumen de la instancia) > Modificar > 20 GB,"
  warn "espera a que diga 'optimizing' o 'completed' y vuelve a ejecutar este script."
fi

# -----------------------------------------------------------------------------
step "2/4 Memoria: archivo de swap de ${SWAP_GB} GB"
if swapon --show --noheadings | grep -q .; then
  echo "Ya hay swap activo:"
  swapon --show
else
  fallocate -l "${SWAP_GB}G" /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo "Swap creado."
fi
echo 'vm.swappiness=10' > /etc/sysctl.d/99-teamportal.conf
sysctl -q -p /etc/sysctl.d/99-teamportal.conf

# -----------------------------------------------------------------------------
step "3/4 Docker, BuildKit y git"
apt-get update -y
apt-get install -y docker.io docker-buildx git curl openssl ca-certificates
systemctl enable --now docker
usermod -aG docker "${TARGET_USER}"

# -----------------------------------------------------------------------------
step "4/4 Resumen"
docker --version
docker buildx version
echo
free -h
echo
df -h /
echo
echo "Listo. Ahora ejecuta:  sudo bash deploy/ec2/desplegar.sh"
