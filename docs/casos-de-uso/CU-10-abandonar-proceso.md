# CU-10. Abandonar proceso

## Identificación

| Campo | Descripción |
| --- | --- |
| Objetivo | Permitir que el usuario suelte un turno bloqueado antes de que venza, para no retenerlo sin necesidad. |
| Actor principal | Usuario final, desde la app KMP. |
| Actor de apoyo | Servicio de la cátedra (cancelación por `reservationProcessId`). |
| Disparador | El usuario elige abandonar un proceso en curso. |

## Precondiciones

- El usuario inició sesión (CU-02).
- El proceso es del usuario y está pendiente: `INICIADO`, `BLOQUEADO`, `ESPERANDO_TELEFONO` o `TELEFONO_ENVIADO`.

## Flujo principal

1. El usuario pide abandonar el proceso.
2. El servicio comprueba que el proceso sea del usuario autenticado (D-13) y que esté pendiente.
3. El servicio guarda que el usuario pidió abandonarlo (D-04).
4. El servicio pide a la cátedra cancelar el proceso por su `reservationProcessId` (D-16).
5. La cátedra responde que quedó cancelado. El servicio pasa el proceso a `CANCELADO`, con motivo "abandonado por el usuario".
6. El servicio responde al usuario que el turno fue liberado.

## Flujos alternativos

- **1a. El usuario sale de la app o vuelve atrás.** No es un abandono: el proceso sigue y puede retomarlo mientras no venza.
- **2a. El proceso no existe o es de otro usuario.** Se responde "no encontrado" (D-13).
- **2b. El proceso ya está en un estado final.** Se informa su estado; no se llama a la cátedra.
- **2c. El proceso está `CONFIRMADO`.** No es un abandono: corresponde cancelar la reserva (CU-08).
- **2d. El proceso está en `INICIADO` sin `reservationProcessId`.** No hay con qué cancelarlo en la cátedra: se marca como abandonado localmente y el hold, si se creó, vence solo (D-17).
- **4a. La reserva se confirmó mientras el usuario abandonaba.** La misma cancelación la cancela; el proceso queda en `CANCELADO`.
- **4b. La cátedra rechaza la cancelación.** El proceso no cambia; se informa que no se pudo liberar el turno y se reconcilia su estado (CU-07).
- **4c. Timeout o la cátedra no responde.** Se reconcilia consultando el proceso (D-08). Si ya figura cancelado, se sigue en 5; si no, se reintenta un número limitado de veces. Si sigue sin respuesta, se informa que no se pudo confirmar el abandono y CU-09 lo vuelve a revisar.
- **5a. Llega después `AppointmentCancelled` u otro evento del mismo proceso.** No cambia nada: el proceso ya es final (D-05).

## Postcondiciones

- **Éxito:** el hold quedó liberado en la cátedra y el proceso está en `CANCELADO`.
- **Fallo:** el proceso no cambia y el usuario sabe que el abandono no se pudo confirmar.

## Reglas

- **D-04, D-05, D-08, D-13, D-16, D-17.**
- Pedir el abandono no es lo mismo que lograrlo: el proceso pasa a `CANCELADO` recién cuando la cátedra lo confirma.
- Liberar el hold no garantiza que el turno siga libre: otro usuario puede tomarlo enseguida.
- Abandonar no depende del catálogo (D-09).

## Fuentes

- Enunciado: secciones 4.2, 7 y 8.
- Contrato de integración: secciones 9, 12, 13.1 y 18.4.
- Aclaración del profesor en Slack (#all-fi-programacion-2-2026, 29/09/2026): la cancelación de la sección 12 se puede usar para cancelar o abandonar un hold.
