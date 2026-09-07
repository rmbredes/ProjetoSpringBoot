package br.com.exemplo.projetospringboot.report;

import br.com.exemplo.projetospringboot.entity.Cliente;
import br.com.exemplo.projetospringboot.entity.Pedido;
import br.com.exemplo.projetospringboot.entity.RelatorioPedido;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes do gerador compacto de relatórios PDF. */
class GeradorRelatorioPedidoPdfTest {

    /**
     * Confirma que o conteúdo é um PDF legível, contém os dados
     * esperados e permanece pequeno.
     */
    @Test
    void deveGerarPdfPequenoComOsDadosDoPedido()
            throws Exception {
        Cliente cliente = new Cliente(
                7L,
                "Ricardo Silva",
                "ricardo@exemplo.com",
                true
        );

        Pedido pedido = new Pedido(
                799L,
                new BigDecimal("149.90"),
                LocalDateTime.of(2026, 9, 3, 20, 30),
                cliente
        );

        RelatorioPedido relatorio = new RelatorioPedido(
                UUID.randomUUID(),
                pedido
        );

        byte[] pdf = new GeradorRelatorioPedidoPdf()
                .gerar(relatorio);

        /* A assinatura textual %PDF identifica o formato do arquivo. */
        assertTrue(
                new String(pdf, 0, 4).equals("%PDF")
        );

        /* Mantemos uma margem confortável: o arquivo deve ficar abaixo de 20 KB. */
        assertTrue(
                pdf.length < 20 * 1024,
                "O PDF deveria ser menor que 20 KB, mas possui "
                        + pdf.length
                        + " bytes"
        );

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper()
                    .getText(documento);

            assertTrue(texto.contains("RELATÓRIO DO PEDIDO"));
            assertTrue(texto.contains("799"));
            assertTrue(texto.contains("Ricardo Silva"));
        }
    }
}
