# turnos-reservas-service
  Servicio de turnos y reservas — Spring Boot. Disponibilidad, holds, confirmación asincrónica por Kafka y gestión de reservas por usuario.

## Base de datos y migraciones

- Base local: PostgreSQL con Docker Compose (`docker compose up -d`).
- El esquema lo administra **Flyway**: al arrancar, el servicio aplica en orden las migraciones de `src/main/resources/db/migration` que todavía no corrieron y las registra en la tabla `flyway_schema_history`.
- Las migraciones se nombran `V<número>__<descripción>.sql`. Una migración ya aplicada no se modifica: cada cambio va en un archivo nuevo.
- Hibernate no crea tablas (`ddl-auto=validate`): solo verifica que las entidades coincidan con el esquema.
- Los tests usan H2 en memoria con las mismas migraciones.
