# CU-07. Consultar mis reservas

## Identificación

| Campo | Descripción |
| --- | --- |
| Objetivo | Mostrar al usuario sus procesos de reserva y sus reservas, con el estado al día. |
| Actor principal | Usuario final, desde la app KMP. |
| Actor de apoyo | Servicio de la cátedra (listado de reservas de la cuenta técnica). |
| Disparador | El usuario abre "Mis reservas" o la app consulta el estado de un proceso en curso. |

## Precondiciones

- El usuario inició sesión (CU-02).

## Flujo principal

1. El usuario pide sus reservas, opcionalmente filtradas por estado o rango de fechas, o el detalle de un proceso.
2. El servicio obtiene los procesos locales **del usuario autenticado** (D-13).
3. El servicio consulta a la cátedra las reservas filtrando por el `externalPatientId` del usuario (D-03, D-12).
4. Para cada proceso local que no está en estado final, el servicio actualiza su estado con lo que informa la cátedra, respetando las transiciones permitidas (D-05).
5. El servicio responde con los procesos del usuario: profesional, fecha, horario, estado, `expiresAt` si sigue en curso, si se espera el teléfono y el motivo si falló.

## Flujos alternativos

- **1a. El detalle pedido es de otro usuario o no existe.** Se responde "no encontrado" (D-13).
- **2a. El usuario no tiene procesos.** Se responde una lista vacía.
- **3a. La cátedra no responde.** Se responden los datos locales indicando que pueden no estar al día (D-12, D-09).
- **3b. La cátedra informa una reserva sin proceso local.** No se muestra: el usuario solo ve procesos asociados a él localmente.

## Postcondiciones

- El usuario ve solamente lo suyo.
- Los procesos no finales pueden quedar actualizados con el estado de la cátedra.

## Reglas

- **D-03, D-05, D-12, D-13.**
- El `externalPatientId` que se usa para filtrar sale del usuario autenticado, nunca de la petición.
- La cátedra devuelve reservas de toda la cuenta técnica: nunca se reenvía su respuesta sin filtrar (contrato 11).

## Fuentes

- Enunciado: secciones 4.2, 5 (punto 13), 8 y 9.
- Contrato de integración: secciones 2 y 11.
