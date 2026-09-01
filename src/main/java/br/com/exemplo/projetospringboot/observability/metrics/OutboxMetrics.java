package br.com.exemplo.projetospringboot.observability.metrics;

import br.com.exemplo.projetospringboot.enums.StatusEventoOutbox;
import br.com.exemplo.projetospringboot.repository.EventoOutboxRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Registra métricas relacionadas ao funcionamento da Outbox.
 */
@Component
public class OutboxMetrics {

    /*
     * Conta cada tentativa de publicação que terminou com falha.
     */
    private final Counter falhasPublicacao;

    /*
     * Armazena o Timer que mede quanto tempo um evento permanece
     * na Outbox antes de sua publicação ser confirmada pelo Kafka.
     */
    private final Timer tempoAtePublicacao;

    /**
     * Registra as métricas quando o componente é criado pelo Spring.
     *
     * O Gauge executa a consulta somente quando seu valor é lido
     * pelo Actuator ou, futuramente, pelo Prometheus.
     *
     * @param meterRegistry registro central de métricas
     * @param repository acesso aos eventos da Outbox
     */
    public OutboxMetrics(
            MeterRegistry meterRegistry,
            EventoOutboxRepository repository
    ) {

        Gauge
                .builder(
                        "outbox.eventos.pendentes",
                        repository,
                        /*
                         * Consulta o valor atual sempre que
                         * a métrica for observada.
                         */
                        repositorio ->
                                repositorio.countByStatus(
                                        StatusEventoOutbox.PENDENTE
                                )
                )
                .description(
                        "Quantidade atual de eventos pendentes na Outbox"
                )
                .baseUnit(
                        "eventos"
                )
                .register(
                        meterRegistry
                );

        /*
         * Registra o contador uma única vez durante
         * a criação do componente pelo Spring.
         */
        this.falhasPublicacao =
                Counter
                        .builder(
                                "outbox.publicacoes.falhas"
                        )
                        .description(
                                "Quantidade de tentativas de publicação da Outbox que falharam"
                        )
                        .baseUnit(
                                "tentativas"
                        )
                        .register(
                                meterRegistry
                        );

        /*
         * Registra o Timer uma única vez no MeterRegistry.
         * O Actuator localizará essa métrica pelo nome abaixo.
         */
        this.tempoAtePublicacao =
                Timer
                        .builder(
                                "outbox.publicacao.tempo"
                        )
                        .description(
                                "Tempo entre a criação do evento na Outbox e sua publicação"
                        )
                        .register(
                                meterRegistry
                        );
    }

    /**
     * Incrementa o contador quando uma tentativa de publicação falha.
     */
    public void registrarFalhaPublicacao() {

        /*
         * Counters somente aumentam e começam automaticamente em zero.
         */
        falhasPublicacao.increment();
    }

    /**
     * Registra o tempo transcorrido entre a criação do evento na Outbox
     * e a confirmação de sua publicação no Kafka.
     *
     * @param duracao intervalo calculado entre criadoEm e publicadoEm
     */
    public void registrarTempoAtePublicacao(
            Duration duracao
    ) {
        /*
         * O Timer atualiza internamente a quantidade de medições,
         * o tempo total acumulado e o maior tempo observado.
         */
        tempoAtePublicacao.record(
                duracao
        );
    }
}
