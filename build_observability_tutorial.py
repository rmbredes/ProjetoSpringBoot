from pathlib import Path
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm, mm
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfgen import canvas
from reportlab.platypus import (
    BaseDocTemplate,
    Flowable,
    Frame,
    HRFlowable,
    KeepTogether,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)


ROOT = Path(__file__).resolve().parent
OUT = ROOT / "output" / "pdf" / "Ficha_Tutorial_Observabilidade_SpringBoot.pdf"
OUT.parent.mkdir(parents=True, exist_ok=True)

NAVY = colors.HexColor("#17324D")
NAVY_2 = colors.HexColor("#244B68")
ORANGE = colors.HexColor("#F28C28")
TEAL = colors.HexColor("#17A6A6")
GREEN = colors.HexColor("#4D9B63")
RED = colors.HexColor("#C94B4B")
BLUE = colors.HexColor("#3D7DA6")
INK = colors.HexColor("#25313C")
MID = colors.HexColor("#66737F")
LIGHT = colors.HexColor("#F3F6F8")
LIGHT_BLUE = colors.HexColor("#EAF3F8")
LIGHT_ORANGE = colors.HexColor("#FFF3E7")
LIGHT_GREEN = colors.HexColor("#EDF7F0")
LIGHT_RED = colors.HexColor("#FCEEEE")
WHITE = colors.white


def register_fonts():
    candidates = [
        ("C:/Windows/Fonts/aptos.ttf", "C:/Windows/Fonts/aptosbd.ttf", "Aptos"),
        ("C:/Windows/Fonts/calibri.ttf", "C:/Windows/Fonts/calibrib.ttf", "Calibri"),
        ("C:/Windows/Fonts/arial.ttf", "C:/Windows/Fonts/arialbd.ttf", "Arial"),
    ]
    for regular, bold, family in candidates:
        if Path(regular).exists() and Path(bold).exists():
            pdfmetrics.registerFont(TTFont(family, regular))
            pdfmetrics.registerFont(TTFont(f"{family}-Bold", bold))
            return family, f"{family}-Bold"
    return "Helvetica", "Helvetica-Bold"


FONT, FONT_BOLD = register_fonts()


styles = getSampleStyleSheet()
styles.add(ParagraphStyle(
    name="SectionLabel", fontName=FONT_BOLD, fontSize=8.5, leading=10,
    textColor=ORANGE, spaceAfter=5, uppercase=True,
))
styles.add(ParagraphStyle(
    name="PageTitle", fontName=FONT_BOLD, fontSize=21, leading=24,
    textColor=NAVY, spaceAfter=10,
))
styles.add(ParagraphStyle(
    name="H2x", fontName=FONT_BOLD, fontSize=13, leading=16,
    textColor=NAVY, spaceBefore=4, spaceAfter=6,
))
styles.add(ParagraphStyle(
    name="BodyX", fontName=FONT, fontSize=9.4, leading=13.2,
    textColor=INK, spaceAfter=6,
))
styles.add(ParagraphStyle(
    name="SmallX", fontName=FONT, fontSize=8, leading=10.5,
    textColor=MID, spaceAfter=4,
))
styles.add(ParagraphStyle(
    name="BulletX", fontName=FONT, fontSize=9.2, leading=12.5,
    textColor=INK, leftIndent=13, firstLineIndent=-8, bulletIndent=2,
    spaceAfter=4,
))
styles.add(ParagraphStyle(
    name="CodeX", fontName="Courier", fontSize=7.5, leading=10,
    textColor=colors.HexColor("#243847"), leftIndent=7, rightIndent=7,
    spaceAfter=0,
))
styles.add(ParagraphStyle(
    name="CalloutTitle", fontName=FONT_BOLD, fontSize=9.5, leading=12,
    textColor=NAVY, spaceAfter=3,
))
styles.add(ParagraphStyle(
    name="CalloutBody", fontName=FONT, fontSize=8.7, leading=11.8,
    textColor=INK,
))
styles.add(ParagraphStyle(
    name="TableHead", fontName=FONT_BOLD, fontSize=8, leading=10,
    textColor=WHITE,
))
styles.add(ParagraphStyle(
    name="TableCell", fontName=FONT, fontSize=7.8, leading=10,
    textColor=INK,
))
styles.add(ParagraphStyle(
    name="TableCellBold", fontName=FONT_BOLD, fontSize=7.8, leading=10,
    textColor=NAVY,
))


def P(text, style="BodyX"):
    return Paragraph(text, styles[style])


def bullet(text):
    return Paragraph(f"• {text}", styles["BulletX"])


def section(label, title, intro=None):
    items = [P(label.upper(), "SectionLabel"), P(title, "PageTitle")]
    if intro:
        items.append(P(intro))
    return items


class Callout(Flowable):
    def __init__(self, title, body, kind="blue", width=17.2 * cm):
        super().__init__()
        palette = {
            "blue": (LIGHT_BLUE, BLUE),
            "orange": (LIGHT_ORANGE, ORANGE),
            "green": (LIGHT_GREEN, GREEN),
            "red": (LIGHT_RED, RED),
        }
        self.bg, self.accent = palette[kind]
        self.width = width
        self.title = Paragraph(title, styles["CalloutTitle"])
        self.body = Paragraph(body, styles["CalloutBody"])

    def wrap(self, availWidth, availHeight):
        self.width = min(self.width, availWidth)
        tw, th = self.title.wrap(self.width - 22, availHeight)
        bw, bh = self.body.wrap(self.width - 22, availHeight)
        self.height = th + bh + 20
        return self.width, self.height

    def draw(self):
        self.canv.setFillColor(self.bg)
        self.canv.setStrokeColor(self.accent)
        self.canv.setLineWidth(0.7)
        self.canv.roundRect(0, 0, self.width, self.height, 5, fill=1, stroke=1)
        self.canv.setFillColor(self.accent)
        self.canv.roundRect(0, 0, 5, self.height, 5, fill=1, stroke=0)
        self.title.drawOn(self.canv, 13, self.height - 10 - self.title.height)
        self.body.drawOn(self.canv, 13, 9)


class FlowBoxes(Flowable):
    def __init__(self, boxes, arrows=True, width=17.2 * cm, box_height=18 * mm):
        super().__init__()
        self.boxes = boxes
        self.arrows = arrows
        self.width = width
        self.height = box_height

    def wrap(self, availWidth, availHeight):
        self.width = min(self.width, availWidth)
        return self.width, self.height

    def draw(self):
        n = len(self.boxes)
        gap = 7 * mm if self.arrows else 3 * mm
        bw = (self.width - gap * (n - 1)) / n
        for i, (title, subtitle, color) in enumerate(self.boxes):
            x = i * (bw + gap)
            self.canv.setFillColor(colors.Color(color.red, color.green, color.blue, alpha=0.10))
            self.canv.setStrokeColor(color)
            self.canv.setLineWidth(1)
            self.canv.roundRect(x, 1, bw, self.height - 2, 4, fill=1, stroke=1)
            self.canv.setFillColor(color)
            self.canv.setFont(FONT_BOLD, 8.2)
            self.canv.drawCentredString(x + bw / 2, self.height - 8 * mm, title)
            self.canv.setFillColor(MID)
            self.canv.setFont(FONT, 6.8)
            self.canv.drawCentredString(x + bw / 2, 4.2 * mm, subtitle)
            if self.arrows and i < n - 1:
                x1 = x + bw + 1.5 * mm
                x2 = x + bw + gap - 1.5 * mm
                y = self.height / 2
                self.canv.setStrokeColor(NAVY_2)
                self.canv.line(x1, y, x2, y)
                self.canv.line(x2 - 2.3 * mm, y + 1.8 * mm, x2, y)
                self.canv.line(x2 - 2.3 * mm, y - 1.8 * mm, x2, y)


class TraceTree(Flowable):
    def __init__(self, width=17.2 * cm):
        super().__init__()
        self.width = width
        self.height = 62 * mm

    def wrap(self, availWidth, availHeight):
        self.width = min(self.width, availWidth)
        return self.width, self.height

    def draw_box(self, x, y, w, h, title, duration, color, error=False):
        self.canv.setFillColor(LIGHT_RED if error else LIGHT_BLUE)
        self.canv.setStrokeColor(RED if error else color)
        self.canv.roundRect(x, y, w, h, 4, fill=1, stroke=1)
        self.canv.setFillColor(RED if error else NAVY)
        self.canv.setFont(FONT_BOLD, 7.2)
        self.canv.drawString(x + 5, y + h - 10, title)
        self.canv.setFillColor(MID)
        self.canv.setFont(FONT, 6.5)
        self.canv.drawRightString(x + w - 5, y + 5, duration)

    def draw(self):
        w = self.width
        root_w = 42 * mm
        child_w = 44 * mm
        h = 11 * mm
        x0 = 0
        y0 = self.height - h - 2 * mm
        self.draw_box(x0, y0, root_w, h, "HTTP POST /pedidos", "191 ms", TEAL)
        x1 = 51 * mm
        children = [
            ("security filterchain", "52 ms", False),
            ("pedido.criar", "56 ms", False),
            ("outbox.registrar", "13 ms", False),
        ]
        for i, (name, dur, err) in enumerate(children):
            y = y0 - i * 16 * mm
            self.canv.setStrokeColor(colors.HexColor("#8BA0AF"))
            self.canv.line(root_w, y0 + h / 2, x1 - 2 * mm, y + h / 2)
            self.draw_box(x1, y, child_w, h, name, dur, BLUE, err)
        x2 = 105 * mm
        self.canv.setStrokeColor(colors.HexColor("#8BA0AF"))
        self.canv.line(x1 + child_w, y0 - 2 * 16 * mm + h / 2, x2 - 2 * mm, y0 - 2 * 16 * mm + h / 2)
        self.draw_box(x2, y0 - 2 * 16 * mm, 36 * mm, h, "outbox.publicar", "103 ms", ORANGE)
        x3 = 146 * mm
        self.canv.line(x2 + 36 * mm, y0 - 2 * 16 * mm + h / 2, x3 - 2 * mm, y0 - 2 * 16 * mm + h / 2)
        self.draw_box(x3, y0 - 2 * 16 * mm, 25 * mm, h, "Kafka", "send/process", GREEN)
        self.canv.setFillColor(MID)
        self.canv.setFont(FONT, 6.5)
        self.canv.drawString(106 * mm, 3 * mm, "Mesmo traceId; spanIds diferentes; lacuna assíncrona esperada")


def code_block(text):
    rows = [[Paragraph(escape(line) if line else "&#160;", styles["CodeX"])] for line in text.strip("\n").splitlines()]
    t = Table(rows, colWidths=[17.2 * cm], hAlign="LEFT")
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#EEF2F4")),
        ("BOX", (0, 0), (-1, -1), 0.6, colors.HexColor("#CAD4DB")),
        ("LEFTPADDING", (0, 0), (-1, -1), 4),
        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
        ("TOPPADDING", (0, 0), (-1, -1), 1.5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 1.5),
    ]))
    return t


def data_table(headers, rows, widths=None):
    data = [[P(h, "TableHead") for h in headers]]
    for row in rows:
        data.append([P(str(v), "TableCellBold" if i == 0 else "TableCell") for i, v in enumerate(row)])
    t = Table(data, colWidths=widths, repeatRows=1, hAlign="LEFT")
    style = [
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#D8E0E5")),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]
    for r in range(1, len(data)):
        style.append(("BACKGROUND", (0, r), (-1, r), WHITE if r % 2 else LIGHT))
    t.setStyle(TableStyle(style))
    return t


def page_break(story):
    story.append(PageBreak())


def header_footer(c: canvas.Canvas, doc):
    c.saveState()
    w, h = A4
    c.setStrokeColor(colors.HexColor("#D3DCE2"))
    c.setLineWidth(0.6)
    c.line(20 * mm, h - 15 * mm, w - 20 * mm, h - 15 * mm)
    c.setFont(FONT_BOLD, 7.2)
    c.setFillColor(NAVY)
    c.drawString(20 * mm, h - 11.5 * mm, "FICHA-TUTORIAL · OBSERVABILIDADE")
    c.setFont(FONT, 7.2)
    c.setFillColor(MID)
    c.drawRightString(w - 20 * mm, h - 11.5 * mm, "ProjetoSpringBoot")
    c.line(20 * mm, 14 * mm, w - 20 * mm, 14 * mm)
    c.setFont(FONT, 7)
    c.drawString(20 * mm, 9.5 * mm, "Material de estudo de Ricardo · ambiente local")
    c.drawRightString(w - 20 * mm, 9.5 * mm, str(doc.page))
    c.restoreState()


def cover(c: canvas.Canvas, doc):
    c.saveState()
    w, h = A4
    c.setFillColor(NAVY)
    c.rect(0, h - 74 * mm, w, 74 * mm, fill=1, stroke=0)
    c.setFillColor(ORANGE)
    c.rect(0, h - 77 * mm, w, 3 * mm, fill=1, stroke=0)
    c.setFont(FONT_BOLD, 9)
    c.drawString(22 * mm, h - 24 * mm, "PROJETOSPRINGBOOT · FICHA-TUTORIAL")
    c.setFont(FONT_BOLD, 27)
    c.setFillColor(WHITE)
    c.drawString(22 * mm, h - 39 * mm, "Observabilidade de ponta a ponta")
    c.setFont(FONT, 12)
    c.setFillColor(colors.HexColor("#DCE8EF"))
    c.drawString(22 * mm, h - 50 * mm, "Métricas, traces e logs em Spring Boot, Kafka e Outbox")

    y = h - 101 * mm
    c.setFillColor(INK)
    c.setFont(FONT_BOLD, 13)
    c.drawString(22 * mm, y, "Do fundamento ao diagnóstico de falhas reais")
    y -= 11 * mm
    intro = Paragraph(
        "Este tutorial registra a arquitetura construída durante o estudo e explica <b>por que cada peça existe</b>: "
        "Actuator, Micrometer, Prometheus, Grafana, OpenTelemetry, OTLP, Jaeger, Loki, Alloy, propagação W3C, "
        "Outbox, retries, DLT e idempotência.",
        ParagraphStyle("CoverBody", fontName=FONT, fontSize=11, leading=16, textColor=INK),
    )
    intro.wrapOn(c, 164 * mm, 50 * mm)
    intro.drawOn(c, 22 * mm, y - intro.height)
    y -= intro.height + 13 * mm

    items = [
        ("MÉTRICAS", "Prometheus + Grafana", TEAL),
        ("TRACES", "OpenTelemetry + Jaeger", ORANGE),
        ("LOGS", "Loki + Alloy + Grafana", GREEN),
    ]
    bw = 50 * mm
    for i, (name, sub, col) in enumerate(items):
        x = 22 * mm + i * 56 * mm
        c.setFillColor(colors.Color(col.red, col.green, col.blue, alpha=0.12))
        c.setStrokeColor(col)
        c.roundRect(x, y - 24 * mm, bw, 24 * mm, 5, fill=1, stroke=1)
        c.setFillColor(col)
        c.setFont(FONT_BOLD, 9)
        c.drawString(x + 5 * mm, y - 9 * mm, name)
        c.setFillColor(MID)
        c.setFont(FONT, 7.8)
        c.drawString(x + 5 * mm, y - 16 * mm, sub)

    c.setFillColor(LIGHT_ORANGE)
    c.setStrokeColor(ORANGE)
    c.roundRect(22 * mm, 40 * mm, 164 * mm, 30 * mm, 5, fill=1, stroke=1)
    c.setFillColor(NAVY)
    c.setFont(FONT_BOLD, 10)
    c.drawString(28 * mm, 59 * mm, "Casos estudados")
    c.setFont(FONT, 8.8)
    c.setFillColor(INK)
    c.drawString(28 * mm, 51 * mm, "Fluxo normal · falha persistente 999.99 · retry + DLT · falha pós-commit 888.88")
    c.drawString(28 * mm, 45 * mm, "Idempotência do producer · idempotência do consumer · janela de duplicidade da Outbox")
    c.setFont(FONT_BOLD, 8)
    c.setFillColor(ORANGE)
    c.drawString(22 * mm, 25 * mm, "ATUALIZADO EM 31 DE AGOSTO DE 2026")
    c.setFont(FONT, 7.5)
    c.setFillColor(MID)
    c.drawRightString(w - 22 * mm, 25 * mm, "Spring Boot 4.1 · Java 21 · Kafka 4.3")
    c.restoreState()


class TutorialDoc(BaseDocTemplate):
    def __init__(self, filename):
        super().__init__(
            filename,
            pagesize=A4,
            leftMargin=20 * mm,
            rightMargin=20 * mm,
            topMargin=21 * mm,
            bottomMargin=18 * mm,
            title="Ficha-Tutorial de Observabilidade — ProjetoSpringBoot",
            author="Ricardo",
            subject="Actuator, Micrometer, Prometheus, Grafana, OpenTelemetry, Jaeger, Loki, Kafka, Outbox e idempotência",
        )
        frame = Frame(self.leftMargin, self.bottomMargin, self.width, self.height, id="body")
        self.addPageTemplates([
            PageTemplate(id="cover", frames=frame, onPage=cover, autoNextPageTemplate="body"),
            PageTemplate(id="body", frames=frame, onPage=header_footer),
        ])


story = [Spacer(1, 244 * mm), PageBreak()]

# 2
story += section("1 · mapa do tutorial", "O sistema que construímos", "A observabilidade foi adicionada sem alterar a regra central: a API grava o pedido e o evento na mesma transação; um job publica a Outbox e dois consumers processam a mensagem.")
story += [Spacer(1, 3 * mm), FlowBoxes([
    ("Cliente HTTP", "POST /pedidos", BLUE),
    ("Pedido + Outbox", "transação local", ORANGE),
    ("Kafka", "pedidos-criados", GREEN),
    ("Consumers", "estoque + notificação", TEAL),
]), Spacer(1, 7 * mm)]
story += [P("Três sinais sobre o mesmo fluxo", "H2x"), data_table(
    ["Sinal", "Pergunta que responde", "Ferramenta no projeto"],
    [
        ("Métricas", "Quanto? Com que frequência? A tendência piorou?", "Micrometer → Prometheus → Grafana"),
        ("Traces", "Por onde a operação passou? Onde demorou ou falhou?", "Micrometer Observation → OpenTelemetry/OTLP → Jaeger"),
        ("Logs", "Qual foi a narrativa detalhada e os valores envolvidos?", "Logback → arquivo → Alloy → Loki → Grafana"),
    ], [27 * mm, 75 * mm, 70 * mm,
]), Spacer(1, 7 * mm), Callout("Ideia-guia", "As três fontes não competem. A métrica aponta o sintoma; o trace localiza a etapa; o log fornece o detalhe. <b>traceId</b> e <b>eventoId</b> conectam a investigação.", "green")]
page_break(story)

# 3
story += section("2 · fundamentos", "Observabilidade não é apenas monitoramento", "Monitorar é acompanhar perguntas conhecidas. Observabilidade é conseguir investigar também perguntas que não foram previstas, usando os sinais emitidos pelo sistema.")
story += [data_table(
    ["Conceito", "Foco", "Exemplo no projeto"],
    [
        ("Monitoramento", "Condição conhecida e alerta", "Outbox pendente acima do normal"),
        ("Observabilidade", "Explicar o estado interno a partir das saídas", "Descobrir se a demora ocorreu no job, Kafka ou consumer"),
        ("Telemetria", "Dados produzidos pelo sistema", "Séries temporais, spans e linhas de log"),
    ], [35 * mm, 59 * mm, 78 * mm,
]), Spacer(1, 7 * mm)]
story += [P("Uma investigação típica", "H2x"), FlowBoxes([
    ("1. Alerta", "métrica anormal", RED),
    ("2. Trace", "etapa crítica", ORANGE),
    ("3. Log", "causa e contexto", BLUE),
    ("4. Correção", "validar recuperação", GREEN),
]), Spacer(1, 7 * mm)]
story += [bullet("Métricas são excelentes para agregação e tendência, mas não carregam identificadores únicos."), bullet("Traces preservam causalidade entre etapas síncronas e assíncronas."), bullet("Logs são flexíveis e detalhados, mas precisam de correlação e retenção controlada."), Callout("Regra de ouro", "Não transforme tudo em métrica, tudo em span ou tudo em log. Escolha o sinal conforme a pergunta operacional.", "orange")]
page_break(story)

# 4
story += section("3 · arquitetura", "A pilha implementada", "Cada ferramenta tem uma responsabilidade clara. O Grafana é a janela de consulta; os dados continuam armazenados nos backends especializados.")
story += [FlowBoxes([
    ("Spring Boot", "instrumentação", NAVY_2),
    ("Prometheus", "métricas", TEAL),
    ("Grafana", "painéis", ORANGE),
]), Spacer(1, 5 * mm), FlowBoxes([
    ("Spring Boot", "spans + contexto", NAVY_2),
    ("Jaeger", "traces OTLP", BLUE),
    ("Jaeger UI", "timeline/graph", ORANGE),
]), Spacer(1, 5 * mm), FlowBoxes([
    ("Arquivo .log", "logs correlacionados", NAVY_2),
    ("Alloy → Loki", "coleta + índice", GREEN),
    ("Grafana", "Explore/LogQL", ORANGE),
]), Spacer(1, 7 * mm)]
story += [data_table(
    ["Componente", "Responsabilidade", "Não confundir com"],
    [
        ("Actuator", "Endpoints operacionais do Spring Boot", "Backend de observabilidade"),
        ("Micrometer", "Fachada e API de observações", "Banco que armazena métricas/traces"),
        ("OpenTelemetry", "Padrão, SDK e exportação de telemetria", "Interface de consulta como o Jaeger"),
        ("Grafana", "Consulta e visualização de fontes", "Armazenamento do Prometheus ou Loki"),
    ], [32 * mm, 76 * mm, 64 * mm,
])]
page_break(story)

# 5
story += section("4 · Spring Boot", "Actuator: a porta operacional", "O Actuator adiciona endpoints de produção para saúde, métricas e integração com sistemas externos. No projeto, a exposição foi restrita ao necessário.")
story += [code_block("""
management:
  endpoints.web.exposure.include: health,metrics,prometheus
  endpoint.health.show-details: when-authorized
  endpoint.health.show-components: when-authorized
"""), Spacer(1, 6 * mm), data_table(
    ["Endpoint", "Uso"],
    [
        ("/actuator/health", "Estado e componentes de saúde; detalhes apenas para autorizado"),
        ("/actuator/metrics", "Catálogo e inspeção pontual dos meters registrados"),
        ("/actuator/prometheus", "Formato coletado periodicamente pelo Prometheus"),
    ], [51 * mm, 121 * mm,
]), Spacer(1, 7 * mm), Callout("Segurança", "Endpoints operacionais podem revelar topologia, dependências e comportamento interno. Exponha somente os necessários e aplique autenticação/autorização, rede privada ou ambos.", "red"), Spacer(1, 6 * mm)]
story += [P("Actuator ≠ observabilidade completa", "H2x"), P("Ele expõe a superfície operacional e integra o Spring Boot com a infraestrutura. A coleta, o armazenamento, as consultas e os painéis pertencem aos backends.")]
page_break(story)

# 6
story += section("5 · instrumentação", "Micrometer e a API Observation", "Micrometer oferece uma camada neutra para instrumentar a aplicação. O código registra a intenção — contador, timer ou observação — sem ficar amarrado a um backend específico.")
story += [data_table(
    ["Peça", "Função no projeto"],
    [
        ("MeterRegistry", "Registra Counter, Gauge e Timer; o registry Prometheus traduz a saída"),
        ("ObservationRegistry", "Cria observações que podem produzir métricas e/ou spans"),
        ("Tracer", "Acessa span/contexto atual e permite criar spans explícitos"),
        ("Propagator", "Injeta e extrai traceparent/tracestate em transportes"),
    ], [45 * mm, 127 * mm,
]), Spacer(1, 7 * mm), code_block("""
Observation.createNotStarted("pedido.criar", observationRegistry)
    .observe(() -> criarPedidoComOutbox(dto));

Timer.builder("kafka.consumer.processamento.tempo")
    .tag("consumer", "estoque")
    .tag("resultado", "sucesso")
    .register(meterRegistry);
"""), Spacer(1, 6 * mm), Callout("Separação saudável", "Micrometer é a fachada usada pelo Spring; OpenTelemetry é o padrão/SDK que transporta traces; OTLP é o protocolo de exportação; Jaeger é o backend e a interface de análise.", "blue")]
page_break(story)

# 7
story += section("6 · métricas", "Counter, Gauge, Timer e tags", "O tipo do meter deve refletir o comportamento do dado. A escolha errada cria painéis enganosos.")
story += [data_table(
    ["Tipo", "Semântica", "Exemplo"],
    [
        ("Counter", "Só cresce; calcule taxa ou aumento por janela", "Falhas de publicação; mensagens enviadas à DLT"),
        ("Gauge", "Fotografia atual; pode subir e descer", "Quantidade atual de eventos pendentes na Outbox"),
        ("Timer", "Duração + contagem; produz sum/count/buckets", "Tempo de publicação e processamento Kafka"),
    ], [31 * mm, 72 * mm, 69 * mm,
]), Spacer(1, 7 * mm), P("Tags e cardinalidade", "H2x"), bullet("Boas tags têm conjunto pequeno e previsível: consumer=estoque|notificacao, resultado=sucesso|falha."), bullet("IDs únicos — pedidoId, eventoId, traceId, usuário — não pertencem a labels de métricas."), bullet("Cada combinação de labels vira uma série temporal. Cardinalidade alta aumenta memória, custo e lentidão."), Spacer(1, 4 * mm), Callout("Onde colocar identificadores únicos?", "Use <b>atributos de span</b> para investigação causal e <b>conteúdo do log</b> para busca detalhada. Em Loki, mantenha esses IDs no texto, não como labels.", "orange")]
page_break(story)

# 8
story += section("7 · implementação", "As métricas criadas no projeto", "As métricas foram desenhadas para responder às perguntas operacionais da Outbox e dos consumers.")
story += [data_table(
    ["Meter Micrometer", "Tipo", "Pergunta respondida"],
    [
        ("outbox.eventos.pendentes", "Gauge", "Há fila acumulando agora?"),
        ("outbox.publicacoes.falhas", "Counter", "Quantas tentativas falharam no período?"),
        ("outbox.publicacao.tempo", "Timer", "Quanto tempo a publicação leva?"),
        ("kafka.consumer.processamento.tempo", "Timer + tags", "Qual consumer/resultado está mais lento?"),
        ("kafka.dlt.mensagens.enviadas", "Counter", "Quantas mensagens esgotaram as tentativas?"),
    ], [65 * mm, 31 * mm, 76 * mm,
]), Spacer(1, 7 * mm), P("Nomes vistos pelo Prometheus", "H2x"), code_block("""
outbox_eventos_pendentes_eventos
outbox_publicacoes_falhas_tentativas_total
outbox_publicacao_tempo_seconds_sum
outbox_publicacao_tempo_seconds_count
kafka_dlt_mensagens_enviadas_mensagens_total
"""), Spacer(1, 6 * mm), Callout("Por que há sufixos?", "O registry Prometheus normaliza pontos para sublinhados e acrescenta unidade/sufixo conforme o meter. Timers expõem várias séries, como <b>sum</b> e <b>count</b>.", "blue")]
page_break(story)

# 9
story += section("8 · coleta", "Prometheus e o modelo pull", "O Prometheus visita o endpoint da aplicação em intervalos regulares. Ele armazena séries temporais compostas por nome, labels, timestamp e valor.")
story += [FlowBoxes([
    ("Aplicação", "/actuator/prometheus", NAVY_2),
    ("Prometheus", "scrape a cada 5 s", TEAL),
    ("Grafana", "PromQL", ORANGE),
]), Spacer(1, 7 * mm), code_block("""
scrape_configs:
  - job_name: projeto-springboot
    scrape_interval: 5s
    metrics_path: /ProjetoSpringBoot/actuator/prometheus
    static_configs:
      - targets: [host.docker.internal:8080]
"""), Spacer(1, 6 * mm)]
story += [P("PromQL usado nos painéis", "H2x"), data_table(
    ["Objetivo", "Consulta"],
    [
        ("Pendentes atuais", "outbox_eventos_pendentes_eventos"),
        ("Falhas no período", "sum(increase(outbox_publicacoes_falhas_tentativas_total[$__range]))"),
        ("Tempo médio", "increase(..._sum[$__range]) / increase(..._count[$__range])"),
        ("Aplicação disponível", "up{job=\"projeto-springboot\"}"),
    ], [49 * mm, 123 * mm,
])]
page_break(story)

# 10
story += section("9 · visualização", "Grafana: dashboards e exploração", "O Grafana consulta as fontes configuradas. No projeto, Prometheus e Loki foram provisionados automaticamente para tornar o ambiente reproduzível.")
story += [data_table(
    ["Painel do dashboard", "Decisão operacional"],
    [
        ("Eventos pendentes na Outbox", "Ver acúmulo e confirmar se o job está drenando"),
        ("Falhas de publicação no período", "Detectar instabilidade no envio"),
        ("Tempo médio até publicação", "Acompanhar latência e degradação"),
        ("Mensagens à DLT", "Identificar eventos que esgotaram retries"),
        ("Processamentos Kafka por resultado", "Separar sucesso/falha por consumer"),
        ("Tempo médio dos consumers", "Comparar estoque e notificação"),
    ], [71 * mm, 101 * mm,
]), Spacer(1, 7 * mm), Callout("Dashboard vs Explore", "Use o <b>dashboard</b> para perguntas recorrentes e visão rápida. Use o <b>Explore</b> para investigação livre, refinando PromQL ou LogQL durante um incidente.", "green"), Spacer(1, 6 * mm), P("Um bom painel", "H2x"), bullet("tem título que expressa a pergunta, unidade correta e janela temporal explícita;"), bullet("mostra ausência de dados de forma consciente, sem confundir zero com falha de coleta;"), bullet("permite navegar do sintoma agregado para trace e logs correlacionados.")]
page_break(story)

# 11
story += section("10 · logs", "Correlação: traceId, spanId e eventoId", "O padrão de log coloca lado a lado a identidade técnica do trace e a identidade de negócio do evento.")
story += [code_block("""
correlation: >-
  [traceId=%X{traceId:-sem-trace}
   spanId=%X{spanId:-sem-span}
   eventoId=%X{eventoId:-sem-evento}]
"""), Spacer(1, 7 * mm), data_table(
    ["Identificador", "Escopo", "Uso"],
    [
        ("traceId", "Operação distribuída completa", "Agrupar HTTP, Outbox, Kafka e consumers"),
        ("spanId", "Uma etapa do trace", "Localizar o ponto exato que produziu o log"),
        ("eventoId", "Evento de negócio", "Reconhecer retries/redeliveries do mesmo evento"),
    ], [35 * mm, 58 * mm, 79 * mm,
]), Spacer(1, 7 * mm), Callout("Por que aparece sem-evento?", "A requisição já tem trace antes de existir um evento de negócio. O eventoId passa a existir durante o registro da Outbox e é colocado no MDC ao publicar/consumir.", "blue"), Spacer(1, 5 * mm), Callout("Por que aparece sem-trace?", "Uma thread sem observação ativa não possui contexto no MDC. Após habilitar observação no Kafka e propagar traceparent, os consumers passaram a receber o trace corretamente.", "orange")]
page_break(story)

# 12
story += section("11 · logs centralizados", "Alloy, Loki e Grafana", "A aplicação continua escrevendo em arquivo. O Alloy acompanha o arquivo, adiciona labels estáveis e envia os registros ao Loki.")
story += [FlowBoxes([
    ("Logback", "projeto-springboot.log", NAVY_2),
    ("Alloy", "tail + labels", GREEN),
    ("Loki", "armazenamento", TEAL),
    ("Grafana", "Explore/LogQL", ORANGE),
]), Spacer(1, 7 * mm), P("Labels usados", "H2x"), code_block("""
service_name = "ProjetoSpringBoot"
environment  = "local"
"""), Spacer(1, 5 * mm), P("Consultas práticas", "H2x"), code_block("""
{service_name="ProjetoSpringBoot"}
{service_name="ProjetoSpringBoot"} |= " ERROR "
{service_name="ProjetoSpringBoot"} |= "traceId=12d87a..."
{service_name="ProjetoSpringBoot"} |= "eventoId=58c3d4f0..."
"""), Spacer(1, 6 * mm), Callout("Cardinalidade no Loki", "traceId e eventoId ficam no conteúdo do log. Transformá-los em labels criaria um fluxo para quase cada mensagem, prejudicando índice e custo.", "red")]
page_break(story)

# 13
story += section("12 · tracing", "Trace, span, eventos e atributos", "Um trace é o caminho causal completo. Cada unidade de trabalho é um span; spans formam uma árvore por relações de pai e filho.")
story += [TraceTree(), Spacer(1, 5 * mm), data_table(
    ["Elemento", "Significado"],
    [
        ("traceId", "Identidade do caminho completo; compartilhada por todos os spans"),
        ("spanId", "Identidade única de uma etapa"),
        ("parentSpanId", "Relação que monta a árvore causal"),
        ("attribute/tag", "Metadado pesquisável: evento.id, outbox.id, destination"),
        ("event/log", "Ocorrência temporal dentro do span, como uma exception"),
        ("status", "Resultado semântico, normalmente unset/ok/error"),
    ], [39 * mm, 133 * mm,
]), Spacer(1, 6 * mm), Callout("O spanId sempre muda?", "Cada nova observação instrumentada cria outro spanId. O span HTTP é automático; pedido.criar, pedido.listar, outbox.registrar e outbox.publicar foram definidos por nós.", "green")]
page_break(story)

# 14
story += section("13 · ecossistema", "Micrometer × OpenTelemetry × OTLP × Jaeger", "Esses nomes descrevem camadas diferentes da solução.")
story += [data_table(
    ["Camada", "Papel", "No projeto"],
    [
        ("Micrometer", "API/fachada de observabilidade no ecossistema Spring", "Meters, Observation, Tracer e Propagator"),
        ("OpenTelemetry", "Padrão e SDK vendor-neutral para telemetria", "Implementação criada pelo starter do Spring Boot"),
        ("OTLP", "Protocolo para exportar traces/métricas/logs", "HTTP para /v1/traces"),
        ("Jaeger", "Backend e UI de tracing", "Recebe OTLP, armazena e exibe os traces"),
    ], [35 * mm, 79 * mm, 58 * mm,
]), Spacer(1, 8 * mm), FlowBoxes([
    ("Código Spring", "Observation API", NAVY_2),
    ("OTel SDK", "gera/exporta", BLUE),
    ("OTLP", "transporte", TEAL),
    ("Jaeger", "consulta", ORANGE),
]), Spacer(1, 8 * mm), Callout("Starter próprio", "<b>spring-boot-starter-opentelemetry</b> reúne dependências e autoconfiguração para criar o tracer/SDK e exportar via OTLP. Ele evita montar manualmente cada componente.", "blue"), Spacer(1, 5 * mm), P("Configuração usada", "H2x"), code_block("""
management.tracing.sampling.probability: 1.0
management.tracing.export.otlp.enabled: true
management.otlp.tracing.endpoint: http://localhost:4318/v1/traces
""")]
page_break(story)

# 15
story += section("14 · instrumentação", "Automática e explícita", "O Spring cria spans em integrações suportadas; nós adicionamos spans onde a regra de negócio precisava de nomes e atributos próprios.")
story += [data_table(
    ["Origem", "Spans observados"],
    [
        ("Automática HTTP", "http post /pedidos"),
        ("Spring Security", "security filterchain before/after, authenticate bearertoken, authorize request"),
        ("Spring Kafka", "pedidos-criados send e pedidos-criados process"),
        ("Explícita no projeto", "pedido.criar, pedido.listar, outbox.registrar, outbox.publicar"),
    ], [48 * mm, 124 * mm,
]), Spacer(1, 7 * mm), P("Span.kind encontrado no Jaeger", "H2x"), data_table(
    ["Kind", "Interpretação"],
    [
        ("internal", "Trabalho interno da aplicação, como outbox.publicar"),
        ("producer", "Envio ao broker"),
        ("consumer", "Processamento de uma mensagem recebida"),
        ("server", "Entrada HTTP atendida pelo servidor"),
    ], [42 * mm, 130 * mm,
]), Spacer(1, 7 * mm), Callout("Granularidade", "Crie spans em fronteiras que ajudam a explicar latência, erro ou causalidade. Instrumentar cada método aumenta ruído e custo sem melhorar o diagnóstico.", "orange")]
page_break(story)

# 16
story += section("15 · propagação", "W3C Trace Context", "O trace atravessa processos por headers padronizados. O principal é traceparent; tracestate transporta estado opcional de fornecedores.")
story += [code_block("""
traceparent: 00-12d87a799cf2e8fc3cf1f84a97c73aa6-c34f4c1934095a95-01
             |               |                |                |
           versão          traceId           spanId          flags
"""), Spacer(1, 7 * mm), data_table(
    ["Campo", "Tamanho", "Significado"],
    [
        ("version", "2 hex", "Versão do formato"),
        ("trace-id", "32 hex", "Identificador do trace completo"),
        ("parent-id", "16 hex", "Span que representa o pai remoto"),
        ("trace-flags", "2 hex", "Bit de sampling; 01 indica sampled"),
    ], [36 * mm, 28 * mm, 108 * mm,
]), Spacer(1, 7 * mm), Callout("Contexto, não identidade de negócio", "traceparent liga a telemetria. eventoId continua sendo a chave de negócio usada para reconhecer o mesmo evento em retries, DLT e idempotência.", "green"), Spacer(1, 5 * mm), Callout("Sampling em estudo", "A probabilidade 1.0 grava 100% dos traces para facilitar aprendizado. Em produção, o volume, o custo e a necessidade de retenção definem uma política menor ou adaptativa.", "orange")]
page_break(story)

# 17
story += section("16 · Outbox", "Como o trace atravessa a espera do job", "A transação HTTP termina antes da publicação. Para preservar a causalidade, o contexto W3C foi salvo junto com o evento da Outbox.")
story += [FlowBoxes([
    ("HTTP", "trace ativo", BLUE),
    ("Outbox", "salva traceparent", ORANGE),
    ("Job", "lê pendentes", NAVY_2),
    ("Publisher", "restaura contexto", GREEN),
]), Spacer(1, 7 * mm), data_table(
    ["Momento", "O que acontece"],
    [
        ("Registrar evento", "Propagator injeta traceparent/tracestate em carrier e a entidade persiste os valores"),
        ("Job encontra lote", "O job possui seu próprio trace técnico, pois o lote pode misturar origens"),
        ("Publicar cada evento", "Publisher extrai o contexto salvo e cria outbox.publicar como descendente do trace original"),
        ("Enviar ao Kafka", "Instrumentação injeta novo traceparent nos headers da mensagem"),
    ], [43 * mm, 129 * mm,
]), Spacer(1, 7 * mm), Callout("Por que o job tinha traceId diferente?", "O span do agendamento representa a execução do lote, não um único pedido. Cada evento restaura seu trace de origem somente no momento da publicação. Isso evita atribuir todos os eventos ao trace arbitrário do job.", "blue")]
page_break(story)

# 18
story += section("17 · Kafka", "Do producer aos dois consumers", "Com observation-enabled no template e nos listeners, o Spring Kafka injeta e extrai o contexto automaticamente.")
story += [code_block("""
spring.kafka.template.observation-enabled: true
spring.kafka.listener.observation-enabled: true
"""), Spacer(1, 7 * mm), FlowBoxes([
    ("outbox.publicar", "span internal", ORANGE),
    ("send", "span producer", GREEN),
    ("process", "estoque", TEAL),
    ("process", "notificação", BLUE),
]), Spacer(1, 7 * mm), P("O que permanece igual", "H2x"), bullet("Os consumers compartilham o mesmo traceId da requisição original."), bullet("Cada envio/processamento tem spanId próprio porque representa uma etapa distinta."), bullet("Os dois consumers são irmãos causais do envio e podem executar em paralelo."), bullet("topic, partition, offset, consumer group e listener id aparecem como atributos automáticos."), Spacer(1, 5 * mm), Callout("Identidades complementares", "<b>topic + partition + offset</b> identificam tecnicamente um registro no Kafka. <b>eventoId</b> identifica o evento de negócio e continua útil se a mensagem mudar de tópico, for para DLT ou for reprocessada.", "green")]
page_break(story)

# 19
story += section("18 · Jaeger", "Como ler um trace", "Comece pela forma geral; depois aprofunde nos spans que concentram duração ou exibem erro.")
story += [TraceTree(), Spacer(1, 5 * mm), data_table(
    ["Visão", "Como interpretar"],
    [
        ("Trace Timeline", "Posição no tempo, duração, paralelismo e lacunas assíncronas"),
        ("Trace Tabular", "Lista compacta para ordenar/comparar operação, duração e spanId"),
        ("Trace Graph", "Grafo causal da execução observada; não é diagrama estático do código"),
        ("Critical path", "Segmentos que determinam a duração percebida do workflow"),
        ("Tags", "Atributos técnicos e de negócio do span"),
        ("Logs", "Eventos do span, incluindo exception.message e stacktrace"),
        ("Process", "Atributos do recurso/SDK que produziu a telemetria"),
    ], [40 * mm, 132 * mm,
]), Spacer(1, 5 * mm), Callout("Atenção ao termo Logs", "No Jaeger, essa seção contém <b>eventos do span</b>. Ela não substitui os logs da aplicação armazenados no Loki.", "orange")]
page_break(story)

# 20
story += section("19 · caso normal", "Pedido criado com sucesso", "O trace mostra o pedido respondendo rapidamente e a continuação assíncrona acontecendo alguns segundos depois.")
story += [data_table(
    ["Ordem", "Etapa", "Resultado"],
    [
        ("1", "POST /pedidos + segurança", "Trace criado automaticamente"),
        ("2", "pedido.criar", "Regra de negócio executada"),
        ("3", "outbox.registrar", "Pedido e evento persistidos na mesma transação"),
        ("4", "Resposta HTTP", "Cliente recebe pedidoId"),
        ("5", "Job encontra pendente", "Trace próprio do lote"),
        ("6", "outbox.publicar", "Contexto original restaurado"),
        ("7", "Kafka send + 2 process", "Estoque e notificação concluem"),
    ], [18 * mm, 65 * mm, 89 * mm,
]), Spacer(1, 7 * mm), Callout("191 ms × 4,73 s", "A duração do span HTTP mede somente a resposta síncrona. A duração total do trace inclui a espera até o próximo ciclo do job e o processamento assíncrono. Nenhum número está errado; eles medem fronteiras diferentes.", "blue"), Spacer(1, 6 * mm), P("Leitura correta", "H2x"), bullet("Barras curtas no início: trabalho síncrono."), bullet("Espaço vazio: evento persistido aguardando o agendamento."), bullet("Barras no fim: publicação e consumers retomando o mesmo trace."), bullet("Branches paralelos: estoque e notificação são processamentos independentes.")]
page_break(story)

# 21
story += section("20 · falha 999.99", "Erro persistente, retries e DLT", "O valor 999.99 aciona uma falha simulada no estoque antes da conclusão do efeito. O erro continua ocorrendo em todas as tentativas.")
story += [FlowBoxes([
    ("Tentativa 1", "erro + exception", RED),
    ("Retry 1", "+1 s · erro", RED),
    ("Retry 2", "+1 s · erro", RED),
    ("DLT", "publish sucesso", ORANGE),
]), Spacer(1, 7 * mm), data_table(
    ["Sinal", "Evidência esperada"],
    [
        ("Trace", "Três spans consumer com error=true e eventos de exception"),
        ("Jaeger Logs", "exception.message = Falha simulada no processamento do estoque + stacktrace"),
        ("Kafka", "Mesmo topic/partition/offset em todas as tentativas"),
        ("DLT", "Span producer para pedidos-criados-dlt"),
        ("Métrica", "kafka.dlt.mensagens.enviadas incrementada"),
        ("Outro branch", "Consumer de notificação pode concluir normalmente"),
    ], [40 * mm, 132 * mm,
]), Spacer(1, 7 * mm), Callout("O envio à DLT não é outro erro", "O span da DLT pode aparecer bem-sucedido: ele representa a recuperação após esgotar retries. A falha de negócio está nos spans anteriores do consumer.", "orange"), Spacer(1, 5 * mm), Callout("Política configurada", "FixedBackOff de 1 segundo com 2 retries significa 3 tentativas totais: a original + duas repetições.", "red")]
page_break(story)

# 22
story += section("21 · falha 888.88", "Commit realizado, confirmação perdida", "O valor 888.88 simula a janela mais perigosa: o efeito e o marcador de idempotência foram commitados, mas o consumer falha antes de confirmar o offset.")
story += [FlowBoxes([
    ("Tentativa 1", "efeito + commit", GREEN),
    ("Falha", "antes do ack", RED),
    ("Redelivery", "mesmo evento", ORANGE),
    ("Idempotência", "efeito ignorado", TEAL),
]), Spacer(1, 7 * mm), data_table(
    ["Tentativa", "Banco", "Kafka", "Resultado"],
    [
        ("Primeira", "Efeito e evento processado gravados", "Offset não confirmado", "Span com erro"),
        ("Segunda", "eventoId já existe para estoque", "Registro reenviado", "Sem novo efeito; sucesso"),
        ("Final", "Estado permanece único", "Offset confirmado", "Sem DLT"),
    ], [27 * mm, 58 * mm, 43 * mm, 44 * mm,
]), Spacer(1, 7 * mm), Callout("O que procurar no trace", "Mesmo traceId e eventoId, mesmo topic/partition/offset, mas spanIds diferentes para cada tentativa. A segunda execução é uma nova etapa técnica sobre o mesmo evento de negócio.", "blue"), Spacer(1, 5 * mm), Callout("Regra transacional", "O efeito de negócio e o registro de idempotência precisam ser gravados na <b>mesma transação</b>. Separá-los recria uma janela em que um existe sem o outro.", "red")]
page_break(story)

# 23
story += section("22 · confiabilidade", "As duas idempotências e a Outbox", "São mecanismos complementares. Cada um fecha uma janela de duplicidade diferente.")
story += [data_table(
    ["Mecanismo", "Protege contra", "Não resolve sozinho"],
    [
        ("Idempotência do producer Kafka", "Duplicação causada por retry interno/ack perdido no envio do producer", "Redelivery e repetição do efeito no consumer"),
        ("Idempotência do consumer", "Executar novamente o mesmo evento de negócio", "Perda do evento antes de chegar ao Kafka"),
        ("Transactional Outbox", "Pedido commitado sem evento e indisponibilidade temporária do Kafka", "Duplicata após ack do Kafka e antes de atualizar status da Outbox"),
    ], [48 * mm, 64 * mm, 60 * mm,
]), Spacer(1, 8 * mm), P("A janela clássica da Outbox", "H2x"), FlowBoxes([
    ("Kafka confirma", "mensagem aceita", GREEN),
    ("Aplicação cai", "antes do status", RED),
    ("Job reenvia", "evento pendente", ORANGE),
    ("Consumer", "deduplica", TEAL),
]), Spacer(1, 8 * mm), Callout("Sem promessa falsa de exactly-once", "No conjunto, trabalhamos de forma robusta com entrega <b>at-least-once</b>: pode haver repetição técnica, mas o efeito de negócio permanece único por idempotência.", "green")]
page_break(story)

# 24
story += section("23 · diagnóstico", "Playbook: do alerta à causa", "Use uma sequência repetível para evitar buscas aleatórias durante um incidente.")
story += [data_table(
    ["Passo", "Ação", "Ferramenta"],
    [
        ("1", "Confirme disponibilidade e janela do problema", "Grafana / up"),
        ("2", "Veja pendentes, falhas, DLT e tempos", "Dashboard Prometheus"),
        ("3", "Escolha um trace lento ou com erro", "Jaeger Search"),
        ("4", "Localize critical path, error span e exception", "Jaeger Timeline"),
        ("5", "Copie traceId ou eventoId", "Tags/log correlation"),
        ("6", "Busque a narrativa completa", "Grafana Explore / Loki"),
        ("7", "Valide retry, DLT, deduplicação e recuperação", "Jaeger + métricas + logs"),
    ], [20 * mm, 95 * mm, 57 * mm,
]), Spacer(1, 7 * mm), P("Atalhos úteis", "H2x"), code_block("""
# Logs de um trace
{service_name="ProjetoSpringBoot"} |= "traceId=<id>"

# Logs de todas as tentativas do evento
{service_name="ProjetoSpringBoot"} |= "eventoId=<uuid>"

# DLT no intervalo visível do Grafana
increase(kafka_dlt_mensagens_enviadas_mensagens_total[$__range])
"""), Spacer(1, 6 * mm), Callout("Finalize com evidência", "Depois da correção, repita o cenário e confirme nos três sinais: contador estabilizado, trace concluído sem erro inesperado e logs coerentes.", "green")]
page_break(story)

# 25
story += section("24 · armadilhas", "Erros comuns e limites de produção", "A implementação de estudo usa 100% de sampling e ambiente local. Produção exige políticas explícitas de volume, segurança e retenção.")
story += [data_table(
    ["Armadilha", "Consequência", "Prática recomendada"],
    [
        ("IDs únicos como labels", "Explosão de cardinalidade", "IDs em span/log; tags limitadas em métricas"),
        ("Trace sem contexto na Outbox", "Fluxo quebrado no job", "Persistir e restaurar W3C trace context"),
        ("Marcar job inteiro com um trace de pedido", "Causalidade incorreta em lotes", "Trace do job separado; contexto por evento"),
        ("Efeito e dedupe em transações distintas", "Duplicidade ou evento marcado sem efeito", "Mesma transação local"),
        ("Sampling 1.0 permanente", "Custo e volume excessivos", "Política compatível com tráfego/risco"),
        ("Actuator aberto", "Exposição operacional", "Menor superfície + autenticação/rede"),
        ("Logs sem retenção", "Disco/custo descontrolado", "Rotação, limites e política de retenção"),
    ], [50 * mm, 55 * mm, 67 * mm,
]), Spacer(1, 7 * mm), Callout("PII e segredos", "Não registre tokens, credenciais, dados pessoais ou payloads sensíveis em tags, logs ou atributos de span. Telemetria também precisa de governança.", "red")]
page_break(story)

# 26
story += section("25 · checklist", "Ficha rápida de verificação", "Use esta página ao revisar uma nova integração ou antes de demonstrar o ambiente.")
story += [P("Fundamentos", "H2x"), bullet("[ ] Sei diferenciar métricas, traces e logs e quando usar cada sinal."), bullet("[ ] Sei explicar Actuator, Micrometer, OpenTelemetry, OTLP, Jaeger e Grafana."), P("Métricas", "H2x"), bullet("[ ] Counter, Gauge e Timer representam a semântica correta."), bullet("[ ] Labels são limitadas e não contêm IDs únicos."), bullet("[ ] Prometheus coleta /actuator/prometheus e o dashboard mostra dados."), P("Tracing", "H2x"), bullet("[ ] HTTP, Outbox, Kafka producer e consumers compartilham o trace original."), bullet("[ ] traceparent é persistido/restaurado na Outbox e propagado no Kafka."), bullet("[ ] Spans têm nomes úteis, atributos seguros e erros registrados."), P("Confiabilidade", "H2x"), bullet("[ ] Retries têm limite e backoff; após esgotar, a mensagem vai à DLT."), bullet("[ ] Consumer deduplica por eventoId + consumer dentro da transação do efeito."), bullet("[ ] Entendo a janela de duplicidade entre ack do Kafka e status da Outbox."), P("Logs", "H2x"), bullet("[ ] Arquivo é coletado pelo Alloy e consultável no Loki."), bullet("[ ] traceId/spanId/eventoId aparecem no texto; labels permanecem estáveis."), Spacer(1, 5 * mm), Callout("Critério de conclusão", "Você consegue partir de uma anomalia no dashboard, encontrar o trace, localizar a exceção e reunir todos os logs do mesmo evento.", "green")]
page_break(story)

# 27
story += section("26 · glossário", "Vocabulário essencial")
story += [data_table(
    ["Termo", "Definição curta"],
    [
        ("Meter", "Instrumento de métrica registrado no Micrometer"),
        ("Observation", "Unidade observável que pode acionar handlers de métricas e tracing"),
        ("Series", "Nome de métrica + conjunto de labels ao longo do tempo"),
        ("Scrape", "Coleta pull feita pelo Prometheus"),
        ("Trace", "Caminho causal completo de uma operação"),
        ("Span", "Etapa temporizada dentro do trace"),
        ("Context propagation", "Transporte do contexto entre processos/threads"),
        ("OTLP", "Protocolo de exportação do OpenTelemetry"),
        ("Sampling", "Decisão de registrar/exportar uma fração dos traces"),
        ("Outbox", "Evento persistido na mesma transação da mudança de negócio"),
        ("Retry", "Nova tentativa do mesmo processamento"),
        ("DLT", "Tópico de destino após esgotar a política de retries"),
        ("Idempotência", "Repetir uma operação sem repetir seu efeito de negócio"),
        ("Cardinalidade", "Quantidade de combinações distintas de labels"),
        ("LogQL", "Linguagem de consulta do Loki"),
        ("PromQL", "Linguagem de consulta do Prometheus"),
    ], [43 * mm, 129 * mm,
])]
page_break(story)

# 28
story += section("27 · referências", "Documentação oficial e implementação consultada", "As referências abaixo consolidam os conceitos e permitem aprofundar cada componente.")
story += [P("Documentação oficial", "H2x")]
refs = [
    ("Spring Boot · Observability", "https://docs.spring.io/spring-boot/reference/actuator/observability.html"),
    ("Spring Boot · Tracing", "https://docs.spring.io/spring-boot/reference/actuator/tracing.html"),
    ("Spring Boot · Actuator Endpoints", "https://docs.spring.io/spring-boot/reference/actuator/endpoints.html"),
    ("Spring Boot · Metrics", "https://docs.spring.io/spring-boot/reference/actuator/metrics.html"),
    ("Micrometer Reference", "https://docs.micrometer.io/micrometer/reference/"),
    ("OpenTelemetry · Traces", "https://opentelemetry.io/docs/concepts/signals/traces/"),
    ("OpenTelemetry · Context propagation", "https://opentelemetry.io/docs/concepts/context-propagation/"),
    ("W3C Trace Context", "https://www.w3.org/TR/trace-context/"),
    ("Prometheus · Overview", "https://prometheus.io/docs/introduction/overview/"),
    ("Grafana · Data sources", "https://grafana.com/docs/grafana/latest/datasources/"),
    ("Grafana Loki · Getting started", "https://grafana.com/docs/loki/latest/get-started/"),
    ("Grafana Alloy · Logs from files", "https://grafana.com/docs/alloy/latest/monitor/monitor-logs-from-file/"),
    ("Jaeger · Deployment", "https://www.jaegertracing.io/docs/1.76/deployment/"),
]
for name, url in refs:
    story.append(Paragraph(f"• <b>{name}</b> — <link href=\"{url}\" color=\"#3D7DA6\">{url}</link>", styles["SmallX"]))
story += [Spacer(1, 4 * mm), P("Arquivos centrais do ProjetoSpringBoot", "H2x"), P(
    "pom.xml · application.yaml · application-docker.yaml · compose.yaml · prometheus.yml · Alloy/Loki configs · "
    "OutboxMetrics.java · KafkaProcessamentoMetrics.java · DltMetrics.java · EventoOutboxService.java · "
    "PublicadorEventoOutboxService.java · EstoquePedidoConsumer.java · KafkaConsumerConfig.java · DltObservabilityRecoverer.java",
    "SmallX",
), Spacer(1, 6 * mm), Callout("Escopo desta ficha", "O documento descreve o ambiente efetivamente estudado em 31/08/2026. Configurações, versões e políticas devem ser revisitadas antes de produção.", "orange")]


doc = TutorialDoc(str(OUT))
doc.build(story)
print(OUT)
