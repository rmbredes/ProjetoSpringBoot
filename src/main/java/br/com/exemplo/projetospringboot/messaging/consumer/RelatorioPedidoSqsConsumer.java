package br.com.exemplo.projetospringboot.messaging.consumer;

import br.com.exemplo.projetospringboot.config.aws.AwsSqsProperties;
import br.com.exemplo.projetospringboot.dto.GerarRelatorioPedidoMensagem;
import br.com.exemplo.projetospringboot.service.ProcessadorRelatorioPedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Consumer responsável pelo ciclo de recebimento de mensagens do SQS.
 *
 * <p>Ele recebe uma mensagem, converte o JSON, chama o processador e
 * exclui a mensagem somente depois da conclusão bem-sucedida.</p>
 */
@Component
@ConditionalOnProperty(
        prefix = "application.aws.sqs",
        name = {"enabled", "consumer-enabled"},
        havingValue = "true"
)
public class RelatorioPedidoSqsConsumer {

    /** Quantidade configurada na fila antes do envio para a DLQ. */
    private static final int MAXIMO_RECEBIMENTOS = 3;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RelatorioPedidoSqsConsumer.class);

    private final SqsClient sqsClient;
    private final AwsSqsProperties properties;
    private final ObjectMapper objectMapper;
    private final ProcessadorRelatorioPedidoService processador;

    /**
     * @param sqsClient cliente utilizado para receber e excluir mensagens
     * @param properties configurações da fila
     * @param objectMapper conversor entre JSON e DTO
     * @param processador serviço que gera e armazena o relatório
     */
    public RelatorioPedidoSqsConsumer(
            SqsClient sqsClient,
            AwsSqsProperties properties,
            ObjectMapper objectMapper,
            ProcessadorRelatorioPedidoService processador
    ) {
        this.sqsClient = sqsClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.processador = processador;
    }

    /**
     * Realiza uma consulta à fila e processa no máximo uma mensagem.
     *
     * <p>Uma mensagem não excluída volta a ficar visível depois do
     * visibility timeout. Após três recebimentos, a configuração da AWS
     * a encaminhará para a DLQ.</p>
     */
    public void receberEProcessar() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .maxNumberOfMessages(1)
                .waitTimeSeconds(properties.waitTimeSeconds())
                .messageSystemAttributeNames(
                        MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT
                )
                .build();

        ReceiveMessageResponse response =
                sqsClient.receiveMessage(request);

        List<Message> mensagens = response.messages();

        if (mensagens.isEmpty()) {
            return;
        }

        processarMensagem(mensagens.getFirst());
    }

    /** Processa individualmente a mensagem recebida. */
    private void processarMensagem(Message message) {
        GerarRelatorioPedidoMensagem mensagemConvertida = null;
        int quantidadeRecebimentos =
                obterQuantidadeRecebimentos(message);

        try {
            mensagemConvertida = objectMapper.readValue(
                    message.body(),
                    GerarRelatorioPedidoMensagem.class
            );

            LOGGER.info(
                    "Mensagem SQS recebida: messageId={}, solicitacaoId={}, recebimento={}",
                    message.messageId(),
                    mensagemConvertida.solicitacaoId(),
                    quantidadeRecebimentos
            );

            processador.processar(mensagemConvertida);

            excluirMensagem(message.receiptHandle());
        } catch (Exception exception) {
            LOGGER.error(
                    "Falha ao processar mensagem SQS: messageId={}, recebimento={}",
                    message.messageId(),
                    quantidadeRecebimentos,
                    exception
            );

            /*
             * No último recebimento, preservamos também a falha no banco.
             * A mensagem continua sem exclusão e será movida para a DLQ
             * pela política configurada diretamente no Amazon SQS.
             */
            if (mensagemConvertida != null
                    && quantidadeRecebimentos >= MAXIMO_RECEBIMENTOS) {
                registrarFalhaDefinitiva(
                        mensagemConvertida,
                        exception
                );
            }
        }
    }

    /** Exclui a mensagem usando o receipt handle do recebimento atual. */
    private void excluirMensagem(String receiptHandle) {
        DeleteMessageRequest request = DeleteMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .receiptHandle(receiptHandle)
                .build();

        sqsClient.deleteMessage(request);
    }

    /** Lê o contador aproximado fornecido pelo próprio SQS. */
    private int obterQuantidadeRecebimentos(Message message) {
        String valor = message.attributes().get(
                MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT
        );

        if (valor == null) {
            return 1;
        }

        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    /** Tenta registrar a falha sem substituir a exceção original do log. */
    private void registrarFalhaDefinitiva(
            GerarRelatorioPedidoMensagem mensagem,
            Exception exception
    ) {
        try {
            processador.registrarFalhaDefinitiva(
                    mensagem.solicitacaoId(),
                    resumirMensagemErro(exception)
            );
        } catch (RuntimeException falhaAoRegistrar) {
            LOGGER.error(
                    "Não foi possível registrar a falha definitiva: solicitacaoId={}",
                    mensagem.solicitacaoId(),
                    falhaAoRegistrar
            );
        }
    }

    /** Limita a mensagem ao tamanho aceito pela coluna ultimo_erro. */
    private String resumirMensagemErro(Exception exception) {
        String mensagem = exception.getMessage();

        if (mensagem == null || mensagem.isBlank()) {
            mensagem = exception.getClass().getSimpleName();
        }

        return mensagem.substring(
                0,
                Math.min(mensagem.length(), 2000)
        );
    }
}
