package br.com.exemplo.projetospringboot.enums;

/**
 * Representa o estado do anexo durante seu ciclo de vida.
 *
 * <p>O banco de dados e o Amazon S3 são sistemas independentes.
 * Por isso, o armazenamento de um anexo acontece em etapas e
 * precisamos registrar o ponto em que o processo se encontra.</p>
 */
public enum StatusAnexoPedido {

    /**
     * O registro foi criado no banco, mas o upload ainda
     * não foi confirmado pelo Amazon S3.
     */
    PENDENTE,

    /**
     * O Amazon S3 confirmou o armazenamento do objeto.
     *
     * <p>Somente anexos nesse estado poderão ser listados
     * e disponibilizados para download.</p>
     */
    DISPONIVEL,

    /**
     * O upload não foi concluído.
     *
     * <p>A causa será armazenada no campo ultimoErro.</p>
     */
    FALHA,

    /**
     * O objeto foi removido do S3.
     *
     * <p>O registro permanece no banco para manter o histórico
     * da operação.</p>
     */
    EXCLUIDO
}