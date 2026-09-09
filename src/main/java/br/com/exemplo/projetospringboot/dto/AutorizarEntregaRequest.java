package br.com.exemplo.projetospringboot.dto;

import java.util.UUID;

/**
 * Corpo enviado pelo monólito ao logistica-service.
 */
public record AutorizarEntregaRequest(
        UUID eventoPagamentoId,
        Long pagamentoId,
        Long pedidoId
) {
}