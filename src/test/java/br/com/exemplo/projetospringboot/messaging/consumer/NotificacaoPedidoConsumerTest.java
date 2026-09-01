package br.com.exemplo.projetospringboot.messaging.consumer;

import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.observability.logging.EventoLogContext;
import br.com.exemplo.projetospringboot.observability.metrics.KafkaProcessamentoMetrics;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica a correlação dos logs produzidos
 * pelo consumer responsável pelas notificações.
 */
@ExtendWith(MockitoExtension.class)
class NotificacaoPedidoConsumerTest {

    /**
     * Simula o componente responsável pela métrica.
     */
    @Mock
    private KafkaProcessamentoMetrics kafkaProcessamentoMetrics;

    /**
     * Representa o início controlado da medição.
     */
    @Mock
    private Timer.Sample amostra;

    /**
     * Cria o consumer com a métrica simulada.
     */
    @InjectMocks
    private NotificacaoPedidoConsumer notificacaoPedidoConsumer;

    /**
     * Logger concreto utilizado para capturar os eventos de log
     * produzidos exclusivamente pela classe que está sendo testada.
     */
    private Logger logger;

    /**
     * Armazena temporariamente os eventos de log em memória
     * para que o teste possa inspecionar seu conteúdo e seu MDC.
     */
    private ListAppender<ILoggingEvent> capturadorDeLogs;

    /**
     * Instala um capturador temporário no logger do consumer
     * antes da execução de cada teste.
     */
    @BeforeEach
    void prepararCapturaDosLogs() {
        /*
         * Faz o consumer utilizar a amostra controlada
         * durante a execução deste teste.
         */
        when(
                kafkaProcessamentoMetrics.iniciarMedicao()
        ).thenReturn(
                amostra
        );

        logger = (Logger) LoggerFactory.getLogger(
                NotificacaoPedidoConsumer.class
        );

        capturadorDeLogs = new ListAppender<>();
        capturadorDeLogs.start();
        logger.addAppender(capturadorDeLogs);
    }

    /**
     * Remove o capturador e limpa o MDC para que o teste
     * não interfira em nenhuma execução posterior.
     */
    @AfterEach
    void limparContextoDoTeste() {
        logger.detachAppender(capturadorDeLogs);
        capturadorDeLogs.stop();
        MDC.clear();
    }

    /**
     * Verifica se o log da notificação recebeu o eventoId
     * e se o contexto foi removido depois do processamento.
     */
    @Test
    void deveCorrelacionarLogComEventoIdELimparMdc() {
        UUID eventoId = UUID.randomUUID();

        PedidoCriadoEvent evento = new PedidoCriadoEvent(
                eventoId,
                20L,
                2L,
                new BigDecimal("250.00"),
                Instant.now()
        );

        /*
         * Representa o registro que seria entregue pelo Kafka
         * ao grupo responsável pelas notificações.
         */
        ConsumerRecord<String, PedidoCriadoEvent> registro =
                new ConsumerRecord<>(
                        "pedidos-criados",
                        1,
                        30L,
                        "20",
                        evento
                );

        notificacaoPedidoConsumer.processar(registro);

        /*
         * A notificação concluída deve ser registrada como sucesso.
         */
        verify(
                kafkaProcessamentoMetrics
        ).finalizarMedicao(
                amostra,
                "notificacao",
                true
        );

        /*
         * Confirma que o consumer realmente produziu um evento de log
         * que poderá ser analisado pelo restante do teste.
         */
        assertFalse(
                capturadorDeLogs.list.isEmpty()
        );

        ILoggingEvent logDaNotificacao =
                capturadorDeLogs.list.getFirst();

        /*
         * O evento de log guarda uma cópia do MDC existente
         * exatamente no instante em que LOGGER.info foi executado.
         */
        assertEquals(
                eventoId.toString(),
                logDaNotificacao
                        .getMDCPropertyMap()
                        .get(EventoLogContext.CHAVE_EVENTO_ID)
        );

        /*
         * Depois do método terminar, o contexto da thread atual
         * deve estar novamente vazio e pronto para reutilização.
         */
        assertNull(
                MDC.get(
                        EventoLogContext.CHAVE_EVENTO_ID
                )
        );
    }
}
