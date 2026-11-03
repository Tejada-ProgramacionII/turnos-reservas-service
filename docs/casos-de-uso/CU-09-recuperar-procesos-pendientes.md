# CU-09. Recuperar procesos pendientes

## Identificación

| Campo | Descripción |
| --- | --- |
| Objetivo | Que ningún proceso quede trabado después de un reinicio, un timeout o un evento perdido. |
| Actor principal | Ninguno: es un proceso automático del servicio. |
| Actor de apoyo | Servicio de la cátedra (listado de reservas, contrato 11). |
| Disparadores | 1) El servicio arranca. 2) Se cumple el intervalo de verificación periódica (1 minuto por defecto, configurable). |

## Precondiciones

- No hay otra recuperación en curso: se corre una vuelta a la vez (supuesto: una sola instancia del servicio).

## Flujo principal

1. El servicio busca los procesos pendientes (`INICIADO`, `BLOQUEADO`, `ESPERANDO_TELEFONO`, `TELEFONO_ENVIADO`) y los `CONFIRMADO` con turno de hoy en adelante, por si una cancelación (CU-08) dio timeout o se perdió el `AppointmentCancelled`.
2. Para cada usuario con procesos que tienen `reservationProcessId`, consulta a la cátedra sus reservas (`GET /api/appointments?externalPatientId=...`, página por página). No se usan los filtros `from` y `to`: con ellos la cátedra responde 500.
3. Antes de decidir, el servicio relee cada proceso: si un evento de Kafka lo movió mientras tanto, no lo toca (D-05). Después lo actualiza según la reserva con su `reservationProcessId`, siempre por la máquina de estados y guardando el motivo:

| Estado local | La cátedra informa | Estado local nuevo |
| --- | --- | --- |
| `BLOQUEADO`, `ESPERANDO_TELEFONO` o `TELEFONO_ENVIADO` | `CONFIRMED` | `CONFIRMADO`, con el `reservationId` |
| ídem | `CANCELLED` | `CANCELADO` |
| ídem | `FAILED` | `VENCIDO` si pasó `expiresAt`; si no, `FALLIDO` |
| ídem | `PHONE_PENDING` y venció | `VENCIDO` (D-14) |
| `BLOQUEADO` | `PHONE_PENDING` y no venció | `ESPERANDO_TELEFONO` |
| `ESPERANDO_TELEFONO` o `TELEFONO_ENVIADO` | `PHONE_PENDING` y no venció | Sigue igual |
| `BLOQUEADO`, `ESPERANDO_TELEFONO` o `TELEFONO_ENVIADO` | No aparece y venció | `VENCIDO` |
| ídem | No aparece y no venció | Sigue igual |
| `CONFIRMADO` | `CANCELLED` | `CANCELADO` |
| `CONFIRMADO` | Otra cosa o no aparece | Sigue igual |

4. Los procesos en `INICIADO` (timeout al pedir el hold, D-17) no se consultan: sin `holdId` no hay cómo identificarlos, y un hold `HELD` de la cuenta técnica no prueba que sea de este proceso (criterio del profesor, Slack 30/10/2026). Pasados 10 minutos (la vigencia de un hold) desde `creadoEn`, pasan a `FALLIDO` con motivo "Resultado incierto al crear el hold": si el hold se llegó a crear, ya venció solo.
5. El servicio registra cuántos procesos revisó y cuántos cambiaron.

## Flujos alternativos

- **1a. Ya hay una recuperación en curso** (por ejemplo, el arranque y la primera vuelta periódica coinciden). La nueva vuelta se saltea y se registra.
- **2a. La cátedra no responde o rechaza la consulta.** Se registra; los procesos de ese usuario no se tocan y se sigue con los demás. La próxima vuelta vuelve a intentar.
- **3a. Durante la recuperación llega un evento de Kafka del mismo proceso.** Gana la transición que se guarde primero; la otra encuentra el estado ya actualizado y no lo retrocede (D-05).
- **3b. El proceso pasa a `ESPERANDO_TELEFONO` por la recuperación.** No tiene `requestEventId`, que solo llega con `AdditionalInformationRequested`: el teléfono todavía no se puede informar (CU-05 8b) hasta que llegue el pedido por Kafka.
- **3c. La cátedra informa un estado desconocido.** No se decide nada con él.

## Postcondiciones

- Cada proceso revisado quedó en su estado real o, si no se pudo verificar, sin cambios hasta el próximo disparador.
- Ningún proceso vencido queda esperando indefinidamente.

## Reglas

- **D-05, D-14, D-17.**
- Los eventos de Kafka que llegaron mientras el servicio estaba caído se procesan al reconectar (el offset no se había confirmado); la recuperación cubre lo que no llegue por ahí.
- Los reintentos son limitados (uno por vuelta) y quedan registrados (contrato 18.4).

## Fuentes

- Enunciado: secciones 4.2, 5 (punto 15) y 8.
- Contrato de integración: secciones 11, 18.3 y 18.4.
- Prueba contra la cátedra del 03/11/2026: el listado con `from` y `to` responde 500; sin ellos, filtrando por `externalPatientId`, responde bien.
