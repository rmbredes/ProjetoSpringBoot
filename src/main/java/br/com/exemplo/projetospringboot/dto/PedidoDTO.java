package br.com.exemplo.projetospringboot.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import br.com.exemplo.projetospringboot.entity.StatusLogisticaPedido;
import br.com.exemplo.projetospringboot.entity.StatusPagamentoPedido;

/**
 * Transporta os dados de um pedido pela API.
 *
 * @param id identificador gerado após a persistência
 * @param valor valor monetário do pedido
 * @param dataCriacao instante em que o pedido foi criado
 * @param clienteId identificador obrigatório do cliente proprietário
 */
public record PedidoDTO(
        Long id,
        BigDecimal valor,
        LocalDateTime dataCriacao,

        @NotNull(message = "O cliente é obrigatório")
        Long clienteId,

        UUID pagamentoEventoId,
        Long pagamentoId,
        StatusPagamentoPedido statusPagamento,
        StatusLogisticaPedido statusLogistica
) {
}