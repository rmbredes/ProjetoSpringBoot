package br.com.exemplo.projetospringboot.config;

import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Configura o tratamento de falhas dos consumers Kafka.
 *
 * Esta configuração define quantas vezes um registro será
 * processado e o que acontecerá após o fim das tentativas.
 */
@Configuration
public class KafkaConsumerConfig {

    /*
     * Nome do tópico que receberá registros que continuarem
     * falhando após todas as tentativas.
     */
    private static final String TOPICO_PEDIDOS_CRIADOS_DLT =
            "pedidos-criados-dlt";

    /*
     * Tempo de espera, em milissegundos, entre as tentativas.
     */
    private static final long INTERVALO_ENTRE_TENTATIVAS = 1_000L;

    /*
     * Quantidade de novas tentativas depois da primeira falha.
     *
     * Duas novas tentativas mais a tentativa original
     * resultam em três tentativas no total.
     */
    private static final long QUANTIDADE_DE_RETRIES = 2L;

    /**
     * Cria o tratador de erros utilizado pelos métodos
     * anotados com @KafkaListener.
     *
     * Quando um registro continua falhando, o recoverer
     * publica esse registro na DLT, mantendo a mesma partição.
     *
     * @param kafkaTemplate componente usado para publicar na DLT
     * @return tratador de erros configurado com retry e DLT
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, PedidoCriadoEvent> kafkaTemplate
    ) {
        /*
         * Define para qual tópico e partição o registro
         * será enviado depois do fim das tentativas.
         */
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (registro, exception) ->
                                new TopicPartition(
                                        TOPICO_PEDIDOS_CRIADOS_DLT,
                                        registro.partition()
                                )
                );

        /*
         * Define espera fixa de um segundo e duas novas
         * tentativas após a tentativa original.
         */
        FixedBackOff backOff = new FixedBackOff(
                INTERVALO_ENTRE_TENTATIVAS,
                QUANTIDADE_DE_RETRIES
        );

        /*
         * Combina a política de tentativas com o envio
         * do registro problemático para a DLT.
         */
        return new DefaultErrorHandler(
                recoverer,
                backOff
        );
    }
}