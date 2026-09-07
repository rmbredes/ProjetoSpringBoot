package br.com.exemplo.lambdaemail.mensagem;

import br.com.exemplo.lambdaemail.dto.EventoRelatorioDisponivel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Testa somente a transformação do JSON recebido do SNS. */
class LeitorEventoRelatorioTest {

    @Test
    void deveConverterJsonEmEventoJava() {
        String json = """
                {
                  "eventoId":"evento-123",
                  "tipo":"RELATORIO_PEDIDO_DISPONIVEL",
                  "relatorioId":15,
                  "pedidoId":809,
                  "solicitacaoId":"solicitacao-123",
                  "objectKey":"pedidos/809/relatorios/arquivo.pdf",
                  "ocorridoEm":"2026-09-04T19:56:48Z"
                }
                """;

        EventoRelatorioDisponivel evento =
                new LeitorEventoRelatorio().ler(json);

        assertEquals("evento-123", evento.eventoId());
        assertEquals("RELATORIO_PEDIDO_DISPONIVEL", evento.tipo());
        assertEquals(15L, evento.relatorioId());
        assertEquals(809L, evento.pedidoId());
        assertEquals(
                "pedidos/809/relatorios/arquivo.pdf",
                evento.objectKey()
        );
    }

    @Test
    void deveRecusarMensagemQueNaoSejaJson() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LeitorEventoRelatorio().ler("mensagem-invalida")
        );
    }
}
