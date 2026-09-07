package br.com.exemplo.projetospringboot.messaging.publisher;

import br.com.exemplo.projetospringboot.config.aws.AwsSnsProperties;
import br.com.exemplo.projetospringboot.dto.RelatorioPedidoDisponivelEvento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa o publicador SNS sem utilizar internet nem gerar uso na AWS.
 */
@ExtendWith(MockitoExtension.class)
class RelatorioPedidoSnsPublisherTest {

    /** Cliente da AWS substituído por uma simulação do Mockito. */
    @Mock
    private SnsClient snsClient;

    /** Conversor JSON também simulado para manter o teste bem isolado. */
    @Mock
    private ObjectMapper objectMapper;

    private RelatorioPedidoSnsPublisher publisher;

    private static final String TOPIC_ARN =
            "arn:aws:sns:sa-east-1:123456789012:topico-teste";

    /** Prepara o publicador antes de cada teste. */
    @BeforeEach
    void configurarPublisher() {
        AwsSnsProperties properties = new AwsSnsProperties(
                true,
                "sa-east-1",
                TOPIC_ARN,
                "projeto-s3"
        );

        publisher = new RelatorioPedidoSnsPublisher(
                snsClient,
                properties,
                objectMapper
        );
    }

    /**
     * Confirma tópico, assunto e corpo enviados ao cliente do SNS.
     *
     * @throws JacksonException caso a simulação do JSON falhe
     */
    @Test
    void devePublicarEventoDeRelatorioDisponivel()
            throws JacksonException {

        RelatorioPedidoDisponivelEvento evento =
                new RelatorioPedidoDisponivelEvento(
                        UUID.randomUUID(),
                        "RELATORIO_PEDIDO_DISPONIVEL",
                        9L,
                        803L,
                        UUID.randomUUID(),
                        "pedidos/803/relatorios/arquivo.pdf",
                        Instant.parse("2026-09-04T15:00:00Z")
                );

        String corpoJson = "{\"tipo\":\"RELATORIO_PEDIDO_DISPONIVEL\"}";

        when(objectMapper.writeValueAsString(evento))
                .thenReturn(corpoJson);

        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(
                        PublishResponse.builder()
                                .messageId("sns-message-123")
                                .build()
                );

        publisher.publicar(evento);

        ArgumentCaptor<PublishRequest> requestCaptor =
                ArgumentCaptor.forClass(PublishRequest.class);

        verify(snsClient).publish(requestCaptor.capture());

        PublishRequest request = requestCaptor.getValue();

        assertEquals(TOPIC_ARN, request.topicArn());
        assertEquals(corpoJson, request.message());
        assertEquals(
                "Relatorio do pedido 803 disponivel",
                request.subject()
        );
    }
}
