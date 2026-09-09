package br.com.exemplo.projetospringboot.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato recebido do tópico pagamentos-processados.
 *
 * Deve possuir os mesmos campos publicados pelo pagamento-service,
 * mas pertence ao código do monólito.
 */
public record PagamentoProcessadoEvent(
        UUID eventoId,
        Long pagamentoId,
        Long pedidoId,
        String status,
        String motivoRecusa,
        Instant ocorridoEm
) {
}