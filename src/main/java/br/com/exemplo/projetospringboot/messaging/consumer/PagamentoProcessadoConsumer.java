package br.com.exemplo.projetospringboot.messaging.consumer;

import br.com.exemplo.projetospringboot.event.PagamentoProcessadoEvent;
import br.com.exemplo.projetospringboot.service.ProcessamentoPagamentoService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consome os resultados produzidos pelo pagamento-service.
 */
@Component
public class PagamentoProcessadoConsumer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    PagamentoProcessadoConsumer.class
            );

    private final ProcessamentoPagamentoService processamentoPagamentoService;

    public PagamentoProcessadoConsumer(
            ProcessamentoPagamentoService processamentoPagamentoService
    ) {
        this.processamentoPagamentoService =
                processamentoPagamentoService;
    }

    @KafkaListener(
            topics = "${application.kafka.pagamentos-processados.topic}",
            groupId = "${application.kafka.pagamentos-processados.group-id}"
    )
    public void processar(
            ConsumerRecord<String, PagamentoProcessadoEvent> registro
    ) {
        PagamentoProcessadoEvent evento =
                registro.value();

        LOGGER.info(
                "Pagamento processado recebido: topico={}, particao={}, " +
                        "offset={}, eventoId={}, pagamentoId={}, pedidoId={}",
                registro.topic(),
                registro.partition(),
                registro.offset(),
                evento.eventoId(),
                evento.pagamentoId(),
                evento.pedidoId()
        );

        boolean processadoAgora =
                processamentoPagamentoService.processar(
                        evento
                );

        LOGGER.info(
                "Processamento do pagamento concluído: " +
                        "pedidoId={}, processadoAgora={}",
                evento.pedidoId(),
                processadoAgora
        );
    }
}