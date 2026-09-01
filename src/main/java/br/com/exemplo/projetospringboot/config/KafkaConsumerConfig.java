package br.com.exemplo.projetospringboot.config;

import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.messaging.recovery.DltObservabilityRecoverer;
import br.com.exemplo.projetospringboot.observability.metrics.DltMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Configura o tratamento de falhas dos consumers Kafka.
 *
 * Esta configuração define:
 *
 * 1. quantas vezes um registro será processado;
 * 2. quem realizará o envio para a DLT;
 * 3. como esse envio será observado.
 */
@Configuration
public class KafkaConsumerConfig {

    /**
     * Tempo de espera, em milissegundos,
     * entre as tentativas de processamento.
     */
    private static final long INTERVALO_ENTRE_TENTATIVAS =
            1_000L;

    /**
     * Quantidade de novas tentativas depois da primeira falha.
     *
     * Duas novas tentativas mais a tentativa original
     * resultam em três tentativas no total.
     */
    private static final long QUANTIDADE_DE_RETRIES =
            2L;

    /**
     * Cria o componente do Spring Kafka responsável
     * pela publicação real das mensagens na DLT.
     *
     * O construtor utilizado aplica a convenção padrão:
     *
     * pedidos-criados -> pedidos-criados-dlt
     *
     * A partição original também é preservada.
     *
     * @param kafkaTemplate componente responsável pela publicação
     * @return recoverer responsável pelo envio real à DLT
     */
    @Bean
    public DeadLetterPublishingRecoverer
    deadLetterPublishingRecoverer(
            KafkaTemplate<String, PedidoCriadoEvent> kafkaTemplate
    ) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate
                );

        /*
         * Obriga o recoverer a esperar a confirmação do Kafka.
         *
         * Se o envio falhar ou atingir o timeout,
         * o método accept lançará uma exceção.
         *
         * Embora este seja o padrão da versão atual,
         * deixamos explícito porque nossa métrica depende disso.
         */
        recoverer.setFailIfSendResultIsError(
                true
        );

        return recoverer;
    }

    /**
     * Cria a camada que acrescenta MDC, log e métrica
     * ao recoverer fornecido pelo Spring Kafka.
     *
     * @param delegate componente responsável pelo envio real
     * @param dltMetrics componente responsável pelas métricas
     * @return recoverer com observabilidade
     */
    @Bean
    public DltObservabilityRecoverer
    dltObservabilityRecoverer(
            DeadLetterPublishingRecoverer delegate,
            DltMetrics dltMetrics
    ) {
        return new DltObservabilityRecoverer(
                delegate,
                dltMetrics
        );
    }

    /**
     * Cria o tratador de erros utilizado pelos métodos
     * anotados com @KafkaListener.
     *
     * Depois que os retries terminam, o handler chama
     * nosso DltObservabilityRecoverer.
     *
     * @param recoverer recuperador instrumentado da DLT
     * @return tratamento de erro com retry e DLT
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            DltObservabilityRecoverer recoverer
    ) {
        /*
         * Define uma espera fixa de um segundo
         * e duas novas tentativas.
         */
        FixedBackOff backOff =
                new FixedBackOff(
                        INTERVALO_ENTRE_TENTATIVAS,
                        QUANTIDADE_DE_RETRIES
                );

        /*
         * Quando o backOff for esgotado,
         * o handler entregará o registro ao recoverer.
         */
        return new DefaultErrorHandler(
                recoverer,
                backOff
        );
    }
}
