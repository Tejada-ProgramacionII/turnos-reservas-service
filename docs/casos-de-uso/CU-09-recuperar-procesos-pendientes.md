# CU-09. Recuperar procesos pendientes

## Identificación

| Campo | Descripción |
| --- | --- |
| Objetivo | Que ningún proceso quede trabado después de un reinicio, un timeout o un evento perdido. |
| Actor principal | Ninguno: es un proceso automático del servicio. |
| Actor de apoyo | Servicio de la cátedra (listado de reservas). |
| Disparadores | 1) El servicio arranca. 2) Se cumple el intervalo de verificación periódica. |

## Precondiciones

- Ninguna.

## Flujo principal

1. El servicio busca los procesos que no están en estado final ni `CONFIRMADO`.
2. Para cada uno con `reservationProcessId`, consulta a la cátedra la reserva correspondiente.
3. El servicio actualiza el proceso según lo que informa la cátedra (D-05, D-14):

| La cátedra informa | Estado local nuevo |
| --- | --- |
| `CONFIRMED` | `CONFIRMADO` |
| `CANCELLED` | `CANCELADO` |
| `FAILED` | `FALLIDO` |
| `PHONE_PENDING` y no venció | Sigue igual (`ESPERANDO_TELEFONO` o `TELEFONO_ENVIADO`). Si estaba en `BLOQUEADO`, pasa a `ESPERANDO_TELEFONO`. |
| No existe y no venció | `BLOQUEADO` sigue igual: se retoma la confirmación de CU-04 (flujo 6d). |
| Cualquier estado no confirmado y ya pasó `expiresAt` | `VENCIDO` |

4. Los procesos en `INICIADO` sin `reservationProcessId` (timeout al pedir el hold, o el servicio se cortó antes de recibirlo) siguen igual hasta que pasa la vigencia máxima de un hold desde el intento. Después pasan a `FALLIDO`: si el hold se llegó a crear, ya venció solo en la cátedra (D-17).
5. El servicio registra cuántos procesos revisó y cuántos cambiaron.

## Flujos alternativos

- **2a. La cátedra no responde.** Se reintenta un número limitado de veces; si sigue fallando, los procesos no se tocan, salvo los que superaron `expiresAt` por un margen, que pasan a `VENCIDO`. El próximo disparador vuelve a intentar.
- **3a. Durante la recuperación llega un evento Kafka del mismo proceso.** Gana la transición válida que se guarde primero; la otra encuentra el estado ya actualizado y no lo retrocede (D-05).

## Postcondiciones

- Cada proceso revisado quedó en su estado real o, si no se pudo verificar, sin cambios hasta el próximo disparador.
- Ningún proceso vencido queda esperando indefinidamente.

## Reglas

- **D-04, D-05, D-08, D-14, D-17.**
- Los eventos Kafka que llegaron mientras el servicio estaba caído se procesan al reconectar (el offset no se había confirmado); la recuperación cubre lo que no llegue por ahí.
- Los reintentos son limitados y quedan registrados (contrato 18.4).

## Fuentes

- Enunciado: secciones 4.2, 5 (punto 15) y 8.
- Contrato de integración: secciones 11, 18.3 y 18.4.
