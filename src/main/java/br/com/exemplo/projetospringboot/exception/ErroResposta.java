package br.com.exemplo.projetospringboot.exception;

import java.time.LocalDateTime;

/**
 * Padroniza o corpo devolvido quando uma requisição termina com erro.
 *
 * @param timestamp instante em que o erro foi tratado
 * @param status código numérico do status HTTP
 * @param erro nome resumido do tipo de erro
 * @param mensagem explicação destinada ao consumidor da API
 * @param caminho endereço da requisição que falhou
 */
public record ErroResposta(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho
) {
}
