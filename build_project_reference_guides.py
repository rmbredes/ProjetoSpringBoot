"""Gera quatro guias de referencia do Projeto Recomeco em formato PDF."""

from __future__ import annotations

import re
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import Frame, PageBreak, PageTemplate, Spacer
from reportlab.platypus.tableofcontents import TableOfContents

from build_aws_reference_guide import (
    BLUE,
    DARK_GREY,
    GREEN,
    LIGHT_BLUE,
    NAVY,
    YELLOW,
    GuideDocument,
    bullets as base_bullets,
    callout as base_callout,
    code_block,
    data_table as base_data_table,
    h1 as base_h1,
    h2 as base_h2,
    h3 as base_h3,
    paragraph as base_paragraph,
    styles,
)


BASE_DIR = Path(__file__).resolve().parent
OUTPUT_DIR = BASE_DIR / "output" / "pdf"


# O código-fonte deste gerador usa somente caracteres simples em muitos trechos
# para facilitar sua manutenção. Antes de montar os parágrafos, aplicamos as
# principais acentuações do texto em português. Blocos de código não passam por
# esta função, pois comandos, caminhos e identificadores devem permanecer exatos.
_ACCENTS = {
    "Pagina": "Página", "pagina": "página", "Recomeco": "Recomeço",
    "Colecao": "Coleção", "colecao": "coleção", "Visao": "Visão", "visao": "visão",
    "aplicacao": "aplicação", "aplicacoes": "aplicações", "requisicao": "requisição",
    "requisicoes": "requisições", "explicacao": "explicação", "configuracao": "configuração",
    "configuracoes": "configurações", "alteracao": "alteração", "alteracoes": "alterações",
    "execucao": "execução", "execucoes": "execuções", "relacao": "relação",
    "informacao": "informação", "informacoes": "informações", "operacao": "operação",
    "operacoes": "operações", "criacao": "criação", "exclusao": "exclusão",
    "validacao": "validação", "serializacao": "serialização", "transacao": "transação",
    "transacoes": "transações", "persistencia": "persistência", "associacao": "associação",
    "conversao": "conversão", "integracao": "integração", "integracoes": "integrações",
    "autenticacao": "autenticação", "autorizacao": "autorização", "conexao": "conexão",
    "conexoes": "conexões", "comunicacao": "comunicação", "organizacao": "organização",
    "implementacao": "implementação", "implementacoes": "implementações",
    "distribuida": "distribuída", "distribuido": "distribuído", "codigo": "código",
    "codigos": "códigos", "metodo": "método", "metodos": "métodos",
    "numero": "número", "numeros": "números", "unico": "único", "unica": "única",
    "usuarios": "usuários", "usuario": "usuário", "criterio": "critério",
    "diretorio": "diretório", "diretorios": "diretórios", "historico": "histórico",
    "logico": "lógico", "logica": "lógica", "pratica": "prática", "praticas": "práticas",
    "basico": "básico", "rapido": "rápido", "rapida": "rápida", "cenario": "cenário",
    "cenarios": "cenários", "proprio": "próprio", "propria": "própria",
    "varios": "vários", "varias": "várias", "possuiam": "possuíam", "esta": "está",
    "sao": "são", "nao": "não", "tambem": "também", "ja": "já", "ate": "até",
    "apos": "após", "porem": "porém", "alem": "além", "so": "só", "e": "e",
    "tres": "três", "conteudo": "conteúdo", "conteudos": "conteúdos", "cabecalho": "cabeçalho",
    "cabecalhos": "cabeçalhos", "corpo": "corpo", "negocio": "negócio", "decisao": "decisão",
    "decisoes": "decisões", "producao": "produção", "restricao": "restrição",
    "restricoes": "restrições", "especificacao": "especificação", "migracao": "migração",
    "migracoes": "migrações", "relacional": "relacional", "relatorios": "relatórios",
    "relatorio": "relatório", "proximo": "próximo", "proximos": "próximos",
    "maquina": "máquina", "maquinas": "máquinas", "temporaria": "temporária",
    "temporario": "temporário", "diferenca": "diferença", "diferencas": "diferenças",
    "possivel": "possível", "necessario": "necessário", "necessaria": "necessária",
    "disponivel": "disponível", "disponiveis": "disponíveis", "memoria": "memória",
    "endereco": "endereço", "enderecos": "endereços", "inicio": "início", "identificacao": "identificação",
    "identificador": "identificador", "identificadores": "identificadores", "arvore": "árvore",
    "evolucao": "evolução", "automacao": "automação", "automacoes": "automações",
    "acao": "ação", "acoes": "ações", "versao": "versão", "versoes": "versões",
    "integracao": "integração", "observacao": "observação", "observacoes": "observações",
    "comparacao": "comparação", "comparacoes": "comparações", "solucao": "solução",
    "manutencao": "manutenção", "instrucao": "instrução", "instrucoes": "instruções",
    "destruicao": "destruição", "tecnica": "técnica", "tecnico": "técnico",
    "periodo": "período", "familia": "família", "serie": "série", "series": "séries",
    "metricas": "métricas", "metrica": "métrica", "saude": "saúde", "analise": "análise",
    "analises": "análises", "criteriosa": "criteriosa", "explicito": "explícito",
    "explicitos": "explícitos", "valido": "válido", "invalido": "inválido",
    "sintaxe": "sintaxe", "padrao": "padrão", "padroes": "padrões", "permissao": "permissão",
    "permissoes": "permissões", "proposito": "propósito", "dependencia": "dependência",
    "dependencias": "dependências", "repositorio": "repositório", "repositorios": "repositórios",
    "distribuicao": "distribuição", "colaboracao": "colaboração", "resolucao": "resolução",
    "descricao": "descrição", "revisao": "revisão", "revisoes": "revisões",
    "integrada": "integrada", "publicacao": "publicação", "publicacoes": "publicações",
}


def _pt(text: str) -> str:
    for original, corrected in _ACCENTS.items():
        text = re.sub(rf"\b{re.escape(original)}\b", corrected, text)
    return text


def paragraph(text: str, style: str = "GuideBody"):
    return base_paragraph(_pt(text), style)


def h1(text: str):
    return base_h1(_pt(text))


def h2(text: str):
    return base_h2(_pt(text))


def h3(text: str):
    return base_h3(_pt(text))


def bullets(items: list[str]):
    return base_bullets([_pt(item) for item in items])


def callout(title: str, text: str, background=LIGHT_BLUE):
    return base_callout(_pt(title), _pt(text), background)


def data_table(headers: list[str], rows: list[list[str]], widths=None):
    return base_data_table(
        [_pt(str(item)) for item in headers],
        [[_pt(str(item)) for item in row] for row in rows],
        widths,
    )


def page_decoration(title: str):
    """Cria o cabecalho e o rodape usados por um guia."""

    def draw(canvas, doc):
        width, height = canvas._pagesize
        canvas.saveState()
        canvas.setStrokeColor(colors.HexColor("#CBD5E1"))
        canvas.setLineWidth(0.4)
        canvas.line(18 * mm, height - 13 * mm, width - 18 * mm, height - 13 * mm)
        canvas.setFont("Arial", 7.2)
        canvas.setFillColor(DARK_GREY)
        canvas.drawString(18 * mm, height - 10 * mm, title)
        canvas.drawRightString(width - 18 * mm, 9 * mm, f"Pagina {doc.page}")
        canvas.restoreState()

    return draw


def cover(story: list, title: str, subtitle: str, scope: str) -> None:
    story.extend([Spacer(1, 35 * mm), paragraph(title, "GuideTitle")])
    story.append(Spacer(1, 5 * mm))
    story.append(paragraph(subtitle, "GuideSubtitle"))
    story.append(Spacer(1, 18 * mm))
    story.append(callout("Escopo", scope, GREEN))
    story.append(Spacer(1, 12 * mm))
    story.append(paragraph("Colecao de referencia - Projeto Recomeco", "GuideSubtitle"))
    story.append(paragraph("Preparado para Ricardo - setembro de 2026", "GuideSubtitle"))
    story.append(PageBreak())


def introduction(story: list, purpose: str, branches: str) -> None:
    story.append(h1("Como usar este guia"))
    story.append(paragraph(purpose))
    story.extend(
        bullets(
            [
                "Cada termo importante e explicado antes de aparecer nos exemplos.",
                "As caixas Projeto Recomeco ligam a teoria ao codigo e a infraestrutura que realmente usamos.",
                "Os comandos foram escritos para PowerShell no Windows, salvo quando outro ambiente estiver indicado.",
                "Comandos destrutivos recebem um aviso; confirme sempre o diretorio e o recurso antes de executa-los.",
                "Versoes e configuracoes representam o laboratorio na data desta edicao.",
            ]
        )
    )
    story.append(callout("Branches consultadas", branches, LIGHT_BLUE))
    story.append(PageBreak())


def toc(story: list) -> None:
    story.append(paragraph("Indice", "GuideTocTitle"))
    index = TableOfContents()
    index.levelStyles = [
        ParagraphStyle(
            name=f"CollectionTOC1-{id(story)}",
            fontName="Arial-Bold",
            fontSize=8.4,
            leading=10.5,
            textColor=NAVY,
            spaceBefore=2,
        ),
        ParagraphStyle(
            name=f"CollectionTOC2-{id(story)}",
            fontName="Arial",
            fontSize=7.3,
            leading=9,
            leftIndent=12,
            textColor=DARK_GREY,
        ),
    ]
    story.append(index)
    story.append(PageBreak())


def api_data_story() -> list:
    s: list = []
    cover(
        s,
        "Guia 1 - API REST, PostgreSQL e Redis",
        "Fundamentos, arquitetura e exemplos reais do Projeto Recomeco",
        "HTTP e REST, camadas Spring Boot, DTOs, validacao, erros, JPA/Hibernate, PostgreSQL, transacoes, Redis e cache.",
    )
    introduction(
        s,
        "Este documento explica o caminho completo de uma requisicao: ela entra pelo servidor HTTP, passa pelo controller e pelo service, consulta o repository, chega ao banco e volta como JSON. Depois acrescentamos o Redis como cache.",
        "master, feature/docker-postgres-redis, feature/observabilidade e a arvore de trabalho atual de feature/aws-services.",
    )
    toc(s)

    s.append(h1("1. Visao geral da aplicacao"))
    s.append(paragraph("O Projeto Recomeco e uma aplicacao Spring Boot executada como um unico processo Java. Atualmente ele e um monolito modular: controllers, services, repositories, mensageria e integracoes AWS convivem na mesma aplicacao, mas estao separados por pacotes e responsabilidades."))
    s.append(code_block("Cliente HTTP\n  -> Controller\n     -> Service\n        -> Repository\n           -> PostgreSQL ou H2\n        -> Redis pode evitar uma nova consulta\n  <- DTO convertido para JSON"))
    s.append(h2("1.1 Responsabilidade de cada camada"))
    s.append(data_table(
        ["Camada", "Responsabilidade", "Exemplo no projeto"],
        [
            ["Controller", "Traduz HTTP para chamadas Java e monta a resposta.", "ClienteController, PedidoController"],
            ["Service", "Aplica regras de negocio e coordena operacoes.", "ClienteService, PedidoService"],
            ["Repository", "Fornece acesso aos dados por Spring Data JPA.", "ClienteRepository, PedidoRepository"],
            ["Entity", "Representa o estado persistido em uma tabela.", "Cliente, Pedido"],
            ["DTO", "Representa os dados que entram ou saem da API.", "ClienteDTO, PedidoDTO"],
        ],
        [28 * mm, 72 * mm, 68 * mm],
    ))
    s.append(h2("1.2 Por que DTO e entidade nao sao a mesma coisa"))
    s.append(paragraph("A entidade pertence ao modelo de persistencia. O DTO pertence ao contrato externo. Separar os dois evita expor detalhes do Hibernate, relacionamentos inteiros ou campos internos. No projeto, ClienteService converte Cliente para ClienteDTO antes de devolver os dados."))
    s.append(callout("Projeto Recomeco", "Os DTOs imutaveis foram implementados como records. O controller recebe e devolve DTOs; o repository trabalha com entidades.", GREEN))

    s.append(h1("2. HTTP e REST sem misterio"))
    s.append(paragraph("HTTP e o protocolo de comunicacao. REST e um estilo para organizar recursos e operacoes sobre HTTP. Um endpoint e a combinacao de metodo, caminho e servidor, como GET http://localhost:8080/ProjetoSpringBoot/clientes."))
    s.append(h2("2.1 Metodos mais usados"))
    s.append(data_table(
        ["Metodo", "Intencao", "Idempotente?", "Exemplo"],
        [
            ["GET", "Consultar sem alterar o recurso.", "Sim", "GET /clientes"],
            ["POST", "Criar ou iniciar um processamento.", "Normalmente nao", "POST /pedidos"],
            ["PUT", "Substituir o estado editavel.", "Sim", "PUT /clientes/{id}"],
            ["PATCH", "Alterar parte do recurso.", "Depende da operacao", "PATCH /clientes/{id}/email"],
            ["DELETE", "Excluir o recurso.", "Deve tender a sim", "DELETE /pedidos/{id}"],
        ],
        [20 * mm, 58 * mm, 38 * mm, 52 * mm],
    ))
    s.append(h2("2.2 Status HTTP"))
    s.extend(bullets([
        "200 OK: consulta ou alteracao concluida com corpo de resposta.",
        "201 Created: novo recurso criado, como cliente ou pedido.",
        "204 No Content: operacao concluida sem corpo, comum na exclusao.",
        "400 Bad Request: corpo, parametro ou arquivo invalido.",
        "401 Unauthorized: autenticacao ausente ou invalida.",
        "403 Forbidden: identidade conhecida, mas sem permissao.",
        "404 Not Found: recurso nao encontrado para os identificadores informados.",
        "500 Internal Server Error: falha inesperada do servidor.",
    ]))
    s.append(h2("2.3 Caminho, query, cabecalho e corpo"))
    s.append(code_block("# Path variable: identifica o recurso\nGET /ProjetoSpringBoot/clientes/porId/5\n\n# Header: transporta metadados e autenticacao\nAuthorization: Bearer <token>\nContent-Type: application/json\n\n# Body JSON: transporta os dados do recurso\n{\n  \"nome\": \"Ricardo\",\n  \"email\": \"ricardo@email.com\",\n  \"ativo\": true\n}"))

    s.append(h1("3. Endpoints reais do Projeto Recomeco"))
    s.append(h2("3.1 Clientes"))
    s.append(data_table(
        ["Metodo", "Caminho", "Resultado"],
        [
            ["GET", "/clientes", "Lista todos"],
            ["GET", "/clientes/porId/{id}", "Busca um cliente"],
            ["GET", "/clientes/ativos", "Lista ativos"],
            ["POST", "/clientes", "Cria"],
            ["PUT", "/clientes/{id}", "Atualiza todos os campos editaveis"],
            ["PATCH", "/clientes/{id}/email", "Altera somente o e-mail"],
            ["DELETE", "/clientes/{id}", "Exclui"],
        ], [22 * mm, 72 * mm, 74 * mm],
    ))
    s.append(h2("3.2 Pedidos"))
    s.append(data_table(
        ["Metodo", "Caminho", "Resultado"],
        [
            ["POST", "/pedidos", "Cria pedido e registra evento na Outbox"],
            ["GET", "/pedidos", "Lista pedidos"],
            ["GET", "/pedidos/{id}", "Busca pelo id"],
            ["GET", "/pedidos/cliente/{clienteId}", "Lista por cliente"],
            ["DELETE", "/pedidos/{id}", "Exclui"],
        ], [22 * mm, 72 * mm, 74 * mm],
    ))
    s.append(h2("3.3 Recursos acrescentados pela branch AWS"))
    s.extend(bullets([
        "Anexos: POST e GET /pedidos/{pedidoId}/anexos; download temporario e DELETE por anexoId.",
        "Relatorios: POST solicita geracao; GET consulta o historico; GET /{relatorioId}/download gera a URL temporaria.",
        "Auditoria: GET /pedidos/{pedidoId}/auditorias-relatorios consulta eventos gravados pelo consumer SQS.",
    ]))
    s.append(callout("Base URL local", "Todos os caminhos recebem o prefixo http://localhost:8080/ProjetoSpringBoot configurado por server.servlet.context-path.", YELLOW))

    s.append(h1("4. Da anotacao Java ate a resposta JSON"))
    s.append(h2("4.1 Controller"))
    s.append(code_block("@RestController\n@RequestMapping(\"/clientes\")\npublic class ClienteController {\n    private final ClienteService service;\n\n    @GetMapping\n    public ResponseEntity<List<ClienteDTO>> listar() {\n        return ResponseEntity.ok(service.listar());\n    }\n}"))
    s.append(paragraph("@RestController registra a classe no Spring MVC. @RequestMapping define o prefixo. @GetMapping escolhe o metodo HTTP. ResponseEntity permite controlar status, cabecalhos e corpo."))
    s.append(h2("4.2 Validacao e tratamento de erro"))
    s.append(paragraph("@Valid pede ao Bean Validation para verificar as restricoes do DTO antes do service. O GlobalExceptionHandler, anotado com @RestControllerAdvice, transforma excecoes em uma resposta JSON consistente. Isso impede que cada controller repita o mesmo tratamento."))
    s.append(h2("4.3 Serializacao"))
    s.append(paragraph("Jackson converte automaticamente DTOs Java para JSON e o JSON recebido para DTOs. Content-Type informa o formato. Uma falha de sintaxe no JSON normalmente termina em 400 antes de executar a regra de negocio."))

    s.append(h1("5. JPA, Hibernate e Spring Data"))
    s.append(paragraph("JPA e uma especificacao Java para persistencia. Hibernate e a implementacao usada pelo Spring Boot. Spring Data JPA cria repositorios e consultas a partir de interfaces, reduzindo codigo repetitivo."))
    s.append(h2("5.1 Entidade e chave primaria"))
    s.append(code_block("@Entity\n@Table(name = \"clientes\")\npublic class Cliente {\n    @Id\n    @GeneratedValue(strategy = GenerationType.IDENTITY)\n    private Long id;\n\n    @Column(nullable = false, unique = true)\n    private String email;\n}"))
    s.append(h2("5.2 Relacionamento Cliente -> Pedido"))
    s.append(paragraph("Cada pedido pertence a um cliente. @ManyToOne representa muitos pedidos apontando para um cliente. @JoinColumn(name = \"cliente_id\") define a chave estrangeira. FetchType.LAZY evita carregar o cliente antes que ele seja necessario."))
    s.append(code_block("pedidos.cliente_id  ->  clientes.id\n\nPedido 801 --+\nPedido 802 --+--> Cliente 5\nPedido 803 --+"))
    s.append(h2("5.3 Repository"))
    s.append(code_block("public interface ClienteRepository extends JpaRepository<Cliente, Long> {\n    List<Cliente> findByAtivoTrue();\n}\n\n// JpaRepository fornece save, findById, findAll, delete e outros.\n// findByAtivoTrue e uma consulta derivada do nome do metodo."))
    s.append(h2("5.4 ddl-auto update"))
    s.append(paragraph("No laboratorio, spring.jpa.hibernate.ddl-auto=update permite ao Hibernate ajustar tabelas. E pratico para estudo, mas nao oferece o controle e a rastreabilidade de uma ferramenta de migracao como Flyway. Em producao, migracoes versionadas sao preferiveis."))

    s.append(h1("6. PostgreSQL"))
    s.append(paragraph("PostgreSQL e um banco relacional. Ele organiza dados em tabelas, garante tipos, chaves e restricoes e executa transacoes ACID. Usamos PostgreSQL 17 no Docker e PostgreSQL 18.3 no Amazon RDS durante o laboratorio AWS."))
    s.append(h2("6.1 Banco, schema, tabela, linha e coluna"))
    s.append(data_table(
        ["Termo", "Explicacao simples"],
        [
            ["Banco", "Conjunto logico conectado pela aplicacao, como projetodb."],
            ["Schema", "Espaco de nomes dentro do banco; normalmente public."],
            ["Tabela", "Estrutura de registros do mesmo tipo, como clientes."],
            ["Linha", "Uma ocorrencia, como o cliente de id 5."],
            ["Coluna", "Um atributo tipado, como email ou valor."],
            ["Indice", "Estrutura auxiliar que acelera buscas e custa espaco/escrita."],
        ], [38 * mm, 130 * mm],
    ))
    s.append(h2("6.2 Transacoes"))
    s.append(paragraph("Uma transacao agrupa operacoes que devem ser confirmadas ou desfeitas juntas. PedidoService.criarPedido usa @Transactional para salvar o pedido e registrar o evento Outbox na mesma unidade. Se o segundo passo falhar, o primeiro tambem e revertido."))
    s.append(code_block("BEGIN;\nINSERT INTO pedidos (...);\nINSERT INTO eventos_outbox (...);\nCOMMIT;  -- confirma tudo\n\n-- Em caso de erro: ROLLBACK desfaz tudo."))
    s.append(h2("6.3 PowerShell e Docker"))
    s.append(code_block("# Verifica se o container do PostgreSQL esta ativo\ndocker compose ps postgres\n\n# Abre o psql dentro do container\ndocker compose exec postgres psql -U postgres -d projetodb\n\n# Lista tabelas no psql\n\\dt\n\n# Sai do psql\n\\q"))
    s.append(h2("6.4 Consultas uteis"))
    s.append(code_block("-- Quantidade de pedidos por cliente\nSELECT c.id, c.nome, COUNT(p.id) AS quantidade_pedidos\nFROM clientes c\nLEFT JOIN pedidos p ON p.cliente_id = c.id\nGROUP BY c.id, c.nome\nORDER BY c.id;\n\n-- Tamanho das tabelas\nSELECT relname AS tabela, pg_size_pretty(pg_total_relation_size(relid)) AS total\nFROM pg_catalog.pg_statio_user_tables\nORDER BY pg_total_relation_size(relid) DESC;"))

    s.append(h1("7. Redis e cache"))
    s.append(paragraph("Redis e um armazenamento em memoria baseado em chaves e valores. No projeto, ele nao substitui o PostgreSQL: funciona como cache para evitar buscas repetidas. O PostgreSQL continua sendo a fonte oficial dos dados."))
    s.append(h2("7.1 Cache hit e cache miss"))
    s.append(code_block("GET cliente 5\n  -> Redis possui clientes::5?\n       SIM: cache hit, devolve rapidamente\n       NAO: cache miss -> consulta PostgreSQL -> grava no Redis -> devolve"))
    s.append(h2("7.2 Anotacoes reais"))
    s.append(data_table(
        ["Anotacao", "Uso no ClienteService"],
        [
            ["@Cacheable", "buscar: consulta o metodo apenas quando a chave nao esta no cache."],
            ["@CachePut", "atualizar e alterarEmail: executa o metodo e renova o valor."],
            ["@CacheEvict", "excluir: remove a entrada que ficou invalida."],
        ], [42 * mm, 126 * mm],
    ))
    s.append(paragraph("CacheConfig usa @Profile(\"docker\"). Portanto, o cache so e habilitado quando o Redis do Compose esta disponivel. O TTL e de 10 minutos no application-docker.yaml."))
    s.append(h2("7.3 PowerShell para observar o Redis"))
    s.append(code_block("# Confirma que o Redis responde\ndocker compose exec redis redis-cli PING\n\n# Lista chaves apenas no laboratorio pequeno\ndocker compose exec redis redis-cli KEYS \"*\"\n\n# Le o tempo de vida restante de uma chave\ndocker compose exec redis redis-cli TTL \"clientes::5\"\n\n# Observa comandos em tempo real; interrompa com Ctrl+C\ndocker compose exec redis redis-cli MONITOR"))
    s.append(callout("Cuidado", "KEYS pode bloquear um Redis grande. No laboratorio e aceitavel; em ambientes reais prefira SCAN.", YELLOW))

    s.append(h1("8. Perfis e ambientes"))
    s.append(paragraph("Um perfil Spring seleciona configuracoes para um ambiente. application.yaml contem a base local com H2. application-docker.yaml troca o banco para jdbc:postgresql://postgres:5432/projetodb, aponta o Redis para redis:6379 e o Kafka para kafka:29092."))
    s.append(code_block("# Aplicacao pela IDE com configuracao base\n.\\mvnw.cmd spring-boot:run\n\n# Ativa um perfil no PowerShell apenas nesta janela\n$env:SPRING_PROFILES_ACTIVE = \"docker\"\n.\\mvnw.cmd spring-boot:run\n\n# Remove a variavel da sessao atual\nRemove-Item Env:SPRING_PROFILES_ACTIVE"))
    s.append(h2("8.1 Nome de servico no Docker"))
    s.append(paragraph("Dentro da rede do Compose, localhost significa o proprio container. Por isso a aplicacao usa postgres, redis e kafka como hosts. No Windows, as portas publicadas permitem usar localhost:5432, localhost:6379 e localhost:9092."))

    s.append(h1("9. Diagnostico pratico"))
    s.append(data_table(
        ["Sintoma", "Verificacao", "Causa comum"],
        [
            ["404", "Metodo, caminho e IDs", "Endpoint errado ou recurso ausente"],
            ["401", "Authorization e configuracao de seguranca", "Token ausente"],
            ["Banco recusou conexao", "docker compose ps e porta 5432", "Container parado ou host incorreto"],
            ["Redis nao usado", "Perfil ativo e CacheConfig", "Perfil docker nao esta ativo"],
            ["Dados sumiram", "Volume postgres-data", "Volume removido ou outro banco selecionado"],
            ["JSON invalido", "Content-Type e sintaxe", "Aspas, virgula ou tipo incorreto"],
        ], [40 * mm, 58 * mm, 70 * mm],
    ))
    s.append(h2("9.1 Comandos PowerShell"))
    s.append(code_block("# Testa a porta HTTP\nTest-NetConnection -ComputerName localhost -Port 8080\n\n# Faz uma consulta HTTP simples\nInvoke-RestMethod -Method Get `\n  -Uri \"http://localhost:8080/ProjetoSpringBoot/clientes\"\n\n# Consulta os logs recentes da aplicacao no Compose\ndocker compose logs --tail 100 app"))

    s.append(h1("10. Referencia rapida"))
    s.append(data_table(
        ["Conceito", "Frase para lembrar"],
        [
            ["REST", "Organizacao de recursos e operacoes sobre HTTP."],
            ["DTO", "Contrato de dados da fronteira da aplicacao."],
            ["Entity", "Objeto ligado a uma tabela."],
            ["Repository", "Porta de acesso aos dados."],
            ["Transacao", "Tudo confirma ou tudo desfaz."],
            ["PostgreSQL", "Fonte relacional e persistente dos dados."],
            ["Redis", "Atalho temporario em memoria."],
            ["TTL", "Prazo de validade de uma entrada."],
        ], [48 * mm, 120 * mm],
    ))
    return s


def tests_story() -> list:
    s: list = []
    cover(
        s,
        "Guia 2 - Testes Automatizados",
        "JUnit, Mockito, MockMvc e a suite real do Projeto Recomeco",
        "Fundamentos de teste, organizacao Arrange-Act-Assert, mocks, testes MVC, contexto Spring, repositorios, integracoes e comandos Maven no PowerShell.",
    )
    introduction(
        s,
        "Este guia mostra por que testamos, qual parte cada tipo de teste protege e como ler os testes existentes. A arvore atual possui 37 classes terminadas em Test e 98 metodos anotados com @Test.",
        "master, feature/mensageria-kafka, feature/observabilidade, feature/aws-services e a arvore de trabalho atual.",
    )
    toc(s)

    s.append(h1("1. O que um teste automatizado faz"))
    s.append(paragraph("Um teste prepara um cenario conhecido, executa uma unidade de comportamento e verifica o resultado. Ele nao prova que o sistema inteiro nunca falhara, mas detecta regressao: algo que funcionava deixou de funcionar depois de uma mudanca."))
    s.append(h2("1.1 Arrange, Act, Assert"))
    s.append(code_block("// Arrange: prepara entradas e comportamentos\nwhen(repository.findById(1L)).thenReturn(Optional.of(cliente));\n\n// Act: executa o comportamento real\nClienteDTO resultado = service.buscar(1L);\n\n// Assert: confirma resultado e colaboracao\nassertEquals(\"Ricardo\", resultado.nome());\nverify(repository).findById(1L);"))
    s.append(h2("1.2 Piramide de testes"))
    s.append(data_table(
        ["Nivel", "Caracteristica", "Exemplo no projeto"],
        [
            ["Unitario", "Rapido; classe isolada; dependencias simuladas.", "ClienteServiceTest"],
            ["Slice", "Carrega uma parte do Spring.", "ClienteControllerTest com @WebMvcTest"],
            ["Integracao", "Carrega contexto e componentes reais.", "AnexoPedidoRepositoryTest"],
            ["Conexao externa", "Fala com servico real e pode ser opcional.", "AwsS3ConnectionIntegrationTest"],
        ], [32 * mm, 66 * mm, 70 * mm],
    ))

    s.append(h1("2. JUnit 5"))
    s.append(paragraph("JUnit e o framework que descobre e executa os testes. @Test marca um metodo. @BeforeEach prepara um novo estado antes de cada teste. Assertions comparam o resultado real com o esperado."))
    s.append(h2("2.1 Assertions comuns"))
    s.append(data_table(
        ["Assertion", "O que confirma"],
        [
            ["assertEquals", "Dois valores sao equivalentes."],
            ["assertTrue / assertFalse", "Uma condicao booleana."],
            ["assertNotNull", "Uma referencia foi produzida."],
            ["assertThrows", "Uma operacao falha com a excecao esperada."],
            ["assertThat", "API fluente do AssertJ para verificacoes expressivas."],
        ], [50 * mm, 118 * mm],
    ))
    s.append(h2("2.2 Um teste deve contar uma historia"))
    s.append(paragraph("Nomes como deveCriarCliente e naoDeveEnviarAnexoSemAutenticacao descrevem a regra. Um teste deve ter um motivo claro para falhar. Evite varios cenarios independentes no mesmo metodo."))

    s.append(h1("3. Mockito"))
    s.append(paragraph("Mockito cria objetos simulados. O mock nao executa a implementacao real; responde conforme programado. Isso permite testar ClienteService sem banco e RelatorioPedidoSqsConsumer sem chamar a AWS."))
    s.append(h2("3.1 Anotacoes"))
    s.append(data_table(
        ["Anotacao", "Papel"],
        [
            ["@ExtendWith(MockitoExtension.class)", "Integra Mockito ao ciclo do JUnit."],
            ["@Mock", "Cria uma dependencia simulada."],
            ["@InjectMocks", "Cria a classe real e injeta seus mocks."],
            ["@MockitoBean", "Substitui um bean por mock dentro do contexto Spring."],
        ], [62 * mm, 106 * mm],
    ))
    s.append(h2("3.2 when, thenReturn e verify"))
    s.append(code_block("when(repository.findAll()).thenReturn(List.of(cliente));\n\nList<ClienteDTO> resultado = service.listar();\n\nassertEquals(1, resultado.size());\nverify(repository, times(1)).findAll();\nverify(repository, never()).delete(any());"))
    s.append(h2("3.3 ArgumentCaptor"))
    s.append(paragraph("Quando o resultado importante foi enviado a outra classe, ArgumentCaptor captura o argumento. Os testes dos consumers SQS o usam para confirmar queueUrl e receiptHandle da exclusao da mensagem."))
    s.append(callout("Mock nao e banco falso", "Um mock apenas imita o contrato de uma dependencia. Se programarmos uma resposta impossivel, o teste pode passar e a integracao real falhar. Por isso mantemos tambem testes de integracao.", YELLOW))

    s.append(h1("4. MockMvc"))
    s.append(paragraph("MockMvc simula requisicoes contra o Spring MVC sem abrir uma porta TCP real. Ele testa mapeamento, serializacao JSON, validacao, seguranca e status HTTP."))
    s.append(h2("4.1 Exemplo do ClienteControllerTest"))
    s.append(code_block("@WebMvcTest(ClienteController.class)\nclass ClienteControllerTest {\n    @Autowired MockMvc mockMvc;\n    @MockitoBean ClienteService service;\n\n    @Test\n    @WithMockUser(roles = \"USER\")\n    void deveListarClientes() throws Exception {\n        when(service.listar()).thenReturn(List.of(cliente));\n        mockMvc.perform(get(\"/clientes\"))\n            .andExpect(status().isOk())\n            .andExpect(content().contentTypeCompatibleWith(\"application/json\"))\n            .andExpect(jsonPath(\"$[0].nome\").value(\"Ricardo\"));\n    }\n}"))
    s.append(h2("4.2 @WebMvcTest versus @SpringBootTest"))
    s.append(data_table(
        ["Opcao", "Carrega", "Quando usar"],
        [
            ["@WebMvcTest", "Camada MVC e controller escolhido.", "Contrato HTTP isolado e rapido."],
            ["@SpringBootTest", "Contexto amplo da aplicacao.", "Integracao entre configuracoes e camadas."],
            ["@AutoConfigureMockMvc", "MockMvc dentro do contexto completo.", "HTTP com seguranca e beans reais/mocados."],
        ], [44 * mm, 62 * mm, 62 * mm],
    ))

    s.append(h1("5. Seguranca nos testes"))
    s.append(paragraph("O erro 201 quando esperavamos 401 revelou que a seguranca nao estava no contexto correto. Depois, ao protege-la, os cenarios de sucesso passaram a devolver 401 porque nao possuam identidade simulada. O conserto correto foi manter o endpoint protegido e autenticar apenas os testes autorizados."))
    s.append(code_block("// Usuario simples em testes da seguranca local\n.with(user(\"ricardo\").roles(\"USER\"))\n\n// JWT simulado nos testes atuais de resource server\n.with(jwt().authorities(new SimpleGrantedAuthority(\"ROLE_USER\")))\n\n// Sem post-processor de seguranca: deve resultar em 401"))
    s.append(h2("5.1 401 nao e 403"))
    s.append(paragraph("401 significa que falta uma autenticacao valida. 403 significa que a identidade foi reconhecida, mas nao possui a autoridade exigida. Os testes devem cobrir os dois quando a regra de autorizacao for relevante."))

    s.append(h1("6. Exemplos reais por camada"))
    s.append(h2("6.1 Entidades"))
    s.append(paragraph("AnexoPedidoTest e RelatorioPedidoTest exercitam transicoes de estado sem Spring. Sao testes de dominio: criacao, disponibilidade, falha, exclusao e operacoes proibidas."))
    s.append(h2("6.2 Services"))
    s.append(paragraph("ClienteServiceTest possui cenarios de busca, listagem, criacao, atualizacao e exclusao. PedidoServiceTest valida a associacao com cliente, persistencia e Outbox. AnexoPedidoServiceTest possui sete cenarios e isola repository, storage e validacoes."))
    s.append(h2("6.3 Repository"))
    s.append(paragraph("AnexoPedidoRepositoryTest carrega o contexto, persiste dados e verifica consultas que combinam pedidoId e anexoId. @Transactional permite reverter alteracoes ao fim do teste."))
    s.append(h2("6.4 Mensageria"))
    s.append(paragraph("EventoOutboxJobTest isola o job e verifica chamadas ao publicador. Consumers Kafka testam processamento e tratamento de erro. Consumers SQS confirmam receive, conversao do JSON, chamada do service e delete apenas depois do sucesso."))
    s.append(h2("6.5 AWS"))
    s.append(paragraph("S3StorageServiceTest simula o SDK. Os testes AwsS3ConnectionIntegrationTest e AwsSqsConnectionIntegrationTest precisam de sessao real e foram desenhados para poder ser ignorados quando a integracao externa nao esta habilitada."))

    s.append(h1("7. Suite atual em numeros"))
    s.append(data_table(
        ["Grupo", "Exemplos", "Caracteristica"],
        [
            ["Controller", "Cliente, Security, Anexo, Relatorio, Auditoria", "MockMvc e contrato HTTP"],
            ["Service", "Cliente, Pedido, Estoque, Outbox, anexos e relatorios", "Mockito e regras"],
            ["Mensageria", "Kafka, SQS, SNS, jobs e recovery", "Fluxos e chamadas"],
            ["Observabilidade", "Health, metricas, contexto de log", "Sinais operacionais"],
            ["Persistencia", "Entidades e repository de anexos", "Estado e consultas"],
            ["Total observado", "37 classes e 98 @Test", "Arvore de trabalho atual"],
        ], [38 * mm, 72 * mm, 58 * mm],
    ))

    s.append(h1("8. Maven e PowerShell"))
    s.append(code_block("# Executa toda a suite\n.\\mvnw.cmd test\n\n# Limpa, testa e valida o projeto como no CI\n.\\mvnw.cmd clean verify\n\n# Executa uma classe\n.\\mvnw.cmd -Dtest=ClienteServiceTest test\n\n# Executa um metodo\n.\\mvnw.cmd -Dtest=ClienteServiceTest#deveBuscarClientePorId test\n\n# Exibe detalhes adicionais de uma falha\n.\\mvnw.cmd -e -Dtest=ClienteServiceTest test"))
    s.append(callout("PowerShell", "Use .\\mvnw.cmd no Windows. ./mvnw aparece no GitHub Actions porque o runner usa Linux. Escrever mvnw sem .\\ pode nao localizar o arquivo no diretorio atual.", GREEN))

    s.append(h1("9. Como diagnosticar uma falha"))
    s.extend(bullets([
        "Leia primeiro a linha com nomeDaClasse.nomeDoMetodo e expected/actual.",
        "Confirme se a falha esta na regra ou na preparacao do teste.",
        "Verifique se um mock necessario recebeu when(...).thenReturn(...).",
        "Em MockMvc, confira autenticacao, Content-Type, corpo JSON e caminho.",
        "Abra target/surefire-reports para o relatorio individual.",
        "Execute somente o teste quebrado durante o diagnostico; depois execute a suite inteira.",
    ]))
    s.append(code_block("# Localiza relatorios recentes no PowerShell\nGet-ChildItem .\\target\\surefire-reports `\n  | Sort-Object LastWriteTime -Descending `\n  | Select-Object -First 10 Name, LastWriteTime\n\n# Procura falhas nos arquivos de texto\nSelect-String -Path .\\target\\surefire-reports\\*.txt -Pattern \"FAILURE|ERROR\""))

    s.append(h1("10. Boas praticas para nosso aprendizado"))
    s.extend(bullets([
        "Um comportamento por teste e nomes que descrevem a expectativa.",
        "Dados pequenos e legiveis; evite depender da ordem de outros testes.",
        "Nao simular a propria classe testada; simule apenas colaboradores externos.",
        "Verificar resultado e efeitos importantes, sem verificar cada chamada interna.",
        "Cobrir caminho feliz, validacao, ausencia, seguranca e falha de dependencia.",
        "Manter testes externos separados para a suite local nao depender da AWS.",
    ]))
    s.append(h2("10.1 Checklist para escrever um novo teste"))
    s.extend(bullets([
        "Definir em uma frase qual regra sera protegida.",
        "Escolher o menor contexto capaz de executar essa regra.",
        "Preparar apenas os dados relevantes para o cenario.",
        "Executar o metodo ou a requisicao uma unica vez.",
        "Confirmar o resultado observavel e o efeito colateral importante.",
        "Executar primeiro o novo teste e depois toda a suite.",
    ]))
    s.append(h2("10.2 Glossario de testes"))
    s.append(data_table(
        ["Termo", "Para lembrar"],
        [
            ["Fixture", "Dados e objetos preparados para um teste."],
            ["Mock", "Substituto programavel de uma dependencia."],
            ["Stub", "Substituto focado em fornecer respostas."],
            ["Spy", "Objeto observado que pode manter comportamento real."],
            ["Assertion", "Verificacao do resultado esperado."],
            ["Regressao", "Funcionalidade antiga quebrada por uma mudanca."],
            ["Flaky test", "Teste que passa ou falha sem mudanca relevante."],
        ], [42 * mm, 126 * mm],
    ))
    return s


def git_cicd_story() -> list:
    s: list = []
    cover(
        s,
        "Guia 3 - Git, GitHub e CI/CD",
        "Branches, Pull Requests, GitHub Actions e Jenkins no Projeto Recomeco",
        "Controle de versao, colaboracao, historico do projeto, pipelines, runners, agents, Maven, Docker e comandos PowerShell.",
    )
    introduction(
        s,
        "Este guia separa tres ideias: Git controla versoes localmente; GitHub hospeda e facilita colaboracao; CI/CD automatiza validacoes e entregas. Os exemplos acompanham as branches e Pull Requests reais do repositorio.",
        "feature/treino-git no historico, feature/jenkins, feature/docker-jenkins-postgres-redis, feature/github, master e demais referencias locais/remotas.",
    )
    toc(s)

    s.append(h1("1. Git, GitHub e repositorio"))
    s.append(data_table(
        ["Termo", "Papel"],
        [
            ["Git", "Ferramenta distribuida que registra versoes dos arquivos."],
            ["Repositorio", "Pasta de trabalho mais banco de historico .git."],
            ["GitHub", "Servidor e interface para hospedar repositorios e colaborar."],
            ["origin", "Nome convencional do repositorio remoto principal."],
            ["working tree", "Arquivos atualmente visiveis e editaveis."],
            ["staging area", "Selecao preparada para o proximo commit."],
        ], [42 * mm, 126 * mm],
    ))
    s.append(h2("1.1 Os tres estados"))
    s.append(code_block("Arquivo modificado\n  -> git add: vai para staging\n     -> git commit: entra no historico local\n        -> git push: referencia e objetos seguem para o GitHub"))
    s.append(h2("1.2 Comandos seguros de observacao"))
    s.append(code_block("git status\ngit branch --all\ngit log --oneline --decorate --graph --all\ngit diff\ngit diff --staged\ngit remote -v"))

    s.append(h1("2. Commits"))
    s.append(paragraph("Commit e uma fotografia logica das alteracoes selecionadas, com autor, data, mensagem e referencia ao commit anterior. Ele nao e apenas um backup: conta a historia de uma decisao."))
    s.append(code_block("# Veja o estado antes de preparar\ngit status\n\n# Prepare arquivos escolhidos\ngit add src/main/java/.../ClienteService.java\ngit add src/test/java/.../ClienteServiceTest.java\n\n# Revise exatamente o que entrara\ngit diff --staged\n\n# Registre\ngit commit -m \"feat: adiciona cache de clientes\""))
    s.append(h2("2.1 Mensagens"))
    s.append(paragraph("Mensagens como feat: adiciona upload de anexos de pedidos explicam a mudanca. Prefixos frequentes: feat para funcionalidade, fix para correcao, test para testes, docs para documentacao, build para construcao e refactor para reorganizacao sem mudar comportamento."))

    s.append(h1("3. Branches"))
    s.append(paragraph("Branch e um ponteiro movel para uma linha de commits. Criar uma branch nao copia fisicamente todo o projeto. Ela permite desenvolver um assunto sem alterar imediatamente a master."))
    s.append(h2("3.1 Mapa real"))
    s.append(data_table(
        ["Branch", "Assunto principal"],
        [
            ["feature/treino-git", "Primeiros commits e Pull Request #1"],
            ["feature/jenkins", "Pipeline Jenkins inicial"],
            ["feature/docker-postgres-redis", "Aplicacao, PostgreSQL e Redis no Docker"],
            ["feature/docker-jenkins-postgres-redis", "Jenkins construindo e validando Compose"],
            ["feature/github", "CI com GitHub Actions"],
            ["feature/mensageria-kafka", "Kafka, retry, DLT e Outbox"],
            ["feature/observabilidade", "Actuator, Prometheus, Grafana, Jaeger, Loki e Alloy"],
            ["feature/aws-services", "S3 e evolucao AWS ainda em trabalho"],
            ["OAUTH2", "Experimento de seguranca com Keycloak"],
        ], [70 * mm, 98 * mm],
    ))
    s.append(h2("3.2 Fluxo PowerShell"))
    s.append(code_block("# Atualiza a master somente quando a arvore estiver limpa\ngit switch master\ngit pull --ff-only origin master\n\n# Cria uma branch a partir dela\ngit switch -c feature/nome-da-funcionalidade\n\n# Publica e configura o upstream\ngit push -u origin feature/nome-da-funcionalidade\n\n# Depois, status mostra a relacao local/remota\ngit status"))
    s.append(callout("Preservacao", "Nunca troque de branch com mudancas que podem conflitar sem antes entender o status. Commit, stash ou outra decisao deve ser consciente; git reset --hard pode destruir trabalho.", YELLOW))

    s.append(h1("4. Merge, rebase e conflitos"))
    s.append(paragraph("Merge une duas linhas e pode criar um commit de merge. Rebase reaplica commits sobre outra base e reescreve seus identificadores. Para o aprendizado e para branches ja compartilhadas, merge costuma ser mais facil de compreender."))
    s.append(h2("4.1 Conflito"))
    s.append(paragraph("Conflito ocorre quando o Git nao consegue decidir como combinar alteracoes. O arquivo recebe marcadores. O desenvolvedor escolhe o conteudo final, remove os marcadores, testa e registra a resolucao."))
    s.append(code_block("<<<<<<< HEAD\nconteudo da branch atual\n=======\nconteudo da outra branch\n>>>>>>> feature/exemplo\n\n# Depois de editar e testar\ngit add arquivo.java\ngit commit"))

    s.append(h1("5. Pull Request"))
    s.append(paragraph("Pull Request, ou PR, e uma proposta de integrar uma branch. Ele mostra diff, conversa, revisoes e verificacoes automatizadas. O merge e a acao final; o PR e o processo de avaliacao."))
    s.append(h2("5.1 Nosso historico"))
    s.extend(bullets([
        "PR #1: treino de Git.",
        "PR #2: Docker, Jenkins, PostgreSQL e Redis.",
        "PR #3: GitHub Actions.",
        "PR #5: mensageria Kafka.",
        "PR #6: observabilidade.",
    ]))
    s.append(h2("5.2 Checklist de PR"))
    s.extend(bullets([
        "Titulo descreve a entrega e descricao explica por que ela existe.",
        "Diff nao contem logs, artefatos ou segredos por acidente.",
        "Testes locais passaram e o pipeline remoto esta verde.",
        "Configuracoes e comandos novos foram documentados.",
        "A branch de destino e a correta e o merge nao leva mudancas estranhas.",
    ]))

    s.append(h1("6. CI/CD"))
    s.append(paragraph("Continuous Integration executa build e testes com frequencia para detectar incompatibilidades cedo. Continuous Delivery mantem o software pronto para ser entregue. Continuous Deployment publica automaticamente cada alteracao aprovada. CI/CD e o conjunto dessas praticas e automacoes."))
    s.append(h2("6.1 Pipeline, job, stage e step"))
    s.append(data_table(
        ["Termo", "Explicacao"],
        [
            ["Pipeline", "Fluxo automatizado completo."],
            ["Job", "Unidade executada por uma maquina ou runner."],
            ["Stage", "Fase logica do Jenkins, como Testes ou Package."],
            ["Step", "Acao individual, como checkout ou mvnw test."],
            ["Artifact", "Arquivo produzido, como JAR ou relatorio."],
            ["Trigger", "Evento que inicia o pipeline, como push ou PR."],
        ], [42 * mm, 126 * mm],
    ))

    s.append(h1("7. GitHub Actions no projeto"))
    s.append(paragraph("O workflow .github/workflows/ci.yaml reage a push na master e feature/github e a Pull Requests destinados a master. O job build usa ubuntu-latest."))
    s.append(code_block("name: CI - Java e Docker\non:\n  pull_request:\n    branches: [master]\npermissions:\n  contents: read\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v6\n      - uses: actions/setup-java@v5\n        with:\n          distribution: temurin\n          java-version: \"21\"\n          cache: maven\n      - run: chmod +x mvnw\n      - run: ./mvnw clean verify\n      - run: docker build --tag projeto-springboot:${{ github.sha }} ."))
    s.append(h2("7.1 Runner"))
    s.append(paragraph("Runner e a maquina que executa o job. O GitHub fornece uma maquina Linux temporaria, baixa o codigo, configura Java 21, usa cache Maven, testa e constroi a imagem. Ao fim, a maquina e descartada."))
    s.append(h2("7.2 SHA como tag"))
    s.append(paragraph("github.sha identifica exatamente o commit. Usar esse valor na tag da imagem cria rastreabilidade: sabemos qual codigo produziu a imagem."))

    s.append(h1("8. Jenkins no projeto"))
    s.append(paragraph("Jenkins e um servidor de automacao que administramos. O Jenkinsfile versiona o pipeline. Nosso agent exige os labels windows e java21 e usa o JDK cadastrado como JDK21."))
    s.append(h2("8.1 Estagios implementados"))
    s.append(data_table(
        ["Stage", "Acao"],
        [
            ["Checkout", "deleteDir e checkout scm"],
            ["Teste Java", "Mostra java -version e JAVA_HOME"],
            ["Testes", "call mvnw.cmd test"],
            ["Package", "call mvnw.cmd clean package"],
            ["Teste Docker", "Valida cliente, Compose e daemon"],
            ["Build Docker", "Cria tags BUILD_NUMBER e latest"],
            ["Deploy Docker", "docker compose up -d --no-build --wait"],
            ["Validar Aplicacao", "curl com retry contra /clientes"],
            ["Limpar Imagens Antigas", "Mantem latest e BUILD_NUMBER atual"],
        ], [48 * mm, 120 * mm],
    ))
    s.append(h2("8.2 Agent"))
    s.append(paragraph("O controller Jenkins organiza o trabalho; o agent executa comandos. Como nosso agent e Windows, o Jenkinsfile usa bat, mvnw.cmd, continuacao com ^ e variaveis %BUILD_NUMBER%."))

    s.append(h1("9. GitHub Actions versus Jenkins"))
    s.append(data_table(
        ["Aspecto", "GitHub Actions", "Jenkins"],
        [
            ["Hospedagem", "Integrado ao GitHub", "Servidor administrado por nos"],
            ["Configuracao", "YAML em .github/workflows", "Jenkinsfile Groovy"],
            ["Executor", "Runner hospedado ou proprio", "Agent configurado"],
            ["Manutencao", "Menor no runner hospedado", "Plugins, servidor e agents"],
            ["Projeto", "CI Linux: verify + docker build", "Pipeline Windows + deploy Compose"],
            ["Forca", "Integracao simples com PR", "Grande flexibilidade interna"],
        ], [32 * mm, 68 * mm, 68 * mm],
    ))
    s.append(paragraph("Nao existe vencedor universal. O nosso estudo mostrou o mesmo objetivo de CI expresso em ferramentas e sistemas operacionais diferentes."))

    s.append(h1("10. PowerShell de referencia"))
    s.append(code_block("# Estado e historico\ngit status\ngit log --oneline --decorate --graph --all\n\n# Sincronizacao conservadora\ngit fetch origin\ngit pull --ff-only origin master\n\n# Branch de trabalho\ngit switch -c feature/exemplo\ngit push -u origin feature/exemplo\n\n# Revisao\ngit diff\ngit diff --staged\ngit show --stat HEAD\n\n# Teste equivalente ao CI\n.\\mvnw.cmd clean verify\ndocker build -t projeto-springboot:teste ."))
    s.append(h2("10.1 O que nao fazer automaticamente"))
    s.extend(bullets([
        "Nao usar git reset --hard para limpar sem saber o que sera perdido.",
        "Nao usar push --force em branch compartilhada sem coordenacao.",
        "Nao commitar senhas, tokens, arquivos .env reais ou credenciais AWS.",
        "Nao interpretar pipeline verde como prova de que todos os cenarios de negocio foram cobertos.",
    ]))

    s.append(h1("11. Diagnostico de pipeline"))
    s.append(data_table(
        ["Falha", "Onde olhar", "Acao inicial"],
        [
            ["Checkout", "URL, credencial e branch", "Confirmar acesso e ref"],
            ["Java", "Versao e JAVA_HOME", "Comparar com Java 21"],
            ["Testes", "Surefire e primeiro teste quebrado", "Reproduzir localmente"],
            ["Docker build", "Contexto, JAR e Dockerfile", "Confirmar package anterior"],
            ["Compose", "docker compose ps/logs", "Ver health e portas"],
            ["curl", "Resposta HTTP e logs da aplicacao", "Testar endpoint local"],
        ], [40 * mm, 68 * mm, 60 * mm],
    ))
    s.append(h2("11.1 Checklist antes de integrar uma branch"))
    s.extend(bullets([
        "Working tree compreendida e sem arquivo acidental.",
        "Diff revisado e commits com mensagens claras.",
        "Suite local equivalente ao CI concluida.",
        "Branch remota atualizada e Pull Request apontando para a base correta.",
        "Pipeline verde e comentarios de revisao resolvidos.",
        "Depois do merge, atualizar a master local com pull --ff-only.",
    ]))
    return s


def docker_story() -> list:
    s: list = []
    cover(
        s,
        "Guia 4 - Docker e Ambientes",
        "Teoria completa e infraestrutura real do Projeto Recomeco",
        "Imagens, containers, Dockerfile, Compose, redes, portas, volumes, perfis Spring, PostgreSQL, Redis, Kafka, observabilidade e comandos PowerShell.",
    )
    introduction(
        s,
        "Este guia explica o que o Docker faz por baixo da interface e mapeia cada servico do compose.yaml atual. A aplicacao pode rodar na IDE ou dentro do Compose; essa diferenca muda hosts, portas, perfis e credenciais disponiveis.",
        "feature/docker-postgres-redis, feature/docker-jenkins-postgres-redis, feature/mensageria-kafka, feature/observabilidade, master e arvore atual.",
    )
    toc(s)

    s.append(h1("1. O problema que o Docker resolve"))
    s.append(paragraph("Uma aplicacao depende de runtime, bibliotecas, sistema de arquivos, rede e servicos. Docker empacota o processo e sua base de arquivos em uma imagem reproduzivel. O container e uma execucao isolada dessa imagem."))
    s.append(h2("1.1 Container nao e maquina virtual"))
    s.append(data_table(
        ["Aspecto", "Container", "Maquina virtual"],
        [
            ["Sistema", "Compartilha o kernel do host", "Possui sistema operacional convidado"],
            ["Inicio", "Normalmente segundos", "Geralmente mais lento"],
            ["Tamanho", "Imagem focada na aplicacao", "Disco completo do sistema"],
            ["Isolamento", "Processos, rede e filesystem", "Virtualizacao de hardware"],
            ["Projeto", "PostgreSQL, Redis, Kafka etc.", "Nao usamos VM local para o Compose"],
        ], [34 * mm, 67 * mm, 67 * mm],
    ))
    s.append(callout("Docker Desktop no Windows", "O Docker Desktop normalmente executa containers Linux dentro de uma VM leve baseada em WSL2. Para nos, a experiencia continua sendo docker e docker compose no PowerShell.", LIGHT_BLUE))

    s.append(h1("2. Conceitos fundamentais"))
    s.append(data_table(
        ["Termo", "Definicao simples"],
        [
            ["Dockerfile", "Receita para construir uma imagem."],
            ["Imagem", "Modelo imutavel em camadas."],
            ["Container", "Processo em execucao criado da imagem."],
            ["Registry", "Repositorio de imagens, como Docker Hub ou ECR."],
            ["Tag", "Nome de uma versao logica da imagem, como latest."],
            ["Volume", "Armazenamento com ciclo de vida separado do container."],
            ["Network", "Rede virtual e DNS entre containers."],
            ["Port mapping", "Ligacao entre porta do host e do container."],
            ["Compose", "Descricao de varios containers como um ambiente."],
        ], [42 * mm, 126 * mm],
    ))
    s.append(h2("2.1 Imutabilidade e estado"))
    s.append(paragraph("Substituir um container e normal. Por isso dados importantes nao devem existir somente na camada gravavel do container. postgres-data preserva o banco e kafka-data preserva registros e offsets."))

    s.append(h1("3. Dockerfile do projeto"))
    s.append(code_block("FROM eclipse-temurin:21-jre\nWORKDIR /app\nCOPY target/ProjetoSpringBoot-0.0.1-SNAPSHOT.jar app.jar\nEXPOSE 8080\nENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]"))
    s.append(h2("3.1 Linha por linha"))
    s.extend(bullets([
        "FROM escolhe uma imagem base com Java 21 JRE.",
        "WORKDIR define /app como diretorio dos proximos passos e da execucao.",
        "COPY leva o JAR ja construido para dentro da imagem.",
        "EXPOSE documenta a porta usada pela aplicacao; sozinho nao publica a porta.",
        "ENTRYPOINT define o processo principal. Se java terminar, o container termina.",
    ]))
    s.append(h2("3.2 Build no PowerShell"))
    s.append(code_block("# O Dockerfile espera o JAR, portanto empacote primeiro\n.\\mvnw.cmd clean package\n\n# Constroi e aplica uma tag\ndocker build -t projeto-springboot:latest .\n\n# Confere a imagem\ndocker image inspect projeto-springboot:latest\ndocker image ls projeto-springboot"))

    s.append(h1("4. Docker Compose"))
    s.append(paragraph("Compose le compose.yaml e cria containers, rede, volumes e configuracoes como uma unidade. O projeto cresceu por etapas: primeiro aplicacao, PostgreSQL e Redis; depois Kafka; por fim a pilha de observabilidade."))
    s.append(h2("4.1 Servicos atuais"))
    s.append(data_table(
        ["Servico", "Imagem ou origem", "Porta no Windows", "Papel"],
        [
            ["app", "Dockerfile local", "8080", "Spring Boot"],
            ["postgres", "postgres:17", "5432", "Banco relacional"],
            ["redis", "redis:7", "6379", "Cache"],
            ["kafka", "apache/kafka:4.3.1", "9092", "Mensageria"],
            ["kafka-ui", "ghcr.io/kafbat/kafka-ui", "8083", "Interface do Kafka"],
            ["jaeger", "jaegertracing/all-in-one:1.76.0", "16686/4317/4318", "Traces"],
            ["loki", "grafana/loki:3.7.0", "3100", "Logs"],
            ["alloy", "grafana/alloy:v1.19.0", "12345", "Coleta logs"],
            ["prometheus", "prom/prometheus:v3.14.0", "9090", "Metricas"],
            ["grafana", "grafana/grafana:13.1.3", "3000", "Visualizacao"],
        ], [28 * mm, 57 * mm, 36 * mm, 47 * mm],
    ))

    s.append(h1("5. Redes e nomes DNS"))
    s.append(paragraph("Compose cria uma rede padrao e registra cada nome de servico no DNS interno. A aplicacao dentro do container encontra jdbc:postgresql://postgres:5432 e redis:6379. Esses nomes nao existem diretamente no Windows."))
    s.append(code_block("Windows/IDE                 Rede interna do Compose\nlocalhost:5432  ----------> postgres:5432\nlocalhost:6379  ----------> redis:6379\nlocalhost:9092  ----------> kafka:9092 (listener externo)\napp -> kafka:29092          listener interno\napp -> jaeger:4318          OTLP HTTP interno"))
    s.append(h2("5.1 Portas"))
    s.append(paragraph("Em \"8080:8080\", a esquerda esta a porta do Windows e a direita a porta do container. Duas aplicacoes nao podem ocupar a mesma porta do host ao mesmo tempo, embora containers distintos possam usar internamente o mesmo numero."))
    s.append(h2("5.2 Kafka com dois listeners"))
    s.append(paragraph("Kafka anuncia localhost:9092 para clientes no Windows e kafka:29092 para clientes na rede Docker. O endereco anunciado precisa ser alcancavel pelo cliente; apenas abrir uma porta nao corrige um advertised listener errado."))

    s.append(h1("6. Volumes e persistencia"))
    s.append(data_table(
        ["Volume", "Montagem", "O que preserva"],
        [
            ["postgres-data", "/var/lib/postgresql/data", "Tabelas e WAL do PostgreSQL"],
            ["kafka-data", "/var/lib/kafka/data", "Topicos, mensagens e offsets"],
            ["prometheus-data", "/prometheus", "Series temporais"],
            ["grafana-data", "/var/lib/grafana", "Estado interno do Grafana"],
            ["loki-data", "/tmp/loki", "Indices e blocos de logs"],
            ["alloy-data", "/var/lib/alloy/data", "Posicao lida nos arquivos"],
        ], [42 * mm, 58 * mm, 68 * mm],
    ))
    s.append(h2("6.1 Bind mount"))
    s.append(paragraph("./logs:/app/logs liga uma pasta do projeto ao container. A aplicacao grava no mesmo diretorio que o Alloy le como somente leitura. Diferente de um volume nomeado, o caminho do host fica explicito."))
    s.append(callout("Operacao destrutiva", "docker compose down remove containers e rede, mas preserva volumes nomeados. docker compose down -v tambem remove os volumes e pode apagar o banco local. Use -v somente quando essa perda for intencional.", YELLOW))

    s.append(h1("7. Variaveis, YAML e perfis Spring"))
    s.append(paragraph("YAML representa hierarquia por indentacao. Espacos importam; tabs nao devem ser usadas. Compose usa environment para passar SPRING_PROFILES_ACTIVE=docker. O Spring entao combina application.yaml com application-docker.yaml, e o arquivo do perfil sobrescreve valores correspondentes."))
    s.append(code_block("# Compose\nenvironment:\n  SPRING_PROFILES_ACTIVE: docker\n\n# application-docker.yaml\nspring:\n  datasource:\n    url: jdbc:postgresql://postgres:5432/projetodb\n  data:\n    redis:\n      host: redis\n      port: 6379\n  cache:\n    type: redis\n    redis:\n      time-to-live: 10m"))
    s.append(h2("7.1 Por que AWS foi desligada no container local"))
    s.append(paragraph("O perfil AWS CLI projeto-s3 esta salvo no Windows e usa sessao temporaria. O container nao recebe esse perfil. No application-docker.yaml, SQS e SNS ficam desabilitados para que Docker, PostgreSQL, Redis, Kafka e observabilidade funcionem independentemente da AWS."))

    s.append(h1("8. depends_on e prontidao"))
    s.append(paragraph("depends_on controla ordem de inicio, mas iniciar um processo nao garante que ele ja aceita conexoes. O Jenkins usa docker compose up --wait, porem healthchecks explicitos poderiam tornar a verificacao mais precisa no futuro."))
    s.append(code_block("app:\n  depends_on:\n    - postgres\n    - redis\n    - kafka\n    - jaeger\n\n# Ideia futura: healthcheck no PostgreSQL\nhealthcheck:\n  test: [\"CMD-SHELL\", \"pg_isready -U postgres -d projetodb\"]\n  interval: 5s\n  timeout: 3s\n  retries: 10"))

    s.append(h1("9. Ciclo diario no PowerShell"))
    s.append(code_block("# Entre no projeto\nSet-Location C:\\Recomeco\\projetos\\ProjetoSpringBoot\n\n# Valide o arquivo final combinado\ndocker compose config\n\n# Construa o JAR e as imagens\n.\\mvnw.cmd clean package\ndocker compose build\n\n# Inicie em segundo plano e aguarde\ndocker compose up -d --wait\n\n# Veja o estado\ndocker compose ps\n\n# Acompanhe a aplicacao; Ctrl+C para sair dos logs\ndocker compose logs -f app\n\n# Pare e remova containers, preservando volumes\ndocker compose down"))
    s.append(h2("9.1 Iniciar apenas infraestrutura"))
    s.append(code_block("# Util para rodar a aplicacao pela IDE\ndocker compose up -d postgres redis kafka kafka-ui jaeger loki alloy prometheus grafana\n\n# Confirme\ndocker compose ps"))
    s.append(paragraph("Nesse modo, application.yaml usa localhost para Kafka e Jaeger. Para usar PostgreSQL e Redis pela IDE, e necessario selecionar configuracoes apropriadas; o perfil docker completo espera nomes internos e nao deve ser ativado fora da rede sem ajustes."))

    s.append(h1("10. Inspecao e diagnostico"))
    s.append(h2("10.1 Estado e logs"))
    s.append(code_block("docker compose ps\ndocker compose logs --tail 100 postgres\ndocker compose logs --since 10m redis\ndocker compose logs -f kafka\n\n# Processos e consumo\ndocker stats\n\n# Detalhes de um container\ndocker inspect projeto-postgres"))
    s.append(h2("10.2 Testes de rede"))
    s.append(code_block("Test-NetConnection localhost -Port 8080\nTest-NetConnection localhost -Port 5432\nTest-NetConnection localhost -Port 6379\nTest-NetConnection localhost -Port 9092\nTest-NetConnection localhost -Port 3000\n\n# HTTP\nInvoke-WebRequest http://localhost:8080/ProjetoSpringBoot/actuator/health\nInvoke-WebRequest http://localhost:9090/-/healthy"))
    s.append(h2("10.3 Entrar no container"))
    s.append(code_block("# Shell Linux dentro do container da aplicacao\ndocker compose exec app sh\n\n# PostgreSQL\ndocker compose exec postgres psql -U postgres -d projetodb\n\n# Redis\ndocker compose exec redis redis-cli PING"))

    s.append(h1("11. Problemas comuns"))
    s.append(data_table(
        ["Sintoma", "Causa provavel", "Primeira verificacao"],
        [
            ["Porta em uso", "Outro processo/container", "docker compose ps e Get-NetTCPConnection"],
            ["JAR nao encontrado", "Package nao executado", ".\\mvnw.cmd clean package"],
            ["Banco reinicia vazio", "Volume diferente/removido", "docker volume ls"],
            ["app nao acha postgres", "Host localhost dentro do container", "Usar postgres:5432"],
            ["IDE nao acha kafka", "Endereco interno kafka:29092", "Usar localhost:9092"],
            ["Trace falha em 4318", "Jaeger parado ou endpoint errado", "docker compose ps jaeger"],
            ["AWS expirada", "Sessao STS venceu", "Nao e falha do Docker; addon pode ficar desligado"],
        ], [42 * mm, 66 * mm, 60 * mm],
    ))
    s.append(code_block("# Descobre quem escuta uma porta no Windows\nGet-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue `\n  | Select-Object LocalAddress, LocalPort, State, OwningProcess\n\n# Exibe o processo pelo PID encontrado\nGet-Process -Id <PID>"))

    s.append(h1("12. Imagem, registry e proximos passos"))
    s.append(paragraph("Hoje construimos a imagem localmente. Um registry armazena e distribui imagens. Docker Hub e generico; Amazon ECR e o registry da AWS. Futuramente o CI pode testar, criar uma tag imutavel, publicar no ECR e um ECS pode executar essa imagem."))
    s.append(code_block("Codigo -> Maven -> JAR -> docker build -> imagem local\n                                  -> push no registry\n                                  -> ambiente executa exatamente essa imagem"))
    s.append(h2("12.1 Checklist antes de encerrar"))
    s.extend(bullets([
        "Conferir docker compose ps e logs de erro.",
        "Parar o ambiente com docker compose down quando nao estiver estudando.",
        "Nao usar -v se deseja preservar PostgreSQL, Kafka e metricas.",
        "Revisar imagens antigas com docker image ls antes de remover.",
        "Manter senhas reais fora do Compose; as atuais sao apenas do laboratorio local.",
    ]))

    s.append(h1("13. Referencia rapida de comandos"))
    s.append(data_table(
        ["Objetivo", "Comando"],
        [
            ["Validar YAML", "docker compose config"],
            ["Construir", "docker compose build"],
            ["Iniciar", "docker compose up -d --wait"],
            ["Estado", "docker compose ps"],
            ["Logs", "docker compose logs -f app"],
            ["Parar", "docker compose stop"],
            ["Remover containers", "docker compose down"],
            ["Listar volumes", "docker volume ls"],
            ["Consumo", "docker stats"],
            ["Limpeza criteriosa", "docker image ls antes de docker image rm <imagem>"],
        ], [52 * mm, 116 * mm],
    ))
    s.append(h2("13.1 Regra mental final"))
    s.append(code_block("Dockerfile constroi UMA imagem\nImagem inicia UM OU MAIS containers\nCompose descreve VARIOS servicos\nNetwork permite comunicacao por nome\nPorta publica acesso no host\nVolume preserva estado fora do container"))
    s.append(callout("Projeto Recomeco", "Nosso Compose e uma miniatura didatica de um ambiente distribuido. Cada container tem um papel independente, mas todos podem ser iniciados, observados e encerrados como uma unidade.", GREEN))
    return s


GUIDES = [
    (
        "01_Guia_API_REST_PostgreSQL_Redis_Projeto_Recomeco.pdf",
        "Projeto Recomeco - API REST, PostgreSQL e Redis",
        "API REST, dados relacionais e cache",
        api_data_story,
    ),
    (
        "02_Guia_Testes_Automatizados_Projeto_Recomeco.pdf",
        "Projeto Recomeco - Testes Automatizados",
        "JUnit, Mockito, MockMvc e testes de integracao",
        tests_story,
    ),
    (
        "03_Guia_Git_GitHub_CICD_Jenkins_Projeto_Recomeco.pdf",
        "Projeto Recomeco - Git, GitHub e CI/CD",
        "Git, Pull Requests, GitHub Actions e Jenkins",
        git_cicd_story,
    ),
    (
        "04_Guia_Docker_Ambientes_Projeto_Recomeco.pdf",
        "Projeto Recomeco - Docker e Ambientes",
        "Docker, Compose, redes, volumes e infraestrutura local",
        docker_story,
    ),
]


def build_pdf(filename: str, header: str, subject: str, story_factory) -> Path:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    output = OUTPUT_DIR / filename
    frame = Frame(
        18 * mm,
        15 * mm,
        A4[0] - 36 * mm,
        A4[1] - 31 * mm,
        id="body",
        topPadding=4 * mm,
        bottomPadding=4 * mm,
    )
    doc = GuideDocument(
        str(output),
        pagesize=A4,
        title=header,
        author="Projeto Recomeco",
        subject=subject,
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=17 * mm,
        bottomMargin=15 * mm,
    )
    doc.addPageTemplates([
        PageTemplate(id="Portrait", pagesize=A4, frames=[frame], onPage=page_decoration(header))
    ])
    doc.multiBuild(story_factory())
    return output


def main() -> None:
    for guide in GUIDES:
        print(build_pdf(*guide))


if __name__ == "__main__":
    main()
