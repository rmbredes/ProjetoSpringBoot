package br.com.exemplo.projetospringboot.report;

import br.com.exemplo.projetospringboot.entity.Cliente;
import br.com.exemplo.projetospringboot.entity.Pedido;
import br.com.exemplo.projetospringboot.entity.RelatorioPedido;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Gera um PDF pequeno contendo o resumo de um pedido.
 *
 * <p>A classe não conhece SQS, S3 ou banco de dados. Ela apenas recebe
 * os dados já carregados e devolve os bytes do documento.</p>
 *
 * <p>Para reduzir o tamanho, o relatório possui uma única página,
 * não utiliza imagens e emprega fontes padrão do formato PDF.</p>
 */
@Component
public class GeradorRelatorioPedidoPdf {

    /** Formato brasileiro utilizado para valores monetários. */
    private static final NumberFormat FORMATO_MOEDA =
            NumberFormat.getCurrencyInstance(
                    Locale.of("pt", "BR")
            );

    /** Formato compacto utilizado para datas apresentadas no relatório. */
    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /** Fonte normal padrão, sem arquivo externo incorporado. */
    private static final PDType1Font FONTE_NORMAL =
            new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA
            );

    /** Fonte em negrito usada em títulos e rótulos. */
    private static final PDType1Font FONTE_NEGRITO =
            new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA_BOLD
            );

    static {
        FORMATO_MOEDA.setRoundingMode(RoundingMode.HALF_EVEN);
    }

    /**
     * Cria o documento integralmente em memória.
     *
     * @param relatorio solicitação associada ao pedido
     * @return bytes que formam um arquivo PDF válido
     */
    public byte[] gerar(RelatorioPedido relatorio) {
        Pedido pedido = relatorio.getPedido();
        Cliente cliente = pedido.getCliente();

        try (
                PDDocument documento = new PDDocument();
                ByteArrayOutputStream saida = new ByteArrayOutputStream()
        ) {
            configurarMetadados(documento, pedido);

            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);

            try (PDPageContentStream conteudo =
                         new PDPageContentStream(documento, pagina)) {

                desenharCabecalho(conteudo);

                float y = 690;

                y = escreverCampo(
                        conteudo,
                        "Pedido",
                        String.valueOf(pedido.getId()),
                        y
                );

                y = escreverCampo(
                        conteudo,
                        "Cliente",
                        cliente.getNome(),
                        y
                );

                y = escreverCampo(
                        conteudo,
                        "E-mail",
                        cliente.getEmail(),
                        y
                );

                y = escreverCampo(
                        conteudo,
                        "Valor",
                        FORMATO_MOEDA.format(pedido.getValor()),
                        y
                );

                y = escreverCampo(
                        conteudo,
                        "Criado em",
                        FORMATO_DATA.format(pedido.getDataCriacao()),
                        y
                );

                escreverCampo(
                        conteudo,
                        "Relatório gerado em",
                        formatarInstante(Instant.now()),
                        y
                );

                desenharRodape(
                        conteudo,
                        relatorio.getSolicitacaoId().toString()
                );
            }

            documento.save(saida);

            return saida.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Não foi possível gerar o PDF do pedido",
                    exception
            );
        }
    }

    /** Adiciona metadados básicos sem aumentar significativamente o arquivo. */
    private void configurarMetadados(
            PDDocument documento,
            Pedido pedido
    ) {
        PDDocumentInformation informacoes =
                documento.getDocumentInformation();

        informacoes.setTitle(
                "Relatório do Pedido " + pedido.getId()
        );
        informacoes.setAuthor("ProjetoSpringBoot");
        informacoes.setSubject("Resumo do pedido");
    }

    /** Desenha a faixa azul e o título no topo da página. */
    private void desenharCabecalho(
            PDPageContentStream conteudo
    ) throws IOException {
        /*
         * No PDFBox 3, cada componente RGB utiliza a escala de 0 a 1.
         * Dividimos os valores usuais de 0 a 255 para obter essa escala.
         */
        conteudo.setNonStrokingColor(
                31 / 255f,
                78 / 255f,
                121 / 255f
        );
        conteudo.addRect(0, 750, PDRectangle.A4.getWidth(), 92);
        conteudo.fill();

        conteudo.setNonStrokingColor(1f, 1f, 1f);
        escreverTexto(
                conteudo,
                FONTE_NEGRITO,
                22,
                48,
                795,
                "RELATÓRIO DO PEDIDO"
        );

        escreverTexto(
                conteudo,
                FONTE_NORMAL,
                10,
                49,
                775,
                "Resumo gerado de forma assíncrona pelo Amazon SQS"
        );
    }

    /**
     * Escreve um rótulo e seu valor, devolvendo a posição da próxima linha.
     */
    private float escreverCampo(
            PDPageContentStream conteudo,
            String rotulo,
            String valor,
            float y
    ) throws IOException {
        conteudo.setNonStrokingColor(
                70 / 255f,
                70 / 255f,
                70 / 255f
        );
        escreverTexto(
                conteudo,
                FONTE_NEGRITO,
                11,
                50,
                y,
                rotulo + ":"
        );

        conteudo.setNonStrokingColor(
                20 / 255f,
                20 / 255f,
                20 / 255f
        );
        escreverTexto(
                conteudo,
                FONTE_NORMAL,
                11,
                190,
                y,
                valor
        );

        conteudo.setStrokingColor(
                225 / 255f,
                225 / 255f,
                225 / 255f
        );
        conteudo.moveTo(50, y - 10);
        conteudo.lineTo(545, y - 10);
        conteudo.stroke();

        return y - 48;
    }

    /** Desenha o identificador de rastreamento no rodapé. */
    private void desenharRodape(
            PDPageContentStream conteudo,
            String solicitacaoId
    ) throws IOException {
        conteudo.setNonStrokingColor(
                110 / 255f,
                110 / 255f,
                110 / 255f
        );

        escreverTexto(
                conteudo,
                FONTE_NORMAL,
                8,
                50,
                42,
                "Solicitação: " + solicitacaoId
        );

        escreverTexto(
                conteudo,
                FONTE_NORMAL,
                8,
                438,
                42,
                "Projeto de estudo AWS"
        );
    }

    /** Executa a sequência exigida pelo PDF para escrever uma linha de texto. */
    private void escreverTexto(
            PDPageContentStream conteudo,
            PDType1Font fonte,
            float tamanho,
            float x,
            float y,
            String texto
    ) throws IOException {
        conteudo.beginText();
        conteudo.setFont(fonte, tamanho);
        conteudo.newLineAtOffset(x, y);
        conteudo.showText(texto);
        conteudo.endText();
    }

    /** Converte o Instant UTC para o fuso horário da máquina da aplicação. */
    private String formatarInstante(Instant instante) {
        return FORMATO_DATA.format(
                instante.atZone(ZoneId.systemDefault())
        );
    }
}
