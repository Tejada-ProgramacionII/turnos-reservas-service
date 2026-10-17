# CU-02. Iniciar sesión

## Identificación

| Campo | Descripción |
| --- | --- |
| Objetivo | Autenticar al usuario final y entregarle un JWT para usar las funcionalidades protegidas. |
| Actor principal | Usuario final, desde la app KMP. |
| Actor de apoyo | Ninguno. |
| Disparador | El usuario ingresa su login y contraseña en la app. |

## Precondiciones

- El usuario está registrado (CU-01).

## Flujo principal

1. El usuario envía su login y su contraseña.
2. El servicio busca el usuario por login (sin distinguir mayúsculas).
3. El servicio compara la contraseña con el hash guardado.
4. El servicio emite un JWT firmado con la identidad del usuario, sus autoridades y una vigencia limitada.
5. El servicio responde con el JWT. La app lo usa en cada llamada protegida durante la sesión.

## Flujos alternativos

- **2a. El login no existe.** Se rechaza con "credenciales inválidas".
- **3a. La contraseña no coincide.** Se rechaza con el mismo mensaje que 2a, para no revelar qué usuarios existen.
- **2b. El usuario no está activo.** Se rechaza el inicio de sesión.

## Postcondiciones

- **Éxito:** la app tiene un JWT válido del usuario.
- **Fallo:** no se emite ningún token.

## Reglas

- **D-01.** Este servicio emite el JWT; el catálogo solo lo valida.
- Toda funcionalidad que no sea registro o inicio de sesión exige un JWT válido (firma y vigencia); sin él se rechaza (enunciado 9).
- La identidad de cada operación sale **del JWT**, nunca de un identificador enviado por el cliente (enunciado 9).
- El JWT de usuario y el JWT técnico de la cátedra son identidades distintas: el técnico nunca llega a la app (contrato 2).
- Ni la contraseña ni el token aparecen en logs.

## Fuentes

- Enunciado: secciones 3.2, 5 (punto 1) y 9.
- Contrato de integración: sección 2 (identidades).
