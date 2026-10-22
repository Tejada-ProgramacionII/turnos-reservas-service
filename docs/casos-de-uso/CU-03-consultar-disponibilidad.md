# CU-03. Consultar disponibilidad

## Identificación

| Campo | Descripción |
| --- | --- |
| Objetivo | Mostrar al usuario los turnos libres de un profesional para una fecha. |
| Actor principal | Usuario final, desde la app KMP. |
| Actores de apoyo | Servicio de catálogo (agenda vigente). Servicio de la cátedra (ocupaciones). |
| Disparador | El usuario elige un profesional y una fecha. |

## Precondiciones

- El usuario inició sesión (CU-02).

## Flujo principal

1. El usuario pide la disponibilidad indicando profesional y fecha.
2. El servicio valida que la fecha no sea anterior a hoy.
3. El servicio pide al catálogo los datos vigentes del profesional y su agenda semanal (CU-05 del catálogo).
4. El servicio toma los horarios semanales habilitados para el día de la semana de la fecha y los parte en turnos según la duración de cada horario.
5. El servicio pide a la cátedra las ocupaciones del profesional para esa fecha.
6. El servicio descarta los turnos ocupados (`CONFIRMED` o `HELD`) y, si la fecha es hoy, los que ya pasaron (D-10).
7. El servicio responde con los turnos libres: fecha, hora de inicio y hora de fin.

## Flujos alternativos

- **2a. Fecha inválida o pasada.** Se rechaza indicando el motivo.
- **3a. El profesional no existe.** Se responde "profesional no encontrado".
- **3b. El profesional está deshabilitado.** Se responde que no admite reservas nuevas.
- **3c. El catálogo no responde o no tiene copia disponible.** Se reintenta un número limitado de veces; si sigue fallando, se responde que la disponibilidad no se puede consultar ahora (D-09).
- **4a. No hay horarios habilitados para ese día.** Se responde una lista vacía.
- **5a. La cátedra no responde.** Se reintenta un número limitado de veces; si sigue fallando, se responde que la disponibilidad no se puede consultar ahora. No se muestran turnos sin haber descontado las ocupaciones.
- **7a. Todos los turnos están ocupados.** Se responde una lista vacía.

## Postcondiciones

- No se modifica nada: es una consulta.
- El usuario ve los turnos libres o un motivo claro de por qué no se pueden mostrar.

## Reglas

- **D-09.** Sin catálogo no se arma disponibilidad.
- **D-10.** Definición de turno libre.
- La agenda sale siempre del catálogo en el momento de la consulta: este servicio no guarda una réplica del catálogo (enunciado 3).
- Si un turno aparece como `CONFIRMED` y como `HELD`, prevalece `CONFIRMED`; en ambos casos no está libre (contrato 8).
- Mostrar un turno libre no lo reserva: otro usuario puede tomarlo antes. Eso se resuelve en CU-04.

## Fuentes

- Enunciado: secciones 4.2, 5 (punto 8), 7 y 8.
- Contrato de integración: secciones 8 (ocupaciones) y 17 (diagrama de reserva).
