# Pruebas

## Cómo correrlas

Hace falta Java 25 (Temurin). No hace falta Docker, ni Kafka, ni la cátedra: los tests usan H2 en memoria y valores ficticios.

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)   # macOS; en Linux, la ruta del JDK 25

./mvnw -B clean verify                               # todos los tests y el reporte de cobertura
./mvnw -B test -Dtest=ReconciliarProcesosUseCaseImplTest   # una clase
./mvnw -B test -Dtest='Reserva*'                     # varias clases por patrón
```

- **Cobertura:** `verify` genera el reporte de JaCoCo en `target/site/jacoco/index.html`, y el XML que lee SonarCloud. Quedan fuera la clase `*Application` y el código que genera Lombok.
- **CI:** cada push y cada PR a `develop`, `testing` y `main` corre `./mvnw -B -ntp clean verify` en GitHub Actions (`.github/workflows/sonar.yml`) y publica el análisis en SonarCloud.

### Entorno de los tests

`src/test/resources/application.properties` deja el entorno armado:

- **H2 en memoria con las mismas migraciones de Flyway** que la base real, y `ddl-auto=validate`: si una entidad no coincide con las tablas, el contexto no levanta.
- Secreto JWT de prueba y URLs ficticias del catálogo y la cátedra; los tests nunca las llaman de verdad.
- `spring.kafka.listener.auto-startup=false`: el consumidor de Kafka no arranca.
- `reconciliacion.habilitada=false`: la reconciliación no corre sola.
- `disponibilidad.espera-entre-intentos=0s`: los reintentos no duermen.

## Qué cubre cada tipo

Los paquetes de test son los mismos que los de `main`.

| Tipo | Cómo | Ejemplos |
| --- | --- | --- |
| **Dominio** | JUnit puro, muchos parametrizados | `EstadoProcesoTest`: todas las transiciones, válidas e inválidas. `ProcesoReservaTest`. `TelefonoTest`: la regla D-11 con casos válidos e inválidos. |
| **Casos de uso** | `@ExtendWith(MockitoExtension.class)` con los puertos mockeados y un `Clock` fijo | Cada flujo principal y alternativo del CU. `IniciarReservaUseCaseImplTest` usa un repositorio en memoria que guarda copias, para comprobar qué estado se guardó **antes** de cada llamada a la cátedra (D-04). `ReconciliarProcesosUseCaseImplTest` tiene una tabla de reglas parametrizada y una prueba del lock con dos hilos. |
| **Servicios (fachadas)** | Mockito | Que deleguen en el caso de uso y conviertan `Optional.empty()` en la excepción "no encontrado". `DisponibilidadServiceTest`: los reintentos. |
| **Mappers** | `new` y comparación campo por campo, incluido `null` | Persistencia, web, cliente REST y mensajería. |
| **Adaptadores de persistencia** | Spring Data mockeado y el mapper real; las consultas con filtros, con `@SpringBootTest` + `@Transactional` sobre H2 | `JpaProcesoReservaRepositoryAdapterTest`, `JpaProcesoReservaRepositoryAdapterConsultaTest`. |
| **Clientes REST** | Un `HttpServer` del JDK que responde los **JSON de ejemplo del contrato**. (`MockRestServiceServer` no sirve: lo pisa el `requestFactory` de los timeouts.) | Lo que se manda (método, path, query, cuerpo), la traducción de la respuesta y la clasificación de errores: 4xx → rechazo con su `code`, 5xx/timeout/conexión → no disponible. Que el JWT y la URL no aparezcan en los mensajes. |
| **Mensajería** | Consumidor con los JSON del contrato (15.4, 15.6 a 15.10) tal cual; productor con `KafkaTemplate` mockeado | Campos extra, tipos desconocidos, JSON roto, duplicados. La serialización de `AdditionalInformationSubmitted` se compara en modo estricto (`JSONAssert`) con el ejemplo del contrato (15.5). Rechazo, timeout e interrupción del envío. |
| **Controllers** | `MockMvcBuilders.standaloneSetup` con un `Jwt` en el `SecurityContext` | Cada código de respuesta (2xx, 400, 404, 409 con `estado`, 502, 503) y que la respuesta no exponga datos internos (`holdId`, usuario, teléfono). |
| **Configuración y seguridad** | `@SpringBootTest` y tests de cada `@Configuration` y adaptador de seguridad | `SecurityConfigTest`: sin token → 401, token inválido → 401, token válido pasa, el registro es público. `JwtUsuarioInterceptorTest`: propaga el JWT al catálogo. `CatalogoClientConfigTest` y `CatedraClientConfigTest`: URL base y token de cada `RestClient`. `ClockConfigTest`: zona horaria de Mendoza. `JwtTokenGeneratorAdapterTest` (claims y HS512), `Argon2PasswordHasherAdapterTest` (Argon2id) y `UsuarioRequestTest` (validaciones del registro). |
| **Scheduler** | `ApplicationContextRunner` | `ReconciliacionSchedulerTest`: corre cada intervalo, corre al arrancar, no existe si está desactivado, y un error no corta las vueltas. |
| **Contexto completo** | `@SpringBootTest` | `TurnosReservasApplicationTests`: la aplicación levanta con las migraciones y la configuración reales. |

## Pruebas manuales de integración

Lo que no se puede automatizar sin la cátedra se prueba a mano antes de cada PR, y el resultado queda en la sección "Cómo se probó" del PR:

- **Contra Postgres:** `docker compose up -d` y el servicio con `./mvnw spring-boot:run`.
- **Kafka:** con un broker local en Docker (`apache/kafka`). Se publican los eventos de la cátedra con el formato del contrato y se lee el topic de teléfono, porque la cátedra todavía no publica eventos.
- **Contra la cátedra real** (por ZeroTier, con el JWT técnico en una variable de entorno):
  - iniciar reservas (CU-04);
  - consultar la disponibilidad (CU-03);
  - la reconciliación (CU-09), con el estado de la base antes y después.
