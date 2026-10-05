#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Genera docs/05-Cajas-Reutilizables-V4.pdf

Documento explicativo de la refactorización V4 del módulo de caja: por qué
existieron los errores, qué clase resuelve qué, y cómo se reparan.

Ejemplo:  python docs/generar_pdf_cajas_v4.py
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
# Paleta: la misma del tema oscuro de la app, para que el PDF se lea como
# parte del proyecto y no como un documento externo.
# --------------------------------------------------------------------------
TINTA = colors.HexColor("#0f172a")
TINTA_SUAVE = colors.HexColor("#334155")
GRIS = colors.HexColor("#64748b")
AZUL = colors.HexColor("#2563eb")
AZUL_CLARO = colors.HexColor("#60a5fa")
VERDE = colors.HexColor("#16a34a")
AMARILLO = colors.HexColor("#f59e0b")
ROJO = colors.HexColor("#dc2626")
FONDO_CODIGO = colors.HexColor("#f1f5f9")
FONDO_CAJA = colors.HexColor("#fef3c7")
FONDO_BIEN = colors.HexColor("#dcfce7")

BASE = os.path.dirname(os.path.abspath(__file__))
DESTINO = os.path.join(BASE, "05-Cajas-Reutilizables-V4.pdf")


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
        "h3": crear(
            "h3", fontName="Helvetica-Bold", fontSize=10, leading=13,
            textColor=TINTA_SUAVE, spaceBefore=9, spaceAfter=4,
        ),
        "p": crear(
            "p", fontSize=9.5, leading=13.6, textColor=TINTA,
            alignment=TA_JUSTIFY, spaceAfter=7,
        ),
        "p_plano": crear(
            "p_plano", fontSize=9.5, leading=13.6, textColor=TINTA, spaceAfter=7,
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


def h3(txt):
    return Paragraph(txt, E["h3"])


def codigo(txt):
    lineas = txt.strip("\n").split("\n")
    return Paragraph("<br/>".join(l.replace(" ", "&nbsp;") for l in lineas), E["codigo"])


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
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f8fafc")]),
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


def pie(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 7.5)
    canvas.setFillColor(GRIS)
    canvas.drawString(0.75 * inch, 0.55 * inch, "Compras-Backend · Cajas reutilizables (V4)")
    canvas.drawRightString(LETTER[0] - 0.75 * inch, 0.55 * inch, f"Página {doc.page}")
    canvas.setStrokeColor(colors.HexColor("#e2e8f0"))
    canvas.line(0.75 * inch, 0.72 * inch, LETTER[0] - 0.75 * inch, 0.72 * inch)
    canvas.restoreState()


def construir():
    f = []

    # ================================================================
    # PORTADA
    # ================================================================
    f.append(p("Cajas reutilizables (V4)", "titulo"))
    f.append(p(
        "Qué es cada clase, cómo se relacionan, por qué el refactor tiro errores "
        "y cómo se repararon. Backend Spring Boot + Frontend Angular.",
        "subtitulo",
    ))

    f.append(tabla(
        ["Campo", "Contenido"],
        [
            ["Fecha", "2026-10-01"],
            ["Alcance", "Módulo de caja: <b>cash_boxes</b> (cajas físicas) y <b>cash_registers</b> (turnos)"],
            ["Motivo del cambio", "El dueño aclaró el modelo real: hay 2 cajas y se abren varios días"],
            ["Backend", "Spring Boot 4 · <font face='Courier'>com.erikjarquin.compras</font> · 258 tests en verde"],
            ["Frontend", "Angular 21 · <font face='Courier'>Compras_Frontend-Angular</font> · <font face='Courier'>ng build</font> en verde"],
            ["Verificación", "<font face='Courier'>mvnw test</font> → 258/258 · <font face='Courier'>ng build</font> → OK"],
        ],
        [1.35 * inch, 5.15 * inch],
    ))
    f.append(Spacer(1, 12))

    # ================================================================
    # 1. EL PROBLEMA
    # ================================================================
    f.append(h1("1. El problema: por qué hubo que cambiar el modelo"))

    f.append(p(
        "Hasta V3, <b>una fila de <font face='Courier'>cash_registers</font> ERA una caja</b>. "
        "Eso parecía lo correcto, pero el dueñoбарrió la idea: en el local hay dos cajas "
        "físicas (CAJA 1 y CAJA 2) y se abren y cierran <b>varios días a la semana</b>."
    ))

    f.append(h2("1.1 La consecuencia del modelo equivocado"))

    f.append(p(
        "Como el número de caja estaba declarado <font face='Courier'>UNIQUE</font> en la base, "
        "había una sola fila por caja física. Y esa fila se cerraba una sola vez. La segunda "
        "vez que se abría “CAJA 1”, el backend respondía <b>409</b> y no había ninguna forma "
        "de volver a abrir la caja que sí existía."
    ))

    f.append(nota(
        "La forma correcta de decirlo",
        "El <font face='Courier'>UNIQUE</font> no estaba mal en sí mismo: estaba en la tabla "
        "equivocada. El número identifica a la <b>caja física</b>, y ese dato casi no cambia. "
        "Lo que cambia todos los días es el <b>turno</b>. Se habían puesto las dos cosas en la "
        "misma tabla, y el número único de una terminó contradictoriamente con la "
        "reutilización de la otra."
    ))

    f.append(h2("1.2 La solución: separar los dos conceptos"))

    f.append(p(
        "V4 parte el modelo en dos tablas. Esta separación es <b>todo</b> el cambio; "
        "el resto son ajustes alrededor."
    ))

    f.append(tabla(
        ["Tabla", "Qué representa", "Cuántas filas", "Crece"],
        [
            ["<font face='Courier'>cash_boxes</font>",
             "Las cajas <b>físicas</b> del local (CAJA 1, CAJA 2)",
             "2 o 3, casi fijas",
             "No"],
            ["<font face='Courier'>cash_registers</font>",
             "Los <b>turnos</b>: una fila por cada apertura y cierre",
             "Una por día",
             "Sí, todos los días"],
        ],
        [1.5 * inch, 2.75 * inch, 1.15 * inch, 1.1 * inch],
    ))
    f.append(Spacer(1, 8))

    f.append(p(
        "<font face='Courier'>cash_registers</font> sigue siendo la tabla que enlaza con "
        "<font face='Courier'>sales.cash_register_id</font>. Por eso <b>no hubo que tocar</b> "
        "<font face='Courier'>SaleEntity</font> ni los reportes de ventas: las ventas siguen "
        "apuntando a un turno, y un turno sigue apuntando a una caja."
    ))

    f.append(h2("1.3 La decisión de diseño más importante"))

    f.append(p(
        "<font face='Courier'>cash_registers.number</font> deja de ser UNIQUE y pasa a ser una "
        "<b>copia histórica</b> del número de la caja al momento de abrir. Se conserva a "
        "propósito, por dos razones:"
    ))
    f.append(li(
        "El reporte “filtrar por caja” sigue funcionando <b>sin JOIN</b>: el número ya está "
        "en la fila del turno."
    ))
    f.append(li(
        "Si mañana renombran “CAJA 1” a “CAJA PRINCIPAL”, los cortes antiguos deben seguir "
        "diciendo “CAJA 1”. Es la evidencia de cómo se llamaba entonces. Es histórico, y por "
        "diseño lo es."
    ))

    # ================================================================
    # 2. DIAGRAMA
    # ================================================================
    f.append(PageBreak())
    f.append(h1("2. Cómo se relacionan las clases"))

    f.append(h2("2.1 El mapa completo"))

    f.append(codigo(r"""
  cash_boxes                          cash_registers
  (CashBoxEntity)                     (CashRegisterEntity)
  ------------                         --------------------
  id            PK                    id              PK
  number        UNIQUE  <----------.  number           (copia historica, ya NO unique)
  description                         cash_box_id  FK --'
  active                             opened_at / closed_at / active
  created_at                          opening_amount / expected_amount
  updated_at                          difference / difference_reason
                                      cash_sales / debit_sales / credit_sales / total_sales
                                      total_tickets
        ^                                          ^
        |                                          |
        |  CashBoxEntity                           |
        +-- CashRegisterEntity.cashBox             |
             (ManyToOne, LAZY)                     |
                                                    |
  CashBoxRepository  <--- una caja, N turnos -------+   CashRegisterRepository
  (pocas filas)            (muchos)                    (crece cada dia)
""".strip()))

    f.append(p(
        "La flecha va de <font face='Courier'>cash_registers.cash_box_id</font> hacia "
        "<font face='Courier'>cash_boxes</font>. Está en el lado del turno a propósito: "
        "una caja <b>tiene</b> muchos turnos, un turno <b>pertenece a</b> una caja. La "
        "relación se lee LAZY porque al listar el historial solo hace falta el número, y "
        "ya está copiado en el turno: un SELECT extra por fila sería un N+1."
    ))

    f.append(h2("2.2 Qué clase hace qué"))

    f.append(tabla(
        ["Clase", "Responsabilidad", "Por qué existe"],
        [
            ["<font face='Courier'>CashBoxEntity</font>",
             "El inventario de cajas físicas: número, descripción, activa.",
             "NUEVA en V4. Antes no existía; cada corte era “una caja”."],
            ["<font face='Courier'>CashRegisterEntity</font>",
             "Un turno: se abre, se venden cosas, se cierra y se congela.",
             "Ya existía. En V4 gana la FK <font face='Courier'>cashBox</font>."],
            ["<font face='Courier'>CashBoxRepository</font>",
             "Consultas de las cajas: por número, todas, las que se pueden abrir.",
             "NUEVA. Distinta de la otra porque manejan cosas distintas."],
            ["<font face='Courier'>CashRegisterRepository</font>",
             "Consultas de los turnos: el abierto, el historial, los de una caja.",
             "Ya existía. Se le agregan las consultas por caja."],
            ["<font face='Courier'>CashRegisterService</font>",
             "El CONTRATO: qué se puede hacer con cajas y con turnos.",
             "Separa “qué” de “cómo”. La interfaz no sabe nada de JPA."],
            ["<font face='Courier'>CashRegisterImpl</font>",
             "La LÓGICA: validaciones, el 409 del cuadre, el nuevo/open.",
             "Donde viven todas las reglas de negocio del módulo."],
            ["<font face='Courier'>CashRegisterController</font>",
             "Los endpoints y el permiso de cada uno.",
             "Traduce HTTP a llamadas del service."],
            ["<font face='Courier'>CashBoxRequest</font>",
             "El cuerpo de alta/edición: número y descripción.",
             "DTO de entrada. El <font face='Courier'>CashBoxResponse</font> es la salida."],
            ["<font face='Courier'>CashBoxResponse</font>",
             "Una caja con 3 datos extra: turnos, en uso, último turno.",
             "Los calcula <font face='Courier'>toBoxResponse()</font> en el service."],
        ],
        [1.75 * inch, 2.6 * inch, 2.15 * inch],
    ))
    f.append(Spacer(1, 8))

    f.append(nota(
        "Clases que probablemente te faltaban",
        "En el punto 6 tu lista mencionaba los DTO y los repositorios, pero no un mapper. "
        "Y no hace falta uno nuevo: el mapper existente (<font face='Courier'>"
        "CashRegisterMapper</font>) ya sabe convertir un <font face='Courier'>"
        "CashRegisterEntity</font> en su DTO, y eso <b>no cambió</b> en V4. Lo que sí es "
        "nuevo es <font face='Courier'>toBoxResponse()</font>, que es un método privado "
        "dentro de <font face='Courier'>CashRegisterImpl</font>, no un mapper. La razón "
        "está en §5.2."
    ))

    # ================================================================
    # 3. FLUJO
    # ================================================================
    f.append(h1("3. El flujo de abrir y cerrar, paso a paso"))

    f.append(tabla(
        ["#", "Acción del cajero", "Endpoint", "Qué hace el backend"],
        [
            ["1", "“Ver cajas”",
             "<font face='Courier'>GET /cash/boxes</font>",
             "Lista las cajas, incluidas las dadas de baja, con su número de turnos."],
            ["2", "“+ Crear caja”",
             "<font face='Courier'>POST /cash/boxes</font>",
             "Registra la caja. 409 si el número ya existe."],
            ["3", "“Abrir caja”",
             "<font face='Courier'>GET /cash/boxes/openable</font>",
             "Cajas activas <b>sin turno abierto</b>. Alimenta el selector."],
            ["4", "Elegir caja + monto",
             "<font face='Courier'>POST /cash/open</font>",
             "Crea un <b>turno nuevo</b> en <font face='Courier'>cash_registers</font> "
             "apuntando a esa caja. La misma caja puede abrirse mil veces."],
            ["5", "Vender",
             "<font face='Courier'>POST /sales</font>",
             "Cada venta guarda el <font face='Courier'>cashRegisterId</font> del turno."],
            ["6", "Cerrar",
             "<font face='Courier'>POST /cash/close</font>",
             "Congela el corte. Exige cuadrar, o un motivo escrito."],
        ],
        [0.3 * inch, 1.15 * inch, 1.85 * inch, 3.2 * inch],
    ))
    f.append(Spacer(1, 10))

    f.append(h2("3.1 El filtro “qué cajas se pueden abrir”"))

    f.append(p(
        "Este es el cambio más sutil de todos, y el que más sese heredó por error. "
        "En V3 la consulta era “cajas que <b>nunca</b> se abrieron”, implementada como "
        "<font face='Courier'>openedAt IS NULL</font>. Era correcta para el modelo viejo. "
        "Al trasplantarla a V4, <b>se vuelve un bug</b>: como una caja ahora se abre todos los días, "
        "ese filtro la excluiría para siempre después de su primer turno."
    ))

    f.append(p(
        "V4 lo cambia por la pregunta correcta: <b>¿tiene algún turno abierto ahora "
        "mismo?</b>"
    ))

    f.append(codigo(r"""
  -- CashBoxRepository.findOpenable()
  SELECT b FROM CashBoxEntity b
  WHERE b.active = true
    AND NOT EXISTS (
      SELECT 1 FROM CashRegisterEntity r
      WHERE r.cashBox.id = b.id AND r.active = true
    )
  ORDER BY b.number ASC
""".strip()))

    f.append(nota(
        "Por qué el NOT EXISTS y no un filtro en Java",
        "Filtrar en Java (“trae todas y quédate con las que no tienen turno abierto”) "
        "significaría traer <b>todos los turnos de todas las cajas</b> para descartar la "
        "mayoría. El <font face='Courier'>NOT EXISTS</font> deja que lo haga la base y solo "
        "trae las cajas libres."
    ))

    # ================================================================
    # 4. LOS ERRORES
    # ================================================================
    f.append(PageBreak())
    f.append(h1("4. Los errores que aparecieron y por qué"))

    f.append(p(
        "Al terminar el punto 6 la compilación fallaba. Estos son los errores, la causa "
        "real de cada uno, y cómo se corrigieron. Los cuatro primeros son los que te "
        "detuvieron; los siguientes los encontré al revisar."
    ))

    # --- 4.1
    f.append(h2("4.1 <font color='#dc2626'>illegal start of type</font> — el bloqueo"))

    f.append(p("<b>Síntoma:</b> la compilación se caía en 2 líneas y no avanzaba."))
    f.append(codigo(r"""
  CashRegisterController.java:[67,79] illegal start of type
  CashRegisterController.java:[74,79] illegal start of type
""".strip()))

    f.append(p("<b>Causa:</b> un punto y coma de más, después de la anotación."))
    f.append(codigo(r"""
  // MAL: el ; corta la declaración de la anotación
  @PreAuthorize("hasAuthority('EDITAR_CAJA' or hasAuthority('ABRIR_CAJA'))");
  @PutMapping("/boxes/{id}")

  // BIEN
  @PreAuthorize("hasAuthority('ABRIR_CAJA')")
  @PutMapping("/boxes/{id}")
""".strip()))
    f.append(p(
        "En Java una anotación es una declaración y lleva punto y coma implícito; un "
        "<font face='Courier'>;</font> explícito la convierte en una sentencia completa, y "
        "la línea siguiente aparece como código suelto. Por eso el error dice "
        "<font face='Courier'>illegal start of type</font> (“comienzo de tipo inválido”) "
        "en la línea <b>siguiente</b> a la anotación, que es donde el compilador se "
        "confunde: no señala la causa, señala el síntoma."
    ))

    f.append(nota(
        "Un detalle que volverá a saltar",
        "El permiso <font face='Courier'>EDITAR_CAJA</font> que usabas en esas dos "
        "anotaciones <b>no existe</b> en <font face='Courier'>PermissionName</font>. "
        "Aunque el punto y coma estuviera bien, la app habría arrancado y "
        "<font face='Courier'>@PreAuthorize</font> habría rechazado todas las peticiones "
        "con 403. Se cambió a <font face='Courier'>ABRIR_CAJA</font>, que es el permiso "
        "de la persona que administra las cajas del local."
    ))

    # --- 4.2
    f.append(h2("4.2 <font color='#dc2626'>getBoxHistory</font> con la condición al revés"))

    f.append(p("<b>El bug más caro de los seis.</b> El código estaba así:"))
    f.append(codigo(r"""
  // MAL: si la caja EXISTE, lanza "no existe"
  if(cashBoxRepository.existsById(boxId)){
      throw new CashException("La caja no existe", HttpStatus.NOT_FOUND);
  }
""".strip()))
    f.append(p("Lo correcto es al revés:"))
    f.append(codigo(r"""
  // BIEN: si NO existe, no hay historial que devolver
  if(!cashBoxRepository.existsById(boxId)){
      throw new CashException("La caja no existe", HttpStatus.NOT_FOUND);
  }
""".strip()))

    f.append(p(
        "<b>Por qué es el más caro:</b> el método <b>siempre</b> lanzaba 404. "
        "Para cualquier caja que existiera, el historial era inalcanzable. "
        "No daba error de compilación y no lo daba ni de ejecución: el tests no lo "
        "cubrían porque aún no existía. UnTests que pasa sin comprobar nada es "
        "peor que ningún test."
    ))

    f.append(p(
        "<b>Por qué se coló:</b> es un error de lectura, no de escritura. El <font "
        "face='Courier'>!</font> es un carácter que el ojo se salta cuando ya estás "
        "pensando en el modelo de datos en vez de en la condición. Por eso el "
        "documento del punto 6 traía el método bien y lo que se escribió al teclearlo "
        "perdió la negación."
    ))

    # --- 4.3
    f.append(h2("4.3 <font color='#dc2626'>getOpenable</font> devolvía el tipo equivocado"))

    f.append(codigo(r"""
  // MAL en el controller: el DTO de ENTRADA como tipo de retorno
  public List<CashBoxRequest> getOpenable(){ return service.getOpenable(); }

  // BIEN
  public List<CashBoxResponse> getOpenable(){ return service.getOpenable(); }
""".strip()))
    f.append(p(
        "<b>Por qué importa:</b> <font face='Courier'>CashBoxRequest</font> es lo que "
        "<b>entra</b> (número y descripción) y <font face='Courier'>CashBoxResponse</font> "
        "es lo que <b>sale</b> (con turnos, en uso, último turno). Devolver el de entrada "
        "significaba que el selector de “Abrir caja” recibiría objetos sin "
        "<font face='Courier'>sessionsCount</font> ni <font face='Courier'>inUse</font>, y "
        "la tabla de cajas no podría mostrar nada. Compilaba porque los dos DTOs tienen "
        "campos compatibles: el error es de <b>contrato</b>, no de tipos."
    ))

    # --- 4.4
    f.append(h2("4.4 <font color='#dc2626'>Faltaban metodos por implementar</font>"))

    f.append(p(
        "La interfaz <font face='Courier'>CashRegisterService</font> declaraba "
        "<font face='Courier'>getBoxes()</font> y <font face='Courier'>updateBox()</font>, "
        "pero <font face='Courier'>CashRegisterImpl</font> no los tenía. Java avisa de esto "
        "con un error claro (“no es abstracto y no sobrescribe”), que es de los pocos casos "
        "en que el compilador te salva. Se implementaron."
    ))

    # --- 4.5
    f.append(PageBreak())
    f.append(h2("4.5 <font color='#dc2626'>La app no arrancaba: PropertyReferenceException</font>"))

    f.append(p(
        "Esta no daba error de compilación. <font face='Courier'>mvn test</font> lo "
        "reveló con 5 errores en <font face='Courier'>ProductRepositoryTest</font>:"
    ))
    f.append(codigo(r"""
  Error creating bean with name 'cashBoxRepository':
  No property 'cashRegistersId' found for type 'CashBoxEntity'
""".strip()))

    f.append(p("<b>Causa:</b> el método <font face='Courier'>existsByCashRegistersId</font> "
               "es un nombre <i>derivado</i>: Spring Data deduce el JPQL a partir del nombre. "
               "Para <font face='Courier'>existsBy</font> + <font face='Courier'>CashRegistersId</font> "
               "busca una propiedad <font face='Courier'>cashRegisters</font> dentro de "
               "<font face='Courier'>CashBoxEntity</font>… y no existe."))

    f.append(p("<b>Por qué no existe:</b> porque la relación está al revés. "
               "<font face='Courier'>CashBoxEntity</font> <b>no</b> tiene una lista de "
               "sesiones: la referencia vive en <font face='Courier'>CashRegisterEntity"
               ".cashBox</font>. Es una relación de un lado a muchos, y el lado uno no "
               "necesita saber de su colección."))

    f.append(p("<b>La corrección:</b> escribir la consulta a mano. Es el caso donde el nombre "
               "derivado no puede expresar la pregunta, porque la pregunta es “existe algún "
               "turno de <i>esta</i> caja”, y eso se pregunta desde el turno."))
    f.append(codigo('''
  @Query("""
          SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
          FROM CashRegisterEntity r
          WHERE r.cashBox.id = :cashBoxId
          """)
  boolean hasSessions(@Param("cashBoxId") Long cashBoxId);
'''.strip()))

    f.append(nota(
        "La lección",
        "Los nombres derivados de Spring Data solo funcionan si la propiedad existe en la "
        "entidad <b>de la que se parte</b>. Cuando la relación es de un lado a muchos, "
        "todas las preguntas se hacen desde el lado muchos, porque es el que tiene la FK. "
        "Si el nombre derivado no existe, la aplicación <b>no arranca</b>: no es una "
        "excepción en runtime, es un fallo de contexto de Spring."
    ))

    # --- 4.6
    f.append(h2("4.6 <font color='#dc2626'>El bug de dinero: el fondo contado dos veces</font>"))

    f.append(p("Este no daba ningún error. Era el más peligroso de todos."))
    f.append(codigo(r"""
  // MAL: sembraba el fondo en las ventas de efectivo
  sesion.setCashSales(openingAmount);      // <-- 500
  sesion.setOpeningAmount(openingAmount);   // <-- 500
  sesion.setExpectedAmount(openingAmount);
""".strip()))

    f.append(p(
        "El resumen del corte calcula <font face='Courier'>expectedAmount = openingAmount "
        "+ cashSales</font>. Con el fondo en los dos lados, el esperado daba "
        "<b>1000 en vez de 500</b>."
    ))

    f.append(tabla(
        ["", "Fondo", "Ventas en efectivo", "Esperado"],
        [
            ["Correcto", "500", "0", "<b>500</b>"],
            ["Con el bug", "500", "500", "<b>1000</b>"],
        ],
        [1.5 * inch, 1.2 * inch, 1.9 * inch, 1.4 * inch],
    ))
    f.append(Spacer(1, 8))

    f.append(p(
        "El cajero abría la caja con 500, no vendía nada, y al cerrar contaba 500. El "
        "sistema le decía “faltan 500”. O cerraba con 1000 y le aceptaba el corte, "
        "guardando como dinero del negocio 500 que nunca existieron. "
        "<b>Cualquiera de las dos opciones destruye la confianza en el sistema de caja.</b>"
    ))

    f.append(codigo(r"""
  // BIEN: los acumulados NACEN en cero. El fondo no es una venta.
  sesion.setCashSales(BigDecimal.ZERO);
  sesion.setDebitSales(BigDecimal.ZERO);
  sesion.setCreditSales(BigDecimal.ZERO);
  sesion.setTotalSales(BigDecimal.ZERO);
  sesion.setOpeningAmount(openingAmount);
  sesion.setExpectedAmount(openingAmount);
""".strip()))

    f.append(nota(
        "Por qué un test es la única defensa aquí",
        "Este bug no se manifiesta en ninguna respuesta de la API: el turno se abre bien, "
        "el resumen se ve bien, y el error solo aparece al <b>cerrar</b>, después de muchas "
        "horas de venta. Un test que verifica “el turno abre con "
        "<font face='Courier'>cashSales = 0</font> y <font face='Courier'>openingAmount = "
        "500</font>” lo atraparía en un segundo. Es el test "
        "<font face='Courier'>acumuladosArrancanEnCero</font>, y por eso se escribió."
    ))

    # --- 4.7
    f.append(h2("4.7 <font color='#dc2626'>desactiveBox</font> borraba en vez de dar de baja"))

    f.append(codigo(r"""
  // MAL: el método se llamaba "desactivar" y hacía un delete
  @Override
  public void desactiveBox(Long id){
      ...
      cashBoxRepository.delete(cash);      // <-- BORRABA la caja
  }
""".strip()))

    f.append(p(
        "El nombre del método y lo que hacía no coincidían. Peor: la condición que lo "
        "acompañaba era una <b>trampa</b>:"
    ))
    f.append(codigo(r"""
  if(cashBoxRepository.existsByCashRegistersId(id)){
      throw new CashException("...no se puede borrar. Dala de baja.", CONFLICT);
  }
  cashBoxRepository.delete(cash);
""".strip()))

    f.append(p(
        "Decía “si tiene cortes, no borres”. La intención era buena, pero el resultado era "
        "el <b>contrario</b> de lo que se pidió: las cajas <b>con</b> cortes (las que hay "
        "que proteger) se borraban sin nada, y las que no tenían cortes eran las únicas "
        "que se bloqueaban. La condición estaba puesta al revés respecto a la regla."
    ))

    f.append(p("V4 lo separa en dos métodos con nombres distintos y responsabilidades distintas:"))
    f.append(li("<font face='Courier'>desactiveBox(id)</font> → <b>siempre</b> desactiva "
                "(<font face='Courier'>active = false</font>). Nunca borra. Es reversible."))
    f.append(li("<font face='Courier'>deleteBox(id)</font> → borra, pero <b>solo</b> si "
                "<font face='Courier'>hasSessions(id)</font> es falso. Con turnos: 409 "
                "diciendo “dala de baja”."))

    # --- 4.8
    f.append(h2("4.8 Los errores del frontend"))

    f.append(p(
        "En Angular los errores son distintos en naturaleza: TypeScript los detecta al "
        "compilar, pero los <i>typos</i> en nombres de propiedad no los detecta, y esos "
        "son los que rompen la pantalla."
    ))

    f.append(tabla(
        ["Qué estaba mal", "Cómo se manifestsó", "Corrección"],
        [
            ["<font face='Courier'>boxForm = {number: \", description: \"}</font>",
             "Objeto mal formado. El formulario no arrancaba.",
             "<font face='Courier'>{ number: '', description: '' }</font> con tipo <font face='Courier'>CashBoxForm</font>"],
            ["<font face='Courier'>OpenableBoxes</font> (mayúscula)",
             "La plantilla pedía <font face='Courier'>openableBoxes</font>: siempre vacío, "
             "el selector de cajas salía en blanco.",
             "Todo en minúscula: <font face='Courier'>openableBoxes</font>"],
            ["<font face='Courier'>showCreateBoxForm</font> no existía",
             "La variable sí se usaba pero nunca se declaraba: error de compilación TS.",
             "Se declaró <font face='Courier'>showBoxForm</font>"],
            ["<font face='Courier'>descrption</font> en el DTO",
             "TypeScript no marca los typos en propiedades opcionales: la descripción "
             "llegaba siempre <font face='Courier'>undefined</font>.",
             "<font face='Courier'>description</font>. Se agregó la interfaz "
             "<font face='Courier'>CashBoxForm</font> para que el error saliera antes."],
            ["<font face='Courier'>*ngIf</font> junto a <font face='Courier'>*hasPermission</font>",
             "La directiva de permiso es <b>estructural</b>: dos directivas estructurales "
             "en un elemento no pueden convivir.",
             "Se envuelve con <font face='Courier'>&lt;ng-container *hasPermission&gt;</font> "
             "y el <font face='Courier'>*ngIf</font> va en el botón de adentro."],
        ],
        [1.85 * inch, 2.55 * inch, 2.1 * inch],
    ))
    f.append(Spacer(1, 8))

    f.append(nota(
        "El patrón de <font face='Courier'>*ngIf</font> + <font face='Courier'>*hasPermission</font>",
        "No es un capricho del framework: es una regla dura. "
        "<font face='Courier'>*hasPermission</font> manipula el "
        "<font face='Courier'>TemplateRef</font> y el <font face='Courier'>ViewContainer</font>, "
        "o sea que es estructural como <font face='Courier'>*ngIf</font>. Angular no admite "
        "dos directivas estructurales en el mismo elemento y lo rechaza en compilación. "
        "Este proyecto ya lo tenía documentado en AGENTS.md; la solución es el "
        "<font face='Courier'>&lt;ng-container&gt;</font>."
    ))

    # ================================================================
    # 5. LA REGLA
    # ================================================================
    f.append(PageBreak())
    f.append(h1("5. La regla que pediste: borrar o dar de baja"))

    f.append(p(
        "La regla quedó implementada de los dos lados, con el backend haciendo la "
        "decisión y el frontend reflejándola."
    ))

    f.append(h2("5.1 La regla"))

    f.append(tabla(
        ["Estado de la caja", "Se puede BORRAR", "Se puede DAR DE BAJA", "Por qué"],
        [
            ["Nunca se abrió (<font face='Courier'>sessionsCount = 0</font>)",
             "<font color='#16a34a'><b>Sí</b></font>",
             "<font color='#16a34a'><b>Sí</b></font>",
             "No hay ventas colgando. Borrarla es limpio."],
            ["Ya tuvo cortes (<font face='Courier'>sessionsCount &gt; 0</font>)",
             "<font color='#dc2626'><b>No</b></font> (409)",
             "<font color='#f59e0b'><b>Sí</b></font>",
             "Sus ventas quedan colgando de ella. Borrarla hace inalcanzable el corte."],
            ["Turno abierto ahora mismo",
             "<font color='#dc2626'><b>No</b></font> (409)",
             "<font color='#dc2626'><b>No</b></font> (409)",
             "El cajero está contando ese dinero. No se puede tocar nada."],
        ],
        [1.9 * inch, 1.05 * inch, 1.15 * inch, 2.4 * inch],
    ))
    f.append(Spacer(1, 10))

    f.append(h2("5.2 Por qué el borrado condicional y no “solo dar de baja”"))

    f.append(p(
        "La alternativa más simple era no exponer borrado físico nunca: todo se da de "
        "baja. Es más segura, pero deja basura (cajas creadas por error que se acumulan "
        "para siempre, sin efecto pero sin salida). Con la regla condicional:"
    ))
    f.append(li("Una caja que nunca se abrió <b>no ensucia</b>: se borra limpio."))
    f.append(li("Una caja con historial <b>no se pierde</b>: el corte sigue consultable."))
    f.append(li("El usuario nunca ve un botón que no puede cumplir, porque el template "
                "muestra “Borrar” solo con <font face='Courier'>sessionsCount = 0</font>."))
    f.append(p(
        "Y el borrado físico sigue siendo una <b>operación sin consecuencias</b>: no toca "
        "dinero, no toca ventas, no toca el historial. Eso es lo que la hace aceptable "
        "en el único caso en que se permite."
    ))

    f.append(h2("5.3 Por qué en usuarios NO conviene (y por qué es distinto)"))

    f.append(p(
        "Tu pregunta era si conviene aplicar lo mismo a usuarios. <b>La respuesta es "
        "no, y la razón es que la base de la regla es diferente.</b>"
    ))

    f.append(p(
        "La regla de la caja protege <b>ventas</b>: cada venta apunta a un turno, y el "
        "turno a la caja. Romper ese encadenamiento deja ventas sin origen, y una venta "
        "sin origen es un número suelto que no se puede auditar."
    ))

    f.append(p(
        "Los usuarios <b>no</b> tienen esa cadena. Un usuario no apunta a ventas: las "
        "ventas apuntan al <i>turno</i>, y el turno no apunta a un usuario. Un usuario "
        "dado de baja deja ventas hechas que siguen ahí, y eso es exactamente lo que "
        "debe pasar: el ticket de marzo lo emitió una persona que hoy no trabaja más "
        "allá. <b>El historial de ventas es de la tienda, no del empleado.</b>"
    ))

    f.append(tabla(
        ["", "Cajas", "Usuarios"],
        [
            ["Qué protege el historial",
             "Ventas (cada una apunta a un turno)",
             "Nada: las ventas no le pertenecen al usuario"],
            ["Referencia desde otras tablas",
             "<font face='Courier'>sales → cash_register → cash_box</font>",
             "<font face='Courier'>users → role</font> (no desde ventas)"],
            ["Si se borra duro",
             "Ventas huérfanas: el corte se pierde",
             "Se pierde quién vendió, pero no cuánto se vendió"],
            ["Recomendación",
             "Borrar condicional + baja",
             "<b>Solo baja.</b> Borrar duro no aporta nada"],
        ],
        [1.55 * inch, 2.35 * inch, 2.6 * inch],
    ))
    f.append(Spacer(1, 10))

    f.append(p(
        "Aun así, <b>hoy los usuarios solo se dan de baja</b>, y eso es correcto. El "
        "endpoint <font face='Courier'>DELETE /users/{id}</font> no borra: activa "
        "<font face='Courier'>active = false</font>. Es el mismo criterio de "
        "productos, y coincide con lo que ya hace la app."
    ))

    f.append(nota(
        "El único caso donde borraría un usuario",
        "Un usuario que <b>nunca abrió sesión</b> (se creó por error y nadie entró). Ahí sí "
        "se puede borrar sin pérdida, con la misma regla condicional de las cajas. Es "
        "coherente con lo que ya hace productos con "
        "<font face='Courier'>hasHistory</font>: si nunca hizo nada, “Eliminar”; si hizo "
        "algo, “Dar de baja”. Si lo quieres, es un cambio pequeño y con la misma forma "
        "que acabamos de hacer en cajas."
    ))

    # ================================================================
    # 6. ENDPOINTS
    # ================================================================
    f.append(PageBreak())
    f.append(h1("6. Los endpoints después del cambio"))

    f.append(tabla(
        ["Endpoint", "Permiso", "Devuelve", "Nota"],
        [
            ["<font face='Courier'>GET /cash/boxes</font>", "VER_CAJA",
             "<font face='Courier'>CashBox[]</font>",
             "Todas, incluidas dadas de baja"],
            ["<font face='Courier'>GET /cash/boxes/openable</font>", "ABRIR_CAJA",
             "<font face='Courier'>CashBox[]</font>",
             "Activas y sin turno abierto"],
            ["<font face='Courier'>POST /cash/boxes</font>", "ABRIR_CAJA",
             "<font face='Courier'>CashBox</font>",
             "409 si el número existe"],
            ["<font face='Courier'>PUT /cash/boxes/{id}</font>", "ABRIR_CAJA",
             "<font face='Courier'>CashBox</font>",
             "No da 409 si se guarda el mismo número"],
            ["<font face='Courier'>PATCH /cash/boxes/{id}</font>", "ABRIR_CAJA",
             "<font face='Courier'>204</font>",
             "Dar de baja. Nunca borra"],
            ["<font face='Courier'>DELETE /cash/boxes/{id}</font>", "ABRIR_CAJA",
             "<font face='Courier'>204</font>",
             "Solo si nunca se abrió; si no, 409"],
            ["<font face='Courier'>GET /cash/boxes/{id}/history</font>", "VER_CAJA",
             "<font face='Courier'>CashResponse[]</font>",
             "Un corte por turno"],
            ["<font face='Courier'>POST /cash/open</font>", "ABRIR_CAJA",
             "<font face='Courier'>CashResponse</font>",
             "Crea un turno nuevo"],
            ["<font face='Courier'>POST /cash/close</font>", "CERRAR_CAJA",
             "<font face='Courier'>CashResponse</font>",
             "409 si no cuadra y no hay motivo"],
            ["<font face='Courier'>GET /cash/next-number</font>", "ABRIR_CAJA",
             "<font face='Courier'>NextNumber</font>",
             "Sugerencia: cuenta sobre las cajas"],
        ],
        [2.15 * inch, 1.0 * inch, 1.35 * inch, 2.0 * inch],
    ))
    f.append(Spacer(1, 8))

    f.append(p(
        "<b>Se eliminaron</b> dos endpoints de V3, que ya no significaban nada: "
        "<font face='Courier'>POST /cash</font> (creaba un corte vacío) y "
        "<font face='Courier'>GET /cash/available</font> (cajas nunca abiertas). "
        "El segundo era especialmente peligroso: su criterio "
        "(<font face='Courier'>openedAt IS NULL</font>) era correcto en V3 pero en V4 "
        "significaba “cajas que nunca se abrieron”, que después del primer turno de cada "
        "caja no sería ninguna."
    ))

    # ================================================================
    # 7. CHECKLIST
    # ================================================================
    f.append(h1("7. Lo que quedó hecho y cómo se verifica"))

    f.append(tabla(
        ["Área", "Cambio", "Estado"],
        [
            ["<font face='Courier'>V4__cajas_reutilizables.sql</font>",
             "Crea <font face='Courier'>cash_boxes</font>, migra los datos, "
             "suelta el UNIQUE, agrega la FK",
             "<font color='#16a34a'>Hecho</font>"],
            ["Entidad",
             "<font face='Courier'>CashBoxEntity</font> nueva; "
             "<font face='Courier'>CashRegisterEntity</font> gana <font face='Courier'>cashBox</font>",
             "<font color='#16a34a'>Hecho</font>"],
            ["Repositorios",
             "<font face='Courier'>CashBoxRepository</font> nuevo; consultas por caja",
             "<font color='#16a34a'>Hecho</font>"],
            ["DTOs",
             "<font face='Courier'>CashBoxRequest</font> y <font face='Courier'>CashBoxResponse</font>",
             "<font color='#16a34a'>Hecho</font>"],
            ["Service",
             "<font face='Courier'>open</font> reescrito; CRUD de cajas; "
             "<font face='Courier'>desactiveBox</font> y <font face='Courier'>deleteBox</font>",
             "<font color='#16a34a'>Hecho</font>"],
            ["Controller",
             "6 endpoints de cajas; 2 de V3 eliminados",
             "<font color='#16a34a'>Hecho</font>"],
            ["Tests",
             "Reescritos de V3 a V4; +tests de reapertura, borrado condicional y acumulados en cero",
             "<font color='#16a34a'>258/258</font>"],
            ["Frontend",
             "Interfaces, service, facade y el modal “Ver cajas” con su CSS",
             "<font color='#16a34a'><font face='Courier'>ng build</font> OK</font>"],
        ],
        [1.75 * inch, 3.3 * inch, 1.45 * inch],
    ))
    f.append(Spacer(1, 10))

    f.append(h2("7.1 Cómo verificarlo a mano"))

    f.append(li("Registrar “CAJA 1” desde “Ver cajas”. Debe quedar con 0 turnos."))
    f.append(li("Abrir la caja con 500. Vender algo en efectivo, por 200."))
    f.append(li("Intentar borrar “CAJA 1”: debe salir el 409 con “dala de baja”, y "
                "en la tabla el botón debe ser “Dar de baja”, no “Borrar”."))
    f.append(li("Cerrar con 700: cuadra exacto, no guarda motivo. Con 650: 409 y "
                "pide motivo; con motivo, cierra y guarda la diferencia de −50."))
    f.append(li("Volver a abrir “CAJA 1” al día siguiente: debe permitirlo. "
                "El selector la ofrece porque no tiene turno abierto."))
    f.append(li("En “Ver cajas”, CAJA 1 debe mostrar 2 turnos. Es la prueba visible "
                "de que el modelo nuevo funciona."))

    f.append(nota(
        "Si el frontend no encuentra los tests",
        "<font face='Courier'>ng test</font> sigue diciendo “No test files found”. No es "
        "un defecto del código: el path del repo tiene paréntesis "
        "(<font face='Courier'>Sistema de ventas (comida)</font>) y rompen el glob de "
        "vitest. Está documentado en el <font face='Courier'>AGENTS.md</font> del frontend "
        "desde 2026-09-30. Para correrlos, hay que mover el proyecto a una ruta sin "
        "paréntesis."
    ))

    # ================================================================
    # 8. CIERRE
    # ================================================================
    f.append(h1("8. En una frase"))

    f.append(p(
        "V4 no agregó una función nueva: <b>arregló un error de modelo</b>. La app decía "
        "tener una caja por fila de corte, cuando en un negocio real hay dos cajas y "
        "muchos turnos. Separar <font face='Courier'>cash_boxes</font> de "
        "<font face='Courier'>cash_registers</font> hizo que las cosas que son distintas "
        "dejeran de pelear entre sí: el número único pasó a estar donde corresponde "
        "(en las cajas), y el crecimiento diario pasó a estar en la tabla que crece "
        "(los turnos)."
    ))

    f.append(p(
        "Los seis errores no fueron seis descuidos. Cinco fueron consecuencia directa de "
        "cambiar un modelo que dos clases habían Fingido ser el mismo: un <font "
        "face='Courier'>!</font> perdido, un nombre derivado que apuntaba al lado "
        "equivocado de la relación, un fondo sembrado en dos columnas. Y el sexto —el "
        "que no daba ningún error— era el peor de todos, porque un corte de caja que "
        "cuadra mal es un sistema que miente sobre el dinero."
    ))

    return f


def main():
    doc = SimpleDocTemplate(
        DESTINO,
        pagesize=LETTER,
        leftMargin=0.75 * inch,
        rightMargin=0.75 * inch,
        topMargin=0.7 * inch,
        bottomMargin=0.85 * inch,
        title="Cajas reutilizables (V4)",
        author="Compras-Backend",
        subject="Explicacion de la refactorizacion V4 del modulo de caja",
    )
    doc.build(construir(), onFirstPage=pie, onLaterPages=pie)
    print("PDF generado:", DESTINO)
    print("Tamaño:", os.path.getsize(DESTINO), "bytes")


if __name__ == "__main__":
    sys.exit(main())