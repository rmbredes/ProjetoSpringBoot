package br.com.exemplo.projetospringboot.dto;

public record TokenDTO(
        String token,
        String tipo,
        long expiresIn
) {
}

