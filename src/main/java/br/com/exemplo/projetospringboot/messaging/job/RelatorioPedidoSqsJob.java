package br.com.exemplo.projetospringboot.messaging.job;

import br.com.exemplo.projetospringboot.messaging.consumer.RelatorioPedidoSqsConsumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job exclusivo que desperta o consumer de relatórios do SQS.
 *
 * <p>Ele não recebe mensagens nem gera arquivos. Sua única tarefa é
 * determinar quando o consumer deverá consultar a fila.</p>
 */
@Component
@ConditionalOnProperty(
        prefix = "application.aws.sqs",
        name = {"enabled", "consumer-enabled"},
        havingValue = "true"
)
public class RelatorioPedidoSqsJob {

    /** Consumer que conhece as operações da fila. */
    private final RelatorioPedidoSqsConsumer consumer;

    /** @param consumer componente que será despertado pelo job */
    public RelatorioPedidoSqsJob(
            RelatorioPedidoSqsConsumer consumer
    ) {
        this.consumer = consumer;
    }

    /**
     * Consulta a fila depois do intervalo configurado.
     *
     * <p>fixedDelay começa a contar somente após a consulta anterior
     * terminar. O scheduler dedicado evita bloquear a Outbox do Kafka.</p>
     */
    @Scheduled(
            fixedDelayString = "${application.aws.sqs.polling-delay-ms:5000}",
            scheduler = "sqsTaskScheduler"
    )
    public void consultarFila() {
        consumer.receberEProcessar();
    }
}
