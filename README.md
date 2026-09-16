# TABLERO-DE-NOTAS
PORTAL DE EQUIPO CON TABLERO DE NOTAS

Portal de equipo con tableros de notas tipo post-it. Backend en Spring Boot 4 (Java 21) con PostgreSQL y frontend en Angular 21.

## Roles

| Permiso | Usuario (`USER`) | Líder (`LEADER`) | Administrador (`ADMIN`) |
|---|:---:|:---:|:---:|
| Ver tableros y sus notas | ✔ | ✔ | ✔ |
| Crear notas | ✔ | ✔ | ✔ |
| Editar, mover y eliminar **sus propias** notas | ✔ | ✔ | ✔ |
| Editar, mover y eliminar notas **de otros** | | ✔ | ✔ |
| Crear, modificar y eliminar tableros | | ✔ | ✔ |
| Crear, modificar, cambiar contraseña y eliminar usuarios | | | ✔ |

- "Eliminar" un usuario lo **desactiva** (`users.active = false`): no puede iniciar sesión y su token deja de funcionar, pero sus notas se conservan con su nombre. El administrador puede reactivarlo.
- Un administrador no puede desactivarse ni cambiar su propio rol, y siempre debe quedar al menos un administrador activo.
- Eliminar un tablero elimina también sus notas.
- Las notas creadas antes de los roles quedan en el tablero **General**, sin autor: solo líderes y administradores pueden modificarlas.

## Dashboard

En `/dashboard`, visible para los tres roles con la misma información:

- **Avance general**: porcentaje de notas completadas.
- **Indicadores**: notas, tableros, empleados con notas, acciones registradas, quién tiene más y menos notas.
- **Estado de las notas** (dona), **tipos de tablero** y **estado de los tableros** (barras).
- **Avance en el tiempo**: notas creadas vs. completadas por día (o por semana en rangos largos).
- **Notas por empleado** y **avance por tablero**: barras apiladas por estado.
- **Actividad de los empleados**: historial de quién creó, editó, cambió de estado, movió o eliminó cada nota o tablero.
- **Detalle de notas**: tabla con tablero, autor, estado y fechas.

Todos los filtros (periodo, tablero, tipo, estado del tablero, empleado) están en una fila arriba y afectan a todo. Hacer clic en una barra o en un empleado aplica ese filtro. Cada gráfica tiene "Ver tabla".

Los tableros tienen **tipo** (Proyecto, Sprint, Soporte, Reunión, Otro) y **estado** (Activo, En pausa, Finalizado). El historial empieza a registrarse con la migración V3; de los datos anteriores solo se recupera quién creó cada tablero y nota.

## Usuarios de prueba

| Rol | Email | Contraseña |
|---|---|---|
| Administrador | admin@demo.com | Admin123* |
| Líder | lider@demo.com | Lider123* |
| Usuario | usuario@demo.com | Usuario123* |

## API

| Método | Ruta | Roles |
|---|---|---|
| POST | `/api/auth/login` | público → `{ token, user }` |
| GET | `/api/auth/me` | todos |
| GET | `/api/boards`, `/api/boards/{id}` | todos |
| POST / PUT / DELETE | `/api/boards`, `/api/boards/{id}` | LEADER, ADMIN |
| GET / POST | `/api/boards/{boardId}/notes` | todos |
| PUT | `/api/notes/{id}` | dueño, LEADER, ADMIN |
| PATCH | `/api/notes/{id}/position` | dueño, LEADER, ADMIN |
| DELETE | `/api/notes/{id}` | dueño, LEADER, ADMIN |
| GET | `/api/dashboard?boardId&boardType&boardStatus&userId&from&to` | todos |
| GET | `/api/activity?userId&boardId&action&from&to&page&size` | todos |
| GET / POST | `/api/users` | ADMIN |
| GET / PUT | `/api/users/{id}` | ADMIN |
| PATCH | `/api/users/{id}/password` | ADMIN |
| PATCH | `/api/users/{id}/status?active=true\|false` | ADMIN |
| DELETE | `/api/users/{id}` (desactiva) | ADMIN |

Códigos de error: `400` datos inválidos, `401` sin sesión o usuario inactivo, `403` sin permiso, `404` no existe, `409` regla de negocio (email repetido, desactivarse a sí mismo, etc.). Todos devuelven `{ message }`.

## Ejecutar con Docker

```powershell
docker build -t teamportal .
docker run -d --name teamportal -p 8081:80 -e DB_PASSWORD=CambiaEstaClave -e JWT_SECRET=UnaClaveLargaDeAlMenos32Caracteres -v teamportal-data:/var/lib/postgresql/data teamportal
```

Abrir http://localhost:8081. Las migraciones de Flyway (`backend/src/main/resources/db/migration`) se aplican solas al arrancar.

## Despliegue en AWS EC2 (Ubuntu)

Probado para Ubuntu 26.04 en un `t3.micro`. La imagen se compila en el propio servidor.

**1. Subir el código a GitHub** (desde tu PC). Primero compila localmente para no esperar 20 minutos en el servidor y descubrir un error allá:

```powershell
docker build -t teamportal .
git add .gitattributes Dockerfile .dockerignore README.md backend frontend deploy
git commit -m "Roles, dashboard y despliegue en EC2"
git push origin main
```

**2. En la consola de AWS:**

- *EC2 > Volúmenes*: selecciona el volumen de la instancia > *Modificar volumen* > **20 GB**. Compilar necesita espacio y un t3.micro trae ~8 GB.
- *EC2 > Grupos de seguridad* (el de la instancia) > *Reglas de entrada* > agregar **HTTP, puerto 80, origen 0.0.0.0/0**. No abras el 5432.

**3. En el servidor** (EC2 Instance Connect o SSH):

```bash
git clone https://github.com/karlajdelgado26-star/TABLERO-DE-NOTAS.git
cd TABLERO-DE-NOTAS
sudo bash deploy/ec2/preparar-servidor.sh   # una sola vez: disco, swap de 2 GB y Docker
sudo bash deploy/ec2/desplegar.sh           # compila y levanta la app (10-20 min la primera vez)
```

Al terminar muestra la dirección, por ejemplo `http://18.216.99.24`. Entra con `admin@demo.com` y **cambia la contraseña de los tres usuarios demo** en *Usuarios > Contraseña*.

**Actualizar a una versión nueva:** haz `git push` desde tu PC y en el servidor vuelve a ejecutar `sudo bash deploy/ec2/desplegar.sh` (hace `git pull`, compila y reinicia; los datos se conservan).

**Volver a la versión anterior:**

```bash
sudo docker tag teamportal:anterior teamportal:latest
sudo bash deploy/ec2/desplegar.sh --sin-actualizar --sin-compilar
```

**Datos y secretos:** la base de datos vive en el volumen Docker `teamportal-data`. Las contraseñas generadas están en `/opt/teamportal/teamportal.env`; no borres ese archivo.

**Respaldos:** `sudo bash deploy/ec2/respaldar-bd.sh` guarda un `.sql.gz` en `/opt/teamportal/backups` (conserva 7). El script explica cómo restaurar y cómo programarlo con cron.

**Comandos útiles:**

| Para | Comando |
|---|---|
| Ver logs | `sudo docker logs -f teamportal` |
| Estado | `sudo docker ps` |
| Reiniciar | `sudo docker restart teamportal` |
| Memoria y disco | `free -h && df -h /` |
