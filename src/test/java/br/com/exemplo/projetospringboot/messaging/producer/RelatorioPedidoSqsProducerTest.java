package br.com.exemplo.projetospringboot.messaging.producer;

import br.com.exemplo.projetospringboot.config.aws.AwsSqsProperties;
import br.com.exemplo.projetospringboot.dto.GerarRelatorioPedidoMensagem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do producer do Amazon SQS.
 *
 * <p>O SqsClient é simulado. Portanto, este teste não envia mensagens
 * reais, não depende da internet e não gera uso na conta AWS.</p>
 */
@ExtendWith(MockitoExtension.class)
class RelatorioPedidoSqsProducerTest {

    /** Simula o cliente que conversa com o Amazon SQS. */
    @Mock
    private SqsClient sqsClient;

    /** Simula a transformação da mensagem Java em JSON. */
    @Mock
    private ObjectMapper objectMapper;

    /** Producer que será testado. */
    private RelatorioPedidoSqsProducer producer;

    /** URL fictícia suficiente para o teste unitário. */
    private static final String QUEUE_URL =
            "https://sqs.sa-east-1.amazonaws.com/123456789012/fila-teste";

    /** Prepara o producer antes de cada teste. */
    @BeforeEach
    void configurarProducer() {
        AwsSqsProperties properties = new AwsSqsProperties(
                true,
                "sa-east-1",
                QUEUE_URL,
                "projeto-s3",
                20,
                false,
                5000,
                "https://sqs.sa-east-1.amazonaws.com/123456789012/auditoria-teste",
                false,
                5000
        );

        producer = new RelatorioPedidoSqsProducer(
                sqsClient,
                properties,
                objectMapper
        );
    }

    /**
     * Confirma que o producer envia o JSON para a URL configurada
     * e devolve o MessageId recebido da AWS.
     *
     * @throws JacksonException caso a simulação do JSON falhe
     */
    @Test
    void deveEnviarSolicitacaoDeRelatorioParaOSqs()
            throws JacksonException {

        UUID solicitacaoId = UUID.randomUUID();
        Instant solicitadoEm = Instant.parse("2026-09-03T20:00:00Z");

        GerarRelatorioPedidoMensagem mensagem =
                new GerarRelatorioPedidoMensagem(
                        solicitacaoId,
                        15L,
                        3L,
                        solicitadoEm
                );

        String corpoJson = """
                {"solicitacaoId":"%s","relatorioId":15,"pedidoId":3}
                """.formatted(solicitacaoId).trim();

        when(
                objectMapper.writeValueAsString(mensagem)
        ).thenReturn(corpoJson);

        when(
                sqsClient.sendMessage(
                        org.mockito.ArgumentMatchers.any(
                                SendMessageRequest.class
                        )
                )
        ).thenReturn(
                SendMessageResponse.builder()
                        .messageId("mensagem-123")
                        .build()
        );

        String messageId = producer.enviar(mensagem);

        ArgumentCaptor<SendMessageRequest> requestCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);

        verify(sqsClient).sendMessage(requestCaptor.capture());

        SendMessageRequest requestEnviado = requestCaptor.getValue();

        assertEquals(QUEUE_URL, requestEnviado.queueUrl());
        assertEquals(corpoJson, requestEnviado.messageBody());
        assertEquals("mensagem-123", messageId);
    }
}
