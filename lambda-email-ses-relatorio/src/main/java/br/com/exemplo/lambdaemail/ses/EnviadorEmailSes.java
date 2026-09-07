package br.com.exemplo.lambdaemail.ses;

import br.com.exemplo.lambdaemail.dto.EventoRelatorioDisponivel;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Attachment;
import software.amazon.awssdk.services.sesv2.model.AttachmentContentDisposition;
import software.amazon.awssdk.services.sesv2.model.AttachmentContentTransferEncoding;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

/**
 * Monta e envia pelo Amazon SES o e-mail com o relatório em anexo.
 *
 * <p>A classe não conhece SNS nem S3. Ela recebe o evento já convertido e os
 * bytes do PDF já baixado. Sua única responsabilidade é criar e enviar o
 * e-mail.</p>
 */
public class EnviadorEmailSes {

    public static final String VARIAVEL_REMETENTE = "EMAIL_REMETENTE";
    public static final String VARIAVEL_DESTINATARIO = "EMAIL_DESTINATARIO";

    private final SesV2Client sesClient;
    private final String remetente;
    private final String destinatario;

    /** Construtor usado pelo runtime da AWS Lambda. */
    public EnviadorEmailSes() {
        this(
                SesV2Client.create(),
                System.getenv(VARIAVEL_REMETENTE),
                System.getenv(VARIAVEL_DESTINATARIO)
        );
    }

    /** Construtor usado pelos testes com um cliente SES controlado. */
    EnviadorEmailSes(
            SesV2Client sesClient,
            String remetente,
            String destinatario
    ) {
        this.sesClient = sesClient;
        this.remetente = validarEmail(remetente, VARIAVEL_REMETENTE);
        this.destinatario = validarEmail(
                destinatario,
                VARIAVEL_DESTINATARIO
        );
    }

    /**
     * Envia um e-mail contendo versões em texto e HTML e o PDF anexado.
     *
     * @param evento dados do relatório usados no assunto e no corpo
     * @param arquivoPdf bytes do PDF obtidos anteriormente no S3
     * @return identificador atribuído pelo SES à mensagem enviada
     */
    public String enviar(
            EventoRelatorioDisponivel evento,
            byte[] arquivoPdf
    ) {
        String nomeArquivo = "relatorio-pedido-"
                + evento.pedidoId()
                + ".pdf";

        Attachment anexo = criarAnexo(nomeArquivo, arquivoPdf);
        Message mensagem = criarMensagem(evento, anexo);

        SendEmailRequest requisicao = SendEmailRequest.builder()
                .fromEmailAddress(remetente)
                .destination(Destination.builder()
                        .toAddresses(destinatario)
                        .build())
                .content(EmailContent.builder()
                        .simple(mensagem)
                        .build())
                .build();

        SendEmailResponse resposta = sesClient.sendEmail(requisicao);
        return resposta.messageId();
    }

    /** Monta o anexo binário que será incluído na mensagem. */
    private Attachment criarAnexo(
            String nomeArquivo,
            byte[] arquivoPdf
    ) {
        return Attachment.builder()
                .rawContent(SdkBytes.fromByteArray(arquivoPdf))
                .fileName(nomeArquivo)
                .contentType("application/pdf")
                .contentDisposition(
                        AttachmentContentDisposition.ATTACHMENT
                )
                .contentTransferEncoding(
                        AttachmentContentTransferEncoding.BASE64
                )
                .contentDescription("Relatório do pedido")
                .build();
    }

    /** Monta o assunto e as duas versões do corpo do e-mail. */
    private Message criarMensagem(
            EventoRelatorioDisponivel evento,
            Attachment anexo
    ) {
        String assunto = "Relatório do pedido " + evento.pedidoId();

        String corpoTexto = "O relatório do pedido "
                + evento.pedidoId()
                + " está disponível e segue anexado a este e-mail.";

        String corpoHtml = """
                <html>
                  <body>
                    <h2>Relatório disponível</h2>
                    <p>O relatório do pedido <strong>%d</strong> foi gerado.</p>
                    <p>O arquivo PDF segue anexado a este e-mail.</p>
                    <hr>
                    <small>Mensagem automática do Projeto Recomeço.</small>
                  </body>
                </html>
                """.formatted(evento.pedidoId());

        return Message.builder()
                .subject(criarConteudo(assunto))
                .body(Body.builder()
                        .text(criarConteudo(corpoTexto))
                        .html(criarConteudo(corpoHtml))
                        .build())
                .attachments(anexo)
                .build();
    }

    /** Informa explicitamente que os textos utilizam UTF-8. */
    private Content criarConteudo(String texto) {
        return Content.builder()
                .data(texto)
                .charset("UTF-8")
                .build();
    }

    /** Impede a execução quando uma variável de e-mail estiver ausente. */
    private String validarEmail(
            String email,
            String nomeVariavel
    ) {
        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                    "A variável de ambiente "
                            + nomeVariavel
                            + " não foi configurada"
            );
        }

        return email;
    }
}
