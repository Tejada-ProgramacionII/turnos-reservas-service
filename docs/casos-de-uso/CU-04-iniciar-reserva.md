# CU-04. Iniciar reserva

## Identificación

| Campo | Descripción |
| --- | --- |
| Objetivo | Bloquear temporalmente el turno elegido y confirmarlo inicialmente, para que la cátedra pida el teléfono. |
| Actor principal | Usuario final, desde la app KMP. |
| Actores de apoyo | Servicio de catálogo (validación vigente). Servicio de la cátedra (hold y confirmación inicial). |
| Disparador | El usuario confirma el turno elegido en la disponibilidad (CU-03). |

## Precondiciones

- El usuario inició sesión (CU-02).

## Flujo principal

1. El usuario envía profesional, fecha y hora de inicio.
2. El servicio pide al catálogo los datos vigentes del profesional y comprueba que esté habilitado y que el turno salga de un horario habilitado (CU-05 del catálogo).
3. El servicio guarda un proceso de reserva nuevo en estado `INICIADO`, asociado al usuario autenticado, con los datos del turno y los datos descriptivos del profesional (D-04).
4. El servicio pide a la cátedra un hold para el turno, enviando el identificador estable del usuario (D-03).
5. La cátedra responde con `holdId`, `reservationProcessId` y `expiresAt`. El servicio los guarda y pasa el proceso a `BLOQUEADO`.
6. El servicio confirma inicialmente el hold ante la cátedra, enviando el identificador del usuario y su nombre y apellido.
7. La cátedra responde que espera el teléfono. El servicio pasa el proceso a `ESPERANDO_TELEFONO`.
8. El servicio responde al usuario con el proceso, su estado y hasta cuándo tiene tiempo para completarlo.

## Flujos alternativos

- **2a. El profesional no existe, está deshabilitado o el turno no corresponde a su agenda.** Se rechaza sin crear el proceso.
- **2b. El catálogo no responde.** No se inicia la reserva (D-09).
- **4a. El turno ya está bloqueado o reservado** (`SLOT_ALREADY_HELD`, `SLOT_ALREADY_RESERVED`). El proceso pasa a `FALLIDO` y se avisa que el turno ya no está libre.
- **4b. La cátedra rechaza el turno** (`PROFESSIONAL_NOT_FOUND`, `PROFESSIONAL_DISABLED`, `INVALID_SLOT`). El proceso pasa a `FALLIDO` con ese motivo.
- **4c. Timeout o la cátedra no responde al pedir el hold.** No se sabe si el hold se creó: el proceso queda en `INICIADO` con resultado incierto y no se repite el pedido (D-17). Se avisa al usuario que el resultado está pendiente; CU-09 lo cierra.
- **6a. El hold venció** (`HOLD_EXPIRED`). El proceso pasa a `VENCIDO`.
- **6b. La cátedra rechaza la confirmación** (`HOLD_NOT_FOUND`, `SLOT_ALREADY_RESERVED`, otros). El proceso pasa a `FALLIDO` con ese motivo.
- **6c. La confirmación ya estaba hecha** (`RESERVATION_PROCESS_ALREADY_CONFIRMED`). Se toma como éxito y sigue en 7 (D-07).
- **6d. Timeout o la cátedra no responde al confirmar.** El proceso queda en `BLOQUEADO`. Se reconcilia consultando la reserva por su `reservationProcessId` (D-08): si existe, se sigue en 7; si no existe y el hold no venció, se reintenta la confirmación un número limitado de veces. Si no se resuelve, queda para CU-09.
- **El pedido de teléfono llega por Kafka antes que la respuesta REST.** El proceso igual llega a `ESPERANDO_TELEFONO` sin duplicarse (ver CU-05).

## Postcondiciones

- **Éxito:** existe un proceso del usuario en `ESPERANDO_TELEFONO`, con `holdId`, `reservationProcessId` y `expiresAt` guardados.
- **Fallo:** el proceso queda en un estado final con su motivo, o en `BLOQUEADO` para que lo resuelva CU-09.

## Reglas

- **D-03, D-04, D-07, D-08, D-09, D-17.**
- Mientras el proceso no tenga respuesta, el usuario puede abandonarlo (CU-10).
- El hold tiene vigencia limitada: guardarlo localmente no la extiende (contrato 9).
- El nombre y apellido que se envían a la cátedra salen del usuario autenticado, no de la petición.
- Los datos del profesional guardados en el proceso son historial: no se usan como catálogo vigente (enunciado 4.2).

## Fuentes

- Enunciado: secciones 4.2, 5 (puntos 9 y 10), 7 y 8.
- Contrato de integración: secciones 9, 10, 13.1, 17 y 18.4.
