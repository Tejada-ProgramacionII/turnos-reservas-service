# Contrato con el servicio de catálogo

Turnos no guarda profesionales ni horarios: los pide al servicio de catálogo cada vez que los necesita. El catálogo es el dueño de la copia local de la cátedra (categorías, profesionales y horarios semanales).

## Qué consume turnos

Una sola operación: la **agenda de un profesional**.

```http
GET {CATALOGO_BASE_URL}/api/profesionales/{id}
Authorization: Bearer <JWT del usuario>
```

La usan dos casos de uso:

| Caso de uso | Para qué |
| --- | --- |
| CU-03 Consultar disponibilidad | Partir los horarios del día en turnos de `duracionTurnoMinutos` y descartar los ocupados según la cátedra. |
| CU-04 Iniciar reserva | Validar que el profesional esté habilitado y que el turno corresponda a su agenda, calcular `horaFin` y copiar nombre y apellido al proceso. |

### Respuesta que lee turnos (`200 OK`)

```json
{
  "id": 201,
  "categoriaId": 3,
  "nombre": "Ana",
  "apellido": "Torres",
  "habilitado": true,
  "horarios": [
    { "diaSemana": "MONDAY", "horaInicio": "09:00:00", "horaFin": "13:00:00", "duracionTurnoMinutos": 30 }
  ],
  "versionCatalogo": 7
}
```

`ProfesionalCatalogoResponse` y `HorarioCatalogoResponse` en `disponibilidad/infrastructure/client/dto` reciben estos campos. `AgendaProfesionalClientMapper` los traduce al modelo `AgendaProfesional` del dominio.

## Autenticación: JWT propagado (D-12 del catálogo)

Turnos **reenvía el JWT del usuario** que hizo el pedido, no usa un token propio entre servicios.

- `JwtUsuarioInterceptor` (en `config`) toma el token del `SecurityContext` (`JwtAuthenticationToken`) y lo pone en `Authorization: Bearer` de cada llamada al catálogo.
- El catálogo lo valida con el **mismo secreto HS512** con el que turnos lo emitió (`JWT_SECRET` tiene que ser igual en los dos servicios).
- Motivo: la app KMP también consulta el catálogo directo con ese JWT (D-01 del catálogo), así que el catálogo igual tiene que validarlo. Un JWT técnico propio sería otra emisión de tokens sin beneficio.

## Configuración

| Propiedad | Variable | Valor por defecto |
| --- | --- | --- |
| `catalogo.base-url` | `CATALOGO_BASE_URL` | ninguno: sin ella el servicio no arranca (`@NotBlank`) |
| `catalogo.connect-timeout` | | 5 s |
| `catalogo.read-timeout` | | 30 s |

El `RestClient` del catálogo se arma en `CatalogoClientConfig`, con el interceptor y los timeouts.

## Errores

`HttpCatalogoClientAdapter` clasifica la respuesta. El caso de uso decide con la clasificación, nunca con el texto del error:

| Respuesta del catálogo | En turnos | Respuesta de turnos |
| --- | --- | --- |
| `200` | `Optional` con la agenda | sigue el caso de uso |
| `404` (no existe el profesional) | `Optional.empty()` | `404` |
| otro `4xx` (por ejemplo, `401` si el JWT no es válido) | `CatalogoRechazoException`: no se reintenta | `502` "Error de integración..." |
| `5xx` (por ejemplo, `503` si el catálogo no tiene una versión aplicada) | `CatalogoNoDisponibleException` | `503` |
| timeout o sin conexión | `CatalogoNoDisponibleException` | `503` |

- Un profesional con `habilitado: false` responde `409` en la disponibilidad y `400` al iniciar una reserva.
- **Reintentos.** La disponibilidad (`DisponibilidadService`) reintenta solo las fallas de "no disponible": hasta 3 intentos con 500 ms entre uno y otro (`disponibilidad.max-intentos` y `disponibilidad.espera-entre-intentos`). Iniciar una reserva no reintenta la consulta al catálogo.
- **D-09.** Si el catálogo no responde, no se arman disponibilidades ni se inician reservas. Consultar, abandonar y cancelar reservas propias sigue funcionando, porque no lo necesitan.

## Qué no consume

- No consume `GET /api/profesionales` ni `GET /api/categorias`: la búsqueda la hace la app directo contra el catálogo (D-01 del catálogo).
- No lee la base ni el Redis del catálogo.
