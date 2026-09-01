package br.com.exemplo.projetospringboot.dto;

/**
 * Devolve ao cliente o token emitido após uma autenticação válida.
 *
 * @param token valor JWT enviado nas requisições protegidas
 * @param tipo esquema de autenticação, normalmente Bearer
 * @param expiresIn tempo de validade do token em segundos
 */
public record TokenDTO(
        String token,
        String tipo,
        long expiresIn
) {
}

