package br.com.exemplo.projetospringboot.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PedidoDTO(
        Long id,
        BigDecimal valor,
        LocalDateTime dataCriacao,

        @NotNull(message = "O cliente é obrigatório")
        Long clienteId
) {
}