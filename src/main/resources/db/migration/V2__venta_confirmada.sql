-- ============================================================================
--  V2__venta_confirmada.sql
-- ----------------------------------------------------------------------------
--  POR QUÉ ESTA MIGRACIÓN EXISTE
--  ----------------------------------------------------------------------------
--  Problema detectado el 2026-09-30 (inconsistencia de inventario):
--
--    1. Una COMPRA suma stock al producto  (PurchaseImpl.create)
--    2. Una VENTA  resta stock al producto (SaleImpl.processSale)
--    3. CANCELAR la compra volvía a restar stock (PurchaseImpl.cancel)
--
--    Escenario que rompía los datos:
--      comprar 10  -> stock = 10
--      vender   5  -> stock = 5
--      cancelar la compra -> 5 - 10 = -5  ... y el código hacía
--      stock = Math.max(-5, 0) = 0
--
--    O sea: se vendieron 5 unidades que NUNCA se compraron, y el `Math.max`
--    escribía un inventario FALSO en lugar de rechazar la operación. El bug
--    no era solo "el número quedó raro": era un dato corrupto y silencioso.
--
--  LA REGLA DE NEGOCIO QUE SE IMPONE
--  ----------------------------------------------------------------------------
--  Una venta tiene un ciclo de vida de DOS estados terminales:
--
--    venta NO confirmada  -> se puede ANULAR (error de caja, mal cobro)
--    venta CONFIRMADA     -> CONGELADA para siempre (el cliente ya se llevó
--                            la comida; el dinero y el stock son reales)
--
--  Y en consecuencia: si el stock de una compra ya fue consumido por una
--  venta, esa compra YA NO SE PUEDE CANCELAR. Se rechaza con HTTP 409 y NO
--  se escribe nada en `products.stock`.
--
--  POR QUÉ UNA MIGRACIÓN Y NO SÓLO LAS ENTIDADES JPA
--  ----------------------------------------------------------------------------
--  `application.yaml` trae `spring.jpa.hibernate.ddl-auto: ${JPA_DDL_AUTO:validate}`.
--  Con `validate` Hibernate COMPRUEBA que el esquema coincide con las entidades
--  pero NUNCA crea columnas. Agregar un campo a SaleEntity sin esta migración
--  haría fallar el arranque con "Schema-validation: missing column".
--  Ésta es exactamente la transición que se hizo el 2026-09-23: el esquema
--  pasa a ser responsabilidad de Flyway, no de Hibernate.
--
--  LECCIÓN SOBRE CHECK CONSTRAINTS (el problema clásico de este proyecto):
--  La tabla `permissions` tiene un CHECK que fija la lista EXACTA de nombres
--  permitidos: permissions_name_check. Es el "type safety" de la BD. Pero ni
--  Hibernate con ddl-auto=update ni una entidad JPA pueden ampliarlo: es DDL
--  puro. Por eso hay que DROP + ADD aquí, y por eso V1__init.sql lo lleva
--  hardcodeado. Si se agrega un permiso al enum y NO se actualiza este CHECK,
--  el INSERT del bootstrap revienta con "violates check constraint".
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 1) sales: los 4 campos del ciclo de vida de la venta
-- ----------------------------------------------------------------------------
--  confirmed / cancelled son INDEPENDIENTES (no un enum) a propósito:
--  un enum obligaría a un 5º estado del tipo PaymentStatus, y PaymentStatus
--  describe el PAGO (¿se cobró?, ¿se revirtió?), no el ESTADO DE LA VENTA
--  (¿sigue abierta?, ¿el cliente ya se la llevó?). Son dos ejes distintos:
--  una venta con tarjeta APROBADA puede estar CANCELADA (se devolvió el dinero).
--
--  DEFAULT false  -> las ventas ya existentes nacen "no confirmadas", que es
--                     el estado correcto: una venta vieja ya cobrada y entregada
--                     se debe poder confirmar, y solo entonces queda congelada.
--  NULL en los timestamps -> "todavía no ocurrió" (mismo patrón que un
--                     Optional vacío; no inventamos una fecha de epoch).

ALTER TABLE public.sales
    ADD COLUMN IF NOT EXISTS confirmed boolean NOT NULL DEFAULT false;

ALTER TABLE public.sales
    ADD COLUMN IF NOT EXISTS confirmed_at timestamp(6) without time zone;

ALTER TABLE public.sales
    ADD COLUMN IF NOT EXISTS cancelled boolean NOT NULL DEFAULT false;

ALTER TABLE public.sales
    ADD COLUMN IF NOT EXISTS cancelled_at timestamp(6) without time zone;

COMMENT ON COLUMN public.sales.confirmed IS
    'true = venta congelada: no se puede anular ni borrar';
COMMENT ON COLUMN public.sales.confirmed_at IS
    'Momento en que se confirmó. NULL mientras la venta siga abierta';
COMMENT ON COLUMN public.sales.cancelled IS
    'true = venta anulada (el stock volvió al inventario)';
COMMENT ON COLUMN public.sales.cancelled_at IS
    'Momento de la anulación. NULL si la venta no fue anulada';


-- ----------------------------------------------------------------------------
-- 2) permissions: 2 permisos nuevos -> el enum pasa de 34 a 36
-- ----------------------------------------------------------------------------
--  CONFIRMAR_VENTAS -> "Cerrar" la venta en el POS. Es la acción que la
--                       KITCHEN/mostrador ejecuta cuando el pedido ya salió.
--                       Congela la venta.
--  CANCELAR_VENTAS  -> anular una venta NO confirmada (devuelve el stock).
--
--  DROP + ADD porque un CHECK no se puede "ampliar" con ALTER: hay que
--  quitarlo y recrearlo con la lista nueva. Se reescribe con `IN (...)`, que
--  es mucho más legible que la forma `= ANY(ARRAY[...])` que genera pg_dump.
--  El nombre del constraint se mantiene (permissions_name_check) para que una
--  posible reversión manual use el mismo nombre.

ALTER TABLE public.permissions DROP CONSTRAINT IF EXISTS permissions_name_check;

ALTER TABLE public.permissions
    ADD CONSTRAINT permissions_name_check CHECK (name IN (
        -- Compras
        'VER_COMPRAS',
        'CREAR_COMPRAS',
        'CANCELAR_COMPRAS',
        -- Proveedores
        'VER_PROVEEDORES',
        'CREAR_PROVEEDORES',
        'EDITAR_PROVEEDORES',
        'ELIMINAR_PROVEEDORES',
        -- Productos
        'VER_PRODUCTOS',
        'CREAR_PRODUCTOS',
        'EDITAR_PRODUCTOS',
        'ELIMINAR_PRODUCTOS',
        'DESACTIVAR_PRODUCTOS',
        'ACTIVAR_PRODUCTOS',
        -- Usuarios
        'VER_USUARIOS',
        'CREAR_USUARIOS',
        'EDITAR_USUARIOS',
        'ACTIVAR_USUARIOS',
        'DESACTIVAR_USUARIOS',
        -- Categorias
        'VER_CATEGORIAS',
        'CREAR_CATEGORIAS',
        'EDITAR_CATEGORIAS',
        'ELIMINAR_CATEGORIAS',
        -- Ventas
        'VER_VENTAS',
        'CREAR_VENTAS',
        'CONFIRMAR_VENTAS',
        'CANCELAR_VENTAS',
        -- Caja
        'VER_CAJA',
        'ABRIR_CAJA',
        'CERRAR_CAJA',
        'CORTE_CAJA',
        -- Roles
        'VER_ROLES',
        'CREAR_ROLES',
        'ELIMINAR_ROLES',
        'EDITAR_ROLES',
        -- Reportes
        'VER_REPORTES',
        -- Pagos
        'PROCESAR_PAGOS'
    ));


-- ----------------------------------------------------------------------------
-- 3) Sembrar los 2 permisos nuevos
-- ----------------------------------------------------------------------------
--  `id` es GENERATED BY DEFAULT AS IDENTITY, igual que el resto, así que no se
--  indica: lo asigna la secuencia. ON CONFLICT DO NOTHING + subconsulta sobre
--  `name` hace la migración IDEMPOTENTE: si la fila ya existe (porque
--  PermissionBootstrap ya corrió), no falla. Sin esto, una BD donde el app ya
--  arrancó con el enum nuevo rompería la migración.

INSERT INTO public.permissions (name)
SELECT 'CONFIRMAR_VENTAS'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE name = 'CONFIRMAR_VENTAS');

INSERT INTO public.permissions (name)
SELECT 'CANCELAR_VENTAS'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE name = 'CANCELAR_VENTAS');


-- ----------------------------------------------------------------------------
-- 4) Asignarlos a ADMIN y CAJERO
-- ----------------------------------------------------------------------------
--  POR QUÉ ESTO ESTÁ AQUÍ Y NO EN RolePermissionBootstrap:
--  ese bootstrap es "seed inicial" a propósito (decisión 2026-09-17): si un rol
--  base ya tiene AL MENOS un permiso, no toca nada, para respetar la gestión
--  manual. Consecuencia: un permiso nuevo NO llega solo a los roles en una BD
--  ya sembrada. Ponerlo en la migración lo hace explícito, versionado y que
--  corre UNA sola vez (Flyway no re-ejecuta una migración aplicada).
--  role_permissions es una tabla puente N:M, por eso el INSERT ... SELECT:
--  une roles × permissions y filtra lo que falta.

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
CROSS JOIN public.permissions p
WHERE r.name IN ('ADMIN', 'CAJERO')
  AND p.name IN ('CONFIRMAR_VENTAS', 'CANCELAR_VENTAS')
  AND NOT EXISTS (
      SELECT 1
      FROM public.role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
