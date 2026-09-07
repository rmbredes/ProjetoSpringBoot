package br.com.exemplo.lambdaemail.ses;

import br.com.exemplo.lambdaemail.dto.EventoRelatorioDisponivel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Attachment;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testa a montagem do e-mail sem chamar o SES verdadeiro. */
class EnviadorEmailSesTest {

    @Test
    void deveEnviarEmailFormatadoComPdfAnexado() {
        SesV2Client sesClient = mock(SesV2Client.class);

        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder()
                        .messageId("mensagem-ses-123")
                        .build());

        EnviadorEmailSes enviador = new EnviadorEmailSes(
                sesClient,
                "remetente@exemplo.com",
                "destinatario@exemplo.com"
        );

        EventoRelatorioDisponivel evento =
                new EventoRelatorioDisponivel(
                        "evento-123",
                        "RELATORIO_PEDIDO_DISPONIVEL",
                        15L,
                        809L,
                        "solicitacao-123",
                        "pedidos/809/relatorios/arquivo.pdf",
                        "2026-09-04T19:56:48Z"
                );

        byte[] pdf = new byte[]{1, 2, 3, 4};
        String messageId = enviador.enviar(evento, pdf);

        assertEquals("mensagem-ses-123", messageId);

        ArgumentCaptor<SendEmailRequest> captor =
                ArgumentCaptor.forClass(SendEmailRequest.class);

        verify(sesClient).sendEmail(captor.capture());

        SendEmailRequest requisicao = captor.getValue();
        assertEquals(
                "remetente@exemplo.com",
                requisicao.fromEmailAddress()
        );
        assertEquals(
                "destinatario@exemplo.com",
                requisicao.destination().toAddresses().getFirst()
        );
        assertEquals(
                "Relatório do pedido 809",
                requisicao.content().simple().subject().data()
        );

        Attachment anexo =
                requisicao.content().simple().attachments().getFirst();

        assertEquals("relatorio-pedido-809.pdf", anexo.fileName());
        assertEquals("application/pdf", anexo.contentType());
        assertArrayEquals(pdf, anexo.rawContent().asByteArray());
    }

    @Test
    void deveRecusarRemetenteVazio() {
        assertThrows(
                IllegalStateException.class,
                () -> new EnviadorEmailSes(
                        mock(SesV2Client.class),
                        " ",
                        "destinatario@exemplo.com"
                )
        );
    }
}
