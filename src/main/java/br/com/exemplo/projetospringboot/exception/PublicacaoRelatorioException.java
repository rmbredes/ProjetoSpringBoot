package br.com.exemplo.projetospringboot.exception;

/**
 * Indica que uma solicitação de relatório não pôde ser enviada ao SQS.
 *
 * <p>A exceção não expõe detalhes do AWS SDK para as outras camadas.
 * A causa técnica original continua disponível nos logs.</p>
 */
public class PublicacaoRelatorioException extends RuntimeException {

    /**
     * Cria a exceção preservando a falha técnica original.
     *
     * @param mensagem descrição simples do problema
     * @param causa exceção original lançada durante a publicação
     */
    public PublicacaoRelatorioException(
            String mensagem,
            Throwable causa
    ) {
        super(mensagem, causa);
    }
}
