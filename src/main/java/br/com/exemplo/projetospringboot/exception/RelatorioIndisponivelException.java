package br.com.exemplo.projetospringboot.exception;

/**
 * Indica que o relatório existe, mas ainda não pode ser baixado.
 */
public class RelatorioIndisponivelException extends RuntimeException {

    /** @param mensagem explicação simples apresentada pela API */
    public RelatorioIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
