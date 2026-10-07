# Compras-Backend — Contexto del Proyecto

> Este archivo registra el estado actual del proyecto y las decisiones tomadas. Se actualiza conforme avanzamos. Es el punto de referencia de contexto para continuar el trabajo desde cualquier máquina.

## Stack

- **Spring Boot 4.0.5** · Java 21 · Maven (`mvnw.cmd`)
- **PostgreSQL** `compras_db` (localhost:5432, usuario `postgres` / `admin`). La BD original del POS (`ventas_db`, misma instancia) se usó como fuente para generar `V1__init.sql`
- **Flyway** (migraciones versionadas en `src/main/resources/db/migration/`: `V1__init.sql` = esquema base de las 13 tablas · `V2__venta_confirmada.sql` = ciclo de vida de la venta, 2026-09-30 · `V3__compras_confirmadas_costo_y_cajas.sql` = confirmación de compras + costo congelado + cajas numeradas, 2026-09-30)
- **Spring Data JPA** (Hibernate 7.2.7), `ddl-auto: update` local / `validate` en producción (ver 2026-09-23)
- **Spring Security + JWT** (jjwt 0.11.5, HS256, expiración 5h, secret desde `${JWT_SECRET}`)
- **spring-boot-starter-mail** (Gmail SMTP para recuperar contraseña)
- **Lombok** (parcial), validación, multipart `uploads` (máx 20MB)
- **Server**: puerto `${PORT:8081}` · CORS global `${CORS_ALLOWED_ORIGINS}`
- **Terminal de pagos**: `payment.terminal.type=SIMULATED` (default) o `PHYSICAL` (Socket TCP)

## Arquitectura

Capas: `controller/ → service/ (interfaz) → service/impl/ → repository/ → model/entity/`. DTOs en `model/dto/`, Mappers en `mapper/` (algunos estáticos, otros `@Component`), config en `config/`, excepciones en `exceptions/`.

- **12 controllers, 68 endpoints** bajo `/api/local/...` (el prefijo `/api/tienda/**` del storefront **nace en la Fase 3**)
- **Prefijos de dominio** (ver `docs/PLAN.md` §1-§2): `/api/local/**` = POS/gestión del empleado (Angular) · `/api/tienda/**` = cliente (Next.js) · `/api/uploads/**` = estático, público y compartido por ambos
- Autorización por permiso vía `@PreAuthorize("hasAuthority('...')")` (**37 permisos** en `PermissionName`: los 34 de siempre + `CONFIRMAR_VENTAS`/`CANCELAR_VENTAS` (V2) + `CONFIRMAR_COMPRAS` (V3), los tres el 2026-09-30)
- 4 `CommandLineRunner` de bootstrap (orden): `RoleBootstrap` → `AdminBootstrap` → `PermissionBootstrap` → `RolePermissionBootstrap`. Todos respetan el flag `app.seed-bootstraps` (`APP_SEED_BOOTSTRAPS`, default `true`) y siembran **solo si está vacío** (NO self-healing; ver 2026-09-17 (2)).
- Admin inicial: `18jarquinsanchezerik1a@gmail.com` / `1234`
- **Paquete raíz**: `com.erikjarquin.compras` (era `...ventas` hasta el 2026-09-30)

## Endpoints principales

Base de los 12 módulos = `/api/local`:

| Módulo | Base | Notas |
|---|---|---|
| Auth | `/api/local/auth` | login, forgot/reset/change-password, me/authorities — público |
| Users | `/api/local/users` | CRUD + `PATCH /{id}/active` |
| Roles | `/api/local/roles` | CRUD |
| Permissions | `/api/local/permissions` | GET all |
| Products | `/api/local/products` | CRUD multipart, `/barcode/{bc}`, `/search?q=`, `?category=`, `/inactive`, `PATCH /{id}/deactivate`, `PATCH /{id}/active` |
| Categories | `/api/local/categories` | CRUD |
| Sales | `/api/local/sales` | POST (efectivo/transit/tarjeta), GET, PATCH `/{id}/confirm`, PATCH `/{id}/cancel` |
| Payments | `/api/local/payments` | card, status/{tx}, retry/{id}, reverse/{id} |
| Purchases | `/api/local/purchases` | GET (todos, por proveedor, detalle), POST (nace PENDIENTE), PATCH `/{id}/confirm`, DELETE (cancelar) |
| Providers | `/api/local/providers` | CRUD |
| Cash | `/api/local/cash` | create, open (por número), close (exige cuadrar), summary, active, history, available, next-number, number/`{number}` |
| Reports | `/api/local/reports` | trend, top-products, payment-methods, categories, low-stock, **margins**, summary, **profit**, **cash/`{cashId}`** (todas con `VER_REPORTES`) |
| Uploads | `/api/uploads/**` | **Público** — imágenes de platillos/productos. **FUERA** de `/api/local` a propósito: el storefront las necesita públicas |

## Estado de módulos

- **Terminal pagos**: abstracción `TerminalService` con `TerminalSimulatedImpl` (tarjetas de prueba DETERMINISTIC) y `TerminalPhysicalImpl` (Socket TCP, protocolo `PAY|`/`REV|`/`STS|`).
- **Correo (recuperación de contraseña)**: hoy es **solo SMTP** y **no funciona en Railway Hobby** (bloquea los puertos 25/465/587). Está en curso migrarlo al patrón de la terminal (`EmailSender` + impl. SMTP + impl. Resend, selector `app.mail.provider`). **Ver la entrada del 2026-10-07: es un plan, aún no implementado.**
- **Pagos**: estados PENDING/PROCESSING/APPROVED/REJECTED/REVERSED, reintentos (máx 3), idempotencia, reversas. `PaymentMonitorJob` (`@Scheduled`, cada 5 min) revisa pagos PENDING > 5 min.
- **Dos frontends** (ruta hermana): `Compras-Frontend-Local` (Angular 21, POS, `:4200`) y `Compras-Frontend-Cliente` (Next.js 16, storefront, `:3000`). Ambos consumen este único backend. El Angular usa `${environment.apiLocal}`; el Next.js usará `${api}/tienda` (aún no existe). Ambos esperan que `img` de productos sea **URL completa**.
- **Reportes**: dashboard hecho (ver sesión 2026-09-12 (3)). Las consultas agregan EN SQL y devuelven DTO; el frontend solo grafica con Chart.js (SSR-safe) y exporta PDF/Excel en cliente.
- **Compras**: módulo completo (ver sesión 2026-09-13 (2)). Proveedores (CRUD, RFC único) + compras con renglones que al registrarse SUMAN stock y guardan el costo real del producto; cancelación revierte stock. `margins` (costo real vs precio) en reportes.
- **Tienda de comida (Fases 2-8, PENDIENTES)**: siguen sin existir `Insumo`, `Platillo`, `RecetaDetalle`, `Cliente`, `Pedido`, `DetallePedido`, el rol CLIENTE ni WebSocket. Ver `docs/PLAN.md`.

## Registro de cambios / decisiones

### 2026-10-07 — Correo de recuperación: SMTP y Resend detrás de una interfaz (OPCIÓN B, **PENDIENTE**)

> Encargo: Railway bloquea el SMTP saliente en plan Hobby y el correo de
> recuperación de contraseña no sale. Se decidió **implementar la opción B** del
> PDF `docs/08-Recuperacion-Contrasena-Resend-SMTP-Outlook.pdf`: **una interfaz, dos
> implementaciones y un selector por configuración**.
>
> ⚠️ **NO SE IMPLEMENTÓ NADA TODAVÍA.** Esta entrada documenta el **plan** para
> poder retomarlo desde cualquier máquina. **No hay ninguna clase nueva en el
> repo.** Lo único que se agregó fue este `AGENTS.md` y el PDF 08.

#### 0. El diagnóstico (por qué falla hoy)

| Límite | Qué controla | Cómo se sube | ¿Aplica? |
|---|---|---|---|
| **Cuota de recursos** | CPU, RAM, disco | Pagar plan / crédito de consumo | **Sí** → por eso el plan ya no se traba |
| **Restricción de red** | Qué puertos puede abrir la máquina | **Plan Pro + `redeploy`** | **Sí** → esta bloquea el correo |

🔑 **Pagar NO lo arregla.** Son dos controles separados: el pago sube la cuota, la
política de red solo sube con Pro **y además exige redeploy** (cambiar el plan no
cambia la máquina ya desplegada).

- Railway bloquea **25, 465 y 587**. Permite **443**.
- El síntoma es `Connection timed out connecting to smtp.gmail.com:587`.
  🔑 **`timeout` y no `connection refused` es la firma del firewall**: un puerto
  cerrado de verdad se rechaza al instante; un paquete descartado se queda
  esperando hasta expirar. Eso descarta "mal usuario/contraseña" como causa.
- La salida: **API HTTPS** (puerto 443). Resend es la recomendada, plan gratuito.

#### 1. El patrón YA EXISTE en el proyecto: la terminal de pagos

🔑 **Este es el argumento más fuerte de la decisión**, y conviene tenerlo presente
antes de escribir una línea. El proyecto **ya resolvió exactamente este problema**
para la terminal, con el patrón completo:

| Pieza | Terminal de pagos | Lo que se hará con el correo |
|---|---|---|
| Interfaz | `service/TerminalService.java` | `service/EmailSender.java` (a crear) |
| Impl. A | `TerminalSimulatedImpl` (`SIMULATED`, `matchIfMissing=true`) | clase SMTP actual (a condicionar) |
| Impl. B | `TerminalPhysicalImpl` (`PHYSICAL`) | `ResendEmailSender` (a crear) |
| Selector | `payment.terminal.type` | `app.mail.provider` |
| Propiedad | `${PAYMENT_TERMINAL_TYPE:SIMULATED}` | `${MAIL_PROVIDER:smtp}` |
| Props tipadas | `config/TerminalConfig.java` (`@ConfigurationProperties`) | `config/MailConfig.java` (a crear) |

🔑 **Mismo patrón, misma propiedad, mismos decoradores.** Quien lea `TerminalImpl`
ya sabe leer `EmailSender` sin instrucción nueva. Elegir la estructura del correo
distinta habría sido inventar una segunda convención en el mismo repo.

⚠️ **La terminal usa `matchIfMissing=true` en la impl. A.** El valor por defecto
tiene que ser el comportamiento **actual**, para que añadir la abstracción **no
cambie nada** de lo que ya funciona. Por eso el default de `app.mail.provider`
será **`smtp`**, no `resend`.

#### 2. Las 3 decisiones tomadas (y por qué)

| # | Ambigüedad | Decisión | Por qué |
|---|---|---|---|
| 1 | ¿Cambiar SMTP a la fuerza o dejarlobehind una interfaz? | **Interfaz + 2 implementaciones** (opción B) | Es lo que ya hace la terminal. Con interfaz, el respaldo SMTP se puede añadir **después** sin rehacer nada |
| 2 | ¿Dónde queda el nombre `EmailService`? | **Hoy es una clase CONCRETA** (`@Service`, depende de `JavaMailSender`). Se **conserva intacta** como la impl. SMTP y se crea al lado `EmailSender` (interfaz) | Respeta "dejar las clases SMTP intactas" y **no obliga a renombrar nada** en un repo con 294 tests |
| 3 | ¿El valor por defecto? | `smtp` | Si el default fuera `resend`, un despliegue sin la variable nueva **dejaría de enviar correo de golpe**. Al revés (default `smtp`) hoy sigue funcionando igual |

⚠️ **Deuda de nomenclatura que se acepta a propósito:** la interfaz se llama
`EmailSender` y no `EmailService`, porque `EmailService` **ya existe** como clase.
La convención del repo sería `EmailService` (interfaz) + `EmailSmtpImpl` +
`EmailResendImpl`. **Se acepta la diferencia** para no mover una clase que hoy
funciona y tiene referencias; se puede alinear después con un rename mecánico si
algún día se toca.

#### 3. Lo que hay que CREAR (6 archivos)

| # | Archivo | Responsabilidad |
|---|---|---|
| 1 | `service/EmailSender.java` | **Interfaz**: el método que hoy tiene `EmailService` (`sendPasswordRecoveryEmail(destinatario, enlace)`). Es la frontera del cambio |
| 2 | `service/impl/EmailSmtpImpl.java` | **Ojo: NO hay que crear esto.** El código SMTP ya existe en `EmailService.java`. Decisión final: `EmailService` **implementa** `EmailSender` y se le pone el `@ConditionalOnProperty`. Ver §5 |
| 3 | `service/impl/ResendEmailSender.java` | Arma el JSON y hace el `POST` HTTPS. Es el camino nuevo, el que desbloquea Railway |
| 4 | `model/dto/resend/ResendEmailRequest.java` | Campos de la API: `from`, `to` (**lista**), `subject`, `html`/`text` |
| 5 | `model/dto/resend/ResendEmailResponse.java` | El `id` del envío, para registrarlo en el log y depurar |
| 6 | `exceptions/EmailDeliveryException.java` | "No se pudo enviar el correo", distinguible de otros fallos |

*(La fila 2 queda así después de revisar el código real: crear una segunda
implementación SMTP sería **duplicar** el que ya existe. Se documenta el cambio
real en §5.)*

#### 4. Lo que hay que MODIFICAR (5 archivos)

| # | Archivo | Cambio | ⚠️ Cuidado |
|---|---|---|---|
| 1 | `service/EmailService.java` | Añadir `implements EmailSender` + `@ConditionalOnProperty(app.mail.provider=smtp, matchIfMissing=true)` | **Solo eso.** El `sendPasswordRecoveryEmail` no se toca. Sigue siendo texto plano |
| 2 | `service/impl/AuthImpl.java` | **2 líneas**: el `import` y el tipo del campo inyectado (`EmailService` → `EmailSender`) | 🔑 **Si aquí hay un tercer cambio, es que se está haciendo el trabajo en el sitio equivocado.** El token, la vigencia de 1 h, el enlace y `repo.save()` quedan intactos |
| 3 | `application.yaml` | Bloque `app.mail.*` (selector, url, api-key, from) y **defaults vacíos** en el bloque `spring.mail` | 🔑 **Ver §6: los `${MAIL_USERNAME}` hoy NO tienen default.** Es la trampa que rompe el arranque |
| 4 | `application-local.yaml` | Las 3 variables nuevas para probar local | No está en git: regenerarlo en cada máquina |
| 5 | `exceptions/GlobalExceptionHandler.java` | Handler de `EmailDeliveryException`, si se quiere respuesta limpia | Que un fallo de correo no sea un 500 opaco |

#### 5. El cambio real en `AuthImpl` (y por qué NO comentar código)

La idea inicial era **comentar** el código SMTP en `AuthImpl` y en los YAML.
🔑 **Eso se descartó, y hay dos razones distintas:**

1. **En `AuthImpl` comentar código es peor que las 2 líneas del cambio.** Dejaría
   código muerto que se pudre en silencio y que nadie sabe si sigue siendo
   válido. 📌 **Precedente en este repo:** el bloque comentado de la relación con
   `User` en `CashRegisterEntity` terminó eliminándose (ver 2026-09-12 (2)). El
   proyecto ya pagó por decidir que el código comentado no es documentación.
2. **En los YAML comentar el bloque `spring.mail` es una bomba de reloj.** Ver §6.

Lo correcto es **inyección por interfaz**: `AuthImpl` depende de `EmailSender` y no
sabe cuál de las dos implementaciones hay. Eso es además lo que permite **probarla
sin red** (mock de la interfaz), cosa que hoy no se puede porque el código llama a
`JavaMailSender` directo.

#### 6. 🔑 La trampa que rompe el arranque (leída del código real)

`application.yaml` tiene:

```yaml
username: ${MAIL_USERNAME}     # <- SIN default
password: ${MAIL_PASSWORD}     # <- SIN default
```

🔑 Si se pasa a `MAIL_PROVIDER=resend` y **se comentan o borran** `MAIL_USERNAME` /
`MAIL_PASSWORD` del despliegue **sin tocar el bloque `spring.mail`**, la aplicación
**NO LEVANTA**: Spring no logra resolver el placeholder y falla al inicializar, ANTES
de llegar a cualquier código de correo. El síntoma sería "la app no arranquó" y
nada tiene que ver con el correo — la peor forma de perder tiempo.

**Regla:** **no borrar ni comentar el bloque `spring.mail`**. Agregarle defaults
vacíos (`${MAIL_USERNAME:}`) para que **siempre** resuelva, y que la decisión se
tome en `app.mail.provider`. Así el bloque es inofensivo en modo Resend.

**Al revés también es cierto:** si el bean SMTP tiene `@ConditionalOnProperty` y en
producción el valor está mal escrito (ej. `resemd`), **no existe ningún bean** y
Spring **falla al arrancar**. 🔑 Eso es lo **deseado**: es mejor que la app no
arranque que arrancar y que el correo llegue a un 500 que nadie descubre hasta que
un usuario reporta que no le llega el enlace.

#### 7. Variables de entorno

| Variable | Obligatoria | Default | Nota |
|---|---|---|---|
| `MAIL_PROVIDER` | No | `smtp` | `smtp` \| `resend`. **Default = comportamiento actual** |
| `RESEND_API_KEY` | Sí (si `resend`) | — | Secreto. Railway + `application-local.yaml` |
| `RESEND_FROM` | Sí (si `resend`) | — | `no-reply@tudominio.com` tras verificar el dominio |
| `RESEND_URL` | No | `https://api.resend.com` | Solo si se quiere explícito |
| `FRONTEND_URL` | No | `http://localhost:4200` | 🔑 **Se mantiene.** Es la base del enlace |

⚠️ **En producción el cambio se hace en las variables de RAILWAY, no en
`application-local.yaml`** (ese archivo es gitignored y solo es de desarrollo).
Cambiar de proveedor = **una** variable: `MAIL_PROVIDER`.

🔑 **`FRONTEND_URL` es lo más delicado del cambio**: si no apunta al frontend real
en el despliegue, el correo **sale bien y no sirve de nada** (enlace roto). En local
el default ya es correcto, así que **las pruebas en local no lo detectan**. Hay
que verificarlo a mano una vez en Railway.

#### 8. Orden de implementación (con verificación en cada paso)

Cada paso deja el sistema **funcionando**. Nada de cambiar todo y luego probar.

| # | Paso | Verificación |
|---|---|---|
| 1 | Crear `EmailSender` + los 2 DTOs + `EmailDeliveryException` | Compila. **No cambia nada** |
| 2 | `EmailService implements EmailSender` + `@ConditionalOnProperty(matchIfMissing=true)` | Local con SMTP: **el correo llega igual que antes** |
| 3 | `AuthImpl`: cambiar `import` + tipo del campo (2 líneas) | Local con SMTP: correo llega. **Suite en verde** |
| 4 | `MailConfig` (`@ConfigurationProperties("app.mail")`) + bloque `app.mail` en el YAML + **defaults vacíos** en `spring.mail` | Arranca en ambos modos |
| 5 | `ResendEmailSender` + su `@ConditionalOnProperty(havingValue="resend")` | Local con `MAIL_PROVIDER=resend`: llega por HTTPS |
| 6 | Probar con `onboarding@resend.dev` (solo al correo de la cuenta) | Correo recibido + estado en el panel de Resend |
| 7 | Verificar dominio propio (SPF + DKIM + DMARC) y cambiar `RESEND_FROM` | 📨 **Bandeja de entrada**, no spam |
| 8 | `MAIL_PROVIDER=resend` en Railway + redeploy | `forgot-password` **desde fuera**, a un Gmail ajeno al proyecto |
| 9 | *(opcional)* Respaldo SMTP con cadena de `catch` | Cortar Resend a propósito: el correo sigue saliendo |

🔑 **El orden importa:** el paso 3 es el que libera la interfaz, y el 5 es el que
añade el camino nuevo. Separarlos permite **probar la refactorización con SMTP
funcionando** (paso 2-3) antes de meter una variable nueva y una API externa. Si
algo falla en el paso 8, el culpable se sabe con exactitud.

#### 9. ⚠️ Lo que NO se resuelve con este cambio

| # | Pendiente | Por qué queda fuera |
|---|---|---|
| 1 | 🔴 **`forgotPassword` revela qué correos existen** | `AuthImpl.java:93` lanza `UserException` si el correo no está registrado. Permite **enumerar cuentas**. Debería responder siempre igual y enviar el correo solo si el usuario existe. **Es el defecto más serio del módulo** y es ortogonal a Resend |
| 2 | **Fallo hacia arriba** | Si la API falla, la excepción sube y el endpoint devuelve error. Lo correcto: registrar y responder como si nada (el usuario no puede arreglar un problema de red) |
| 3 | **Sin límite de intentos** | Nada impide pedir recuperación en bucle y gastar cuota. Es denegación de servicio + correo no deseado. Se resuelve limitando por IP o correo |
| 4 | **Texto plano** | Hoy es `SimpleMailMessage`. Con Resend el `html` es nativo. Mejora **cosmética**, no necesaria |
| 5 | **`to` es una LISTA** | `"to": ["..."]`, no un texto. Como cadena suelta la API responde **422** y no sale nada |

#### 10. Verificación (aún NO ejecutada — es el plan)

- `mvnw test -Dtest='!ComprasApplicationTests'` → debe seguir en **294/294**.
  🔑 **`AuthImpl` no tiene test unitario hoy.** El cambio de 2 líneas de §4.2 no
  está cubierto por la suite: es el punto que más conviene cubrir al implementar
  (con un mock de `EmailSender`, que solo es posible **gracias a la interfaz**).
- **2 pruebas manuales en local**: SMTP (modo actual) y Resend (`onboarding@`).
- **2 en Railway**: correo a un Gmail **ajeno** al proyecto (revisar spam) y
  verificar que el enlace del correo apunta al frontend real.
- **Prueba negativa**: `MAIL_PROVIDER` mal escrito → la app **debe fallar al
  arrancar** (§6). Si arranca, el selector no está funcionando.

**Pendientes / debilidades conocidas de este plan**

1. ⚠️ **El nombre de la interfaz** (`EmailSender`) no sigue la convención
   (`EmailService`). Aceptado a propósito (§2, decisión 2).
2. ⚠️ **No hay tests de `AuthImpl`**: el punto más delicado del cambio es también
   el que hoy está sin cubrir.
3. ⚠️ **`EmailService` usa `@Value("${spring.mail.username}")` en el
   constructor.** Con la interfaz, ese `@Value` debe pasar a la clase SMTP: si el
   bean SMTP es condicional, **`@Value` en el constructor solo se resuelve si el
   bean se crea**. Es la forma más fácil de que el modo Resend rompa algo que en
   SMTP funcionaba.
4. ⚠️ **Dependencia de un tercero**: si Resend cae, no hay correo. El respaldo
   SMTP (paso 9) lo cubre.

---

### 2026-10-04 (2) — V7: ningún campo obligatorio se guarda como NULL

> Encargo: *"validar que no se guarden los campos como null: nombre de
> categorías, nombre de usuario, correo, contraseña, nombre de rol, sku,
> barcode, nombre de proveedor y rfc de proveedor"*.
> Migración `V7__campos_obligatorios.sql`, **aplicada y registrada** en Supabase.
> **294/294 tests** en verde. Verificado con 12 peticiones reales contra la API.

#### 0. El resultado de la auditoría: la respuesta era **desigual**

Antes de tocar nada se auditó campo por campo. El hallazgo importante es que
**no era un problema uniforme**, y por eso la solución tampoco:

| Campo | Antes | Ahora |
|---|---|---|
| Categoría, rol, proveedor (nombre/RFC) | **3 capas**: validador + `@Column(nullable=false)` + `NOT NULL` | Sin cambios |
| Usuario · nombre, correo (alta) | Solo lo frenaba el `NOT NULL` de la base | `requerido()` + formato |
| Usuario · nombre, correo (**edición**) | **NADA**: se copiaba el texto crudo | `requerido()` + formato + duplicados |
| Usuario · contraseña | **NADA** | `requerida()` + mínimo 8 |
| Producto · SKU, barcode | Opcionales **por decisión** | **Obligatorios** + `NOT NULL` |

🔑 El 2026-09-12 quedó escrito que *"el carrito del POS no se pagina"* y el
2026-09-28 que *"el SKU es opcional"*. **Las dos decisiones estaban bien
razonadas y las dos se revierten**, cada una por un motivo de operación, no de
gusto. Lo que falla es dejar escrito un "es opcional" sin anotar la condición
que lo(reverse).

#### 1. El bug más caro: **una contraseña vacía SÍ se guardaba**

```java
user.setPassword(passwordEncoder.encode(request.getPassword()));  // sin validar
```

🔑 `BCryptPasswordEncoder.encode("")` **no lanza nada**: devuelve un hash
perfectamente válido. O sea que un alta de usuario con la contraseña en blanco
creaba la cuenta y la fila en la base. El problema aparecía **después**, al
intentar entrar: `matches("", hash)` devuelve `false` **siempre**, así que esa
persona quedaba bloqueada para siempre y sin explicación.

**Un dato basura en la base y un ticket de soporte que nadie puede resolver.**

Por eso `InputValidator.password()` se llama **ANTES** de `encode()`, no
después: validar el hash sería demasiado tarde. Hay un test que lo fija con un
`BCryptPasswordEncoder` **real** (no el mock), porque lo que se documenta es el
comportamiento de la librería.

#### 2. Los validadores que faltaban: `requerido()` y `requerida()`

`InputValidator` tenía 8 validadores de **formato** y **ninguno de
obligatoriedad**. Por eso tres servicios escribieron su propio `if (x == null)
throw` (categoría, rol, proveedor) y el de usuario **se olvidó**: el `null`
llegaba al `INSERT`.

- `requerido(valor, campo)` → *"El nombre es obligatorio."*
- `requerida(valor, campo)` → *"La contraseña es obligatoria."*

🔑 **Son dos métodos y no un `boolean` de género** porque en la llamada
`requerida(p, "contraseña")` se lee solo, mientras que `requerido(p,
"contraseña", true)` deja adivinar qué significa el `true`. El primero versión
que salió decía *"El contraseña es obligatorio"*, y la prueba de API lo cazó.

- La comparación es contra `isBlank()`, **no** contra `!= null`: el formulario
  manda `"   "` cuando el usuario deja el campo en blanco, y eso **pasa** un
  `!= null`. El test `nombreSoloEspacios` existe por esto.

#### 3. El `PUT /users/{id}` era un agujero

`updateUser` hacía `user.setName(request.getName())`: sin `trim`, sin límite de
longitud, sin formato de correo y sin chequeo de duplicados. Un nombre de 500
caracteres o un correo con mayúsculas y espacios se guardaban tal cual.

Ahora pasa por los mismos validadores que el alta, **más** una corrección que
hacía falta: el chequeo de correo duplicado **excluye al propio usuario**
(`filter(otro -> !otro.getId().equals(id))`). Sin ese filtro, editar a alguien
sin tocarle el correo siempre daría conflicto y sería **imposible editar a un
usuario**.

#### 4. SKU y barcode: de opcionales a obligatorios

Dos razones, ambas de operación:

1. En el POS el producto se cobra **escaneando** su código. Un producto sin
   barcode es un producto que el cajero no puede cobrar sin buscarlo a mano.
2. En PostgreSQL un `UNIQUE` admite **varios `NULL`** (`NULL != NULL`). Con el
   barcode opcional, "producto sin código" se acumulaba **en silencio**: no
   decía nada, no rompía nada y no se podía ni contar. El índice único no
   avisaba porque técnicamente no había duplicado.

#### 5. La migración rellena antes de restringir (y por qué no puede ser simple)

Un `NOT NULL` a secas **falla** si hay datos sucios. Y "sucio" tiene **tres**
formas, cada una por un lado distinto:

| Forma | Qué es | Por qué la atrapa |
|---|---|---|
| `NULL` | Falta el campo en el INSERT | `IS NULL` |
| `''` o `'   '` | El usuario dejó el campo en blanco | Para una columna `NOT NULL` son valores **válidos**: la base los acepta |
| `'NULL'` | Se guardó la **palabra** NULL como si fuera el dato | Es una cadena normal de 4 caracteres |

🔑 La tercera se encontró **revisando la base**, no leyendo el código: el
producto "Leche 3" tenía el texto `NULL` como SKU. Para la base es un SKU
perfectamente normal, y el `NOT NULL` lo acepta igual. Si la migración solo
mirara `IS NULL`, el producto habría quedado guardado "sin SKU" y nadie se
enteraría.

- Los rellenos usan `WHERE ... IS NULL OR btrim()='' OR upper(btrim())='NULL'`
  y **nunca pisan un valor que ya existe**.
- Los valores inventados son **internos y rastreables**: SKU `PEND-SKU-<id>` y
  barcode con **prefijo 20**, que es el rango que GS1 reserva a retail interno,
  así que no colisiona con el EAN de fábrica (750, 789, 737...).
- El barcode se rellena con ceros a la **izquierda** (`'20' || lpad(id,12,'0')`)
  para que todos midan 14 dígitos: el código es un identificador de **texto** y
  un lector de ancho fijo los leería distinto si variaran.
- La comprobación final es un `DO $$` que **falla con un mensaje que dice qué
  hacer**, en vez de un error técnico de PostgreSQL.

#### 6. Entidad y base van de la mano

`ProductEntity` pasó a `@Column(unique = true, nullable = false)`. Con
`ddl-auto: validate`, Hibernate **no toca** el esquema: solo lo comprueba al
arrancar. Si la entidad dice "no puede ser nulo" y la columna lo permite, la
aplicación **no levanta**. Ese arranque fallido es la red que hace honesto
mantener las dos cosas sincronizadas, y es la razón de no confiar solo en la
validación de Java.

#### 7. Verificación

- **294/294 tests**: 15 nuevos de usuarios (`UserImplCamposObligatoriosTest`),
  6 del validador, y **2 tests viejos invertidos** (afirmaban que SKU/barcode
  vacío era válido).
- **12 peticiones reales** contra la API, todas con **400** y mensaje útil:
  categoría, rol, proveedor (nombre y RFC), usuario (nombre, correo, contraseña
  vacía y corta) en alta y en edición, y producto (SKU y barcode vacíos).
- El caso que **sí** debe pasar también pasó: un producto nuevo con SKU y
  barcode válidos se creó (id 18, luego eliminado para no dejar basura).
- Los 2 productos rellenados quedaron como `PEND-SKU-1` / `20000000000001` y
  `PEND-SKU-14`. **Son provisionales**: el dueño debería poner los reales.

### 2026-10-04 — V6: idempotencia del cobro (un doble Enter ya no cobra dos veces)

> Encargo: que apretar Enter dos veces en el modal de pago **no** genere dos
> ventas, y revisar la paginación del carrito de compras.
> Explicación completa en **`docs/07-Paginacion-Carrito-e-Idempotencia-Ventas.pdf`**
> (generador: `docs/generar_pdf_v6_idempotencia.py`, que **reimporta** los
> estilos del generador del PDF 06 para que la serie no se diversifique).

**El bug**: el modal de cobro es un `<form>`. Doble Enter → dos POST a
`/sales` → **dos ventas**, con el stock descontado dos veces, dos tickets y el
corte de caja descuadrado. El daño no era visual: era una inconsistencia en los
números que alimentan inventario y reportes.

#### 0. La decisión de la que depende todo: **la clave la genera el cliente**

🔑 Si el backend generara la clave en cada petición, cada una sería distinta y
la protección no serviría de nada. La clave identifica la **intención de
cobro**, y esa intención vive en el navegador del cajero. Por eso la columna es
nullable y **sin `DEFAULT`**: las ventas anteriores a V6 (y las de otros
caminos) no la llevan.

| Momento | Qué pasa con la clave |
|---|---|
| Se abre el modal de cobro | Se genera **una clave nueva** |
| Se aprieta Enter (1 o 100 veces) | Se **reutiliza** la misma |
| Se paga con tarjeta | Se **reutiliza**: es el mismo cobro |
| Venta confirmada | La clave muere con ese cobro |

🔑 **Se genera al ABRIR el modal, no al CONFIRMAR.** Si se generara en el clic
de confirmar, cada Enter tendría su propia clave y cada una sería una venta
nueva: exactamente el bug. Está escrito en `openPaymentModal()` para que nadie
lo "simplifique" moviéndolo.

#### 1. Las TRES capas (y por qué la del medio no basta)

| Capa | Pieza | Qué aporta |
|---|---|---|
| Frontend | `isProcessing` + botón `[disabled]` | Evita el 2.º **clic**. Nada más. |
| Frontend | `idempotencyKey` en el request | Viaja la **intención** de cobro |
| **Base de datos** | `UNIQUE` parcial | **Garantiza** que no haya venta doble |
| Backend | `findByIdempotencyKey` | Reconoce el reintento |
| Backend | `catch DataIntegrityViolationException` | Resuelve la carrera |

🔑 El botón deshabilitado **no arreglaba el bug** y esa es la parte que costó
entender: previene el segundo clic, pero no el caso real, que es que **las dos
peticiones ya viajan por la red a la vez**. Además recargar la pestaña o
reintentar por timeout volvería a crear la venta.

#### 2. La carrera simultánea y el detalle no obvio de PostgreSQL

La comprobación previa resuelve el doble Enter **secuencial**, pero no el
paralelo: si las dos peticiones buscan antes de que ninguna escriba, las dos
ven "no existe" y las dos insertan. Lo resuelve el `UNIQUE`:

```sql
CREATE UNIQUE INDEX idx_sales_idempotency
    ON public.sales (idempotency_key) WHERE idempotency_key IS NOT NULL;
```

🔑 **Lo que hace que la recuperación funcione sin reintentos**: en PostgreSQL el
INSERT perdedor **se bloquea dentro del índice** hasta que la transacción
ganadora resuelve, y el error de duplicado **solo se emite cuando la ganadora
ya hizo COMMIT**. Por eso al releer, la fila **ya está confirmada y visible**.
No hay carrera al releer. Si se cambia a una BD sin este comportamiento, hace
falta un ciclo de reintentos y esta nota hay que rehacerla.

- `SaleController.processSale()` → `catch (DataIntegrityViolationException)` →
  `service.findByIdempotencyKey(clave)` → si no hay venta, **relanza el error**.
- 🔑 **Ese `orElseThrow(() -> e)` no es un detalle menor**: un
  `DataIntegrityViolationException` no siempre es un cobro duplicado (puede ser
  stock o un dato inválido). Tragárselo dejaría al cajero creyendo que cobró.
- 🔑 **La relectura es `@Transactional(readOnly = true)` a propósito**: se
  ejecuta *después* de que la transacción perdedora revirtió. Leer en la
  transacción ya `rollback-only` lanzaría `UnexpectedRollbackException`.

#### 3. `normalizarClave()` — tres trampas evitadas

| Trampa | Qué pasó si no se normaliza |
|---|---|
| `"  x  "` vs `"x"` | La **misma** intención se volvía 2 claves distintas |
| `"   "` (blanco) | Buscar por `""` hace que **todas** las ventas sin clave colisionen |
| 200 caracteres | Revienta el `VARCHAR(64)` → 500 |

#### 4. Índice **parcial**, y por qué

Un `UNIQUE` normal ya permite varios `NULL` (`NULL != NULL`), así que el filtro
no es necesario para la corrección: es **optimización**. Se deja porque `sales`
es la tabla más grande y casi todas sus filas tienen la clave en `NULL` (ventas
viejas, otros flujos). Un índice parcial sobre las pocas que sí la tienen es
mucho más pequeño.

#### 5. `findByIdempotencyKey` es un nombre DERIVADO

`findBy` + `IdempotencyKey` → propiedad `idempotencyKey` de `SaleEntity`, que
existe. 🔑 El mismo error ya se cometió en `CashBoxRepository` con
`existsByCashRegistersId` (la propiedad se llama `cashRegister`, no
`cashRegisters`). Regla vigente: si el nombre derivado no compila, el problema
suele estar en el **nombre de la propiedad**.

#### 6. Verificación

- **`273/273` tests en verde** (262 previos + 11 nuevos):
  `SaleImplIdempotencyTest` (7) y `SaleControllerIdempotencyTest` (4).
- 🔑 **Dos pruebas fallaron al escribirlas y la culpa fue del código, no del
  test**: con la clave ausente el servicio **no consulta el repo** (corta
  antes). El test afirmaba un stub que nunca se usaba y Mockito lo marcó como
  *unnecessary stubbing*. Se corrigió el assert a `never()`, **no** la prueba.
- **V6 aplicada en Supabase** y registrada por Flyway (informó
  `already exists, skipping`: el `IF NOT EXISTS` haciendo su trabajo).
- **Prueba real con 2 peticiones EN PARALELO**, misma clave, 2 unidades cada una,
  stock inicial 17: ambas respondieron **`saleId=8`**, **1** venta creada, stock
  17 → **15** (no 13), **1** renglón de detalle.
- Caso **secuencial** con otra clave: 1.ª `saleId=10`, 2.ª `saleId=10`.
- **Limpieza**: las 2 ventas de prueba se **anularon por la API** (no se
  borraron a mano: el módulo es append-only, ver doc 02) y la caja abierta se
  cerró. Estado final: stock **17** y **0** cajas abiertas.

### 2026-10-01 — V5: ventas por usuario, fechas de alta/baja, re-alta de caja e historial de caja

> Encargo: poder buscar *"quién vendió esto"*, *"cuándo se fue esta persona"*, y
> ver *"qué pasó en CAJA 1"* con todos sus turnos. Todo sale de una pregunta mal
> hecha: **¿qué pasa cuando algo se repite?**
>
> Explicación completa en `docs/06-Venta-Usuario-y-Historial-Caja.pdf`
> (13 secciones, con el código). Verificación: `mvnw test` → **258/258**,
> `ng build` del frontend → OK.

**0. Las tres decisiones que se tomaron antes de escribir código**

| # | Ambigüedad | Decisión | Por qué |
|---|---|---|---|
| 1 | ¿Se rellena el usuario de las ventas viejas? | **No: quedan en `NULL`** | El dato no existía. Inventar un usuario fabricaría un reporte falso; un `NULL` se muestra como "Sin usuario" y no miente. |
| 2 | ¿La caja dada de baja libera su número? | **No, nunca** | El `UNIQUE` está en `cash_boxes.number` y la fila no se borra al dar de baja. Reactivarla es **la misma caja** con su historial: sus ventas apuntan a sus turnos y los turnos a ella. |
| 3 | Con filtro de usuario, ¿el turno sin ventas de ese usuario aparece? | **No, desaparece** | Mostrarlo con $0 sería información falsa: "este turno no vendió nada" ≠ "este turno no aparece porque Juan no trabajó ahí". |

**1. `sales.user_id` (V5) — la venta sabe quién la hizo**

La venta solo apuntaba al TURNO. Eso hacía imposibles dos preguntas muy
normales: *¿qué caja vendió Juan?* y *¿cuánto vendió cada cajero?*.

- `V5__ventas_usuario_y_usuarios_fechas.sql`: columna + índice + FK.
- 🔑 **`ON DELETE SET NULL`, no `CASCADE`**: borrar un usuario **no** borra sus
  ventas. Con `CASCADE`, borrar un usuario borraría el historial de ventas del
  local, que es un dato del negocio y no del empleado. Perder una venta es peor
  que perder un dato de ella.
- `SaleEntity.user` es **`FetchType.LAZY`**: el filtro por usuario solo necesita
  el id (que ya viene en la propia venta) y el nombre se pide en el reporte. Con
  `EAGER`, listar el historial haría un SELECT extra por venta (N+1).

**2. El usuario sale del token, nunca del cuerpo de la petición**

```java
//SaleImpl.createBaseSale()
sale.setUser(currentUserOrNull());   // lee el SecurityContext
```

🔑 Si el endpoint aceptara `{"userId": 7}` en el JSON, cualquiera con un token
válido podría registrar ventas a nombre de otro y el reporte de "quién vendió"
dejaría de ser confiable — que es justo lo que se agregó para poder auditar.
El usuario sale del `SecurityContext` porque el JWT ya fue validado.

`currentUserOrNull()` devuelve `null` en vez de fallar: los tests de integración
no levantan el filtro de seguridad, y una llamada interna no lo tiene. El costo
es una venta sin usuario, que es el mismo estado que las de antes de V5.

**3. `users.activated_at` / `users.deactivated_at` (V5) — "desde cuándo"**

`active` es un booleano: dice SÍ está dado de baja, pero no CUÁNDO. Con eso
*"buscar el registro de quien se fue en abril"* no tiene respuesta.

```java
createUser     -> setActivatedAt(now); setDeactivatedAt(null);
activateUser   -> setActivatedAt(now); setDeactivatedAt(null);   // re-activar
deactivateUser -> if(estaba activo) setDeactivatedAt(now);
                  setActivatedAt(null);
```

Dos detalles que parecen redundantes y no lo son:

- 🔑 **La guarda `if(user.isActive())` en la baja**: si se llama dos veces, sin
  la guarda la segunda pisaría la fecha y se perdería la **original**, que es la
  que responde "¿desde cuándo se fue?". La fecha de baja es un *hecho*, no un
  estado reescribible.
- **Se limpia `activatedAt` al dar de baja**: la regla de coherencia es que cada
  usuario tiene exactamente una de las dos fechas. Si vuelve a darse de alta, la
  tabla de "dados de baja" no debe seguir mostrándolo.

> Esto **no** es un historial de cambios: es la última alta y la última baja.
> Para tener cada cambio haría falta una tabla de auditoría, que no existe y no
> hace falta aquí.

`idx_users_deactivated_at` es un índice **parcial** (`WHERE active = false`):
la única consulta que lo usa es la tabla de dados de baja; indexar toda la tabla
gastaría espacio en las filas activas, que nunca se buscan por esa columna.

**4. Re-alta de caja: `PATCH /api/local/cash/boxes/{id}/active`**

Hace que "dar de baja" sea **reversible**: si se dio de baja por error, o porque
una caja se estaba reparando y ya terminó, hay salida. No borra nada ni crea una
caja nueva.

| Estado | `DELETE` | `PATCH /{id}` (baja) | `PATCH /{id}/active` |
|---|---|---|---|
| Nunca se abrió | ✅ | ✅ | no hace falta |
| Ya tuvo turnos | ❌ 409 | ✅ | ✅ |
| Turno abierto | ❌ 409 | ❌ 409 | ❌ 409 |

**5. `GET /api/local/reports/cash-box/{boxId}` — historial por CAJA**

Reemplaza la pantalla de "corte de caja". Antes el selector listaba **turnos**:
una caja abierta diez veces aparecía diez veces con el mismo texto "CAJA 1", y
era imposible responder "¿qué pasó en CAJA 1?". Ahora el selector trae **cajas** y
la tabla trae sus **turnos**, uno por fila.

DTO nuevo: `CashBoxReportDTO` → `CashBoxSessionDTO` → `CashSessionSellerDTO`.

Tres decisiones que hay que entender:

- 🔑 **El filtro se aplica en Java, no en SQL.** Una caja tiene 2-3 turnos: el
  conjunto es chico y se filtra en memoria sin penalizar. A cambio, **una sola
  pasada** calcula ventas, el agrupado por vendedor y la utilidad. En SQL serían
  4 consultas y luego reconciliarlas en Java, que es donde se esconden los bugs.
- **Los montos se calculan sobre las ventas FILTRADAS** (si filtras por Juan,
  ves el total de Juan). El `openingAmount` sí viene del corte congelado: es
  dinero físico del cajón, no una venta.
- 🔑 **`difference` llega en `NULL` con filtros, y hay un flag `filtrado`.** La
  diferencia es un dato congelado del turno **completo**. Con filtro de usuario
  y un total de $200, mostrar "diferencia: -50" afirmaría un descuadre que **no
  ocurrió**. Es el tipo de mentira que un corte de caja no puede decir, así que
  la UI **oculta la columna** en vez de pintar un cero.

El agrupado por vendedor usa `computeIfAbsent` sobre un `LinkedHashMap`: el mismo
vendedor puede vender en varios turnos y en la tabla aparece una vez por turno.
Las ventas sin dueño se agrupan aparte al final, con nombre explícito
("Sin usuario"), en vez de inventar un "Desconocido" que parecería real.

**6. `SaleDetailHistoryResponse` gana `userId` / `userName`**

`SaleMapper.toDetailResponse()` copia el **nombre** al DTO en vez de dejar la
entidad: así el reporte no depende de la relación LAZY (que fuera de
transacción lanzaría `LazyInitializationException`) y el frontend recibe el texto
listo.

**7. Cambios en `SaleEntity` / `CashRegisterService`**

- `SaleImpl` gana `UserRepository` en el constructor (para `currentUserOrNull`).
- `CashRegisterService/Impl` ganan `activateBox(Long)`.
- `CashBoxRepository` cambió `existsByCashRegistersId` (nombre derivado roto, ya
  documentado en la entrada de V4) por `hasSessions(@Param)` con `@Query`.

**8. Lo que NO se hizo (a propósito)**

- **No se rellenó el usuario de las ventas anteriores a V5.** Ver decisión 0.1.
- **No se tocó `cash_registers.number` ni V4.** Se acumulan: V4 ya hizo las cajas reutilizables.
- **No se agregaron las columnas "Estado"/"Acciones" al historial.** Se
  quitaron el 2026-09-30 y sigue igual: el historial es una consulta, no una mesa
  de trabajo.
- **No se borró ningún endpoint.** Los de V3 (`POST /cash`, `GET /cash/available`)
  ya se habían eliminado; `DELETE /boxes/{id}` se mantiene con su 409.

**9. Verificación**

| Qué | Cómo | Resultado |
|---|---|---|
| Suite completa | `mvnw clean test -Dtest='!ComprasApplicationTests'` | **258/258** |
| Frontend | `ng build` | OK |
| Filtro de usuario | Smoke manual (ver §7.1 del PDF) | Pendiente de probar en la UI |

⚠️ Al probar en la UI: las ventas nuevas deben salir con nombre de usuario en
el detalle del turno. Las viejas saldrán como "Sin usuario", y eso es lo
correcto.



### 2026-09-30 (4) — Precio de venta, caja que cuadra, validaciones y estilos

> Encargo de 7 puntos. El común denominador: **nada se contabiliza sin que un humano
> lo confirme, y nada se guarda si no es un dato posible**. 3 de los 7 puntos
> resultaron ser **el mismo bug** (ver §3) y 2 eran reglas de negocio nuevas.

**0. Las 4 decisiones que tomé y por qué las planteé**

El encargo era ambiguo en 4 puntos. Antes de escribir código las aclaré con el usuario,
porque implementarlas "como semké" habría costado más rehacerlas:

| Punto | Ambiguidad | Decisión | Por qué la otra opción era peor |
|---|---|---|---|
| 5.1 | "no dejes cerrar si no cuadra" | **Bloquear + salida de emergencia con motivo** | Bloquear en seco deja el turno encerrado si el cajero se equivocó al contar |
| 3 | ¿"precio de venta" escribe `product.price`? | **Sí, al confirmar** | El usuario aclaró el flujo real (abajo). El "no" era mi regla anterior documentada |
| 7.6 | Barcode/SKU "solo enteros" | **Barcode = texto numérico** | Como número, `0001234567895` → `1234567895` y el lector deja de funcionar |
| 7.4 | Mayúsculas en "todos los inputs" | **Solo campos de código** | "TACOS DE CHICHARRÓN" se lee peor; los nombres de producto quedan normales |

⚠️ **Cambio de regla de negocio (punto 3).** Hasta esta sesión el código documentaba
que el módulo de compras **NUNCA** escribía `product.price` ("es decisión comercial del
dueño"). El usuario aclaró el flujo real y es otro: *"al crear un producto por primera
vez das el precio, y cuando haces una compra para reabastecer escribes el precio de
compra al proveedor y también el precio de venta, que puede seguir igual o subirlo; lo
que se actualiza en productos es solo el stock y el precio"*. **Implementado como lo dijo.**
`unit_price` es **nullable**, y NULL significa *"esta compra no opina sobre el precio"* →
confirmar conserva el actual. Un NOT NULL con default 0 dejaría productos gratis.

**1. `purchase_details.unit_price` (V3) — el precio de venta del renglón**

`PurchaseItemRequest/DTO/Entity` ganan `unitPrice`. Se guarda al **registrar** (para que
la compra pendiente ya muestre el precio que se aplicará) y `confirm()` lo copia a
`product.price`. Compras y productos siguen tablas separadas: la compra **historiza** la
decisión, el producto guarda el **vigente**. Por eso `margins` (costo vs precio) refleja
el último precio confirmado.

**2. `cash_registers.difference_reason` (V3) + caja pre-creada (punto 5)**

- El cierre **exige cuadrar** (`difference != 0` y sin motivo → **409**, la caja sigue
  abierta). Con motivo → cierra y guarda diferencia + motivo. El motivo va **junto** a la
  diferencia porque "me sobraron 200" y "me faltaron 200" son opuestos con el mismo síntoma.
- Si cuadra exactamente, el motivo se **limpia** (no queda colgado de una versión previa).
- **`POST /api/local/cash` crea la caja** (nace con `opened_at NULL`), y `POST /open` ahora
  **elige una existente**. El número debe existir antes para poder elegirlo de una lista.
- **`getNextSuggestedNumber()`** devuelve "CAJA n" para prellenar el modal.
- **Una caja se abre UNA sola vez** (409 si `openedAt != NULL`). Como el número es UNIQUE
  hay una fila por caja física: reabrir "CAJA 1" **mezclaría dos turnos** en un mismo corte.
  ⚠️ Es la misma razón por la que existe el filtro por caja de Reportes.
- Fondo mínimo **100**, no negativo (V1 ya tenía `opened_at`/`opening_amount` nullable, así
  que no hizo falta alterar la tabla para cajas sin abrir).

**3. 🐛 El `if` NO daba idempotencia — era la carrera del stock duplicado**

Este fue el hallazgo técnico más importante, y **también era un bug en `SaleImpl.confirm()`
desde V2**. Un `if(entity.isConfirmed())` resuelve el reintento **secuencial** (doble clic)
pero **no** el **simultáneo**:

```
T1: lee confirmed=false ─┐
T2: lee confirmed=false ─┴─→ las dos pasan el if → las dos suman stock
```

**Arreglo: `UPDATE ... WHERE confirmed = false` que devuelve las filas afectadas**
(`markConfirmedIfPending`). `1` = esta ganó y aplica stock; `0` = otra se adelantó y
**no toca nada**. El `if` **se queda** (atiende el secuencial); el UPDATE atiende el
concurrente. **Elegí UPDATE condicional sobre `@Lock(PESSIMISTIC_WRITE)`** porque no
mantiene un lock de fila abierto durante todo el `@Transactional` (incluidos los `save()`
de cada producto) y no depende de que nadie anote el método de lectura.

Efecto lateral discovered: tras el UPDATE la entidad en memoria quedaba obsoleta, así que
se **refleja el estado sobre ella** en vez de releer la BD (un query menos). El timestamp
se calcula **una vez** y se usa para ambos, para que el `confirmedAt` guardado coincida con
el que ve el usuario.

**4. `util/InputValidator.java` (nuevo) + `validadores.ts` (nuevo) — punto 7**

El punto central: **se valida el TEXTO, no el número ya parseado**, porque `new
BigDecimal("000.2")` es `0.2` y `"1.875"` es `1.875`: los ceros iniciales y el tercer
decimal **se pierden antes de poder detectarlos**. Por eso `ProductController` recibe
`price`/`stock` como **`String`** y no `BigDecimal`/`int`.

| Campo | Regla | Por qué |
|---|---|---|
| stock | entero, ≥0 | No existe 1.6 de jabón. `numeric(38,2)` además |
| precio | ≥0, máx **2** decimales | `1.875` se redondearía a 1.88 **en silencio** |
| ambos | sin `1e5` | `<input type=number>` **acepta** `e`/`E`: un error de dedo → 100,000 |
| ambos | sin ceros iniciales | `000.2` es error de dedo. Un `0` solo **sí** vale |
| SKU | letras + `-_./`, máx 50 | Un SKU **no** es un número |
| barcode | solo dígitos, máx 20, **texto** | Identificador: como número pierde los ceros |
| RFC | letras+números, máx 13 | RFC mexicano: 13 física / 12 moral |
| códigos | MAYÚSCULAS | "choc-500" y "CHOC-500" no pueden ser 2 SKUs |

**Duplicado a propósito (defensa en profundidad)**: el frontend es la UX (error mientras
escribes), el backend es la **frontera de confianza** (un script no se salta nada).
Los pegados se sanean **aparte** del `keydown` porque **pegar NO dispara `keydown`**.

**5. 🐛 Bug de paso: el SKU PROHUBÍA letras**

Tenía `pattern="[0-9]"` **y** `replace(/\D/g,'')` → "CHOC-500" se volvía **"500"**, otro
producto. El `pattern` además **nunca hizo nada** (sin `<form>` nativo Angular no lo
valida). Corregido al aplicar 7.2. El barcode **no** se tocó en ese sentido.

**6. Frontend**

- **Compras**: 3 columnas con título (Cantidad / Costo de compra / **Precio de venta**), el
  precio se autocompleta con el del producto y se puede cambiar; `.linea-cabecera` para que
  no se confundan. Detalle con margen por renglón (**rojo si es negativo** = estás vendiendo
  más barato de lo que compras). **Botón Confirmar con estilo propio** (verde, más alto, con
  sombra): es la acción de la que depende que el stock exista, y *un botón que no se
  encuentra es peor que uno que no existe*. `.delete-linea` para el botón de quitar renglón.
- **Caja**: botón **Crear caja** (modal con Crear/Cancelar, número precargado con la
  sugerencia del backend), **selector** de cajas disponibles en "Abrir", **"Caja: N"** visible,
  y **contador en vivo** (`efectivoAcumulado` = fondo + efectivo) que se recalcula solo por
  ser un getter. En el cierre: si cuadra lo dice en verde; si no, aparece el **campo de
  motivo** en ámbar (salida de emergencia).
- **Historial**: quité las columnas **Estado** y **Acciones** (punto 2). El historial es una
  consulta, no una mesa de trabajo. `colSpan` pasó a **fijo 6** (ya no depende de permisos).
  El ciclo de vida no se perdió: la venta anulada sigue con la fila tachada, y los endpoints
  siguen existiendo.
- **Sidebar**: `.active` con fondo + barra de 3px. `esActiva()` compara **exacto** (con
  `includes`, "/sales" matchearía "/sales-history") y limpia el query string.
- **Perfil y productos**: foto 48px con borde doble y sombra; imágenes de producto con marco,
  fondo gris (si falla la carga, no sale ícono roto) y `hover` que amplía. **"Eliminar"
  (rojo sólido, irreversible) vs "Dar de baja" (ámbar, reversible)** diferenciados.
- **Explicaciones (punto 4)**: bloques `.aviso-explicacion` en **Reportes** (de dónde sale
  Ingresos / Costo de lo vendido / Utilidad, y por qué NO son las compras) y en **Márgenes**
  (con la advertencia de que margen ≠ utilidad).

**7. Tests: 173 → 234** (+61)

`CashRegisterV3Test` (19, nuevo) · `InputValidatorTest` (30, nuevo, con `@Nested` por campo) ·
`PurchaseImplTest` (11→13) · `CashRegisterControllerTest` (12→21) ·
`PurchaseControllerTest` (+4) · `ReportControllerTest` (+5) · `ReportQueryTest` (+4).

Dos tests que documentan decisiones:
- `confirmarCompra_carreraConOtraPeticion_NoSumaStockDosVeces`: la entidad de la 2ª petición
  dice `confirmed=false` en memoria (un `if` la habría dejado sumar); el `UPDATE` devuelve 0.
- `validarLinea`/`CHOC/500/1/2/x`: **falló porque mi expectativa estaba mal** (`/` SÍ es
  separador válido). Corregí el test, no el código — tocar el validador para satisfacer una
  expectativa inventada habría **quitado un separador válido del SKU**.

**8. PDF en `docs/`**

`04-Precio-Venta-Caja-Cuadra-Validaciones.pdf` (17 pág.) — incluye la **tabla de relaciones**
(qué columna escribe quién y quién la lee) porque las 7 partes no son independientes.

**Pendientes / debilidades conocidas**

1. ⚠️ Sigue sin `CHECK NOT (confirmed AND cancelled)` para ventas **ni compras**. Va en **V4**.
2. ⚠️ El cierre de caja guarda el motivo como **texto libre**: no lo clasifica. Un análisis
   posterior ("me faltó vs me sobró") sería otro módulo.
3. Validaciones solo en **productos y proveedores**. Categoría y otros siguen con las reglas
   viejas; aplicarlas es **añadir handlers**, no reinventar (los validadores ya existen).
4. La caja no sabe quién la abrió (decisión tomada, no olvido).
5. El backfill de `sale_details.unit_cost` sigue siendo **aproximación** (costo actual).

### 2026-09-30 (3) — V3: confirmar compras, costo congelado, cajas numeradas y utilidad

> Tres cosas que el dueño pidió y que son **la misma idea**: nada se contabiliza hasta que
> pasa por un acto explícito. Comprar no suma stock hasta **confirmar**; la utilidad usa el
> costo **congelado al vender**; una caja se identifica por el **número** que escribe el
> vendedor. Las 3 decisiones del usuario: una sola caja activa (con número), utilidad por
> **costo real de lo vendido** (no compras del periodo), y cancelar una compra pendiente la
> **elimina**.

**1. El efecto en el inventario se mudó de `create()` a `confirm()`**

`PurchaseImpl.create()` ya **no toca el inventario**: arma cabecera + renglones + total y
nace **PENDIENTE**. El stock y el `product.cost` se escriben en
`PurchaseImpl.confirm()` (V3), que es el acto que declara que la mercancía **llegó**.

| Estado | Significado | ¿Se cancela? |
|---|---|---|
| **PENDIENTE** | registrada, el inventario intacto | **sí**, se borra (nada que revertir) |
| **CONFIRMADA** | la mercancía entró: stock + y costo vigente | **no (409)** |

- **Confirmar es idempotente**: 200 aunque ya estuviera confirmada, y **no vuelve a sumar el
  stock** (doble clic / reintento tras corte de red). Un 409 ahí sería confuso.
- **Cancelar se simplificó de golpe**: el `Math.max(stock - qty, 0)` y la validación de stock
  en dos fases **desaparecieron**, porque una compra pendiente nunca tocó el inventario. El
  bug de 2026-09-30 (comprar 10 → vender 5 → cancelar escribía stock=0 con 5 unidades ya
  vendidas) **ya no es representable**: no está escondido detrás de una validación, está
  estructuralmente eliminado. Confirmar devuelve 409 si ya está confirmada.
- El `cost` lo escribe confirmar y **no se revierte nunca** (mismo criterio de V2).
- `PurchaseImplTest` se **reescribió** (11 tests): el que blinda el bug ahora es
  `cancelarCompra_confirmada_Lanza409YNoEscribe` + `cancelarCompra_pendiente_laBorraYNoEscribe`
  (que además exige que cancelar **ni consulte** los productos, con
  `verify(productRepository, never()).findById(any())`).

**2. `sale_details.unit_cost`: el costo congelado (base de la utilidad)**

`SaleImpl.createDetail()` copia `product.getCost()` al renglón, igual que ya hacía con
`unitPrice`. Sin esto, la utilidad de una venta pasada habría que calcularla con
`product.cost`, que es el del **último purchase de hoy**: comprar algo más barato mañana
reescribiría la ganancia de ayer. Nullable a propósito (NULL = "costo desconocido" ≠ 0, que
inflaría la utilidad).

**3. Utilidad real: `GET /api/local/reports/profit` (con `ProfitDTO`)**

`revenue` − `costOfGoodsSold` = `grossProfit`, y `marginPercent` sobre venta. **No** es
"ventas − compras del periodo": ese dinero no se perdió, quedó en el almacén, y restarlo
subestimaría la utilidad. `itemsWithoutCost` cuenta los renglones sin costo para que la
pantalla **avise** en vez de mostrar una utilidad inflada. Nueva `ReportException` (404 para
una caja inexistente) + handler `REPORT_ERROR`.

**4. 🐛 Bug encontrado de paso: las ventas ANULADAS se contabilizaban como ingresos**

V2 dejó de **borrar** la venta al anularla (es evidencia contable) pero **no cambió su
`paymentStatus`**, que sigue en `APPROVED`. Los agregados filtraban solo por `APPROVED`, así
que **una venta anulada seguía sumando ingresos** — y con V3 también habría sumando costo.
Se agregó `AND s.cancelled = false` a las **6** consultas de `SaleRepository` y un `continue`
en `CashRegisterImpl.calculateSummary` (si no, anular una venta en efectivo dejaba su dinero
dentro del "esperado" del corte y el cajero sacaba una diferencia fantasma).
⚠️ Son **2 ejes ortogonales** (`PaymentStatus` × `cancelled`), por eso hacen falta las dos
condiciones. Regresión fijada en `ReportQueryTest` con una venta anulada en el seed.

**5. Cajas numeradas + filtro por caja (V3)**

- `cash_registers.number` `varchar(50) NOT NULL UNIQUE` (backfill `'CAJA-' || id`). Se agrega
  NULL → rellena → `SET NOT NULL` porque el default de una columna nueva **no** aplica a
  filas existentes en PostgreSQL. Sin `UNIQUE` dos cortes distintos se mezclarían al filtrar.
- **Una sola caja activa** (se mantiene el 409) y **NO** se agregó `user_id`: se decidió que
  la caja se identifica por número, no por vendedor.
- `OpenCashRequest.number` **obligatorio**; `CashRegisterImpl.normalizeNumber()` recorta y
  **capitaliza** a propósito: `"caja 1"` y `"CAJA 1"` son la misma caja y el UNIQUE de
  PostgreSQL las dejaría pasar como dos.
- Endpoints nuevos: `GET /api/local/cash/history`, `GET /api/local/cash/number/{number}`
  (`VER_CAJA`, no `CORTE_CAJA`: ver una lista es consultar) y
  `GET /api/local/reports/cash/{cashId}` (`CashReportDTO`: ventas + utilidad de esa caja).
- `PATCH /api/local/purchases/{id}/confirm` con permiso **`CONFIRMAR_COMPRAS`** (37º
  permiso), asignado a **ADMIN y ALMACENISTA** en la **migración** y no en el bootstrap
  (mismo criterio que V2: en una BD ya sembrada un permiso nuevo nunca llegaría solo).

**6. Tests (20 nuevos → suite en 173)** · `PurchaseImplTest` reescrito (11),
`PurchaseControllerTest` +4 (confirm 200/409/404/403), `CashRegisterControllerTest` +5
(número obligatorio/409/historial/por número), `ReportControllerTest` +5 (profit, rango,
caja, 404, 403), `ReportQueryTest` +4. `mvn test -Dtest='!ComprasApplicationTests'` →
**173/173**.

**7. Frontend (Angular)** · botón **Confirmar** + badge Pendiente/Confirmada + fila atenuada
+ `Cancelar` deshabilitado si está confirmada, todo por permiso; `confirmPurchase()` con
`PATCH`; el input de número en el modal de abrir caja; selector "filtrar por caja" y 3
tarjetas de utilidad (Ingresos / Costo de lo vendido / Utilidad) con `accent-red` si es
negativa, aviso de renglones sin costo y detalle de caja con sus ventas.

**Pendientes / debilidades conocidas**

1. ⚠️ **Falta `purchases.confirmed` en el CHECK** — no hay `CHECK NOT (confirmed AND
   cancelled)` para ventas (ver más arriba) ni para compras. Ambas irían en una **V4**.
2. ⚠️ `PurchaseImpl.confirm()` **tampoco** usa bloqueo pesimista: dos peticiones
   simultáneas podrían pasar el `if(!isConfirmed())` y **sumar el stock dos veces**. Es el
   mismo hueco que en `SaleImpl.confirm()`. Arreglo: `@Lock(PESSIMISTIC_WRITE)` o
   `UPDATE ... WHERE confirmed = false` devolviendo las filas afectadas.
3. El backfill de `unit_cost` usa el **costo actual** del producto (mejor aproximación, no
   el costo histórico exacto). Las ventas **anteriores a V3** pueden tener utilidad
   levemente incorrecta; las nuevas son exactas.
4. El **CHECK `permissions_name_check` de `V1` (34) quedó desactualizado**: ahora hay 3
   migraciones que lo reescriben (V2 → 36, V3 → 37). En una BD **nueva** Flyway las
   aplica en orden y el resultado es correcto; en una **existente** hay que aplicar V2 y V3
   (lo hacen solas). El enum de Java y las 3 migraciones deben seguir sincronizados.

### 2026-09-30 (2) — Ciclo de vida de la venta + integridad del inventario (V2)

> Corrige un bug **de datos**: cancelar una compra cuyo stock ya se había vendido escribía
> inventario FALSO con un 200 OK. Y cierra la puerta por la que se llegaba a ese estado.

**1. El bug (Punto 1 del encargo)**

`PurchaseImpl.cancel()` hacía `product.setStock(Math.max(stock - qty, 0))`. El `max` no impedía
la operación imposible: la obligaba a **escribir un número falso**. Escenario: comprar 10 →
vender 5 → cancelar la compra → `5-10 = -5` → `max` → **stock = 0**, con 5 unidades ya
vendidas. El usuario recibía 200 OK y nadie se enteraba.

**Regla implementada**: si `product.stock < detail.quantity` para algún renglón, la compra
**NO se cancela** → 409 con la **lista de todos** los productos en conflicto, y **no se
escribe absolutamente nada**. Por eso `cancel()` está en **dos fases**: (1) validar todos los
renglones acumulando conflictos, (2) escribir solo si la fase 1 terminó limpia. El `Math.max`
se eliminó. Las 3 razones están en el Javadoc de `PurchaseImpl.cancel()` (mensaje útil, no
depender del rollback de `@Transactional`, legibilidad).

**2. `cost` y `price` NO se revierten en la cancelación**

`product.cost` es el **último costo de compra vigente**, no un promedio ni un saldo: el
inventario que sobrevive a la cancelación sí costó eso. `price` es decisión comercial y el
módulo de compras **jamás** lo escribe. Solo se revierte la cantidad. (Cerrado el pendiente que
"faltaba decidir" sobre precio.)

**3. La venta recibió estados (el diseño que faltaba)**

Para que el inventario no sea manipulable después de que el cliente se llevó la comida:

| Estado | Significado | ¿Se anula? |
|---|---|---|
| abierta | registrada, stock descontado, pedido en proceso | **sí**, si es efectivo |
| **CONFIRMADA** | el pedido salió: dinero y stock son hechos reales → **congelada** | **no (409)** |
| **ANULADA** | error de caja / mal cobro; el stock volvió | **no (409)** |

- **Confirmar es idempotente**: 200 aunque ya estuviera confirmada (doble clic, reintento). Un
  409 ahí sería confuso.
- **Anular NO borra la fila**: la deja con `cancelled=true`. El historial de un POS es
  evidencia contable; un contador que baja solo es un agujero de fraude.
- **Anular solo accepts `CASH`**: con tarjeta el dinero ya entró y la reversa real es
  `POST /api/local/payments/reverse/{id}` (que además pide su propio permiso).
- **La anulación recorre `sale.getDetails()`, no un request**: se deshace exactamente lo que se
  hizo. No acepta cantidades del cliente.
- Efecto dominó: venta confirmada ⇒ el stock es real ⇒ esa compra ya no se puede cancelar.
- `PaymentStatus` y el estado de la venta son **ejes ortogonales**: una venta con pago
  APPROVED puede estar ANULADA. Por eso son 2 booleanos y **no** un enum de estado (un enum
  metería "ANULADA" dentro del eje del pago y rompería los reportes).

**4. `V2__venta_confirmada.sql` (198 líneas, la 2ª migración del proyecto)**

- `sales`: `confirmed` / `cancelled` `boolean NOT NULL DEFAULT false` + `confirmed_at` /
  `cancelled_at` `timestamp(6)` NULL, con `IF NOT EXISTS` y `COMMENT ON COLUMN`. Las ventas
  existentes nacen **no confirmadas** (estado correcto: una venta vieja se confirma hoy y
  recién ahí queda congelada).
- `permissions`: `DROP` + `ADD` de `permissions_name_check` con los **36** nombres
  (`IN (...)`, legible) y seed idempotente (`INSERT ... SELECT ... WHERE NOT EXISTS`) de
  `CONFIRMAR_VENTAS` / `CANCELAR_VENTAS`.
- Asignados a `ADMIN` y `CAJERO` con `INSERT ... SELECT ... CROSS JOIN ... NOT EXISTS` sobre
  la tabla puente `role_permissions`. **En la migración y no en `RolePermissionBootstrap`** a
  propósito: ese bootstrap es seed-inicial (no self-healing, ver 2026-09-17 (2)), así que en
  una BD ya sembrada un permiso nuevo nunca llegaría solo a los roles.
- Verificado: los 36 nombres de V2 = los 34 de V1 + los 2 nuevos, sin perder ninguno.

**5. Endpoints, permisos, excepciones, frontend**

- `PATCH /api/local/sales/{id}/confirm` (`CONFIRMAR_VENTAS`) y
  `PATCH /api/local/sales/{id}/cancel` (`CANCELAR_VENTAS`) — PATCH y no PUT porque es un
  cambio **parcial** de estado: un PUT reenviaría la venta entera y podría pisar stock/total.
- `SaleException` ahora transporta su propio `HttpStatus` (404 / 409 / 400) en vez de ser
  siempre 400; `GlobalExceptionHandler` lo traduce. `getSaleById` inexistente: 400 → **404**.
- Angular (`Compras-Frontend-Local`): `SaleHistory` con los campos nuevos,
  `confirmSale()`/`cancelSale()` con PATCH, badges Pendiente/Confirmada/Anulada, fila anulada
  tachada, diálogos de confirmación, botones por permiso, y **409 mostrado al usuario** con
  `alert(err.error?.message)`. El `Anular` solo aparece con `CANCELAR_VENTAS` **y** efectivo
  **y** venta abierta. La fila se reemplaza por la respuesta del servidor (no estado local
  optimista).

**6. Tests (24 nuevos → suite en 153)**

`PurchaseImplTest` (+4), `SaleControllerTest` (+8), `SaleImplLifecycleTest` (nuevo, 11).
El assert que blinda el bug original es
`verify(productRepository, never()).save(any())`: no basta que se lance la excepción, hay que
verificar que **nada se escribió**. `mvn test -Dtest='!ComprasApplicationTests'` → **153/153**.

**7. PDFs en `docs/`** (generados con `reportlab`, leyendo el código real de los archivos)

- `01-Fase1-Paquete-Compras-y-Prefijo-Api-Local.pdf` (12 pág.) — este punto 1.
- `02-Ciclo-Ventas-Integridad-Stock-Compras.pdf` (18 pág.) — este punto 2.

⚠️ Generador **fuera** del repo (en `%TEMP%\opencode\`: `generar_pdfs.py` + `pdf1.py` +
`pdf2.py` + `generar_docs.py`). Se lee el código de los archivos, así que si el código cambia
hay que regenerar. Lecciones del generador: dividir el contenido en módulos (un solo archivo
excede el límite de longitud del shell → `spawn ENAMETOOLONG`), `st.extend(bullets([...]))` y
no `st.append` (los bullets devuelven una **lista**, no un Flowable), y los bloques de código
largos se recortan por rango de líneas (`bloque(ruta, inicio=, fin=)`) porque ReportLab no
puede partir un bloque de más de 654 pt.

**Pendiente / debilidades conocidas de este diseño** (documentadas en §19 del PDF 2):

1. La exclusión mutua `confirmed`/`cancelled` vive en el **código**, no en la BD (no hay
   `CHECK NOT (confirmed AND cancelled)`). Se puede agregar en una V3. ⚠️ **OJO: la V3 ya
   existe y se usó para otro cosa** (compras/costo/cajas) → si se agrega el CHECK va en una V4.
2. `confirm()`/`cancel()` no usan bloqueo pesimista: dos peticiones simultáneas podrían pasar
   el mismo `if` y devolver el stock dos veces. Arreglo futuro: `@Lock(PESSIMISTIC_WRITE)` o
   `UPDATE ... WHERE cancelled = false`. **Aplica igual a `PurchaseImpl.confirm()` de V3.**
3. La regla de la compra es una **heurística de stock**, no un histórico: si otra compra repuso
   el mismo producto en medio, la cancelación podría pasar. Resolverlo requiere una tabla de
   movimientos.

### 2026-09-30 — FASE 1 del PLAN: paquete `compras` + prefijo `/api/local`

> Inicio de la transformación a "tienda de comida" (ver `docs/PLAN.md`).
> Esta sesión cubre SOLO la Fase 1 (base). Las Fases 2-8 siguen pendientes.
> Decisiones de `PLAN.md` §6 confirmadas el 2026-09-30: **(a) stock de platillo
> derivado de insumos por receta**, **(b) pasarela Stripe (modo test)**,
> **(c) solo "recoger en tienda" al inicio** (sin domicilio).

**Qué cambió**

1. **Paquete `com.erikjarquin.ventas` → `com.erikjarquin.compras`** (164 archivos
   .java, main + test, con `git mv` para que git registre el rename).
   `VentasApplication` → `ComprasApplication`, `VentasApplicationTests` →
   `ComprasApplicationTests`. `pom.xml`: `artifactId` `ventas` → `compras`
   (+ `name`/`description`, antes vacíos). `spring.application.name` ya era
   `compras`. El `Dockerfile` usa `target/*.jar` (glob), así que no hubo que
   tocarlo; solo su comentario de cabecera pasó a "Compras-Backend".
   ⚠️ **NO** se renombró la palabra "venta"/"ventas" en el dominio: `Venta`/`Sale`
   sigue siendo un concepto válido y distinto de `Pedido` (el online). Solo se
   renombró lo que es *identidad del proyecto*.
2. **Prefijo de rutas: los 12 controllers pasaron a `/api/local/...`** (p. ej.
   `ProductController` `@RequestMapping("/api/products")` →
   `"/api/local/products"`). También se actualizaron las ~19 referencias en
   javadoc `{@code ...}`.
3. **`SecurityConfig`**: la ruta pública es ahora `/api/local/auth/**` (antes
   `/api/auth/**`). Se mantiene `/api/uploads/**` como pública y **fuera** de
   `/api/local` a propósito: en la Fase 3 el catálogo público del storefront
   tendrá que servir las mismas imágenes de platillos.
4. **CORS**: el default de desarrollo ahora incluye los dos frontends →
   `${CORS_ALLOWED_ORIGINS:http://localhost:4200,http://localhost:3000}`. Se
   agregó `localhost:3000` (Next.js) ya para no olvidarlo en la Fase 3.
5. **Tests**: 85 URLs de MockMvc actualizadas al nuevo prefijo (13 clases).
   `javadoc` de `SecurityConfig` documenta la arquitectura de dos prefijos.

**Verificación**: `mvn test -Dtest='!ComprasApplicationTests'` → **129/129 en
verde, BUILD SUCCESS**. `ComprasApplicationTests` es el único que no corre: es
`@SpringBootTest` y exige BD real (hoy `application-local.yaml` apunta al
pooler de Supabase, que rechaza la conexión — pre-existente, no es defecto).

**Lo que NO se tocó (a propósito)**: no se creó ninguna entidad nueva. Siguen sin
existir `Insumo`, `Platillo`, `RecetaDetalle`, `Cliente`, `Pedido` ni
`DetallePedido`; el enum `PermissionName` sigue en **34** y los roles en 3
(ADMIN/CAJERO/ALMACENISTA). No hubo migración Flyway nueva: renombrar paquetes no
cambia el esquema, así que **`V1__init.sql` seguía siendo la única migración al cerrar esta
fase** (la 2ª, `V2__venta_confirmada.sql`, llegó ese mismo día: ver la entrada de arriba).

**Contraparte en el frontend** (commit en `Compras-Frontend-Local`):
`environment.api` pasó a derivar `apiLocal` (`${api}/local`) y los 12 servicios
HTTP usan `${environment.apiLocal}/...`. El `api` base sigue exportado porque es
la raíz de la que saldrán `${api}/tienda/...` (Fase 3) y `${api}/uploads`.

**⚠️ Trampa para el Angular**: si agregas un servicio nuevo, usa
`${environment.apiLocal}` (con `Local`), **nunca** `${environment.api}` directo.

### 2026-09-28 — SKU / Barcode duplicado → 409 con mensaje (YA IMPLEMENTADO)

> **Estado: IMPLEMENTADO** en el commit `1444d87` ("Sku y barcode duplicados").
> La versión anterior de este archivo lo dejaba como plan; ya no aplica.
> La contraparte en el frontend (`Compras-Frontend-Local/AGENTS.md`, commits
> `bfcdb43` y `1aa72b3`) también está implementada.

**Problema que había:** un SKU o barcode duplicado producía **HTTP 500** y el
usuario no veía nada. La cadena: `@Column(unique = true)` en `ProductEntity` +
`UNIQUE` en `V1__init.sql` → `repository.save()` reventaba el constraint →
`DataIntegrityViolationException` sin handler → 500 genérico.

**Lo que se implementó (5 archivos):**

1. **`repository/ProductRepository.java:74-82`** — 4 métodos:
   `existsBySku`, `existsBySkuAndIdNot`, `existsByBarcode`,
   `existsByBarcodeAndIdNot`. Los `...AndIdNot` son para el update (si no, editar
   sin cambiar el SKU se detectaría a sí mismo → falso positivo).
2. **`service/impl/ProductImpl.java`** — 2 helpers privados:
   `normalizeCode(String)` (vacío/blank → `null`, porque `""` también es un
   valor UNIQUE y haría chocar al segundo producto sin SKU) y
   `validateDuplicates(id, sku, barcode)` (lanza `ProductException` con
   `HttpStatus.CONFLICT`). Se llaman **antes** de `repository.save()` en `save()`
   (con `id == null`) y en `update()` (con el `id` real).
3. **`exceptions/GlobalExceptionHandler.java:227`** — handler de
   `DataIntegrityViolationException` como **red de seguridad** para la ventana de
   carrera (dos requests simultáneos con el mismo SKU). Inspecciona
   `ex.getMostSpecificCause().getMessage()` buscando `"sku"` / `"barcode"` y
   devuelve **409** con `code: "PRODUCT_ERROR"`. (El mensaje de PostgreSQL trae
   `key (sku)=(...) already exists`, por eso el `contains` funciona.)

**No hizo falta crear la excepción**: `ProductException(String, HttpStatus)` ya
existía y `GlobalExceptionHandler` ya la traducía a 409 con `code:
"PRODUCT_ERROR"`. Mismo patrón que `ProviderImpl` (RFC duplicado).

**Nada de esto cambió endpoints ni permisos**: `POST /api/local/products` sigue
devolviendo 200 en éxito y ahora 409 en duplicado. Total sigue en **57
endpoints / 34 permisos**.

**Test**: `ProductControllerTest.crearProducto_skuDuplicado_devuelve409`.

**⚠️ Conflicto con el roadmap (SIGUE VIGENTE)**: `docs/PLAN.md` §2.2 dice
*"Producto → Platillo: se quita `barcode`/`sku` (o quedan opcionales, `null`)"*.
Si ese refactor se hace en la Fase 2, **este trabajo queda obsoleto**. Decisión
tomada: como `normalizeCode` ya devuelve `null` para vacíos y `validateDuplicates`
salta los `null`, `sku`/`barcode` **pueden quedar como opcionales (`null`)** sin
tocar nada. Si se confirman como opcionales, no hay que rehacer este código.


### 2026-09-23 — Migraciones Flyway + puntería a Supabase + "compras" como identidad

> Objetivo: transformar el backend POS de abarrotes (`com.erikjarquin.ventas`) en un
> backend de **tienda de comida** (`compras`) con dos frontends: Angular local (POS) y
> Next.js cliente (storefront). El plan completo vive en `docs/PLAN.md` y
> `docs/Especificaciones_ComprasBackend_2026-09-23.pdf`.

1. **Flyway adoptado**: `pom.xml` agrega `spring-boot-starter-flyway` y `flyway-database-postgresql` (sin versión, la gestiona la BOM de Spring Boot 4). Se eliminó la dependencia del `ddl-auto:update` como creador de esquema en producción.
2. **`application.yaml`**: `spring.application.name: compras`; datasource con `url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:compras_db}?sslmode=${DB_SSLMODE:disable}`; `ddl-auto: ${JPA_DDL_AUTO:update}` con comentario de que en Supabase/prod va `validate`. Comentarios actualizados en el bloque datasource y jpa.
3. **`application-local.yaml` (gitignored)**: ahora apunta a **Postgres LOCAL** (`localhost`, `compras_db`, `postgres`/`admin`, `sslmode` disable) con un bloque de comentarios arriba que documenta EXACTAMENTE qué cambiar para volver a Supabase (host del Dashboard, `DB_SSLMODE: require`, `DB_NAME: postgres`, password real, puerto 5432 o 6543 del pooler). ⚠️ **Este archivo NO está en git → replicarlo en cada máquina**. PREVIO: estaba con host de Supabase (ver punto 5 del historial local).
4. **`V1__init.sql` generado** (`src/main/resources/db/migration/`, 659 líneas): dump de esquema puro de **`ventas_db` local** (`pg_dump -U postgres -h localhost -d ventas_db --schema-only --no-owner --no-privileges`) con las **13 tablas** + secuencias (GENERATED BY DEFAULT AS IDENTITY) + constraints (PK/FK/UNIQUE) + CHECKs de `permissions_name_check` (34) y `payments_status_check`/`sales_*_check`. ⚠️ **Se le eliminaron las directivas `\restrict`/`\unrestrict`** que pg_dump 18 antepone/cierra: son comandos de psql que Flyway NO entiende ("error de sintaxis en o cerca de «\»"). Si se regenera el dump a mano, quitarlas.
5. **BD local `compras_db`** verificada: Flyway aplicó v1 (13 tablas + `flyway_schema_history` = 14), los bootstraps sembraron **roles=3, permissions=34, usuario admin=1** y `POST /api/auth/login` responde 200 con token (admin `18jarquinsanchezerik1a@gmail.com` / `1234`). Marca "app arranca en local".
6. **Git limpio**: commits `24cc737 README` → `06eea4a Configuraciones y actualizaciones` → `15bd392 Supabase` (este último: yaml + V1 + restauración de tests). `git status` limpio.
7. **Tests**: restauración de la suite a su ruta/paquete original (`src/test/java/com/erikjarquin/ventas/**`) con `git restore --source=ef24245 --worktree -- src/test/java`. **130 tests en verde** (H2, sin Postgres) — solo `VentasApplicationTests` NO corre: es `@SpringBootTest` y exige BD real accesible (por eso en local con `admin` puede fallar el contexto; no es defecto).

**Estado de fases del PLAN** (ver `docs/PLAN.md` sección 6): decisiones PENDIENTES de confirmar — (1) BD nueva `compras_db` vs migrar `ventas_db`, (2) stock de platillo: derivado de insumos vs campo aparte, (3) pasarela Stripe (test) vs MercadoPago, (4) delivery o solo recoger, (5) lector código de barras/prods sin barcode.

### 2026-09-17 (3) — Borrado lógico de productos (dar de baja / reactivar)

> Objetivo: un producto con ventas/compras NO se puede borrar (409, FK). Ahora se
> puede "dar de baja" (soft delete) para sacarlo del catálogo conservando el
> histórico, y "reactivar" después. Mismo patrón que `UserEntity.active`.

1. **`ProductEntity.active`** (`@Column(nullable=false, columnDefinition="boolean default true")`, default `true`): borrado lógico. `ddl-auto:update` agrega la columna y las filas existentes quedan activas por el default.
2. **`ProductDto`**: nuevos campos `active` (estado) y `hasHistory` (calculado, no columna). El frontend los usa para pintar el badge "De baja" y decidir entre Eliminar (sin histórico) o Dar de baja (con histórico).
3. **`ProductMapper.toDto(entity)`** sigue existiendo y delega en el nuevo overload **`toDto(entity, boolean hasHistory)`**.
4. **`ProductRepository`**: `findByActiveTrue()`, `findByActiveFalse()`, `findByCategory_NameAndActiveTrue(...)`; `search(q)` y `findLowStock(threshold)` ahora agregan `AND p.active = true` (el POS no muestra ni busca productos dados de baja). Se conserva `findByCategory_Name` (sin filtro). Nuevas `findProductIdsWithSales()` / `findProductIdsWithPurchases()` (JPQL `SELECT DISTINCT d.product.id`) → `hasHistory` se calcula con 2 consultas, evitando N+1 (`exists...` por producto).
5. **`ProductService`/`ProductImpl`**: `getInactive()`, `deactivate(Long)` (`active=false`, conserva imagen), `activate(Long)` (`active=true`); `getAll()`/`getByCategory()` solo activos; `findByBarcode()` trata un inactivo como no encontrado (no se vende); `delete()` sigue siendo borrado FÍSICO y solo sin histórico (409 si lo tiene, ahora el mensaje sugiere dar de baja).
6. **`ProductController`**: `GET /api/products/inactive` (`VER_PRODUCTOS`), `PATCH /api/products/{id}/deactivate` (`DESACTIVAR_PRODUCTOS`), `PATCH /api/products/{id}/active` (`ACTIVAR_PRODUCTOS`). Se usa **PATCH** porque `DELETE /{id}` ya es el borrado físico. Total: **57 endpoints**.
7. **`PermissionName`**: 32 → **34** (`DESACTIVAR_PRODUCTOS`, `ACTIVAR_PRODUCTOS`). `PermissionBootstrap` los inserta solo; `RolePermissionBootstrap` NO es self-healing → en BD ya inicializada hay que asignarlos a mano desde la pantalla de Roles (a ALMACENISTA/roles que gestionen inventario).
8. **⚠️ BD local**: igual que en 2026-09-13 (2), el `CHECK permissions_name_check` no se actualiza con `ddl-auto:update` y el arranque fallaba al insertar los 2 permisos nuevos. Se droppeó en `ventas_db` (`ALTER TABLE permissions DROP CONSTRAINT IF EXISTS permissions_name_check;`). En Railway BD nueva lo creará con los 34 nombres; en BD existente, dropear.
9. **Tests**: nuevo `ProductImplTest` (8, Mockito: baja conserva imagen, baja repetida 409, reactivar, listar inactivos, listar activos usa `findByActiveTrue`, barcode inactivo no se encuentra, delete con histórico 409 sin borrar imagen, delete sin histórico borra imagen+fila) + 4 en `ProductControllerTest` (inactive, deactivate, activate, 403 sin permiso). **118 → 130 tests en verde**.
10. **Pendiente (fase frontend)**: vista "Productos dados de baja" (los scaffolds `deactivated-products/` del frontend están vacíos) + botones Dar de baja/Reactivar conectados a los nuevos endpoints.

### 2026-09-17 (2) — Flag de seed + fin del self-healing + tests de controllers

1. **Flag `APP_SEED_BOOTSTRAPS`** (default `true`, `application.yaml:82`): los 4 bootstraps lo inyectan con `@Value("${app.seed-bootstraps:true}")` y hacen `return` temprano si está en `false` (entornos donde el admin gestiona roles/permisos a mano).
   - ⚠️ **Trampa**: en `false` con la BD **vacía** no se siembra NADA (ni roles ni admin) → no se puede loguear. En `application-local.yaml` quedó en `true` con ese aviso comentado. Ponlo en `false` SOLO en BD ya inicializadas o producción.
2. **`RolePermissionBootstrap` dejó de ser self-healing** (bug reportado: borrabas CAJERO y al reiniciar volvía a crearse / reasignaba permisos en cada arranque). Ahora **siembra solo la primera vez**: si algún rol base ya tiene permisos, omite el seed y respeta la gestión manual. Los roles borrados a mano NO se recrean (se omiten) y ya no lanza excepción si falta ADMIN/CAJERO/ALMACENISTA.
   - `RoleBootstrap` sigue sembrando los 3 roles **solo si la tabla `roles` está vacía**; `AdminBootstrap` solo crea el admin **si su email no existe**; `PermissionBootstrap` solo **inserta los faltantes**.
   - **Usuarios NO tienen doble ejecución**: si cambias la contraseña del admin, persiste al reiniciar (verificado).
3. **Permisos `VER_CLIENTES` y `VER_FACTURAS` eliminados** (módulos clientes/facturas descartados): enum 34 → **32**. La caja se conserva para el POS. ⚠️ Corrige lo que decía la sesión "2026-sesión" punto 3 (que los conservaba).
4. **Tests de controllers**: 10 clases nuevas (`@WebMvcTest` + `@WithMockUser` + `@MockitoBean`) en `src/test/.../controller/`; `pom.xml` habilitó `spring-boot-starter-webmvc-test` y `spring-boot-starter-security-test`. Nuevo `RolePermissionBootstrapTest` (4 casos: ya sembrado no reasigna, BD vacía siembra, rol borrado no falla, flag off no hace nada). **118 tests en verde**. Bug corregido: `ReportControllerTest` mezclaba matchers con valor crudo (`isNull(), isNull(), 10` → `eq(10)`).
5. `mvn test` verificado OK.

### 2026-09-17 — Eliminado `GET /ping` + limpieza de referencias

1. **`pingController` eliminado** (`controller/pingController.java`): otro equipo lo recreó (siguió instrucciones de una sesión vieja de este archivo) pero el proyecto NO lo necesita. El borrado afecta: contadores (eran "13 controllers" → **12**) y la fila `GET /ping` de la tabla de endpoints (quitada).
2. **Limpieza en `SecurityConfig`**: quitado `.requestMatchers("/ping").permitAll()` y las referencias a `/ping` en javadoc/comentarios. Rutas públicas ahora: `OPTIONS /**`, `/api/auth/**`, `/api/uploads/**`.
3. ⚠️ **No usar `/ping` como healthcheck de Railway** (ya no existe). Opciones: quitar el healthcheck custom o apuntarlo a un endpoint público (`/api/auth/...`) — ver pasos de deploy abajo.

### 2026-09-15 — Corrección FK en borrados + bloqueo productos con histórico

1. **Productos con histórico no se borran**: `ProductImpl.delete` verificaba
   `deleteById` sin comprobar referencias → al borrar un producto con ventas o
   compras, PostgreSQL lanzaba FK violation y el frontend recibía 500. Ahora se
   consulta `ProductRepository.existsBySaleDetailsProductId` / `exists...Purchase...`
   (JPQL sobre `SaleDetailEntity`/`PurchaseDetailEntity` con `product.id = :id`).
   Si existe histórico → `ProductException` **409** con mensaje claro. Nueva
   `exceptions/ProductException.java` + handler `PRODUCT_ERROR` en
   `GlobalExceptionHandler`. (El borrado físico solo aplica a productos NUNCA
   vendidos/comparados; el stock como 0 o edición son las alternativas.)
2. **Categorías y roles: fin de la dependencia de lazy-loading**: `delete` de
   `CategoryImpl`/`RoleImpl` tocaban colecciones lazy (`getProducts()`/`getUsers()`)
   fuera de transacción (dependía de `open-in-view` para no reventar). Ahora usan
   conteos SQL: `ProductRepository.countByCategory_Id` y
   `UserRepository.countByRole_Id` — mismo resultado 409, sin sorpresas.
3. **Usuarios**: no aplica (nunca se borran físicamente, solo `active=false`).
4. **Imágenes huérfanas → 404, no 500**: el bug original borraba la imagen de disco
   antes de fallar el DELETE, dejando productos con `img` apuntando a un archivo
   inexistente → el navegador pedía `/api/uploads/x.jpg` y Spring lanzaba
   `NoResourceFoundException` → 500. Ahora `GlobalExceptionHandler` la mapea a
   404 silencioso.
5. **Stock 0 permitido**: el formulario de productos tenía `min="1"` en stock y
   `validateNumber` forzaba ≥1; se cambió a min 0 (el precio sigue ≥1). Nota: el
   borrado NO depende del stock — un producto con ventas/compras no se borra
   (409) aunque tenga stock 0.
6. `mvn compile`, `ng build` y **35 tests en verde** verificado.

### 2026-sesión — Limpieza final + configuración local + comentarios
1. **`application-local.yaml` CREADO** en `src/main/resources/` (gitignored). Sin él la app NO arranca en local (los `${VAR}` sin default fallan rápido). Contiene: `DB_USER/PASSWORD`, `MAIL_USERNAME/PASSWORD`, `JWT_SECRET` (generado, ≥32 bytes), `ADMIN_EMAIL/PASSWORD`, `PAYMENT_MERCHANT_ID/TERMINAL_ID/KEYSTORE_PASSWORD`, `CORS_ALLOWED_ORIGINS`. ⚠️ Si agregas un `${VAR}` nuevo a `application.yaml`, agrégalo también aquí.
2. **Asimetría CAJERO corregida**: `RolePermissionBootstrap` ahora da `VER_CAJA` al rol CAJERO. Antes podía abrir/cerrar caja pero `GET /api/cash/active` le daba 403 al cargar el POS.
3. **4 permisos huérfanos eliminados** del enum `PermissionName` (38 → **34**): `CANCELAR_VENTAS`, `EXPORTAR_REPORTES`, `VER_CONFIGURACION`, `EDITAR_CONFIGURACION` (ningún endpoint ni guard del frontend los usaba). Se conservaron `VER_CLIENTES` y `VER_FACTURAS`: el frontend los usa como guard de rutas `/clientes` y `/facturas` (módulos pendientes). Referencia en javadoc de `ReportController` actualizada.
4. **Comentarios didácticos añadidos** en componentes clave (ver abajo).
5. `mvn compile` verificado OK.

### 2026-09-13 (2) — Módulo de Compras completo (backend + frontend)

**Backend**
1. **Campo `cost`** en `ProductEntity` (BigDecimal, default 0): último costo real comprado. Se expone en `ProductDto`/`ProductMapper`; lo escribe el módulo de Compras (no el CRUD de productos).
2. **Entidades nuevas**: `ProviderEntity` (`providers`: name, rfc UNIQUE, phone, email), `PurchaseEntity` (`purchases`: provider N:1, detalles 1:N cascade ALL + orphanRemoval, total) y `PurchaseDetailEntity` (`purchase_details`: product N:1, quantity, unitCost, subtotal). Creadas por `ddl-auto: update`.
3. **Repos**: `ProviderRepository` (findByRfc para 409, findAllByOrderByNameAsc) y `PurchaseRepository` con **`@EntityGraph`** (`provider` + `details.product`) en todos los listados para evitar N+1; `countByProvider_Id` para bloquear borrado de proveedor con compras (409).
4. **Permisos**: 32 → **38** (`CREAR_COMPRAS`, `CANCELAR_COMPRAS`, `VER_PROVEEDORES`, `CREAR_PROVEEDORES`, `EDITAR_PROVEEDORES`, `ELIMINAR_PROVEEDORES`). El bootstrap los crea solo; `RolePermissionBootstrap` ahora da a ALMACENISTA ver/crear compras y ver proveedores.
5. **⚠️ BD local**: Hibernate genera un CHECK `permissions_name_check` para `@Enumerated(STRING)` que **NO se actualiza** con `ddl-auto:update` → al agregar permisos al enum el arranque fallaba con "viola la restricción check". Se droppeó el constraint en la BD local (ALTER TABLE permissions DROP CONSTRAINT). En Railway la BD es nueva y creará el CHECK con los 38 nombres.
6. **`ProviderException`/`PurchaseException`** (+ handlers `PROVIDER_ERROR`/`PURCHASE_ERROR`). Reglas: RFC duplicado → 409, proveedor con compras → 409, proveedor/compra no encontrada → 404, items vacíos/cantidad≤0/costo negativo → 400.
7. **`PurchaseImpl.create`** (@Transactional): arma cabecera+renglones, subtotal = unitCost*qty, **stock += qty** y **product.cost = unitCost** (costo real vigente). **`cancel`** revierte stock (mín 0) y elimina la compra (el costo no se toca: sigue siendo el último vigente).
8. **`GET /api/reports/margins`** (`VER_REPORTES`) con `MarginDTO`: margin = price-cost, marginPercent = margin/price*100, ordenados asc (menores márgenes primero = prioridad de renegociación).
9. **Tests**: `ProviderRepositoryTest` (6, H2: RFC, orden, count, EntityGraph) y `PurchaseImplTest` (5, Mockito: stock+cost, 409 proveedor, items vacíos, cancelación revierte y elimina, 404). **Suite completa: 35 tests en verde**.
10. `mvn compile` y `mvn test` verificados OK.

**Frontend (Ventas-Frontend)**
11. `interfaces/purchases/purchases.ts` (espejo de los DTO) + `MarginDTO` en reports; servicios nuevos `provider-service.ts` y `purchase-service.ts`; `getMargins()` en `report-service.ts`.
12. **Página `/compras` reemplaza el placeholder**: 3 pestañas — **Compras** (tabla con fecha/proveedor/total, detalle expandible con renglones, cancelar solo con `CANCELAR_COMPRAS`, filtro por proveedor, modal "Nueva compra" con renglones producto/cantidad/costo y fecha opcional), **Proveedores** (CRUD con RFC, teléfono, email) y **Márgenes** (tabla costo/precio/margen/% con badges de color, requiere `VER_REPORTES`).
13. `ng build` verificado OK (warnings comunes benignos de jspdf/canvg/html2canvas).

### 2026-09-09 — Corrección de 3 bugs + ajuste imágenes
1. **`@EnableScheduling` agregado** en `VentasApplication.java` → `PaymentMonitorJob` ahora se ejecuta (antes estaba inactivo).
2. **`CategoryRepository.findByName`** corregido: retornaba `Optional<RoleEntity>` → ahora `Optional<CategoryEntity>` (e import eliminado).
3. **Almacenamiento real de imágenes**: nuevo `FileStorageService` (guarda en disco con nombre UUID + extensión), `WebConfig` sirve `/api/uploads/**`, `ProductImpl` guarda/actualiza/borra archivo físico junto al producto. Config: `app.upload-dir: ./uploads`.
4. **URL de imagen en DTO**: `ProductMapper` convertido a `@Component` (patrón de SaleMapper/PaymentMapper), inyecta `app.upload-url` y devuelve URL completa (`http://localhost:8081/api/uploads/<archivo>`). Frontend usaba `[src]="p.img"` directo → ahora resuelve. `ProductImpl` inyecta el mapper (ya no usa llamadas estáticas).

## Pendientes / issues conocidos

- 🔜 **Fases 2-8 de `docs/PLAN.md`** (lo siguiente: Fase 2 = Platillos + Insumos + Recetas). Antes de escribir código nuevo, leer la entrada del 2026-09-30 de arriba.
- 🔜 **Migrar el correo de recuperación a Resend** (Railway Hobby bloquea SMTP). **Opción B decidida, NADA implementado todavía**: interfaz `EmailSender` + SMTP + Resend con selector `app.mail.provider`. Leer la entrada del **2026-10-07** completa antes de tocar una línea: avisa de la trampa de los `${MAIL_USERNAME}` sin default (rompe el arranque si se comentan) y de que `AuthImpl` cambia en **2 líneas**, no se comenta.
- 🔴 **Defecto de seguridad abierto en `AuthImpl.java:93`**: `forgotPassword` lanza `UserException` si el correo no está registrado, lo que permite **enumerar qué correos existen**. Es ortogonal a Resend y hay que tratarlo aparte.
- ✅ ~~SKU/Barcode duplicado devuelve 500~~ → **resuelto** (409). Ver sesión 2026-09-28.
- ✅ ~~Doble Enter cobra dos veces~~ → **resuelto en V6** (clave de idempotencia + índice UNIQUE). Ver sesión 2026-10-04 y el PDF 07. Requisito: el cliente debe mandar la clave; sin ella la venta se crea normal (compatibilidad con clientes viejos).
- ⚠️ **`/ping` sigue sin existir** — no usarlo como healthcheck de Railway.
- ⚠️ **Secretos en historial de git**: purgar con `git filter-repo` antes de publicar el repo.
- **Imágenes**: sin perfil dev/prod separado en el frontend para `environment-prod.ts` (requiere definir la API de Railway al desplegar).
- **Tests**: **294 en verde** con '-Dtest='!ComprasApplicationTests' (repos + servicios + file storage + 12 controllers WebMvc + bootstraps + 11 de idempotencia + 15 de campos obligatorios + 6 del validador). `ComprasApplicationTests` (`@SpringBootTest`) solo corre contra una BD real accesible.
- Frontend: módulos **Clientes y Facturas descartados** (permisos eliminados). `Caja` sigue como placeholder porque su "hoja de corte" vive hoy en Reportes. `reversePayment` del backend no tiene UI (requiere un listado/detalle de pagos).
- **BD local**: el CHECK `permissions_name_check` (generado por Hibernate para `@Enumerated`) NO se actualiza con `ddl-auto:update`. Al agregar permisos al enum el arranque puede fallar con "viola la restricción check" → droppear el constraint en BD local (`ALTER TABLE permissions DROP CONSTRAINT permissions_name_check`) o usar BD nueva. Con Flyway en `validate` el CHECK lo define la migración V1 (34) — mantenerla sincronizada con `PermissionName`.
- **Código pendiente**: falta cambiar `System.out.println` de los bootstraps por un logger. ⚠️ Al estar trabajando entre máquinas, un AGENTS.md desactualizado hizo que otra laptop recreara `pingController`; ya está eliminado de nuevo (ver sesión 2026-09-17) — no recrearlo.
- ⚠️ **`application-local.yaml` NO está en git**: en cada máquina hay que regenerarlo (copiar el patrón de los comentarios del archivo más reciente). Sin él la app no arranca en local (los `${VAR}` sin default fallan rápido).
- ⚠️ **Supabase provisional**: el host `db.dohbigwwsqgcovppihlb.supabase.co` resolvía SOLO a IPv6 (sin registro A) desde esta máquina → `UnknownHostException`. Verificado que MySQL/local no es el problema: **la app corre en local perfecto**. Para conectar a Supabase revisar el host correcto en el Dashboard (proyecto pausado o host del pooler 6543) y cambiar el bloque de `application-local.yaml` (ver comentarios del archivo).

## Cómo ejecutar

```sh
.\mvnw.cmd compile      # compilar
.\mvnw.cmd test         # tests (H2; ComprasApplicationTests requiere BD real)
.\mvnw.cmd spring-boot:run   # arranca en http://localhost:8081
```

> 💡 Para la suite completa en verde sin BD real:
> `.\mvnw.cmd test -Dtest='!ComprasApplicationTests'` → 129/129.
> `ComprasApplicationTests` es el único `@SpringBootTest` y exige una BD real
> accesible; hoy `application-local.yaml` apunta al pooler de Supabase, que
> rechaza la conexión (falla por credenciales, no es defecto del código).

> ⚠️ **En otra máquina**: (1) recrear `src/main/resources/application-local.yaml`
> (gitignored) con los valores REALES de dev — sin él la app no arranca. Apuntar a
> Postgres local (`DB_HOST=localhost`, `DB_NAME=compras_db`, `DB_SSLMODE=disable`)
> o a Supabase siguiendo el bloque de comentarios. (2) Asegurar que la BD `compras_db`
> exista (Flyway crea las tablas). Los pasos de Dockerfile/Railway siguen igual.

### 2026-09-12 — Seguridad: secretos externalizados (preparación Railway)
1. **`application.yaml` reescrito**: todos los valores sensibles como `${VAR}` (sin defaults: `DB_USER`, `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `PAYMENT_MERCHANT_ID`, `PAYMENT_TERMINAL_ID`, `PAYMENT_KEYSTORE_PASSWORD`). `spring.profiles.default: local`.
2. **`application-local.yaml` creado** (gitignored): valores reales de dev. **En Railway los valores van en las Variables del servicio** (esto es "dónde viven" de donde sale `admin`/`postgres`/Gmail/JWT).
3. **JWT**: `JwtUtil` inyecta `@Value("${JWT_SECRET}")` (deja el estático). Admin creado desde `ADMIN_EMAIL`/`ADMIN_PASSWORD`.
4. **CORS global** en `SecurityConfig` con `${CORS_ALLOWED_ORIGINS}` (split por `,`); se quitaron los `@CrossOrigin` de Product/Sale/Cash/Category/Role.
5. **GlobalExceptionHandler** reescrito (400 validación/argumento, 401/403, 500). `CashException`/`UserException`/`PaymentException` llevan HttpStatus; runtime de negocio convertidos a 400/404/409 en User/Cash/Product; `ErrorResponse` (no usado) eliminado.
6. **Imágenes seguras**: `FileStorageService` valida tamaño 5MB + extensión + magic bytes; `ProductMapper` sigue devolviendo URL completa.
7. **`TerminalPhysicalImpl`**: bug corregido — REV/STS ahora sí se envían por el socket. `TestCardRepository` tiene javadoc de "solo prueba".
8. **`GET /ping`** creado (`controller/pingController.java`, público) — antes no existía pese a lo que decía este archivo. ⚠️ **Eliminado después** (ver sesión 2026-09-17): ya no se necesita como healthcheck.
9. **Dockerfile multi-etapa** (build maven → jre21), `server.port=${PORT:8081}`; `.dockerignore`/`.gitignore` actualizados con `uploads/` fuera del repo. (Este `AGENTS.md` es un archivo de contexto del proyecto y SÍ se sube a git para llevar el historial de decisiones entre máquinas.)
10. Comentariados: controllers, bootstraps, interfaces, impls, servicios de config, entidades, mappers, repos, enums.
11. **`mvn compile` verificado OK**. NADA commiteado aún. Secretos previos siguen en el historial de git → **purgar con `git filter-repo` antes de publicar el repo**. `PaymentMonitorJob` movido a paquete `jobs/`.

### 2026-09-12 (2) — Tests + self-healing + deploy
1. **Tests nuevos (15, sobre H2, SIN Postgres)**: `UserRepositoryTest` (findByEmail, JOIN FETCH rol+permisos, resetToken), `ProductRepositoryTest` (barcode, categoría, `search(q)` case-insensitive), `FileStorageServiceTest` (extensión, magic bytes, 5MB, store/delete). Importante: en Spring Boot 4 `@DataJpaTest` vive en `org.springframework.boot.data.jpa.test.autoconfigure`. Ejecutar: `.\mvnw.cmd test -Dtest="UserRepositoryTest,ProductRepositoryTest,FileStorageServiceTest"`.
2. **`RolePermissionBootstrap`**: ahora REASIGNA los permisos en cada arranque (self-healing: cambias la lista → se sincroniza solo al reiniciar). Ya no es "solo si está vacío".
3. **`CashRegisterEntity`**: eliminado el bloque de código comentado (relación a User) — documentado que está planeado, sin FK.
4. **Dockerfile**: usuario **no-root** (`appuser` UID 1001, grupo appgroup). Los Bootstraps exigen `@Transactional` aún dentro del startup.
5. **`app.upload-dir: ${UPLOAD_DIR:./uploads}`**: en Railway apunta al **Volumen persistente** montado en `/app/uploads`.

### 2026-09-13 — Frontend conectado + PUT categorías + estados HTTP de roles

**Backend**
1. **`RoleException`** nueva (404 por defecto, HttpStatus configurable). `RoleImpl`: borrar rol con usuarios → **409** "rol con usuarios asignados"; rol no encontrado → 404; nombre duplicado → 409. Validaciones de input → `IllegalArgumentException` (400). `GlobalExceptionHandler` añade handlers `ROLE_ERROR` y `CATEGORY_ERROR`.
2. **`CategoryException`** nueva; `CategoryImpl`: borrar categoría con productos → **409**, no encontrada → 404, nombre duplicado al crear/editar → 409, nombre vacío → 400.
3. **`PUT /api/categories/{id}`** nuevo (permiso **`EDITAR_CATEGORIAS`** — ahora 32 permisos; los bootstraps lo sincronizan solos al arrancar).
4. `mvn compile` verificados y build Angular OK.

**Frontend (Ventas-Frontend)**
5. **SaleHistory ruteado** en `/salehistory` (guard `VER_VENTAS`), con datos reales de `GET /api/sales`, paginación y fecha/método formateados; enlace en el sidebar.
6. **Categorías**: `updateCategory()` (PUT) + modo edición (botón Editar/Cancelar, botón guarda dice Actualizar); errores 409 del backend se muestran con `alert`.
7. **Roles**: el borrado con usuarios muestra el mensaje del backend (409) en vez de fallar en silencio.
8. **Pagos**: botón **"Reintentar pago"** en el modal de tarjeta cuando un pago fue REJECTED (usa `POST /api/payments/retry/{id}`, reintroduce polling). `reversePayment` sigue en el servicio (sin UI: solo aplica a pagos APPROVED, requiere listado de pagos).
9. **`environment-prod.ts`**: placeholder documentado con pasos exactos para reemplazarlo con el dominio Railway al desplegar (build prod usa `fileReplacements`).

### 2026-09-12 (3) — Módulo de Reportes completo (backend + frontend)

**Backend** (agregación EN SQL, el frontend solo grafica)
1. **DTOs** en `model/dto/Reports/`: `PeriodSalesDTO`, `TopProductDTO`, `PaymentMethodDTO`, `CategoryPerformanceDTO`, `LowStockDTO`, `ReportsSummaryDTO` (con desglose `paymentMethods`).
2. **`ReportGroup`** enum (`DAY`/`MONTH`/`YEAR`) y **`ReportsMapper`** estático (labels CASH→Efectivo/DEBIT→Tarjeta débito/CREDIT→Tarjeta crédito, categoría null→"Sin categoría").
3. **`SaleRepository`** reescrita con queries JPQL agregadas (solo `PaymentStatus.APPROVED` + rango): `sumSalesByDay/Month/Year` (usa `CAST(saleDate AS date)`, `year()/month()` — portable H2/Postgres), `findTopSellingProducts` (ORDER BY SUM(qty) DESC + Pageable), `findPaymentMethodDistribution`, `findCategoryPerformance` (LEFT JOIN producto→categoría), `sumSalesTotals` (COUNT/SUM/AVG).
4. **`ProductRepository.findLowStock(threshold)`** (stock ≤ umbral, ordenado asc). **`ReportsService`** + **`ReportsImpl`** (defaults from=2000-01-01/to=hoy, `from<=to`→400, límite top 1-100, umbral ≥0).
5. **`ReportController`** `/api/reports/{trend, top-products, payment-methods, categories, low-stock, summary}` — todos con `@PreAuthorize("hasAuthority('VER_REPORTES')")`; `from`/`to` opcionales ISO.
6. **`ReportQueryTest`** (H2, 8 tests): día/mes/año excluyen REJECTED, top ordenado+limite, distribución por método, categorías LEFT JOIN incluye productos sin categoría, stock bajo, resumen. **Suite completa: 24 tests en verde** (`mvn test`).

**Frontend (Ventas-Frontend)**
7. Dependencias nuevas: **chart.js** y **xlsx** (jspdf+autotable ya estaban). `npm install` audita 44 vulnerabilidades (transitivas, sin fix no-forzado).
8. **`interfaces/reports/reports.ts`** espejo exacto de los DTO; **`report-service.ts`** con `HttpParams` solo-para-definidos.
9. **Página `/reportes`** reemplaza el placeholder: filtros (desde/hasta/agrupación/top N/umbral), tarjetas resumen (`summary`), 4 gráficas Chart.js (línea tendencia, barras top productos, dona métodos de pago, barras categorías), tabla stock bajo con badges, y **corte de caja** (`getSummary` de `/api/cash/summary`) con Imprimir (media-print que solo muestra el corte), PDF y Excel.
10. **Exportaciones**: PDF multi-sección con jsPDF+autotable; XLSX multi-hoja con SheetJS; corte con sus propios PDF/Excel. Todo en cliente (SSR-safe: `isPlatformBrowser` guarda la creación de Chart y `window.print`).
11. `ng build` verificado OK (warnings CommonJS benignos de jspdf/canvg/html2canvas).

## Pendiente (próximos pasos)
- **Deploy Railway** (guía completa):
  1. Subir repo a GitHub (purgar antes el historial con `git filter-repo`).
  2. Railway → New Project → Deploy from GitHub repo → la detección usará el Dockerfile.
  3. **Variables** (sin defaults → obligatorias): `DB_USER`, `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `JWT_SECRET` (generar con `openssl rand -base64 48`), `ADMIN_EMAIL`, `ADMIN_PASSWORD` (robusta), `PAYMENT_MERCHANT_ID`, `PAYMENT_TERMINAL_ID`, `PAYMENT_KEYSTORE_PASSWORD`. Con default ajustable: `CORS_ALLOWED_ORIGINS=https://<app>.netlify.app`, `FRONTEND_URL=https://<app>.netlify.app`, `UPLOAD_DIR=/app/uploads`, `UPLOAD_URL=https://<backend>.up.railway.app/api/uploads`, `JPA_DDL_AUTO=update` (usar `validate` en prod madura), `SHOW_SQL=false`, `PAYMENT_TERMINAL_TYPE=SIMULATED` (o `PHYSICAL` con host/keystore).
  4. **Postgres**: Railway Postgres (no usar el local). `DB_HOST`/`DB_PORT` serán los del servicio Postgres.
  5. **Volumen**: service → Volumes → create volume montado en `/app/uploads` (UID 1001).
  6. **Healthcheck de Railway**: ⚠️ `/ping` YA NO EXISTE (eliminado en 2026-09-17). Quita el healthcheck HTTP custom o apúntalo a un endpoint público como `GET /api/auth/me` (responde sin token con 401/403 pero confirma que la app está viva).
  7. **HTTPS**: el dominio `*.up.railway.app` trae HTTPS automático; el frontend Netlify usa la URL https del backend.
- Frontend Angular (Netlify): definir en `environment.prod.ts` la API = `https://<backend>.up.railway.app/api`.
- Purgar historial de git (secrets + uploads), iniciar repo limpio y commitear.
- Pendiente de calidad: cobertura de controllers con WebMvc/Mockito (de momento los tests cubren repos, servicios y file storage).