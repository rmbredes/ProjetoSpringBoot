package br.com.exemplo.projetospringboot.messaging.job;

import br.com.exemplo.projetospringboot.messaging.consumer.AuditoriaRelatorioSqsConsumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Desperta periodicamente o consumer da fila SQS de auditoria. */
@Component
@ConditionalOnProperty(
        prefix = "application.aws.sqs",
        name = {"enabled", "audit-consumer-enabled"},
        havingValue = "true"
)
public class AuditoriaRelatorioSqsJob {

    private final AuditoriaRelatorioSqsConsumer consumer;

    public AuditoriaRelatorioSqsJob(
            AuditoriaRelatorioSqsConsumer consumer
    ) {
        this.consumer = consumer;
    }

    /**
     * Inicia nova consulta depois que a anterior terminar e o intervalo
     * configurado transcorrer.
     */
    @Scheduled(
            fixedDelayString = "${application.aws.sqs.audit-polling-delay-ms:5000}",
            scheduler = "sqsTaskScheduler"
    )
    public void consultarFila() {
        consumer.receberEProcessar();
    }
}
