package br.com.exemplo.projetospringboot.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Resposta devolvida pelo logistica-service.
 *
 * Usamos String no status para não compartilhar o enum interno
 * existente em outra aplicação.
 */
public record EntregaResponse(
        Long id,
        UUID eventoPagamentoId,
        Long pagamentoId,
        Long pedidoId,
        String status,
        Instant autorizadaEm
) {
}