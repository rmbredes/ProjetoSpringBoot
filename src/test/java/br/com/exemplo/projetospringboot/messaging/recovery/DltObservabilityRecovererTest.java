package br.com.exemplo.projetospringboot.messaging.recovery;

import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.observability.logging.EventoLogContext;
import br.com.exemplo.projetospringboot.observability.metrics.DltMetrics;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Verifica a correlação e as métricas produzidas
 * durante o envio de mensagens para a DLT.
 */
@ExtendWith(MockitoExtension.class)
class DltObservabilityRecovererTest {

    /**
     * Simula o recoverer que realiza o envio ao Kafka.
     */
    @Mock
    private DeadLetterPublishingRecoverer delegate;

    /**
     * Simula o componente que registra a métrica da DLT.
     */
    @Mock
    private DltMetrics dltMetrics;

    /**
     * Simula o consumer fornecido pelo tratamento de erros.
     */
    @Mock
    private Consumer<Object, Object> consumer;

    /**
     * Cria o recoverer testado e injeta seus mocks.
     */
    @InjectMocks
    private DltObservabilityRecoverer recoverer;

    /**
     * Impede o vazamento de informações entre testes.
     */
    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    /**
     * Verifica se o envio é correlacionado e se a métrica
     * somente é incrementada depois do delegate.
     */
    @Test
    void deveRegistrarMetricaDepoisDoEnvioParaDlt() {
        UUID eventoId = UUID.randomUUID();

        PedidoCriadoEvent evento = new PedidoCriadoEvent(
                eventoId,
                30L,
                1L,
                new BigDecimal("999.99"),
                Instant.now()
        );

        ConsumerRecord<String, PedidoCriadoEvent> registro =
                new ConsumerRecord<>(
                        "pedidos-criados",
                        0,
                        50L,
                        "30",
                        evento
                );

        Exception falhaOriginal =
                new IllegalStateException(
                        "Falha no estoque"
                );

        /*
         * Verifica o MDC exatamente durante a chamada
         * que realiza o envio ao Kafka.
         */
        doAnswer(invocacao -> {
            assertEquals(
                    eventoId.toString(),
                    MDC.get(
                            EventoLogContext.CHAVE_EVENTO_ID
                    )
            );

            return null;
        }).when(delegate).accept(
                registro,
                consumer,
                falhaOriginal
        );

        recoverer.accept(
                registro,
                consumer,
                falhaOriginal
        );

        /*
         * Confirma a ordem: primeiro ocorre o envio;
         * somente depois a métrica é incrementada.
         */
        InOrder ordem = inOrder(
                delegate,
                dltMetrics
        );

        ordem.verify(delegate).accept(
                registro,
                consumer,
                falhaOriginal
        );

        ordem.verify(dltMetrics)
                .registrarMensagemEnviada();

        /*
         * Ao terminar, a thread deve estar limpa.
         */
        assertNull(
                MDC.get(
                        EventoLogContext.CHAVE_EVENTO_ID
                )
        );
    }

    /**
     * Confirma que uma falha no envio não é contabilizada
     * como mensagem entregue à DLT.
     */
    @Test
    void naoDeveRegistrarMetricaQuandoEnvioParaDltFalhar() {
        UUID eventoId = UUID.randomUUID();

        PedidoCriadoEvent evento = new PedidoCriadoEvent(
                eventoId,
                31L,
                1L,
                new BigDecimal("999.99"),
                Instant.now()
        );

        ConsumerRecord<String, PedidoCriadoEvent> registro =
                new ConsumerRecord<>(
                        "pedidos-criados",
                        0,
                        51L,
                        "31",
                        evento
                );

        Exception falhaOriginal =
                new IllegalStateException(
                        "Falha no estoque"
                );

        /*
         * Simula uma falha do próprio envio para a DLT.
         */
        doThrow(
                new KafkaException(
                        "Kafka não confirmou a DLT"
                )
        ).when(delegate).accept(
                registro,
                consumer,
                falhaOriginal
        );

        assertThrows(
                KafkaException.class,
                () -> recoverer.accept(
                        registro,
                        consumer,
                        falhaOriginal
                )
        );

        /*
         * Uma entrega malsucedida não pode aumentar o contador.
         */
        verify(
                dltMetrics,
                never()
        ).registrarMensagemEnviada();

        /*
         * O contexto também precisa ser limpo no caminho de erro.
         */
        assertNull(
                MDC.get(
                        EventoLogContext.CHAVE_EVENTO_ID
                )
        );
    }
}
