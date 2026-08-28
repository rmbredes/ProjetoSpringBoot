package br.com.exemplo.projetospringboot.messaging.consumer;

import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.service.EstoqueService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
     * Recebe o serviço de estoque por injeção de dependência.
     *
     * @param estoqueService serviço responsável pelo processamento
     */
    public EstoquePedidoConsumer(
            EstoqueService estoqueService
    ) {
        this.estoqueService = estoqueService;
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
         * Recupera o evento de negócio contido na mensagem.
         */
        PedidoCriadoEvent evento = registro.value();

        /*
         * Simula uma falha persistente antes do processamento.
         *
         * Depois de três tentativas, esse registro será
         * encaminhado para a DLT.
         */
        if (evento.valor().compareTo(
                new BigDecimal("999.99")
        ) == 0) {
            LOGGER.warn(
                    "Falha persistente simulada: particao={}, offset={}, eventoId={}",
                    registro.partition(),
                    registro.offset(),
                    evento.eventoId()
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
         * Registra a conclusão da chamada do consumer,
         * incluindo se o efeito foi executado ou ignorado.
         */
        LOGGER.info(
                "Estoque concluído: topico={}, particao={}, offset={}, " +
                        "eventoId={}, pedidoId={}, processadoAgora={}",
                registro.topic(),
                registro.partition(),
                registro.offset(),
                evento.eventoId(),
                evento.pedidoId(),
                processadoAgora
        );
    }
}