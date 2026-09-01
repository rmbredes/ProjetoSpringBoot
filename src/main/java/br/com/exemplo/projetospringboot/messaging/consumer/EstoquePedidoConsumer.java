package br.com.exemplo.projetospringboot.messaging.consumer;

import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.observability.logging.EventoLogContext;
import br.com.exemplo.projetospringboot.observability.metrics.KafkaProcessamentoMetrics;
import br.com.exemplo.projetospringboot.service.EstoqueService;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Consumer responsável por receber eventos de pedidos
 * e encaminhá-los para o serviço de estoque.
 *
 * O consumer cuida da comunicação com Kafka.
 * A regra de idempotência fica no EstoqueService.
 */
@Component
public class EstoquePedidoConsumer {

    /*
     * Logger utilizado para registrar os metadados
     * das mensagens recebidas do Kafka.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    EstoquePedidoConsumer.class
            );

    /*
     * Serviço responsável pela regra de processamento
     * e pelo controle persistente de idempotência.
     */
    private final EstoqueService estoqueService;

    /**
     * Registra o tempo e o resultado
     * de cada tentativa de processamento.
     */
    private final KafkaProcessamentoMetrics kafkaProcessamentoMetrics;

    /**
     * Recebe as dependências utilizadas pelo consumer.
     *
     * @param estoqueService serviço responsável pelo estoque
     * @param kafkaProcessamentoMetrics métricas de processamento
     */
    public EstoquePedidoConsumer(
            EstoqueService estoqueService,
            KafkaProcessamentoMetrics kafkaProcessamentoMetrics
    ) {
        this.estoqueService = estoqueService;
        this.kafkaProcessamentoMetrics =
                kafkaProcessamentoMetrics;
    }

    /**
     * Recebe registros do tópico pedidos-criados.
     *
     * O valor 999.99 continua simulando uma falha persistente.
     * O valor 888.88 simula uma falha após o commit no banco,
     * mas antes da confirmação do offset Kafka.
     *
     * @param registro registro completo recebido do Kafka
     */
    @KafkaListener(
            topics = "pedidos-criados",
            groupId = "estoque-service-group"
    )
    public void processar(
            ConsumerRecord<String, PedidoCriadoEvent> registro
    ) {
        /*
         * Guarda o instante em que esta tentativa começou.
         */
        Timer.Sample amostra =
                kafkaProcessamentoMetrics.iniciarMedicao();

        /*
         * Começa como false porque uma exceção pode interromper
         * o método antes de ele chegar ao final.
         */
        boolean sucesso = false;

        try {
            processarRegistro(
                    registro
            );

            /*
             * Somente consideramos sucesso quando todo o processamento
             * chegou ao final sem lançar uma exceção.
             */
            sucesso = true;
        } finally {
            /*
             * O finally executa no sucesso e em qualquer falha.
             *
             * Assim também medimos as tentativas que serão
             * repetidas pelo tratamento de erros do Kafka.
             */
            kafkaProcessamentoMetrics.finalizarMedicao(
                    amostra,
                    "estoque",
                    sucesso
            );
        }
    }

    /**
     * Executa a regra atual do consumer dentro do contexto de log.
     *
     * Separar essa operação deixa o método público responsável
     * somente pelo ciclo de medição da tentativa.
     *
     * @param registro mensagem recebida do Kafka
     */
    private void processarRegistro(
            ConsumerRecord<String, PedidoCriadoEvent> registro
    ) {
        /*
         * Recupera o evento de negócio contido na mensagem.
         */
        PedidoCriadoEvent evento = registro.value();

        /*
         * O consumer é executado por uma thread administrada pelo Kafka.
         *
         * Como o MDC da thread produtora não atravessa o Kafka,
         * reconstruímos o contexto usando o eventoId recebido no payload.
         */
        try (
                MDC.MDCCloseable contextoEvento =
                        EventoLogContext.abrir(
                                evento.eventoId()
                        )
        ) {
            /*
             * Simula uma falha persistente antes do processamento.
             *
             * Depois de três tentativas, esse registro será
             * encaminhado para a DLT.
             */
            if (evento.valor().compareTo(
                    new BigDecimal("999.99")
            ) == 0) {
                /*
                 * O eventoId não precisa ser informado como argumento.
                 * O padrão de logging o recuperará automaticamente do MDC.
                 */
                LOGGER.warn(
                        "Falha persistente simulada: particao={}, offset={}",
                        registro.partition(),
                        registro.offset()
                );

                throw new IllegalStateException(
                        "Falha simulada no processamento do estoque"
                );
            }

            /*
             * Solicita o processamento idempotente ao serviço.
             *
             * true significa que o efeito foi executado agora.
             * false significa que a repetição foi ignorada.
             */
            boolean processadoAgora =
                    estoqueService.processar(evento);

            /*
             * Simula a janela crítica:
             *
             * 1. a transação de estoque já foi confirmada;
             * 2. o offset Kafka ainda não foi confirmado;
             * 3. ocorre uma falha.
             *
             * A condição processadoAgora garante que a falha
             * seja lançada somente na primeira entrega.
             */
            if (processadoAgora
                    && evento.valor().compareTo(
                    new BigDecimal("888.88")
            ) == 0) {
                LOGGER.warn(
                        "Falha simulada após commit no banco: particao={}, offset={}",
                        registro.partition(),
                        registro.offset()
                );

                throw new IllegalStateException(
                        "Falha simulada após commit do estoque"
                );
            }

            /*
             * O eventoId aparecerá automaticamente no início da linha.
             * Mantemos no corpo apenas os dados específicos da operação.
             */
            LOGGER.info(
                    "Estoque concluído: topico={}, particao={}, offset={}, " +
                            "pedidoId={}, processadoAgora={}",
                    registro.topic(),
                    registro.partition(),
                    registro.offset(),
                    evento.pedidoId(),
                    processadoAgora
            );
        }
    }
}
