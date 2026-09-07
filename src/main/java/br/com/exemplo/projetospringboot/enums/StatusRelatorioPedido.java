package br.com.exemplo.projetospringboot.enums;

/**
 * Representa as etapas da geração assíncrona de um relatório de pedido.
 *
 * <p>O status permite que a API responda imediatamente à solicitação
 * e que o usuário consulte depois se o arquivo já está disponível.</p>
 */
public enum StatusRelatorioPedido {

    /**
     * A solicitação foi registrada no banco, mas ainda precisa ser
     * confirmada na fila do Amazon SQS.
     */
    PENDENTE,

    /**
     * O Amazon SQS confirmou o recebimento da mensagem e agora
     * aguardamos um consumer gerar o arquivo.
     */
    ENFILEIRADO,

    /**
     * O consumer terminou a geração e o Amazon S3 confirmou
     * o armazenamento do PDF.
     */
    DISPONIVEL,

    /**
     * A solicitação não pôde ser concluída.
     *
     * <p>A descrição da última falha permanece registrada na entidade
     * para auxiliar o estudo e a investigação do problema.</p>
     */
    FALHA
}
