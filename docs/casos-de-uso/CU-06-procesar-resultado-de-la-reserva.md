# CU-06. Procesar resultado de la reserva

## Identificación

| Campo | Descripción |
| --- | --- |
| Objetivo | Reflejar en el proceso local el resultado que informa la cátedra por Kafka. |
| Actor principal | Ninguno: es un proceso automático del servicio. |
| Actor de apoyo | Servicio de la cátedra (topic de acciones de turnos). |
| Disparador | Llega `AppointmentConfirmed`, `AdditionalInformationRejected`, `AppointmentProcessExpired`, `AppointmentProcessInvalid` o `AppointmentCancelled`. |

## Precondiciones

- El servicio está suscripto al topic de acciones de su integración técnica.

## Flujo principal

1. El servicio recibe el evento.
2. Si su `eventId` ya fue procesado, lo da por procesado y termina (D-06).
3. El servicio busca el proceso por `reservationProcessId`.
4. El servicio aplica el evento según su tipo, siempre que la transición esté permitida (D-05):

| Evento | Estado nuevo | Qué se guarda |
| --- | --- | --- |
| `AppointmentConfirmed` | `CONFIRMADO` | `reservationId` y fecha de confirmación. |
| `AdditionalInformationRejected` | `ESPERANDO_TELEFONO` | Motivo y mensaje del rechazo, `expiresAt`. |
| `AppointmentProcessExpired` | `VENCIDO` | Motivo. |
| `AppointmentProcessInvalid` | `INVALIDO` | Motivo. |
| `AppointmentCancelled` | `CANCELADO` | Motivo y fecha de cancelación. |

5. El servicio registra el `eventId` como procesado junto con el cambio de estado, en una misma operación.
6. El servicio da el evento por procesado. La app ve el nuevo estado la próxima vez que consulta (CU-07).

## Flujos alternativos

- **2a. Evento duplicado.** No se modifica nada (contrato 18.1).
- **3a. No existe el proceso.** Se registra y se da por procesado.
- **4a. El proceso ya está en un estado final, o el evento intenta retrocederlo** (por ejemplo, un rechazo de teléfono después de la confirmación). Se registra y se ignora (D-05).
- **4b. Llega el mismo resultado que ya se obtuvo por REST** (por ejemplo, `AppointmentCancelled` después de cancelar en CU-08). El estado ya es ese: solo se registra el `eventId`.
- **4c. `AppointmentProcessInvalid`.** Además de marcarlo `INVALIDO`, el servicio puede reconciliar el proceso por REST para conocer el estado persistido en la cátedra.
- **4d. Tipo de evento desconocido o campos adicionales.** Los campos desconocidos se ignoran; un tipo desconocido se registra y se da por procesado.
- **5a. Falla el guardado.** No se da el evento por procesado: Kafka lo vuelve a entregar y se procesa de nuevo.

## Postcondiciones

- **Éxito:** el proceso refleja el resultado y el evento queda registrado como procesado.
- **Fallo:** no cambió nada y el evento se reprocesa.

## Reglas

- **D-05, D-06.**
- El estado que llega por REST y el que llega por Kafka pueden llegar en distinto orden: el proceso converge sin duplicar reservas ni retroceder (enunciado 7).
- El offset se confirma recién después de guardar el efecto.
- Después de un rechazo de teléfono, el usuario puede reenviarlo hasta `expiresAt` (CU-05).

## Fuentes

- Enunciado: secciones 5 (punto 12), 7 y 8.
- Contrato de integración: secciones 15.1, 15.6 a 15.10, 17, 18.1 y 18.3.
