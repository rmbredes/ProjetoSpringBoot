package br.com.exemplo.projetospringboot.exception;

/**
 * Indica que a aplicação não conseguiu armazenar o anexo.
 *
 * <p>A causa técnica original fica encadeada nesta exceção para ser
 * registrada nos logs, mas não será exposta ao cliente da API.</p>
 */
public class ArmazenamentoAnexoException
        extends RuntimeException {

    /**
     * @param mensagem descrição segura da operação que falhou
     * @param causa exceção técnica original
     */
    public ArmazenamentoAnexoException(
            String mensagem,
            Throwable causa
    ) {
        super(mensagem, causa);
    }
}