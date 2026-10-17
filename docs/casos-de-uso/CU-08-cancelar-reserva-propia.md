# CU-08. Cancelar reserva propia

## Identificación

| Campo | Descripción |
| --- | --- |
| Objetivo | Cancelar una reserva confirmada del usuario. |
| Actor principal | Usuario final, desde la app KMP. |
| Actor de apoyo | Servicio de la cátedra (cancelación). |
| Disparador | El usuario elige cancelar una de sus reservas. |

## Precondiciones

- El usuario inició sesión (CU-02).
- La reserva es del usuario y está `CONFIRMADO`.

## Flujo principal

1. El usuario pide cancelar una reserva, opcionalmente con un motivo (hasta 500 caracteres).
2. El servicio busca el proceso y comprueba que sea del usuario autenticado (D-13).
3. El servicio comprueba que el proceso esté `CONFIRMADO`.
4. El servicio pide a la cátedra cancelar la reserva por su `reservationProcessId`.
5. La cátedra responde `CANCELLED`. El servicio guarda el motivo y la fecha de cancelación y pasa el proceso a `CANCELADO`.
6. El servicio responde al usuario que la reserva quedó cancelada.

## Flujos alternativos

- **2a. El proceso no existe o es de otro usuario.** Se responde "no encontrado" (D-13).
- **3a. El proceso ya está `CANCELADO`.** Se responde su estado actual, sin volver a llamar a la cátedra.
- **3b. El proceso está pendiente** (`BLOQUEADO`, `ESPERANDO_TELEFONO` o `TELEFONO_ENVIADO`). Corresponde abandonarlo (CU-10).
- **3c. El proceso está en otro estado final.** Se rechaza informando su estado.
- **4a. La cátedra responde `RESERVATION_NOT_CANCELLABLE`.** Se rechaza y se reconcilia el estado del proceso (CU-07).
- **4b. Timeout o la cátedra no responde.** Se reconcilia consultando la reserva (D-08). Si ya figura cancelada, se sigue en 5; si no, se reintenta un número limitado de veces (la cancelación de la cátedra es idempotente). Si sigue sin respuesta, se informa que no se pudo cancelar y el proceso no cambia.
- **5a. Llega después `AppointmentCancelled` por Kafka.** No cambia nada (CU-06, flujo 4b).

## Postcondiciones

- **Éxito:** la reserva figura cancelada en la cátedra y el proceso local está en `CANCELADO`.
- **Fallo:** el proceso no cambia.

## Reglas

- **D-05, D-08, D-13.**
- La propiedad se valida localmente **antes** de llamar a la cátedra: la cátedra solo conoce la cuenta técnica (contrato 11 y 12).
- Cancelar no depende del catálogo: funciona aunque el catálogo no esté disponible (D-09).

## Fuentes

- Enunciado: secciones 4.2, 5 (punto 14), 8 y 9.
- Contrato de integración: secciones 12, 13.1 y 15.10.
