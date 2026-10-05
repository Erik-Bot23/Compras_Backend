-- ============================================================================
--  V6__ventas_idempotencia.sql
--  2026-10-01 · Que un doble "Enter" NO cobre dos veces.
--
--  El problema
--  ----------
--  En el POS, el modal de confirmar venta es un <form>. El usuario paga en
--  efectivo y aprieta Enter. Si el cajero aprieta Enter dos veces seguidas (o
--  una vez y el botón queda habilitado y vuelve a apretar), el frontend lanza
--  DOS peticiones a POST /sales y se hacen DOS ventas: se descuenta el stock dos
--  veces y el reporte muestra dos tickets por una sola compra.
--
--  Por qué un flag en el frontend NO lo arregla
--  --------------------------------------------
--  Un boton deshabilitado evita el SEGUNDO clic, pero no el caso real: las dos
--  peticiones ya pueden estar viajando por la red al mismo tiempo. El flag
--  protege la UI, no la operacion. Si alguien recarga, cambia de pestaña o
--  reintenta porque la respuesta tardo, la venta se vuelve a crear.
--
--  La solucion: una LLAVE DE IDEMPOTENCIA
--  --------------------------------------
--  El frontend genera una clave única por cada intento de cobro y la manda en
--  cada reintento. El backend guarda esa clave en la venta:
--
--    · Si la clave NO existe  -> es una venta nueva, se crea normal.
--    · Si la clave YA existe -> NO se crea otra: se devuelve la venta que ya
--                               se había hecho, con su mismo id y su mismo
--                               total.
--
--  Es el patrón estándar de idempotencia (el mismo que usan las pasarelas de
--  pago y Stripe). El nombre "idempotencia" viene de una propiedad de las
--  operaciones: aplicar la MISMA operacion N veces tiene el mismo efecto que
--  aplicarla una.
--
--  El UNIQUE es la parte importante y no un adorno: sin el, dos peticiones que
--  llegan en el MISMO instante se leerían mutuamente como "no existe" y las dos
--  insertarían. Con el, PostgreSQL deja pasar a una sola y rechaza a la otra.
--
--  ⚠️ NO se pondría un DEFAULT: la clave la genera el CLIENTE, no el servidor.
--  Si la generara el backend en cada petición, cada una sería distinta y la
--  idempotencia no serviría de nada. Por eso es nullable: las ventas creadas
--  antes de V6 (y las que se creen por otros caminos) simplemente no la tienen.
-- ============================================================================

ALTER TABLE public.sales
    ADD COLUMN IF NOT EXISTS idempotency_key varchar(64);

-- UNIQUE: es lo que garantiza que dos peticiones simultáneas no creen dos
-- ventas. Nótese que NO es un índice normal: un UNIQUE sí rechaza duplicados.
-- Un índice común solo aceleraría la búsqueda y no impediría el doble cobro.
CREATE UNIQUE INDEX IF NOT EXISTS idx_sales_idempotency
    ON public.sales (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

COMMENT ON COLUMN public.sales.idempotency_key IS
    'Clave que envia el cliente por intento de cobro. Si se repite, el backend '
    'devuelve la venta ya creada en vez de crear otra. NULL en las ventas '
    'anteriores a V6 y en las creadas por otros caminos';

-- ----------------------------------------------------------------------------
--  Nota sobre el índice PARCIAL (WHERE idempotency_key IS NOT NULL)
-- ----------------------------------------------------------------------------
--  En PostgreSQL un UNIQUE normal ya permite múltiples NULL (NULL != NULL), así
--  que el filtro no es necesario para la corrección: es una optimización.
--
--  Se deja igualmente porque la tabla de ventas es la más grande del sistema y
--  casi todas sus filas tienen la columna en NULL (ventas viejas, ventas de
--  otros flujos). Un índice parcial sobre las pocas filas que sí la tienen es
--  mucho más pequeño y rápido de mantener, y es lo que se va a consultar.