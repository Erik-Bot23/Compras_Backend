-- ============================================================================
--  V3__compras_confirmadas_costo_y_cajas.sql
-- ----------------------------------------------------------------------------
--  POR QUE ESTA MIGRACION EXISTE (3 cambios del mismo encargo)
-- ----------------------------------------------------------------------------
--  1) CONFIRMACION DE COMPRAS
--     Hoy POST /purchases suma el stock y guarda el costo INMEDIATAMENTE. No
--     hay forma de registrar una compra "en transito" sin que ya afecte al
--     inventario, ni de deshacerla si el proveedor no entrego la mercaderia.
--
--     El ciclo nuevo es el MISMO que ya tiene la venta (V2), y por la misma
--     razon: el estado terminal es el que protege al inventario.
--
--        compra PENDIENTE   -> NO toco stock, NO toco costo.
--                              Se puede cancelar (se borra).
--        compra CONFIRMADA  -> YA toco stock (stock += qty) y costo
--                              (cost = unitCost). CONGELADA: no se cancela.
--
--     Por que el efecto esta en CONFIRMAR y no al registrar: confirmar es lo
--     que declara que la mercaderia llego de verdad al almacen. Antes de
--     confirmar, el almacen todavia no sabe nada.
--
--  2) COSTO DE LO VENDIDO (para la utilidad real)
--     sale_details guardaba unit_price (lo que se COBRO) pero NO el costo del
--     producto al momento de la venta. Cualquier reporte de utilidad tenia
--     que usar products.cost, que es el costo del ULTIMO purchase HOY, no el
--     del dia que se vendio: comprar algo mas barato manana reescribia la
--     ganancia de ayer.
--
--     Fix: sale_details.unit_cost guarda el costo CONGELADO al vender. Es el
--     mismo patron que ya usa la venta con unit_price: si el costo del
--     producto cambia manana, la venta de ayer conserva lo que ocurrio.
--
--  3) CAJAS NUMERADAS
--     cash_registers no tenia numero. Se le agrega `number`, que es lo que el
--     vendedor escribe al abrir la caja y lo que despues permite filtrar por
--     caja en Reportes y ver las ventas de cada una.
--
--  NOTA DE ALCANCE (decidido el 2026-09-30): se mantiene la regla de UNA sola
--  caja activa a la vez. Este cambio NO habilita varias cajas simultaneas.
-- ============================================================================


-- ----------------------------------------------------------------------------
--  1) purchases: el ciclo de vida de la compra
-- ----------------------------------------------------------------------------
--  confirmed NOT NULL DEFAULT false -> las compras nuevas nacen PENDIENTES.
--
--  BACKFILL (critico, no opcional): las compras que YA EXISTEN en esta base de
--  datos tienen su stock YA SUMADO (lo hizo POST /purchases entre el 2026-09-13
--  y el 2026-09-30, ver PurchaseImpl.create). Si se dejaran en false, el
--  sistema diria "estas compras no han movido stock" cuando si lo movieron, y
--  quedaria imposible distinguirlas de las nuevas. Por eso se marcan como
--  CONFIRMADAS con confirmed_at = purchase_date: la fecha real en que
--  entraron al almacen fue la de la compra, no hoy.
--
--  Mismo criterio que en V2 con las ventas: DEFAULT false para lo nuevo,
--  backfill a true para lo viejo que ya produjo efectos.

ALTER TABLE public.purchases
    ADD COLUMN IF NOT EXISTS confirmed boolean NOT NULL DEFAULT false;

ALTER TABLE public.purchases
    ADD COLUMN IF NOT EXISTS confirmed_at timestamp(6) without time zone;

-- Backfill: TODAS las compras que ya existian se marcan CONFIRMADAS.
--
-- Antes de V3 crear la compra ya sumaba el stock, asi que su mercaderia esta
-- (o estuvo) en el almacen y su costo ya es el vigente. Dejarlas como
-- pendientes seria un error de DATOS: permitiria borrar una compra que ya sumo
-- stock, y el inventario quedaria con existencias de fantasma. Es el mismo
-- criterio que en V2 con las ventas: lo que ya ocurrio no se "desconfirma".
--
-- confirmed_at = purchase_date (no NOW()) para que la fecha de confirmacion sea
-- la REAL y no la de la migracion;asi el historico de caja sigue siendo creible.
-- COALESCE protege el caso purchase_date IS NULL: en vez de dejar esa compra
-- colgada en PENDIENTE (que permitiria borrar un stock ya aplicado) se le pone
-- la fecha de la migracion.
UPDATE public.purchases
SET confirmed = true,
    confirmed_at = COALESCE(purchase_date, CURRENT_TIMESTAMP)
WHERE confirmed = false;

COMMENT ON COLUMN public.purchases.confirmed IS
    'true = compra confirmada: el stock ya sumo y el costo ya se guardo. NO se puede cancelar';
COMMENT ON COLUMN public.purchases.confirmed_at IS
    'Momento en que se confirmo la mercaderia y se aplico el stock. NULL mientras siga pendiente';


-- ----------------------------------------------------------------------------
--  2) sale_details: el costo CONGELADO al momento de la venta
-- ----------------------------------------------------------------------------
--  Nullable a proposito (igual que unit_price, que en V1 tambien lo es): si
--  un renglon viejo no tiene producto, o el producto no tiene costo, queda
--  NULL y el reporte lo trata como costo desconocido en vez de inventar un 0.
--  Un 0 inflaria la utilidad, que es el error mas caro de los dos.
--
--  El backfill usa el costo ACTUAL como mejor aproximacion disponible: no es
--  el costo historico exacto, es el ultimo conocido. Las ventas nuevas si lo
--  guardan congelado en su momento, asi que a partir de esta migracion el
--  dato es fiable.

ALTER TABLE public.sale_details
    ADD COLUMN IF NOT EXISTS unit_cost numeric(38,2);

UPDATE public.sale_details d
SET unit_cost = p.cost
FROM public.products p
WHERE d.product_id = p.id
  AND d.unit_cost IS NULL;

COMMENT ON COLUMN public.sale_details.unit_cost IS
    'Costo del producto congelado al momento de la venta. Base del reporte de utilidad';


-- ----------------------------------------------------------------------------
--  3) cash_registers: el numero que escribe el vendedor al abrir
-- ----------------------------------------------------------------------------
--  Se agrega NULL primero, se rellena, y SOLO DESPUES se vuelve NOT NULL: en
--  una tabla con filas eso no se puede hacer en un solo ALTER, porque el
--  default de una columna nueva no aplica a filas existentes en PostgreSQL.
--
--  El backfill usa 'CAJA-' || id: es unico por construccion (id lo es), legible
--  y deja claro que esas cajas NO fueron numeradas por un humano, sino
--  generadas para poderlas distinguir.
--
--  UNIQUE: dos cajas con el mismo numero harian imposible el "filtrar por
--  caja" de Reportes (juntaria dos cortes distintos en una misma fila), que es
--  justamente para lo que se pide el numero.

ALTER TABLE public.cash_registers
    ADD COLUMN IF NOT EXISTS number varchar(50);

UPDATE public.cash_registers
SET number = 'CAJA-' || id
WHERE number IS NULL
  OR btrim(number) = '';

ALTER TABLE public.cash_registers
    ALTER COLUMN number SET NOT NULL;

ALTER TABLE public.cash_registers
    DROP CONSTRAINT IF EXISTS cash_registers_number_key;

ALTER TABLE public.cash_registers
    ADD CONSTRAINT cash_registers_number_key UNIQUE (number);

COMMENT ON COLUMN public.cash_registers.number IS
    'Numero que escribe el vendedor al abrir la caja. UNIQUE: identifica el corte en Reportes';

-- ----------------------------------------------------------------------------
--  4) permissions: CONFIRMAR_COMPRAS -> el enum pasa de 36 a 37
-- ----------------------------------------------------------------------------
--  CONFIRMAR_COMPRAS -> la operacion que SUMA el stock y guarda el costo. Sin
--                       este permiso, crear una compra no mueve el almacen.
--
--  DROP + ADD porque un CHECK no se "amplia" con ALTER: hay que quitarlo y
--  recrearlo con la lista nueva. Se reescribe con IN (...), que es mucho mas
--  legible que la forma = ANY(ARRAY[...]) que genera pg_dump. El nombre del
--  constraint se mantiene para que una posible reversion manual use el mismo.
--
--  La lista de 37 nombres se DERIVO de la de V2 (36) agregando este, para no
--  poder perder ninguno por un error de dedo.

ALTER TABLE public.permissions DROP CONSTRAINT IF EXISTS permissions_name_check;

ALTER TABLE public.permissions
    ADD CONSTRAINT permissions_name_check CHECK (name IN (
        'VER_COMPRAS',
        'CREAR_COMPRAS',
        'CONFIRMAR_COMPRAS',
        'CANCELAR_COMPRAS',
        'VER_PROVEEDORES',
        'CREAR_PROVEEDORES',
        'EDITAR_PROVEEDORES',
        'ELIMINAR_PROVEEDORES',
        'VER_PRODUCTOS',
        'CREAR_PRODUCTOS',
        'EDITAR_PRODUCTOS',
        'ELIMINAR_PRODUCTOS',
        'DESACTIVAR_PRODUCTOS',
        'ACTIVAR_PRODUCTOS',
        'VER_USUARIOS',
        'CREAR_USUARIOS',
        'EDITAR_USUARIOS',
        'ACTIVAR_USUARIOS',
        'DESACTIVAR_USUARIOS',
        'VER_CATEGORIAS',
        'CREAR_CATEGORIAS',
        'EDITAR_CATEGORIAS',
        'ELIMINAR_CATEGORIAS',
        'VER_VENTAS',
        'CREAR_VENTAS',
        'CONFIRMAR_VENTAS',
        'CANCELAR_VENTAS',
        'VER_CAJA',
        'ABRIR_CAJA',
        'CERRAR_CAJA',
        'CORTE_CAJA',
        'VER_ROLES',
        'CREAR_ROLES',
        'ELIMINAR_ROLES',
        'EDITAR_ROLES',
        'VER_REPORTES',
        'PROCESAR_PAGOS'
    ));

COMMENT ON CONSTRAINT permissions_name_check ON public.permissions IS
    'Lista cerrada de los 37 permisos del sistema. Al agregar uno: enum + esta migracion + bootstrap';


-- ----------------------------------------------------------------------------
--  5) Sembrar CONFIRMAR_COMPRAS y asignarlo a ADMIN y ALMACENISTA
-- ----------------------------------------------------------------------------
--  Idempotente (INSERT ... SELECT ... WHERE NOT EXISTS): si la fila ya existe
--  porque PermissionBootstrap ya corrio, la migracion no falla.
--
--  Va AQUI y no en RolePermissionBootstrap porque ese bootstrap es seed inicial
--  a proposito (decision del 2026-09-17): si un rol base ya tiene al menos un
--  permiso, no toca nada. En una BD ya sembrada un permiso nuevo nunca llegaria
--  solo a los roles. En la migracion es explicito, versionado y corre una vez.
--
--  ALMACENISTA (y no CAJERO) porque confirmar una compra es una operacion de
--  almacen: es quien recibe y cuenta la mercaderia. El cajero no deberia poder
--  sumar stock al inventario.

INSERT INTO public.permissions (name)
SELECT 'CONFIRMAR_COMPRAS'
WHERE NOT EXISTS (SELECT 1 FROM public.permissions WHERE name = 'CONFIRMAR_COMPRAS');

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
CROSS JOIN public.permissions p
WHERE r.name IN ('ADMIN', 'ALMACENISTA')
  AND p.name = 'CONFIRMAR_COMPRAS'
  AND NOT EXISTS (
      SELECT 1
      FROM public.role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
