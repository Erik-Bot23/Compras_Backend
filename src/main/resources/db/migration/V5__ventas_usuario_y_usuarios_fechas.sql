-- ============================================================================
--  V5__ventas_usuario_y_usuarios_fechas.sql
--  2026-10-01 · Dos agregados que hacen falta para poder buscar registros.
--
--  Por qué existe
--  -------------
--  1) sales.user_id — "quién vendió"
--
--     Una venta solo apuntaba al TURNO (cash_register_id), no a la persona. Eso
--     tapaba una pregunta muy común: ¿qué caja vendieron Juan y María? Y otra
--     igual de común: ¿cuánto vendió cada cajero el mes pasado?
--
--     Sin esta columna, ninguna de las dos se puede responder. Con ella, el
--     filtro por usuario de Reportes y la búsqueda en el historial de ventas
--     se vuelven posibles.
--
--     ⚠️ NULL EN LAS VENTAS QUE YA EXISTEN: no se puede saber quién las hizo,
--     porque el dato nunca se guardó. Se dejan en NULL a propósito (no se inventa
--     un usuario), y por eso el filtro por usuario solo aplica a las ventas
--     nuevas. Un NULL no es un error: es "no lo sabemos".
--
--     La FK es ON DELETE SET NULL y NO cascade: si se borra un usuario, sus
--     ventas NO se borran (eso sería perder ventas), solo quedan sin dueño.
--
--  2) users.activated_at / users.deactivated_at — "cuándo entró y salió"
--
--     users.active es un booleano: dice SI está dado de baja, pero no CUÁNDO.
--     Con un solo booleano, "buscar el registro de la persona que se fue en
--     abril" es imposible: no se sabe el momento.
--
--     Estos dos timestamps lo hacen posible. No es un historial completo de
--     cambios (para eso haría falta una tabla aparte de auditoría); es la
--     última alta y la última baja, que es lo que hace falta para buscar un
--     registro.
--
--     Regla de coherencia que mantiene el service:
--       active = true  -> activated_at  siempre informada
--       active = false -> deactivated_at siempre informada
--     Es decir: cada usuario tiene exactamente una de las dos fechas.
-- ============================================================================

-- ----------------------------------------------------------------------------
--  1) ventas: quién hizo la venta
-- ----------------------------------------------------------------------------

ALTER TABLE public.sales
    ADD COLUMN IF NOT EXISTS user_id bigint;

-- Índice: es el filtro "ventas de este usuario", que se usa siempre que se
-- abre Reportes. Sin índice es un seq scan sobre toda la tabla de ventas.
CREATE INDEX IF NOT EXISTS idx_sales_user ON public.sales (user_id);

-- ON DELETE SET NULL a propósito: borrar un usuario no debe borrar sus ventas.
-- Las ventas se quedan y quedan sin dueño, que es el dato honesto.
ALTER TABLE public.sales
    ADD CONSTRAINT fk_sales_user
    FOREIGN KEY (user_id) REFERENCES public.users(id)
    ON DELETE SET NULL;

COMMENT ON COLUMN public.sales.user_id IS
    'Usuario (empleado) que registro la venta. NULL en las ventas anteriores a V5: el dato no existia';

-- ----------------------------------------------------------------------------
--  2) users: fechas de alta y de baja
-- ----------------------------------------------------------------------------

ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS activated_at timestamp(6) without time zone;

ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS deactivated_at timestamp(6) without time zone;

-- Índice parcial: solo las filas dadas de baja se buscan por fecha, y son
-- muchas menos que las activas. Un índice sobre toda la tabla sería mayor de
-- lo necesario para la única consulta que lo usa.
CREATE INDEX IF NOT EXISTS idx_users_deactivated_at
    ON public.users (deactivated_at)
    WHERE active = false;

COMMENT ON COLUMN public.users.activated_at IS
    'Ultima vez que se dio de alta el usuario (creacion o futura re-activacion)';

COMMENT ON COLUMN public.users.deactivated_at IS
    'Ultima vez que se dio de baja. Solo se informa si active = false';

-- ----------------------------------------------------------------------------
--  3) Backfill: los usuarios que YA estan dados de baja
-- ----------------------------------------------------------------------------
--  No hay fecha guardada, pero los cortes de caja si dicen desde cuando existe
--  el sistema. No se inventa una fecha: se deja NULL y la app lo muestra como
--  "sin fecha". Poner una fecha inventada seria peor que no tener dato.
-- ----------------------------------------------------------------------------