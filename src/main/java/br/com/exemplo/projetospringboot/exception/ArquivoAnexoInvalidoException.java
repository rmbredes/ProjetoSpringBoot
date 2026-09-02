package br.com.exemplo.projetospringboot.exception;

/**
 * Indica que o arquivo enviado viola uma regra da aplicação.
 *
 * <p>Exemplos:</p>
 *
 * <ul>
 *     <li>arquivo vazio;</li>
 *     <li>arquivo maior que 10 MB;</li>
 *     <li>tipo não permitido;</li>
 *     <li>nome ausente ou muito grande.</li>
 * </ul>
 */
public class ArquivoAnexoInvalidoException
        extends RuntimeException {

    /**
     * @param mensagem explicação pública da validação que falhou
     */
    public ArquivoAnexoInvalidoException(String mensagem) {
        super(mensagem);
    }
}