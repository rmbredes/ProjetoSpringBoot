package br.com.exemplo.projetospringboot.dto;

import java.time.Instant;
import java.util.UUID;

/** Representa uma auditoria devolvida pela API REST. */
public record AuditoriaRelatorioDTO(
        Long id,
        UUID eventoId,
        String tipo,
        Long relatorioId,
        Long pedidoId,
        UUID solicitacaoId,
        String objectKey,
        Instant ocorridoEm,
        Instant recebidoEm
) {
}
