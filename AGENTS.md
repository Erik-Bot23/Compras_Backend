# Ventas-Backend — Contexto del Proyecto

> Este archivo registra el estado actual del proyecto y las decisiones tomadas. Se actualiza conforme avanzamos. Es el punto de referencia de contexto para continuar el trabajo desde cualquier máquina.

## Stack

- **Spring Boot 4.0.5** · Java 21 · Maven (`mvnw.cmd`)
- **PostgreSQL** `ventas_db` (localhost:5432, usuario `postgres` / `admin`)
- **Spring Data JPA** (Hibernate 7.2.7), `ddl-auto: update` (sin migraciones)
- **Spring Security + JWT** (jjwt 0.11.5, HS256, expiración 5h, secret desde `${JWT_SECRET}`)
- **spring-boot-starter-mail** (Gmail SMTP para recuperar contraseña)
- **Lombok** (parcial), validación, multipart `uploads` (máx 20MB)
- **Server**: puerto `${PORT:8081}` · CORS global `${CORS_ALLOWED_ORIGINS}`
- **Terminal de pagos**: `payment.terminal.type=SIMULATED` (default) o `PHYSICAL` (Socket TCP)

## Arquitectura

Capas: `controller/ → service/ (interfaz) → service/impl/ → repository/ → model/entity/`. DTOs en `model/dto/`, Mappers en `mapper/` (algunos estáticos, otros `@Component`), config en `config/`, excepciones en `exceptions/`.

- **12 controllers, 54 endpoints** (`/api/...`)
- Autorización por permiso vía `@PreAuthorize("hasAuthority('...')")` (**32 permisos** en `PermissionName`)
- 4 `CommandLineRunner` de bootstrap (orden): `RoleBootstrap` → `AdminBootstrap` → `PermissionBootstrap` → `RolePermissionBootstrap`. Todos respetan el flag `app.seed-bootstraps` (`APP_SEED_BOOTSTRAPS`, default `true`) y siembran **solo si está vacío** (NO self-healing; ver 2026-09-17 (2)).
- Admin inicial: `18jarquinsanchezerik1a@gmail.com` / `1234`

## Endpoints principales

| Módulo | Base | Notas |
|---|---|---|
| Auth | `/api/auth` | login, forgot/reset/change-password, me/authorities — público |
| Users | `/api/users` | CRUD + `PATCH /{id}/active` |
| Roles | `/api/roles` | CRUD |
| Permissions | `/api/permissions` | GET all |
| Products | `/api/products` | CRUD multipart, `/barcode/{bc}`, `/search?q=`, `?category=` |
| Categories | `/api/categories` | CRUD |
| Sales | `/api/sales` | POST (efectivo/transit/tarjeta), GET |
| Payments | `/api/payments` | card, status/{tx}, retry/{id}, reverse/{id} |
| Purchases | `/api/purchases` | GET (todos, por proveedor, detalle), POST, DELETE (cancelar) |
| Providers | `/api/providers` | CRUD |
| Cash | `/api/cash` | open, close, summary, active |
| Reports | `/api/reports` | trend, top-products, payment-methods, categories, low-stock, **margins**, summary (todas con `VER_REPORTES`) |
| Uploads | `/api/uploads/**` | **Público** — sirve imágenes de productos |

## Estado de módulos

- **Terminal pagos**: abstracción `TerminalService` con `TerminalSimulatedImpl` (tarjetas de prueba DETERMINISTIC) y `TerminalPhysicalImpl` (Socket TCP, protocolo `PAY|`/`REV|`/`STS|`).
- **Pagos**: estados PENDING/PROCESSING/APPROVED/REJECTED/REVERSED, reintentos (máx 3), idempotencia, reversas. `PaymentMonitorJob` (`@Scheduled`, cada 5 min) revisa pagos PENDING > 5 min.
- **Frontend conectado**: Angular 21 standalone + SSR en `Ventas-Frontend` (ruta hermana), consume `http://localhost:8081/api`. Espera que `img` de productos sea **URL completa**.
- **Reportes**: dashboard hecho (ver sesión 2026-09-12 (3)). Las consultas agregan EN SQL y devuelven DTO; el frontend solo grafica con Chart.js (SSR-safe) y exporta PDF/Excel en cliente.
- **Compras**: módulo completo (ver sesión 2026-09-13 (2)). Proveedores (CRUD, RFC único) + compras con renglones que al registrarse SUMAN stock y guardan el costo real del producto; cancelación revierte stock. `margins` (costo real vs precio) en reportes.

## Registro de cambios / decisiones

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

- ⚠️ **Secretos en historial de git**: purgar con `git filter-repo` antes de publicar el repo.
- **Imágenes**: sin perfil dev/prod separado en el frontend para `environment-prod.ts` (requiere definir la API de Railway al desplegar).
- **Tests**: **118 en verde** (repos + servicios + file storage + 10 controllers WebMvc + bootstraps).
- Frontend: módulos **Clientes y Facturas descartados** (permisos eliminados). `Caja` sigue como placeholder porque su "hoja de corte" vive hoy en Reportes. `reversePayment` del backend no tiene UI (requiere un listado/detalle de pagos).
- ⚠️ **BD local**: el CHECK `permissions_name_check` (generado por Hibernate para `@Enumerated`) NO se actualiza con `ddl-auto:update`. Al agregar permisos al enum el arranque puede fallar con "viola la restricción check" → droppear el constraint en BD local (`ALTER TABLE permissions DROP CONSTRAINT permissions_name_check`) o usar BD nueva.
- **Código pendiente**: falta cambiar `System.out.println` de los bootstraps por un logger. ⚠️ Al estar trabajando entre máquinas, un AGENTS.md desactualizado hizo que otra laptop recreara `pingController`; ya está eliminado de nuevo (ver sesión 2026-09-17) — no recrearlo.

## Cómo ejecutar

```sh
.\mvnw.cmd compile      # compilar
.\mvnw.cmd test         # tests (requiere PostgreSQL local levantado)
.\mvnw.cmd spring-boot:run
```

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