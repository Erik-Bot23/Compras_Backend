#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Genera docs/07-Paginacion-Carrito-e-Idempotencia-Ventas.pdf

Documenta la V6: idempotencia del cobro (que un doble "Enter" en el modal de
pago NO cree dos ventas), y cierra el punto de la paginacion del carrito de
compras que ya se habia descrito en el PDF 06.

Los estilos, la paleta y los ayudantes (p/h1/h2/codigo/li/tabla/nota/info) se
REIMPORTAN del generador del PDF 06 en vez de copiarse: asi la coleccion de docs
no puede ir divergiendo de una version a otra. Solo se redefinen el pie de
pagina y el contenido, que es lo propio de este documento.

Ejemplo:  python docs/generar_pdf_v6_idempotencia.py
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from reportlab.lib import colors  # noqa: E402
from reportlab.lib.pagesizes import LETTER  # noqa: E402
from reportlab.lib.units import inch  # noqa: E402
from reportlab.platypus import (  # noqa: E402
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
)

# Solo se reimporta lo que este documento usa: la paleta y los estilos vienen
# del PDF 06, y aquí se reusan tal cual para que la serie no se diversifique.
from generar_pdf_v5_mejoras import (  # noqa: E402
    GRIS,
    codigo,
    h1,
    h2,
    info,
    li,
    nota,
    p,
    tabla,
)

BASE = os.path.dirname(os.path.abspath(__file__))
DESTINO = os.path.join(BASE, "07-Paginacion-Carrito-e-Idempotencia-Ventas.pdf")

FECHA = "2026-10-04"


def pie(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 7.5)
    canvas.setFillColor(GRIS)
    canvas.drawString(
        0.75 * inch, 0.55 * inch,
        "Compras-Backend · Paginación del carrito e idempotencia de ventas (V6)",
    )
    canvas.drawRightString(
        LETTER[0] - 0.75 * inch, 0.55 * inch, f"Página {doc.page}")
    canvas.setStrokeColor(colors.HexColor("#e2e8f0"))
    canvas.line(0.75 * inch, 0.72 * inch,
                LETTER[0] - 0.75 * inch, 0.72 * inch)
    canvas.restoreState()


def mono(txt):
    """Codigo en una sola linea, para schemas y firmas."""
    return codigo(txt)


def construir():
    f = []

    # ==================================================================
    # PORTADA
    # ==================================================================
    f.append(p("Documento 07 · Compras-Backend + Compras-Frontend-Angular",
               "subtitulo"))
    f.append(p("Paginación del carrito de compras e idempotencia de ventas",
               "titulo"))
    f.append(p(
        f"{FECHA} · V6 · Continúa del documento 06 "
        f"(Venta–Usuario e Historial de Caja). Este documento explica qué "
        f"cambió desde aquel PDF hasta aquí: por qué un doble &quot;Enter&quot; "
        f"en el modal de cobro creaba dos ventas, cómo se resolvió en las tres "
        f"capas (base de datos, backend y frontend) y cómo se verificó contra "
        f"la base de datos real.", "subtitulo"))

    f.append(tabla(
        ["Capa", "Qué se toca", "Archivo"],
        [
            ["Base de datos",
             "Columna <b>idempotency_key</b> + índice <b>UNIQUE</b> parcial",
             "<font face='Courier'>V6__ventas_idempotencia.sql</font>"],
            ["Backend",
             "Comprobación de la clave <b>antes</b> de cobrar y recuperación "
             "de la carrera",
             "<font face='Courier'>SaleImpl</font>, <font face='Courier'>"
             "SaleController</font>, <font face='Courier'>SaleRepository</font>"],
            ["Frontend",
             "Bandera de vuelo + clave estable por intento de cobro",
             "<font face='Courier'>sale-facade.ts</font>, "
             "<font face='Courier'>cobro.html</font>"],
        ],
        [1.05 * inch, 2.75 * inch, 2.45 * inch]))

    f.append(Spacer(1, 10))

    f.append(info("Estado al cierre de este documento", (
        f"Migración V6 <b>aplicada</b> en la base de datos real (Supabase) y "
        f"registrada por Flyway. Backend: <b>273 pruebas</b> en verde "
        f"(262 previas + 11 nuevas de idempotencia). Frontend: build de "
        f"desarrollo en verde. La prueba de extremo a extremo contra la base "
        f"real está documentada en la sección 9.")))

    f.append(nota("Sobre la paginación del carrito", (
        "Hay <b>dos</b> carritos y los dos acabaron paginados, pero en "
        "momentos distintos. El de <b>compras</b> (modal de nueva compra) se "
        "paginó en la V5 y está en el <b>documento 06</b>, sección 2. El de "
        "<b>venta</b> (el carrito del POS, en <font face='Courier'>cobro.html"
        "</font>) se pagina aquí, en la sección 2. Los dos se ven iguales al "
        "final, pero el del POS pasó cuatro meses con la decisión escrita de "
        "&quot;no paginarlo&quot;, y esa decisión estaba equivocada: se explica "
        "abajo por qué.")))

    f.append(PageBreak())

    # ==================================================================
    # 1. EL PROBLEMA
    # ==================================================================
    f.append(h1("1. El problema: un doble Enter cobraba dos veces"))

    f.append(p(
        "El modal de confirmar venta del POS es un <font face='Courier'>"
        "&lt;form&gt;</font>. El cajero recibe el efectivo, escribe el monto "
        "recibido y aprieta Enter. Eso envía el formulario y se registra la "
        "venta. El problema aparece cuando el Enter se aprieta dos veces."))

    f.append(p(
        "La consecuencia no era un mensaje duplicado: era una venta duplicada. "
        "Concretamente, y esto es lo que hace grave el bug:"))

    f.append(li("<b>El stock se descuenta dos veces.</b> El detalle de venta "
                "se guarda dos veces, así que el inventario baja el doble de lo "
                "que salió del local."))
    f.append(li("<b>Se emiten dos tickets.</b> El cliente (o el cajero) recibe "
                "un comprobante por cada pulsedón."))
    f.append(li("<b>Los reportes mienten.</b> El corte de caja suma dos ventas "
                "por una sola compra, y el dinero de la caja aparece "
                "descuadrado contra el total real."))

    f.append(p(
        "Es decir: el daño no era una molestia visual, sino una inconsistencia "
        "en los números que luego alimentan el inventario y los reportes. Por "
        "eso la solución tiene que vivir en el servidor y no en el botón."))

    f.append(h2("1.1. Qué NO era el problema"))
    f.append(p(
        "No era un problema de validaciones del formulario, ni de que el "
        "botón&quot;no se deshabilitara&quot;, ni de la doble ejecución del "
        "evento <font face='Courier'>ngSubmit</font>. El formulario hacía "
        "exactamente lo que se le pedía: enviar. El defecto estaba en que "
        "<b>enviar dos veces dos veces creaba dos ventas</b>, y eso es una "
        "decisión del servidor, no de la pantalla."))

    f.append(h2("1.2. La evidencia medida"))
    f.append(p(
        "Antes de tocar código se reprodujo el problema contra la base de datos "
        "real con dos peticiones <b>simultáneas</b> y la misma clave. El "
        "resultado sin la protección, y la diferencia con la protección "
        "aplicada, están en la sección 9."))

    # ==================================================================
    # 2. PAGINACION DEL CARRITO DEL POS
    # ==================================================================
    f.append(h1("2. El carrito de venta (POS): por qué sí se pagina ahora"))

    f.append(p(
        "El encargo original era &quot;paginar el carrito&quot;, y había dos "
        "carritos distintos con el mismo nombre. El de <b>compras</b> ya estaba "
        "hecho desde la V5 (documento 06). Este es el del <b>POS</b>: la tabla "
        "de productos que el cajero arma antes de cobrar, en "
        "<font face='Courier'>cobro.html</font>."))

    f.append(h2("2.1. El problema: la tabla no tenía tope"))
    f.append(p(
        "El carrito del POS se dibuja con <font face='Courier'>*ngFor</font> "
        "sobre <b>todos</b> los productos del carrito, sin ningún recorte. Con "
        "treinta productos la tabla crecía treinta renglones hacia abajo y "
        "empujaba el botón verde <b>Cobrar</b> fuera de la pantalla: el cajero "
        "tenía que scrollear para llegar al botón con el que cobra. En un POS "
        "eso no es una molestia estética, es tiempo por venta."))

    f.append(h2("2.2. La decisión anterior, y por qué estaba equivocada"))
    f.append(nota(
        "Una decisión escrita que este encargo revierte",
        "El 2026-09-12 quedó escrito en el AGENTS.md del frontend: <i>&quot;El "
        "carrito del POS no se pagina: es un carrito vivo, no una tabla de "
        "registros&quot;</i>. La razón tenía sentido, pero confundía dos cosas "
        "distintas. El carrito <b>sigue siendo vivo</b> y se sigue pareciendo "
        "como tal: el renglón conserva sus botones de +/−/✕ y la cantidad se "
        "sigue tocando en el sitio. Lo que estaba mal era la <b>altura</b>: "
        "&quot;vivo&quot; no significa &quot;sin tope&quot;. Paginarlo no lo "
        "convierte en una tabla de registros; le pone un límite a la altura."))

    f.append(h2("2.3. Lo que se implementó"))
    f.append(mono(
        "sale-facade.ts\n"
        "  cartPage     = 0;\n"
        "  cartPageSize = 8;    // 8 y no 5: aquí el renglón es UNA línea\n"
        "\n"
        "  get cartTotalItems()  { return this.cobro.cart$.value.length; }\n"
        "  get cartTotalPages()  { return Math.max(1, ceil(total / 8)); }\n"
        "  get cartDesde()       { return cartPaginaValida * 8 + 1; }\n"
        "  get cartHasta()       { return min(desde + 7, total); }\n"
        "\n"
        "cobro.html\n"
        "  *ngFor=\"let item of (sale.cobroItems$ | async)\n"
        "            | paginate: sale.cartPage : sale.cartPageSize\"\n"
        "  *ngIf=\"((sale.cobroItems$ | async) ?? []).length > sale.cartPageSize\"" ))

    f.append(nota(
        "8 renglones y no 5, a propósito",
        "En el carrito de compras el renglón tiene seis columnas y cuatro "
        "<font face='Courier'>input</font>, así que es alto y caben cinco. En el "
        "carrito del POS el renglón es <b>una sola línea</b>, así que caben ocho. "
        "Copiar el 5 del otro carrito por costumbre habría dejado el carrito del "
        "POS más bajo de lo necesario."))

    f.append(h2("2.4. Reutilización: el MISMO PaginatePipe"))
    f.append(p(
        "La lógica de paginación <b>no se duplicó</b>. El carrito usa el "
        "<font face='Courier'>PaginatePipe</font> de "
        "<font face='Courier'>core/pipes/paginate</font>, el mismo que los "
        "CRUDs, y el componente solo lo importa. Además ese pipe ya resolvía "
        "por su cuenta el caso de una página fuera de rango (si se borra el "
        "último renglón de la página 2, se recorta a la última válida)."))

    f.append(nota(
        "Un ajuste de tipo que hizo falta (y por qué no era cosmético)",
        "Al escribir <font face='Courier'>(sale.cobroItems$ | async) | "
        "paginate: ...</font> el build falló con <font face='Courier'>TS2345"
        "</font>: el pipe <font face='Courier'>async</font> devuelve "
        "<font face='Courier'>null</font> antes del primer valor y la firma del "
        "pipe era <font face='Courier'>T[]</font>. La firma se cambió a "
        "<font face='Courier'>T[] | null | undefined</font>. No es una "
        "concesión al compilador: el cuerpo del pipe <b>ya</b> comprobaba "
        "ese caso en su primera línea (<font face='Courier'>if (!items?.length)"
        "</font>). El tipo solo describía menos de lo que el código hacía, y esa "
        "diferencia obligaba a escribir <font face='Courier'>| async ?? []</font> "
        "en cada template."))

    f.append(h2("2.5. Los tres detalles que hacen que se comporte bien"))
    f.append(p(
        "Pagar el doble de una línea de código son los tres casos en los que la "
        "paginación &quot;parece no hacer nada&quot;. Los tres están "
        "resueltos en la facade:"))

    f.append(tabla(
        ["Detalle", "Qué pasa si falta"],
        [
            ["<b>irAPaginaDelProducto(id)</b> al agregar, incluido el "
             "<b>escáner</b>",
             "Escanear el producto 12 con la vista en la página 1 lo agrega "
             "<b>invisible</b>: el carrito &quot;no cambia&quot; a ojos del "
             "cajero, que es justo cuando más engaña"],
            ["<b>ajustarPaginaAlQuitar()</b> al quitar o bajar",
             "Borrar el último renglón de la página 2 deja la vista en una "
             "página que ya no existe: carrito vacío"],
            ["<b>cartPage = 0</b> tras <font face='Courier'>cobro.clear()"
            "</font>",
             "Tras cobrar un carrito que estaba en la página 3, la venta "
             "siguiente aparece &quot;en la página 3&quot; de un carrito de 1 "
             "producto"],
        ],
        [2.3 * inch, 3.95 * inch]))

    f.append(nota(
        "Por qué se calcula por índice real y no \"saltar a la última página\"",
        "Agregar un producto <b>que ya estaba</b> en el carrito no crea renglón "
        "nuevo: solo le sube la cantidad. Si ese renglón está en la página 1 y "
        "el carrito va en la página 3, saltar a la última página dejaría al "
        "cajero mirando la página equivocada. "
        "<font face='Courier'>irAPaginaDelProducto()</font> calcula "
        "<font face='Courier'>Math.floor(indice / cartPageSize)</font> sobre el "
        "<b>índice real</b> del producto, que acierta en los dos casos."))

    f.append(h2("2.6. El pie no puede mentir"))
    f.append(p(
        "El <font face='Courier'>paginate</font> se recorta solo cuando la "
        "página queda fuera de rango, pero los números del pie (&quot;Mostrando "
        "9–16 de 24&quot;) se calculan aparte. Si una ruta futura quitara "
        "productos sin llamar al ajuste, el pie podría llegar a mostrar "
        "&quot;Mostrando 9–3 de 3&quot;: un rango al revés."))
    f.append(p(
        "Por eso los getters del rango usan <font face='Courier'>"
        "cartPaginaValida</font> (la página recortada al rango válido) y no "
        "<font face='Courier'>cartPage</font> directo. Si algún día hay un "
        "desajuste, el texto sigue siendo correcto. El botón &quot;Siguiente&quot; "
        "sigue moviendo <font face='Courier'>cartPage</font>, que es el estado "
        "real, y por eso el contador de página sí lo muestra tal cual."))

    f.append(nota(
        "El pie solo aparece con más de 8 productos",
        "El <font face='Courier'>*ngIf</font> compara el total contra "
        "<font face='Courier'>cartPageSize</font>. En una venta de dos o tres "
        "productos un control de paginación es ruido, y lo que importa ahí es "
        "el botón verde Cobrar. También explica por qué en una venta corta no "
        "se ven las flechas: no es que falten. Y se añadió una fila de "
        "&quot;carrito vacío&quot;, que antes no existía: con la tabla "
        "recortada, un carrito sin productos es un hueco más notorio."))

    f.append(h2("2.7. Lo que NO se cambió"))
    f.append(li("El pie <b>no</b> usa el componente "
                "<font face='Courier'>&lt;app-pagination&gt;</font> "
                "compartido, aunque se podría. Ahí los controles son la "
                "navegación principal de una tabla; en el POS son "
                "secundarios frente al botón verde Cobrar, y el estilo del "
                "componente compartido (barra de tabla con borde superior) "
                "compite visualmente con él. Se replica su estructura con las "
                "clases <font face='Courier'>.cart-paginacion</font>, que "
                "siguen la misma paleta que el pie del carrito de compras."))
    f.append(li("Las flechas usan <font face='Courier'>izquierda.png</font> y "
                "<font face='Courier'>derecha.png</font> con "
                "<font face='Courier'>filter: brightness(0) invert(1)</font>. "
                "Es seguro aquí porque esos dos iconos son de un solo color: el "
                "mismo filtro en <font face='Courier'>anadir.png</font>, que "
                "tiene cruz verde y contorno negro, los fundiría en un bloque "
                "blanco macizo."))

    # ==================================================================
    # 3. IDEMPOTENCIA: CONCEPTO
    # ==================================================================
    f.append(h1("3. Qué es la idempotencia y por qué es la respuesta correcta"))

    f.append(p(
        "<b>Idempotencia</b> es la propiedad de una operación según la cual "
        "aplicarla N veces tiene el mismo efecto que aplicarla una. Es "
        "exactamente lo que necesita un cobro: si la misma intención de cobro "
        "llega dos veces, el efecto debe ser <b>una sola venta</b>."))

    f.append(p(
        "No es un invento nuestro: es el patrón estándar de las pasarelas de "
        "pago (Stripe lo llama <font face='Courier'>idempotency key</font>), y "
        "por eso el nombre de la columna y del campo es ese."))

    f.append(h2("3.1. La clave la genera el CLIENTE, no el servidor"))
    f.append(nota(
        "Esta es la decisión de diseño de la que depende todo lo demás",
        "Si el backend generara la clave en cada petición, cada una sería "
        "distinta y la protección <b>no serviría de nada</b>: el servidor "
        "reconocería cada petición como una venta nueva. La clave tiene que "
        "identificar la <b>intención de cobrar</b>, y esa intención vive en el "
        "navegador del cajero. Por eso la columna es <b>nullable</b> y no "
        "tiene <font face='Courier'>DEFAULT</font>: las ventas anteriores a V6 "
        "y las que se creen por otros caminos simplemente no la llevan."))

    f.append(h2("3.2. Cuándo se genera y cuándo se reutiliza"))
    f.append(tabla(
        ["Momento", "Qué pasa con la clave"],
        [
            ["Se abre el modal de cobro",
             "Se genera <b>una clave nueva</b>. Cada apertura es una venta "
             "nueva y merece su propia clave."],
            ["Se aprieta Enter",
             "Se <b>reutiliza</b> la misma clave. Da igual cuántas veces se "
             "apriete."],
            ["Se paga con tarjeta",
             "Se <b>reutiliza</b> la misma clave: es el mismo cobro, solo "
             "cambia el método."],
            ["Venta confirmada",
             "La clave muere con ese cobro. La siguiente venta genera otra."],
        ],
        [1.85 * inch, 4.4 * inch]))

    f.append(nota(
        "El detalle que arregla el bug",
        "La clave se genera al <b>abrir</b> el modal, no al <b>confirmar</b> el "
        "pago. Si se generara en el clic de confirmar, cada Enter tendría su "
        "propia clave y cada una sería una venta nueva: exactamente el "
        "problema que se quería evitar. El código lo deja escrito en "
        "<font face='Courier'>openPaymentModal()</font> para que nadie lo "
        "“simplifique” moviéndolo."))

    f.append(PageBreak())

    # ==================================================================
    # 4. CAPA 1: FRONTEND
    # ==================================================================
    f.append(h1("4. Capa 1 — Frontend: dos barreras en la interfaz"))

    f.append(p(
        "En el frontend hay dos barreras, y conviene nombrarlas por separado "
        "porque <b>no hacen lo mismo</b>."))

    f.append(h2("4.1. Barrera 1: la bandera de vuelo"))
    f.append(mono(
        "// sale-facade.ts\n"
        "isProcessing = false;\n"
        "\n"
        "confirmPayment(){\n"
        "  if(this.isProcessing){ return; }        // <-- barrera 1\n"
        "  ...\n"
        "  this.isProcessing = true;\n"
        "  this.saleService.processSale(request).subscribe({\n"
        "    next: (r) => { this.isProcessing = false; ... },\n"
        "    error: (e) => { this.isProcessing = false; ... }   // se libera\n"
        "                                                   // también en error\n"
        "  });\n"
        "}"))

    f.append(p(
        "La bandera se libera <b>tanto en el éxito como en el error</b>. Eso es "
        "un detalle pequeño con consecuencia real: si solo se liberara en el "
        "éxito, un cobro rechazado dejaría el modal bloqueado y el cajero no "
        "podría ni reintentar ni cerrar."))

    f.append(mono(
        "// cobro.html — el botónAVISA\n"
        "<button type=\"submit\" [disabled]=\"sale.isProcessing\">\n"
        "  {{ sale.isProcessing ? 'Cobrando…' : 'Confirmar pago' }}\n"
        "</button>"))

    f.append(h2("4.2. Barrera 2: la clave que viaja al backend"))
    f.append(mono(
        "// sale-facade.ts\n"
        "private claveCobro = '';\n"
        "\n"
        "openPaymentModal(){            // se genera AQUÍ, no al confirmar\n"
        "  ...\n"
        "  this.iniciarIntentoDeCobro();\n"
        "  this.showPaymentModal = true;\n"
        "}\n"
        "\n"
        "confirmPayment(){\n"
        "  const request: SaleRequest = {\n"
        "    paymentMethod: this.selectedPaymentMethod,\n"
        "    cashReceived: this.cashReceived,\n"
        "    idempotencyKey: this.claveCobro,     // <-- barrera 2\n"
        "    items: ...\n"
        "  };\n"
        "}"))

    f.append(p(
        "La generación del identificador usa "
        "<font face='Courier'>crypto.randomUUID()</font> cuando existe, con un "
        "respaldo basado en <font face='Courier'>Math.random()</font> para "
        "entornos sin esa API. El respaldo <b>no es criptográficamente "
        "seguro</b> y el código lo dice: aquí la clave sirve para evitar un "
        "cobro doble, no para proteger un secreto. Si algún día se usara para "
        "algo sensible, esa nota es la advertencia de que hay que cambiarlo."))

    f.append(h2("4.3. El flujo de tarjeta"))
    f.append(p(
        "El modal de tarjeta tiene su propia bandera, "
        "<font face='Courier'>cardProcessing</font>, y la misma clave. Se "
        "comparte a propósito: elegir tarjeta y luego efectivo sigue siendo "
        "<b>el mismo cobro</b>, así que si el cajero cerrara el modal de "
        "tarjeta y pagara en efectivo, el backend sigue reconociéndolo como el "
        "mismo intento y no cobra dos veces."))

    f.append(nota(
        "Por qué la barrera 1 no alcanza (y por qué está la 2)",
        "El botón deshabilitado evita el segundo <b>clic</b>, pero no el caso "
        "real: las dos peticiones ya pueden estar viajando por la red al "
        "mismo tiempo. Además, si el usuario recarga la pestaña, cambia de "
        "ventana o reintenta porque la respuesta tardó, la venta se volvería a "
        "crear. La barrera 1 protege la <b>interfaz</b>; la barrera 2 protege "
        "la <b>operación</b>, y por eso la que resuelve el problema es la "
        "segunda."))

    # ==================================================================
    # 5. CAPA 3: BASE DE DATOS
    # ==================================================================
    f.append(h1("5. La capa que de verdad garantiza: la base de datos"))

    f.append(p(
        "Ni el frontend ni una comprobación previa en JavaScript garantizan "
        "que no haya doble venta. La garantía real es una restricción de la "
        "base de datos."))

    f.append(mono(
        "-- V6__ventas_idempotencia.sql\n"
        "ALTER TABLE public.sales\n"
        "    ADD COLUMN IF NOT EXISTS idempotency_key varchar(64);\n"
        "\n"
        "CREATE UNIQUE INDEX IF NOT EXISTS idx_sales_idempotency\n"
        "    ON public.sales (idempotency_key)\n"
        "    WHERE idempotency_key IS NOT NULL;"))

    f.append(h2("5.1. Por qué UNIQUE y no un índice normal"))
    f.append(p(
        "Un índice común solo <b>acelera</b> la búsqueda: dos ventas con la "
        "misma clave seguirían existiendo. Un <font face='Courier'>UNIQUE</font> "
        "<b>rechaza</b> el duplicado. Son cosas distintas y confundirlas "
        "deja el bug abierto, porque la aplicación se vería correcta (la "
        "búsqueda funciona) mientras el stock se sigue descontando dos veces."))

    f.append(h2("5.2. Por qué el índice es PARCIAL"))
    f.append(p(
        "En PostgreSQL un <font face='Courier'>UNIQUE</font> normal ya permite "
        "varios <font face='Courier'>NULL</font>, porque "
        "<font face='Courier'>NULL != NULL</font>. El filtro "
        "<font face='Courier'>WHERE idempotency_key IS NOT NULL</font> no es "
        "necesario para la corrección: es una optimización."))

    f.append(p(
        "Se deja igual porque la tabla de ventas es la más grande del sistema y "
        "casi todas sus filas tienen la columna en <font face='Courier'>NULL"
        "</font> (ventas viejas, ventas de otros flujos). Un índice parcial "
        "sobre las pocas filas que sí la tienen es mucho más pequeño y rápido "
        "de mantener, y es exactamente lo que se va a consultar."))

    f.append(nota(
        "Por qué no se acortó el nombre de la columna",
        "El nombre largo <font face='Courier'>idempotency_key</font> es el "
        "que usa la documentación de Stripe y el que aparece en los logs. "
        "Una abreviatura como <font face='Courier'>idem_key</font> llenaría "
        "menos, pero obligaría a traducirlo cada vez que se lee una "
        "integración externa. En tablas de 14 columnas el ahorro de dos "
        "caracteres no paga ese costo."))

    f.append(PageBreak())

    # ==================================================================
    # 6. CAPA 2: BACKEND
    # ==================================================================
    f.append(h1("6. Capa 2 — Backend: comprobar antes de cobrar"))

    f.append(p(
        "El servicio recibe la clave por el DTO, la busca, y solo cobra si no "
        "existe."))

    f.append(h2("6.1. Las clases y cómo se relacionan"))
    f.append(tabla(
        ["Clase", "Qué aporta", "Relación"],
        [
            ["<font face='Courier'>SaleRequest</font><br/>(DTO de entrada)",
             "El campo <font face='Courier'>idempotencyKey</font> que recibe "
             "la clave",
             "Lo llena el frontend"],
            ["<font face='Courier'>SaleEntity</font>",
             "El campo <font face='Courier'>idempotencyKey</font> que la "
             "persiste",
             "Se copia desde el DTO"],
            ["<font face='Courier'>SaleRepository</font>",
             "<font face='Courier'>findByIdempotencyKey()</font>",
             "Interroga a la tabla"],
            ["<font face='Courier'>SaleImpl</font>",
             "Comprueba la clave, normaliza y decide",
             "Orquesta el caso normal"],
            ["<font face='Courier'>SaleService</font>",
             "El método de relectura para la carrera",
             "Contrato, para el controlador"],
            ["<font face='Courier'>SaleController</font>",
             "Recupera la carrera perdida",
             "Última línea de defensa"],
        ],
        [1.7 * inch, 2.6 * inch, 1.95 * inch]))

    f.append(h2("6.2. El orden de las operaciones (esto es lo importante)"))
    f.append(p(
        "La comprobación va <b>primero, antes de validar el carrito y antes de "
        "crear la venta</b>. Si se hiciera más tarde, la venta loser ya habría "
        "descontado stock antes de detectar que era repetida: la protección "
        "serviría de adorno."))

    f.append(mono(
        "// SaleImpl.processSale(request)\n"
        "if(ventaPrevia != null){\n"
        "    log.info(\"Venta {} ya procesada con la clave de idempotencia. \"\n"
        "             + \"Se devuelve sin cobrar de nuevo.\", ventaPrevia.getId());\n"
        "    return mapper.toResponse(ventaPrevia);   // <-- atajo temprano\n"
        "}\n"
        "validateRequest(request);\n"
        "CashRegisterEntity cash = getActiveCashRegister();\n"
        "SaleEntity sale = createBaseSale(request, cash);   // aquí va la clave"))

    f.append(h2("6.3. Normalizar la clave antes de usarla"))
    f.append(mono(
        "private String normalizarClave(String clave){\n"
        "    if(clave == null || clave.isBlank()){ return null; }\n"
        "    String limpia = clave.trim();\n"
        "    return limpia.length() > 64 ? limpia.substring(0, 64) : limpia;\n"
        "}"))

    f.append(li("<b>Recortar</b> los espacios: un salto de más convertiría la "
                "misma intención de cobro en dos claves distintas, que es "
                "justo lo que la idempotencia debe evitar."))
    f.append(li("<b>Tratar el blanco como ausente</b>: buscar por cadena vacía "
                "haría que todas las ventas sin clave colisionaran entre sí."))
    f.append(li("<b>Acotar a 64</b>, que es el ancho de la columna: un cliente "
                "que mande algo enorme no debe romper el INSERT."))

    f.append(h2("6.4. Un nombre DERIVADO, y el error que casi se repite"))
    f.append(nota(
        "Por qué el repositorio se llama findByIdempotencyKey",
        "Spring Data deriva el nombre de la consulta de la propiedad. "
        "<font face='Courier'>findBy</font> + "
        "<font face='Courier'>IdempotencyKey</font> resuelve a la propiedad "
        "<font face='Courier'>idempotencyKey</font> de la entidad, que existe. "
        "El error similar ya se cometió una vez en este proyecto, en "
        "<font face='Courier'>CashBoxRepository</font>: "
        "<font face='Courier'>existsByCashRegistersId</font> no funcionaba "
        "porque del lado de la relación la propiedad se llama "
        "<font face='Courier'>cashRegister</font>, no "
        "<font face='Courier'>cashRegisters</font>. La regla que dejó esa nota "
        "sigue vigente: si el nombre derivado no compila, el problema suele "
        "estar en el nombre de la propiedad, no en la consulta."))

    f.append(PageBreak())

    # ==================================================================
    # 7. LA CARRERA
    # ==================================================================
    f.append(h1("7. El caso que la comprobación previa no cubre"))

    f.append(p(
        "Todo lo anterior resuelve el doble Enter <b>secuencial</b>: la "
        "primera petición terminó y creó la venta, la segunda la encuentra "
        "y la devuelve. Pero hay un caso peor: las dos peticiones <b>al mismo "
        "tiempo</b>."))

    f.append(h2("7.1. Por qué la comprobación previa no basta"))
    f.append(mono(
        "petición A:  buscar clave -> no existe  ...  insertar\n"
        "petición B:  buscar clave -> no existe  ...  insertar\n"
        "\n"
        "Las dos buscan antes de que ninguna escriba. Es una carrera "
        "clásica: cada una ve un mundo donde la otra todavía no existe."))

    f.append(h2("7.2. El UNIQUE resuelve la carrera"))
    f.append(p(
        "Con el índice <font face='Courier'>UNIQUE</font>, PostgreSQL deja "
        "pasar a una de las dos y rechaza a la otra. En ese momento el "
        "problema cambia: ya no hay venta duplicada (la perdedora revierte "
        "toda su transacción, stock incluido), pero el cajero vería un error "
        "500 aunque su venta <b>sí</b> estuviera cobrada y registrada. Eso es "
        "peor que un bug: parece un fallo y además cobró."))

    f.append(h2("7.3. El detalle no obvio de PostgreSQL (y por qué funciona)"))
    f.append(p(
        "Para recuperar la venta ganadora hace falta saber algo: cuando el "
        "INSERT perdedor choca con el índice, <b>no falla de inmediato</b>. "
        "PostgreSQL lo bloquea dentro del índice hasta que la transacción "
        "ganadora resuelve. El error de clave duplicada <b>solo se emite "
        "cuando la ganadora ya hizo COMMIT</b>."))

    f.append(nota(
        "Consecuencia práctica",
        "Cuando el backend recibe el error de duplicado y relee la venta por "
        "esa clave, la fila <b>ya está confirmada y es visible</b>. Por eso "
        "esta recuperación no necesita reintentos ni esperas: no hay carrera "
        "al releer, porque el perdedor esperó a que el ganador terminara. Si "
        "alguien cambia a una base de datos sin este comportamiento, este "
        "código necesitará un ciclo de reintentos y esta nota habrá que "
        "rehacerla."))

    f.append(h2("7.4. Dónde se recupera, y por qué en transacción nueva"))
    f.append(mono(
        "// SaleController.processSale(request)\n"
        "try {\n"
        "    return ResponseEntity.ok(service.processSale(request));\n"
        "}\n"
        "catch(DataIntegrityViolationException e){\n"
        "    return service.findByIdempotencyKey(request.getIdempotencyKey())\n"
        "            .map(ResponseEntity::ok)\n"
        "            .orElseThrow(() -> e);   // <-- si NO es un cobro duplicado,\n"
        "}                                //     el error NO se esconde"))

    f.append(p(
        "La relectura se declara <font face='Courier'>@Transactional("
        "readOnly = true)</font> a propósito, y ese es un detalle "
        "importante: se ejecuta <b>después</b> de que la transacción de la "
        "venta perdedora revirtió. Abrir una transacción nueva es lo que "
        "permite que la lectura sea válida; leer dentro de la transacción ya "
        "marcada como <i>rollback-only</i> lanzaría "
        "<font face='Courier'>UnexpectedRollbackException</font> en lugar de "
        "la venta."))

    f.append(nota(
        "Por qué el orElseThrow no es un detalle menor",
        "Un error de integridad no siempre es un cobro duplicado: también "
        "puede ser una restricción de stock o un dato inválido. En esos casos "
        "no hay ninguna venta que devolver, y tragarse el error dejaría al "
        "cajero creyendo que cobró cuando no fue así. Si la relectura no "
        "encuentra nada, el error original se relanza tal cual."))

    f.append(tabla(
        ["Escenario", "Qué hace el backend", "Qué ve el cajero"],
        [
            ["Una sola pulsación",
             "No encuentra la clave, cobra normal",
             "Su venta y su ticket"],
            ["Doble Enter <b>secuencial</b>",
             "La 2ª encuentra la venta de la 1ª y la devuelve",
             "Su venta, sin error"],
            ["Dos peticiones <b>simultáneas</b>",
             "El UNIQUE rechaza a una; la otra se relee y se devuelve",
             "Su venta, sin error"],
            ["Restricción que <b>no</b> es duplicado",
             "No hay venta que devolver: relanza el error",
             "El error real, no un falso éxito"],
        ],
        [1.75 * inch, 2.55 * inch, 1.95 * inch]))

    f.append(PageBreak())

    # ==================================================================
    # 8. VERIFICACION CON CODIGO
    # ==================================================================
    f.append(h1("8. Verificación con código (automatizada)"))

    f.append(p(
        "La protección se fijó con pruebas en dos niveles. Todas en verde: "
        "<b>273 pruebas</b> en el backend (262 previas + 11 nuevas)."))

    f.append(h2("8.1. SaleImplIdempotencyTest — la lógica del servicio"))
    f.append(li("<b>devuelve la venta existente y no crea otra.</b> El assert "
                "que de verdad importa es "
                "<font face='Courier'>verify(saleRepository, never()).save"
                "(...)</font>: si se guardara algo, habría venta doble."))
    f.append(li("<b>tampoco toca el stock ni abre la caja.</b> Se comprueba "
                "que <font face='Courier'>productRepository.findById</font> no "
                "se llega a llamar, lo que prueba que el atajo está antes de "
                "validar el carrito."))
    f.append(li("<b>crea la venta y guarda la clave</b> en el camino normal."))
    f.append(li("<b>sin clave</b> (un cliente viejo) la venta se crea normal, "
                "y además no se consulta el repositorio."))
    f.append(li("<b>normalización:</b> recorta espacios, trata el blanco como "
                "ausente y acota a 64 caracteres."))

    f.append(h2("8.2. SaleControllerIdempotencyTest — la carrera"))
    f.append(li("<b>si el UNIQUE rechaza el INSERT, devuelve la venta ya "
                "registrada</b> con 200 en vez de un 500."))
    f.append(li("<b>si no hay venta con esa clave, el error no se esconde.</b> "
                "Esta prueba blinda el comportamiento contrario: tapar un "
                "error de integridad que no era duplicado sería peor que el "
                "bug original."))
    f.append(li("<b>un cobro normal no consulta la clave:</b> el camino feliz "
                "no debe pagar el costo de la búsqueda extra."))
    f.append(li("<b>la venta original se procesa una sola vez:</b> en la "
                "recuperación solo se lee, nunca se vuelve a cobrar."))

    f.append(nota(
        "Una lección sobre las pruebas",
        "Dos pruebas fallaron al escribirlas, y el motivo fue que el código "
        "era <b>mejor de lo que la prueba asumía</b>: con la clave ausente, "
        "el servicio no consulta el repositorio en absoluto (corta antes). La "
        "prueba afirmaba un stub que nunca se usaba, y Mockito la marcó como "
        "stubbing innecesario. La corrección fue <b>aceptar el "
        "comportamiento real</b> y cambiar el assert por "
        "<font face='Courier'>never()</font>, no relajar la prueba para "
        "hacela pasar."))

    # ==================================================================
    # 9. VERIFICACION REAL
    # ==================================================================
    f.append(h1("9. Verificación contra la base de datos real"))

    f.append(p(
        f"La migración se aplicó en Supabase el {FECHA} y <b>Flyway la "
        f"registró sola</b> al arrancar (informando que ya existía, que es "
        f"exactamente el comportamiento correcto de "
        f"<font face='Courier'>IF NOT EXISTS</font>). Con la columna "
        f"presente, la aplicación mapea el campo nuevo sin problemas."))

    f.append(h2("9.1. Que el UNIQUE existe y funciona"))
    f.append(p(
        "Dos <font face='Courier'>INSERT</font> con la misma clave dentro de "
        "una transacción que se revierte:"))
    f.append(mono(
        "BEGIN;\n"
        "INSERT ... idempotency_key='PRUEBA-V6-DUPLICADO' ...;\n"
        "INSERT ... idempotency_key='PRUEBA-V6-DUPLICADO' ...;\n"
        "ROLLBACK;\n"
        "\n"
        "ERROR:  duplicate key value violates unique constraint\n"
        "        \"idx_sales_idempotency\"\n"
        "DETALLE:  Key (idempotency_key)=(PRUEBA-V6-DUPLICADO) already exists."))

    f.append(h2("9.2. El doble cobro, con números reales"))
    f.append(p(
        "Dos peticiones <b>en paralelo</b> (dos jobs al mismo tiempo, no una "
        "detrás de otra) con la misma clave, comprando 2 unidades de cada vez. "
        "Stock inicial del producto: <b>17</b>."))

    f.append(tabla(
        ["Qué se comprobó", "Resultado"],
        [
            ["Respuesta de la 1ª petición", "saleId <b>8</b>, total 42.00"],
            ["Respuesta de la 2ª petición", "saleId <b>8</b>, total 42.00 "
             "(la misma)"],
            ["Ventas creadas con esa clave", "<b>1</b> (no 2)"],
            ["Stock del producto", "17 → <b>15</b> (bajó 2, no 4)"],
            ["Renglones en el detalle", "<b>1</b> de cantidad 2"],
        ],
        [2.5 * inch, 3.75 * inch]))

    f.append(h2("9.3. Y el caso secuencial"))
    f.append(p(
        "El caso real de un doble Enter: la segunda petición llega después de "
        "que la primera terminó. Con otra clave y una unidad, la 1ª llamada "
        "devolvió <font face='Courier'>saleId=10</font> y la 2ª devolvió "
        "<font face='Courier'>saleId=10</font>. Una sola venta; el stock bajó "
        "de 15 a 14, una unidad."))

    f.append(h2("9.4. Limpieza de los datos de prueba"))
    f.append(p(
        "Las dos ventas de prueba se <b>anularon por la API</b> (no se "
        "borraron a mano de la tabla), lo que devolvió el stock a su valor "
        "original, y la caja que se abrió para la prueba se cerró. Estado "
        "final verificado: producto con stock <b>17</b> y <b>cero</b> cajas "
        "abiertas. Las filas de venta se conservan como anuladas, que es lo "
        "que el propio sistema hace con una venta normal y por lo que el "
        "historial no miente."))

    f.append(nota(
        "Por qué se anulan y no se borran",
        "El módulo de ventas es append-only por decisión de diseño (ver el "
        "documento 02): una venta no se borra, se anula. Borrar filas de "
        "prueba a mano dejaría huecos en los números de ticket y en los "
        "reportes. Anular por la API mantiene la auditoría coherente."))

    f.append(PageBreak())

    # ==================================================================
    # 10. QUE FALTA Y POR QUE
    # ==================================================================
    f.append(h1("10. Decisiones que quedaron fuera (y por qué)"))

    f.append(tabla(
        ["Se descartó", "Por qué"],
        [
            ["Generar la clave en el backend",
             "Cada petición tendría una clave distinta: no habría forma de "
             "reconocer el duplicado (sección 3.1)."],
            ["Quitar el <font face='Courier'>Enter</font> del formulario",
             "Escribiendo el monto con Enter es la forma natural de cobrar. "
             "Se corrigió la consecuencia, no la costumbre."],
            ["Bloqueo pesimista con "
             "<font face='Courier'>SELECT ... FOR UPDATE</font>",
             "No protege nada: sobre una fila que todavía no existe no hay "
             "nada que bloquear. El UNIQUE resuelve la carrera sin "
             "serializar a todos los cajeros."],
            ["Tabla aparte de claves reservadas",
             "Una reserva previa en tabla propia duplica el estado de la "
             "venta y deja transactions colgando si el proceso muere entre "
             "la reserva y el cobro."],
            ["Reintentos en la recuperación",
             "En PostgreSQL no hacen falta: el error se emite tras el "
             "COMMIT de la ganadora (sección 7.3)."],
        ],
        [1.9 * inch, 4.35 * inch]))

    f.append(h2("10.1. Lo que sigue pendiente"))
    f.append(li("<b>La migración V6 está aplicada en Supabase</b>, pero "
                "cualquier base de datos <i>nueva</i> la toma Flyway "
                "automáticamente al arrancar. No hay paso manual."))
    f.append(li("<b>El POS sigue sin un reintento automático</b>: "
                "si el backend no responde, el cajero tiene que volver a "
                "apretar. Eso está bien, porque el segundo intento lleva la "
                "<b>misma clave</b> y no cobra dos veces: es justamente el "
                "escenario para el que se construyó esto."))
    f.append(li("<b>Las alertas nativas (<font face='Courier'>alert("
                "</font>) siguen siendo la forma de aviso</b> en el POS. "
                "Cambiarlo a <font face='Courier'>MatSnackBar</font> es una "
                "tanda aparte de consistencia visual; no se mezcló aquí "
                "porque no tiene relación con el doble cobro."))

    f.append(h1("11. Índice de archivos tocados"))

    f.append(tabla(
        ["Archivo", "Qué se hizo"],
        [
            ["<font face='Courier'>db/migration/V6__ventas_idempotencia.sql"
            "</font>", "Nuevo: columna + índice UNIQUE parcial"],
            ["<font face='Courier'>model/entity/SaleEntity.java</font>",
             "Nuevo campo <font face='Courier'>idempotencyKey</font>"],
            ["<font face='Courier'>model/dto/Sale/SaleRequest.java</font>",
             "Nuevo campo <font face='Courier'>idempotencyKey</font> (entrada)"],
            ["<font face='Courier'>repository/SaleRepository.java</font>",
             "Nuevo <font face='Courier'>findByIdempotencyKey</font>"],
            ["<font face='Courier'>service/SaleService.java</font>",
             "Nuevo <font face='Courier'>findByIdempotencyKey</font> "
             "(relectura)"],
            ["<font face='Courier'>service/impl/SaleImpl.java</font>",
             "Comprobación previa, normalización, atajo temprano, "
             "persistencia de la clave"],
            ["<font face='Courier'>controller/SaleController.java</font>",
             "Recuperación de la carrera simultánea"],
            ["<font face='Courier'>test/.../SaleImplIdempotencyTest.java"
            "</font>", "Nuevo: 7 pruebas del servicio"],
            ["<font face='Courier'>test/.../SaleControllerIdempotencyTest.java"
            "</font>", "Nuevo: 4 pruebas de la carrera"],
            ["<font face='Courier'>features/charge/facade/sale-facade.ts"
            "</font>",
             "Clave por intento, banderas de vuelo, envío de la clave"],
            ["<font face='Courier'>features/charge/cobro.html</font>",
             "Botón deshabilitado y etiqueta de “Cobrando…”"],
            ["<font face='Courier'>core/interfaces/sale/sale.ts</font>",
             "Campo opcional <font face='Courier'>idempotencyKey</font>"],
        ],
        [2.55 * inch, 3.7 * inch]))

    f.append(Spacer(1, 12))
    f.append(p(
        "Con esta V6 el cobro del POS tiene, por fin, la propiedad que "
        "necesitaba: pulsar Enter dos veces cobra una vez. Y la paginación del "
        "carrito deja de empujar el botón de guardar fuera de la pantalla.",
        "nota"))

    return f


def main():
    doc = SimpleDocTemplate(
        DESTINO,
        pagesize=LETTER,
        leftMargin=0.75 * inch,
        rightMargin=0.75 * inch,
        topMargin=0.7 * inch,
        bottomMargin=0.85 * inch,
        title="Paginación del carrito e idempotencia de ventas (V6)",
        author="Compras-Backend",
        subject="V6: clave de idempotencia para que un doble Enter no cobre "
                "dos veces, con indice UNIQUE, carrera simultanea y "
                "verificacion real",
    )
    doc.build(construir(), onFirstPage=pie, onLaterPages=pie)
    print("PDF generado:", DESTINO)
    print("Tamano:", os.path.getsize(DESTINO), "bytes")


if __name__ == "__main__":
    sys.exit(main())