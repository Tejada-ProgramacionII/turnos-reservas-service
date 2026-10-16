# CU-01. Registrar usuario

## Identificación

| Campo | Descripción |
| --- | --- |
| Objetivo | Crear la cuenta de un usuario final para que pueda usar la aplicación. |
| Actor principal | Usuario final, desde la app KMP. |
| Actor de apoyo | Ninguno. |
| Disparador | El usuario completa el formulario de registro en la app. |

## Precondiciones

- Ninguna: el registro es público.

## Flujo principal

1. El usuario envía `login`, `password`, `firstName`, `lastName`, `email`, `langKey` y, si quiere, `imageUrl`.
2. El servicio valida cada dato con las reglas del usuario de JHipster (D-02):
   - `login`: 1 a 50 caracteres, con los caracteres admitidos por JHipster;
   - `password`: 4 a 100 caracteres;
   - `firstName` y `lastName`: hasta 50 caracteres;
   - `email`: formato válido, 5 a 254 caracteres;
   - `imageUrl`: opcional, hasta 256 caracteres;
   - `langKey`: 2 a 10 caracteres.
3. El servicio normaliza `login` y `email` a minúsculas y comprueba que ninguno esté usado.
4. El servicio crea el usuario:
   - guarda la contraseña como hash, nunca en texto plano;
   - lo deja **activo**;
   - le asigna la autoridad `ROLE_USER`;
   - genera su identificador interno y su identificador estable para la cátedra (D-03);
   - completa los campos de auditoría.
5. El servicio responde que el registro fue correcto, sin devolver la contraseña ni su hash.

## Flujos alternativos

- **2a. Algún dato es inválido.** El servicio rechaza el registro e indica qué campos fallaron. No se crea nada.
- **3a. El `login` ya existe.** Se rechaza con un motivo claro ("usuario ya registrado").
- **3b. El `email` ya existe.** Se rechaza con un motivo claro ("email ya registrado").
- **1a. El cliente envía identificador, estado de activación, autoridades o auditoría.** Esos datos se ignoran: los administra el backend.

## Postcondiciones

- **Éxito:** existe un usuario activo que puede iniciar sesión (CU-02) sin verificar el correo.
- **Fallo:** no se crea ningún usuario.

## Reglas

- **D-01.** El registro vive en este servicio.
- **D-02.** Modelo y validaciones compatibles con el usuario de JHipster.
- **D-03.** El identificador para la cátedra lo genera el backend.
- La contraseña nunca aparece en respuestas ni en logs (enunciado 9).
- La recuperación de contraseña es opcional y no se incluye por ahora.

## Fuentes

- Enunciado: secciones 3.2, 5 (punto 1) y 9.
- Contrato de integración: sección 2 (identidades).
