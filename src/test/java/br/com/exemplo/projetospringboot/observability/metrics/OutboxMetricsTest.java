package br.com.exemplo.projetospringboot.observability.metrics;

import br.com.exemplo.projetospringboot.enums.StatusEventoOutbox;
import br.com.exemplo.projetospringboot.repository.EventoOutboxRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Verifica o registro e a leitura da métrica da Outbox
 * sem iniciar a aplicação ou acessar um banco real.
 */
@ExtendWith(MockitoExtension.class)
class OutboxMetricsTest {

    /*
     * Simula o repositório para controlar a quantidade
     * devolvida durante o teste.
     */
    @Mock
    private EventoOutboxRepository repository;

    @Test
    void deveExporQuantidadeDeEventosPendentes() {

        /*
         * Registro simples mantido somente na memória do teste.
         */
        SimpleMeterRegistry meterRegistry =
                new SimpleMeterRegistry();

        /*
         * Registra o Gauge utilizando o repositório simulado.
         */
        new OutboxMetrics(
                meterRegistry,
                repository
        );

        /*
         * Define o valor que será devolvido quando
         * o Gauge consultar o repositório.
         */
        when(
                repository.countByStatus(
                        StatusEventoOutbox.PENDENTE
                )
        )
                .thenReturn(
                        4L
                );

        /*
         * Localiza a métrica pelo nome registrado.
         */
        Gauge gauge =
                meterRegistry
                        .find(
                                "outbox.eventos.pendentes"
                        )
                        .gauge();

        /*
         * Confirma que a métrica existe e reflete
         * o valor atual devolvido pelo repositório.
         */
        assertNotNull(
                gauge
        );

        assertEquals(
                4.0,
                gauge.value()
        );

        /*
         * Libera os recursos do registro criado pelo teste.
         */
        meterRegistry.close();
    }

    @Test
    void deveContabilizarFalhaDePublicacao() {

        /*
         * Cria um registro de métricas isolado e mantido
         * somente na memória durante este teste.
         */
        SimpleMeterRegistry meterRegistry =
                new SimpleMeterRegistry();

        /*
         * O construtor registra tanto o Gauge da fila pendente
         * quanto o Counter das falhas de publicação.
         */
        OutboxMetrics outboxMetrics =
                new OutboxMetrics(
                        meterRegistry,
                        repository
                );

        /*
         * Simula a notificação de uma tentativa de publicação
         * que terminou com falha.
         */
        outboxMetrics.registrarFalhaPublicacao();

        /*
         * Localiza no MeterRegistry o Counter registrado
         * pelo componente de métricas da Outbox.
         */
        Counter counter =
                meterRegistry
                        .find(
                                "outbox.publicacoes.falhas"
                        )
                        .counter();

        /*
         * Confirma que a métrica foi registrada e que a
         * notificação incrementou seu valor de zero para um.
         */
        assertNotNull(
                counter
        );

        assertEquals(
                1.0,
                counter.count()
        );

        /*
         * Libera o registro utilizado somente por este teste.
         */
        meterRegistry.close();
    }

    @Test
    void deveRegistrarTempoAtePublicacao() {

        /*
         * Cria um registro de métricas isolado, mantido somente
         * na memória durante este teste.
         */
        SimpleMeterRegistry meterRegistry =
                new SimpleMeterRegistry();

        OutboxMetrics outboxMetrics =
                new OutboxMetrics(
                        meterRegistry,
                        repository
                );

        /*
         * Simula um evento que permaneceu 250 milissegundos
         * na Outbox antes de sua publicação ser confirmada.
         */
        outboxMetrics.registrarTempoAtePublicacao(
                Duration.ofMillis(250)
        );

        /*
         * Localiza o Timer pelo mesmo nome utilizado em seu registro.
         */
        Timer timer =
                meterRegistry
                        .find(
                                "outbox.publicacao.tempo"
                        )
                        .timer();

        /*
         * Confirma a existência da métrica, uma medição registrada
         * e o total correspondente aos 250 milissegundos informados.
         */
        assertNotNull(
                timer
        );

        assertEquals(
                1L,
                timer.count()
        );

        assertEquals(
                250.0,
                timer.totalTime(TimeUnit.MILLISECONDS)
        );

        meterRegistry.close();
    }
}
