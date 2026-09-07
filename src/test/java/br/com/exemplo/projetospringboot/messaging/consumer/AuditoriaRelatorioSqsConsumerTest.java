package br.com.exemplo.projetospringboot.messaging.consumer;

import br.com.exemplo.projetospringboot.config.aws.AwsSqsProperties;
import br.com.exemplo.projetospringboot.dto.RelatorioPedidoDisponivelEvento;
import br.com.exemplo.projetospringboot.service.AuditoriaRelatorioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testa o ciclo receber, auditar e excluir da fila de auditoria. */
@ExtendWith(MockitoExtension.class)
class AuditoriaRelatorioSqsConsumerTest {

    private static final String AUDIT_QUEUE_URL =
            "https://sqs.sa-east-1.amazonaws.com/123456789012/auditoria-teste";

    @Mock
    private SqsClient sqsClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AuditoriaRelatorioService auditoriaService;

    private AuditoriaRelatorioSqsConsumer consumer;

    /** Cria o consumer com endereços fictícios, sem acessar a AWS real. */
    @BeforeEach
    void configurarConsumer() {
        AwsSqsProperties properties = new AwsSqsProperties(
                true,
                "sa-east-1",
                "https://sqs.sa-east-1.amazonaws.com/123456789012/relatorios-teste",
                "projeto-s3",
                20,
                true,
                5000,
                AUDIT_QUEUE_URL,
                true,
                5000
        );

        consumer = new AuditoriaRelatorioSqsConsumer(
                sqsClient,
                properties,
                objectMapper,
                auditoriaService
        );
    }

    /** Confirma que a mensagem é excluída somente após ser registrada. */
    @Test
    void deveRegistrarEExcluirMensagemComSucesso() throws Exception {
        Message mensagemSqs = criarMensagemSqs();
        RelatorioPedidoDisponivelEvento evento = criarEvento();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(
                        ReceiveMessageResponse.builder()
                                .messages(mensagemSqs)
                                .build()
                );
        when(objectMapper.readValue(
                mensagemSqs.body(),
                RelatorioPedidoDisponivelEvento.class
        )).thenReturn(evento);

        consumer.receberEProcessar();

        verify(auditoriaService).registrar(evento);

        ArgumentCaptor<DeleteMessageRequest> captor =
                ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqsClient).deleteMessage(captor.capture());

        assertEquals(AUDIT_QUEUE_URL, captor.getValue().queueUrl());
        assertEquals(
                "receipt-auditoria-123",
                captor.getValue().receiptHandle()
        );
    }

    /**
     * Confirma a segurança do fluxo: se o banco falhar, a mensagem
     * não é apagada e poderá voltar para uma tentativa futura.
     */
    @Test
    void naoDeveExcluirMensagemQuandoRegistroFalhar() throws Exception {
        Message mensagemSqs = criarMensagemSqs();
        RelatorioPedidoDisponivelEvento evento = criarEvento();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(
                        ReceiveMessageResponse.builder()
                                .messages(mensagemSqs)
                                .build()
                );
        when(objectMapper.readValue(
                mensagemSqs.body(),
                RelatorioPedidoDisponivelEvento.class
        )).thenReturn(evento);
        doThrow(new RuntimeException("Banco indisponível"))
                .when(auditoriaService)
                .registrar(evento);

        consumer.receberEProcessar();

        verify(sqsClient, never()).deleteMessage(
                any(DeleteMessageRequest.class)
        );
    }

    private Message criarMensagemSqs() {
        return Message.builder()
                .messageId("message-auditoria-123")
                .receiptHandle("receipt-auditoria-123")
                .body("{json-simulado}")
                .build();
    }

    private RelatorioPedidoDisponivelEvento criarEvento() {
        return new RelatorioPedidoDisponivelEvento(
                UUID.randomUUID(),
                "RELATORIO_PEDIDO_DISPONIVEL",
                14L,
                808L,
                UUID.randomUUID(),
                "pedidos/808/relatorios/arquivo.pdf",
                Instant.parse("2026-09-04T19:21:30Z")
        );
    }
}
