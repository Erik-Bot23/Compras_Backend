#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Genera docs/06-Venta-Usuario-y-Historial-Caja.pdf

Explicacion de las mejoras V5: backend (migracion, entidades, servicios,
endpoint de reportes) y frontend (paginacion del carrito, modal de cajas,
historial de caja, filtros de usuarios dados de baja, alineacion de columnas).

Ejemplo:  python docs/generar_pdf_v5_mejoras.py
"""

import os
import sys

from reportlab.lib import colors
from reportlab.lib.enums import TA_JUSTIFY
from reportlab.lib.pagesizes import LETTER
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import (
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)

# --------------------------------------------------------------------------
# Paleta: la misma del tema oscuro de la app y del PDF anterior (05-), para
# que la coleccion de docs se lea como una sola serie.
# --------------------------------------------------------------------------
TINTA = colors.HexColor("#0f172a")
TINTA_SUAVE = colors.HexColor("#334155")
GRIS = colors.HexColor("#64748b")
AZUL = colors.HexColor("#2563eb")
VERDE = colors.HexColor("#16a34a")
AMARILLO = colors.HexColor("#f59e0b")
ROJO = colors.HexColor("#dc2626")
FONDO_CODIGO = colors.HexColor("#f1f5f9")
FONDO_CAJA = colors.HexColor("#fef3c7")
FONDO_BIEN = colors.HexColor("#dcfce7")
FONDO_INFO = colors.HexColor("#e0f2fe")

BASE = os.path.dirname(os.path.abspath(__file__))
DESTINO = os.path.join(BASE, "06-Venta-Usuario-y-Historial-Caja.pdf")


def estilos():
    base = getSampleStyleSheet()

    def crear(nombre, **kwargs):
        return ParagraphStyle(nombre, parent=base["Normal"], **kwargs)

    return {
        "titulo": crear(
            "titulo", fontName="Helvetica-Bold", fontSize=19, leading=23,
            textColor=TINTA, spaceAfter=4,
        ),
        "subtitulo": crear(
            "subtitulo", fontName="Helvetica", fontSize=10.5, leading=14,
            textColor=GRIS, spaceAfter=16,
        ),
        "h1": crear(
            "h1", fontName="Helvetica-Bold", fontSize=14, leading=17,
            textColor=TINTA, spaceBefore=16, spaceAfter=7,
        ),
        "h2": crear(
            "h2", fontName="Helvetica-Bold", fontSize=11.5, leading=14,
            textColor=AZUL, spaceBefore=12, spaceAfter=5,
        ),
        "p": crear(
            "p", fontSize=9.5, leading=13.6, textColor=TINTA,
            alignment=TA_JUSTIFY, spaceAfter=7,
        ),
        "li": crear(
            "li", fontSize=9.5, leading=13.4, textColor=TINTA,
            leftIndent=15, bulletIndent=5, spaceAfter=3.5,
        ),
        "codigo": crear(
            "codigo", fontName="Courier", fontSize=8, leading=10.4,
            textColor=TINTA, backColor=FONDO_CODIGO, leftIndent=7,
            rightIndent=7, spaceBefore=3, spaceAfter=7, borderPadding=5,
        ),
        "nota": crear(
            "nota", fontSize=9.5, leading=13.4, textColor=TINTA,
            backColor=FONDO_CAJA, borderPadding=7, leftIndent=5, rightIndent=5,
            spaceBefore=5, spaceAfter=8,
        ),
        "info": crear(
            "info", fontSize=9.5, leading=13.4, textColor=TINTA,
            backColor=FONDO_INFO, borderPadding=7, leftIndent=5, rightIndent=5,
            spaceBefore=5, spaceAfter=8,
        ),
        "tabla_txt": crear("tabla_txt", fontSize=8.6, leading=11.4, textColor=TINTA),
        "tabla_cab": crear(
            "tabla_cab", fontName="Helvetica-Bold", fontSize=8.6, leading=11.4,
            textColor=colors.white,
        ),
    }


E = estilos()


def p(txt, estilo="p"):
    return Paragraph(txt, E[estilo])


def h1(txt):
    return Paragraph(txt, E["h1"])


def h2(txt):
    return Paragraph(txt, E["h2"])


def codigo(txt):
    """
    Convierte un bloque de código en un Paragraph.

    <b>Escapa los ángulos</b> (&lt; &gt;) porque reportlab interpreta el texto
    como HTML: sin escapar, un `<img>` dentro de un comentario CSS se toma por
    una etiqueta real y el documento falla al generarse. Passando por los
    caracteres entity, todo bloque de código se puede escribir tal cual aparece
    en el archivo, con sus etiquetas y sus comentarios.
    """
    escapado = (
        txt.strip("\n")
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    )
    lineas = escapado.split("\n")
    return Paragraph(
        "<br/>".join(l.replace(" ", "&nbsp;") for l in lineas), E["codigo"]
    )


def li(txt):
    return Paragraph(txt, E["li"], bulletText="•")


def tabla(cabeceras, filas, anchos):
    datos = [[Paragraph(c, E["tabla_cab"]) for c in cabeceras]]
    for fila in filas:
        datos.append([Paragraph(str(c), E["tabla_txt"]) for c in fila])

    t = Table(datos, colWidths=anchos, repeatRows=1)
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), TINTA_SUAVE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#cbd5e1")),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1),
         [colors.white, colors.HexColor("#f8fafc")]),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]))
    return t


def nota(titulo, cuerpo):
    return Paragraph(
        f'<font color="#92400e"><b>{titulo}</b></font><br/>{cuerpo}',
        E["nota"],
    )


def info(titulo, cuerpo):
    return Paragraph(
        f'<font color="#075985"><b>{titulo}</b></font><br/>{cuerpo}',
        E["info"],
    )


def pie(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 7.5)
    canvas.setFillColor(GRIS)
    canvas.drawString(
        0.75 * inch, 0.55 * inch,
        "Compras-Backend · Venta–Usuario e Historial de Caja (V5)")
    canvas.drawRightString(
        LETTER[0] - 0.75 * inch, 0.55 * inch, f"Página {doc.page}")
    canvas.setStrokeColor(colors.HexColor("#e2e8f0"))
    canvas.line(0.75 * inch, 0.72 * inch,
                LETTER[0] - 0.75 * inch, 0.72 * inch)
    canvas.restoreState()


def construir():
    f = []

    # ==================================================================
    # PORTADA
    # ==================================================================
    f.append(p("Venta–Usuario e Historial de Caja (V5)", "titulo"))
    f.append(p(
        "Todo lo que se hizo en esta tanda: qué clase resuelve qué, cómo se "
        "relacionan, por qué se hizo así y el código que se usó. Backend Spring "
        "Boot + Frontend Angular.",
        "subtitulo"))

    f.append(tabla(
        ["Campo", "Contenido"],
        [
            ["Fecha", "2026-10-01"],
            ["Alcance",
             "<b>Backend:</b> ventas ligadas a usuarios, fechas de alta/baja de "
             "usuarios, re-alta de caja, endpoint de historial de caja con "
             "filtros.<br/>"
             "<b>Frontend:</b> paginación del carrito de compras, flechas de la "
             "paginación, modal de cajas, historial de caja en Reportes, filtros "
             "de usuarios dados de baja, alineación de columnas."],
            ["Base de datos", "Migración nueva <font face='Courier'>V5</font> "
                              "(no se editó ninguna anterior)"],
            ["Verificación",
             "<font face='Courier'>mvnw test</font> → 258/258 · "
             "<font face='Courier'>ng build</font> → OK"],
        ],
        [1.3 * inch, 5.2 * inch],
    ))
    f.append(Spacer(1, 12))

    f.append(info(
        "Antes de empezar: dos cosas NO se hicieron a propósito",
        "<b>1. No se rellenó el usuario de las ventas viejas.</b> El dato no "
        "existía, así que quedó en NULL. Inventar un usuario sería peor que no "
        "tener el dato.<br/>"
        "<b>2. No se cambió la tabla de ventas a 'una fila por usuario'.</b> "
        "Los totales por usuario se calculan al vuelo en el reporte, que es "
        "menos código y no duplica la información."))

    # ==================================================================
    # 1. EL MAPA
    # ==================================================================
    f.append(h1("1. El mapa de clases, en una imagen"))

    f.append(codigo("""
  users                          sales                       cash_boxes
  (UserEntity)                   (SaleEntity)                 (CashBoxEntity)
  -----------                    -----------                 --------------
  id                         +--> id                             id
  name                         |   sale_date                     number  UNIQUE
  email  UNIQUE                |   total                         description
  role_id --> roles            |   payment_method               active
  active                       |   cash_register_id --> cash_registers
  activated_at      (V5)        |   user_id       --> users  <----+  (V5 NUEVO)
  deactivated_at    (V5)        |
  |                              cash_registers
  |                              (CashRegisterEntity)
  |                              --------------------
  +--------------------------->  id
       (V5)                       number        (copia historica)
                                  opened_at / closed_at / active
                                  opening_amount / expected_amount
                                  difference / difference_reason
                                  cash_box_id --> cash_boxes   (V4)
                                  cash_sales / debit_sales / ...
""".strip()))

    f.append(p(
        "Lo nuevo de esta tanda son las dos relaciones marcadas "
        "<b>V5</b>: <font face='Courier'>sales.user_id</font> y las dos fechas "
        "en <font face='Courier'>users</font>. Todo lo demás ya existía."))

    f.append(h2("1.1 Quién hace qué"))

    f.append(tabla(
        ["Clase / archivo", "Responsabilidad", "Qué cambió"],
        [
            ["<font face='Courier'>V5__ventas_usuario...</font>",
             "Las dos columnas nuevas y su backfill",
             "<font color='#16a34a'><b>NUEVA</b></font>"],
            ["<font face='Courier'>SaleEntity</font>",
             "La venta, ahora con su usuario",
             "<font color='#16a34a'>+ <font face='Courier'>user</font></font>"],
            ["<font face='Courier'>SaleImpl</font>",
             "Lee el usuario del JWT y lo pone en la venta",
             "<font color='#16a34a'>+ <font face='Courier'>currentUserOrNull</font></font>"],
            ["<font face='Courier'>UserEntity</font>",
             "El usuario, ahora con fechas de alta y baja",
             "<font color='#16a34a'>+ 2 campos</font>"],
            ["<font face='Courier'>UserImpl</font>",
             "Mantiene coherentes esas fechas en alta/baja",
             "<font color='#16a34a'>+ lógica</font>"],
            ["<font face='Courier'>UserDto</font> / <font face='Courier'>UserMapper</font>",
             "Exponen las fechas al frontend",
             "<font color='#16a34a'>+ 2 campos</font>"],
            ["<font face='Courier'>CashRegisterService/Impl</font>",
             "Re-alta de una caja dada de baja",
             "<font color='#16a34a'>+ <font face='Courier'>activateBox</font></font>"],
            ["<font face='Courier'>CashBoxReportDTO</font>",
             "El DTO del historial de caja (caja + turnos + vendedores)",
             "<font color='#16a34a'><b>NUEVO</b></font>"],
            ["<font face='Courier'>ReportsService/Impl</font>",
             "El filtro por usuario y fechas, y el agrupado por vendedor",
             "<font color='#16a34a'>+ método</font>"],
            ["<font face='Courier'>SaleMapper</font>",
             "Copia el nombre del usuario al DTO de venta",
             "<font color='#16a34a'>+ 6 líneas</font>"],
        ],
        [1.85 * inch, 2.75 * inch, 1.9 * inch],
    ))
    f.append(Spacer(1, 8))

    # ==================================================================
    # 2. MIGRACION
    # ==================================================================
    f.append(PageBreak())
    f.append(h1("2. La migración V5"))

    f.append(p(
        "Es una migración <b>nueva</b>. Nunca se edita una migración que ya "
        "se aplicó: si se edita, Flyway cambia el checksum de la versión que ya "
        "está en la base y la app deja de arrancar."))

    f.append(h2("2.1 Qué agrega"))

    f.append(codigo("""
  ALTER TABLE public.sales
      ADD COLUMN IF NOT EXISTS user_id bigint;

  CREATE INDEX IF NOT EXISTS idx_sales_user ON public.sales (user_id);

  ALTER TABLE public.sales
      ADD CONSTRAINT fk_sales_user
      FOREIGN KEY (user_id) REFERENCES public.users(id)
      ON DELETE SET NULL;          <-- importante, ver abajo

  ALTER TABLE public.users ADD COLUMN IF NOT EXISTS activated_at    timestamp(6);
  ALTER TABLE public.users ADD COLUMN IF NOT EXISTS deactivated_at timestamp(6);

  -- Indice PARCIAL: solo las filas dadas de baja se buscan por fecha
  CREATE INDEX IF NOT EXISTS idx_users_deactivated_at
      ON public.users (deactivated_at)
      WHERE active = false;
""".strip()))

    f.append(h2("2.2 Las dos decisiones del diseño"))

    f.append(nota(
        "<b>ON DELETE SET NULL</b>, y no CASCADE",
        "Si se borra un usuario, sus ventas <b>no</b> se borran. Con CASCADE, "
        "borrar un usuario borraría el historial de ventas del local, que es un "
        "dato del negocio y no del empleado. Con SET NULL la venta sobrevive y "
        "queda sin dueño, que es la verdad: nadie puede decir quién la hizo. "
        "Perder una venta es peor que perder un dato de ella."))

    f.append(nota(
        "Por qué <font face='Courier'>NULL</font> y no un usuario inventado",
        "Las ventas anteriores a V5 no tienen dueño porque el dato nunca se "
        "guardó. Ponerle el admin, o el primer usuario, sería fabricar "
        "información: el reporte mostraría 'este usuario vendió 500' y sería "
        "falso. Un NULL se muestra como 'Sin usuario' y no miente."))

    f.append(p(
        "El índice de <font face='Courier'>deactivated_at</font> es <b>parcial</b> "
        "(tiene <font face='Courier'>WHERE active = false</font>) porque la única "
        "consulta que lo usa es la de la tabla de dados de baja. Indexar toda la "
        "tabla para eso sería gastar espacio en las filas activas, que son las "
        "que nunca se buscan por esa columna."))

    # ==================================================================
    # 3. VENTA -> USUARIO
    # ==================================================================
    f.append(h1("3. La venta ligada a su usuario"))

    f.append(h2("3.1 La entidad"))

    f.append(codigo('''
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private UserEntity user;
'''.strip()))

    f.append(p(
        "<b>LAZY</b> porque el filtro por usuario solo necesita el id (que ya "
        "viene en la propia venta) y el nombre se pide en la consulta de "
        "reportes. Con EAGER, listar el historial de ventas haría un SELECT "
        "extra por venta: es el problema clásico de N+1."))

    f.append(h2("3.2 De dónde sale el usuario"))

    f.append(p(
        "Del <b>contexto de seguridad</b>, no del body de la petición. Esta es la "
        "decisión de seguridad más importante de esta tanda:"))
    f.append(codigo("""
  private UserEntity currentUserOrNull(){
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();

      if(auth == null || !auth.isAuthenticated()){
          return null;
      }
      // "anonymousUser" es lo que Spring pone cuando no hay token: no es un
      // usuario real y no debe buscarlo en la base.
      if(!(auth.getPrincipal() instanceof UserDetails details)){
          return null;
      }
      return userRepository.findByEmail(details.getUsername()).orElse(null);
  }
""".strip()))

    f.append(nota(
        "Por qué NO pedir el usuario en el JSON",
        "Si el endpoint aceptara <font face='Courier'>{\"userId\": 7}</font> en el "
        "cuerpo, cualquiera con un token válido podría registrar ventas a nombre "
        "de otro. El reporte de 'quién vendió' dejaría de ser confiable, que es "
        "justo lo que se agregó para poder auditar. El usuario siempre sale "
        "del token, que ya fue validado."))

    f.append(p(
        "Que devuelva <b>null</b> en vez de fallar es deliberado: los tests de "
        "integración no levantan el filtro de seguridad, y una llamada interna "
        "no lo tiene. El costo es una venta sin usuario, que es exactamente el "
        "mismo estado que tienen las ventas anteriores a V5."))

    # ==================================================================
    # 4. FECHAS DE USUARIOS
    # ==================================================================
    f.append(PageBreak())
    f.append(h1("4. Las fechas de alta y baja de los usuarios"))

    f.append(p(
        "<font face='Courier'>users.active</font> es un booleano: dice <b>sí</b> "
        "está dado de baja, pero no <b>cuándo</b>. Con eso la pregunta 'buscar "
        "el registro de la persona que se fue en abril' no tiene respuesta."))

    f.append(codigo("""
  createUser   -> setActivatedAt(now); setDeactivatedAt(null);
  activateUser -> setActivatedAt(now); setDeactivatedAt(null);   // re-activar
  deactivateUser -> if(estaba activo) setDeactivatedAt(now);
                    setActivatedAt(null);                          // ver abajo
""".strip()))

    f.append(h2("4.1 Por qué la baja no pisa la fecha si ya estaba dado de baja"))

    f.append(p(
        "La guarda <font face='Courier'>if(user.isActive())</font> parece "
        "redundante y no lo es. Si se llama dos veces <i>dar de baja</i>, la segunda "
        "actualizaría la fecha y se perdería la <b>original</b>. Y la original es "
        "la que responde '¿desde cuándo se fue?'. La fecha de baja es un "
        "hecho, no un estado que se pueda reescribir."))

    f.append(nota(
        "Por qué al dar de baja se limpia la fecha de alta",
        "Es contraintuitivo y por eso se comenta en el código. La regla de "
        "coherencia es: <b>cada usuario tiene exactamente una de las dos fechas</b>. "
        "Si un usuario se da de baja y se vuelve a dar de alta, la tabla de "
        "'dados de baja' no debe seguir mostrándolo; y si se da de baja otra "
        "vez, tiene que constar la <b>última</b> baja. Limpiar la de alta al dar "
        "de baja deja el estado sin contradicciones."))

    f.append(nota(
        "Esto NO es un historial de cambios",
        "Es la última alta y la última baja. Para tener cada cambio haría falta "
        "una tabla de auditoría aparte, que no existe y aquí no hace falta. "
        "Decirlo evita que alguien asuma que se puede preguntar '¿cuántas veces "
        "se dio de baja esta persona?' y descubra que no."))

    # ==================================================================
    # 5. RE-ALTA DE CAJA
    # ==================================================================
    f.append(h1("5. La caja se puede volver a dar de alta"))

    f.append(codigo("""
  @PreAuthorize("hasAuthority('ABRIR_CAJA')")
  @PatchMapping("/boxes/{id}/active")
  public ResponseEntity<Void> activateBox(@PathVariable Long id){
      service.activateBox(id);
      return ResponseEntity.noContent().build();
  }
""".strip()))

    f.append(h2("5.1 Por qué el número NO se puede reutilizar"))

    f.append(p(
        "La pregunta original era: 'cuando una caja se da de baja, ¿se puede "
        "volver a abrir una caja con ese mismo nombre?'. La respuesta es "
        "<b>no, y no hay forma de hacerlo</b>, y eso ya era así antes de V5:"))

    f.append(li("El <font face='Courier'>UNIQUE</font> está en "
                "<font face='Courier'>cash_boxes.number</font>."))
    f.append(li("Dar de baja <b>no borra</b> la fila: solo pone "
                "<font face='Courier'>active = false</font>."))
    f.append(li("El número sigue ocupado por esa fila, para siempre."))

    f.append(p(
        "Reactivarla es <b>la misma caja</b>, con su mismo número y todo su "
        "historial. Y debe serlo: sus ventas apuntan a sus turnos, y los turnos a "
        "ella. Crear una caja nueva con el mismo número rompería ese encadenamiento "
        "y dejaría ventas sin caja."))

    f.append(tabla(
        ["Estado de la caja", "Borrar", "Dar de baja", "Dar de alta"],
        [
            ["Nunca se abrió", "<font color='#16a34a'><b>Sí</b></font>",
             "<font color='#64748b'>Sí</font>",
             "<font color='#64748b'>No hace falta</font>"],
            ["Ya tuvo turnos", "<font color='#dc2626'><b>No</b></font> (409)",
             "<font color='#f59e0b'><b>Sí</b></font>",
             "<font color='#16a34a'><b>Sí</b></font>"],
            ["Turno abierto", "<font color='#dc2626'><b>No</b></font> (409)",
             "<font color='#dc2626'><b>No</b></font> (409)",
             "<font color='#dc2626'><b>No</b></font> (409)"],
        ],
        [1.95 * inch, 1.35 * inch, 1.35 * inch, 1.85 * inch],
    ))
    f.append(Spacer(1, 10))

    # ==================================================================
    # 6. ENDPOINT DE REPORTES
    # ==================================================================
    f.append(PageBreak())
    f.append(h1("6. El endpoint del historial de caja"))

    f.append(codigo("""
  @PreAuthorize("hasAuthority('VER_REPORTES')")
  @GetMapping("/cash-box/{boxId}")
  public CashBoxReportDTO getCashBoxReport(
          @PathVariable Long boxId,
          @RequestParam(required = false) LocalDate from,
          @RequestParam(required = false) LocalDate to,
          @RequestParam(required = false) Long userId){
      return reportsService.getCashBoxReport(boxId, from, to, userId);
  }
""".strip()))

    f.append(h2("6.1 Por qué se pide una CAJA y no un TURNO"))

    f.append(p(
        "Antes el selector de Reportes listaba <b>turnos</b>. Una caja abierta "
        "diez veces aparecía diez veces, todas con el mismo texto 'CAJA 1', y el "
        "usuario tenía que adivinar cuál de las diez era. La pregunta '¿qué pasó "
        "en CAJA 1?' era imposible de responder."))

    f.append(tabla(
        ["", "Antes (por turno)", "Ahora (por caja)"],
        [
            ["El dropdown muestra",
             "Un renglón por cada apertura (10 veces 'CAJA 1')",
             "Un renglón por caja ('CAJA 1', 'CAJA 2')"],
            ["La tabla muestra",
             "Los montos de UN turno, en vertical",
             "Una fila por turno, en tabla"],
            ["Comparar turnos",
             "Imposible: había que memorizar el número",
             "Una fila por turno, se comparan de un vistazo"],
            ["Filtrar por usuario",
             "No se podía: no había ni la columna",
             "Sí, y por vendedor dentro de cada turno"],
        ],
        [1.3 * inch, 2.55 * inch, 2.65 * inch],
    ))
    f.append(Spacer(1, 10))

    f.append(h2("6.2 La forma del DTO"))

    f.append(codigo("""
  CashBoxReportDTO              // una caja
    boxId, number, description, active
    sessions -> List<CashBoxSessionDTO>
    totalSessions              // SIN contar filtros

  CashBoxSessionDTO             // un turno
    sessionId, number, openedAt, closedAt, active
    openingAmount   <- del corte CONGELADO
    cashSales / debitSales / creditSales / totalSales  <- calculados
    expectedAmount, difference, differenceReason
    totalTickets, grossProfit
    sellers  -> List<CashSessionSellerDTO>
    sales    -> List<SaleDetailHistoryResponse>
    filtrado   <- true si hay algun filtro activo

  CashSessionSellerDTO          // un vendedor dentro de un turno
    userId, userName, tickets, total
""".strip()))

    f.append(h2("6.3 Las tres decisiones que hay que entender"))

    f.append(p(
        "<b>El filtro se aplica en Java, no en SQL.</b> Una caja tiene dos o tres "
        "turnos, así que el conjunto es chico y se filtra en memoria sin "
        "penalizar. A cambio, una sola pasada de código calcula las ventas, el "
        "agrupado por vendedor y la utilidad. Hacerlo en SQL serían cuatro "
        "consultas distintas y luego reconciliarlas en Java, que es justo donde "
        "se esconden los bugs."))

    f.append(p(
        "<b>Los montos se calculan sobre las ventas filtradas.</b> Si el usuario "
        "filtra por Juan, el total que ve es el de Juan. El fondo inicial "
        "(<font face='Courier'>openingAmount</font>) sí viene del corte "
        "congelado, porque es dinero físico que se puso en el cajón y no es una "
        "venta."))

    f.append(nota(
        "Por qué <font face='Courier'>difference</font> llega en NULL con filtros",
        "La diferencia del corte es un dato congelado del turno COMPLETO. Si el "
        "usuario filtra por Juan y ve 'diferencia: -50' junto a un total de $200, "
        "el sistema estaría afirmando que faltaron $50 en un turno del que solo "
        "se está viendo una parte. Es un descuadre que no ocurrió, y es "
        "exactamente el tipo de mentira que un corte de caja no puede decir. Por "
        "eso va en NULL y el flag <font face='Courier'>filtrado</font> le dice a "
        "la tabla que debe ocultar la columna en vez de pintar un cero."))

    f.append(p(
        "<b>Un turno sin ventas que cumplan el filtro no aparece.</b> Es la "
        "decisión que se confirmó al revisar: con filtro de usuario, un turno "
        "donde no vendió ese usuario desaparece en vez de mostrar $0. Mostrarlo "
        "con ceros sería información falsa: 'este turno no vendió nada' es muy "
        "distinto de 'este turno no aparece porque Juan no trabajó ahí'."))

    f.append(h2("6.4 El agrupado por vendedor"))

    f.append(codigo("""
  // Una caja puede tener VARIOS vendedores: es lo normal, no la excepcion.
  Map<Long, CashSessionSellerDTO> porUsuario = new LinkedHashMap<>();

  for(SaleEntity venta : ventas){
      UserEntity usuario = venta.getUser();
      if(usuario == null){
          // Venta anterior a V5: no tiene dueno. Se agrupa aparte en vez de
          // inventar un "Desconocido" que pareceria un usuario real.
          sinUsuario = sinUsuario.add(venta.getTotal());
          sinUsuarioTickets++;
      }else{
          CashSessionSellerDTO vendedor = porUsuario.computeIfAbsent(
              usuario.getId(),
              id -> new CashSessionSellerDTO(id, usuario.getName()));
          vendedor.setTickets(vendedor.getTickets() + 1);
          vendedor.setTotal(vendedor.getTotal().add(venta.getTotal()));
      }
  }
""".strip()))

    f.append(p(
        "El <font face='Courier'>computeIfAbsent</font> agrupa en una sola "
        "pasada: el mismo vendedor puede tener ventas en varios turnos y en cada "
        "uno aparece una vez. Las ventas sin dueño se agrupan aparte al final, "
        "con un nombre explícito, para que se lean como lo que son."))

    # ==================================================================
    # 7. FRONTEND: PAGINACION
    # ==================================================================
    f.append(PageBreak())
    f.append(h1("7. Frontend: la paginación"))

    f.append(h2("7.1 Las flechas de la paginación (un bug de 512 píxeles)"))

    f.append(p(
        "El botón de <font face='Courier'>&lt;app-pagination&gt;</font> ya tenía "
        "<font face='Courier'>&lt;img src=\"izquierda.png\"&gt;</font> desde "
        "hace semanas. El problema es que los PNG miden <b>512×512</b> y no "
        "tenían ninguna regla CSS: el navegador los renderizaba a su tamaño "
        "natural y reventaba el botón por dentro."))

    f.append(codigo("""
  .pagination-buttons button {
      display: inline-flex;      /* el <img> y el texto en una fila */
      align-items: center;
      gap: 6px;
  }

  .pagination-buttons button img {
      width: 14px;
      height: 14px;
      object-fit: contain;
      flex-shrink: 0;
      filter: brightness(0) invert(1);   /* el PNG es negro -> blanco */
  }
""".strip()))

    f.append(nota(
        "El filtro SÍ funciona aquí (y en otros sitios NO)",
        "Se verificó muestreando los píxeles del archivo: "
        "<font face='Courier'>izquierda.png</font> es un PNG de un solo color "
        "(negro) sobre fondo transparente. Para una imagen así, "
        "<font face='Courier'>brightness(0) invert(1)</font> es correcto: "
        "aplasta todo a negro y lo invierte a blanco.<br/><br/>"
        "Ese mismo filtro aplicado a <font face='Courier'>anadir.png</font> "
        "<b>no</b> funciona: esa imagen tiene dos colores (una cruz verde y su "
        "contorno negro) y el filtro fusiona los dos en un bloque blanco macizo. "
        "Por eso los dos casos se tratan distinto en esta tanda."))

    f.append(h2("7.2 El carrito de la compra, paginado"))

    f.append(p(
        "Cada renglón de compra tiene 6 columnas y 4 inputs. Con 30 productos el "
        "modal medía más que la pantalla y el botón 'Guardar' quedaba "
        "inalcanzable. La solución es paginar a 5 renglones y darle scroll "
        "interno."))

    f.append(codigo("""
  lineasPage = 0;
  lineasPageSize = 5;           // 5 y no 8: cada renglón es alto

  /** Primer indice de `lineas` de la pagina visible. */
  get lineasIndiceBase(): number {
      return this.lineasPage * this.lineasPageSize;
  }

  get lineasTotalPaginas(): number {
      return Math.max(1, Math.ceil(this.lineas.length / this.lineasPageSize));
  }

  /** Renglones de la pagina actual: los que se dibujan. */
  get lineasPagina(): LineaCompra[] {
      return this.lineas.slice(this.lineasIndiceBase,
                               this.lineasIndiceBase + this.lineasPageSize);
  }
""".strip()))

    f.append(h2("7.3 El bug de índices que casi no se ve"))

    f.append(p(
        "Este es el detalle más importante de la paginación, y el que más "
        "bugs produce. En la plantilla:"))
    f.append(codigo("""
  <div class="linea" *ngFor="let l of lineasPagina; let i = index">
      <button (click)="quitarLinea(lineasIndiceBase + i)">   <!-- OJO -->
  </div>
""".strip()))

    f.append(p(
        "La <b><font face='Courier'>i</font> del <font face='Courier'>"
        "*ngFor</font> es el índice dentro de la página visible</b>, no dentro de "
        "<font face='Courier'>lineas</font>. En la página 1 coinciden, así que el "
        "bug no se ve. En la página 2, el primer botón borraría el renglón 1 (de "
        "la página 1) en vez del 6."))

    f.append(nota(
        "El síntoma clásico de este error",
        "Funciona en la primera página y falla en las demás. Eso hace que sea "
        "difícil de reproducir si solo se prueba con pocos productos. Es la "
        "razón por la que el comentario del template lo explica en mayúsculas: "
        "para que el próximo que lo lea no lo 'simplifique'."))

    f.append(p("Y en el template:"))
    f.append(codigo("""
  <div class="lineas-cabecera-wrap">     <!-- sticky: los titulos no se scrollan -->
    <div class="linea-cabecera">Producto | Cantidad | ...</div>
  </div>

  <div class="lineas-scroll">            <!-- max-height + overflow-y: auto -->
    <div class="linea" *ngFor="let l of lineasPagina; let i = index">
      ...
  </div>
""".strip()))

    f.append(p(
        "La cabecera va <b>fuera</b> del bloque con scroll y con "
        "<font face='Courier'>position: sticky</font>: si se scrolleara con las "
        "filas, en el renglón 6 el usuario no sabría qué columna está leyendo."))

    f.append(p(
        "Y un detalle de manejo de la misma clase:"))
    f.append(codigo("""
  agregarLinea() {
      this.lineas.push(this.nuevaLinea());

      // Si la compra ya crecio mas alla de la ultima pagina, se salta a esa
      // pagina para que el renglon nuevo quede a la vista. Sin esto, agregar
      // el renglon 8 con pageSize 5 lo agrega invisible (pagina 1) y parece
      // que el boton no hizo nada.
      const ultimaPagina = Math.max(0,
          Math.ceil(this.lineas.length / this.lineasPageSize) - 1);
      if(this.lineasPage < ultimaPagina) {
          this.lineasPage = ultimaPagina;
      }
  }
""".strip()))

    # ==================================================================
    # 8. FRONTEND: MODAL DE CAJAS
    # ==================================================================
    f.append(PageBreak())
    f.append(h1("8. Frontend: el modal de cajas"))

    f.append(h2("8.1 Los inputs que se salían de la tarjeta"))

    f.append(p(
        "Este error lo causó esta misma tanda, y es instructivo. El CSS "
        "original era:"))
    f.append(codigo("""
  .box-form input { width: 100%; }      /* MAL */
""".strip()))
    f.append(p("Y la corrección:"))
    f.append(codigo("""
  .box-form input {
      width: 100%;
      box-sizing: border-box;    /* indispensable */
  }
""".strip()))

    f.append(nota(
        "Por qué <font face='Courier'>box-sizing</font> no es opcional aquí",
        "Por defecto los <font face='Courier'>&lt;input&gt;</font> usan "
        "<font face='Courier'>content-box</font>: el <font face='Courier'>width</font> "
        "se aplica al <b>contenido</b>, y el padding y el borde se suman "
        "encima. Con 10px de padding a cada lado y 1px de borde, el input medía "
        "22px más que la tarjeta y se salía.<br/><br/>"
        "Con <font face='Courier'>border-box</font> el 100% ya incluye padding "
        "y borde. Es el mismo bug que provoca un scroll horizontal fantasma en "
        "cualquier página con un input al 100%."))

    f.append(h2("8.2 Los cuatro botones de la tabla"))

    f.append(p(
        "La celda de acciones pasó de 1 botón a 4, y los textos tienen largos muy "
        "distintos. Con un solo nombre de clase, el CSS queda corto y el CSS no "
        "avisa. Se corrigieron dos cosas:"))

    f.append(tabla(
        ["Botón", "Cuándo aparece", "Color", "Por qué ese color"],
        [
            ["Editar", "Siempre", "Azul", "Acción neutra, la de siempre"],
            ["Borrar", "Solo si <font face='Courier'>sessionsCount === 0</font>",
             "Rojo",
             "Es una eliminación real y definitiva"],
            ["Dar de baja", "Con turnos y activa", "Naranja",
             "No es borrar: el color debe distinguirlo de un borrado"],
            ["Dar de alta", "Si está dada de baja", "Verde",
             "Es el inverso de 'dar de baja' y tiene que leerse como tal"],
        ],
        [1.15 * inch, 1.95 * inch, 0.75 * inch, 2.65 * inch],
    ))
    f.append(Spacer(1, 10))

    f.append(codigo("""
  .boxes-actions {
      display: flex;
      gap: 6px;
      justify-content: flex-end;
      align-items: center;
      flex-wrap: wrap;   /* si no caben, bajan en vez de mutilarse */
      min-width: 0;      /* permite que los hijos se encogen */
  }
""".strip()))

    f.append(p(
        "<font face='Courier'>min-width: 0</font> es la parte que se olvida: sin "
        "él, el ancho mínimo de los hijos empuja la celda, la tabla se ensancha y "
        "las columnas se descuadran. Con <font face='Courier'>flex-wrap</font> "
        "las acciones bajan de línea antes que partir un texto a la mitad."))

    f.append(h2("8.3 El icono del botón Crear caja"))

    f.append(p(
        "<font face='Courier'>anadir.png</font> es una cruz <b>verde</b>. Dos "
        "decisiones que van juntas:"))
    f.append(li("<b>Sin filtro de color</b>: herechar la cruz la convertiría en "
                "un bloque blanco (el problema descrito en §7.1)."))
    f.append(li("<b>Fondo oscuro detrás del icono</b>: cruz verde sobre botón "
                "verde es invisible, así que el icono lleva un círculo oscuro "
                "que lo separa."))
    f.append(codigo("""
  .btn-confirm-open img {
      width: 16px; height: 16px;
      object-fit: contain;
      background: #0f172a;     /* separa la cruz verde del boton verde */
      border-radius: 50%;
      padding: 2px;
  }
""".strip()))

    # ==================================================================
    # 9. REPORTES EN EL FRONTEND
    # ==================================================================
    f.append(PageBreak())
    f.append(h1("9. Frontend: el historial de caja en Reportes"))

    f.append(h2("9.1 Dónde viven los dos filtros"))

    f.append(p(
        "Fue una aclaración importante del usuario y define toda la estructura:"))
    f.append(tabla(
        ["Filtro", "Dónde vive", "Por qué"],
        [
            ["Caja",
             "En la barra de filtros general de arriba",
             "Es un filtro de la consulta, como las fechas"],
            ["Usuario",
             "Dentro de la sección de caja",
             "Solo tiene sentido respecto a una caja elegida"],
        ],
        [1.15 * inch, 2.5 * inch, 2.85 * inch],
    ))
    f.append(Spacer(1, 10))

    f.append(h2("9.2 El filtro de usuario se arma con los que venden ahí"))

    f.append(codigo("""
  get boxReportUsers(): { userId: number | null; userName: string }[] {
      if (!this.cashBoxReport) return [];

      const porId = new Map<number | null, { userId, userName }>();

      for (const sesion of this.cashBoxReport.sessions) {
        for (const v of sesion.sellers) {
          porId.set(v.userId, { userId: v.userId, userName: v.userName });
        }
      }

      // Null (sin usuario) al final: es el caso excepcional, no el principal.
      return [...porId.values()].sort((a, b) => {
        if (a.userId === null) return 1;
        if (b.userId === null) return -1;
        return a.userName.localeCompare(b.userName);
      });
  }
""".strip()))

    f.append(nota(
        "Por qué NO es la lista de usuarios del sistema",
        "En un local con 8 empleados, ofrecer los 8 cuando solo 2 trabajan en "
        "esa caja produce 6 opciones que devuelven una tabla vacía. Y el usuario "
        "no puede distinguir 'sin resultados' de 'este usuario no vendió aquí'. "
        "La lista sale de los <font face='Courier'>sellers</font> que ya vienen "
        "en cada sesión, así que no cuesta nada."))

    f.append(p(
        "Una caja puede tener <b>varios</b> vendedores, y eso es lo normal: la "
        "lista los muestra todos. El <font face='Courier'>set</font> en vez de "
        "<font face='Courier'>add</font> es porque el mismo vendedor aparece en "
        "varios turnos y en el dropdown debe salir una sola vez."))

    f.append(h2("9.3 Por qué el filtro va al backend"))

    f.append(p(
        "Los filtros se envían como parámetros, no se aplican en el navegador. Si "
        "se filtrara en el frontend habría que traer todas las ventas de todos los "
        "turnos para descartar la mayoría en el cliente, y los totales de la "
        "tabla no coincidirían con los del backend. Un filtro que no coincide con "
        "los números que muestra es peor que no tenerlo."))

    f.append(codigo("""
  getCashBoxReport(boxId: number, filters: {...} = {}) {
      return this.http.get<CashBoxReportDTO>(`${this.api}/cash-box/${boxId}`, {
        params: buildParams({
          from: filters.from ?? undefined,
          to: filters.to ?? undefined,
          userId: filters.userId ?? undefined,
        }),
      });
  }
""".strip()))

    f.append(nota(
        "El detalle de <font face='Courier'>null</font> en buildParams",
        "La función que arma el query string filtraba <font face='Courier'>"
        "undefined</font> y <font face='Courier'>''</font>, pero <b>no</b> "
        "<font face='Courier'>null</font>. Un <font face='Courier'>&lt;select&gt;"
        "</font> sin opción elegida vale <font face='Courier'>null</font>, y "
        "<font face='Courier'>String(null)</font> es la cadena "
        "<font face='Courier'>\"null\"</font>, que el backend no puede convertir a "
        "Long y respondería 400. Por eso el filtro agrega "
        "<font face='Courier'>!== null</font>."))

    f.append(h2("9.4 Un turno a la vez"))

    f.append(p(
        "El detalle de las ventas se despliega con un clic en la fila, como en el "
        "detalle de renglones de Compras. Solo uno a la vez, y eso obliga a que el "
        "estado sea un id y no una lista:"))
    f.append(codigo("""
  sessionAbiertaId: number | null = null;

  toggleSesion(sessionId: number) {
      this.sessionAbiertaId =
          this.sessionAbiertaId === sessionId ? null : sessionId;
  }
""".strip()))

    f.append(p(
        "Con dos turnos abiertos a la vez la tabla duplicaría su alto y se "
        "perdería de vista de qué turno es cada bloque."))

    f.append(h2("9.5 `?? 0` en la plantilla"))

    f.append(codigo("""
  <td class="money" *ngIf="!s.filtrado"
      [class.dif-negativa]="(s.difference ?? 0) < 0">
      {{ (s.difference ?? 0) > 0 ? '+' : '' }}{{ (s.difference ?? 0) | number:'1.2-2' }}
  </td>
""".strip()))

    f.append(p(
        "Hace falta porque <font face='Courier'>difference</font> es "
        "<font face='Courier'>number | null</font> (null cuando hay filtro) y "
        "comparar <font face='Courier'>null &lt; 0</font> es un error de tipos "
        "que Angular reporta como 'Object is possibly null'. Como la celda solo "
        "se pinta cuando NO hay filtro, el 0 nunca llega a verse."))

    # ==================================================================
    # 10. USUARIOS DADOS DE BAJA
    # ==================================================================
    f.append(PageBreak())
    f.append(h1("10. Frontend: buscar en usuarios dados de baja"))

    f.append(h2("10.1 Qué se agregó"))

    f.append(p(
        "La tabla de <font face='Courier'>/usuarios-desactivados</font> no "
        "tenía nada para buscar. Se le agregaron tres filtros y dos columnas, "
        "para responder una pregunta que antes no tenía respuesta: "
        "'¿cuándo se fue esta persona?'."))

    f.append(tabla(
        ["Filtro", "Sobre qué dato", "Nota"],
        [
            ["Texto", "NOMBRE <b>y</b> CORREO",
             "Con las dos columnas: quien busca un registro muchas veces "
             "recuerda el correo"],
            ["Desde", "Fecha de baja", "Compara solo la parte yyyy-MM-dd"],
            ["Hasta", "Fecha de baja", ""],
        ],
        [1.05 * inch, 1.75 * inch, 3.7 * inch],
    ))
    f.append(Spacer(1, 10))

    f.append(h2("10.2 El detalle de las fechas (y por qué no se usan objetos Date)"))

    f.append(codigo("""
  if (this.fromDate || this.toDate) {
      if (!u.deactivatedAt) return false;   // sin fecha: no entra al rango

      const dia = u.deactivatedAt.substring(0, 10);   // "2026-10-01"
      if (this.fromDate && dia < this.fromDate) return false;
      if (this.toDate   && dia > this.toDate)   return false;
  }
""".strip()))

    f.append(nota(
        "Por qué comparar STRINGS y no objetos <font face='Courier'>Date</font>",
        "El input <font face='Courier'>type=\"date\"</font> entrega "
        "<font face='Courier'>\"2026-10-01\"</font>. El orden lexicográfico de "
        "ese formato <b>es</b> el orden cronológico, así que comparar strings "
        "funciona y evita cualquier conversión.<br/><br/>"
        "Si se usara <font face='Courier'>new Date(...)</font> la zona horaria "
        "haría que un registro del 1/10 se evaluara como 30/09 en un huso como "
        "el de México (UTC-6). Ese bug desplaza un día los bordes del rango y es "
        "de los que cuesta encontrar."))

    f.append(h2("10.3 El aviso de los usuarios sin fecha"))

    f.append(p(
        "Los usuarios dados de baja <b>antes</b> de V5 no tienen "
        "<font face='Courier'>deactivatedAt</font>: la columna se agregó después. "
        "Con un filtro de fechas quedan fuera, y sin aviso el usuario pensaría "
        "que desaparecieron. Por eso el contador:"))
    f.append(codigo("""
  get sinFecha(): number {
      return this.users.filter(u => !u.deactivatedAt).length;
  }
""".strip()))

    f.append(p(
        "El contador se calcula sobre la lista <b>completa</b>, no sobre la "
        "filtrada. Si se calculara sobre la filtrada daría siempre 0 (los que no "
        "tienen fecha ya quedaron excluidos por el filtro) y el aviso nunca "
        "aparecería. Es un detalle fácil de escribir mal."))

    f.append(h2("10.4 Los dos estados vacíos distintos"))

    f.append(codigo("""
  <tr *ngIf="!loading && users.length === 0">
    No hay usuarios dados de baja          <-- nunca hubo usuarios de baja
  <tr *ngIf="!loading && users.length > 0 && filteredUsers.length === 0">
    Ningun usuario dado de baja coincide con los filtros
""".strip()))

    f.append(p(
        "Son mensajes distintos porque son hechos distintos. 'No hay usuarios "
        "dados de baja' es un dato del sistema; 'ninguno coincide con los "
        "filtros' es un dato sobre lo que el usuario acaba de escribir, y lo que "
        "tiene que arreglar es el filtro, no el sistema."))

    # ==================================================================
    # 11. ALINEACION DE COLUMNAS
    # ==================================================================
    f.append(h1("11. Frontend: alinear las columnas del historial"))

    f.append(p(
        "El bug era sutil: los datos de las columnas de dinero iban a la derecha "
        "(clase <font face='Courier'>.money</font>) pero sus títulos iban a la "
        "izquierda. El resultado era una tabla que se veía corrida aunque los "
        "números estuvieran perfectamente alineados entre sí."))
    f.append(codigo("""
  <th class="col-id">#</th>
  <th class="col-fecha">Fecha</th>
  <th>Método de pago</th>
  <th class="money">Efectivo</th>      <!-- <- antes sin clase -->
  <th class="money">Cambio</th>
  <th class="money">Total</th>

  <td class="col-id">{{ sale.id }}</td>
  <td class="col-fecha">{{ sale.saleDate | date:'dd/MM/yyyy HH:mm' }}</td>
  <td class="money">...</td>
""".strip()))

    f.append(nota(
        "La regla que deja todo resuelto",
        "<b>Si una columna tiene los datos alineados a la derecha, su título "
        "también.</b> La clase de alineación va en el <font face='Courier'>th"
        "</font> y en el <font face='Courier'>td</font> por igual, y las dos "
        "columnas quedan sobre sus datos sin tocar nada más."))

    f.append(p(
        "Dos detalles más del mismo arreglo: <font face='Courier'>col-id</font> "
        "centra el número de ticket (es un identificador, no una cantidad, y "
        "centrado se escanea mejor en vertical), y <font face='Courier'>col-fecha"
        "</font> lleva <font face='Courier'>white-space: nowrap</font> porque "
        "'04/10/2026 14:30' partido en dos líneas hace la fila el doble de alta."))

    # ==================================================================
    # 12. VERIFICACION
    # ==================================================================
    f.append(PageBreak())
    f.append(h1("12. Cómo se verificó"))

    f.append(tabla(
        ["Qué", "Cómo", "Resultado"],
        [
            ["Backend completo", "<font face='Courier'>mvnw clean test</font>",
             "<font color='#16a34a'><b>258/258</b></font>"],
            ["Frontend completo", "<font face='Courier'>ng build</font>",
             "<font color='#16a34a'>OK</font>"],
            ["Alineación de columnas",
             "Cada <font face='Courier'>th</font> lleva la misma clase de "
             "alineación que su <font face='Courier'>td</font>",
             "Corregido"],
            ["Índices del carrito",
             "<font face='Courier'>quitarLinea(lineasIndiceBase + i)</font>",
             "Corregido antes de que se notara"],
        ],
        [1.5 * inch, 2.85 * inch, 2.15 * inch],
    ))
    f.append(Spacer(1, 10))

    f.append(h2("12.1 Qué falta probar a mano"))

    f.append(li("Vender algo y ver que el reporte de la caja muestra el nombre "
                "del usuario en el detalle del turno."))
    f.append(li("Dar de baja una caja, volver a darla de alta y comprobar que "
                "conserva sus turnos y que el número sigue ocupado."))
    f.append(li("Dar de alta una caja y filtrar en Reportes por un usuario que "
                "no vendió en ella: la tabla debe salir vacía, no con ceros."))
    f.append(li("En la tabla de sesiones, poner un filtro de fecha y ver que la "
                "columna Diferencia muestra '—' en vez de un número."))
    f.append(li("En usuarios dados de baja, filtrar por un rango de fechas y ver "
                "el aviso de los usuarios sin fecha."))

    f.append(nota(
        "Sobre los tests del frontend",
        "<font face='Courier'>ng test</font> sigue diciendo 'No test files "
        "found'. No es un defecto del código: el path del repositorio tiene "
        "paréntesis y rompen el glob de vitest. Está documentado en el "
        "<font face='Courier'>AGENTS.md</font> del frontend desde 2026-09-30. "
        "Para correrlos hay que mover el proyecto a una ruta sin paréntesis."))

    # ==================================================================
    # 13. CIERRE
    # ==================================================================
    f.append(h1("13. Lo que resume esta tanda"))

    f.append(tabla(
        ["Motivo", "Qué se hizo"],
        [
            ["'No sé quién vendió esto'",
             "La venta guarda su usuario, tomado del token"],
            ["'No sé cuándo se fue esta persona'",
             "Dos fechas en el usuario, coherentes con su estado"],
            ["'Esta caja se dio de baja y ya no vuelve'",
             "Re-alta: la misma caja, mismo número, mismo historial"],
            ["'En Reportes veo CAJA 1 diez veces'",
             "El selector lista cajas; la tabla lista sus turnos"],
            ["'No encuentro a un usuario que se fue'",
             "Filtros por nombre, correo y rango de fechas"],
            ["'La tabla se ve corrida'",
             "Cada título con la misma alineación que sus datos"],
            ["'El modal no cabe en la pantalla'",
             "El carrito se pagina y tiene scroll propio"],
        ],
        [2.6 * inch, 3.9 * inch],
    ))
    f.append(Spacer(1, 12))

    f.append(p(
        "Casi todo lo de esta tanda sale de la misma pregunta mal hecha: "
        "<b>¿qué pasa cuando algo se repite?</b> Una caja se abre cada día, una "
        "persona entra y sale del equipo varias veces, y la respuesta buena no es "
        "acotar el caso sino <b>cambiar el modelo</b> para que lo repetido no sea "
        "una excepción. La caja dejó de ser el turno (V4), el usuario dejó de ser "
        "un booleano (V5), y la venta dejó de ser un número suelto (V5)."))

    return f


def main():
    doc = SimpleDocTemplate(
        DESTINO,
        pagesize=LETTER,
        leftMargin=0.75 * inch,
        rightMargin=0.75 * inch,
        topMargin=0.7 * inch,
        bottomMargin=0.85 * inch,
        title="Venta-Usuario e Historial de Caja (V5)",
        author="Compras-Backend",
        subject="Mejoras V5: ventas por usuario, fechas de alta/baja, "
                "re-alta de caja, historial de caja con filtros",
    )
    doc.build(construir(), onFirstPage=pie, onLaterPages=pie)
    print("PDF generado:", DESTINO)
    print("Tamano:", os.path.getsize(DESTINO), "bytes")


if __name__ == "__main__":
    sys.exit(main())