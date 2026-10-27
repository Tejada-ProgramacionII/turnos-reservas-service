# turnos-reservas-service
  Servicio de turnos y reservas — Spring Boot. Disponibilidad, holds, confirmación asincrónica por Kafka y gestión de reservas por usuario.

## Base de datos y migraciones

- Base local: PostgreSQL con Docker Compose (`docker compose up -d`).
- El esquema lo administra **Flyway**: al arrancar, el servicio aplica en orden las migraciones de `src/main/resources/db/migration` que todavía no corrieron y las registra en la tabla `flyway_schema_history`.
- Las migraciones se nombran `V<número>__<descripción>.sql`. Una migración ya aplicada no se modifica: cada cambio va en un archivo nuevo.
- Hibernate no crea tablas (`ddl-auto=validate`): solo verifica que las entidades coincidan con el esquema.
- Los tests usan H2 en memoria con las mismas migraciones.

## Configuración

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

- Los tests no necesitan estas variables: usan valores de prueba propios en `src/test/resources/application.properties`.
