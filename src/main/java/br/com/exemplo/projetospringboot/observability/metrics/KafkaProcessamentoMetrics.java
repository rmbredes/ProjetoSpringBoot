package br.com.exemplo.projetospringboot.observability.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Registra métricas relacionadas ao tempo
 * de processamento dos consumers Kafka.
 */
@Component
public class KafkaProcessamentoMetrics {

    /**
     * Valor utilizado na tag resultado quando
     * o consumer termina normalmente.
     */
    private static final String RESULTADO_SUCESSO =
            "sucesso";

    /**
     * Valor utilizado na tag resultado quando
     * o consumer termina com uma exceção.
     */
    private static final String RESULTADO_FALHA =
            "falha";

    /**
     * Registro central utilizado para criar
     * e localizar os Timers do Micrometer.
     */
    private final MeterRegistry meterRegistry;

    /**
     * Recebe o registro central das métricas.
     *
     * @param meterRegistry registro gerenciado pelo Spring
     */
    public KafkaProcessamentoMetrics(
            MeterRegistry meterRegistry
    ) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Inicia a medição usando o relógio
     * associado ao MeterRegistry.
     *
     * @return amostra que representa o início da medição
     */
    public Timer.Sample iniciarMedicao() {
        return Timer.start(
                meterRegistry
        );
    }

    /**
     * Finaliza a medição e registra o resultado
     * no Timer correspondente às tags informadas.
     *
     * @param sample amostra criada no início do processamento
     * @param consumer nome lógico do consumer
     * @param sucesso indica se o processamento terminou normalmente
     */
    public void finalizarMedicao(
            Timer.Sample sample,
            String consumer,
            boolean sucesso
    ) {
        /*
         * Converte o booleano em uma tag legível
         * nas consultas do Prometheus e Grafana.
         */
        String resultado = sucesso
                ? RESULTADO_SUCESSO
                : RESULTADO_FALHA;

        /*
         * O MeterRegistry reutiliza o mesmo Timer quando
         * o nome e as tags já estiverem registrados.
         *
         * Portanto, não é criado um Timer novo para cada mensagem.
         */
        Timer timer = Timer
                .builder(
                        "kafka.consumer.processamento.tempo"
                )
                .description(
                        "Tempo de processamento dos consumers Kafka"
                )
                .tag(
                        "consumer",
                        consumer
                )
                .tag(
                        "resultado",
                        resultado
                )
                .register(
                        meterRegistry
                );

        /*
         * stop calcula o intervalo desde iniciarMedicao
         * e registra a duração no Timer selecionado.
         */
        sample.stop(
                timer
        );
    }
}