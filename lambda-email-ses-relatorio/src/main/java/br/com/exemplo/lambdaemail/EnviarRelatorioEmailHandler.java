package br.com.exemplo.lambdaemail;

import br.com.exemplo.lambdaemail.dto.EventoRelatorioDisponivel;
import br.com.exemplo.lambdaemail.mensagem.LeitorEventoRelatorio;
import br.com.exemplo.lambdaemail.s3.LeitorArquivoS3;
import br.com.exemplo.lambdaemail.ses.EnviadorEmailSes;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;

import java.util.List;

/**
 * Ponto de entrada da função Lambda responsável pelo e-mail do relatório.
 *
 * <p>Nesta primeira etapa, a função apenas recebe o evento do SNS e registra
 * seu conteúdo. Tudo que for escrito com {@link LambdaLogger} será enviado
 * automaticamente pelo runtime Lambda para o Amazon CloudWatch Logs.</p>
 *
 * <p>Esta classe coordena o fluxo completo: converte a mensagem, baixa o PDF
 * no S3 e solicita o envio do e-mail pelo Amazon SES.</p>
 */
public class EnviarRelatorioEmailHandler
        implements RequestHandler<SNSEvent, Integer> {

    private final LeitorEventoRelatorio leitorEvento;
    private final LeitorArquivoS3 leitorArquivoS3;
    private final EnviadorEmailSes enviadorEmailSes;

    /** Construtor utilizado automaticamente pelo runtime da AWS Lambda. */
    public EnviarRelatorioEmailHandler() {
        this(
                new LeitorEventoRelatorio(),
                new LeitorArquivoS3(),
                new EnviadorEmailSes()
        );
    }

    /** Permite fornecer dependências controladas durante os testes. */
    EnviarRelatorioEmailHandler(
            LeitorEventoRelatorio leitorEvento,
            LeitorArquivoS3 leitorArquivoS3,
            EnviadorEmailSes enviadorEmailSes
    ) {
        this.leitorEvento = leitorEvento;
        this.leitorArquivoS3 = leitorArquivoS3;
        this.enviadorEmailSes = enviadorEmailSes;
    }

    /**
     * Método chamado automaticamente pela AWS quando o SNS invoca a função.
     *
     * @param evento envelope contendo uma ou mais notificações do SNS
     * @param contexto informações da execução atual fornecidas pela AWS
     * @return quantidade de mensagens SNS processadas nesta invocação
     */
    @Override
    public Integer handleRequest(
            SNSEvent evento,
            Context contexto
    ) {
        LambdaLogger logger = contexto.getLogger();

        logger.log(
                "Lambda iniciada. requestId="
                        + contexto.getAwsRequestId()
                        + System.lineSeparator()
        );

        List<SNSEvent.SNSRecord> registros = obterRegistros(evento);

        for (SNSEvent.SNSRecord registro : registros) {
            String mensagem = registro.getSNS().getMessage();
            EventoRelatorioDisponivel eventoRelatorio =
                    leitorEvento.ler(mensagem);

            byte[] arquivoPdf = leitorArquivoS3.baixar(
                    eventoRelatorio.objectKey()
            );

            String mensagemId = enviadorEmailSes.enviar(
                    eventoRelatorio,
                    arquivoPdf
            );

            // O identificador confirma que o SES aceitou a mensagem.
            logger.log(
                    "E-mail enviado pelo SES. pedidoId="
                            + eventoRelatorio.pedidoId()
                            + ", relatorioId="
                            + eventoRelatorio.relatorioId()
                            + ", objectKey="
                            + eventoRelatorio.objectKey()
                            + ", tamanhoBytes="
                            + arquivoPdf.length
                            + ", mensagemId="
                            + mensagemId
                            + System.lineSeparator()
            );
        }

        logger.log(
                "Lambda concluída. mensagensProcessadas="
                        + registros.size()
                        + System.lineSeparator()
        );

        return registros.size();
    }

    /**
     * Evita erro caso um teste manual envie um evento sem a lista Records.
     */
    private List<SNSEvent.SNSRecord> obterRegistros(
            SNSEvent evento
    ) {
        if (evento == null || evento.getRecords() == null) {
            return List.of();
        }

        return evento.getRecords();
    }
}
