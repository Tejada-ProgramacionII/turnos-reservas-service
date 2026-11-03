# turnos-reservas-service
  Servicio de turnos y reservas — Spring Boot. Disponibilidad, holds, confirmación asincrónica por Kafka y gestión de reservas por usuario.

## Levantar con Docker

Requisitos: **Docker** (Docker Desktop en macOS/Windows) y **ZeroTier** conectado a la red de la cátedra.

1. Copiar el archivo de variables y completarlo con los valores reales de la cuenta técnica (ver [Configuración](#configuración)). El `.env` no se commitea.

   ```bash
   cp .env.example .env
   ```

### Turnos solo

Levanta la base (`turnos-db`, puerto 5433) y el servicio en `http://localhost:8081` (Swagger en `/swagger-ui/index.html`). El servicio espera a que la base esté sana y aplica las migraciones. Con `compose.yaml` el catálogo tiene que estar corriendo en el host, en el 8080 (`CATALOGO_BASE_URL=http://host.docker.internal:8080`).

```bash
docker compose up -d --build
docker compose logs -f turnos
docker compose down          # los datos quedan en el volumen turnos-db-data; -v también lo borra
```

El nombre del proyecto es fijo (`turnos-reservas-service`), así que reutiliza el mismo contenedor y volumen de `turnos-db` aunque se levante desde otra carpeta.

### Todo junto (catálogo + turnos)

`compose.integrado.yaml` levanta el catálogo, turnos y sus dos bases, con los repos como carpetas hermanas (`../catalogo-sincronizacion-service`, que necesita su propio `.env`). Turnos llega al catálogo por nombre (`http://catalogo:8080`) en una red compartida. Usa volúmenes propios (nunca la misma base que los compose de cada servicio) y no publica las bases en el host.

```bash
docker compose down                                   # liberar el 8081 si turnos solo estaba arriba
docker compose -f compose.integrado.yaml up -d --build
docker compose -f compose.integrado.yaml down
```

Quedan en `http://localhost:8080` (catálogo) y `http://localhost:8081` (turnos). Si esos puertos están ocupados, se cambian con `CATALOGO_PORT` y `TURNOS_PORT`.

La imagen se compila en dos etapas (JDK para compilar, solo el JRE para correr) y corre con un usuario sin privilegios. Los tests no corren en el build de la imagen: los corre el CI. Las redes (`10.21.0.0/24` y `10.22.0.0/24` en el integrado) quedan fuera de la `172.17.x` de la cátedra.

### Limitación conocida: Docker Desktop en macOS con ZeroTier

En macOS los contenedores no llegan a la cátedra por ZeroTier (`172.17.201.1`): el servicio arranca y atiende lo que no depende de ella (registro, login, consultas locales), pero las llamadas a la cátedra fallan y Kafka queda reintentando la conexión. Mientras tanto, se puede levantar solo la base con Docker y correr el servicio en el host:

```bash
docker compose up -d turnos-db
set -a && source .env && set +a && SERVER_PORT=8081 ./mvnw spring-boot:run
```

## Base de datos y migraciones

- Base local: PostgreSQL con Docker Compose (`docker compose up -d turnos-db` para levantar solo la base y correr el servicio con `./mvnw spring-boot:run`).
- El esquema lo administra **Flyway**: al arrancar, el servicio aplica en orden las migraciones de `src/main/resources/db/migration` que todavía no corrieron y las registra en la tabla `flyway_schema_history`.
- Las migraciones se nombran `V<número>__<descripción>.sql`. Una migración ya aplicada no se modifica: cada cambio va en un archivo nuevo.
- Hibernate no crea tablas (`ddl-auto=validate`): solo verifica que las entidades coincidan con el esquema.
- Los tests usan H2 en memoria con las mismas migraciones.

## Configuración

- `CATALOGO_BASE_URL` (obligatoria): URL base de `catalogo-sincronizacion-service`, al que turnos consulta la agenda vigente de un profesional con el JWT del usuario. Si falta, el servicio no arranca. En local, con los dos servicios en la misma máquina, el catálogo usa el 8080 y turnos tiene que ir en otro puerto:

  ```bash
  export CATALOGO_BASE_URL=http://localhost:8080
  SERVER_PORT=8081 ./mvnw spring-boot:run
  ```

- `JWT_SECRET` (obligatoria): secreto en Base64, de al menos 64 bytes, con el que se firman los JWT de usuario (HS512). No tiene valor por defecto y nunca se guarda en el repositorio. Para generar uno y usarlo en la terminal actual:

  ```bash
  export JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
  ```

- `CATEDRA_BASE_URL` y `CATEDRA_JWT` (obligatorias): URL base de la API REST de la cátedra y JWT técnico de la cuenta del proyecto (dura un año). Si falta alguna, el servicio no arranca. Sus valores se reciben por otro canal y **no se publican** en el repositorio, en los logs ni en la documentación:

  ```bash
  export CATEDRA_BASE_URL=<url-de-la-catedra>
  export CATEDRA_JWT=<jwt-tecnico>
  ```

  Los timeouts tienen valores por defecto (5 s de conexión y 30 s de lectura) y se pueden cambiar con `CATEDRA_CONNECT_TIMEOUT` y `CATEDRA_READ_TIMEOUT` (por ejemplo, `10s`).

- `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_TOPIC_ACCIONES` y `KAFKA_GRUPO` (obligatorias): broker de Kafka de la cátedra, topic por el que llegan el pedido de teléfono y los resultados de las reservas (`kafkaAppointmentActionsTopic` de la integración técnica) y consumer group de este servicio. Si falta alguna, el servicio no arranca. Turnos usa un grupo propio (por ejemplo, el sugerido con el sufijo `-turnos`) para no compartir rebalanceos con el catálogo:

  ```bash
  export KAFKA_BOOTSTRAP_SERVERS=<broker-de-la-catedra>
  export KAFKA_TOPIC_ACCIONES=alumnos.turnos.acciones.<groupId>
  export KAFKA_GRUPO=alumnos-<groupId>-turnos
  ```

- Los tests no necesitan estas variables: usan valores de prueba propios en `src/test/resources/application.properties`.
