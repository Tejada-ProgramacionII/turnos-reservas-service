# CU-05. Informar teléfono

## Identificación

| Campo | Descripción |
| --- | --- |
| Objetivo | Recibir el pedido de teléfono de la cátedra y responderle con el teléfono del usuario. |
| Actor principal | Usuario final, desde la app KMP. |
| Actor de apoyo | Servicio de la cátedra (envía `AdditionalInformationRequested` y recibe `AdditionalInformationSubmitted` por Kafka). |
| Disparadores | 1) Llega `AdditionalInformationRequested`. 2) El usuario envía su teléfono desde la app. |

## Precondiciones

- Existe un proceso del usuario confirmado inicialmente (CU-04).

## Flujo principal

**Parte A — llega el pedido**

1. El servicio recibe `AdditionalInformationRequested`.
2. Si su `eventId` ya fue procesado, lo da por procesado y termina (D-06).
3. El servicio busca el proceso por `reservationProcessId`.
4. El servicio guarda el `eventId` del pedido (se envía después como `requestEventId`), el `expiresAt` y el mensaje, y deja el proceso en `ESPERANDO_TELEFONO`.
5. El servicio da el evento por procesado.

**Parte B — el usuario informa el teléfono**

6. La app consulta el proceso (CU-07) y ve que se pide el teléfono.
7. El usuario envía su teléfono para ese proceso.
8. El servicio comprueba que el proceso es del usuario (D-13), que está en `ESPERANDO_TELEFONO`, que ya llegó el pedido y que no venció.
9. El servicio valida el teléfono con la regla de la cátedra (D-11).
10. El servicio guarda el teléfono en el proceso y publica `AdditionalInformationSubmitted` con un `eventId` nuevo, el `requestEventId` guardado y el `reservationProcessId` como clave del mensaje.
11. El servicio pasa el proceso a `TELEFONO_ENVIADO` y responde al usuario que el teléfono fue enviado y se espera el resultado (CU-06).

## Flujos alternativos

- **2a. Pedido duplicado.** No se modifica nada.
- **3a. No existe un proceso con ese `reservationProcessId`.** Se registra y se da el evento por procesado: no pertenece a ningún usuario de la aplicación.
- **4a. El proceso está en un estado final.** Se registra y se ignora (D-05).
- **4b. El pedido llega antes que la respuesta REST de la confirmación (CU-04).** Se guarda igual y el proceso pasa a `ESPERANDO_TELEFONO`; cuando llega la respuesta REST, no cambia nada.
- **8a. El proceso no existe o es de otro usuario.** Se responde "no encontrado" (D-13).
- **8b. El pedido todavía no llegó.** Se responde que aún no se puede informar el teléfono; la app vuelve a consultar.
- **8c. El proceso venció o está en un estado final.** Se rechaza informando el estado.
- **9a. El teléfono no cumple la regla.** Se rechaza indicando el formato esperado. El proceso no cambia y el usuario puede corregirlo.
- **10a. Kafka no acepta el mensaje.** Se reintenta un número limitado de veces; si falla, el proceso sigue en `ESPERANDO_TELEFONO` y se avisa al usuario que intente de nuevo.

## Postcondiciones

- **Éxito:** el teléfono llegó a la cátedra y el proceso está en `TELEFONO_ENVIADO`.
- **Fallo:** el proceso sigue en `ESPERANDO_TELEFONO` (si todavía está vigente) o en su estado final.

## Reglas

- **D-05, D-06, D-11, D-13.**
- El evento enviado lleva todos los campos obligatorios del contrato, con `eventType` `AdditionalInformationSubmitted` y `schemaVersion` `1`.
- Si la cátedra rechaza el teléfono (CU-06), el usuario puede reenviarlo hasta `expiresAt`, siempre con un `eventId` nuevo.
- El teléfono no aparece en logs.

## Fuentes

- Enunciado: secciones 5 (punto 11) y 7.
- Contrato de integración: secciones 15.1, 15.2, 15.4, 15.5, 17 y 18.1.
