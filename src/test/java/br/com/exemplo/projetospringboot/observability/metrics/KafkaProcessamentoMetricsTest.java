package br.com.exemplo.projetospringboot.observability.metrics;

import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica o registro das durações e das tags
 * utilizadas pelos consumers Kafka.
 */
class KafkaProcessamentoMetricsTest {

    /**
     * Confirma que sucesso e falha são armazenados
     * em séries diferentes da mesma métrica.
     */
    @Test
    void deveRegistrarTempoPorConsumerEResultado() {
        /*
         * O MockClock permite controlar o tempo do teste
         * sem utilizar Thread.sleep.
         */
        MockClock clock =
                new MockClock();

        SimpleMeterRegistry meterRegistry =
                new SimpleMeterRegistry(
                        SimpleConfig.DEFAULT,
                        clock
                );

        KafkaProcessamentoMetrics metrics =
                new KafkaProcessamentoMetrics(
                        meterRegistry
                );

        /*
         * Simula um processamento de estoque
         * bem-sucedido que levou 250 milissegundos.
         */
        Timer.Sample sucesso =
                metrics.iniciarMedicao();

        clock.add(
                Duration.ofMillis(250)
        );

        metrics.finalizarMedicao(
                sucesso,
                "estoque",
                true
        );

        /*
         * Simula uma tentativa do estoque que
         * terminou com falha depois de 100 milissegundos.
         */
        Timer.Sample falha =
                metrics.iniciarMedicao();

        clock.add(
                Duration.ofMillis(100)
        );

        metrics.finalizarMedicao(
                falha,
                "estoque",
                false
        );

        /*
         * Recupera somente a série correspondente
         * ao processamento bem-sucedido.
         */
        Timer timerSucesso = meterRegistry
                .get(
                        "kafka.consumer.processamento.tempo"
                )
                .tag(
                        "consumer",
                        "estoque"
                )
                .tag(
                        "resultado",
                        "sucesso"
                )
                .timer();

        /*
         * Recupera separadamente a série
         * correspondente à falha.
         */
        Timer timerFalha = meterRegistry
                .get(
                        "kafka.consumer.processamento.tempo"
                )
                .tag(
                        "consumer",
                        "estoque"
                )
                .tag(
                        "resultado",
                        "falha"
                )
                .timer();

        assertEquals(
                1L,
                timerSucesso.count()
        );

        assertEquals(
                250.0,
                timerSucesso.totalTime(
                        TimeUnit.MILLISECONDS
                )
        );

        assertEquals(
                1L,
                timerFalha.count()
        );

        assertEquals(
                100.0,
                timerFalha.totalTime(
                        TimeUnit.MILLISECONDS
                )
        );
    }
}