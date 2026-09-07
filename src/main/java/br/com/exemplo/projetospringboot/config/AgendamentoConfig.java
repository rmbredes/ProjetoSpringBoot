package br.com.exemplo.projetospringboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Configuração responsável por habilitar a execução
 * de tarefas agendadas na aplicação.
 *
 * <p>A propriedade {@code app.scheduling.enabled} permite
 * desligar todos os agendamentos em ambientes nos quais eles
 * não devem executar, como na suíte comum de testes.</p>
 *
 * <p>O {@code matchIfMissing = true} mantém o comportamento
 * atual da aplicação: se a propriedade não for informada,
 * os agendamentos continuarão habilitados.</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
        prefix = "app.scheduling",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
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
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler =
                new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("jobs-aplicacao-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);

        return scheduler;
    }
}
