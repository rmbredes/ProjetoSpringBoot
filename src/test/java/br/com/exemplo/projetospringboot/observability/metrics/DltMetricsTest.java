package br.com.exemplo.projetospringboot.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica o registro e o incremento
 * do contador de mensagens enviadas à DLT.
 */
class DltMetricsTest {

    /**
     * Confirma que o contador começa em zero
     * e aumenta a cada envio registrado.
     */
    @Test
    void deveContarMensagensEnviadasParaDlt() {
        /*
         * O SimpleMeterRegistry mantém as métricas em memória
         * e permite testar o Micrometer sem iniciar o Spring.
         */
        SimpleMeterRegistry meterRegistry =
                new SimpleMeterRegistry();

        DltMetrics dltMetrics =
                new DltMetrics(
                        meterRegistry
                );

        /*
         * Recupera exatamente o Counter que foi
         * registrado pelo construtor de DltMetrics.
         */
        Counter contador = meterRegistry
                .get(
                        "kafka.dlt.mensagens.enviadas"
                )
                .counter();

        /*
         * Nenhuma mensagem foi registrada até este momento.
         */
        assertEquals(
                0.0,
                contador.count()
        );

        dltMetrics.registrarMensagemEnviada();
        dltMetrics.registrarMensagemEnviada();

        /*
         * Cada chamada deve incrementar o contador uma vez.
         */
        assertEquals(
                2.0,
                contador.count()
        );
    }
}