# CU-10. Abandonar proceso

## Identificación

| Campo | Descripción |
| --- | --- |
| Objetivo | Permitir que el usuario deje un proceso de reserva en curso que ya no quiere continuar. |
| Actor principal | Usuario final, desde la app KMP. |
| Actor de apoyo | Ninguno: no se llama a la cátedra (D-16). |
| Disparador | El usuario elige abandonar un proceso en curso. |

## Precondiciones

- El usuario inició sesión (CU-02).
- El proceso es del usuario y está pendiente: `INICIADO`, `BLOQUEADO`, `ESPERANDO_TELEFONO` o `TELEFONO_ENVIADO`.

## Flujo principal

1. El usuario pide abandonar el proceso.
2. El servicio comprueba que el proceso sea del usuario autenticado (D-13) y que esté pendiente.
3. El servicio pasa el proceso a `CANCELADO`, con motivo "Abandonado por el usuario".
4. El servicio responde con el proceso cancelado. El hold de la cátedra, si existe, vence solo (D-16).

## Flujos alternativos

- **1a. El usuario sale de la app o vuelve atrás.** No es un abandono: el proceso sigue y puede retomarlo mientras no venza.
- **2a. El proceso no existe o es de otro usuario.** Se responde "no encontrado" (D-13).
- **2b. El proceso ya está en un estado final.** Se rechaza informando su estado.
- **2c. El proceso está `CONFIRMADO`.** No es un abandono: se rechaza indicando que corresponde cancelar la reserva (CU-08).
- **3a. Llega después un evento de la cátedra del mismo proceso** (por ejemplo, `AppointmentProcessExpired`). No cambia nada: el proceso ya es final (D-05).

## Postcondiciones

- **Éxito:** el proceso está en `CANCELADO` y el usuario puede iniciar otra reserva.
- **Fallo:** el proceso no cambia.

## Reglas

- **D-05, D-13, D-16.**
- Abandonar no libera el turno en el momento: queda bloqueado en la cátedra hasta que vence el hold (10 minutos).
- Abandonar no depende del catálogo (D-09) ni de la cátedra.

## Fuentes

- Enunciado: secciones 4.2, 7 y 8.
- Contrato de integración: secciones 9, 12 y 13.1.
- Prueba contra la cátedra del 27/10/2026: cancelar un hold sin confirmar responde 404 `RESERVATION_NOT_FOUND`, y una reserva `PHONE_PENDING` no es cancelable.
