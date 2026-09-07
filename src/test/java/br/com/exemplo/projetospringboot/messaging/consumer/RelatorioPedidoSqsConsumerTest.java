package br.com.exemplo.projetospringboot.messaging.consumer;

import br.com.exemplo.projetospringboot.config.aws.AwsSqsProperties;
import br.com.exemplo.projetospringboot.dto.GerarRelatorioPedidoMensagem;
import br.com.exemplo.projetospringboot.service.ProcessadorRelatorioPedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testa o ciclo receber, processar e excluir realizado pelo consumer. */
@ExtendWith(MockitoExtension.class)
class RelatorioPedidoSqsConsumerTest {

    private static final String QUEUE_URL =
            "https://sqs.sa-east-1.amazonaws.com/123456789012/fila-teste";

    @Mock
    private SqsClient sqsClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProcessadorRelatorioPedidoService processador;

    private RelatorioPedidoSqsConsumer consumer;

    /** Cria o consumer com configurações fictícias. */
    @BeforeEach
    void configurarConsumer() {
        AwsSqsProperties properties = new AwsSqsProperties(
                true,
                "sa-east-1",
                QUEUE_URL,
                "projeto-s3",
                20,
                true,
                5000,
                "https://sqs.sa-east-1.amazonaws.com/123456789012/auditoria-teste",
                false,
                5000
        );

        consumer = new RelatorioPedidoSqsConsumer(
                sqsClient,
                properties,
                objectMapper,
                processador
        );
    }

    /**
     * Confirma que a mensagem só é excluída depois do processador retornar.
     *
     * @throws JacksonException caso o mock de JSON falhe
     */
    @Test
    void deveProcessarEExcluirMensagemComSucesso()
            throws JacksonException {
        UUID solicitacaoId = UUID.randomUUID();

        GerarRelatorioPedidoMensagem mensagemConvertida =
                new GerarRelatorioPedidoMensagem(
                        solicitacaoId,
                        1L,
                        799L,
                        Instant.now()
                );

        Message message = Message.builder()
                .messageId("message-123")
                .receiptHandle("receipt-handle-123")
                .body("{json-simulado}")
                .attributes(
                        Map.of(
                                MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT,
                                "1"
                        )
                )
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(
                        ReceiveMessageResponse.builder()
                                .messages(message)
                                .build()
                );

        when(objectMapper.readValue(
                message.body(),
                GerarRelatorioPedidoMensagem.class
        )).thenReturn(mensagemConvertida);

        consumer.receberEProcessar();

        verify(processador).processar(mensagemConvertida);

        ArgumentCaptor<DeleteMessageRequest> deleteCaptor =
                ArgumentCaptor.forClass(DeleteMessageRequest.class);

        verify(sqsClient).deleteMessage(deleteCaptor.capture());

        assertEquals(
                QUEUE_URL,
                deleteCaptor.getValue().queueUrl()
        );
        assertEquals(
                "receipt-handle-123",
                deleteCaptor.getValue().receiptHandle()
        );
    }
}
