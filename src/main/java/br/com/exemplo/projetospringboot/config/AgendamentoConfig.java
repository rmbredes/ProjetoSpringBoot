package br.com.exemplo.projetospringboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuração responsável por habilitar a execução
 * de tarefas agendadas na aplicação.
 *
 * Depois desta configuração, o Spring reconhecerá métodos
 * anotados com @Scheduled.
 */
@Configuration
@EnableScheduling
public class AgendamentoConfig {

    /**
     * Cria o agendador padrão utilizado pelos jobs já existentes,
     * incluindo o job da Outbox responsável pelo fluxo Kafka.
     *
     * <p>Ele é separado do sqsTaskScheduler para que o long polling
     * do Amazon SQS não interrompa ou atrase os outros jobs.</p>
     *
     * @return agendador padrão da aplicação
     */
    @Bean(
            name = "taskScheduler",
            destroyMethod = "shutdown"
    )
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler =
                new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("jobs-aplicacao-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);

        return scheduler;
    }
}
