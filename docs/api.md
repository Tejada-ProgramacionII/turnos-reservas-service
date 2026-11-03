# API del servicio de turnos

Base: `http://<host>:8080`. Todo es JSON. La documentación interactiva está en `/swagger-ui.html` (OpenAPI en `/v3/api-docs`).

## Convenciones

- **Autenticación.** Salvo `POST /api/register` y `POST /api/authenticate`, todos los endpoints piden `Authorization: Bearer <id_token>`. Sin token o con uno inválido o vencido la respuesta es `401`, sin cuerpo y con `WWW-Authenticate: Bearer`.
- **Usuario.** El usuario sale siempre del `sub` del token, nunca de un parámetro. Un proceso de otro usuario responde igual que uno inexistente: `404` (D-13).
- **Errores de negocio.** Vienen como `ProblemDetail` (RFC 9457):

  ```json
  { "type": "about:blank", "title": "Conflict", "status": 409, "detail": "El proceso ya terminó", "instance": "/api/reservas/4/abandonar", "estado": "CANCELADO" }
  ```

  Los `409` de reservas agregan `estado`, el estado actual del proceso.
- **Errores de validación** (Bean Validation sobre el cuerpo). Responden `400` con el formato de error de Spring Boot. El campo `errors` dice qué campos fallaron (`spring.web.error.include-binding-errors=always`).

## Usuarios

### `POST /api/register` — registrar usuario (CU-01, público)

```json
{
  "login": "jtejada",
  "password": "una-clave",
  "firstName": "Joaquín",
  "lastName": "Tejada",
  "email": "jtejada@example.com",
  "imageUrl": null,
  "langKey": "es"
}
```

| Campo | Regla |
| --- | --- |
| `login` | obligatorio, 1 a 50, expresión de login de JHipster (`UsuarioRequest.LOGIN_REGEX`) |
| `password` | obligatorio, 4 a 100 |
| `firstName`, `lastName` | opcionales, hasta 50 (para reservar hacen falta, de al menos 2 caracteres) |
| `email` | obligatorio, email válido, 5 a 254 |
| `imageUrl` | opcional, hasta 256 |
| `langKey` | obligatorio, 2 a 10 |

| Código | Cuándo |
| --- | --- |
| `201` | registrado, sin cuerpo |
| `400` | validación, o `detail` "usuario ya registrado" / "email ya registrado" |

### `POST /api/authenticate` — iniciar sesión (CU-02, público)

```json
{ "username": "jtejada", "password": "una-clave" }
```

| Código | Cuándo |
| --- | --- |
| `200` | `{"id_token": "<JWT>"}`; el mismo token va en el header `Authorization: Bearer` de la respuesta |
| `400` | falta `username` o `password` |
| `401` | `detail` "credenciales inválidas" (login inexistente, clave incorrecta o usuario no activado) |

### `GET /api/account` — usuario autenticado

| Código | Cuándo |
| --- | --- |
| `200` | `{"login", "firstName", "lastName", "email", "imageUrl", "langKey", "activated", "authorities": ["ROLE_USER"]}` |
| `401` | sin token válido |
| `404` | el usuario del token ya no existe |

## Disponibilidad

### `GET /api/disponibilidad?profesionalId={id}&fecha={yyyy-MM-dd}` — turnos libres (CU-03)

```json
{
  "profesionalId": 201,
  "fecha": "2026-11-09",
  "turnos": [ { "horaInicio": "09:00:00", "horaFin": "09:30:00" } ]
}
```

Los horarios del día salen del catálogo y se descartan los turnos que la cátedra informa ocupados (confirmados o retenidos). Si la fecha es hoy, también los que ya empezaron.

| Código | Cuándo |
| --- | --- |
| `200` | lista de turnos libres (puede venir vacía) |
| `400` | fecha pasada, o falta un parámetro |
| `404` | el profesional no existe en el catálogo |
| `409` | el profesional está deshabilitado |
| `502` | el catálogo o la cátedra rechazaron el pedido (error de integración) |
| `503` | el catálogo o la cátedra no respondieron después de 3 intentos |

## Reservas

Todas devuelven el proceso con este formato (`ProcesoReservaResponse`):

```json
{
  "id": 4,
  "estado": "ESPERANDO_TELEFONO",
  "profesionalId": 201,
  "profesionalNombre": "Ana",
  "profesionalApellido": "Torres",
  "fecha": "2026-11-09",
  "horaInicio": "09:30:00",
  "horaFin": "10:00:00",
  "expiresAt": "2026-11-03T15:53:13Z",
  "mensajePedido": "Ingrese un numero de telefono para continuar.",
  "motivo": null,
  "canceladaEn": null,
  "puedeAbandonar": true,
  "puedeCancelar": false
}
```

- `puedeAbandonar` es `true` en `INICIADO`, `BLOQUEADO` y `ESPERANDO_TELEFONO`. `puedeCancelar` es `true` solo en `CONFIRMADO`.
- El teléfono, el `holdId`, el `reservationProcessId` y el usuario **no** se exponen.

### `POST /api/reservas` — iniciar reserva (CU-04)

```json
{ "profesionalId": 201, "fecha": "2026-11-09", "horaInicio": "09:30" }
```

Crea el hold en la cátedra y lo confirma. El header `Location` apunta a `/api/reservas/{id}`.

| Código | Cuándo |
| --- | --- |
| `201` | el hold se confirmó: el proceso ya espera el teléfono (o avanzó por Kafka) |
| `202` | la cátedra no respondió: el proceso quedó `INICIADO` o `BLOQUEADO` y lo resuelve la reconciliación (CU-09) |
| `400` | falta un campo, la cuenta no tiene nombre y apellido, el turno ya pasó, no corresponde a la agenda o el profesional está deshabilitado |
| `404` | el profesional no existe en el catálogo |
| `409` | la cátedra rechazó el turno (ocupado, vencido, inválido); el cuerpo trae `procesoId` y `estado` (`FALLIDO` o `VENCIDO`) |
| `502` | error de integración con el catálogo o con la cátedra |
| `503` | el catálogo no respondió |

### `GET /api/reservas?estado={estado}&page={n}&size={m}` — mis reservas (CU-07)

Los procesos del usuario autenticado, primero los más recientes. `estado` es opcional (un valor de `EstadoProceso`). Por defecto `page=0` y `size=20`. Lee los procesos locales sin consultar a la cátedra: el estado se mantiene al día con los eventos de Kafka y con la reconciliación (CU-09, cada minuto).

| Código | Cuándo |
| --- | --- |
| `200` | lista de procesos; el total va en el header `X-Total-Count` |
| `400` | `page < 0`, `size` fuera de 1..100, o `estado` desconocido |

### `GET /api/reservas/{id}` — un proceso (CU-07)

| Código | Cuándo |
| --- | --- |
| `200` | el proceso |
| `404` | no existe o es de otro usuario |

### `POST /api/reservas/{id}/telefono` — informar el teléfono (CU-05)

```json
{ "telefono": "+54 9 261 555-1234" }
```

Se quitan espacios, guiones, paréntesis y puntos, y el resultado tiene que cumplir `^\+?[0-9]{7,15}$` (D-11). El teléfono se publica en Kafka (`AdditionalInformationSubmitted`) y el resultado llega después (CU-06).

| Código | Cuándo |
| --- | --- |
| `202` | enviado: el proceso queda `TELEFONO_ENVIADO` |
| `400` | vacío, o no cumple la regla (el `detail` explica el formato); el proceso no cambia |
| `404` | no existe o es de otro usuario |
| `409` | la cátedra todavía no pidió el teléfono, el proceso no está en `ESPERANDO_TELEFONO` o venció `expiresAt` |
| `503` | Kafka no confirmó el envío; el proceso sigue en `ESPERANDO_TELEFONO` y se puede reintentar |

### `POST /api/reservas/{id}/abandonar` — abandonar un proceso (CU-10)

Sin cuerpo. No llama a la cátedra: el proceso queda `CANCELADO` y el hold vence solo (D-16).

| Código | Cuándo |
| --- | --- |
| `200` | el proceso `CANCELADO`, con motivo "Abandonado por el usuario" |
| `404` | no existe o es de otro usuario |
| `409` | `TELEFONO_ENVIADO` (esperar el resultado), `CONFIRMADO` (usar la cancelación) o estado final |

### `POST /api/reservas/{id}/cancelar` — cancelar una reserva confirmada (CU-08)

```json
{ "motivo": "No puedo asistir" }
```

El cuerpo es opcional; `motivo` admite hasta 500 caracteres. Sin motivo se usa "Cancelada por el usuario".

| Código | Cuándo |
| --- | --- |
| `200` | el proceso `CANCELADO`, con `canceladaEn` |
| `400` | `motivo` de más de 500 caracteres |
| `404` | no existe o es de otro usuario |
| `409` | el proceso no está `CONFIRMADO` |
| `502` | la cátedra rechazó la cancelación: la reserva sigue confirmada |
| `503` | la cátedra no respondió: la reserva sigue confirmada y la reconciliación la revisa (CU-09) |
