package br.com.exemplo.projetospringboot.messaging.consumer;

import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.observability.logging.EventoLogContext;
import br.com.exemplo.projetospringboot.observability.metrics.KafkaProcessamentoMetrics;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer responsável por reagir à criação de pedidos
 * para executar ações de notificação.
 *
 * Nesta primeira versão, ele apenas registra o evento no log.
 * Futuramente, essa responsabilidade poderá ser movida para
 * um microsserviço independente.
 */
@Component
public class NotificacaoPedidoConsumer {

    /*
     * Logger utilizado para mostrar no terminal
     * os eventos processados pelo consumer.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    NotificacaoPedidoConsumer.class
            );

    /**
     * Registra o tempo e o resultado
     * do processamento das notificações.
     */
    private final KafkaProcessamentoMetrics kafkaProcessamentoMetrics;

    /**
     * Recebe o componente responsável pelas métricas.
     *
     * @param kafkaProcessamentoMetrics métricas dos consumers
     */
    public NotificacaoPedidoConsumer(
            KafkaProcessamentoMetrics kafkaProcessamentoMetrics
    ) {
        this.kafkaProcessamentoMetrics =
                kafkaProcessamentoMetrics;
    }

    /**
     * Recebe o registro completo publicado no tópico pedidos-criados.
     *
     * ConsumerRecord fornece tanto o evento quanto os metadados
     * técnicos usados para localizar o registro no Kafka.
     *
     * @param registro mensagem completa recebida do Kafka
     */
    @KafkaListener(
            topics = "pedidos-criados",
            groupId = "notificacao-service-group"
    )
    public void processar(
            ConsumerRecord<String, PedidoCriadoEvent> registro
    ) {
        /*
         * Inicia a medição desta tentativa.
         */
        Timer.Sample amostra =
                kafkaProcessamentoMetrics.iniciarMedicao();

        /*
         * Permanece false se qualquer parte do processamento
         * terminar lançando uma exceção.
         */
        boolean sucesso = false;

        try {
            processarRegistro(
                    registro
            );

            /*
             * Só chegamos aqui quando o processamento
             * terminou sem lançar uma exceção.
             */
            sucesso = true;
        } finally {
            /*
             * Também registrará falha quando a implementação
             * real da notificação lançar uma exceção.
             */
            kafkaProcessamentoMetrics.finalizarMedicao(
                    amostra,
                    "notificacao",
                    sucesso
            );
        }
    }

    /**
     * Processa a notificação dentro do contexto correlacionado.
     *
     * @param registro mensagem recebida do Kafka
     */
    private void processarRegistro(
            ConsumerRecord<String, PedidoCriadoEvent> registro
    ) {

        /*
         * Recupera o evento de negócio armazenado no valor
         * da mensagem Kafka.
         */
        PedidoCriadoEvent evento = registro.value();

        /*
         * O Kafka executa este consumer em uma nova thread.
         *
         * Como o MDC do produtor não atravessa a fila,
         * reconstruímos o contexto com o eventoId do payload.
         */
        try (
                MDC.MDCCloseable contextoEvento =
                        EventoLogContext.abrir(
                                evento.eventoId()
                        )
        ) {
            /*
             * Simula a notificação que futuramente poderia
             * ser enviada por e-mail, SMS ou outro canal.
             *
             * O eventoId será acrescentado automaticamente
             * ao log por meio do MDC da thread atual.
             */
            LOGGER.info(
                    "Notificacao Processada: topico={}, particao={}, offset={}, " +
                            "chave={}, pedidoId={}, clienteId={}, valor={}",
                    registro.topic(),
                    registro.partition(),
                    registro.offset(),
                    registro.key(),
                    evento.pedidoId(),
                    evento.clienteId(),
                    evento.valor()
            );
        }
    }
}
