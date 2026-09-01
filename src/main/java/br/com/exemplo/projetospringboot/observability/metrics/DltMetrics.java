package br.com.exemplo.projetospringboot.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Registra métricas relacionadas às mensagens
 * encaminhadas para tópicos de Dead Letter.
 */
@Component
public class DltMetrics {

    /**
     * Conta quantas mensagens foram enviadas com sucesso
     * para a DLT depois do esgotamento dos retries.
     */
    private final Counter mensagensEnviadas;

    /**
     * Registra o contador no MeterRegistry do Micrometer.
     *
     * O registro acontece uma única vez quando o Spring
     * cria este componente durante a inicialização.
     *
     * @param meterRegistry registro central das métricas
     */
    public DltMetrics(
            MeterRegistry meterRegistry
    ) {
        this.mensagensEnviadas =
                Counter
                        .builder(
                                "kafka.dlt.mensagens.enviadas"
                        )
                        .description(
                                "Quantidade de mensagens enviadas com sucesso para a DLT"
                        )
                        .baseUnit(
                                "mensagens"
                        )
                        .register(
                                meterRegistry
                        );
    }

    /**
     * Incrementa o contador depois que o envio
     * da mensagem para a DLT for confirmado.
     */
    public void registrarMensagemEnviada() {
        /*
         * Counter é monotônico: começa em zero
         * e somente aumenta durante a execução.
         */
        mensagensEnviadas.increment();
    }
}