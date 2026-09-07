package br.com.exemplo.projetospringboot.messaging.consumer;

import br.com.exemplo.projetospringboot.config.aws.AwsSqsProperties;
import br.com.exemplo.projetospringboot.dto.RelatorioPedidoDisponivelEvento;
import br.com.exemplo.projetospringboot.service.AuditoriaRelatorioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Consome os eventos entregues pelo SNS à fila SQS de auditoria.
 *
 * <p>Como ativamos Raw message delivery na assinatura, o body contém
 * diretamente o JSON de {@link RelatorioPedidoDisponivelEvento}.</p>
 */
@Component
@ConditionalOnProperty(
        prefix = "application.aws.sqs",
        name = {"enabled", "audit-consumer-enabled"},
        havingValue = "true"
)
public class AuditoriaRelatorioSqsConsumer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuditoriaRelatorioSqsConsumer.class);

    private final SqsClient sqsClient;
    private final AwsSqsProperties properties;
    private final ObjectMapper objectMapper;
    private final AuditoriaRelatorioService auditoriaService;

    public AuditoriaRelatorioSqsConsumer(
            SqsClient sqsClient,
            AwsSqsProperties properties,
            ObjectMapper objectMapper,
            AuditoriaRelatorioService auditoriaService
    ) {
        this.sqsClient = sqsClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.auditoriaService = auditoriaService;
    }

    /** Consulta a fila e processa no máximo uma mensagem por chamada. */
    public void receberEProcessar() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(properties.auditQueueUrl())
                .maxNumberOfMessages(1)
                .waitTimeSeconds(properties.waitTimeSeconds())
                .build();

        ReceiveMessageResponse response =
                sqsClient.receiveMessage(request);

        List<Message> mensagens = response.messages();

        if (mensagens.isEmpty()) {
            return;
        }

        processarMensagem(mensagens.getFirst());
    }

    /**
     * Converte, registra e confirma uma mensagem individual.
     *
     * <p>Se qualquer etapa falhar, não executamos DeleteMessage.
     * Assim, a mensagem voltará a ficar visível para outra tentativa.</p>
     */
    private void processarMensagem(Message message) {
        try {
            RelatorioPedidoDisponivelEvento evento =
                    objectMapper.readValue(
                            message.body(),
                            RelatorioPedidoDisponivelEvento.class
                    );

            auditoriaService.registrar(evento);
            excluirMensagem(message.receiptHandle());

            LOGGER.info(
                    "Evento removido da fila de auditoria: messageId={}, eventoId={}",
                    message.messageId(),
                    evento.eventoId()
            );
        } catch (Exception exception) {
            LOGGER.error(
                    "Falha ao processar mensagem da fila de auditoria: messageId={}",
                    message.messageId(),
                    exception
            );
        }
    }

    /** Confirma ao SQS que a mensagem foi processada com sucesso. */
    private void excluirMensagem(String receiptHandle) {
        DeleteMessageRequest request = DeleteMessageRequest.builder()
                .queueUrl(properties.auditQueueUrl())
                .receiptHandle(receiptHandle)
                .build();

        sqsClient.deleteMessage(request);
    }
}
