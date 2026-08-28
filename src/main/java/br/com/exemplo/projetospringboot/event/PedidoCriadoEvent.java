package br.com.exemplo.projetospringboot.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado quando um pedido é criado.
 *
 * Este record funciona como um contrato entre o produtor
 * e os consumidores da mensagem.
 *
 * Ele não é uma entidade JPA e não representa diretamente
 * uma tabela do banco de dados.
 */
public record PedidoCriadoEvent(

        // Identifica exclusivamente este evento.
        // Ajuda a detectar e impedir processamentos duplicados.
        UUID eventoId,

        // Identifica o pedido que originou o evento.
        // Também será usado como chave da mensagem no Kafka.
        Long pedidoId,

        // Identifica o cliente responsável pelo pedido.
        Long clienteId,

        // Informa o valor total do pedido no momento da criação.
        BigDecimal valor,

        // Registra o instante exato em que o evento aconteceu.
        Instant ocorridoEm

) {
}