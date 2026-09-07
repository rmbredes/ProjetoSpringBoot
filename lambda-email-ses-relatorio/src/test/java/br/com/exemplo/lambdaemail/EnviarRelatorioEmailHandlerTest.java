package br.com.exemplo.lambdaemail;

import br.com.exemplo.lambdaemail.mensagem.LeitorEventoRelatorio;
import br.com.exemplo.lambdaemail.s3.LeitorArquivoS3;
import br.com.exemplo.lambdaemail.ses.EnviadorEmailSes;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testa o handler sem acessar SNS, Lambda ou CloudWatch reais. */
class EnviarRelatorioEmailHandlerTest {

    /** Confirma que uma mensagem SNS é lida e registrada pelo handler. */
    @Test
    void deveProcessarMensagemRecebidaDoSns() {
        String mensagemJson = """
                {
                  "eventoId":"evento-123",
                  "tipo":"RELATORIO_PEDIDO_DISPONIVEL",
                  "relatorioId":15,
                  "pedidoId":809,
                  "solicitacaoId":"solicitacao-123",
                  "objectKey":"pedidos/809/relatorios/solicitacao-123.pdf",
                  "ocorridoEm":"2026-09-04T19:56:48Z"
                }
                """.trim();

        SNSEvent.SNS notificacao = new SNSEvent.SNS();
        notificacao.setMessage(mensagemJson);

        SNSEvent.SNSRecord registro = new SNSEvent.SNSRecord();
        registro.setSns(notificacao);

        SNSEvent evento = new SNSEvent();
        evento.setRecords(List.of(registro));

        LambdaLogger logger = mock(LambdaLogger.class);
        Context contexto = mock(Context.class);

        when(contexto.getLogger()).thenReturn(logger);
        when(contexto.getAwsRequestId()).thenReturn("request-teste-123");

        LeitorEventoRelatorio leitorEvento =
                new LeitorEventoRelatorio();
        LeitorArquivoS3 leitorArquivoS3 =
                mock(LeitorArquivoS3.class);
        EnviadorEmailSes enviadorEmailSes =
                mock(EnviadorEmailSes.class);

        when(leitorArquivoS3.baixar(
                "pedidos/809/relatorios/solicitacao-123.pdf"
        )).thenReturn(new byte[]{1, 2, 3, 4});

        when(enviadorEmailSes.enviar(
                any(),
                any(byte[].class)
        )).thenReturn("mensagem-ses-123");

        EnviarRelatorioEmailHandler handler =
                new EnviarRelatorioEmailHandler(
                        leitorEvento,
                        leitorArquivoS3,
                        enviadorEmailSes
                );

        Integer quantidade = handler.handleRequest(evento, contexto);

        assertEquals(1, quantidade);
        verify(logger).log(
                "E-mail enviado pelo SES. pedidoId=809"
                        + ", relatorioId=15"
                        + ", objectKey=pedidos/809/relatorios/solicitacao-123.pdf"
                        + ", tamanhoBytes=4"
                        + ", mensagemId=mensagem-ses-123"
                        + System.lineSeparator()
        );
        verify(enviadorEmailSes).enviar(
                any(),
                any(byte[].class)
        );
    }

    /** Confirma que um evento vazio termina normalmente. */
    @Test
    void deveAceitarEventoSemRegistros() {
        LambdaLogger logger = mock(LambdaLogger.class);
        Context contexto = mock(Context.class);

        when(contexto.getLogger()).thenReturn(logger);
        when(contexto.getAwsRequestId()).thenReturn("request-vazio-123");

        EnviarRelatorioEmailHandler handler =
                new EnviarRelatorioEmailHandler(
                        mock(LeitorEventoRelatorio.class),
                        mock(LeitorArquivoS3.class),
                        mock(EnviadorEmailSes.class)
                );

        Integer quantidade = handler.handleRequest(
                new SNSEvent(),
                contexto
        );

        assertEquals(0, quantidade);
    }
}
