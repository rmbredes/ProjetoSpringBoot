package br.com.exemplo.projetospringboot.observability.health;

import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import org.springframework.kafka.core.KafkaAdminOperations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testa o health indicator sem iniciar a aplicação
 * e sem depender de um broker Kafka real.
 */
@ExtendWith(MockitoExtension.class)
class KafkaHealthIndicatorTest {

    /*
     * Substitui a comunicação administrativa real
     * por um objeto controlado pelo teste.
     */
    @Mock
    private KafkaAdminOperations kafkaAdmin;

    /*
     * Representa a descrição do tópico devolvida pelo Kafka.
     */
    @Mock
    private TopicDescription topicDescription;

    private KafkaHealthIndicator healthIndicator;

    @BeforeEach
    void prepararTeste() {

        /*
         * Cria o objeto testado utilizando a dependência simulada.
         */
        healthIndicator =
                new KafkaHealthIndicator(
                        kafkaAdmin
                );
    }

    @Test
    void deveRetornarUpQuandoTopicoPuderSerConsultado() {

        /*
         * Simula um tópico existente.
         *
         * Para este teste, uma lista vazia é suficiente porque
         * o objetivo principal é validar o estado UP.
         */
        when(
                topicDescription.partitions()
        )
                .thenReturn(
                        List.of()
                );

        /*
         * Simula a resposta da API administrativa do Kafka.
         */
        when(
                kafkaAdmin.describeTopics(
                        "pedidos-criados"
                )
        )
                .thenReturn(
                        Map.of(
                                "pedidos-criados",
                                topicDescription
                        )
                );

        /*
         * Executa diretamente o contrato do HealthIndicator.
         */
        Health health =
                healthIndicator.health();

        /*
         * Confirma o estado e os detalhes produzidos.
         */
        assertEquals(
                Status.UP,
                health.getStatus()
        );

        assertEquals(
                "pedidos-criados",
                health.getDetails()
                        .get("topico")
        );

        assertEquals(
                0,
                health.getDetails()
                        .get("particoes")
        );
    }

    @Test
    void deveRetornarDownQuandoKafkaEstiverIndisponivel() {

        /*
         * Simula a falha que aconteceria ao consultar
         * um broker Kafka indisponível.
         */
        when(
                kafkaAdmin.describeTopics(
                        "pedidos-criados"
                )
        )
                .thenThrow(
                        new KafkaException(
                                "Kafka indisponível"
                        )
                );

        /*
         * O indicador deve converter a exceção em DOWN,
         * sem propagá-la para o restante da aplicação.
         */
        Health health =
                healthIndicator.health();

        assertEquals(
                Status.DOWN,
                health.getStatus()
        );

        assertEquals(
                "pedidos-criados",
                health.getDetails()
                        .get("topico")
        );
    }
}