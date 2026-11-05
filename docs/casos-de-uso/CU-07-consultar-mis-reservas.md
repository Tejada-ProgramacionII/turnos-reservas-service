# CU-07. Consultar mis reservas

## Identificación

| Campo | Descripción |
| --- | --- |
| Objetivo | Mostrar al usuario sus procesos de reserva y sus reservas, con el estado al día. |
| Actor principal | Usuario final, desde la app KMP. |
| Actor de apoyo | Ninguno en el pedido: se leen los procesos locales (D-12). |
| Disparador | El usuario abre "Mis reservas" o la app consulta el estado de un proceso en curso. |

## Precondiciones

- El usuario inició sesión (CU-02).

## Flujo principal

1. El usuario pide sus reservas, opcionalmente filtradas por estado y paginadas, o el detalle de un proceso.
2. El servicio obtiene los procesos locales **del usuario autenticado** (D-13), primero los más recientes.
3. El servicio responde con los procesos del usuario y el total: profesional, fecha, horario, estado, `expiresAt` si sigue en curso, si se espera el teléfono, el motivo si falló, y si se puede abandonar o cancelar.

## Flujos alternativos

- **1a. El detalle pedido es de otro usuario o no existe.** Se responde "no encontrado" (D-13).
- **1b. La paginación no es válida.** Se rechaza indicando los valores admitidos.
- **2a. El usuario no tiene procesos.** Se responde una lista vacía.

## Postcondiciones

- El usuario ve solamente lo suyo.

## Reglas

- **D-12, D-13.**
- No se consulta a la cátedra en cada pedido: la consulta funciona aunque la cátedra no responda (D-09). El estado local se mantiene al día con los eventos de Kafka (CU-06) y con la reconciliación (CU-09), que corre al arrancar y cada minuto.
- El usuario sale del token, nunca de la petición. Una reserva de la cátedra sin proceso local no se muestra: el usuario solo ve procesos asociados a él.

## Fuentes

- Enunciado: secciones 4.2, 5 (punto 13), 8 y 9.
- Contrato de integración: secciones 2 y 11.
