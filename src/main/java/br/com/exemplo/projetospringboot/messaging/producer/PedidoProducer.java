package br.com.exemplo.projetospringboot.messaging.producer;

import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Producer responsável por publicar eventos relacionados a pedidos no Kafka.
 *
 * Esta classe conhece o Kafka, mas não contém regras de negócio
 * relacionadas à criação do pedido.
 */
@Component
public class PedidoProducer {

    /*
     * Nome do tópico que receberá os eventos de pedidos criados.
     *
     * O valor deve ser igual ao nome do tópico criado no Kafka.
     */
    private static final String TOPICO_PEDIDOS_CRIADOS =
            "pedidos-criados";

    /*
     * Componente fornecido pelo Spring Kafka para publicar mensagens.
     *
     * String representa o tipo da chave.
     * PedidoCriadoEvent representa o tipo do conteúdo da mensagem.
     */
    private final KafkaTemplate<String, PedidoCriadoEvent> kafkaTemplate;

    /**
     * Recebe o KafkaTemplate por injeção de dependência.
     *
     * @param kafkaTemplate componente utilizado para enviar eventos ao Kafka
     */
    public PedidoProducer(
            KafkaTemplate<String, PedidoCriadoEvent> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publica um evento de pedido criado no tópico pedidos-criados.
     *
     * O pedidoId é enviado como chave. Dessa maneira, todos os eventos
     * do mesmo pedido serão direcionados para a mesma partição.
     *
     * @param evento evento que será publicado
     * @return resultado futuro da operação de envio ao Kafka
     */
    public CompletableFuture<SendResult<String, PedidoCriadoEvent>> publicar(
            PedidoCriadoEvent evento
    ) {
        String chave = evento.pedidoId().toString();

        return kafkaTemplate.send(
                TOPICO_PEDIDOS_CRIADOS,
                chave,
                evento
        );
    }
}