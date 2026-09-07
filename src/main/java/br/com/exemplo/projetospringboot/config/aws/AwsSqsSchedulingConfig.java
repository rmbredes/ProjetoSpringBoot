package br.com.exemplo.projetospringboot.config.aws;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Cria um agendador exclusivo para os consumers do Amazon SQS.
 *
 * <p>O long polling pode aguardar até vinte segundos por uma mensagem.
 * Uma thread exclusiva impede que essa espera bloqueie o job da Outbox,
 * que continua responsável pelo fluxo Kafka.</p>
 */
@Configuration
@ConditionalOnProperty(
        prefix = "application.aws.sqs",
        name = {"enabled", "consumer-enabled"},
        havingValue = "true"
)
public class AwsSqsSchedulingConfig {

    /**
     * Cria duas threads: uma para geração de relatórios e outra para
     * auditoria. Assim, um long polling não bloqueia a outra fila.
     *
     * @return agendador que será referenciado pelo nome no @Scheduled
     */
    @Bean(
            name = "sqsTaskScheduler",
            destroyMethod = "shutdown"
    )
    public TaskScheduler sqsTaskScheduler() {
        ThreadPoolTaskScheduler scheduler =
                new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("sqs-relatorios-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);

        return scheduler;
    }
}
