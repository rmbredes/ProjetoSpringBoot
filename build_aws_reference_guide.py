"""Gera o guia de referência AWS do Projeto Recomeço em formato PDF."""

from __future__ import annotations

import html
import os
import textwrap
from datetime import date
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    Image,
    KeepTogether,
    NextPageTemplate,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)
from reportlab.platypus.tableofcontents import TableOfContents


BASE_DIR = Path(__file__).resolve().parent
OUTPUT_DIR = BASE_DIR / "output" / "pdf"
OUTPUT_PDF = OUTPUT_DIR / "Guia_Referencia_AWS_Projeto_Recomeco.pdf"
DIAGRAM_PATH = (
    BASE_DIR
    / "output"
    / "diagramas"
    / "fluxograma-completo-aws-grande.png"
)


def register_fonts() -> None:
    """Registra fontes do Windows para manter os acentos nítidos."""
    candidates = {
        "Arial": Path("C:/Windows/Fonts/arial.ttf"),
        "Arial-Bold": Path("C:/Windows/Fonts/arialbd.ttf"),
        "Consolas": Path("C:/Windows/Fonts/consola.ttf"),
    }
    for name, path in candidates.items():
        if path.exists():
            pdfmetrics.registerFont(TTFont(name, str(path)))


register_fonts()


NAVY = colors.HexColor("#17365D")
BLUE = colors.HexColor("#DCEAF7")
LIGHT_BLUE = colors.HexColor("#EEF5FB")
GREEN = colors.HexColor("#E7F4E4")
YELLOW = colors.HexColor("#FFF4CE")
RED = colors.HexColor("#FCE8E6")
LIGHT_GREY = colors.HexColor("#F3F4F6")
DARK_GREY = colors.HexColor("#374151")


class GuideDocument(BaseDocTemplate):
    """Documento com índice automático e marcadores de navegação."""

    def afterFlowable(self, flowable):  # noqa: N802 - nome exigido pelo ReportLab
        if not isinstance(flowable, Paragraph):
            return
        style_name = flowable.style.name
        if style_name not in {"GuideHeading1", "GuideHeading2"}:
            return
        level = 0 if style_name == "GuideHeading1" else 1
        text = flowable.getPlainText()
        key = f"sec-{self.page}-{abs(hash(text))}"
        self.canv.bookmarkPage(key)
        self.canv.addOutlineEntry(text, key, level=level, closed=False)
        self.notify("TOCEntry", (level, text, self.page, key))


styles = getSampleStyleSheet()
styles.add(
    ParagraphStyle(
        name="GuideTitle",
        fontName="Arial-Bold",
        fontSize=28,
        leading=34,
        textColor=NAVY,
        alignment=TA_CENTER,
        spaceAfter=12,
    )
)
styles.add(
    ParagraphStyle(
        name="GuideSubtitle",
        fontName="Arial",
        fontSize=13,
        leading=19,
        textColor=DARK_GREY,
        alignment=TA_CENTER,
        spaceAfter=10,
    )
)
styles.add(
    ParagraphStyle(
        name="GuideHeading1",
        fontName="Arial-Bold",
        fontSize=18,
        leading=23,
        textColor=NAVY,
        spaceBefore=9,
        spaceAfter=9,
        keepWithNext=True,
    )
)
styles.add(
    ParagraphStyle(
        name="GuideHeading2",
        fontName="Arial-Bold",
        fontSize=13,
        leading=17,
        textColor=NAVY,
        spaceBefore=8,
        spaceAfter=6,
        keepWithNext=True,
    )
)
styles.add(
    ParagraphStyle(
        name="GuideHeading3",
        fontName="Arial-Bold",
        fontSize=10.5,
        leading=14,
        textColor=DARK_GREY,
        spaceBefore=6,
        spaceAfter=4,
        keepWithNext=True,
    )
)
styles.add(
    ParagraphStyle(
        name="GuideBody",
        fontName="Arial",
        fontSize=9.4,
        leading=14.2,
        textColor=colors.HexColor("#1F2937"),
        alignment=TA_LEFT,
        spaceAfter=5,
    )
)
styles.add(
    ParagraphStyle(
        name="GuideSmall",
        fontName="Arial",
        fontSize=7.8,
        leading=11,
        textColor=DARK_GREY,
        spaceAfter=3,
    )
)
styles.add(
    ParagraphStyle(
        name="GuideCode",
        fontName="Consolas",
        fontSize=7.2,
        leading=10.2,
        textColor=colors.HexColor("#111827"),
        leftIndent=1 * mm,
        rightIndent=1 * mm,
    )
)
styles.add(
    ParagraphStyle(
        name="GuideTocTitle",
        fontName="Arial-Bold",
        fontSize=20,
        leading=25,
        textColor=NAVY,
        spaceAfter=12,
    )
)


def page_decoration(canvas, doc) -> None:
    """Desenha cabeçalho discreto, rodapé e número da página."""
    width, height = canvas._pagesize
    canvas.saveState()
    canvas.setStrokeColor(colors.HexColor("#CBD5E1"))
    canvas.setLineWidth(0.4)
    canvas.line(18 * mm, height - 13 * mm, width - 18 * mm, height - 13 * mm)
    canvas.setFont("Arial", 7.5)
    canvas.setFillColor(DARK_GREY)
    canvas.drawString(18 * mm, height - 10 * mm, "Projeto Recomeço - Guia AWS")
    canvas.drawRightString(width - 18 * mm, 9 * mm, f"Página {doc.page}")
    canvas.restoreState()


def paragraph(text: str, style: str = "GuideBody") -> Paragraph:
    return Paragraph(text.strip().replace("\n", " "), styles[style])


def h1(text: str) -> Paragraph:
    return paragraph(text, "GuideHeading1")


def h2(text: str) -> Paragraph:
    return paragraph(text, "GuideHeading2")


def h3(text: str) -> Paragraph:
    return paragraph(text, "GuideHeading3")


def bullets(items: list[str]) -> list:
    result = []
    for item in items:
        result.append(Paragraph(f"- {item}", styles["GuideBody"]))
    result.append(Spacer(1, 2 * mm))
    return result


def code_block(text: str) -> Table:
    """Cria bloco monoespaçado e quebra linhas grandes para não haver corte."""
    lines: list[str] = []
    for raw_line in text.strip("\n").splitlines():
        if len(raw_line) <= 104:
            lines.append(raw_line)
        else:
            indent = len(raw_line) - len(raw_line.lstrip())
            prefix = " " * indent
            wrapped = textwrap.wrap(
                raw_line,
                width=104,
                subsequent_indent=prefix + "    ",
                break_long_words=True,
                break_on_hyphens=False,
            )
            lines.extend(wrapped or [raw_line])
    safe = "<br/>".join(html.escape(line).replace(" ", "&nbsp;") for line in lines)
    table = Table([[Paragraph(safe, styles["GuideCode"])]], colWidths=[168 * mm])
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), LIGHT_GREY),
                ("BOX", (0, 0), (-1, -1), 0.4, colors.HexColor("#CBD5E1")),
                ("LEFTPADDING", (0, 0), (-1, -1), 6),
                ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                ("TOPPADDING", (0, 0), (-1, -1), 6),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
            ]
        )
    )
    return table


def callout(title: str, text: str, background=LIGHT_BLUE) -> Table:
    content = Paragraph(
        f"<b>{html.escape(title)}</b><br/>{text}",
        styles["GuideBody"],
    )
    table = Table([[content]], colWidths=[168 * mm])
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), background),
                ("BOX", (0, 0), (-1, -1), 0.7, NAVY),
                ("LEFTPADDING", (0, 0), (-1, -1), 8),
                ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                ("TOPPADDING", (0, 0), (-1, -1), 7),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
            ]
        )
    )
    return table


def data_table(headers: list[str], rows: list[list[str]], widths=None) -> Table:
    wrapped = [[Paragraph(f"<b>{html.escape(x)}</b>", styles["GuideSmall"]) for x in headers]]
    for row in rows:
        wrapped.append([Paragraph(str(x), styles["GuideSmall"]) for x in row])
    table = Table(wrapped, colWidths=widths, repeatRows=1, hAlign="LEFT")
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), BLUE),
                ("TEXTCOLOR", (0, 0), (-1, 0), NAVY),
                ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#CBD5E1")),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 5),
                ("RIGHTPADDING", (0, 0), (-1, -1), 5),
                ("TOPPADDING", (0, 0), (-1, -1), 4),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
            ]
        )
    )
    return table


def add_section_break(story: list) -> None:
    story.append(Spacer(1, 3 * mm))


def build_story() -> list:
    s: list = []

    # Capa
    s.extend([Spacer(1, 37 * mm), paragraph("Guia de Referência AWS", "GuideTitle")])
    s.append(paragraph("Projeto Recomeço", "GuideTitle"))
    s.append(Spacer(1, 7 * mm))
    s.append(
        paragraph(
            "Teoria, arquitetura, exemplos reais, comandos, código Java, comparações e operação do laboratório",
            "GuideSubtitle",
        )
    )
    s.append(Spacer(1, 18 * mm))
    s.append(
        callout(
            "Escopo",
            "IAM e STS, AWS CLI, S3, RDS PostgreSQL, SQS, DLQ, SNS, Lambda Java, SES, CloudWatch, custos e integração com a aplicação Spring Boot.",
            GREEN,
        )
    )
    s.append(Spacer(1, 12 * mm))
    s.append(paragraph("Edição de estudo - 5 de setembro de 2026", "GuideSubtitle"))
    s.append(paragraph("Preparado para Ricardo", "GuideSubtitle"))
    s.append(PageBreak())

    # Como usar
    s.append(h1("Como usar este guia"))
    s.append(
        paragraph(
            "Este material registra o que foi estudado e implementado no laboratório. Ele não tenta reproduzir uma arquitetura de produção. As escolhas priorizam aprendizado, código simples, comentários e custo baixo. Quando uma prática de produção for diferente, ela é indicada explicitamente."
        )
    )
    s.extend(
        bullets(
            [
                "Leia as seções na ordem para reconstruir a jornada de aprendizado.",
                "Use as caixas de referência rápida quando precisar relembrar um conceito.",
                "Os nomes dos recursos e configurações refletem o ambiente real do Projeto Recomeço.",
                "Comandos que alteram ou excluem recursos devem ser executados somente depois de conferir região, conta e identificadores.",
                "Preços e regras de plano gratuito podem mudar; confirme sempre no console AWS antes de criar recursos.",
            ]
        )
    )
    s.append(
        callout(
            "Estado atual",
            "A aplicação roda localmente. O banco local permite continuar trabalhando sem RDS. O bucket S3, as filas SQS, o tópico SNS, a Lambda, o SES e o CloudWatch formam o fluxo AWS ativo. A instância RDS está parada temporariamente.",
            YELLOW,
        )
    )
    s.append(PageBreak())

    # Índice automático
    s.append(paragraph("Índice", "GuideTocTitle"))
    toc = TableOfContents()
    toc.levelStyles = [
        ParagraphStyle(
            name="TOC1",
            fontName="Arial-Bold",
            fontSize=10,
            leading=15,
            leftIndent=0,
            firstLineIndent=0,
            textColor=NAVY,
            spaceBefore=4,
        ),
        ParagraphStyle(
            name="TOC2",
            fontName="Arial",
            fontSize=8.5,
            leading=12,
            leftIndent=10 * mm,
            firstLineIndent=0,
            textColor=DARK_GREY,
        ),
    ]
    s.append(toc)
    s.append(PageBreak())

    # 1
    s.append(h1("1. Fundamentos da AWS e da computação em nuvem"))
    s.append(h2("1.1 O que é computação em nuvem"))
    s.append(
        paragraph(
            "Computação em nuvem é o uso de recursos de computação fornecidos sob demanda por um provedor. Em vez de comprar e manter todos os servidores, discos, redes e data centers, uma equipe cria recursos por console, linha de comando ou código, usa pelo tempo necessário e paga conforme as regras do serviço."
        )
    )
    s.append(
        data_table(
            ["Modelo", "Responsabilidade principal", "Exemplo"],
            [
                ["IaaS", "A equipe administra sistema operacional e aplicação.", "Máquina virtual EC2."],
                ["Serviço gerenciado", "A AWS administra grande parte da infraestrutura.", "RDS, SQS e SNS."],
                ["Serverless", "Não há servidor da aplicação para manter continuamente.", "Lambda."],
            ],
            [27 * mm, 83 * mm, 58 * mm],
        )
    )
    s.append(h2("1.2 Região, zona de disponibilidade e endpoint"))
    s.extend(
        bullets(
            [
                "Região é uma área geográfica da AWS. Nosso laboratório usa sa-east-1, São Paulo.",
                "Zona de disponibilidade é uma instalação isolada dentro da região. O RDS foi criado em uma única zona, por economia.",
                "Endpoint é o endereço de rede de um recurso, como o hostname do RDS ou a URL de uma fila SQS.",
                "Recursos normalmente são regionais. Criar ou procurar na região errada é uma causa comum de confusão.",
            ]
        )
    )
    s.append(h2("1.3 Responsabilidade compartilhada"))
    s.append(
        paragraph(
            "A AWS protege a infraestrutura física e os serviços gerenciados. O cliente continua responsável por identidades, permissões, dados, configurações de rede e uso correto da aplicação. Um bucket privado pode se tornar público por uma política errada; um banco gerenciado ainda pode ser exposto por um Security Group permissivo."
        )
    )
    s.append(h2("1.4 ARN e identificadores"))
    s.append(
        paragraph(
            "ARN significa Amazon Resource Name. É o nome completo e inequívoco de um recurso dentro da AWS. Políticas IAM usam ARNs para definir exatamente onde uma ação é permitida."
        )
    )
    s.append(code_block("arn:aws:s3:::recomeco-pedidos-anexos-df33a1eb/pedidos/*\narn:aws:sqs:sa-east-1:033649548808:recomeco-relatorios-pedidos-dev\narn:aws:sns:sa-east-1:033649548808:recomeco-eventos-relatorios-dev"))

    # 2
    s.append(h1("2. Conta, segurança inicial e controle de custos"))
    s.append(h2("2.1 Usuário root, IAM e MFA"))
    s.append(
        paragraph(
            "O usuário root representa o proprietário integral da conta. Ele deve ser reservado para tarefas que realmente exigem esse nível. Criamos o usuário administrativo admin-estudo e protegemos o acesso com autenticação multifator no Google Authenticator."
        )
    )
    s.append(
        callout(
            "Regra prática",
            "MFA protege o login. IAM define o que a identidade pode fazer depois do login. São controles complementares.",
        )
    )
    s.append(h2("2.2 Créditos e orçamento"))
    s.append(
        paragraph(
            "A conta informou US$ 100 em créditos e 160 dias restantes no momento da configuração. Esses valores mudam com o uso e com o tempo. Um orçamento foi criado para alertar sobre consumo; orçamento não bloqueia automaticamente cobranças, a menos que ações específicas sejam configuradas."
        )
    )
    s.extend(
        bullets(
            [
                "Consultar Billing e Budgets regularmente.",
                "Verificar a estimativa antes de criar RDS, NAT Gateway, balanceador ou recursos contínuos.",
                "Parar RDS suspende horas da instância, mas armazenamento e backups ainda podem gerar custo.",
                "Uma instância RDS parada reinicia automaticamente depois de até sete dias.",
                "Excluir um recurso é diferente de pará-lo; antes de excluir banco, avaliar snapshot manual.",
            ]
        )
    )

    # 3
    s.append(h1("3. IAM, roles, políticas, STS e AWS CLI"))
    s.append(h2("3.1 Usuário, role e política"))
    s.append(
        data_table(
            ["Conceito", "Função"],
            [
                ["IAM user", "Identidade persistente para uma pessoa ou integração legada."],
                ["Role", "Conjunto de permissões assumido temporariamente por pessoa, aplicação ou serviço AWS."],
                ["Policy", "Documento JSON que permite ou nega ações em recursos."],
                ["STS", "Serviço que emite credenciais temporárias para uma sessão."],
                ["Execution role", "Role usada pela Lambda durante a execução."],
            ],
            [40 * mm, 128 * mm],
        )
    )
    s.append(h2("3.2 Política anexada e política inline"))
    s.append(
        paragraph(
            "Uma política gerenciada pode ser anexada a várias identidades. Uma política inline pertence diretamente a uma identidade. No laboratório usamos ambas: a política de S3 aparece como customer managed; as permissões específicas de SQS, SNS e Lambda podem ficar inline para manter o vínculo simples."
        )
    )
    s.append(h2("3.3 Perfil projeto-s3 e sessão temporária"))
    s.append(
        paragraph(
            "A aplicação local usa o perfil projeto-s3. Esse perfil assume a role ProjetoSpringBootS3DevRole. A autenticação gera uma sessão temporária; quando expira, o SDK informa que é necessário autenticar novamente. Não armazenamos access key e secret key permanentes no projeto."
        )
    )
    s.append(code_block("# Ver a origem das credenciais do perfil\naws configure list --profile projeto-s3\n\n# Conferir conta e role assumida\naws sts get-caller-identity --profile projeto-s3\n\n# Autenticar o perfil-base quando a sessão expirar\naws login --profile default"))
    s.append(
        callout(
            "Por que a Lambda não usa projeto-s3?",
            "Dentro da AWS, o runtime fornece credenciais temporárias da execution role. S3Client.create() e SesV2Client.create() encontram essas credenciais automaticamente.",
            GREEN,
        )
    )

    # 4
    s.append(h1("4. Amazon S3 - armazenamento de objetos"))
    s.append(h2("4.1 Conceitos fundamentais"))
    s.append(
        data_table(
            ["Conceito", "Significado no projeto"],
            [
                ["Bucket", "Contêiner globalmente nomeado: recomeco-pedidos-anexos-df33a1eb."],
                ["Key", "Endereço textual do objeto dentro do bucket."],
                ["Objeto", "Conteúdo binário mais metadados, identificado por bucket + key."],
                ["Prefixo", "Parte inicial da key usada para organização e filtragem."],
                ["Metadados", "Content-Type, tamanho, ETag e outras informações do objeto."],
            ],
            [37 * mm, 131 * mm],
        )
    )
    s.append(code_block("bucket + key = objeto\n\nrecomeco-pedidos-anexos-df33a1eb\n+ pedidos/1/anexos/38e48370-bdc5-49f0-a3a6-47e8146af769\n= objeto armazenado"))
    s.append(
        paragraph(
            "O S3 não possui pastas reais como o Windows. A barra faz parte da key. O console interpreta prefixos como pastas para facilitar a navegação."
        )
    )
    s.append(h2("4.2 Operações estudadas"))
    s.append(
        data_table(
            ["Operação", "Uso"],
            [
                ["PUT", "Envia ou substitui um objeto."],
                ["HEAD", "Consulta metadados sem baixar o conteúdo."],
                ["GET", "Baixa o conteúdo do objeto."],
                ["DELETE", "Exclui o objeto."],
                ["URL pré-assinada", "Autoriza GET temporário sem tornar o bucket público."],
            ],
            [39 * mm, 129 * mm],
        )
    )
    s.append(h2("4.3 Segurança e integridade"))
    s.extend(
        bullets(
            [
                "Object Ownership: BucketOwnerEnforced.",
                "Criptografia padrão: AES256, também chamada SSE-S3.",
                "BlockPublicAcls, IgnorePublicAcls, BlockPublicPolicy e RestrictPublicBuckets estão habilitados.",
                "SHA-256 é calculado pela aplicação e armazenado no banco para comprovar integridade.",
                "ETag identifica a versão armazenada, mas não deve ser tratado universalmente como SHA-256 ou MD5.",
            ]
        )
    )
    s.append(code_block("Get-FileHash $testFile -Algorithm SHA256\nGet-FileHash $downloadFile -Algorithm SHA256\n\n# Se os hashes forem iguais, os bytes enviados e baixados são iguais."))
    s.append(h2("4.4 URL pré-assinada"))
    s.append(
        paragraph(
            "Uma URL pré-assinada contém uma assinatura temporária. Quem possui a URL pode executar a ação autorizada até a expiração. Depois disso, o S3 responde AccessDenied e Request has expired. O objeto continua privado; apenas aquela operação recebeu autorização limitada."
        )
    )
    s.append(h2("4.5 S3 versus banco relacional para imagens"))
    s.append(
        data_table(
            ["Aspecto", "S3", "Banco relacional"],
            [
                ["Uso principal", "Arquivos e objetos binários.", "Dados estruturados e relacionamentos."],
                ["Escala", "Muito apropriado para grande volume de arquivos.", "BLOBs podem aumentar backup, replicação e I/O."],
                ["Consulta", "Busca por bucket e key.", "SQL, índices, filtros e junções."],
                ["Padrão comum", "Arquivo no S3; metadados e key no banco.", "Registro referencia o objeto externo."],
            ],
            [32 * mm, 67 * mm, 69 * mm],
        )
    )

    # 5
    s.append(h1("5. S3 dentro da aplicação Spring Boot"))
    s.append(h2("5.1 Configuração"))
    s.append(code_block("application:\n  aws:\n    s3:\n      bucket-name: recomeco-pedidos-anexos-df33a1eb\n      region: sa-east-1\n      credentials-profile: projeto-s3"))
    s.append(
        paragraph(
            "AwsS3Properties recebe as configurações. AwsS3Config cria os beans AwsCredentialsProvider, S3Client e S3Presigner. O serviço de armazenamento usa esses clientes; AnexoPedidoService coordena banco e S3."
        )
    )
    s.append(h2("5.2 Endpoints reais de anexos"))
    s.append(
        data_table(
            ["Método", "Endpoint", "Resultado"],
            [
                ["POST", "/pedidos/{pedidoId}/anexos", "Envia multipart, grava S3 e metadados."],
                ["GET", "/pedidos/{pedidoId}/anexos", "Lista metadados dos anexos."],
                ["GET", "/pedidos/{pedidoId}/anexos/{anexoId}/download", "Cria URL temporária."],
                ["DELETE", "/pedidos/{pedidoId}/anexos/{anexoId}", "Exclui registro e objeto correspondente."],
            ],
            [20 * mm, 91 * mm, 57 * mm],
        )
    )
    s.append(
        callout(
            "pedidoId e anexoId não são a mesma coisa",
            "Em /pedidos/3/anexos/33, o primeiro número identifica o pedido e o último identifica o anexo. Usar 3 no lugar de 33 retorna Anexo não encontrado.",
            YELLOW,
        )
    )
    s.append(h2("5.3 Formato das keys"))
    s.append(code_block("pedidos/{pedidoId}/anexos/{uuid}\npedidos/{pedidoId}/relatorios/{solicitacaoId}.pdf"))

    # 6
    s.append(h1("6. Amazon RDS - banco relacional gerenciado"))
    s.append(h2("6.1 O que o RDS administra"))
    s.append(
        paragraph(
            "O Amazon RDS executa um mecanismo relacional em infraestrutura AWS. A aplicação ainda usa JDBC e SQL normalmente, mas a AWS automatiza criação da instância, substituição de hardware, backups, snapshots, manutenção, métricas e parte da recuperação. O usuário não recebe acesso administrativo ao sistema operacional da máquina."
        )
    )
    s.append(h2("6.2 Mecanismos"))
    s.append(
        paragraph(
            "RDS oferece PostgreSQL, MySQL, MariaDB, Oracle, Microsoft SQL Server e Db2 conforme região e plano. Aurora é um mecanismo da AWS compatível com MySQL ou PostgreSQL e possui arquitetura diferente. Para nosso laboratório escolhemos PostgreSQL, o elefante."
        )
    )
    s.append(h2("6.3 CPU, memória, rede e armazenamento"))
    s.extend(
        bullets(
            [
                "A classe db.t4g.micro define 2 vCPUs, 1 GiB de RAM e limites de rede/EBS.",
                "O armazenamento foi configurado como General Purpose SSD gp2, 20 GiB.",
                "Single-AZ significa uma única instância sem standby em outra zona.",
                "Multi-AZ adicionaria redundância e recuperação automática, mas aumentaria o custo.",
                "A capacidade percebida é combinação de CPU, memória, cache, disco, IOPS, rede e padrão de consultas.",
            ]
        )
    )
    s.append(h2("6.4 Backup, snapshot e restauração"))
    s.append(
        data_table(
            ["Recurso", "Características", "Uso típico"],
            [
                ["Backup automático", "Gerenciado, retenção definida e recuperação no tempo.", "Proteção contínua."],
                ["Snapshot manual", "Criado sob demanda e mantido até exclusão.", "Antes de mudança arriscada ou exclusão."],
                ["Restauração", "Cria outra instância; não volta por cima da original.", "Teste ou recuperação."],
            ],
            [35 * mm, 76 * mm, 57 * mm],
        )
    )
    s.append(h2("6.5 Manutenção e logs"))
    s.extend(
        bullets(
            [
                "Auto minor version upgrade: habilitado.",
                "Janela de manutenção observada: 22:31 a 23:01 no horário UTC-03:00.",
                "Backups automáticos: habilitados com retenção de 1 dia.",
                "Logs PostgreSQL são rotacionados e podem ser vistos no console RDS.",
                "Mensagens checkpoint starting e checkpoint complete são rotinas normais de persistência do PostgreSQL.",
            ]
        )
    )

    # 7
    s.append(h1("7. Nosso laboratório RDS PostgreSQL"))
    s.append(h2("7.1 Recurso criado"))
    s.append(
        data_table(
            ["Item", "Valor"],
            [
                ["Identificador", "recomeco-postgresql-dev"],
                ["Mecanismo", "PostgreSQL 18.3"],
                ["Classe", "db.t4g.micro"],
                ["Região e zona", "sa-east-1 / sa-east-1a"],
                ["Banco", "projetodb"],
                ["Usuário", "adminprojeto"],
                ["Porta", "5432"],
                ["Acesso", "Publicamente acessível, limitado por Security Group /32"],
                ["Estado atual", "Parado temporariamente"],
            ],
            [44 * mm, 124 * mm],
        )
    )
    s.append(h2("7.2 Conexão segura"))
    s.append(
        paragraph(
            "O modo verify-full exige criptografia e valida se o certificado corresponde ao hostname acessado. O pacote de certificados foi movido para C:\\Recomeco\\projetos\\aws\\rds\\certificado. DBeaver e aplicação precisam apontar para o novo caminho."
        )
    )
    s.append(code_block("$env:SPRING_PROFILES_ACTIVE = \"rds\"\n$env:RDS_HOST = \"recomeco-postgresql-dev.c7g2qsu0ub80.sa-east-1.rds.amazonaws.com\"\n$env:RDS_PORT = \"5432\"\n$env:RDS_DATABASE = \"projetodb\"\n$env:RDS_USERNAME = \"adminprojeto\"\n$env:RDS_PASSWORD = \"<senha armazenada localmente, não colocar no Git>\"\n$env:RDS_SSL_ROOT_CERT = \"C:\\Recomeco\\projetos\\aws\\rds\\certificado\\global-bundle.pem\"\n\n.\\mvnw.cmd spring-boot:run"))
    s.append(h2("7.3 Consultas de diagnóstico comentadas"))
    s.append(code_block("/* Identifica versão, banco, usuário e horário do servidor. */\nSELECT jsonb_pretty(jsonb_build_object(\n    'versao', version(),\n    'banco_atual', current_database(),\n    'usuario_atual', current_user,\n    'agora', now()\n)) AS diagnostico;"))
    s.append(code_block("/* Resume atividade acumulada do banco atual em uma saída JSON. */\nSELECT jsonb_pretty(jsonb_build_object(\n    'banco', datname,\n    'conexoes_atuais', numbackends,\n    'transacoes_confirmadas', xact_commit,\n    'transacoes_desfeitas', xact_rollback,\n    'linhas_inseridas', tup_inserted,\n    'linhas_atualizadas', tup_updated,\n    'linhas_excluidas', tup_deleted,\n    'blocos_lidos_armazenamento', blks_read,\n    'blocos_encontrados_cache', blks_hit,\n    'estatisticas_reiniciadas_em', stats_reset\n)) AS estatisticas\nFROM pg_stat_database\nWHERE datname = current_database();"))
    s.append(code_block("/* Mostra tamanho total do banco em formato legível. */\nSELECT pg_size_pretty(pg_database_size(current_database())) AS tamanho_banco;\n\n/* Mostra dados, índices e total por tabela do schema public. */\nSELECT\n    relname AS tabela,\n    pg_size_pretty(pg_relation_size(relid)) AS dados,\n    pg_size_pretty(pg_indexes_size(relid)) AS indices,\n    pg_size_pretty(pg_total_relation_size(relid)) AS total\nFROM pg_catalog.pg_statio_user_tables\nORDER BY pg_total_relation_size(relid) DESC;"))
    s.append(code_block("/* Instala a extensão somente no banco atual. Pode ser executado novamente. */\nCREATE EXTENSION IF NOT EXISTS pg_stat_statements;\n\n/* Consulta comandos mais executados e seus tempos. */\nSELECT\n    calls AS execucoes,\n    query AS comando_sql,\n    ROUND(mean_exec_time::numeric, 3) AS tempo_medio_ms,\n    ROUND(total_exec_time::numeric, 3) AS tempo_total_ms,\n    rows AS linhas_processadas,\n    shared_blks_read AS blocos_armazenamento,\n    shared_blks_hit AS blocos_cache\nFROM pg_stat_statements\nORDER BY calls DESC\nLIMIT 10;"))
    s.append(h2("7.4 Parar não é excluir"))
    s.append(
        callout(
            "RDS parado",
            "A cobrança de horas da instância é pausada, mas armazenamento provisionado e backups continuam existindo. A AWS reinicia automaticamente a instância após até sete dias. Para parar permanentemente, seria necessário criar snapshot se desejado e excluir a instância.",
            YELLOW,
        )
    )

    # 8
    s.append(h1("8. Mensageria e processamento assíncrono"))
    s.append(h2("8.1 Síncrono e assíncrono"))
    s.append(
        paragraph(
            "Em um fluxo síncrono, quem solicita espera o trabalho terminar. Em um fluxo assíncrono, o produtor registra uma solicitação e continua; outro componente executa o trabalho depois. O SqsClient usado no Java é síncrono porque cada chamada do SDK bloqueia até obter resposta, embora a arquitetura baseada na fila seja assíncrona."
        )
    )
    s.append(h2("8.2 Producer, consumer e job"))
    s.append(
        data_table(
            ["Papel", "Responsabilidade no projeto"],
            [
                ["Producer", "Transforma o DTO em JSON e envia para a fila."],
                ["Fila", "Mantém a mensagem até ser recebida e excluída."],
                ["Job", "Consulta periodicamente a fila."],
                ["Consumer", "Converte e processa a mensagem."],
                ["Serviço", "Executa a regra de negócio: gerar PDF, gravar S3 e atualizar banco."],
            ],
            [38 * mm, 130 * mm],
        )
    )
    s.append(h2("8.3 Entrega pelo menos uma vez"))
    s.append(
        paragraph(
            "SQS Standard trabalha com entrega pelo menos uma vez. Uma mensagem pode reaparecer. Por isso o processamento deve ser idempotente: repetir a mesma solicitação não deve multiplicar efeitos indevidamente. No relatório, a solicitaçãoId e a regra de reutilização evitam criar vários relatórios para o mesmo pedido sem necessidade."
        )
    )

    # 9
    s.append(h1("9. Amazon SQS e DLQ"))
    s.append(h2("9.1 Filas criadas"))
    s.append(
        data_table(
            ["Uso", "Nome"],
            [
                ["Geração de relatório", "recomeco-relatorios-pedidos-dev"],
                ["Mensagens problemáticas", "recomeco-relatorios-pedidos-dlq-dev"],
                ["Auditoria de eventos SNS", "recomeco-auditoria-relatorios-dev"],
            ],
            [57 * mm, 111 * mm],
        )
    )
    s.append(h2("9.2 Ciclo de vida da mensagem"))
    s.append(code_block("Producer -> SendMessage -> mensagem disponível\nConsumer -> ReceiveMessage -> mensagem invisível temporariamente\nSucesso -> DeleteMessage usando ReceiptHandle\nFalha -> não exclui -> visibilidade expira -> mensagem volta\nRecebimentos excedem maxReceiveCount -> SQS move para a DLQ"))
    s.append(
        paragraph(
            "MessageId identifica a mensagem. ReceiptHandle identifica aquele recebimento e é usado para exclusão. ApproximateReceiveCount mostra aproximadamente quantas vezes a mensagem foi entregue. Visibility Timeout evita que dois consumidores trabalhem simultaneamente na mesma mensagem durante o processamento normal."
        )
    )
    s.append(h2("9.3 Long polling"))
    s.append(
        paragraph(
            "wait-time-seconds=20 permite que ReceiveMessage aguarde por mensagens, reduzindo consultas vazias. O job espera cinco segundos entre execuções. Long polling não transforma o SQS em push; o consumidor ainda toma a iniciativa de consultar."
        )
    )
    s.append(h2("9.4 Comandos de referência"))
    s.append(code_block("$queueUrl = \"https://sqs.sa-east-1.amazonaws.com/033649548808/recomeco-relatorios-pedidos-dev\"\n\n# Envia uma mensagem.\naws sqs send-message --profile projeto-s3 --region sa-east-1 `\n  --queue-url $queueUrl `\n  --message-body '{\"tipo\":\"TESTE_CONEXAO\",\"origem\":\"PowerShell\"}'\n\n# Recebe até uma mensagem e solicita todos os atributos do sistema.\n$response = aws sqs receive-message --profile projeto-s3 --region sa-east-1 `\n  --queue-url $queueUrl --max-number-of-messages 1 `\n  --wait-time-seconds 20 --message-system-attribute-names All | ConvertFrom-Json\n\n# Exclui a mensagem depois do processamento bem-sucedido.\naws sqs delete-message --profile projeto-s3 --region sa-east-1 `\n  --queue-url $queueUrl --receipt-handle $response.Messages[0].ReceiptHandle"))
    s.append(h2("9.5 DLQ"))
    s.append(
        paragraph(
            "A Dead-Letter Queue não corrige mensagens; ela isola as que falharam repetidamente. A mensagem permanece conforme o período de retenção configurado, até expirar, ser excluída ou ser redirecionada. Receber uma mensagem da DLQ não a exclui; por isso o mesmo MessageId apareceu várias vezes até executarmos DeleteMessage."
        )
    )

    # 10
    s.append(h1("10. Amazon SNS - tópico e distribuição"))
    s.append(h2("10.1 Tópico não é fila"))
    s.append(
        paragraph(
            "O SNS recebe uma publicação e distribui cópias para assinaturas. Ele não mantém uma fila independente para cada consumidor por conta própria. Quando uma assinatura aponta para SQS, a cópia passa a ficar armazenada naquela fila."
        )
    )
    s.append(code_block("RelatorioPedidoSnsPublisher.publicar(evento)\n        -> snsClient.publish(request)\n        -> tópico recomeco-eventos-relatorios-dev\n             -> assinatura EMAIL: Gmail recebe JSON bruto\n             -> assinatura SQS: fila de auditoria\n             -> assinatura LAMBDA: Lambda Java é executada"))
    s.append(h2("10.2 Momento do disparo"))
    s.append(
        paragraph(
            "ProcessadorRelatorioPedidoService só publica depois que o PDF foi enviado ao S3, o relatório foi marcado como DISPONIVEL e o registro foi salvo. Assim o evento anuncia um fato já concluído, e não uma intenção."
        )
    )
    s.append(code_block("storageService.upload(...);                 // PDF confirmado no S3\nrelatorio.marcarComoDisponivel(...);         // estado em memória\nrelatorioPedidoRepository.save(relatorio);   // estado confirmado no banco\npublicarEventoRelatorioDisponivel(relatorio);// publica no SNS"))
    s.append(h2("10.3 Auditoria"))
    s.append(
        paragraph(
            "A fila recomeco-auditoria-relatorios-dev recebe uma cópia bruta do evento. AuditoriaRelatorioSqsJob chama AuditoriaRelatorioSqsConsumer, que converte o JSON, grava AuditoriaRelatorio no banco local e exclui a mensagem em caso de sucesso."
        )
    )

    # 11
    s.append(h1("11. AWS Lambda Java"))
    s.append(h2("11.1 O que significa serverless"))
    s.append(
        paragraph(
            "Serverless não significa ausência de servidores. Significa que não administramos um servidor permanentemente ligado para essa função. A AWS cria um ambiente quando necessário, executa o código, registra métricas e pode reutilizar o ambiente em chamadas seguintes."
        )
    )
    s.append(h2("11.2 Função criada"))
    s.append(
        data_table(
            ["Item", "Valor"],
            [
                ["Nome", "recomeco-email-ses-relatorio-dev"],
                ["Runtime", "Java 21"],
                ["Arquitetura", "x86_64"],
                ["Handler", "br.com.exemplo.lambdaemail.EnviarRelatorioEmailHandler::handleRequest"],
                ["Disparo", "Assinatura do tópico SNS"],
                ["Memória observada", "512 MB"],
            ],
            [44 * mm, 124 * mm],
        )
    )
    s.append(h2("11.3 RequestHandler"))
    s.append(code_block("public class EnviarRelatorioEmailHandler\n        implements RequestHandler<SNSEvent, Integer> {\n\n    @Override\n    public Integer handleRequest(SNSEvent evento, Context contexto) {\n        // A AWS chama este método quando o SNS aciona a função.\n    }\n}"))
    s.append(
        paragraph(
            "RequestHandler não é obrigatório, mas deixa os tipos de entrada e saída explícitos. Também existe RequestStreamHandler e é possível configurar um método público compatível. Nossa função não usa Spring; o runtime instancia a classe pelo construtor sem parâmetros."
        )
    )
    s.append(h2("11.4 Classes da função"))
    s.append(
        data_table(
            ["Classe", "Responsabilidade"],
            [
                ["EnviarRelatorioEmailHandler", "Coordena o fluxo."],
                ["LeitorEventoRelatorio", "Converte a String JSON em DTO com Jackson."],
                ["EventoRelatorioDisponivel", "Record que representa o evento."],
                ["LeitorArquivoS3", "Baixa o PDF e devolve byte[]."],
                ["EnviadorEmailSes", "Monta texto, HTML, anexo e chama o SES."],
            ],
            [58 * mm, 110 * mm],
        )
    )
    s.append(h2("11.5 Construtores e injeção sem Spring"))
    s.append(code_block("public EnviarRelatorioEmailHandler() {\n    this(\n        new LeitorEventoRelatorio(),\n        new LeitorArquivoS3(),\n        new EnviadorEmailSes()\n    );\n}\n\n// this(...) chama outro construtor da mesma classe.\n// Nos testes, fornecemos dependências simuladas."))
    s.append(h2("11.6 Cold start e relatório de execução"))
    s.append(
        paragraph(
            "INIT_START indica inicialização do runtime Java. Isso é um cold start. START e END delimitam a chamada. REPORT mostra duração, duração cobrada, memória disponível, memória usada e tempo de inicialização. No teste completo observamos cerca de 4,5 segundos de execução, 512 MB configurados e 187 MB usados."
        )
    )

    # 12
    s.append(h1("12. Amazon SES - e-mail transacional"))
    s.append(h2("12.1 Identidade e ambiente de testes"))
    s.append(
        paragraph(
            "SES exige uma identidade de remetente verificada. Verificamos ricardo.bredes@gmail.com. No ambiente de testes do SES, destinatários também precisam estar verificados. Usamos o mesmo endereço como remetente e destinatário. Uma identidade de domínio seria mais adequada para produção."
        )
    )
    s.append(h2("12.2 Mensagem com anexo"))
    s.append(
        paragraph(
            "EnviadorEmailSes usa a API v2. O e-mail contém assunto, corpo em texto, corpo HTML e um Attachment application/pdf. O SDK recebe os bytes e cuida da codificação Base64. A resposta fornece messageId, que registramos no CloudWatch."
        )
    )
    s.append(code_block("Attachment.builder()\n    .rawContent(SdkBytes.fromByteArray(arquivoPdf))\n    .fileName(\"relatorio-pedido-\" + pedidoId + \".pdf\")\n    .contentType(\"application/pdf\")\n    .contentDisposition(ATTACHMENT)\n    .contentTransferEncoding(BASE64)\n    .build();"))
    s.append(
        callout(
            "Dois e-mails são esperados",
            "A assinatura EMAIL do SNS entrega o JSON bruto. A assinatura Lambda executa o fluxo S3 + SES e entrega o e-mail formatado com PDF. O Gmail inicialmente classificou a mensagem do SES como spam; marcá-la como não spam corrigiu a experiência.",
            GREEN,
        )
    )

    # 13
    s.append(h1("13. Amazon CloudWatch"))
    s.append(h2("13.1 Logs da Lambda"))
    s.append(
        paragraph(
            "LambdaLogger envia nossas mensagens para CloudWatch Logs. A execution role possui a permissão básica para criar grupos e fluxos de log e gravar eventos. O grupo segue normalmente o padrão /aws/lambda/nome-da-funcao."
        )
    )
    s.append(code_block("Lambda iniciada. requestId=...\nE-mail enviado pelo SES. pedidoId=810, relatorioId=16, objectKey=...,\n    tamanhoBytes=1417, mensagemId=...\nLambda concluída. mensagensProcessadas=1\nREPORT RequestId: ... Duration: ... Memory Size: 512 MB Max Memory Used: 187 MB"))
    s.append(h2("13.2 Métricas automáticas"))
    s.extend(
        bullets(
            [
                "Invocations: quantidade de execuções.",
                "Errors: execuções que terminaram com falha.",
                "Duration: tempo de execução.",
                "Throttles: chamadas recusadas por limite de concorrência.",
                "ConcurrentExecutions: execuções simultâneas.",
            ]
        )
    )
    s.append(h2("13.3 CloudWatch versus Prometheus"))
    s.append(
        data_table(
            ["Aspecto", "CloudWatch", "Prometheus + Grafana"],
            [
                ["Origem", "Serviço nativo AWS.", "Ecossistema aberto e independente."],
                ["Coleta", "Integração automática com serviços AWS.", "Scrape de endpoints de métricas."],
                ["Uso atual", "Logs e métricas da Lambda.", "Métricas da aplicação Spring local."],
                ["Visualização", "Console e dashboards CloudWatch.", "Grafana."],
                ["Relação", "Podem coexistir e observar camadas diferentes.", "Não é substituição obrigatória."],
            ],
            [31 * mm, 68 * mm, 69 * mm],
        )
    )
    s.append(
        paragraph(
            "OpenTelemetry complementa ambos ao padronizar traces, métricas e logs. A aplicação já usa observabilidade local; CloudWatch acrescenta visibilidade da parte executada dentro da AWS."
        )
    )

    # 14 - arquitetura em paisagem
    s.append(h1("14. Arquitetura completa do laboratório"))
    s.append(
        paragraph(
            "O desenho a seguir reúne fluxo de dados, configurações e permissões. Setas contínuas representam dados; setas pontilhadas representam configuração ou autorização."
        )
    )
    s.append(NextPageTemplate("Landscape"))
    s.append(PageBreak())
    if DIAGRAM_PATH.exists():
        diagram = Image(str(DIAGRAM_PATH))
        # Mantemos uma pequena folga em relação ao frame da página paisagem.
        # Isso evita que diferenças de arredondamento do ReportLab cortem o diagrama.
        max_width = 255 * mm
        max_height = 168 * mm
        ratio = min(max_width / diagram.imageWidth, max_height / diagram.imageHeight)
        diagram.drawWidth = diagram.imageWidth * ratio
        diagram.drawHeight = diagram.imageHeight * ratio
        s.append(diagram)
    else:
        s.append(callout("Diagrama não encontrado", str(DIAGRAM_PATH), RED))
    s.append(NextPageTemplate("Portrait"))
    s.append(PageBreak())
    s.append(h2("14.1 Sequência completa de um relatório"))
    s.extend(
        bullets(
            [
                "Cliente chama POST /pedidos/{pedidoId}/relatorios.",
                "RelatorioPedidoService cria ou reutiliza a solicitação e o producer envia GerarRelatorioPedidoMensagem.",
                "SQS mantém a mensagem na fila principal.",
                "RelatorioPedidoSqsJob chama o consumer.",
                "ProcessadorRelatorioPedidoService gera o PDF, calcula SHA-256 e envia ao S3.",
                "O banco recebe status DISPONIVEL, nome, tamanho, checksum, ETag e objectKey.",
                "RelatorioPedidoSnsPublisher publica o evento no tópico.",
                "SNS entrega cópia ao e-mail direto, à fila de auditoria e à Lambda.",
                "A auditoria é gravada no banco local.",
                "A Lambda converte o evento, baixa o PDF, envia pelo SES e grava logs no CloudWatch.",
            ]
        )
    )

    # 15
    s.append(h1("15. Comparações com outras tecnologias"))
    s.append(h2("15.1 SQS + SNS versus Kafka"))
    s.append(
        data_table(
            ["Aspecto", "SQS + SNS", "Kafka"],
            [
                ["Modelo", "Fila para trabalho e tópico para fan-out.", "Log distribuído particionado."],
                ["Consumo", "SQS entrega e a mensagem é excluída.", "Consumer mantém offset."],
                ["Releitura", "Não é o uso principal após exclusão.", "Natural enquanto houver retenção."],
                ["Grupos", "Consumidores da mesma fila dividem mensagens.", "Consumers do mesmo grupo dividem partições."],
                ["Operação", "Serviços totalmente gerenciados e simples.", "Cluster ou serviço gerenciado mais complexo."],
                ["Força", "Integração AWS, pouca administração e escala automática.", "Streaming, histórico, alto volume e replay."],
                ["Custo", "Cobrança por uso segundo tabelas AWS.", "Software é gratuito, infraestrutura e operação não são."],
            ],
            [28 * mm, 70 * mm, 70 * mm],
        )
    )
    s.append(
        callout(
            "Kafka ser poderoso não elimina SNS/SQS",
            "A melhor ferramenta depende do problema. Para distribuir um evento e desacoplar consumidores dentro da AWS, SNS e SQS podem exigir muito menos infraestrutura e conhecimento operacional.",
        )
    )
    s.append(h2("15.2 Consumidores do mesmo grupo"))
    s.append(
        paragraph(
            "Se três instâncias pertencem ao mesmo grupo consumidor Kafka, elas dividem as partições e o trabalho; não são três cópias independentes. Grupos diferentes recebem o fluxo independentemente. No SQS, vários consumidores da mesma fila competem pelas mensagens. Para cópias independentes, usamos filas separadas, normalmente alimentadas por SNS."
        )
    )
    s.append(h2("15.3 Lambda versus servidor e batch"))
    s.append(
        data_table(
            ["Aspecto", "Lambda", "Aplicação/servidor", "Batch agendado"],
            [
                ["Vida", "Execução sob demanda.", "Processo contínuo.", "Inicia em horário ou intervalo."],
                ["Disparo", "Evento, API ou agenda.", "Requisições e listeners.", "Agendador."],
                ["Estado", "Preferencialmente sem estado local.", "Pode manter caches e conexões.", "Depende da implementação."],
                ["Nosso uso", "E-mail do relatório.", "Spring Boot local.", "Jobs de polling locais."],
            ],
            [27 * mm, 45 * mm, 50 * mm, 46 * mm],
        )
    )

    # 16
    s.append(h1("16. Permissões e configurações de referência"))
    s.append(h2("16.1 Permissão da Lambda para ler relatórios"))
    s.append(code_block("{\n  \"Version\": \"2012-10-17\",\n  \"Statement\": [{\n    \"Sid\": \"PermitirLeituraDosRelatorios\",\n    \"Effect\": \"Allow\",\n    \"Action\": \"s3:GetObject\",\n    \"Resource\": \"arn:aws:s3:::recomeco-pedidos-anexos-df33a1eb/pedidos/*/relatorios/*\"\n  }]\n}"))
    s.append(h2("16.2 Permissão da Lambda para enviar e-mail"))
    s.append(code_block("{\n  \"Version\": \"2012-10-17\",\n  \"Statement\": [{\n    \"Sid\": \"PermitirEnvioDeEmailPeloSes\",\n    \"Effect\": \"Allow\",\n    \"Action\": \"ses:SendEmail\",\n    \"Resource\": \"arn:aws:ses:sa-east-1:033649548808:identity/ricardo.bredes@gmail.com\"\n  }]\n}"))
    s.append(h2("16.3 Interpretação de Resource"))
    s.append(code_block("arn:aws:s3:::recomeco-pedidos-anexos-df33a1eb/pedidos/*\n\narn:aws:s3:::        -> serviço e formato do ARN\nrecomeco-...          -> bucket\npedidos/              -> prefixo obrigatório\n*                      -> qualquer key abaixo desse prefixo"))
    s.append(h2("16.4 Variáveis e propriedades"))
    s.append(
        data_table(
            ["Componente", "Nome", "Finalidade"],
            [
                ["Spring S3", "bucket-name / region / credentials-profile", "Bucket, região e perfil local."],
                ["Spring SQS", "queue-url / audit-queue-url", "Filas principal e auditoria."],
                ["Spring SQS", "enabled / consumer-enabled", "Liga addon e consumer separadamente."],
                ["Spring SNS", "topic-arn / enabled", "Destino e chave geral do addon."],
                ["Lambda", "BUCKET_RELATORIOS", "Bucket usado pelo LeitorArquivoS3."],
                ["Lambda", "EMAIL_REMETENTE", "Identidade verificada no SES."],
                ["Lambda", "EMAIL_DESTINATARIO", "Destinatário do relatório."],
                ["RDS", "RDS_HOST, RDS_PORT, RDS_DATABASE", "Endereço lógico do banco."],
                ["RDS", "RDS_USERNAME, RDS_PASSWORD", "Credenciais do PostgreSQL."],
                ["RDS", "RDS_SSL_ROOT_CERT", "Pacote de certificados para verify-full."],
            ],
            [33 * mm, 65 * mm, 70 * mm],
        )
    )

    # 17
    s.append(h1("17. Guia de requisições reais"))
    s.append(h2("17.1 Anexos"))
    s.append(code_block("POST   /ProjetoSpringBoot/pedidos/{pedidoId}/anexos\nGET    /ProjetoSpringBoot/pedidos/{pedidoId}/anexos\nGET    /ProjetoSpringBoot/pedidos/{pedidoId}/anexos/{anexoId}/download\nDELETE /ProjetoSpringBoot/pedidos/{pedidoId}/anexos/{anexoId}"))
    s.append(
        paragraph(
            "No upload, o corpo é multipart/form-data com o campo de arquivo esperado pelo controller. A requisição precisa do token JWT quando o endpoint está protegido. O Content-Type do arquivo é preservado, permitindo texto, PNG, JPG e GIF quando aceitos pela validação."
        )
    )
    s.append(h2("17.2 Relatórios e auditoria"))
    s.append(code_block("POST /ProjetoSpringBoot/pedidos/{pedidoId}/relatorios\nGET  /ProjetoSpringBoot/pedidos/{pedidoId}/relatorios\nGET  /ProjetoSpringBoot/pedidos/{pedidoId}/relatorios/{relatorioId}/download\nGET  /ProjetoSpringBoot/pedidos/{pedidoId}/auditorias-relatorios"))
    s.append(
        data_table(
            ["Endpoint", "Semântica"],
            [
                ["POST relatórios", "Solicita ou reutiliza a geração; pode retornar ENFILEIRADO."],
                ["GET relatórios", "Consulta o histórico e o estado atual."],
                ["GET download", "Gera URL S3 pré-assinada para um relatório DISPONIVEL."],
                ["GET auditorias", "Consulta eventos consumidos da fila de auditoria."],
            ],
            [77 * mm, 91 * mm],
        )
    )

    # 18
    s.append(h1("18. Diagnóstico e solução de problemas"))
    s.append(
        data_table(
            ["Sintoma", "Verificação", "Ação provável"],
            [
                ["Your session has expired", "aws sts get-caller-identity", "Executar aws login no perfil-base e repetir."],
                ["Fila não recebe", "URL, região, role e SendMessage.", "Corrigir propriedade ou policy."],
                ["Mensagem reaparece", "ApproximateReceiveCount e logs.", "Garantir DeleteMessage só após sucesso."],
                ["Mensagem foi para DLQ", "Ler Body e erro do consumer.", "Corrigir causa; excluir ou redirecionar depois."],
                ["Lambda não executa", "Assinatura SNS e permissão InvokeFunction.", "Recriar/ativar o trigger."],
                ["S3 AccessDenied na Lambda", "Resource e objectKey.", "Conferir s3:GetObject e prefixo."],
                ["SES AccessDenied", "Execution role.", "Conferir ses:SendEmail e identidade ARN."],
                ["SES aceitou, Gmail não mostra", "messageId no CloudWatch.", "Ver Spam, Todos os e-mails e supressão."],
                ["RDS não resolve hostname", "Copiar endpoint sem xxxxx ou erro de digitação.", "Test-NetConnection com hostname correto."],
                ["RDS porta fecha", "Public access e Security Group.", "Atualizar regra 5432 para IP atual /32."],
                ["IntelliJ usa banco local", "Perfil ativo e variáveis.", "Ativar rds explicitamente somente quando desejado."],
            ],
            [48 * mm, 58 * mm, 62 * mm],
        )
    )
    s.append(h2("18.1 Leitura rápida do log Lambda"))
    s.append(code_block("INIT_START -> runtime iniciou; pode indicar cold start\nSTART      -> começou uma invocação\nlogs nossos -> etapas concluídas pelo código\nEND        -> handler terminou\nREPORT     -> duração, memória e cobrança\nERROR      -> exceção não tratada\nTask timed out -> tempo limite insuficiente"))

    # 19
    s.append(h1("19. Custos, parada e limpeza"))
    s.append(
        data_table(
            ["Serviço", "O que observar", "Ação de economia"],
            [
                ["S3", "Armazenamento, requisições e transferência.", "Excluir objetos de teste sem utilidade."],
                ["RDS", "Horas, armazenamento e backups.", "Manter parado durante pausa; lembrar reinício em 7 dias."],
                ["SQS", "Quantidade de requisições e retenção.", "Desligar polling local quando não estiver estudando."],
                ["SNS", "Publicações e entregas.", "Evitar testes repetitivos desnecessários."],
                ["Lambda", "Invocações, duração e memória.", "Evitar loops e erros com repetição contínua."],
                ["SES", "Quantidade e volume total das mensagens.", "Usar PDFs pequenos no laboratório."],
                ["CloudWatch", "Ingestão e retenção de logs.", "Definir retenção curta para logs de estudo."],
            ],
            [30 * mm, 65 * mm, 73 * mm],
        )
    )
    s.append(
        callout(
            "Checklist antes de encerrar uma sessão",
            "Verificar DLQs, interromper aplicação e jobs locais, conferir RDS parado, olhar Budgets, evitar snapshots manuais sem necessidade e confirmar que não existem recursos de teste inesperados em outra região.",
            YELLOW,
        )
    )

    # 20
    s.append(h1("20. Próximos passos sugeridos"))
    s.extend(
        bullets(
            [
                "Criar alarmes CloudWatch simples para Errors da Lambda e mensagens visíveis na DLQ.",
                "Definir retenção do grupo de logs da Lambda para evitar crescimento indefinido.",
                "Estudar eventos de entrega do SES e métricas de envio, rejeição e bounce.",
                "Avaliar uma DLQ para a assinatura SNS -> Lambda.",
                "Criar métrica de negócio, como relatórios enviados com sucesso.",
                "Versionar esquema do banco com Flyway antes de mudanças maiores.",
                "Mais adiante, empacotar a aplicação em ECR e executá-la em ECS, mantendo as integrações atuais independentes.",
                "Experimentar infraestrutura como código somente depois de dominar os recursos manualmente.",
            ]
        )
    )
    s.append(
        callout(
            "Tecnologias ainda não implementadas",
            "EC2, ECR, ECS, API Gateway, Aurora e outras tecnologias discutidas continuam no radar, mas não fazem parte do fluxo funcional documentado aqui.",
        )
    )

    # 21
    s.append(h1("21. Glossário rápido"))
    glossary = [
        ["ARN", "Nome globalmente inequívoco de um recurso AWS."],
        ["At-least-once", "Modelo no qual a mesma mensagem pode ser entregue novamente."],
        ["Bucket", "Contêiner de objetos do S3."],
        ["Cold start", "Inicialização de um novo ambiente de execução Lambda."],
        ["Consumer", "Componente que recebe e processa mensagens."],
        ["DLQ", "Fila que isola mensagens após falhas repetidas."],
        ["Endpoint", "Endereço de acesso a um serviço ou recurso."],
        ["ETag", "Identificador retornado pelo S3 para o objeto armazenado."],
        ["Idempotência", "Repetir a operação produz o mesmo efeito final desejado."],
        ["Key", "Identificador completo de um objeto dentro do bucket."],
        ["Long polling", "ReceiveMessage aguarda por mensagem antes de retornar vazio."],
        ["MFA", "Segundo fator usado além da senha."],
        ["ObjectKey", "Nome usado no código para representar a key do S3."],
        ["Policy", "Documento de permissão IAM."],
        ["Prefixo", "Início compartilhado por várias keys S3."],
        ["Producer", "Componente que publica ou envia uma mensagem."],
        ["ReceiptHandle", "Identificador de um recebimento SQS usado para excluir a mensagem."],
        ["Role", "Identidade de permissões assumida temporariamente."],
        ["Serverless", "Execução sem administrar um servidor de aplicação contínuo."],
        ["Snapshot", "Cópia persistente sob demanda do estado do banco."],
        ["Tópico", "Ponto de publicação SNS que distribui para assinaturas."],
        ["Visibility Timeout", "Período em que uma mensagem recebida fica invisível."],
    ]
    s.append(data_table(["Termo", "Definição"], glossary, [42 * mm, 126 * mm]))

    # Referências
    s.append(h1("22. Referências oficiais"))
    s.extend(
        bullets(
            [
                "AWS IAM: https://docs.aws.amazon.com/iam/",
                "Amazon S3: https://docs.aws.amazon.com/s3/",
                "Amazon RDS: https://docs.aws.amazon.com/rds/",
                "Amazon SQS: https://docs.aws.amazon.com/sqs/",
                "Amazon SNS: https://docs.aws.amazon.com/sns/",
                "AWS Lambda Java: https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html",
                "Amazon SES e anexos: https://docs.aws.amazon.com/ses/latest/dg/attachments.html",
                "Amazon CloudWatch: https://docs.aws.amazon.com/cloudwatch/",
                "AWS CLI: https://docs.aws.amazon.com/cli/",
            ]
        )
    )
    s.append(
        paragraph(
            "Fim do guia. Este documento representa o estado do laboratório em 5 de setembro de 2026. Recursos, políticas e preços devem ser reconferidos no console antes de uso fora do ambiente de estudo.",
            "GuideSmall",
        )
    )
    return s


def build_pdf() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    portrait_size = A4
    landscape_size = landscape(A4)

    portrait_frame = Frame(
        18 * mm,
        15 * mm,
        portrait_size[0] - 36 * mm,
        portrait_size[1] - 31 * mm,
        id="portrait-frame",
        topPadding=4 * mm,
        bottomPadding=4 * mm,
    )
    landscape_frame = Frame(
        15 * mm,
        14 * mm,
        landscape_size[0] - 30 * mm,
        landscape_size[1] - 29 * mm,
        id="landscape-frame",
        topPadding=4 * mm,
        bottomPadding=4 * mm,
    )

    doc = GuideDocument(
        str(OUTPUT_PDF),
        pagesize=portrait_size,
        title="Guia de Referência AWS - Projeto Recomeço",
        author="Projeto Recomeço",
        subject="Guia técnico e didático do laboratório AWS",
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=17 * mm,
        bottomMargin=15 * mm,
    )
    doc.addPageTemplates(
        [
            PageTemplate(
                id="Portrait",
                pagesize=portrait_size,
                frames=[portrait_frame],
                onPage=page_decoration,
            ),
            PageTemplate(
                id="Landscape",
                pagesize=landscape_size,
                frames=[landscape_frame],
                onPage=page_decoration,
            ),
        ]
    )
    doc.multiBuild(build_story())


if __name__ == "__main__":
    build_pdf()
    print(OUTPUT_PDF)
