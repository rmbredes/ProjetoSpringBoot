package br.com.exemplo.projetospringboot.observability.health;

import org.apache.kafka.clients.admin.TopicDescription;

import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import org.springframework.kafka.core.KafkaAdminOperations;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Verifica se a aplicação consegue consultar o tópico principal
 * utilizado para publicar eventos de pedidos.
 *
 * Esse indicador valida duas condições:
 *
 * 1. a conexão administrativa com o Kafka está funcionando;
 * 2. o tópico pedidos-criados existe e pode ser consultado.
 */
@Component
@ConditionalOnEnabledHealthIndicator("kafka")
public class KafkaHealthIndicator
        implements HealthIndicator {

    /*
     * Tópico principal utilizado pela Outbox e pelos consumers.
     *
     * Posteriormente poderemos mover esse nome para uma propriedade
     * compartilhada, evitando repetições em diferentes classes.
     */
    private static final String TOPICO_PEDIDOS_CRIADOS =
            "pedidos-criados";

    /*
     * Interface administrativa fornecida pelo Spring Kafka.
     *
     * Utilizamos a interface em vez da implementação concreta
     * para reduzir o acoplamento e facilitar os testes unitários.
     */
    private final KafkaAdminOperations kafkaAdmin;

    public KafkaHealthIndicator(
            KafkaAdminOperations kafkaAdmin
    ) {
        this.kafkaAdmin =
                kafkaAdmin;
    }

    /**
     * Executa a verificação sempre que o Actuator consulta
     * a saúde deste componente.
     *
     * @return UP quando o tópico pode ser consultado;
     *         DOWN quando a consulta falha
     */
    @Override
    public Health health() {

        try {

            /*
             * Consulta o Kafka utilizando a API administrativa.
             *
             * Se o broker estiver indisponível ou o tópico não existir,
             * o Spring Kafka lançará uma exceção.
             */
            Map<String, TopicDescription> topicos =
                    kafkaAdmin.describeTopics(
                            TOPICO_PEDIDOS_CRIADOS
                    );

            TopicDescription descricao =
                    topicos.get(
                            TOPICO_PEDIDOS_CRIADOS
                    );

            /*
             * Esta validação protege o indicador caso a consulta
             * retorne um mapa sem a descrição solicitada.
             */
            if (descricao == null) {

                return Health
                        .down()
                        .withDetail(
                                "topico",
                                TOPICO_PEDIDOS_CRIADOS
                        )
                        .withDetail(
                                "motivo",
                                "Tópico não encontrado"
                        )
                        .build();
            }

            /*
             * Além do estado UP, fornecemos detalhes úteis
             * para usuários autorizados.
             */
            return Health
                    .up()
                    .withDetail(
                            "topico",
                            TOPICO_PEDIDOS_CRIADOS
                    )
                    .withDetail(
                            "particoes",
                            descricao
                                    .partitions()
                                    .size()
                    )
                    .build();

        } catch (Exception exception) {

            /*
             * Health.down(exception) registra a causa como detalhe
             * técnico, sem derrubar a aplicação.
             *
             * A falha será convertida pelo Actuator em estado DOWN.
             */
            return Health
                    .down(exception)
                    .withDetail(
                            "topico",
                            TOPICO_PEDIDOS_CRIADOS
                    )
                    .build();
        }
    }
}