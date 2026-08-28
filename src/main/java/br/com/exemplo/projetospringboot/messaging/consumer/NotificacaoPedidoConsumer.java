package br.com.exemplo.projetospringboot.messaging.consumer;

import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer responsável por reagir à criação de pedidos
 * para executar ações de notificação.
 *
 * Nesta primeira versão, ele apenas registra o evento no log.
 * Futuramente, essa responsabilidade poderá ser movida para
 * um microsserviço independente.
 */
@Component
public class NotificacaoPedidoConsumer {

    /*
     * Logger utilizado para mostrar no terminal
     * os eventos processados pelo consumer.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    NotificacaoPedidoConsumer.class
            );

    /**
     * Recebe o registro completo publicado no tópico pedidos-criados.
     *
     * ConsumerRecord fornece tanto o evento quanto os metadados
     * técnicos usados para localizar o registro no Kafka.
     *
     * @param registro mensagem completa recebida do Kafka
     */
    @KafkaListener(
            topics = "pedidos-criados",
            groupId = "notificacao-service-group"
    )
    public void processar(
            ConsumerRecord<String, PedidoCriadoEvent> registro
    ) {

        /*
         * Recupera o evento de negócio armazenado no valor
         * da mensagem Kafka.
         */
        PedidoCriadoEvent evento = registro.value();
        /*
         * Simula a notificação que futuramente poderia
         * ser enviada por e-mail, SMS ou outro canal.
         */
        LOGGER.info(
                "Notificacao Processada: topico={}, particao={}, offset={}, " +
                        "chave={}, pedidoId={}, clienteId={}, valor={}",
                registro.topic(),
                registro.partition(),
                registro.offset(),
                registro.key(),
                evento.pedidoId(),
                evento.clienteId(),
                evento.valor()
        );
    }
}