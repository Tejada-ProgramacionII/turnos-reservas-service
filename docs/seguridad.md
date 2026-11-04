# Seguridad

Turnos es el que registra, autentica y emite el JWT de los usuarios finales (D-01). El catálogo solo lo valida.

## Registro (CU-01)

`POST /api/register` es público. `CreateUsuarioUseCaseImpl` hace lo siguiente:

- Valida el cuerpo con las reglas del usuario de JHipster (D-02): login con la expresión de JHipster, clave de 4 a 100 caracteres, email válido. Ver [api.md](api.md).
- Guarda `login` y `email` **en minúsculas**, así el login no distingue mayúsculas. Si alguno ya existe responde `400` ("usuario ya registrado" / "email ya registrado"). La base también los tiene como únicos.
- Guarda solo el **hash** de la clave, nunca la clave.
- Deja al usuario activado con la autoridad `ROLE_USER`. El cliente no elige roles.
- Genera el `externalPatientId` con `UUID.randomUUID()`. Es el identificador que se manda a la cátedra: lo crea el backend, no lo elige el cliente y nunca cambia (D-03).

## Hash de claves: Argon2id

- `SecurityConfig` define el `PasswordEncoder` como `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`: Argon2id con 16 MiB de memoria, 2 iteraciones, paralelismo 1, sal de 16 bytes y hash de 32 bytes. Usa la librería `bcprov-jdk18on`.
- El caso de uso solo conoce el puerto `PasswordHasher`; la implementación es `Argon2PasswordHasherAdapter`.
- Antes era BCrypt; se cambió a Argon2id a pedido del profesor. La migración V3 agrandó `password_hash` a 255 caracteres porque un hash Argon2id ocupa 97 y crece si se suben los parámetros.

## JWT (CU-02)

`POST /api/authenticate` es público.

- `AuthenticateUsuarioUseCaseImpl` busca el login en minúsculas, exige que el usuario esté activado y compara la clave con el hash.
- Cualquier falla responde lo mismo, `401` "credenciales inválidas": no dice si el login existe.
- Si sale bien, `JwtTokenGeneratorAdapter` emite el token:

| Aspecto | Valor |
| --- | --- |
| Algoritmo | **HS512** (`JwsHeader.with(MacAlgorithm.HS512)`) |
| Secreto | `JWT_SECRET`, en Base64, de al menos 64 bytes; sin él el servicio no arranca |
| `sub` | el login |
| `auth` | las autoridades separadas por espacio (`ROLE_USER`) |
| `iat` / `exp` | emisión y vencimiento; dura `jwt.token-validity-seconds` = 86400 s (24 h) |
| Respuesta | `{"id_token": "..."}` y el header `Authorization: Bearer ...` |

**Validación.** El servicio es un *resource server* de OAuth2:

- `JwtConfig` arma un `NimbusJwtDecoder` con el mismo secreto que **solo acepta HS512**. Un token firmado con otro algoritmo o con otra clave, o vencido, responde `401`.
- Las autoridades salen del claim `auth`, sin prefijo.
- El catálogo valida el mismo token con el mismo secreto: turnos se lo propaga (ver [contrato-con-catalogo.md](contrato-con-catalogo.md)).

## Qué es público

`SecurityConfig` deja públicos solo:

- `/api/register` y `/api/authenticate`;
- `/v3/api-docs/**`, `/swagger-ui/**` y `/swagger-ui.html` (documentación);
- `/error`.

Todo lo demás exige un JWT válido (`anyRequest().authenticated()`).

## Aislamiento entre usuarios (D-13)

- El usuario de cada pedido sale del `sub` del token (`@AuthenticationPrincipal Jwt`), nunca de un parámetro o del cuerpo.
- Cada caso de uso de reservas busca el usuario por login y filtra el proceso por `usuarioId`. Un proceso de otro usuario se trata **igual que uno inexistente**: `404`, sin revelar que existe.
- "Mis reservas" (CU-07) consulta solo los procesos del usuario del token.
- La cátedra devuelve las reservas de toda la cuenta técnica (contrato 11). Turnos aplica la propiedad: la reconciliación consulta por el `externalPatientId` de cada usuario, y la API nunca expone reservas ajenas.

## Sin sesión y CSRF

- La API es **sin estado** (`SessionCreationPolicy.STATELESS`): no crea sesión HTTP ni usa cookies. Cada pedido trae el token en el header `Authorization`.
- Por eso **CSRF está deshabilitado** (`csrf(AbstractHttpConfigurer::disable)`). CSRF se aprovecha de que el navegador manda solo las cookies de sesión. Acá no hay cookies: un sitio ajeno no puede hacer que el navegador agregue el header `Authorization`.
- SonarCloud lo reporta como `java:S4502`. En una API sin estado con JWT es un falso positivo y se justifica así.

## Secretos y datos personales

- **Ningún secreto va al repositorio.** Todos llegan por variables de entorno:
  - `JWT_SECRET`, sin valor por defecto;
  - `CATEDRA_JWT` y `CATEDRA_BASE_URL`;
  - `KAFKA_BOOTSTRAP_SERVERS`, los topics y el `groupId`;
  - `CATALOGO_BASE_URL`;
  - `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.
- Las obligatorias usan `${VAR:}` con `@NotBlank`: si falta alguna, el servicio no arranca, en vez de arrancar con un valor vacío.
- Los tests usan valores ficticios propios (`src/test/resources/application.properties`).
- El JWT técnico de la cátedra se agrega como header en `CatedraClientConfig` y no aparece en los mensajes de error de los adaptadores (hay tests que lo verifican).
- **El teléfono no va a los logs.** El productor registra solo el `eventId`, el proceso y el tipo de error, nunca el mensaje ni la excepción. La API no lo devuelve.
- Del listado de la cátedra solo se leen `reservationId`, `reservationProcessId` y `status`: el teléfono del paciente no se deserializa.
