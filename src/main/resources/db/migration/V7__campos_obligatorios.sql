-- ============================================================================
--  V7__campos_obligatorios.sql
--  2026-10-04 · Que ningún campo obligatorio se guarde como NULL.
--
--  El encargo
--  ----------
--  "Que no se guarden los campos como null": nombre de categoría, nombre de
--  usuario, correo, contraseña, nombre de rol, SKU, barcode, nombre de
--  proveedor y RFC de proveedor.
--
--  Qué estaba protegido y qué no
--  -----------------------------
--  Antes de este trabajo, la respuesta fue desigual según el módulo:
--
--    · Categoría, rol, proveedor  -> TRES capas: validador imperativo,
--      @Column(nullable=false) y NOT NULL en la base. Ya estaban bien.
--
--    · Usuario (nombre, correo)   -> lo frenaba el NOT NULL de la base, pero
--      no había validación propia. Y el error que devolvía la base era un 409
--      que decía "el dato ya existe", que no era lo que pasaba.
--
--    · Usuario (contraseña)       -> SIN NADA. Y aquí lo grave: BCrypt hashea
--      la cadena vacía sin quejarse, así que una contraseña en blanco SÍ se
--      guardaba. El usuario quedaba creado y con una fila en la base, pero
--      después no podía iniciar sesión jamás (matches("", hash) devuelve false
--      siempre). Un dato basura y un soporte sin explicación.
--
--    · SKU y barcode              -> opcionales POR DECISIÓN (2026-09-28):
--      las columnas permitían NULL y el validador devolvía null. Ahora pasan a
--      obligatorios (ver abajo por qué se revierte esa decisión).
--
--  Por qué SKU y barcode ahora son obligatorios
--  ---------------------------------------------
--  Dos razones, y las dos son de operación, no de gusto:
--
--   1. En el POS el producto se cobra ESCANEANDO su código. Un producto sin
--      barcode es un producto que el cajero no puede cobrar sin buscarlo a mano.
--
--   2. En PostgreSQL un UNIQUE admite VARIOS NULL (porque NULL != NULL). Con el
--      barcode opcional, "producto sin código" se acumulaba en silencio: no
--      decía nada, no rompía nada y no se podía ni contar. El índice único no
--      avisaba porque técnicamente no había duplicado.
--
--  Lo que hace ESTA migración
--  --------------------------
--  Solo la parte que no puede hacerse en Java: el NOT NULL de la base. La
--  validación de los formularios (InputValidator.requerido / .password) vive
--  en el código, porque un mensaje útil ("el SKU es obligatorio") solo se puede
--  escribir a mano; ver el comentario de cabecera de InputValidator.
--
--  Esta migración NO puede vivir en un NOT NULL pelado, porque hay filas
--  sucias y un NOT NULL sobre datos sucios FALLA. Por eso primero rellena, y
--  solo después restringe.
-- ============================================================================

-- ----------------------------------------------------------------------------
--  1. Relleno de lo que ya estaba vacío (solo si falta)
-- ----------------------------------------------------------------------------
--
--  🔑 REGLA DE ORO: esta migración NUNCA pisa un valor que ya existe. Solo
--  escribe donde no hay nada. Si mañana se corre sobre una base con todos los
--  SKUs puestos, estas sentencias no cambian ni un solo dato.
--
--  Los valores inventados son códigos INTERNOS y se distinguen a propósito:
--
--   · SKU     -> PEND-SKU-<id>       ("pendiente de SKU, id 14")
--   · barcode -> 20 + 12 dígitos     (prefijo 20 reservado a códigos internos
--                                      de tienda; los EAN de fábrica empiezan
--                                      por 750, 789, 737...)
--
--  El prefijo 20 es el rango que GS1 reserva para retail interno, así que un
--  código que empiece por 20 NO colisiona con un barcode real de proveedor. Es
--  la razón de elegirlo: si alguien escanea este producto con una terminal,
--  el código no se confunde con nada externo.
--
--  ⚠️ ESTOS VALORES SON PROVISIONALES. El dueño del negocio debería
--  reemplazarlos por los SKU y códigos de barras reales del producto. Hasta
--  entonces el producto queda identificable y cobrable, que era el problema.
--
--  ----------------------------------------------------------------------------
--  LAS TRES FORMAS DE "VACÍO" (y por qué son tres y no una)
--  ----------------------------------------------------------------------------
--  La comprobación no es solo "IS NULL". Hay tres maneras distintas en que un
--  campo obligatorio llega vacío, y cada una se cuela por un lado distinto:
--
--    1. NULL        -> lo deja el INSERT cuando no se manda el campo.
--    2. '' o '   '  -> lo manda un formulario en el que el usuario dejó el
--                      campo en blanco. Para una columna NOT NULL son valores
--                      VÁLIDOS: la base los acepta sin quejarse.
--    3. 'NULL'      -> el texto literal. Se encontró uno así al revisar la base
--                      (producto "Leche 3"): alguien guardó la palabra NULL
--                      como si fuera el dato. Para la base es una cadena
--                      perfectamente normal de 4 caracteres, y el NOT NULL la
--                      acepta igual. Es el peor de los tres porque parece
--                      dato y no lo es.
--
--  Por eso las tres se comparan con upper(btrim(...)): si solo se mirara
--  "IS NULL", los casos 2 y 3 pasarían inadvertidos y el producto quedaría
--  guardado igual, que es exactamente lo que se pidió evitar.
--
--  SKU primero
UPDATE public.products
    SET sku = 'PEND-SKU-' || id
    WHERE sku IS NULL
       OR btrim(sku) = ''
       OR upper(btrim(sku)) = 'NULL';

--  Barcode después, en su propia sentencia
--
--  El id se rellena con ceros a la IZQUIERDA a propósito: el barcode es un
--  identificador de texto, y '20' + lpad(id) da siempre 14 dígitos. Si se
--  hiciera '20' || id, el producto 5 tendría un código de 3 dígitos y el 500 uno
--  de 5, y un lector que espera ancho fijo los leería distinto.
--
--  El rango posible es 20 000 000 000 01 .. 20 000 000 999 99: cabe en los 20
--  dígitos de la columna y del validador, y no se solapa con el UNIQUE.
UPDATE public.products
    SET barcode = '20' || lpad(id::text, 12, '0')
    WHERE barcode IS NULL
       OR btrim(barcode) = ''
       OR upper(btrim(barcode)) = 'NULL';

-- ----------------------------------------------------------------------------
--  2. Ahora sí: NOT NULL
-- ----------------------------------------------------------------------------
--
--  ProductEntity: se cambia @Column(unique = true) por
--  @Column(unique = true, nullable = false). Con ddl-auto: validate, si el
--  cambio fuera por un lado y la base no lo reflejara, la aplicación NO
--  arrancaría al validar el esquema: esa es la red que hace honesto este paso.
ALTER TABLE public.products
    ALTER COLUMN sku SET NOT NULL,
    ALTER COLUMN barcode SET NOT NULL;

-- ----------------------------------------------------------------------------
-- ----------------------------------------------------------------------------
--  3. Comprobación final (si algo quedó sin rellenar, esto avisa)
-- ----------------------------------------------------------------------------
--
--  🔑 La condición no puede ser "NOT NULL" a secas: el NOT NULL ya está puesto
--  en el paso 2 y por lo tanto siempre es cierto. Lo que se verifica aquí es lo
--  OPPOSTO: que no queden cadenas VACÍAS ni el texto 'NULL'.
--
--  Esto es lo que un NOT NULL NO atrapa. La cadena vacía, la de tres espacios y
--  la palabra 'NULL' son valores perfectamente válidos para una columna NOT
--  NULL: la base los acepta sin quejarse y el producto queda guardado "sin
--  código". Por eso el paso 1 los cubre, y esta comprobación se encarga de que
--  si algo se colara, salga un mensaje que dice QUÉ hacer en vez de un error
--  técnico de PostgreSQL.
--
--  No se añade un CHECK (que también losatraparía en la base) a propósito: un CHECK
--  sobre texto tiene que crecer con cada regla de formato (solo dígitos,
--  longitud, etc.) y esa lista ya vive mejor en InputValidator, que además da
--  mensajes en español que el usuario puede entender.
DO $$
DECLARE
    sucios INTEGER;
BEGIN
    SELECT count(*) INTO sucios
    FROM public.products
    WHERE btrim(sku) = ''
       OR btrim(barcode) = ''
       OR upper(btrim(sku)) = 'NULL'
       OR upper(btrim(barcode)) = 'NULL';

    IF sucios > 0 THEN
        RAISE EXCEPTION
            'Quedaron % producto(s) con SKU o código de barras vacío, o con el '
            'texto ''NULL'' en vez del dato. Un NOT NULL no los atrapa: para la '
            'base la cadena vacía y la palabra NULL son valores válidos. '
            'Rellénalos y vuelve a correr.', sucios;
    END IF;
END $$;
COMMENT ON COLUMN public.products.sku IS
    'Código interno del producto. OBLIGATORIO (V7): antes era opcional y se '
    'guardaban productos sin él. Antes de V7 se rellenan con PEND-SKU-<id>';

COMMENT ON COLUMN public.products.barcode IS
    'Código de barras del producto, como TEXTO (los ceros a la izquierda son '
    'parte del código). OBLIGATORIO (V7). Los rellenados por la migración usan '
    'el prefijo 20, reservado a códigos internos de tienda';
